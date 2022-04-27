package com.sju18001.petmanagement.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.sju18001.petmanagement.MainActivity
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.databinding.ActivityLoginBinding
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.ui.login.createAccount.CreateAccountActivity
import com.sju18001.petmanagement.ui.login.recover.RecoverActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private var isViewDestroyed = false
    private val viewModel: LoginViewModel by viewModels()

    private lateinit var binding: ActivityLoginBinding

    private var snackBar: Snackbar? = null

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            displaySuccessMessage(getText(R.string.create_account_success)!!.toString())
        }
    }

    private fun displaySuccessMessage(message: String) {
        snackBar = Snackbar.make(findViewById(R.id.constraintlayout_parent)!!,
            message, Snackbar.LENGTH_SHORT)
        val snackBarView = snackBar!!.view
        snackBarView.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
        snackBarView.findViewById<TextView>(R.id.snackbar_text).textAlignment = View.TEXT_ALIGNMENT_CENTER
        snackBar!!.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBinding()
        isViewDestroyed = false

        supportActionBar?.hide()
    }


    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        binding.lifecycleOwner = this
        binding.fragment = this@LoginActivity
        binding.viewModel = viewModel
    }

    override fun onStart() {
        super.onStart()

        binding.edittextPassword.setOnEditorActionListener { _, _, _ ->
            Util.hideKeyboard(this)
            login()
            true
        }

        Util.setupViewsForHideKeyboard(this, binding.constraintlayoutParent)
    }

    fun login() {
        viewModel.isApiLoading.value = true
        val call = RetrofitBuilder.getServerApi()
            .loginReq(LoginReqDto(viewModel.username.value!!, viewModel.password.value!!))
        call.enqueue(object: Callback<LoginResDto> {
            override fun onResponse(
                call: Call<LoginResDto>,
                response: Response<LoginResDto>
            ) {
                if(isViewDestroyed) return

                if(response.isSuccessful){
                    checkIsFirstLoginAndStartActivity(response.body()!!.token!!)
                }else{
                    displayErrorMessage(getText(R.string.login_failed)!!.toString())
                    viewModel.isApiLoading.value = false
                }
            }

            override fun onFailure(call: Call<LoginResDto>, t: Throwable) {
                if(isViewDestroyed) return

                displayErrorMessage(getText(R.string.default_error_message)!!.toString())

                Util.log(baseContext, t.message.toString())
                Log.d("error", t.message.toString())

                viewModel.isApiLoading.value = false
            }
        })
    }

    private fun displayErrorMessage(message: String) {
        snackBar = Snackbar.make(findViewById(R.id.constraintlayout_parent)!!,
            message, Snackbar.LENGTH_SHORT)
        val snackBarView = snackBar!!.view
        snackBarView.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
        snackBarView.findViewById<TextView>(R.id.snackbar_text).textAlignment = View.TEXT_ALIGNMENT_CENTER
        snackBar!!.show()
    }

    private fun checkIsFirstLoginAndStartActivity(token: String){
        val fetchAccountCall = RetrofitBuilder.getServerApiWithToken(token).fetchAccountReq(ServerUtil.getEmptyBody())
        fetchAccountCall.enqueue(object: Callback<FetchAccountResDto> {
            override fun onResponse(
                call: Call<FetchAccountResDto>,
                response: Response<FetchAccountResDto>
            ) {
                if(isViewDestroyed) return

                response.body()?.let{
                    if(response.isSuccessful){
                        SessionManager.saveUserToken(baseContext, token)

                        val account = Account(
                            it.id, it.username, it.email, it.phone, null, it.marketing,
                            it.nickname, it.photoUrl, it.userMessage, it.representativePetId,
                            it.fcmRegistrationToken, it.notification, it.mapSearchRadius)
                        if(isFirstLogin(it.nickname!!)){
                            updateAccountWithDefaultNickname(token, it)
                            SessionManager.saveLoggedInAccount(baseContext, account.apply { nickname = username })
                            startWelcomeActivity()
                        }else{
                            SessionManager.saveLoggedInAccount(baseContext, account)
                            startMainActivity()
                        }
                    }else{
                        Toast.makeText(baseContext, it._metadata.toString(), Toast.LENGTH_LONG).show()
                        viewModel.isApiLoading.value = false
                    }
                }
            }

            override fun onFailure(call: Call<FetchAccountResDto>, t: Throwable) {
                if(isViewDestroyed) return

                displayErrorMessage(t.message.toString())
                Log.d("error", t.message.toString())

                viewModel.isApiLoading.value = false
            }
        })
    }

    private fun isFirstLogin(nickname: String): Boolean {
        return nickname == getString(R.string.default_nickname)
    }

    private fun updateAccountWithDefaultNickname(token: String, body: FetchAccountResDto) {
        // nickname을 디폴트(#)에서 username과 동일하게 변경(ex. "#" -> "abc123")
        val updateAccountCall = RetrofitBuilder.getServerApiWithToken(token)
            .updateAccountReq(UpdateAccountReqDto(body.email, body.phone, body.username, body.marketing,
                body.userMessage, body.representativePetId, body.notification, body.mapSearchRadius))
        ServerUtil.enqueueApiCall(updateAccountCall, {isViewDestroyed}, baseContext, {}, {}, {})
    }

    private fun startWelcomeActivity() {
        startActivity(Intent(baseContext, WelcomeActivity::class.java))
        finish()
    }

    private fun startMainActivity() {
        startActivity(Intent(baseContext, MainActivity::class.java))
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
        snackBar?.dismiss()
    }


    /** Databinding functions */
    fun onClickCreateAccountButton() {
        startForResult.launch(Intent(baseContext, CreateAccountActivity::class.java))
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    fun onClickRecoveryButton() {
        startActivity(Intent(baseContext, RecoverActivity::class.java))
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }
}