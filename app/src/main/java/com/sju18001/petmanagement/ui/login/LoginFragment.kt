package com.sju18001.petmanagement.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.sju18001.petmanagement.MainActivity
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentLoginBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.ui.login.createAccount.CreateAccountActivity
import com.sju18001.petmanagement.ui.login.recover.RecoverActivity
import com.sju18001.petmanagement.ui.welcome.WelcomeActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginFragment : Fragment() {
    private var isViewDestroyed = false
    private val viewModel: LoginViewModel by activityViewModels()

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private var snackBar: Snackbar? = null

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            displaySuccessMessage(context?.getText(R.string.create_account_success)!!.toString())
        }
    }

    private fun displaySuccessMessage(message: String) {
        snackBar = Snackbar.make(view?.findViewById(R.id.constraintlayout_parent)!!,
            message, Snackbar.LENGTH_SHORT)
        val snackBarView = snackBar!!.view
        snackBarView.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
        snackBarView.findViewById<TextView>(R.id.snackbar_text).textAlignment = View.TEXT_ALIGNMENT_CENTER
        snackBar!!.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        isViewDestroyed = false

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@LoginFragment
        binding.viewModel = viewModel
    }

    override fun onStart() {
        super.onStart()

        binding.edittextPassword.setOnEditorActionListener { _, _, _ ->
            Util.hideKeyboard(requireActivity())
            login()
            true
        }

        Util.setupViewsForHideKeyboard(requireActivity(), binding.constraintlayoutParent)
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
                    displayErrorMessage(context?.getText(R.string.login_failed)!!.toString())
                    viewModel.isApiLoading.value = false
                }
            }

            override fun onFailure(call: Call<LoginResDto>, t: Throwable) {
                if(isViewDestroyed) return

                displayErrorMessage(context?.getText(R.string.default_error_message)!!.toString())

                Util.log(context!!, t.message.toString())
                Log.d("error", t.message.toString())

                viewModel.isApiLoading.value = false
            }
        })
    }

    private fun displayErrorMessage(message: String) {
        snackBar = Snackbar.make(view?.findViewById(R.id.constraintlayout_parent)!!,
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
                        SessionManager.saveUserToken(requireContext(), token)

                        if(isFirstLogin(it.nickname!!)){
                            updateAccountWithDefaultNickname(token, it)
                            startWelcomeActivity(it)
                        }else{
                            startMainActivity(it)
                        }
                    }else{
                        Toast.makeText(context, it._metadata.toString(), Toast.LENGTH_LONG).show()
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
        // nickname을 username과 같게 변경(ex. "#" -> "abc123")
        val updateAccountCall = RetrofitBuilder.getServerApiWithToken(token)
            .updateAccountReq(UpdateAccountReqDto(body.email, body.phone, body.username, body.marketing,
                body.userMessage, body.representativePetId, body.notification, body.mapSearchRadius))
        ServerUtil.enqueueApiCall(updateAccountCall, {isViewDestroyed}, requireContext(), {}, {}, {})
    }

    private fun startWelcomeActivity(body: FetchAccountResDto) {
        val intent = Intent(context, WelcomeActivity::class.java)
        body.run{
            // nickname에 username을 넣은 것에 유의할 것
            val account = Account(id, username, email, phone, null, marketing, username, photoUrl,
                userMessage, representativePetId, fcmRegistrationToken, notification, mapSearchRadius)
            SessionManager.saveLoggedInAccount(requireContext(), account)
        }

        startActivity(intent)
        activity?.finish()
    }

    private fun startMainActivity(body: FetchAccountResDto) {
        val intent = Intent(context, MainActivity::class.java)
        body.run{
            val account = Account(id, username, email, phone, null, marketing, nickname, photoUrl,
                userMessage, representativePetId, fcmRegistrationToken, notification, mapSearchRadius)
            SessionManager.saveLoggedInAccount(requireContext(), account)
        }

        startActivity(intent)
        activity?.finish()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        isViewDestroyed = true
        snackBar?.dismiss()
    }


    /** Databinding functions */
    fun onClickCreateAccountButton() {
        val intent = Intent(context, CreateAccountActivity::class.java)
        startForResult.launch(intent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    fun onClickRecoveryButton() {
        val intent = Intent(context, RecoverActivity::class.java)
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }
}