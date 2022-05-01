package com.sju18001.petmanagement.ui.setting.detailedSetting

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityDetailedsettingBinding
import com.sju18001.petmanagement.ui.setting.detailedSetting.account.UpdateAccountFragment
import com.sju18001.petmanagement.ui.setting.detailedSetting.information.LicenseFragment
import com.sju18001.petmanagement.ui.setting.detailedSetting.information.PrivacyTermsFragment
import com.sju18001.petmanagement.ui.setting.detailedSetting.information.UsageTermsFragment
import com.sju18001.petmanagement.ui.setting.detailedSetting.preferences.NotificationPreferenceFragment
import com.sju18001.petmanagement.ui.setting.detailedSetting.preferences.RadiusPreferenceFragment
import com.sju18001.petmanagement.ui.setting.detailedSetting.preferences.ThemePreferenceFragment

class DetailedSettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailedsettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // view binding
        binding = ActivityDetailedsettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionBar: ActionBar? = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        // get fragment type and show it(for first launch)
        val fragmentType = intent.getStringExtra("fragmentType")

        if(supportFragmentManager.findFragmentById(R.id.constraintlayout_parent) == null) {
            val fragment = when(fragmentType){
                "update_account" -> {
                    supportActionBar?.hide()
                    UpdateAccountFragment()
                }
                "radius_preference" -> {
                    actionBar?.setTitle(R.string.radius_preferences)
                    RadiusPreferenceFragment()
                }
                "notification_preference" -> {
                    actionBar?.setTitle(R.string.notification_preferences)
                    NotificationPreferenceFragment()
                }
                "theme_preference" -> {
                    actionBar?.setTitle(R.string.theme_preferences)
                    ThemePreferenceFragment()
                }
                "privacy_terms" -> {
                    actionBar?.setTitle(R.string.privacy_terms_title)
                    PrivacyTermsFragment()
                }
                "usage_terms" -> {
                    actionBar?.setTitle(R.string.usage_terms_title)
                    UsageTermsFragment()
                }
                "license" -> {
                    actionBar?.setTitle(R.string.license_title)
                    LicenseFragment()
                }
                else -> LicenseFragment()
            }
            supportFragmentManager
                .beginTransaction()
                .add(R.id.constraintlayout_parent, fragment)
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }
}