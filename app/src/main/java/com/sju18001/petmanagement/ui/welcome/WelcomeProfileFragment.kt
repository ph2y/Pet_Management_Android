package com.sju18001.petmanagement.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.sju18001.petmanagement.MainActivity
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentWelcomeprofileBinding
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dto.FetchAccountPhotoReqDto
import com.sju18001.petmanagement.ui.setting.SettingActivity

class WelcomeProfileFragment : Fragment() {
    private var _binding: FragmentWelcomeprofileBinding? = null
    private val binding get() = _binding!!

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
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_welcomeprofile, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@WelcomeProfileFragment
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        isViewDestroyed = true
    }


    /** Databinding functions */
    fun onClickAcceptButton() {
        val account = SessionManager.fetchLoggedInAccount(requireContext())
        if(account != null){
            fetchAccountPhotoAndStartSettingActivity(account)
        }else{
            Toast.makeText(requireContext(), getString(R.string.try_again), Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAccountPhotoAndStartSettingActivity(account: Account) {
        val settingIntent = makeSettingIntent(account)

        if(account.photoUrl == null){
            Util.putByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
                requireContext().getString(R.string.data_name_setting_selected_account_photo), null)

            startActivity(settingIntent)
            requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }else{
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .fetchAccountPhotoReq(FetchAccountPhotoReqDto(null))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                Util.putByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
                    requireContext().getString(R.string.data_name_setting_selected_account_photo), response.body()!!.byteStream().readBytes())

                startActivity(settingIntent)
                requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
            }, {}, {})
        }
    }

    private fun makeSettingIntent(account: Account): Intent {
        val settingIntent = Intent(context, SettingActivity::class.java)
        settingIntent.putExtra("fragmentType", "update_account")
        settingIntent.putExtra("id", account.id)
        settingIntent.putExtra("username", account.username)
        settingIntent.putExtra("email", account.email)
        settingIntent.putExtra("phone", account.phone)
        settingIntent.putExtra("marketing", account.marketing)
        settingIntent.putExtra("nickname", account.nickname)
        settingIntent.putExtra("userMessage", account.userMessage)

        return settingIntent
    }

    fun onClickDeclineButton() {
        startActivity(Intent(context, MainActivity::class.java))
        activity?.finish()
    }
}