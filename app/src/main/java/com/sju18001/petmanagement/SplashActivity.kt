package com.sju18001.petmanagement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.ui.login.LoginActivity

class SplashActivity: AppCompatActivity() {
    private var isViewDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isViewDestroyed = false

        setTheme()
        validateTokenAndLogin()
    }

    private fun setTheme() {
        val sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val defaultThemeValue = resources.getInteger(R.integer.saved_default_theme_preference_key)
        val themeValue = sharedPref.getInt(getString(R.string.saved_theme_preference_key), defaultThemeValue)

        AppCompatDelegate.setDefaultNightMode(themeValue)
    }

    private fun validateTokenAndLogin() {
        if (SessionManager.fetchUserToken(applicationContext) != null) {
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(applicationContext)!!).fetchAccountReq(ServerUtil.getEmptyBody())
            ServerUtil.enqueueApiCall(call, { isViewDestroyed }, baseContext, { response ->
                response.body()?.run{
                    val account = Account(id, username, email, phone, null, marketing, nickname, photoUrl,
                        userMessage, representativePetId, fcmRegistrationToken, notification, mapSearchRadius)
                    SessionManager.saveLoggedInAccount(this@SplashActivity, account)

                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }, { logoutAndStartLoginActivity() }, { logoutAndStartLoginActivity() })
        }else{
            logoutAndStartLoginActivity()
        }
    }

    private fun logoutAndStartLoginActivity() {
        ServerUtil.doLogout(applicationContext)

        val intent = Intent(this@SplashActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
    }
}