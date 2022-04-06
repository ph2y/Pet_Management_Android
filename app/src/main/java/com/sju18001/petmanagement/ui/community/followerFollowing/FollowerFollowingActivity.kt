package com.sju18001.petmanagement.ui.community.followerFollowing

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityFollowBinding

class FollowerFollowingActivity : AppCompatActivity() {

    // variable for view binding
    private lateinit var binding: ActivityFollowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // view binding
        binding = ActivityFollowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        // open fragment
        if(supportFragmentManager.findFragmentById(R.id.framelayout_follow_fragmentcontainer) == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.framelayout_follow_fragmentcontainer, FollowerFollowingFragment())
                .commit()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }
}