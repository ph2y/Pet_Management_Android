package com.sju18001.petmanagement.ui.setting.detailedSetting.account

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.PatternRegex
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.LayoutChangepasswordBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dto.UpdateAccountPasswordReqDto
import com.sju18001.petmanagement.ui.setting.detailedSetting.DetailedSettingViewModel

class ChangePasswordDialog(
    private val activity: Activity,
    private val updateAccountPassword: (newPassword: String, password: String) -> Unit
): Dialog(activity) {
    private lateinit var binding: LayoutChangepasswordBinding
    
    private var isNewPasswordValid = false
    private var isNewPasswordCheckValid = false
    private var isPasswordValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = LayoutChangepasswordBinding.inflate(LayoutInflater.from(context))
        setContentView(R.layout.layout_changepassword)

        Util.setupViewsForHideKeyboard(activity, findViewById(R.id.linearlayout_changepassword))
    }

    override fun onStart() {
        super.onStart()

        findViewById<EditText>(R.id.edittext_changepassword_newpassword).addTextChangedListener(object:
            TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(PatternRegex.checkPasswordRegex(s)){
                    isNewPasswordValid = true
                    findViewById<TextView>(R.id.textview_changepassword_invalidnewpassword).visibility = View.GONE
                }else{
                    isNewPasswordValid = false
                    findViewById<TextView>(R.id.textview_changepassword_invalidnewpassword).visibility = View.VISIBLE
                }
                if(s.toString() == findViewById<EditText>(R.id.edittext_changepassword_reconfirm).text.toString()) {
                    isNewPasswordCheckValid = true
                    findViewById<TextView>(R.id.textview_changepassword_invalidreconfirm).visibility = View.GONE
                }else{
                    isNewPasswordCheckValid = false
                    findViewById<TextView>(R.id.textview_changepassword_invalidreconfirm).visibility = View.VISIBLE
                }

                findViewById<Button>(R.id.button_changepassword_confirm).isEnabled =
                    isNewPasswordValid && isNewPasswordCheckValid && isPasswordValid
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<EditText>(R.id.edittext_changepassword_reconfirm).addTextChangedListener(object:
            TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s.toString() == findViewById<EditText>(R.id.edittext_changepassword_newpassword).text.toString()){
                    isNewPasswordCheckValid = true
                    findViewById<TextView>(R.id.textview_changepassword_invalidreconfirm).visibility = View.GONE
                }else{
                    isNewPasswordCheckValid = false
                    findViewById<TextView>(R.id.textview_changepassword_invalidreconfirm).visibility = View.VISIBLE
                }

                findViewById<Button>(R.id.button_changepassword_confirm).isEnabled =
                    isNewPasswordValid && isNewPasswordCheckValid && isPasswordValid
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<EditText>(R.id.edittext_changepassword_oldpassword).addTextChangedListener(object:
            TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(PatternRegex.checkPasswordRegex(s)){
                    isPasswordValid = true
                    findViewById<TextView>(R.id.textview_changepassword_invalidoldpassword).visibility = View.GONE
                }else{
                    isPasswordValid = false
                    findViewById<TextView>(R.id.textview_changepassword_invalidoldpassword).visibility = View.VISIBLE
                }

                findViewById<Button>(R.id.button_changepassword_confirm).isEnabled =
                    isNewPasswordValid && isNewPasswordCheckValid && isPasswordValid
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<Button>(R.id.button_changepassword_confirm).setOnClickListener {
            val newPassword = findViewById<EditText>(R.id.edittext_changepassword_newpassword).text.toString()
            val password = findViewById<EditText>(R.id.edittext_changepassword_oldpassword).text.toString()

            updateAccountPassword.invoke(newPassword, password)
            dismiss()
        }

        findViewById<Button>(R.id.button_changepassword_cancel).setOnClickListener {
            dismiss()
        }
    }
}