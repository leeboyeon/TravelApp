package com.whitebear.travel.src.network.binding

import android.annotation.SuppressLint
import android.os.Build
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.whitebear.travel.R
import com.whitebear.travel.src.dto.*
import com.whitebear.travel.src.main.home.*
import com.whitebear.travel.src.main.place.PlaceAdapter
import com.whitebear.travel.src.main.place.PlaceReviewAdapter
import com.whitebear.travel.src.main.route.RouteAdapter
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@BindingAdapter("imageUrlArea")
fun bindImageArea(imgView:ImageView, imgUrl:String?){
    Glide.with(imgView.context)
        .load(imgUrl)
        .circleCrop()
        .into(imgView)
}
@BindingAdapter("areaListData")
fun bindAreaRecyclerView(recyclerView: RecyclerView, data:List<Area>?){
    var adapter = recyclerView.adapter as AreaAdapter
    if(recyclerView.adapter == null){
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
    }else{
        adapter = recyclerView.adapter as AreaAdapter
    }

    adapter.list = data as MutableList<Area>
    adapter.notifyDataSetChanged()
}
@BindingAdapter("bestRoutesListData")
fun bindBestRouteRecyclerView(recyclerView: RecyclerView, data:List<Route>?){
    var adapter = recyclerView.adapter as BestRoutesAdapter
    if(recyclerView.adapter == null){
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
    }else{
        adapter = recyclerView.adapter as BestRoutesAdapter
    }
    adapter.list = data as MutableList<Route>
    adapter.notifyDataSetChanged()
}
@BindingAdapter("bestPlacesListData")
fun bindBestPlaceRecyclerView(recyclerView: RecyclerView, data:List<Place>?){
    var adapter = recyclerView.adapter as BestPlaceAdapter
    if(recyclerView.adapter == null){
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
    }else{
        adapter = recyclerView.adapter as BestPlaceAdapter
    }
    adapter.list = data as MutableList<Place>
    adapter.notifyDataSetChanged()
}
@BindingAdapter("imageUrlPlace")
fun bindImagePlace(imgView:ImageView, imgUrl: String?){
    Glide.with(imgView.context)
        .load(imgUrl)
        .into(imgView)
}
@BindingAdapter("textViewRoutePlaceListSize")
fun bindTextViewRoutePlaceListSize(textView: TextView, size:Int){
    if(size == 0){
        textView.text = "총 4곳"
    }
    textView.text = "총 ${size}곳"
}
@SuppressLint("SetTextI18n")
@BindingAdapter("textViewContent")
fun bindTextViewContent(textView: TextView, content:String){
    if(content.length > 35){
        textView.text = content.substring(0,35)+"..."
    }else{
        textView.text = content
    }

}
@BindingAdapter("textViewTitle")
fun bindTextViewTitle(textView: TextView, title:String){
    if(title.length>10){
        textView.text = title.substring(0,9)+".."
    }else{
        textView.text = title
    }
}
@SuppressLint("NotifyDataSetChanged")
@BindingAdapter("placeListData")
fun bindPlaceRecyclerView(recyclerView: RecyclerView, data:List<Place>?){
    var adapter = recyclerView.adapter as PlaceAdapter
    if(recyclerView.adapter == null){
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
    }else{
        adapter = recyclerView.adapter as PlaceAdapter
    }
    adapter.list = data as MutableList<Place>
    adapter.notifyDataSetChanged()
}
@BindingAdapter("placeTypeListData")
fun bindPlaceTypeRecyclerView(recyclerView: RecyclerView, data:List<Place>?){
    var adapter = recyclerView.adapter as PlaceTypeAdapter
    if(recyclerView.adapter == null){
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
    }else{
        adapter = recyclerView.adapter as PlaceTypeAdapter
    }
    adapter.list = data as MutableList<Place>
    adapter.notifyDataSetChanged()
}

