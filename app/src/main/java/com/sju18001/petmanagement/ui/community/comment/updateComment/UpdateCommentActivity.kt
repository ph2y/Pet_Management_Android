package com.sju18001.petmanagement.ui.community.comment.updateComment

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityUpdatecommentBinding


class UpdateCommentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdatecommentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUpdatecommentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        // 프래그먼트 실행
        if(supportFragmentManager.findFragmentById(R.id.constraintlayout_updatecomment_fragmentcontainer) == null){
            supportFragmentManager
                .beginTransaction()
                .add(R.id.constraintlayout_updatecomment_fragmentcontainer, UpdateCommentFragment())
                .commit()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }
}