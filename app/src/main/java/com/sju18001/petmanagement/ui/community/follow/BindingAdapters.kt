package com.sju18001.petmanagement.ui.community.follow

import android.util.Log
import android.widget.Button
import androidx.databinding.BindingAdapter
import com.sju18001.petmanagement.R

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("accountId", "followerIdList")
    fun bindFollowUnfollowButton(button: Button, accountId: Long, followerIdList: MutableList<Long>) {
        if(accountId == null || followerIdList == null) return

        val context = button.context
        if(accountId in followerIdList){
            button.setBackgroundColor(context.getColor(R.color.border_line))
            button.setTextColor(context.resources.getColor(R.color.black))
            button.text = context.getText(R.string.unfollow_button)
        }else{
            button.setBackgroundColor(context.getColor(R.color.carrot))
            button.setTextColor(context.resources.getColor(R.color.white))
            button.text = context.getText(R.string.follow_button)
        }
    }
}