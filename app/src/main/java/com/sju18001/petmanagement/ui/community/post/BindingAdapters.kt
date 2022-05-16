package com.sju18001.petmanagement.ui.community.post

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.controller.Util

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("timestamp")
    fun bindTimestamp(textView: TextView, timestamp: String) {
        textView.text = Util.getTimestampForDisplay(timestamp) + "전"
    }

    @JvmStatic
    @BindingAdapter("holder", "postAdapterInterface", "accountId", "photoUrl")
    fun bindAccountPhoto(
        view: View,
        holder: PostAdapter.ViewHolder,
        postAdapterInterface: PostAdapterInterface,
        accountId: Long,
        photoUrl: String?
    ){
        if(!photoUrl.isNullOrEmpty()){
            postAdapterInterface.setAccountPhoto(holder, accountId)
        }else{
            postAdapterInterface.setAccountDefaultPhoto(holder)
        }
    }

    @JvmStatic
    @BindingAdapter("fileAttachments")
    fun bindGeneralFileButton(view: View, fileAttachments: String?) {
        view.visibility = if(fileAttachments.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    @JvmStatic
    @BindingAdapter("serializedHashTags", "postAdapterInterface")
    fun bindTagRecyclerView(
        recyclerView: RecyclerView,
        serializedHashTags: String?,
        postAdapterInterface: PostAdapterInterface
    ) {
        recyclerView.run{
            if(!serializedHashTags.isNullOrEmpty()){
                visibility = View.VISIBLE
                adapter = PostTagAdapter(ArrayList(serializedHashTags.split(',')))
                layoutManager = LinearLayoutManager(postAdapterInterface.getContextFromFragment())
                (layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL
            }else{
                visibility = View.GONE
                adapter = PostTagAdapter(arrayListOf())
            }
        }
    }
}