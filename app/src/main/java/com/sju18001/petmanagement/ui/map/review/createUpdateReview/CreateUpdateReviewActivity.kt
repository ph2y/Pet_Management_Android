package com.sju18001.petmanagement.ui.map.review.createUpdateReview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.databinding.ActivityCreateUpdateReviewBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dto.FetchReviewReqDto
import com.sju18001.petmanagement.restapi.dto.FetchReviewResDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateUpdateReviewActivity : AppCompatActivity() {
    companion object {
        const val CREATE_REVIEW = 0
        const val UPDATE_REVIEW = 1
    }

    private lateinit var binding: ActivityCreateUpdateReviewBinding
    private val viewModel: CreateUpdateReviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBinding()

        supportActionBar?.hide()
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_update_review)

        binding.lifecycleOwner = this
        binding.activity = this@CreateUpdateReviewActivity
        binding.viewModel = viewModel
    }

    override fun onStart() {
        super.onStart()

        initializeViewModelWithExtra()
        if(isFragmentForUpdateReviewAndIsReviewNotFetched()) {
            fetchReviewForUpdate(viewModel.reviewId)
        }
    }

    private fun initializeViewModelWithExtra() {
        viewModel.fragmentType = intent.getIntExtra("fragmentType", 0)
        viewModel.reviewId = intent.getLongExtra("reviewId", -1)
    }

    // UpdateReview를 위한 FetchReview는 최초 1회만 수행해야함에 유의해야한다.
    private fun isFragmentForUpdateReviewAndIsReviewNotFetched(): Boolean {
        return viewModel.fragmentType == UPDATE_REVIEW && !viewModel.isReviewFetched
    }

    private fun fetchReviewForUpdate(reviewId: Long) {
        viewModel.isApiCalling.set(true)

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchReviewReq(FetchReviewReqDto(reviewId, null, null))
        ServerUtil.enqueueApiCall(call, {isDestroyed}, baseContext, { response ->
            response.body()?.reviewList?.get(0)?.let {
                viewModel.contents.set(it.contents)
                viewModel.rating.set(it.rating)
            }

            viewModel.isReviewFetched = true
            viewModel.isApiCalling.set(false)
        }, {}, {})
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // TODO: 작성 취소? dialog
    }


    /**
     * Databinding functions
     */
    fun onBackButtonClicked() {
        finish()
    }

    fun onStarImageClicked(index: Int) {
        viewModel.rating.set(index)
    }
}