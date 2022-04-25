package com.sju18001.petmanagement.ui.login.createAccount

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.sju18001.petmanagement.controller.PatternRegex
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentCreateaccountcredentialsBinding
import com.sju18001.petmanagement.ui.login.LoginViewModel

class CreateAccountCredentialsFragment : Fragment() {
    private var _binding: FragmentCreateaccountcredentialsBinding? = null
    private val binding get() = _binding!!

    val viewModel: LoginViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)

        Util.setupViewsForHideKeyboard(requireActivity(), binding.cardviewParent)
        (parentFragment as CreateAccountFragment).showPreviousButton()

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentCreateaccountcredentialsBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@CreateAccountCredentialsFragment
        binding.viewModel = viewModel
    }


    override fun onStart() {
        super.onStart()

        binding.edittextPassword.setOnEditorActionListener { _, _, _ ->
            Util.hideKeyboard(requireActivity())
            true
        }

        viewModel.createAccountUsername.observe(this, {
            viewModel.isUsernameValid.value = PatternRegex.checkUsernameRegex(it)
            viewModel.isUsernameOverlapped.value = false
        })

        viewModel.createAccountPassword.observe(this, {
            viewModel.isPasswordValid.value = PatternRegex.checkPasswordRegex(it)
            viewModel.isPasswordCheckValid.value = it == viewModel.createAccountPasswordCheck.value
        })

        viewModel.createAccountPasswordCheck.observe(this, {
            viewModel.isPasswordCheckValid.value = it == viewModel.createAccountPassword.value
        })

        viewModel.isUsernameValid.observe(this, {checkValidationToSetNextButton()})
        viewModel.isPasswordValid.observe(this, {checkValidationToSetNextButton()})
        viewModel.isPasswordCheckValid.observe(this, {checkValidationToSetNextButton()})
    }

    private fun checkValidationToSetNextButton() {
        if(viewModel.isUsernameValid.value!! && viewModel.isPasswordValid.value!! && viewModel.isPasswordCheckValid.value!!){
            (parentFragment as CreateAccountFragment).enableNextButton()
        }else{
            (parentFragment as CreateAccountFragment).disableNextButton()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}