package com.sju18001.petmanagement.controller

import android.content.Context
import com.google.gson.Gson
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.restapi.dao.Account

class SessionManager {
    companion object {
        private const val FCM = "FCM"
        private const val TOKEN = "TOKEN"
        private const val ACCOUNT = "ACCOUNT"
        private const val ALARM_MANAGER = "ALARM_MANAGER"

        fun saveFcmRegistrationToken(context:Context, token: String) {
            val prefs = context.getSharedPreferences(context.getString(R.string.user_session), Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(FCM, token)
            editor.apply()
        }

        fun fetchFcmRegistrationToken(context:Context): String? {
            val prefs = context.getSharedPreferences(context.getString(R.string.user_session), Context.MODE_PRIVATE)
            return prefs.getString(FCM, null)
        }

        fun removeFcmRegistrationToken(context:Context) {
            val prefs = context.getSharedPreferences(context.getString(R.string.user_session), Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.remove(FCM)
            editor.apply()
        }

        fun saveUserToken(context:Context, token: String) {
            val prefs = context.getSharedPreferences(context.getString(R.string.user_session), Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(TOKEN, token)
            editor.apply()
        }

        fun fetchUserToken(context:Context): String? {
            val prefs = context.getSharedPreferences(context.getString(R.string.user_session), Context.MODE_PRIVATE)
            return prefs.getString(TOKEN, null)
        }

        fun removeUserToken(context:Context) {
            val prefs = context.getSharedPreferences(context.getString(R.string.user_session), Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.remove(TOKEN)
            editor.apply()
        }

        fun saveLoggedInAccount(context: Context, account: Account) {
            val prefs = context.getSharedPreferences(context.getString(R.string.user_session), Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(ACCOUNT, Gson().toJson(account))
            editor.apply()
        }

        fun fetchLoggedInAccount(context:Context): Account? {
            val prefs = context.getSharedPreferences(context.getString(R.string.user_session), Context.MODE_PRIVATE)
            return Gson().fromJson(prefs.getString(ACCOUNT, null), Account::class.java)
        }

        fun removeLoggedInAccount(context:Context) {
            val prefs = context.getSharedPreferences(context.getString(R.string.user_session), Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.remove(ACCOUNT)
            editor.apply()
        }


        // For AlarmManager
        fun getRequestCodesOfAlarmManager(context: Context): Set<Int> {
            val res = mutableSetOf<Int>()

            val prefs = context.getSharedPreferences(context.getString(R.string.alarm_manager_session), Context.MODE_PRIVATE)
            for(requestCode in prefs.getStringSet(ALARM_MANAGER, mutableSetOf())!!){
                res.add(Integer.parseInt(requestCode))
            }

            return res
        }

        fun addRequestCodeOfAlarmManager(context: Context, requestCode: Int) {
            val prefs = context.getSharedPreferences(context.getString(R.string.alarm_manager_session), Context.MODE_PRIVATE)
            val editor = prefs.edit()

            val requestCodes = prefs.getStringSet(ALARM_MANAGER, mutableSetOf())?.apply {
                add(requestCode.toString())
            }

            editor.putStringSet(ALARM_MANAGER, requestCodes)
            editor.apply()
        }

        fun removeRequestCodeOfAlarmManager(context: Context, requestCode: Int) {
            val prefs = context.getSharedPreferences(context.getString(R.string.alarm_manager_session), Context.MODE_PRIVATE)
            val editor = prefs.edit()

            val requestCodes = prefs.getStringSet(ALARM_MANAGER, mutableSetOf())?.apply {
                remove(requestCode.toString())
            }

            editor.putStringSet(ALARM_MANAGER, requestCodes)
            editor.apply()
        }
    }
}