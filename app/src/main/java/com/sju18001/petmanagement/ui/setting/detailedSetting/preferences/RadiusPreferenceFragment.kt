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
import com.sju18001.petmanagement.databinding.FragmentRadiuspreferenceBinding
import com.sju18001.petmanagement.ui.setting.SettingViewModel
import com.sju18001.petmanagement.ui.setting.detailedSetting.DetailedSettingViewModel

object RadiusPreferencesFragmentConstants {
    const val RADIUS_VALUE_0_KM = 0.0
    const val RADIUS_VALUE_100_KM = 100000.0
}

class RadiusPreferenceFragment : Fragment() {

    private val viewModel: DetailedSettingViewModel by activityViewModels()

    private var _binding: FragmentRadiuspreferenceBinding? = null
    private val binding get() = _binding!!

    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRadiuspreferenceBinding.inflate(inflater, container, false)

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
                binding.switchRadius.visibility = View.INVISIBLE
                binding.progressbarRadius.visibility = View.VISIBLE
                binding.sliderRadius.isEnabled = false
            }else{
                binding.switchRadius.visibility = View.VISIBLE
                binding.progressbarRadius.visibility = View.INVISIBLE
                binding.sliderRadius.isEnabled = true
            }
        })

        viewModel.radiusSlider.observe(this, { newValue ->
            binding.switchRadius.isChecked = newValue != 0.0

            if (binding.switchRadius.isChecked){
                binding.relativelayoutSlider.visibility = View.VISIBLE
                binding.sliderRadius.value = newValue.toFloat()
                binding.textviewMessage.setText(R.string.radius_enabled_message)
            }else{
                binding.relativelayoutSlider.visibility = View.GONE
                binding.textviewMessage.setText(R.string.radius_disabled_message)
            }
        })
    }

    private fun setViewDetails() {
        binding.switchRadius.setOnClickListener {
            val newRadius = if(binding.switchRadius.isChecked) RadiusPreferencesFragmentConstants.RADIUS_VALUE_100_KM
            else RadiusPreferencesFragmentConstants.RADIUS_VALUE_0_KM
            viewModel.updateAccountWithNewRadius(requireContext(), isViewDestroyed, newRadius)
        }

        binding.sliderRadius.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.updateAccountWithNewRadius(requireContext(), isViewDestroyed,
                    binding.sliderRadius.value.toDouble())
            }
        })

        binding.sliderRadius.setLabelFormatter { value ->
            return@setLabelFormatter "${value.toInt() / 1000}km"
        }
    }
}