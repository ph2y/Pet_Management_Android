package com.sju18001.petmanagement.ui.login.createAccount

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.sju18001.petmanagement.databinding.FragmentCreateaccounttermsBinding
import com.sju18001.petmanagement.ui.login.LoginViewModel

class CreateAccountTermsFragment: Fragment() {
    private val viewModel: CreateAccountViewModel by activityViewModels()
    
    private var _binding: FragmentCreateaccounttermsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)

        (activity as CreateAccountActivity).hidePreviousButton()
        checkValidationToSetNextButton()

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentCreateaccounttermsBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@CreateAccountTermsFragment
        binding.viewModel = viewModel
    }

    private fun checkValidationToSetNextButton() {
        if(viewModel.isTermsChecked.value!! && viewModel.isPrivacyChecked.value!!) {
            (activity as CreateAccountActivity).enableNextButton()
        }else{
            (activity as CreateAccountActivity).disableNextButton()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    /** Databinding functions */
    fun onClickAllCheckBox(view: View) {
        val flag = (view as CheckBox).isChecked
        viewModel.isTermsChecked.value = flag
        viewModel.isPrivacyChecked.value = flag
        viewModel.isMarketingChecked.value = flag
        checkValidationToSetNextButton()
    }

    fun onClickCheckBox() {
        viewModel.isAllChecked.value = viewModel.isTermsChecked.value!! && viewModel.isPrivacyChecked.value!! && viewModel.isMarketingChecked.value!!
        checkValidationToSetNextButton()
    }
}