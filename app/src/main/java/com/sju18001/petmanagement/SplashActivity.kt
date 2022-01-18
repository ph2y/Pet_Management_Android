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
import com.sju18001.petmanagement.restapi.dto.UpdateFcmRegistrationTokenReqDto
import com.sju18001.petmanagement.ui.login.LoginActivity

class SplashActivity: AppCompatActivity() {
    private var isViewDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isViewDestroyed = false

        // load preference values
        val sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val defaultThemeValue = resources.getInteger(R.integer.saved_default_theme_preference_key)
        val themeValue = sharedPref.getInt(getString(R.string.saved_theme_preference_key), defaultThemeValue)

        AppCompatDelegate.setDefaultNightMode(themeValue)

        // token validation check & auto login
        validateTokenAndLogin()
    }

    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
    }

    private fun validateTokenAndLogin() {
        if (SessionManager.fetchUserToken(applicationContext) != null) {
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(applicationContext)!!).fetchAccountReq(ServerUtil.getEmptyBody())
            ServerUtil.enqueueApiCall(call, { isViewDestroyed }, baseContext, { response ->
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                response.body()?.run{
                    // SessionManager 에 저장된 FcmRegistrationToken 과 비교, 다르면 FcmRegistrationToken 업데이트
                    var currentFcmRegistrationToken = SessionManager.fetchFcmRegistrationToken(applicationContext)!!
                    if(currentFcmRegistrationToken != fcmRegistrationToken) {
                        updateFcmRegistrationToken(currentFcmRegistrationToken)
                    }

                    val account = Account(id, username, email, phone, null, marketing, nickname, photoUrl, userMessage, representativePetId, currentFcmRegistrationToken, notification)
                    SessionManager.saveLoggedInAccount(this@SplashActivity, account)

                    startActivity(intent)
                    finish()
                }
            }, { startLoginActivityAndFinish() }, { startLoginActivityAndFinish() })
        }
        else { // token is empty -> LoginActivity
            startLoginActivityAndFinish()
        }
    }

    private fun startLoginActivityAndFinish() {
        val intent = Intent(this@SplashActivity, LoginActivity::class.java)

        startActivity(intent)
        finish()
    }

    private fun updateFcmRegistrationToken(fcmRegistrationToken: String) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(applicationContext)!!).updateFcmRegistrationTokenReq(
            UpdateFcmRegistrationTokenReqDto(fcmRegistrationToken)
        )
        ServerUtil.enqueueApiCall(call, { isViewDestroyed }, baseContext, {}, {}, {})
    }
}