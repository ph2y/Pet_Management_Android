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

    override fun onResume() {
        super.onResume()

        fetchAccountProfileData()
        setTemporaryFilesSize()
    }

    private fun fetchAccountProfileData() {
        val account = SessionManager.fetchLoggedInAccount(requireContext())!!
        viewModel.nickname.value = account.nickname!!
        viewModel.photoUrl.value = account.photoUrl!!

        fetchAccountPhotoAndSetView()
    }

    private fun fetchAccountPhotoAndSetView() {
        if(viewModel.photoUrl.value == null){
            binding.circleimageviewAccountphoto.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_baseline_account_circle_36))
            return
        }

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchAccountPhotoReq(FetchAccountPhotoReqDto(null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            viewModel.photoByteArray.value = response.body()!!.byteStream().readBytes()
            val bitmap = Util.getBitmapFromByteArray(viewModel.photoByteArray.value!!)
            binding.circleimageviewAccountphoto.setImageBitmap(bitmap)
        }, {}, {})
    }

    private fun setTemporaryFilesSize() {
        viewModel.temporaryFilesSizeText.value =
            String.format("%.1f", (Util.getTemporaryFilesSize(requireContext()) / 1e6)) + "MB"
    }


    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        isViewDestroyed = true
    }


    /** Databinding functions */
    fun onClickUpdateAccount() {
        val data = if(viewModel.photoUrl.value != null) viewModel.photoByteArray.value else null
        Util.putByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
            requireContext().getString(R.string.data_name_setting_selected_account_photo), data)

        startDetailedSettingActivity("update_account")
    }

    private fun startDetailedSettingActivity(fragmentType: String){
        val intent = Intent(context, DetailedSettingActivity::class.java)
        intent.putExtra("fragmentType", fragmentType)
        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    fun onClickDeleteTemporaryFile() {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(context?.getString(R.string.delete_temporary_files_message))
            .setPositiveButton(R.string.confirm) { _, _ ->
                deleteTemporaryFiles()
                setTemporaryFilesSize()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create().show()
    }

    private fun deleteTemporaryFiles() {
        val dir = File(requireContext().getExternalFilesDir(null).toString())
        if (dir.exists() && dir.listFiles() != null) {
            for (file in dir.listFiles()) {
                if (Util.LOG_FILE_NAME !in file.toString()) file.deleteRecursively()
            }
        }
    }

    fun onClickRadiusPreference() {
        startDetailedSettingActivity("radius_preferences")
    }

    fun onClickNotificationPreference() {
        startDetailedSettingActivity("notification_preferences")
    }

    fun onClickThemePreference() {
        startDetailedSettingActivity("theme_preferences")
    }

    fun onClickPrivacyTerms() {
        startDetailedSettingActivity("privacy_terms")
    }

    fun onClickUsageTerms() {
        startDetailedSettingActivity("usage_terms")
    }

    fun onClickLicensePreference() {
        startDetailedSettingActivity("license")
    }
}