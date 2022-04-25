package com.sju18001.petmanagement.ui.login

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        if(supportFragmentManager.findFragmentById(R.id.framelayout_login_fragmentcontainer) == null) {
            val fragment = LoginFragment()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.framelayout_login_fragmentcontainer, fragment)
                .commit()
        }
    }
}