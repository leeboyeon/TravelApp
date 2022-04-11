package com.whitebear.travel.src.main.my

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.whitebear.travel.R
import com.whitebear.travel.databinding.ItemLikePlaceRouteBinding

class LikePlaceRouteRecyclerviewAdapter() : RecyclerView.Adapter<LikePlaceRouteRecyclerviewAdapter.MyScheduleViewHolder>() {
    var list = mutableListOf<Int>()

    inner class MyScheduleViewHolder(private val binding: ItemLikePlaceRouteBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyScheduleViewHolder {
        return MyScheduleViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.item_like_place_route, parent, false))
    }

    override fun onBindViewHolder(holder: MyScheduleViewHolder, position: Int) {
        holder.apply {
            bind()
//            .setOnClickListener {
//                itemClickListener.onClick(it, position)
//            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface ItemClickListener{
        fun onClick(view: View, position: Int)
    }

    private lateinit var itemClickListener : ItemClickListener

    fun setItemClickListener(itemClickListener: ItemClickListener){
        this.itemClickListener = itemClickListener
    }
}