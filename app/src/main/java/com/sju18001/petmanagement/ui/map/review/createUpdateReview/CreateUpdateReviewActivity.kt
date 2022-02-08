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
     * Databinding functions
     */
    fun onBackButtonClicked() {
        finish()
    }

    fun onStarImageClicked(index: Int) {
        model.rating.set(index)
    }
}