package com.sju18001.petmanagement.ui.setting.detailedSetting.account

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.PatternRegex
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentUpdateAccountBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.restapi.global.FileType
import com.sju18001.petmanagement.ui.login.LoginActivity
import com.sju18001.petmanagement.ui.setting.SettingViewModel
import com.sju18001.petmanagement.ui.setting.detailedSetting.DetailedSettingViewModel
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class UpdateAccountFragment : Fragment() {
    companion object{
        private const val PICK_PHOTO = 0
        private const val UPDATE_ACCOUNT_DIRECTORY: String = "update_account"
    }

    private var _binding: FragmentUpdateAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailedSettingViewModel by activityViewModels()
    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        isViewDestroyed = false

        if(!viewModel.isViewModelInitialized) initializeViewModel()

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentUpdateAccountBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@UpdateAccountFragment
        binding.viewModel = viewModel
    }

    private fun initializeViewModel() {
        viewModel.isViewModelInitialized = true

        viewModel.photoByteArray.value = Util.getByteArrayFromSharedPreferences(requireContext(),
            requireContext().getString(R.string.pref_name_byte_arrays),
            requireContext().getString(R.string.data_name_setting_selected_account_photo))

        SessionManager.fetchLoggedInAccount(requireContext())?.run{
            viewModel.id.value = id
            viewModel.username.value = username
            viewModel.email.value = email
            viewModel.phone.value = phone
            viewModel.marketing.value = marketing
            viewModel.nickname.value = nickname
            viewModel.photoUrl.value = photoUrl
            viewModel.userMessage.value = userMessage
            viewModel.representativePetId.value = representativePetId
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.adView.loadAd(AdRequest.Builder().build())
    }


    override fun onStart() {
        super.onStart()

        setListeners()
        Util.setupViewsForHideKeyboard(requireActivity(), binding.fragmentUpdateAccountParentFragment)
    }

    private fun setListeners() {
        viewModel.isApiLoading.observe(this, {
            if(it){
                binding.accountPhotoInput.borderColor = resources.getColor(R.color.gray)
                binding.accountPhotoInputButton.circleBackgroundColor = resources.getColor(R.color.gray)
            }else{
                binding.accountPhotoInput.borderColor = resources.getColor(R.color.carrot)
                binding.accountPhotoInputButton.circleBackgroundColor = resources.getColor(R.color.carrot)
            }
        })

        viewModel.photoByteArray.observe(this, {
            if(it != null){
                val bitmap = Util.getBitmapFromByteArray(it)
                binding.accountPhotoInput.setImageBitmap(bitmap)
            }else if(viewModel.photoPath.value == ""){
                binding.accountPhotoInput.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_baseline_account_circle_36))
            }
        })

        viewModel.photoPath.observe(this, {
            if(it != ""){
                Glide.with(requireContext())
                    .load(BitmapFactory.decodeFile(it))
                    .into(binding.accountPhotoInput)
                binding.accountPhotoInput.rotation = Util.getImageRotation(it)
            }else if(viewModel.photoByteArray.value == null){
                binding.accountPhotoInput.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_baseline_account_circle_36))
            }
        })

        binding.marketingSwitch.setOnCheckedChangeListener { _, isChecked ->
            val message = if(isChecked) context?.getText(R.string.marketing_agree)
            else context?.getText(R.string.marketing_decline)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        binding.phoneEdit.addTextChangedListener(PhoneNumberFormattingTextWatcher("KR"))
    }


    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        isViewDestroyed = true

        if(isRemoving || requireActivity().isFinishing) {
            Util.deleteCopiedFiles(requireContext(), UPDATE_ACCOUNT_DIRECTORY)
        }
    }


    // 사진을 고르고 난 이후
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == AppCompatActivity.RESULT_OK && requestCode == PICK_PHOTO && data != null) {
            // Exception: Exceeds size limit
            if (Util.isExceedsFileSizeLimit(requireContext(), data, FileType.FILE_SIZE_LIMIT_PHOTO)) {
                Toast.makeText(context, context?.getText(R.string.file_size_limit_exception_message_20MB), Toast.LENGTH_SHORT).show()
                return
            }

            val fileName = Util.getSelectedFileName(requireContext(), data.data!!)
            val photoPath = ServerUtil.createCopyAndReturnRealPathLocal(requireActivity(),
                data.data!!, UPDATE_ACCOUNT_DIRECTORY, fileName)

            // Exception: Not a photo
            if (!Util.isUrlPhoto(photoPath)) {
                Toast.makeText(context, context?.getText(R.string.photo_file_type_exception_message), Toast.LENGTH_LONG).show()
                File(photoPath).delete()
                return
            }

            // 기존 photoPath를 참조하여 이전 파일을 삭제한다.
            if(viewModel.photoPath.value != "") {
                File(viewModel.photoPath.value).delete()
            }

            viewModel.photoPath.value = photoPath
            viewModel.photoByteArray.value = null

            viewModel.hasPhotoChanged = true
        }
    }


    /** Databinding functions */
    fun onClickBackButton() {
        activity?.finish()
    }

    fun onClickAccountPhotoInputButton() {
        Dialog(requireActivity()).run{
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.select_photo_dialog)
            show()

            findViewById<ImageView>(R.id.close_button2).setOnClickListener { dismiss() }
            findViewById<Button>(R.id.upload_photo_button).setOnClickListener {
                dismiss()

                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "사진 선택"), PICK_PHOTO)
            }
            findViewById<Button>(R.id.use_default_image).setOnClickListener {
                dismiss()

                // 기존에 Photo가 있는데 사진을 삭제한 경우
                if(viewModel.photoByteArray.value != null) viewModel.hasPhotoChanged = true

                // 기존 photoPath를 참조하여 이전 파일을 삭제한다.
                if (viewModel.photoPath.value != "") {
                    File(viewModel.photoPath.value).delete()
                }
                viewModel.photoPath.value = ""
                viewModel.photoByteArray.value = null
            }
        }
    }

    fun onClickLogoutButton() {
        AlertDialog.Builder(activity).setMessage(context?.getString(R.string.logout_dialog))
            .setPositiveButton(R.string.confirm) { _, _ -> logoutAndStartLoginActivity() }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create().show()
    }

    private fun logoutAndStartLoginActivity() {
        ServerUtil.doLogout(requireContext())

        val intent = Intent(context, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
    

    fun onClickDeleteAccountButton() {
        AlertDialog.Builder(activity).setMessage(context?.getString(R.string.delete_account_dialog))
            .setPositiveButton(R.string.confirm) { _, _ -> deleteAccount() }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create().show()
    }

    private fun deleteAccount() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .deleteAccountReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            if(response.body()?._metadata?.status == true) {
                Toast.makeText(context, context?.getText(R.string.account_delete_success), Toast.LENGTH_LONG).show()
                logoutAndStartLoginActivity()
            }
        }, {}, {})
    }


    /** onClickConfirmButton() */
    fun onClickConfirmButton() {
        if(PatternRegex.checkNicknameRegex(viewModel.nickname.value)) updateAccount()
        else {
            Toast.makeText(context, context?.getText(R.string.account_regex_invalid), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAccount() {
        viewModel.isApiLoading.value = true

        val account = SessionManager.fetchLoggedInAccount(requireContext())!!
        val updateAccountReqDto = UpdateAccountReqDto(viewModel.email.value!!, viewModel.phone.value!!,
            viewModel.nickname.value, viewModel.marketing.value, viewModel.userMessage.value,
            viewModel.representativePetId.value, account.notification, account.mapSearchRadius)

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .updateAccountReq(updateAccountReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            val newAccount = Account(
                account.id, account.username, viewModel.email.value!!, viewModel.phone.value!!,
                null, viewModel.marketing.value, viewModel.nickname.value, account.photoUrl,
                viewModel.userMessage.value, viewModel.representativePetId.value, account.fcmRegistrationToken,
                account.notification, account.mapSearchRadius
            )
            SessionManager.saveLoggedInAccount(requireContext(), newAccount)

            if(viewModel.hasPhotoChanged){
                if(viewModel.photoPath.value != "") updateAccountPhotoAndFinish()
                else deleteAccountPhotoAndFinish()
            }else{
                finishAfterSuccess()
            }
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }

    private fun updateAccountPhotoAndFinish() {
        val file = File(viewModel.photoPath.value)
        val body = MultipartBody.Part.createFormData("file", file.name, RequestBody.create(MediaType.parse("multipart/form-data"), file))
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .updateAccountPhotoReq(body)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            val account = SessionManager.fetchLoggedInAccount(requireContext())!!
            account.photoUrl = response.body()!!.fileUrl
            SessionManager.saveLoggedInAccount(requireContext(), account)

            file.delete()

            finishAfterSuccess()
        }, {}, {})
    }

    private fun deleteAccountPhotoAndFinish() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .deleteAccountPhotoReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            val account = SessionManager.fetchLoggedInAccount(requireContext())!!
            account.photoUrl = null
            SessionManager.saveLoggedInAccount(requireContext(), account)

            finishAfterSuccess()
        }, {}, {})
    }

    private fun finishAfterSuccess() {
        Toast.makeText(context, context?.getText(R.string.account_update_success), Toast.LENGTH_LONG).show()
        activity?.finish()
    }

    fun onClickChangePasswordButton() {
        ChangePasswordDialog(requireActivity()) { newPassword: String, password: String ->
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                    .updateAccountPasswordReq(UpdateAccountPasswordReqDto(password, newPassword))
            ServerUtil.enqueueApiCall(call, { isViewDestroyed }, requireContext(), { response ->
                Toast.makeText(context, context?.getText(R.string.account_password_changed), Toast.LENGTH_LONG).show()
            }, {}, {})
        }.show()
    }
}