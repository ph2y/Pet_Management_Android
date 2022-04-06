package com.sju18001.petmanagement.ui.welcome

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        if(supportFragmentManager.findFragmentById(R.id.framelayout_welcome_fragmentcontainer) == null){
            val fragment = WelcomeFragment()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.framelayout_welcome_fragmentcontainer, fragment)
                .commit()
        }
    }
}