package com.sju18001.petmanagement.ui.login.createAccount

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.ActivityCreateaccountBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.ui.login.LoginViewModel
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateAccountActivity : AppCompatActivity() {
    companion object{
        // 하위 프래그먼트 태그
        private const val FRAGMENT_TAG_TERMS: String = "terms"
        private const val FRAGMENT_TAG_CREDENTIALS: String = "username_password"
        private const val FRAGMENT_TAG_USER_INFO: String = "user_info"

        // CreateAccount Error Messages
        private const val MESSAGE_USERNAME_OVERLAP: String = "Username already exists"
        private const val MESSAGE_PHONE_OVERLAP: String = "Phone number already exists"
        private const val MESSAGE_EMAIL_OVERLAP: String = "Email already exists"
    }

    private lateinit var binding: ActivityCreateaccountBinding

    private val viewModel: CreateAccountViewModel by viewModels()
    private var isViewDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 프래그먼트가 비었다는 건 LoginActivity에서 넘어왔다는 것이고, 이땐 첫번째 프래그먼트를 실행합니다.
        if(supportFragmentManager.findFragmentById(R.id.framelayout_contents) == null) {
            val createAccountTermsFragment = CreateAccountTermsFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.framelayout_contents, createAccountTermsFragment, FRAGMENT_TAG_TERMS)
                .commit()
        }

        setBinding()
        supportActionBar?.hide()
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_createaccount)

        binding.lifecycleOwner = this
        binding.activity = this@CreateAccountActivity
        binding.viewModel = viewModel
    }

    override fun onStart() {
        super.onStart()
        Util.setupViewsForHideKeyboard(this, binding.constraintlayoutParent)
    }

    override fun onBackPressed() {
        showAlertDialogToFinish()
    }

    private fun showAlertDialogToFinish() {
        AlertDialog.Builder(this)
            .setMessage(R.string.return_to_login_dialog)
            .setPositiveButton(R.string.confirm){ _, _-> finish() }
            .setNegativeButton(R.string.cancel){ _, _-> }
            .create().show()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
    }

    
    /** 하위 프래그먼트에서 접근하는 공개 함수 */
    fun showPreviousButton() {
        binding.buttonPrevious.visibility = View.VISIBLE
    }

    fun hidePreviousButton() {
        binding.buttonPrevious.visibility = View.INVISIBLE
    }

    fun enableNextButton() {
        binding.buttonNext.isEnabled = true
        binding.buttonNext.text =
            if(supportFragmentManager.fragments[0].tag == FRAGMENT_TAG_USER_INFO) getText(R.string.create_account_button)
            else getText(R.string.next_step_button)
    }

    fun disableNextButton() {
        binding.buttonNext.isEnabled = false
        binding.buttonNext.text =
            if(supportFragmentManager.fragments[0].tag == FRAGMENT_TAG_USER_INFO) getText(R.string.create_account_button)
            else getText(R.string.next_step_button)
    }


    /** Databinding funtions */
    fun onClickBackButton() {
        showAlertDialogToFinish()
    }

    fun onClickPreviousButton() {
        supportFragmentManager.popBackStack()
    }

    fun onClickNextButton() {
        when(supportFragmentManager.fragments[0].tag) {
            FRAGMENT_TAG_TERMS -> {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                    .replace(R.id.framelayout_contents, CreateAccountCredentialsFragment(), FRAGMENT_TAG_CREDENTIALS)
                    .addToBackStack(null)
                    .commit()
            }
            FRAGMENT_TAG_CREDENTIALS -> {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                    .replace(R.id.framelayout_contents, CreateAccountUserInfoFragment(), FRAGMENT_TAG_USER_INFO)
                    .addToBackStack(null)
                    .commit()
            }
            // 회원가입 버튼
            FRAGMENT_TAG_USER_INFO -> {
                if(viewModel.email.value == viewModel.currentCodeRequestedEmail.value){
                    verifyAuthCode()
                }else{
                    Toast.makeText(baseContext, getString(R.string.email_message_request), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun verifyAuthCode() {
        if(viewModel.isEmailVerified.value == true) {
            createAccount()
        }else{
            setIsApiLoadingForUserInfo(true)
            val body = VerifyAuthCodeReqDto(viewModel.currentCodeRequestedEmail.value!!, viewModel.emailCode.value!!)
            val verifyAuthCodeCall = RetrofitBuilder.getServerApi().verifyAuthCodeReq(body)

            verifyAuthCodeCall.enqueue(object: Callback<VerifyAuthCodeResDto> {
                override fun onResponse(
                    call: Call<VerifyAuthCodeResDto>,
                    response: Response<VerifyAuthCodeResDto>
                ) {
                    if(isViewDestroyed) return

                    if(response.isSuccessful) {
                        viewModel.isEmailVerified.value = true
                        createAccount()
                    }else{
                        setIsApiLoadingForUserInfo(false)
                        Util.showToastAndLog(baseContext, getString(R.string.email_message_code_invalid))
                    }
                }

                override fun onFailure(call: Call<VerifyAuthCodeResDto>, t: Throwable) {
                    if(isViewDestroyed) return

                    setIsApiLoadingForUserInfo(false)
                    Util.showToastAndLog(baseContext, t.message.toString())
                }
            })
        }
    }

    private fun setIsApiLoadingForUserInfo(flag: Boolean) {
        viewModel.isApiLoading.value = flag

        binding.buttonPrevious.isEnabled = !flag
        binding.buttonNext.text = if(flag) "" else getText(R.string.create_account_button)
        binding.buttonNext.isEnabled = !flag
    }

    private fun createAccount() {
        setIsApiLoadingForUserInfo(true)
        val body = CreateAccountReqDto(viewModel.username.value!!, viewModel.password.value!!,
            viewModel.email.value!!, viewModel.phone.value!!,
            getString(R.string.default_nickname), viewModel.isMarketingChecked.value!!, null, true)
        val createAccountCall = RetrofitBuilder.getServerApi().createAccountReq(body)

        createAccountCall.enqueue(object: Callback<CreateAccountResDto> {
            override fun onResponse(
                call: Call<CreateAccountResDto>,
                response: Response<CreateAccountResDto>
            ) {
                if(isViewDestroyed) return

                if(response.isSuccessful){
                    setResult(Activity.RESULT_OK)
                    finish()
                }else{
                    when (Util.getMessageFromErrorBody(response.errorBody()!!)) {
                        MESSAGE_USERNAME_OVERLAP -> {
                            viewModel.isUsernameOverlapped.value = true
                            supportFragmentManager.popBackStack()
                        }
                        MESSAGE_EMAIL_OVERLAP -> {
                            viewModel.isEmailOverlapped.value = true
                            viewModel.isEmailVerified.value = false
                            viewModel.chronometerBase.value = 0
                            viewModel.currentCodeRequestedEmail.value = ""
                        }
                        MESSAGE_PHONE_OVERLAP -> {
                            viewModel.isPhoneOverlapped.value = true
                        }
                        else -> {
                            viewModel.isEmailVerified.value = false
                            Toast.makeText(baseContext, getText(R.string.fail_request), Toast.LENGTH_SHORT).show()
                        }
                    }

                    setIsApiLoadingForUserInfo(false)
                }
            }

            override fun onFailure(call: Call<CreateAccountResDto>, t: Throwable) {
                if(isViewDestroyed) return

                setIsApiLoadingForUserInfo(false)
                Util.showToastAndLog(baseContext, t.message.toString())
            }
        })
    }
}
