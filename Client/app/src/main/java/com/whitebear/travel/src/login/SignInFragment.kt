package com.whitebear.travel.src.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.rx
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthBehavior
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import com.whitebear.travel.R
import com.whitebear.travel.config.ApplicationClass
import com.whitebear.travel.config.BaseFragment
import com.whitebear.travel.databinding.FragmentSignInBinding
import com.whitebear.travel.src.dto.NidProfile
import com.whitebear.travel.src.dto.User
import com.whitebear.travel.src.network.service.UserService
import com.whitebear.travel.util.CommonUtils
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import retrofit2.Response
import java.lang.reflect.Type

class SignInFragment : BaseFragment<FragmentSignInBinding>(FragmentSignInBinding::bind, R.layout.fragment_sign_in) {
    private val TAG = "SignInFragment"

    private lateinit var loginActivity: LoginActivity

    // google ?????????
    private lateinit var mAuth: FirebaseAuth
    var mGoogleSignInClient: GoogleSignInClient? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        loginActivity = context as LoginActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListener()

    }

    private fun initListener() {

        // SignUp now
        binding.signInFragmentSignUpBtn.setOnClickListener {
            loginActivity.openFragment(3)
        }

        // Forgot password
        binding.signInFragmentLostPwTv.setOnClickListener {
            loginActivity.openFragment(4)
        }

        // login
        binding.signInFragmentLoginBtn.setOnClickListener {
            login(binding.signInFragmentEmailEt.text.toString(), binding.signInFragmentPwEt.text.toString())
        }

        // kakao Login
        kakaoLoginBtnClickEvent()

        // google Login
        googleLoginBtnClickEvent()

        // naver login SDK ?????????
        NaverIdLoginSDK.apply {
            showDevelopersLog(true)
            initialize(requireContext(), getString(R.string.naver_client_id), getString(R.string.naver_client_secret), getString(R.string.naver_client_name))
            isShowMarketLink = true
            isShowBottomTab = true
        }

        naverLoginBtnClickEvent()
    }

    private fun login(email: String, password: String){
        var result : HashMap<String, Any>

        runBlocking {
            result = mainViewModel.login(email, loginActivity.sha256(password))
        }

        if(result["data"] != null && result["message"] == "login success") {
            val loginUser = result["data"]

            val type: Type = object : TypeToken<User>() {}.type
            val user = CommonUtils.parseDto<User>(loginUser!!, type)

            ApplicationClass.sharedPreferencesUtil.addUser(User(user.id, user.token))

            if(binding.signInFragmentAutoLoginCb.isChecked) {   // ?????? ???????????? ???????????? ?????????
                ApplicationClass.sharedPreferencesUtil.setAutoLogin(user.id)
            }

            showCustomToast("????????? ???????????????.")
            loginActivity.openFragment(1)

        } else if(result["isSuccess"] == false) {
            showCustomToast("ID??? PW??? ????????? ?????????.")

        } else if(email.isEmpty() || password.isEmpty()){
            showCustomToast("E-MAIN, PW??? ????????? ?????????")
        } else {
            showCustomToast("???????????? ??????????????????. ?????? ????????? ?????????.")
            Log.d(TAG, "loginBtnClickEvent: ${result["data"]} ${result["message"]}")
        }
    }

    /**
     * email ?????? ??????
     * @return ????????? ???????????? ????????? true ??????
     */
    private fun existEmailChk(user: User) : Boolean {
        var existEmailRes : HashMap<String, Any>
        runBlocking {
            existEmailRes = mainViewModel.existsChkUserEmail(user.email)
        }

        val msg = existEmailRes["message"] as String

        if(existEmailRes["isSuccess"] == true && msg.contains("there is no email")) {   // ???????????? ????????? ??????.
            snsLoginJoin(user)
            return true
        } else if(existEmailRes["isSuccess"] == true && msg.contains("exist email") ) { // ?????? ???????????? ?????????
            login(user.email, user.password)
            return false
        } else {
            showCustomToast("?????? ????????? ??????????????????.")
            Log.d(TAG, "existEmailChk: ${existEmailRes["message"]}")
            return false
        }
    }

    /**
     * ????????? ??????????????? ???????????? ??????
     */
    private fun snsLoginJoin(user: User) {
        val realPw = user.password

        val encPw = loginActivity.sha256(user.password)
        user.password = encPw

        var response : Response<HashMap<String, Any>>

        runBlocking {
            response = UserService().insertUser(user)
//            mainViewModel.join(User(email = email, password = loginActivity.sha256(password), nickname = nickname, username = username, social_type = socialType))
        }
        if(response.code() == 200 || response.code() == 500 || response.code() == 201) {
            val res = response.body()
            if (res != null) {
                Log.d(TAG, "join: $res")
                if(res["isSuccess"] == true && res["message"] == "create user successful") {
                    login(user.email, realPw)
                } else if(res["isSuccess"] == false) {
                    showCustomToast("??????????????? ??????????????????. ?????? ????????? ?????????.")
                }
//                else {
//                    Log.d(TAG, "join: $res")
//                    showCustomToast("??????????????? ??????????????????. ?????? ????????? ?????????.")
//                }
            }
        }
    }

    /**
     * sns Login - Google ?????????
     */

    // firebase auth ?????? ?????????
    private fun initAuth() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_login_key))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        mAuth = FirebaseAuth.getInstance()

    }

    private fun googleLoginBtnClickEvent() {
        binding.signInFragmentGoogleBtn.setOnClickListener {
            initAuth()
            signIn()
        }
    }

    // ?????? ????????? ?????? ????????? ??????
    private fun signIn() {
        val signInIntent = mGoogleSignInClient!!.signInIntent
        requestActivity.launch(signInIntent)
    }

    // ?????? ?????? ?????? ?????? ??? ?????? ??????
    private val requestActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val data = it.data

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)

            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    // ?????? ?????? ?????? ?????? ????????? ?????? ??????
    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    if(user != null) {
                        // email nickname pw photo
                        val email = user.email.toString()
                        val nickname = user.displayName.toString()
                        val uid = user.uid
                        val newUser = User(email = email, password = uid, username = nickname, nickname = nickname, "google")

                        Log.d(TAG, "firebaseAuthWithGoogle: $newUser")
                        existEmailChk(newUser)
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    // ---------------------------------------------------------------------------------------------
    /**
     * sns Login - Kakao
     */
    private fun kakaoLoginBtnClickEvent() {
        val disposables = CompositeDisposable()
        binding.signInFragmentKakaoBtn.setOnClickListener {
            // ??????????????? ???????????? ????????? ?????????????????? ?????????, ????????? ????????????????????? ?????????
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(requireContext())) {
                UserApiClient.rx.loginWithKakaoTalk(requireContext())
                    .observeOn(AndroidSchedulers.mainThread())
                    .onErrorResumeNext { error ->
                        // ???????????? ???????????? ?????? ??? ???????????? ?????? ?????? ???????????? ???????????? ????????? ??????,
                        // ???????????? ????????? ????????? ?????? ????????????????????? ????????? ?????? ?????? ????????? ????????? ?????? (???: ?????? ??????)
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            Single.error(error)
                        } else {
                            // ??????????????? ????????? ?????????????????? ?????? ??????, ????????????????????? ????????? ??????
                            UserApiClient.rx.loginWithKakaoAccount(requireContext())
                        }
                    }.observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ token ->
                        Log.i(TAG, "????????? ?????? ${token.accessToken}")
                        kakaoLogin()
                    }, { error ->
                        Log.e(TAG, "????????? ??????", error)
                    }).addTo(disposables)
            } else {
                UserApiClient.rx.loginWithKakaoAccount(requireContext())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ token ->
                        Log.i(TAG, "????????? ?????? ${token.accessToken}")
                        kakaoLogin()
                    }, { error ->
                        Log.e(TAG, "????????? ??????", error)
                    }).addTo(disposables)
            }
        }
    }

    private fun kakaoLogin() {
        val disposables = CompositeDisposable()
        // ????????? ?????? ?????? (??????)
        UserApiClient.rx.me()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ user ->
//                Log.i(TAG, "????????? ?????? ?????? ??????" +
//                        "\n????????????: ${user.id}" +  // pw
//                        "\n?????????: ${user.kakaoAccount?.email}" +  // id, email
//                        "\n?????????: ${user.kakaoAccount?.profile?.nickname}" +  // nickname
//                        "\n???????????????: ${user.kakaoAccount?.profile?.thumbnailImageUrl}")   // image

                val email = user.kakaoAccount?.email.toString()
                val uid = user.id.toString()
                val nickname = user.kakaoAccount?.profile?.nickname.toString()
//                val image = user.kakaoAccount?.profile?.thumbnailImageUrl.toString()

                val newUser = User(email = email, password = uid, username = nickname, nickname = nickname, "kakao")
                existEmailChk(newUser)

            }, { error ->
                Log.e(TAG, "????????? ?????? ?????? ??????", error)
            })
            .addTo(disposables)
    }




    /**
     * #S06P12D109-14
     * sns Login - Naver
     */

    private fun naverLoginBtnClickEvent() {
        binding.signInFragmentNaverBtn.setOnClickListener {
//            NaverIdLoginSDK.authenticate(requireContext(), oAuthLoginCallback)
            NaverIdLoginSDK.behavior = NidOAuthBehavior.DEFAULT
            NaverIdLoginSDK.authenticate(requireContext(), object : OAuthLoginCallback {
                override fun onSuccess() {
                    // ???????????? ????????? ????????? ???????????? api ??????
                    NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
                        override fun onSuccess(profileResponse: NidProfileResponse) {

                            val type: Type = object : TypeToken<NidProfile>() {}.type
                            val user = CommonUtils.parseDto<NidProfile>(profileResponse.profile!!, type)

                            val email = user.email
                            val uid = user.id
                            val nickname = user.nickname
                            val username = user.name

                            val newUser = User(email = email, password = uid, username = username, nickname = nickname, "naver")
                            existEmailChk(newUser)
                        }

                        override fun onFailure(httpStatus: Int, message: String) {
                            val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                            val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                            showCustomToast("errorCode:$errorCode, errorDesc:$errorDescription")
                        }

                        override fun onError(errorCode: Int, message: String) {
                            onFailure(errorCode, message)
                        }
                    })
                }

                override fun onFailure(httpStatus: Int, message: String) {
                    val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                    val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                    showCustomToast("errorCode:$errorCode, errorDesc:$errorDescription")
                }

                override fun onError(errorCode: Int, message: String) {
                    onFailure(errorCode, message)
                }
            })
        }
    }


    // ---------------------------------------------------------------------------------------------
