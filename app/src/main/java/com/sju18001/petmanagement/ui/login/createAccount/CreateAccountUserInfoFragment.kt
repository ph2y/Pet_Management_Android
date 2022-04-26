package com.sju18001.petmanagement.ui.login.createAccount

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
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
import com.sju18001.petmanagement.ui.login.LoginViewModel

class CreateAccountUserInfoFragment : Fragment() {
    private val viewModel: CreateAccountViewModel by activityViewModels()
    
    private var _binding: FragmentCreateaccountuserinfoBinding? = null
    private val binding get() = _binding!!

    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)

        (activity as CreateAccountActivity).showPreviousButton()
        checkValidationToSetNextButton()

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentCreateaccountuserinfoBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@CreateAccountUserInfoFragment
        binding.viewModel = viewModel
    }

    private fun checkValidationToSetNextButton() {
        if(viewModel.isPhoneValid.value!! && viewModel.isEmailValid.value!!) {
            (activity as CreateAccountActivity).enableNextButton()
        }else{
            (activity as CreateAccountActivity).disableNextButton()
        }
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

        viewModel.phone.observe(this, {
            viewModel.isPhoneValid.value = PatternRegex.checkPhoneRegex(it)
            viewModel.isPhoneOverlapped.value = false
            checkValidationToSetNextButton()
        })

        viewModel.email.observe(this, {
            viewModel.isEmailValid.value = PatternRegex.checkEmailRegex(it)
            viewModel.isEmailOverlapped.value = false
            checkValidationToSetNextButton()
        })

        viewModel.isEmailVerified.observe(this, {binding.chronometerAuthcode.stop()})
    }

    override fun onResume() {
        super.onResume()
        if(viewModel.chronometerBase.value != 0L) startChronometer()
    }

    private fun startChronometer() {
        binding.chronometerAuthcode.base = viewModel.chronometerBase.value!!
        binding.chronometerAuthcode.start()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isViewDestroyed = true
    }


    /** Databinding functions */
    fun onClickRequestEmailCodeButton() {
        val sendAuthCodeReqDto = SendAuthCodeReqDto(viewModel.email.value!!)
        viewModel.isApiLoading.value = true

        val call = RetrofitBuilder.getServerApi().sendAuthCodeReq(sendAuthCodeReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            Toast.makeText(context, R.string.email_code_sent, Toast.LENGTH_LONG).show()

            viewModel.currentCodeRequestedEmail.value = viewModel.email.value
            viewModel.chronometerBase.value = SystemClock.elapsedRealtime() + 600000.toLong()
            startChronometer()

            viewModel.isApiLoading.value = false
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }
}