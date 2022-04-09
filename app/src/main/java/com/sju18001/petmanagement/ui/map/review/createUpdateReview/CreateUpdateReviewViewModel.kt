package com.sju18001.petmanagement.ui.map.review.createUpdateReview

import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel

class CreateUpdateReviewViewModel: ViewModel() {
    var fragmentType: Int = 0
    var placeId: Long = -1
    var reviewId: Long = -1 // update review일 때 필요합니다.

    var isReviewFetched: Boolean = false

    // UPDATE_REVIEW일 때, FetchReview 이후에 아래 값들을 불러옵니다.
    var rating: ObservableField<Int> = ObservableField(0)
    var contents: ObservableField<String> = ObservableField("")

    var isApiLoading: ObservableField<Boolean> = ObservableField(false)
}