//    val mOAuthLoginHandler: OAuthLoginHandler = object : OAuthLoginHandler() {
//        override fun run(success: Boolean) {
//            if (success) {
//                val accessToken: String = mOAuthLoginInstance.getAccessToken(requireContext())
//                Log.d(TAG, "run: $accessToken")
//                RequestApiTask(requireContext(), mOAuthLoginInstance).execute()
//            } else {
//                val errorCode: String = mOAuthLoginInstance.getLastErrorCode(requireContext()).code
//                val errorDesc = mOAuthLoginInstance.getLastErrorDesc(requireContext())
//                Log.d(TAG, "run: errorCode:" + errorCode + ", errorDesc:" + errorDesc)
//            }
//        }
//    }
//
//
//    inner class RequestApiTask(private val mContext: Context, private val mOAuthLoginModule: OAuthLogin) :
//        AsyncTask<Void?, Void?, String>() {
//        override fun onPreExecute() {}
//
//        override fun onPostExecute(content: String) {
//            try {
//                val loginResult = JSONObject(content)
//                if (loginResult.getString("resultcode") == "00") {
//                    val response = loginResult.getJSONObject("response")
//                    val id = response.getString("email")
//                    val pw = response.getString("id")   // ????????? ?????? ??????
//                    val nickname = response.getString("nickname")
//                    val mobile = response.getString("mobile")
//                    val gender = response.getString("gender")
//                    val birthYear = response.getString("birthyear")
//                    val birthDay = response.getString("birthday")
//                    var image = response.getString("profile_image")
//                    image = image.replace("\\", "")
//                    val newUser = User(id, pw, nickname, mobile, id, "$birthYear-$birthDay", gender, "naver", image)
//                    UserService().isUsedId(id, isUsedIdCallback(newUser))
//                }
//            } catch (e: JSONException) {
//                e.printStackTrace()
//            }
//        }
//
//        override fun doInBackground(vararg params: Void?): String {
//            val url = "https://openapi.naver.com/v1/nid/me"
//            val at = mOAuthLoginModule.getAccessToken(mContext)
//            return mOAuthLoginModule.requestApi(mContext, at, url)
//        }
//    }


}