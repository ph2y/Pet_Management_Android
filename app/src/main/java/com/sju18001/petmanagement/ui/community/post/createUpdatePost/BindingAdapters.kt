package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.view.View
import androidx.databinding.BindingAdapter

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
}