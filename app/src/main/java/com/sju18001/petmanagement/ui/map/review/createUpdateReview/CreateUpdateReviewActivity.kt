package com.sju18001.petmanagement.ui.map.review.createUpdateReview

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.google.android.gms.ads.AdRequest
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.ActivityCreateupdatereviewBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dto.CreateReviewReqDto
import com.sju18001.petmanagement.restapi.dto.FetchReviewReqDto
import com.sju18001.petmanagement.restapi.dto.UpdateReviewReqDto

class CreateUpdateReviewActivity : AppCompatActivity() {
    companion object {
        const val CREATE_REVIEW = 0
        const val UPDATE_REVIEW = 1
    }

    private lateinit var binding: ActivityCreateupdatereviewBinding
    private val viewModel: CreateUpdateReviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBinding()

        supportActionBar?.hide()

        binding.adView.loadAd(AdRequest.Builder().build())
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_createupdatereview)

        binding.lifecycleOwner = this
        binding.activity = this@CreateUpdateReviewActivity
        binding.viewModel = viewModel
        binding.util = Util.Companion
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
        viewModel.placeId = intent.getLongExtra("placeId", -1)
        viewModel.reviewId = intent.getLongExtra("reviewId", -1)
    }

    // UpdateReview를 위한 FetchReview는 최초 1회만 수행해야함에 유의해야한다.
    private fun isFragmentForUpdateReviewAndIsReviewNotFetched(): Boolean {
        return viewModel.fragmentType == UPDATE_REVIEW && !viewModel.isReviewFetched
    }

    private fun fetchReviewForUpdate(reviewId: Long) {
        viewModel.isApiLoading.set(true)

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchReviewReq(FetchReviewReqDto(reviewId, null, null, null, null))
        ServerUtil.enqueueApiCall(call, {isDestroyed}, baseContext, { response ->
            response.body()?.reviewList?.get(0)?.let {
                viewModel.contents.set(it.contents)
                viewModel.rating.set(it.rating)
            }

            viewModel.isReviewFetched = true
            viewModel.isApiLoading.set(false)
        }, { finish() }, { finish() })
    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(this.getString(R.string.cancel_review_dialog_message))
            .setPositiveButton(R.string.confirm) { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 정상적으로 종료될 때만 RESULT_OK를 갖게 된다.
        if(resultCode != Activity.RESULT_OK) {
            Toast.makeText(baseContext, getText(R.string.default_error_message), Toast.LENGTH_SHORT).show()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }


    /**
     * Databinding functions
     */
    fun onClickBackButton() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(this.getString(R.string.cancel_review_dialog_message))
            .setPositiveButton(R.string.confirm) { _, _ ->
                finish()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }

    fun onClickStarImage(index: Int) {
        viewModel.rating.set(index + 1)
    }

    fun onClickConfirmButton() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(this.getString(R.string.post_review_dialog_message))
            .setPositiveButton(R.string.confirm) { _, _ ->
                when(viewModel.fragmentType){
                    CREATE_REVIEW -> {
                        createReviewAndFinishActivity()
                    }
                    UPDATE_REVIEW -> {
                        updateReviewAndFinishActivity()
                    }
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }

    private fun createReviewAndFinishActivity() {
        if(viewModel.rating.get() == 0 || viewModel.contents.get().toString().isNullOrEmpty()) return

        viewModel.isApiLoading.set(true)

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .createReviewReq(CreateReviewReqDto(viewModel.placeId, viewModel.rating.get()!!, viewModel.contents.get()!!))
        ServerUtil.enqueueApiCall(call, {isDestroyed}, baseContext, {
            Toast.makeText(baseContext, getText(R.string.create_review_successful), Toast.LENGTH_LONG).show()

            intent.putExtra("reviewId", it.body()!!.id)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }, { viewModel.isApiLoading.set(false) }, { viewModel.isApiLoading.set(false) })
    }

    private fun updateReviewAndFinishActivity() {
        if(viewModel.rating.get() == 0 || viewModel.contents.get().toString().isNullOrEmpty()) return

        viewModel.isApiLoading.set(true)

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .updateReviewReq(UpdateReviewReqDto(viewModel.reviewId, viewModel.rating.get()!!, viewModel.contents.get()!!))
        ServerUtil.enqueueApiCall(call, {isDestroyed}, baseContext, {
            Toast.makeText(baseContext, getText(R.string.update_review_successful), Toast.LENGTH_LONG).show()

            setResult(Activity.RESULT_OK, intent)
            finish()
        }, { viewModel.isApiLoading.set(false) }, { viewModel.isApiLoading.set(false) })
    }
}