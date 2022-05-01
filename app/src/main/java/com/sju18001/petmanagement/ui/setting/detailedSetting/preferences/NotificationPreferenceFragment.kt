package com.sju18001.petmanagement.ui.setting.detailedSetting.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.ui.setting.SettingViewModel
import com.sju18001.petmanagement.ui.setting.detailedSetting.DetailedSettingViewModel

class NotificationPreferenceFragment : PreferenceFragmentCompat() {
    private val viewModel: DetailedSettingViewModel by activityViewModels()

    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isViewDestroyed = false

        setLiveDataObservers()

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun setLiveDataObservers() {
        viewModel.isApiLoading.observe(this) { newValue ->
            (findPreference("notification_preference") as SwitchPreference?)?.let { pref ->
                pref.isEnabled = !newValue
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.notification_preferences, rootKey)

        (findPreference("notification_preference") as SwitchPreference?)?.let{ pref ->
            initializeNotificationPreference(pref)
            setListenerOnPreference(pref)
        }
    }

    private fun initializeNotificationPreference(pref: SwitchPreference) {
        // SessionManager에서 홀딩하고 있는 Account 정보가 항상 최신 정보를 담기 때문에
        // ViewModel을 사용할 필요가 없으며, 따라서 해당 정보를 기준으로 초기화합니다.
        val notification = SessionManager.fetchLoggedInAccount(requireContext())!!.notification == true
        pref.isChecked = notification
    }

    private fun setListenerOnPreference(pref: SwitchPreference) {
        pref.setOnPreferenceChangeListener { _, newValue ->
            viewModel.updateAccountWithNewNotification(
                requireContext(), isViewDestroyed, newValue as Boolean
            )
            true
        }
    }

    override fun onDestroyView() {
        isViewDestroyed = true
        super.onDestroyView()
    }
}