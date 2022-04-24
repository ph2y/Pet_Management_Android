package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.content.Context
import android.view.View
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.sju18001.petmanagement.R
import de.hdodenhof.circleimageview.CircleImageView

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("contents", "selectedPetId", "isApiLoading")
    fun bindPostButton(
        view: View,
        contents: String,
        selectedPetId: Long,
        isApiLoading: Boolean
    ) {
        view.isEnabled = contents.trim().isNotEmpty() && selectedPetId != null && !isApiLoading
    }

    @JvmStatic
    @BindingAdapter("petSelectorItem", "context")
    fun bindPetPhoto(
        petPhotoCircleImageView: CircleImageView,
        petSelectorItem: CreateUpdatePostPetSelectorItem,
        context: Context
    ) {
        if(petSelectorItem.petPhotoUrl != null){
            Glide.with(context).load(petSelectorItem.petPhoto).into(petPhotoCircleImageView)
        }else{
            petPhotoCircleImageView.setImageResource(R.drawable.ic_baseline_pets_60_with_padding)
        }
    }
}