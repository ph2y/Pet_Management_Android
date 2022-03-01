package com.sju18001.petmanagement.ui.map.review

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel

class ReviewViewModel : ViewModel() {
    var placeId: ObservableField<Long> = ObservableField(-1)
    var rating: ObservableField<Double> = ObservableField(0.0)
    var reviewCount: ObservableField<Long> = ObservableField(0)
}