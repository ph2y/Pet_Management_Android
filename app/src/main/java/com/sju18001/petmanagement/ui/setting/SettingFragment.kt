package com.sju18001.petmanagement.ui.setting

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.ads.AdRequest
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentSettingBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dto.FetchAccountPhotoReqDto
import com.sju18001.petmanagement.ui.setting.detailedSetting.DetailedSettingActivity
import java.io.File

class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingViewModel by activityViewModels()

    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        isViewDestroyed = false

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@SettingFragment
        binding.viewModel = viewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.adView.loadAd(AdRequest.Builder().build())
    }

    override fun onStart() {
        super.onStart()

        binding.accountLookup.setOnClickListener {
            val accountLookupIntent = Intent(context, DetailedSettingActivity::class.java)
            accountLookupIntent.putExtra("fragmentType", "update_account")

            if(viewModel.photoUrl.value != null) {
                Util.putByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
                    requireContext().getString(R.string.data_name_setting_selected_account_photo), viewModel.photoByteArray.value)
            }
            else {
                Util.putByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
                    requireContext().getString(R.string.data_name_setting_selected_account_photo), null)
            }

            startActivity(accountLookupIntent)
            requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }

        binding.radiusLookup.setOnClickListener {
            val preferencesLookupIntent = Intent(context, DetailedSettingActivity::class.java)
            preferencesLookupIntent.putExtra("fragmentType", "radius_preferences")
            startActivity(preferencesLookupIntent)
            requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }

        binding.notificationLookup.setOnClickListener {
            val notificationPreferencesIntent = Intent(context, DetailedSettingActivity::class.java)
            notificationPreferencesIntent.putExtra("fragmentType", "notification_preferences")
            startActivity(notificationPreferencesIntent)
            requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }

        binding.themeLookup.setOnClickListener {
            val themePreferencesIntent = Intent(context, DetailedSettingActivity::class.java)
            themePreferencesIntent.putExtra("fragmentType", "theme_preferences")
            startActivity(themePreferencesIntent)
            requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }

        binding.deleteTemporaryFilesButton.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(context?.getString(R.string.delete_temporary_files_message))
                .setPositiveButton(
                    R.string.confirm
                ) { _, _ ->
                    // delete temporary files + set size to 0 after deletion
                    deleteTemporaryFiles()
                    setTemporaryFilesSize()
                }
                .setNegativeButton(
                    R.string.cancel
                ) { dialog, _ ->
                    dialog.cancel()
                }
                .create().show()
        }

        binding.privacyTermsLookup.setOnClickListener {
            val privacyTermsIntent = Intent(context, DetailedSettingActivity::class.java)
            privacyTermsIntent.putExtra("fragmentType", "privacy_terms")
            startActivity(privacyTermsIntent)
            requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }
        binding.usageTermsLookup.setOnClickListener {
            val usageTermsIntent = Intent(context, DetailedSettingActivity::class.java)
            usageTermsIntent.putExtra("fragmentType", "usage_terms")
            startActivity(usageTermsIntent)
            requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }
        binding.licenseLookup.setOnClickListener {
            val licenseIntent = Intent(context, DetailedSettingActivity::class.java)
            licenseIntent.putExtra("fragmentType", "license")
            startActivity(licenseIntent)
            requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }
    }

    override fun onResume() {
        super.onResume()

        fetchAccountProfileData()
        setTemporaryFilesSize()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        isViewDestroyed = true
    }

    private fun fetchAccountProfileData() {
        // create empty body
        val account = SessionManager.fetchLoggedInAccount(requireContext())!!
        viewModel.nickname.value = account.nickname!!
        viewModel.photoUrl.value = account.photoUrl!!

        // set views after API response
        setViewsWithAccountProfileData()
    }

    private fun fetchAccountPhotoAndSetView() {
        if(viewModel.photoUrl.value == null){
            // 기본 사진으로 세팅
            binding.accountPhoto.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_baseline_account_circle_36))
            return
        }

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchAccountPhotoReq(FetchAccountPhotoReqDto(null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            viewModel.photoByteArray.value = response.body()!!.byteStream().readBytes()
            val bitmap = Util.getBitmapFromByteArray(viewModel.photoByteArray.value!!)
            binding.accountPhoto.setImageBitmap(bitmap)
        }, {}, {})
    }

    private fun setViewsWithAccountProfileData() {
        fetchAccountPhotoAndSetView()
    }

    private fun deleteTemporaryFiles() {
        val dir = File(requireContext().getExternalFilesDir(null).toString())

        if (dir.exists()) {
            val files = dir.listFiles()

            if (files != null) {
                for (file in files) {
                    if (Util.LOG_FILE_NAME !in file.toString()) {
                        file.deleteRecursively()
                    }
                }
            }
        }
    }

    private fun setTemporaryFilesSize() {
        val size = String.format("%.1f", (Util.getTemporaryFilesSize(requireContext()) / 1e6)) + "MB"
        binding.temporaryFilesSize.text = size
    }
}