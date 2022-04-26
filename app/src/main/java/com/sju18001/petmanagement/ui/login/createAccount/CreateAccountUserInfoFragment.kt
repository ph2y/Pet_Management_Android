package com.sju18001.petmanagement.ui.login.createAccount

import android.os.Bundle
import android.os.SystemClock
import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.PatternRegex
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentCreateaccountuserinfoBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dto.SendAuthCodeReqDto
import com.sju18001.petmanagement.restapi.dto.SendAuthCodeResDto
import com.sju18001.petmanagement.ui.login.LoginViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateAccountUserInfoFragment : Fragment() {
    val viewModel: LoginViewModel by activityViewModels()
    
    private var _binding: FragmentCreateaccountuserinfoBinding? = null
    private val binding get() = _binding!!

    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        isViewDestroyed = false

        if(viewModel.chronometerBase.value != 0L) startChronometer()
        (parentFragment as CreateAccountFragment).showPreviousButton()

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentCreateaccountuserinfoBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@CreateAccountUserInfoFragment
        binding.viewModel = viewModel
    }

    private fun startChronometer() {
        binding.chronometerAuthcode.base = viewModel.chronometerBase.value!!
        binding.chronometerAuthcode.start()
    }


    override fun onStart() {
        super.onStart()

        setListeners()
        Util.setupViewsForHideKeyboard(requireActivity(), binding.cardviewParent)
    }

    private fun setListeners(){
        binding.edittextEmail.setOnEditorActionListener { _, _, _ ->
            Util.hideKeyboard(requireActivity())
            true
        }

        binding.edittextAuthcode.setOnEditorActionListener { _, _, _ ->
            Util.hideKeyboard(requireActivity())
            true
        }

        viewModel.createAccountPhone.observe(this, {
            viewModel.isPhoneValid.value = PatternRegex.checkPhoneRegex(it)
            viewModel.isPhoneOverlapped.value = false
            checkValidationToSetNextButton(viewModel)
        })

        viewModel.createAccountEmail.observe(this, {
            viewModel.isEmailValid.value = PatternRegex.checkEmailRegex(it)
            viewModel.isEmailOverlapped.value = false
            checkValidationToSetNextButton(viewModel)
        })

        viewModel.isEmailLocked.observe(this, {binding.chronometerAuthcode.stop()})
    }

    private fun checkValidationToSetNextButton(viewModel: LoginViewModel) {
        if(viewModel.isPhoneValid.value!! && viewModel.isEmailValid.value!!) {
            (parentFragment as CreateAccountFragment).enableNextButton()
        }else{
            (parentFragment as CreateAccountFragment).disableNextButton()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isViewDestroyed = true
    }


    /** Databinding functions */
    fun onClickRequestEmailCodeButton() {
        val sendAuthCodeReqDto = SendAuthCodeReqDto(viewModel.createAccountEmail.value!!)
        viewModel.isApiLoading.value = true

        val call = RetrofitBuilder.getServerApi().sendAuthCodeReq(sendAuthCodeReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            Toast.makeText(context, R.string.email_code_sent, Toast.LENGTH_LONG).show()

            viewModel.currentCodeRequestedEmail.value = viewModel.createAccountEmail.value
            viewModel.chronometerBase.value = SystemClock.elapsedRealtime() + 600000.toLong()
            startChronometer()

            viewModel.isApiLoading.value = false
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }
}