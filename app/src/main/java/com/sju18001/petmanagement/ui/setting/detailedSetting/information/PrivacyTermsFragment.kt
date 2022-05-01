package com.sju18001.petmanagement.ui.setting.detailedSetting.information

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sju18001.petmanagement.databinding.FragmentPrivacytermsBinding

class PrivacyTermsFragment : Fragment() {
    private var _binding: FragmentPrivacytermsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPrivacytermsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}