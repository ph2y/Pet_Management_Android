package com.sju18001.petmanagement.ui.myPet.petManager.petProfile

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.FormattedDate
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.databinding.ActivityCreateUpdatePetBinding
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.restapi.global.FileType
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.util.*

/**
 * 다른 곳에서 이 액티비티를 호출하는 과정에서 Intent를 전달하는데, 그 정보를 ViewModel에
 * 저장합니다. ViewModel의 정보는 Databinding되어 있으므로 ViewModel의 값이 매우 중요합니다.
 * 추가로 UpdatePet을 성공적으로 마쳤을 경우 PetProfileActivity에 결과를 전달합니다.
 */

class CreateUpdatePetActivity : AppCompatActivity() {
    companion object {
        const val PICK_PHOTO = 0
        const val CREATE_UPDATE_PET_DIRECTORY: String = "create_update_pet"
    }

    enum class ActivityType {
        CREATE_PET, UPDATE_PET
    }

    private lateinit var binding: ActivityCreateUpdatePetBinding

    private val viewModel: CreateUpdatePetViewModel by viewModels()
    private var isViewDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBinding()

        isViewDestroyed = false
        supportActionBar?.hide()

        initializeViewModel()
        setObserversOfLiveData()

        binding.adView.loadAd(AdRequest.Builder().build())
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_update_pet)

        binding.lifecycleOwner = this
        binding.activity = this@CreateUpdatePetActivity
        binding.viewModel = viewModel
    }

    private fun initializeViewModel() {
        // ViewModel Lifecycle 기준 최초 1회만 수행합니다.
        if(viewModel.activityType.value != null) return

        viewModel.activityType.value = intent.getIntExtra("activityType", 0)

        // UpdatePetActivity인 경우에는 기존 펫의 정보를 불러옵니다.
        if(viewModel.isActivityTypeUpdatePet()){
            intent.getStringExtra("petMessage")?.let{ viewModel.petMessage.value = it }
            intent.getStringExtra("petName")?.let{ viewModel.petName.value = it }
            intent.getStringExtra("petSpecies")?.let{ viewModel.petSpecies.value = it }
            intent.getStringExtra("petBreed")?.let{ viewModel.petBreed.value = it }
            viewModel.petId = intent.getLongExtra("petId", -1)
            viewModel.petPhotoByteArray.value = intent.getByteArrayExtra("petPhotoByteArray")
            viewModel.petGender.value = intent.getBooleanExtra("petGender", true)
            viewModel.yearOnly.value = intent.getBooleanExtra("yearOnly", false)

            intent.getStringExtra("petBirth")?.let{
                val formattedDate = FormattedDate(it)
                viewModel.petBirthYear.value = formattedDate.getYear()
                viewModel.petBirthMonth.value = formattedDate.getMonth()
                viewModel.petBirthDay.value = formattedDate.getDay()
            }
        }
    }

    private fun setObserversOfLiveData() {
        viewModel.petPhotoPath.observe(this, {
            if(!it.isNullOrEmpty()){
                val bitmap = BitmapFactory.decodeFile(it)
                Glide.with(baseContext).load(bitmap).into(binding.petPhotoInput)
            }else{
                val drawable = getDrawable(R.drawable.ic_baseline_pets_60_with_padding)
                binding.petPhotoInput.setImageDrawable(drawable)
            }
        })

        viewModel.petPhotoByteArray.observe(this, {
            if(it != null){
                val bitmap = Util.getBitmapFromByteArray(it)
                Glide.with(baseContext).load(bitmap).into(binding.petPhotoInput)
            }else{
                val drawable = getDrawable(R.drawable.ic_baseline_pets_60_with_padding)
                binding.petPhotoInput.setImageDrawable(drawable)
            }
        })

        // 해당 property에 databinding이 적용되지 않는 문제가 있어 옵저버로 대체하였음
        viewModel.isApiLoading.observe(this, {
            if(it){
                binding.petPhotoInput.borderColor = resources.getColor(R.color.gray)
                binding.petPhotoInputButton.circleBackgroundColor = resources.getColor(R.color.gray)
            }else{
                binding.petPhotoInput.borderColor = resources.getColor(R.color.carrot)
                binding.petPhotoInputButton.circleBackgroundColor = resources.getColor(R.color.pumpkin)
            }
        })
    }


    override fun onStart() {
        super.onStart()
        Util.setupViewsForHideKeyboard(this, binding.activityCreateUpdatePetParentLayout)
    }


    // For photo select
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == PICK_PHOTO){
            if (data != null) {
                // Exception: 사이즈 초과
                if (Util.isExceedsFileSizeLimit(baseContext, data, FileType.FILE_SIZE_LIMIT_PHOTO)) {
                    Toast.makeText(baseContext, baseContext?.getText(R.string.file_size_limit_exception_message_20MB), Toast.LENGTH_SHORT).show()
                    return
                }

                val petPhotoPath = getPetPhotoPath(data)?: return

                // 기존에 파일이 있었다면 삭제
                if(viewModel.petPhotoPath.value != "") File(viewModel.petPhotoPath.value).delete()

                viewModel.petPhotoPath.value = petPhotoPath
                viewModel.petPhotoRotation.value = Util.getImageRotation(viewModel.petPhotoPath.value!!)
            }
        }
    }

    // 예외가 발생하면 null을 반환합니다.
    private fun getPetPhotoPath(data: Intent): String? {
        val fileName = Util.getSelectedFileName(baseContext, data.data!!)
        val petPhotoPath = ServerUtil.createCopyAndReturnRealPathLocal(
            baseContext, data.data!!, CREATE_UPDATE_PET_DIRECTORY, fileName
        )

        return if (Util.isUrlPhoto(petPhotoPath)) {
            petPhotoPath
        }else{
            // Exception: 지원하는 타입이 아님
            Toast.makeText(baseContext, baseContext?.getText(R.string.photo_file_type_exception_message), Toast.LENGTH_LONG).show()
            File(petPhotoPath).delete()

            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        isViewDestroyed = true
        viewModel.isApiLoading.value = false

        if(isFinishing) Util.deleteCopiedFiles(baseContext, CREATE_UPDATE_PET_DIRECTORY)
    }

    override fun finish() {
        super.finish()
        if(viewModel.activityType.value == ActivityType.UPDATE_PET.ordinal){
            overridePendingTransition(0, 0)
        }
    }


    /**
     * Databinding functions
     */
    fun onClickPetPhotoInputButton() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setContentView(R.layout.select_photo_dialog)
        dialog.show()

        setListenersOnDialog(dialog)
    }

    private fun setListenersOnDialog(dialog: Dialog) {
        dialog.findViewById<ImageView>(R.id.close_button2).setOnClickListener { dialog.dismiss() }

        dialog.findViewById<Button>(R.id.upload_photo_button).setOnClickListener {
            dialog.dismiss()

            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "사진 선택"), PICK_PHOTO)
        }

        dialog.findViewById<Button>(R.id.use_default_image).setOnClickListener {
            dialog.dismiss()

            // 기존에 파일이 있었다면 삭제
            if (viewModel.petPhotoPath.value != "") File(viewModel.petPhotoPath.value).delete()

            viewModel.petPhotoRotation.value = 0f
            viewModel.petPhotoByteArray.value = null
            viewModel.petPhotoPath.value = ""
        }
    }

    fun onClickConfirmButton() {
        if(isConfirmable()){
            if(viewModel.isActivityTypeCreatePet()) createPet()
            else updatePet()
        }else{
            Toast.makeText(baseContext, getString(R.string.not_confirmable_message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun isConfirmable(): Boolean {
        return !viewModel.petName.value.isNullOrEmpty() && viewModel.petGender.value != null && !viewModel.petSpecies.value.isNullOrEmpty() && !viewModel.petBreed.value.isNullOrEmpty()
    }

    private fun createPet() {
        trimViewModelOfInputText()
        val createPetRequestDto = CreatePetReqDto(
            binding.petNameInput.text.toString(),
            binding.petSpeciesInput.text.toString(),
            binding.petBreedInput.text.toString(),
            FormattedDate(binding.petBirthInput.year, binding.petBirthInput.month, binding.petBirthInput.dayOfMonth).getFormattedString(),
            binding.yearOnlyCheckbox.isChecked,
            binding.genderFemale.isChecked,
            binding.petMessageInput.text.toString()
        )

        viewModel.isApiLoading.value = true
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .createPetReq(createPetRequestDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            if(viewModel.petPhotoPath.value != "") updatePetPhotoAndFinish(it.body()!!.id, viewModel.petPhotoPath.value!!)
            else showToastAndFinishAfterCreateUpdatePet()
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }

    private fun trimViewModelOfInputText() {
        viewModel.petMessage.value = viewModel.petMessage?.value?.trim()
        viewModel.petName.value = viewModel.petName?.value?.trim()
        viewModel.petSpecies.value = viewModel.petSpecies?.value?.trim()
        viewModel.petBreed.value = viewModel.petBreed?.value?.trim()
    }

    private fun updatePet() {
        trimViewModelOfInputText()
        val updatePetReqDto = UpdatePetReqDto(
            viewModel.petId!!,
            binding.petNameInput.text.toString(),
            binding.petSpeciesInput.text.toString(),
            binding.petBreedInput.text.toString(),
            FormattedDate(binding.petBirthInput.year, binding.petBirthInput.month+1, binding.petBirthInput.dayOfMonth)
                .getFormattedString(),
            binding.yearOnlyCheckbox.isChecked,
            binding.genderFemale.isChecked,
            binding.petMessageInput.text.toString()
        )

        viewModel.isApiLoading.value = true
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .updatePetReq(updatePetReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            if(hasPetPhotoChanged()){
                if(viewModel.petPhotoPath.value != "") updatePetPhotoAndFinish(viewModel.petId, viewModel.petPhotoPath.value!!)
                else deletePetPhotoAndFinish(viewModel.petId)
            }else{
                showToastAndFinishAfterCreateUpdatePet()
            }
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }

    private fun hasPetPhotoChanged(): Boolean {
        return intent.getIntExtra("originalPetPhotoSize", 0) != viewModel.petPhotoByteArray.value?.size?:0
    }

    private fun updatePetPhotoAndFinish(id: Long, path: String) {
        val file = MultipartBody.Part.createFormData("file", File(path).name, RequestBody.create(MediaType.parse("multipart/form-data"), File(path)))
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .updatePetPhotoReq(id, file)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            File(path).delete()
            showToastAndFinishAfterCreateUpdatePet()
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }

    private fun deletePetPhotoAndFinish(id: Long) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .deletePetPhotoReq(DeletePetPhotoReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            showToastAndFinishAfterCreateUpdatePet()
        }, {
            // TODO: 아래 코드로 변경할 것
            // viewModel.isApiLoading.value = false
            // 정상 처리가 되어도 서버에서 NullPointerException이 나오기 때문에 임시 조치를 내렸습니다.
            // PetProfile로 복귀한 뒤에 에러 메시지가 떠도 정상입니다.
            showToastAndFinishAfterCreateUpdatePet()
           }, {
            viewModel.isApiLoading.value = false
           }
        )
    }

    private fun showToastAndFinishAfterCreateUpdatePet() {
        if(viewModel.isActivityTypeCreatePet()) {
            Toast.makeText(baseContext, baseContext?.getText(R.string.create_pet_successful), Toast.LENGTH_LONG).show()
            finish()
        }
        else {
            Toast.makeText(baseContext, baseContext?.getText(R.string.update_pet_successful), Toast.LENGTH_LONG).show()

            savePetPhotoByteArrayToSharedPreferences()
            setResult(Activity.RESULT_OK, makeResultIntentForUpdate())
            finish()
        }
    }

    private fun savePetPhotoByteArrayToSharedPreferences() {
        // 사진이 기본 이미지일 때 예외 처리
        try{
            val photoByteArray = Util.getByteArrayFromDrawable(binding.petPhotoInput.drawable)
            Util.saveByteArrayToSharedPreferences(
                baseContext, baseContext.getString(R.string.pref_name_byte_arrays),
                baseContext.getString(R.string.data_name_my_pet_selected_pet_photo), photoByteArray
            )
        }catch(e: Exception){
            Util.saveByteArrayToSharedPreferences(
                baseContext, baseContext.getString(R.string.pref_name_byte_arrays),
                baseContext.getString(R.string.data_name_my_pet_selected_pet_photo), null
            )
        }
    }

    // 변경된 펫의 정보를 기반으로 Intent를 생성합니다. 해당 Intent는 이후 PetProfile로 넘어갑니다.
    private fun makeResultIntentForUpdate(): Intent {
        val intent = Intent()
        intent.putExtra("petBirth",
            FormattedDate(
                binding.petBirthInput.year,
                binding.petBirthInput.month + 1,
                binding.petBirthInput.dayOfMonth
            ).getFormattedString()
        )
        intent.putExtra("petPhotoRotation", viewModel.petPhotoRotation.value)
        intent.putExtra("petName", binding.petNameInput.text.toString())
        intent.putExtra("petSpecies", binding.petSpeciesInput.text.toString())
        intent.putExtra("petBreed", binding.petBreedInput.text.toString())
        intent.putExtra("petGender", binding.genderFemale.isChecked)
        intent.putExtra("petAge", LocalDate.now().year - binding.petBirthInput.year)
        intent.putExtra("petMessage", binding.petMessageInput.text.toString())
        intent.putExtra("yearOnly", binding.yearOnlyCheckbox.isChecked)

        return intent
    }


    fun onClickBackButton() {
        finish()
    }

    fun onClickDeleteButton() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.delete_pet_dialog_message))
            .setPositiveButton(
                R.string.confirm
            ) { _, _ ->
                deletePet()
            }
            .setNegativeButton(
                R.string.cancel
            ) { dialog, _ ->
                dialog.cancel()
            }
            .create().show()
    }

    private fun deletePet() {
        viewModel.isApiLoading.value = true
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .deletePetReq(DeletePetReqDto(intent.getLongExtra("petId", -1)))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            showToastAndFinishAfterDeletePet()
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }

    private fun showToastAndFinishAfterDeletePet() {
        Toast.makeText(baseContext, baseContext?.getText(R.string.delete_pet_successful), Toast.LENGTH_LONG).show()

        setResult(Activity.RESULT_OK)
        finish()
    }
}