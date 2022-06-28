package com.sju18001.petmanagement.ui.community.comment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sju18001.petmanagement.restapi.dao.Account

class CommentViewModel: ViewModel() {
    var isApiLoading = MutableLiveData(false)

    var replyId = MutableLiveData<Long?>(null)
    var replyNickname = MutableLiveData("")

    var postId: Long = -1
    var loggedInAccount: Account? = null

    var isLast = false
    var topCommentId: Long? = null
    var pageIndex: Int = 1

    var contents = MutableLiveData("")
}