@SuppressLint("NotifyDataSetChanged")
@BindingAdapter("routeListData")
fun bindRouteRecyclerView(recyclerView: RecyclerView, data:List<Route>?){
    var adapter = recyclerView.adapter as RouteAdapter
    if(recyclerView.adapter == null){
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
    }else{
        adapter = recyclerView.adapter as RouteAdapter
    }
    adapter.list = data as MutableList<Route>
    adapter.notifyDataSetChanged()
}
@BindingAdapter("placeReviewListData")
fun bindPlaceReviewRecyclerView(recyclerView: RecyclerView, data:List<PlaceReview>?) {
    var adapter = recyclerView.adapter as PlaceReviewAdapter
    if(recyclerView.adapter == null){
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
    }else{
        adapter = recyclerView.adapter as PlaceReviewAdapter
    }

    adapter.list = data as MutableList<PlaceReview>
    adapter.notifyDataSetChanged()
}
@BindingAdapter("textViewTotalReview")
fun bindTextViweReviewTotal(textView: TextView, size:Int){
    textView.text = "총 ${size}건"
}

@BindingAdapter("placeNavListData")
fun bindPlaceNavRecyclerView(recyclerView: RecyclerView, data:List<Navigator>?) {
    var adapter = recyclerView.adapter as NavPlaceAdapter
    if(recyclerView.adapter == null){
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
    }else{
        adapter = recyclerView.adapter as NavPlaceAdapter
    }
    adapter.list = data as MutableList<Navigator>
    adapter.notifyDataSetChanged()
}
//@BindingAdapter("weatherImageUrl")
//fun bindWeatherImageUrl(imgView:ImageView, weathers : Weather.Items){
//    var weather = weathers.item
//    for(i in 0..weather.size-1){
//        if(weather[i].category.equals("SKY")){
//            if(weather[i].fcstValue.equals("1")){
//                Glide.with(imgView.context)
//                    .load(R.drawable.weather1)
//                    .into(imgView)
//            }else if(weather[i].fcstValue.equals("2")){
//                Glide.with(imgView.context)
//                    .load(R.drawable.weather2)
//                    .into(imgView)
//            }else if(weather[i].fcstValue.equals("3")){
//                Glide.with(imgView.context)
//                    .load(R.drawable.weather3)
//                    .into(imgView)
//            }else if(weather[i].fcstValue.equals("4")){
//                Glide.with(imgView.context)
//                    .load(R.drawable.weather4)
//                    .into(imgView)
//            }
//        }
//    }
//
//}
//
//@BindingAdapter("weatherTmpText")
//fun bindTextViewWeatherTemp(textView: TextView, weathers:Weather.Items){
//    var weather = weathers.item
//    for(i in 0..weather.size-1){
//        if(weather[i].category.equals("T3H") || weather[i].category.equals("T1H") || weather[i].category.equals("TMP")){
//            textView.text = weather[i].fcstValue + "℃"
//        }
//    }
//
//}

@BindingAdapter("myLikePlaceName")
fun bindLikePlaceName(textView: TextView, address: String) {
    val tmp = address.split(" ")
    if(tmp.size > 1) {
        textView.text = "[${tmp[0]}]"
    } else if(tmp.size == 0) {
        textView.isVisible = false
    }
}

@BindingAdapter("distancePlace")
fun bindPlaceDistance(textView: TextView, dist: Double) {
    textView.text = "${String.format("%.2f", dist)} km"
}

@BindingAdapter("textViewDate")
fun bindTextViewDate(textView: TextView, date:String) {
    var modStr = date.replace("T"," ")
    var result = modStr.substring(0,19)
    textView.text = result
}

@BindingAdapter("notiTypeImg")
fun bindNotiTypeImg(imgView: ImageView, type: String) {
    if(type == "info") {
        Glide.with(imgView.context)
            .load(R.drawable.ic_noti_info)
            .into(imgView)
    } else {
        Glide.with(imgView.context)
            .load(R.drawable.ic_noti_event)
            .into(imgView)
    }
}