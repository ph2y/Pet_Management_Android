package com.sju18001.petmanagement.ui.map.review.createUpdateReview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_update_review)

        binding.lifecycleOwner = this
        binding.viewModel = model

        supportActionBar?.hide()
    }

    override fun onStart() {
        super.onStart()

        model.fragmentType = intent.getIntExtra("fragmentType", 0)
    }
}