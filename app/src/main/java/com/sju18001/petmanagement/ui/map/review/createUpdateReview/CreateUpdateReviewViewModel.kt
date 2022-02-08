package com.sju18001.petmanagement.ui.map.review.createUpdateReview

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel

class CreateUpdateReviewViewModel: ViewModel() {
    var fragmentType = 0
    var rating: ObservableField<Int> = ObservableField(0)
}