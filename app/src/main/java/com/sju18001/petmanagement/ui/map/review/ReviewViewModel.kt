package com.sju18001.petmanagement.ui.map.review

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReviewViewModel : ViewModel() {
    var placeId: MutableLiveData<Long> = MutableLiveData(-1)
    var rating: MutableLiveData<Double> = MutableLiveData(0.0)
    var reviewCount: MutableLiveData<Long> = MutableLiveData(0)

    /**
     * doInitialFetch(): fetchMyReview()에서 recycler view 최상단에 먼저 추가하기 때문에
     * updateReviewRecyclerView()에서 같은 리뷰가 중복하여 추가되지 않도록 해야합니다.
     * 따라서 첫번째 함수에서 myReview의 id를 따로 저장하고, 두번째 함수에서 중복을 체크해줍니다.
     * myReview가 없을 경우 디폴트인 -1을 갖게 되며, 있을 경우에는 해당 review의 id를 갖습니다.
     */
    var myReviewId: MutableLiveData<Long> = MutableLiveData(-1)
}