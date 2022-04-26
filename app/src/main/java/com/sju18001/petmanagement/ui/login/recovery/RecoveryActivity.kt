package com.sju18001.petmanagement.ui.login.recovery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.ActivityRecoveryBinding

class RecoveryActivity: AppCompatActivity() {
    private lateinit var binding: ActivityRecoveryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewPager = binding.viewPager
        viewPager.adapter = RecoveryCollectionAdapter(this)
        TabLayoutMediator(binding.tabLayout, viewPager){ tab, position ->
            tab.text = if(position == 0) "아이디 찾기" else "비밀번호 찾기"
        }.attach()

        supportActionBar?.hide()
    }

    override fun onStart() {
        super.onStart()

        binding.backButton.setOnClickListener{ finish() }
        Util.setupViewsForHideKeyboard(this, binding.fragmentRecoveryParentLayout)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }
}