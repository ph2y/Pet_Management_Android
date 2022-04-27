package com.sju18001.petmanagement.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.sju18001.petmanagement.MainActivity
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBinding()

        val viewPager = binding.viewpager
        viewPager.adapter = WelcomeCollectionAdapter(this)
        TabLayoutMediator(binding.tablayout, viewPager){ tab, _ ->
            tab.view.isClickable = false
        }.attach()
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_welcome)
        binding.lifecycleOwner = this
        binding.activity = this@WelcomeActivity
    }


    /** Databinding functions */
    fun onClickSkipButton() {
        startActivity(Intent(baseContext, MainActivity::class.java))
        finish()
    }
}