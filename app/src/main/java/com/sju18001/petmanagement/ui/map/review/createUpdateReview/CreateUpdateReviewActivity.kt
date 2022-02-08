package com.sju18001.petmanagement.ui.map.review.createUpdateReview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityCreateUpdateReviewBinding

class CreateUpdateReviewActivity : AppCompatActivity() {
    companion object {
        const val CREATE_REVIEW = 0
        const val UPDATE_REVIEW = 1
    }

    private lateinit var binding: ActivityCreateUpdateReviewBinding
    private val model: CreateUpdateReviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_update_review)
        setBinding()

        supportActionBar?.hide()
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_update_review)

        binding.lifecycleOwner = this
        binding.activity = this@CreateUpdateReviewActivity
        binding.viewModel = model
    }

    override fun onStart() {
        super.onStart()

        initializeViewModelWithExtra()
    }

    private fun initializeViewModelWithExtra() {
        model.fragmentType = intent.getIntExtra("fragmentType", 0)
        // TODO: review data for update
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }


    /**
     * xml의 onClick에서 처리하는 영역입니다. 따라서 따로 리스너를 달지 않아도 됩니다.
     */
    fun onBackButtonClicked() {
        finish()
    }
}