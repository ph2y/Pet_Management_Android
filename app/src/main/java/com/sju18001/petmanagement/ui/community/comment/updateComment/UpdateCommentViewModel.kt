package com.sju18001.petmanagement.ui.community.comment.updateComment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UpdateCommentViewModel: ViewModel() {
    var isApiLoading = MutableLiveData(false)
    var contents = MutableLiveData("")
}