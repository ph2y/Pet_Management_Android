package com.sju18001.petmanagement.ui.login.recover

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.PatternRegex
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentRecoverusernameBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dto.RecoverUsernameReqDto

class RecoverUsernameFragment : Fragment() {
    private var _binding: FragmentRecoverusernameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecoverViewModel by viewModels()
    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_recoverusername, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@RecoverUsernameFragment
        binding.viewModel = viewModel
    }

    override fun onStart() {
        super.onStart()

        setListeners()
        Util.setupViewsForHideKeyboard(requireActivity(), binding.framelayoutParent)
    }

    private fun setListeners() {
        binding.edittextEmail.setOnEditorActionListener{ _, _, _ ->
            if(viewModel.isEmailForRecoverUsernameValid.value!!){
                Util.hideKeyboard(requireActivity())
                recoverUsername()
            }
            true
        }

        binding.edittextEmail.addTextChangedListener {
            viewModel.isEmailForRecoverUsernameValid.value = PatternRegex.checkEmailRegex(it)
        }
    }

    fun recoverUsername(){
        viewModel.isApiLoading.value = true
        val call = RetrofitBuilder.getServerApi()
            .recoverUsernameReq(RecoverUsernameReqDto(viewModel.emailForRecoverUsername.value!!))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            viewModel.username.value = response.body()?.username
            viewModel.isApiLoading.value = false
            viewModel.isUsernameShowing.value = true
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }


    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        isViewDestroyed = true
    }
}
