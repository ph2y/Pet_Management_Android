package com.sju18001.petmanagement.ui.login.createAccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.sju18001.petmanagement.controller.PatternRegex
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentCreateaccountcredentialsBinding
import com.sju18001.petmanagement.ui.login.LoginViewModel

class CreateAccountCredentialsFragment : Fragment() {
    private var _binding: FragmentCreateaccountcredentialsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateAccountViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)

        Util.setupViewsForHideKeyboard(requireActivity(), binding.cardviewParent)
        (activity as CreateAccountActivity).showPreviousButton()
        checkValidationToSetNextButton()

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentCreateaccountcredentialsBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@CreateAccountCredentialsFragment
        binding.viewModel = viewModel
    }

    private fun checkValidationToSetNextButton() {
        if(viewModel.isUsernameValid.value!! && viewModel.isPasswordValid.value!! && viewModel.isPasswordCheckValid.value!!){
            (activity as CreateAccountActivity).enableNextButton()
        }else{
            (activity as CreateAccountActivity).disableNextButton()
        }
    }


    override fun onStart() {
        super.onStart()
        setListeners()
    }

    private fun setListeners() {
        binding.edittextPassword.setOnEditorActionListener { _, _, _ ->
            Util.hideKeyboard(requireActivity())
            true
        }

        binding.edittextUsername.addTextChangedListener {
            viewModel.isUsernameValid.value = PatternRegex.checkUsernameRegex(it)
            viewModel.isUsernameOverlapped.value = false
        }

        viewModel.password.observe(this, {
            viewModel.isPasswordValid.value = PatternRegex.checkPasswordRegex(it)
            viewModel.isPasswordCheckValid.value = it == viewModel.passwordCheck.value
        })

        viewModel.passwordCheck.observe(this, {
            viewModel.isPasswordCheckValid.value = it == viewModel.password.value
        })

        viewModel.isUsernameValid.observe(this, {checkValidationToSetNextButton()})
        viewModel.isPasswordValid.observe(this, {checkValidationToSetNextButton()})
        viewModel.isPasswordCheckValid.observe(this, {checkValidationToSetNextButton()})
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}