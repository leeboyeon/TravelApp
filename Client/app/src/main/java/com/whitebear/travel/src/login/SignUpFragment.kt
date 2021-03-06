package com.whitebear.travel.src.login

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import com.google.android.material.snackbar.Snackbar
import com.google.common.reflect.TypeToken
import com.jakewharton.rxbinding3.widget.textChanges
import com.whitebear.travel.R
import com.whitebear.travel.config.BaseFragment
import com.whitebear.travel.databinding.FragmentSignUpBinding
import com.whitebear.travel.src.dto.User
import com.whitebear.travel.src.network.service.UserService
import com.whitebear.travel.util.CommonUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.runBlocking
import retrofit2.Response
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


class SignUpFragment : BaseFragment<FragmentSignUpBinding>(FragmentSignUpBinding::bind, R.layout.fragment_sign_up) {
    private val TAG = "SignUpFragment"
    private lateinit var loginActivity: LoginActivity
    private lateinit var editTextSubscription: Disposable

    private var isEmailPossible = false
    private lateinit var certCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        loginActivity = context as LoginActivity
        loginActivity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        joinBtnClickEvent()
        initDomain()

        loginActivity.runOnUiThread(kotlinx.coroutines.Runnable {
            inputObservable()
        })
    }


    /**
     * email ?????? ??????
     * @return ????????? ???????????? ????????? true ??????
     */
    private fun existEmailChk(email: String) : Boolean {
        var existEmailRes : HashMap<String, Any>
        runBlocking {
            existEmailRes = mainViewModel.existsChkUserEmail(email)
        }

        val msg = existEmailRes["message"] as String

        if(existEmailRes["isSuccess"] == true && msg.contains("there is no email")) {   // ???????????? ????????? ??????.
            binding.signUpFragmentTilEmail.isErrorEnabled = false
            binding.signUpFragmentTilDomain.isErrorEnabled = false
            return true
        } else if(existEmailRes["isSuccess"] == true && msg.contains("exist email") ) { // ?????? ???????????? ?????????
            binding.signUpFragmentTilEmail.error = "????????? ??????????????????. ?????? ????????? ?????????."
            val type: Type = object : TypeToken<HashMap<String, Any>>() {}.type
            val socialType = CommonUtils.parseDto<HashMap<String, Any>>(existEmailRes["data"]!!, type)

            if(socialType["social_type"] == "none") {
                showCustomToast("?????? ???????????? ??????????????????.")
                return false
            } else {
                Snackbar.make(requireView(), "?????? ???????????? ?????? ???????????????! \n$socialType (???)??? ????????? ???????????????(*???????*)???", Snackbar.LENGTH_LONG).show()
                (requireActivity() as LoginActivity).onBackPressed()
                return false
            }
            return false
        } else {
            showCustomToast("?????? ????????? ??????????????????.")
            Log.d(TAG, "existEmailChk: ${existEmailRes["message"]}")
            return false
        }
    }


    // join ?????? ?????? ?????????
    private fun joinBtnClickEvent() {
        binding.signUpFragmentBtnJoin.setOnClickListener {
            val nickname = binding.signUpFragmentTietNickname.text.toString()
            val username = binding.signUpFragmentTietUserName.text.toString()
            val password = binding.signUpFragmentTietPw.text.toString()

            val email = validatedEmail()
            if(existEmailChk(email!!) == false) {
                showCustomToast("????????? ??????????????????. ?????? ???????????? ????????? ?????????.")
                binding.signUpFragmentEtEmail.requestFocus()
            } else {

                val user = isAvailable(nickname, username, password, email, "none")
                if(user != null) {
                    join(email!!, nickname, username, password, "none")

    //                if(joinRes.data["isSignup"] == true && joinRes.message == "???????????? ??????") {
    //                    showCustomToast("??????????????? ?????????????????????. ?????? ????????? ????????????.")
    //                    (requireActivity() as LoginActivity).onBackPressed()
    //                } else if(joinRes.data["isSignup"] == false && joinRes.message == "???????????? ??????") {
    //                    showCustomToast("??????????????? ??????????????????. ?????? ????????? ?????????.")
    //                } else if(joinRes.data["isExist"] == false && joinRes.message == "???????????? ??????") {
    //                    showCustomToast("?????? ???????????? ??????????????????. ?????? ????????? ?????????.")
    //                } else {
    //                    showCustomToast("?????? ????????? ??????????????????.")
    //                    Log.d(TAG, "joinBtnClickEvent: ${joinRes.message}")
    //                }
                } else {
                    showCustomToast("?????? ?????? ?????? ????????? ?????????.")
                }

            }
        }

    }

    // ????????????
    private fun join(email: String, nickname: String, username: String, password: String, socialType: String) {

        var response : Response<HashMap<String, Any>>

        runBlocking {
            response = UserService().insertUser(User(email = email, password = loginActivity.sha256(password), nickname = nickname, username = username, social_type = socialType))
            Log.d(TAG, "join: $response")
//            mainViewModel.join(User(email = email, password = loginActivity.sha256(password), nickname = nickname, username = username, social_type = socialType))
        }
        if(response.code() == 200 || response.code() == 500 || response.code() == 201) {
            val res = response.body()
            if (res != null) {
                Log.d(TAG, "join: $res")
                if(res["isSuccess"] == true && res["message"] == "create user successful") {
                    showCustomToast("??????????????? ?????????????????????. ?????? ????????? ????????????.")
                    (requireActivity() as LoginActivity).onBackPressed()
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
     * ?????? ????????? email ????????? ?????? ??? ?????? ??????, pw, nickname ????????? ?????? ?????? ??????
     * @return ?????? ????????? ???????????? user ????????? ??????
     */
    private fun isAvailable(nickname: String, username: String, password: String, email: String?, socialType: String) : User? {

        if(validatedNickname(nickname) && validatedUsername(username) && validatedPw(password) && email != null) {
            return User(email = email, password = password, nickname = nickname, username = username, social_type = socialType)
        } else {
            return null
        }
    }


    /**
     * ??? EditText ?????? ???????????? ??????
     */
    private fun inputObservable() {

        binding.signUpFragmentTietNickname.setQueryDebounce {
            validatedNickname(it)
        }

        binding.signUpFragmentTietUserName.setQueryDebounce {
            validatedUsername(it)
        }

        binding.signUpFragmentTietPw.setQueryDebounce {
            validatedPw(it)
        }

        binding.signUpFragmentEtEmail.setQueryDebounce {
            validatedEmail()
        }

        binding.signUpFragmentEtDomain.setQueryDebounce {
            validatedEmail()
        }
    }

    /**
     * ????????? nickname ?????? ??? ??? ??? ??????
     * @return ?????? ??? true ??????
     */
    private fun validatedNickname(nickname: String) : Boolean{
        if(nickname.trim().isEmpty()){
            binding.signUpFragmentTilNickname.error = "Required Field"
            binding.signUpFragmentTietNickname.requestFocus()
            return false
        } else if(nickname.length >= 25) {
            binding.signUpFragmentTilNickname.error = "Nickname ????????? 25??? ????????? ????????? ?????????."
            binding.signUpFragmentTietNickname.requestFocus()
            return false
        }
        else {
            binding.signUpFragmentTilNickname.error = null
            return true
        }
    }

    /**
     * ????????? username ?????? ??? ??? ??? ??????
     * @return ?????? ??? true ??????
     */
    private fun validatedUsername(username: String) : Boolean{
        if(username.trim().isEmpty()){
            binding.signUpFragmentTilUserName.error = "Required Field"
            binding.signUpFragmentTietUserName.requestFocus()
            return false
        } else if(username.length >= 25) {
            binding.signUpFragmentTilUserName.error = "UserName ????????? 25??? ????????? ????????? ?????????."
            binding.signUpFragmentTietUserName.requestFocus()
            return false
        }
        else {
            binding.signUpFragmentTilUserName.error = null
            return true
        }
    }

    /**
     * ????????? password ????????? ??????
     * @return ????????? ?????? ?????? ??? true ??????
     */
    private fun validatedPw(pw: String) : Boolean {
        if(pw.trim().isEmpty()) {   // ?????? ???????????????
            binding.signUpFragmentTilPW.error = "Required Field"
            binding.signUpFragmentTietPw.requestFocus()
            return false
        } else if(!Pattern.matches("^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[\$@!%*#?&]).{8,25}.\$", pw)) {
            binding.signUpFragmentTilPW.error = "???????????? ????????? ??????????????????.(??????, ??????, ???????????? ?????? 8 ~ 25)"
            binding.signUpFragmentTietPw.requestFocus()
            return false
        }
        else {
            binding.signUpFragmentTilPW.isErrorEnabled = false
            return true
        }
    }

    /**
     * email ?????? ????????? ??????
     * @return email ???????????? email(String), ????????? null
     */
    private fun validatedEmail() : String? {
        val inputEmail = binding.signUpFragmentEtEmail.text.toString()
        val inputDomain = binding.signUpFragmentEtDomain.text.toString()

        val email = "$inputEmail@$inputDomain"

        if(inputDomain.trim().isEmpty()) {
            binding.signUpFragmentTilDomain.error = "Required Domain Field"
            return null
        }
        if(inputEmail.trim().isEmpty()) {
            binding.signUpFragmentTilEmail.error = "Required Email Field"
            return null
        }
        else if(!Pattern.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+.[A-Za-z].{2,25}\$", email)) {
            binding.signUpFragmentTilEmail.error = "????????? ????????? ??????????????????."
            return null
        }
        else {
            binding.signUpFragmentTilEmail.isErrorEnabled = false
            binding.signUpFragmentTilDomain.isErrorEnabled = false
            existEmailChk(email)
            return email
        }
    }

    /**
     * email domain list set Adapter
     */
    private fun initDomain() {
        // ?????????????????? ????????? ?????????
        val domains = arrayOf("gmail.com", "naver.com", "nate.com", "daum.net", "kakao.com", "icloud.com", "outlook.com", "hotmail.com", "outlook.kr")

        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, domains)
        binding.signUpFragmentEtDomain.setAdapter(adapter)
    }


    /**
     * EditText??? ?????? ???????????? ??????
     */
    private fun EditText.setQueryDebounce(queryFunction: (String) -> Unit): Disposable {
        val editTextChangeObservable = this.textChanges()
        editTextSubscription =
            editTextChangeObservable
                // ????????? ?????? ?????? 0.5??? ?????? onNext ???????????? ????????? ??????
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                // ????????? ?????? ????????? ?????? ??????
                .subscribeBy(
                    onNext = {
                        Log.d(TAG, "onNext : $it")
                        queryFunction(it.toString())
                    },
                    onComplete = {
                        Log.d(TAG, "onComplete")
                    },
                    onError = {
                        Log.i(TAG, "onError : $it")
                    }
                )
        return editTextSubscription  // Disposable ??????
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!editTextSubscription.isDisposed()) {
            editTextSubscription.dispose()
        }
    }
}