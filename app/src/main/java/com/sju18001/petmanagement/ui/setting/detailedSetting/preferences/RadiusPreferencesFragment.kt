package com.sju18001.petmanagement.ui.setting.detailedSetting.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.slider.Slider
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.FragmentRadiusPreferencesBinding
import com.sju18001.petmanagement.ui.setting.SettingViewModel
import com.sju18001.petmanagement.ui.setting.detailedSetting.DetailedSettingViewModel

object RadiusPreferencesFragmentConstants {
    const val RADIUS_VALUE_0_KM = 0.0
    const val RADIUS_VALUE_100_KM = 100000.0
}

class RadiusPreferencesFragment : Fragment() {

    private val viewModel: DetailedSettingViewModel by activityViewModels()

    private var _binding: FragmentRadiusPreferencesBinding? = null
    private val binding get() = _binding!!

    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRadiusPreferencesBinding.inflate(inflater, container, false)

        viewModel.initializeViewModelForRadius(requireContext())
        setLiveDataObservers()
        setViewDetails()

        isViewDestroyed = false

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isViewDestroyed = true
    }

    private fun setLiveDataObservers() {
        viewModel.isApiLoading.observe(this, { newValue ->
            if (newValue == true){
                binding.radiusSwitch.visibility = View.INVISIBLE
                binding.radiusSwitchProgressBar.visibility = View.VISIBLE
                binding.radiusSlider.isEnabled = false
            }else{
                binding.radiusSwitch.visibility = View.VISIBLE
                binding.radiusSwitchProgressBar.visibility = View.INVISIBLE
                binding.radiusSlider.isEnabled = true
            }
        })

        viewModel.radiusSlider.observe(this, { newValue ->
            binding.radiusSwitch.isChecked = newValue != 0.0

            if (binding.radiusSwitch.isChecked){
                binding.radiusSliderLayout.visibility = View.VISIBLE
                binding.radiusSlider.value = newValue.toFloat()
                binding.radiusMessage.setText(R.string.radius_enabled_message)
            }else{
                binding.radiusSliderLayout.visibility = View.GONE
                binding.radiusMessage.setText(R.string.radius_disabled_message)
            }
        })
    }

    private fun setViewDetails() {
        binding.radiusSwitch.setOnClickListener {
            val newRadius = if(binding.radiusSwitch.isChecked) RadiusPreferencesFragmentConstants.RADIUS_VALUE_100_KM
            else RadiusPreferencesFragmentConstants.RADIUS_VALUE_0_KM
            viewModel.updateAccountWithNewRadius(requireContext(), isViewDestroyed, newRadius)
        }
        
        binding.radiusSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.updateAccountWithNewRadius(requireContext(), isViewDestroyed,
                    binding.radiusSlider.value.toDouble())
            }
        })

        binding.radiusSlider.setLabelFormatter { value ->
            return@setLabelFormatter "${value.toInt() / 1000}km"
        }
    }
}