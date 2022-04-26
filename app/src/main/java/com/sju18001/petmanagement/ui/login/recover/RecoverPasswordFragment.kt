package com.sju18001.petmanagement.ui.login.recover

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.PatternRegex
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentRecoverpasswordBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dto.*

class RecoverPasswordFragment : Fragment() {
    private var _binding: FragmentRecoverpasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecoverViewModel by activityViewModels()
    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        isViewDestroyed = false

        Util.setupViewsForHideKeyboard(requireActivity(), binding.framelayoutParent)

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_recoverpassword, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@RecoverPasswordFragment
        binding.viewModel = viewModel
    }

    override fun onStart() {
        super.onStart()
        setListeners()
    }

    private fun setListeners() {
        binding.edittextEmail.addTextChangedListener {
            viewModel.isEmailForRecoverPasswordValid.value = PatternRegex.checkEmailRegex(it)
        }

        binding.edittextEmail.setOnEditorActionListener{ _, _, _ ->
            if(viewModel.isEmailForRecoverPasswordValid.value!!){
                Util.hideKeyboard(requireActivity())
                sendAuthCode()
            }
            true
        }

        binding.edittextUsername.addTextChangedListener {
            viewModel.isUsernameForRecoverPasswordValid.value = PatternRegex.checkUsernameRegex(it)
        }

        binding.edittextAuthcode.setOnEditorActionListener{ _, _, _ ->
            if(viewModel.isUsernameForRecoverPasswordValid.value!!){
                Util.hideKeyboard(requireActivity())
                recoverPassword()
            }
            true
        }
    }

    fun sendAuthCode(){
        viewModel.isApiLoading.value = true
        val call = RetrofitBuilder.getServerApi()
            .sendAuthCodeReq(SendAuthCodeReqDto(viewModel.emailForRecoverPassword.value!!))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            viewModel.recoverPasswordPhase.value = 1
            viewModel.isApiLoading.value = false
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }


    fun recoverPassword(){
        viewModel.isApiLoading.value = true
        val call = RetrofitBuilder.getServerApi().recoverPasswordReq(
            RecoverPasswordReqDto(viewModel.usernameForRecoverPassword.value!!,
            viewModel.authCodeForRecoverPassword.value!!)
        )
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            viewModel.recoverPasswordPhase.value = 2
            viewModel.isApiLoading.value = false
        }, {
            binding.textviewAuthcodemessage.visibility = View.VISIBLE
            viewModel.isApiLoading.value = false
        }, { viewModel.isApiLoading.value = false })
    }


    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        isViewDestroyed = true
    }
}
