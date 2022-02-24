package com.sju18001.petmanagement.ui.setting.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.sju18001.petmanagement.databinding.FragmentRadiusPreferencesBinding
import com.sju18001.petmanagement.ui.setting.SettingViewModel

class RadiusPreferencesFragment : Fragment() {

    private val settingViewModel: SettingViewModel by activityViewModels()

    private var _binding: FragmentRadiusPreferencesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRadiusPreferencesBinding.inflate(inflater, container, false)
        return binding.root
    }
}