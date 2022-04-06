package com.sju18001.petmanagement.ui.map.review

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityReviewBinding

class ReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        // 프래그먼트 실행
        if(supportFragmentManager.findFragmentById(R.id.constraintlayout_review_fragmentcontainer) == null){
            supportFragmentManager
                .beginTransaction()
                .add(R.id.constraintlayout_review_fragmentcontainer, ReviewFragment())
                .commit()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_top, R.anim.exit_to_bottom)
    }
}