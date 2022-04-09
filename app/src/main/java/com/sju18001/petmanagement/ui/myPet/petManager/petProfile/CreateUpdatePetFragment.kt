package com.sju18001.petmanagement.ui.myPet.petManager.petProfile

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.FormattedDate
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentCreateUpdatePetBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class CreateUpdatePetFragment : Fragment() {
    companion object {
        const val PICK_PHOTO = 0
        const val CREATE_UPDATE_PET_DIRECTORY: String = "create_update_pet"
    }

    // variables for view binding
    private var _binding: FragmentCreateUpdatePetBinding? = null
    private val binding get() = _binding!!

    // variable for ViewModel
    private val viewModel: CreateUpdatePetViewModel by viewModels()
    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        setViewModel()
        isViewDestroyed = false

        if(!viewModel.isViewModelInitializedByBundle) {
            initializeViewModelByBundle()
        }

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentCreateUpdatePetBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@CreateUpdatePetFragment
        binding.viewModel = viewModel
    }

    private fun setViewModel() {
        viewModel.petId = requireActivity().intent.getLongExtra("petId", -1)
    }

    private fun initializeViewModelByBundle() {
        viewModel.petPhotoByteArray = arguments?.getByteArray("petPhotoByteArray")
        arguments?.getString("petMessage")?.let{ viewModel.petMessage = it }
        arguments?.getString("petName")?.let{ viewModel.petName = it }
        viewModel.petGender = arguments?.getBoolean("petGender") == true
        arguments?.getString("petSpecies")?.let{ viewModel.petSpecies = it }
        arguments?.getString("petBreed")?.let{ viewModel.petBreed = it }
        viewModel.yearOnly = arguments?.getBoolean("yearOnly") == true
        arguments?.getString("petBirth")?.let{ viewModel.petBirth = it }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.adView.loadAd(AdRequest.Builder().build())
    }

    override fun onStart() {
        super.onStart()

        // for title
        val fragmentType = requireActivity().intent.getIntExtra("fragmentType", 0)
        if(fragmentType == PetProfileActivity.FragmentType.PET_PROFILE_FROM_MY_PET.ordinal) {
            binding.backButtonTitle.text = context?.getText(R.string.update_pet_title)
        }
        else if(fragmentType == PetProfileActivity.FragmentType.CREATE_PET.ordinal){
            binding.deletePetLayout.visibility = View.GONE
        }

        // for DatePicker
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        if(viewModel.petBirth == null) {
            viewModel.petBirth = SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance())
        }
        binding.petBirthInput.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ) { _, year, monthOfYear, dayOfMonth ->
            viewModel.petBirth = FormattedDate(year, monthOfYear, dayOfMonth).getFormattedString()
        }

        restoreState()

        // for pet photo picker
        binding.petPhotoInputButton.setOnClickListener {
            val dialog = Dialog(requireActivity())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            dialog.setContentView(R.layout.select_photo_dialog)
            dialog.show()

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

                binding.petPhotoInput.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_baseline_pets_60_with_padding))
                viewModel.petPhotoRotation = 0f
                binding.petPhotoInput.rotation = viewModel.petPhotoRotation!!

                viewModel.petPhotoByteArray = null
                if (viewModel.petPhotoPath != "") {
                    File(viewModel.petPhotoPath).delete()
                    viewModel.petPhotoPath = ""
                }
                viewModel.isDeletePhoto = true
            }
        }

        // for EditText text change listeners
        binding.petMessageInput.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.petMessage = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.petNameInput.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.petName = s.toString()
                checkIsValid()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.petSpeciesInput.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.petSpecies = s.toString()
                checkIsValid()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.petBreedInput.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.petBreed = s.toString()
                checkIsValid()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        // for gender RadioButtons
        binding.genderFemale.setOnClickListener{
            if(binding.genderFemale.isChecked) {
                viewModel.petGender = true
                checkIsValid()
            }
        }
        binding.genderMale.setOnClickListener{
            if(binding.genderMale.isChecked) {
                viewModel.petGender = false
                checkIsValid()
            }
        }

        // for year only CheckBox
        binding.yearOnlyCheckbox.setOnClickListener{
            viewModel.yearOnly = binding.yearOnlyCheckbox.isChecked
        }

        // for confirm button
        binding.confirmButton.setOnClickListener {
            if(fragmentType == PetProfileActivity.FragmentType.PET_PROFILE_FROM_MY_PET.ordinal) {
                updatePet()
            }
            else {
                createPet()
            }
        }

        // for back button
        binding.backButton.setOnClickListener {
            if(fragmentType == PetProfileActivity.FragmentType.PET_PROFILE_FROM_MY_PET.ordinal) {
                fragmentManager?.popBackStack()
            }
            else {
                activity?.finish()
            }
        }

        // for delete button
        binding.deletePetButton.setOnClickListener {
            val builder = AlertDialog.Builder(activity)
            builder.setMessage(context?.getString(R.string.delete_pet_dialog_message))
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

        Util.setupViewsForHideKeyboard(requireActivity(), binding.fragmentCreateUpdatePetParentLayout)
    }

    // trim text values
    private fun trimTextValues() {
        viewModel.petMessage = viewModel.petMessage?.trim()
        viewModel.petName = viewModel.petName?.trim()
        viewModel.petSpecies = viewModel.petSpecies?.trim()
        viewModel.petBreed = viewModel.petBreed?.trim()

        binding.petMessageInput.setText(viewModel.petMessage)
        binding.petNameInput.setText(viewModel.petName)
        binding.petSpeciesInput.setText(viewModel.petSpecies)
        binding.petBreedInput.setText(viewModel.petBreed)
    }

    // create pet
    private fun createPet() {
        // set api state/button to loading
        viewModel.isApiLoading = true
        lockViews()

        trimTextValues()

        val createPetRequestDto = CreatePetReqDto(
            binding.petNameInput.text.toString(),
            binding.petSpeciesInput.text.toString(),
            binding.petBreedInput.text.toString(),
            FormattedDate(binding.petBirthInput.year, binding.petBirthInput.month, binding.petBirthInput.dayOfMonth).getFormattedString(),
            binding.yearOnlyCheckbox.isChecked,
            binding.genderFemale.isChecked,
            binding.petMessageInput.text.toString()
        )

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .createPetReq(createPetRequestDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            getIdAndUpdatePhoto()
        }, { response ->
            // set api state/button to normal
            viewModel.isApiLoading = false
            unlockViews()

            Util.showToastAndLogForNotSuccessfulResponse(requireContext(), response.errorBody())
        }, {
            // set api state/button to normal
            viewModel.isApiLoading = false
            unlockViews()
        })
    }


    private fun updatePet() {
        // set api state/button to loading
        viewModel.isApiLoading = true
        lockViews()

        trimTextValues()

        // for birth value
        val petBirthStringValue: String = if (!binding.yearOnlyCheckbox.isChecked) {
            FormattedDate(binding.petBirthInput.year, binding.petBirthInput.month+1, binding.petBirthInput.dayOfMonth)
                .getFormattedString()
        } else {
            "${binding.petBirthInput.year}-01-01"
        }

        // create DTO
        val updatePetReqDto = UpdatePetReqDto(
            viewModel.petId!!,
            binding.petNameInput.text.toString(),
            binding.petSpeciesInput.text.toString(),
            binding.petBreedInput.text.toString(),
            petBirthStringValue,
            binding.yearOnlyCheckbox.isChecked,
            binding.genderFemale.isChecked,
            binding.petMessageInput.text.toString()
        )

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .updatePetReq(updatePetReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            updatePetPhoto(viewModel.petId!!, viewModel.petPhotoPath)
        }, {
            // set api state/button to normal
            viewModel.isApiLoading = false
            unlockViews()
        }, {
            // set api state/button to normal
            viewModel.isApiLoading = false
            unlockViews()
        })
    }

    // update pet photo
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePetPhoto(id: Long, path: String) {
        // exception
        if (!viewModel.isDeletePhoto && path == "") {
            closeAfterSuccess()
            return
        }

        // delete photo
        if(viewModel.isDeletePhoto) {
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .deletePetPhotoReq(DeletePetPhotoReqDto(id))
            call.enqueue(object: Callback<DeletePetPhotoResDto> {
                override fun onResponse(
                    call: Call<DeletePetPhotoResDto>,
                    response: Response<DeletePetPhotoResDto>
                ) {
                    if(isViewDestroyed) return

                    if(response.isSuccessful){
                        closeAfterSuccess()
                    }else{
                        // get error message
                        val errorMessage = Util.getMessageFromErrorBody(response.errorBody()!!)

                        // ignore null error(delete nothing)
                        if (errorMessage == "null") {
                            closeAfterSuccess()
                        } else {
                            // set api state/button to normal
                            viewModel.isApiLoading = false
                            unlockViews()

                            Util.showToastAndLogForNotSuccessfulResponse(requireContext(), response.errorBody())
                        }
                    }
                }

                override fun onFailure(call: Call<DeletePetPhotoResDto>, t: Throwable) {
                    if(isViewDestroyed) return

                    // set api state/button to normal
                    viewModel.isApiLoading = false
                    unlockViews()

                    Util.showToastAndLog(requireContext(), t.message.toString())
                }
            })
        }
        // update photo
        else {
            val file = MultipartBody.Part.createFormData("file", File(path).name, RequestBody.create(MediaType.parse("multipart/form-data"), File(path)))
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .updatePetPhotoReq(id, file)
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
                File(path).delete()

                closeAfterSuccess()
            }, {
                // set api state/button to normal
                viewModel.isApiLoading = false
                unlockViews()
            }, {
                viewModel.isApiLoading = false
                unlockViews()
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getIdAndUpdatePhoto() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPetReq(FetchPetReqDto( null , null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            val petIdList: ArrayList<Long> = ArrayList()
            response.body()?.petList?.map {
                petIdList.add(it.id)
            }

            updatePetPhoto(petIdList[petIdList.size - 1], viewModel.petPhotoPath)
        }, {
            // set api state/button to normal
            viewModel.isApiLoading = false
            unlockViews()
        }, {
            viewModel.isApiLoading = false
            unlockViews()
        })
    }

    private fun lockViews() {
        binding.confirmButton.visibility = View.GONE
        binding.createPetProgressBar.visibility = View.VISIBLE

        binding.petPhotoInputButton.isEnabled = false
        binding.petMessageInput.isEnabled = false
        binding.petNameInput.isEnabled = false
        binding.genderFemale.isEnabled = false
        binding.genderMale.isEnabled = false
        binding.petSpeciesInput.isEnabled = false
        binding.petBreedInput.isEnabled = false
        binding.petBirthInput.isEnabled = false
        binding.yearOnlyCheckbox.isEnabled = false
        binding.backButton.isEnabled = false
        binding.petPhotoInput.borderColor = resources.getColor(R.color.gray)
        binding.petPhotoInputButton.circleBackgroundColor = resources.getColor(R.color.gray)
    }

    private fun unlockViews() {
        binding.confirmButton.visibility = View.VISIBLE
        binding.createPetProgressBar.visibility = View.GONE

        binding.petPhotoInputButton.isEnabled = true
        binding.petMessageInput.isEnabled = true
        binding.petNameInput.isEnabled = true
        binding.genderFemale.isEnabled = true
        binding.genderMale.isEnabled = true
        binding.petSpeciesInput.isEnabled = true
        binding.petBreedInput.isEnabled = true
        binding.petBirthInput.isEnabled = true
        binding.yearOnlyCheckbox.isEnabled = true
        binding.backButton.isEnabled = true
        binding.petPhotoInput.borderColor = resources.getColor(R.color.carrot)
        binding.petPhotoInputButton.circleBackgroundColor = resources.getColor(R.color.pumpkin)
    }

    private fun checkIsValid() {
        // if valid -> enable confirm button
        binding.confirmButton.isEnabled = viewModel.petName != "" && viewModel.petGender != null &&
                viewModel.petSpecies != "" && viewModel.petBreed != ""
    }

    private fun checkIsLoading() {
        // if loading -> set button to loading
        if(viewModel.isApiLoading) {
            lockViews()
        }
        else {
            unlockViews()
        }
    }

    private fun restoreState() {
        // set selected photo(if any)
        if(viewModel.petPhotoPath != "") {
            Glide.with(requireContext()).load(BitmapFactory.decodeFile(viewModel.petPhotoPath)).into(binding.petPhotoInput)
        }
        // if photo not selected, and is in update mode -> set photo
        else if(requireActivity().intent.getIntExtra("fragmentType", 0) == PetProfileActivity.FragmentType.PET_PROFILE_FROM_MY_PET.ordinal) {
            if(viewModel.petPhotoByteArray != null) {
                val bitmap = Util.getBitmapFromByteArray(viewModel.petPhotoByteArray!!)
                Glide.with(requireContext()).load(bitmap).into(binding.petPhotoInput)
            }
            else {
                binding.petPhotoInput.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_baseline_pets_60_with_padding))
            }
        }
        // if photo not selected and is in create mode -> set default
        else {
            binding.petPhotoInput.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_baseline_pets_60_with_padding))
        }
        binding.petPhotoInput.rotation = viewModel.petPhotoRotation

        binding.petNameInput.setText(viewModel.petName)
        binding.petMessageInput.setText(viewModel.petMessage)
        if(viewModel.petGender != null) {
            if(viewModel.petGender!!) {
                binding.genderFemale.isChecked = true
                binding.genderMale.isChecked = false
            }
            else {
                binding.genderFemale.isChecked = false
                binding.genderMale.isChecked = true
            }
        }
        binding.petSpeciesInput.setText(viewModel.petSpecies)
        binding.petBreedInput.setText(viewModel.petBreed)

        val formattedDate = FormattedDate(viewModel.petBirth)
        if(viewModel.petBirth != null) {
            binding.petBirthInput.updateDate(
                formattedDate.getYear(), formattedDate.getMonth()-1, formattedDate.getDay()
            )
        }
        binding.yearOnlyCheckbox.isChecked = viewModel.yearOnly

        checkIsValid()
        checkIsLoading()
    }

    // close fragment/activity after create/update success
    private fun closeAfterSuccess() {
        // set api state/button to normal
        viewModel.isApiLoading = false
        unlockViews()

        // show message + return to previous activity/fragment
        if(requireActivity().intent.getIntExtra("fragmentType", 0) == PetProfileActivity.FragmentType.PET_PROFILE_FROM_MY_PET.ordinal) {
            Toast.makeText(context, context?.getText(R.string.update_pet_successful), Toast.LENGTH_LONG).show()
            savePetDataForPetProfile()
            requireActivity().supportFragmentManager.popBackStack()
        }
        else {
            Toast.makeText(context, context?.getText(R.string.create_pet_successful), Toast.LENGTH_LONG).show()
            activity?.finish()
        }
    }

    private fun savePetDataForPetProfile() {
        // 사진이 기본 이미지일 때 예외 처리
        try{
            val bitmap = (binding.petPhotoInput.drawable as BitmapDrawable).bitmap
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val photoByteArray = stream.toByteArray()

            Util.saveByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
            requireContext().getString(R.string.data_name_my_pet_selected_pet_photo), photoByteArray)
        }catch(e: Exception){
            Util.saveByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
                requireContext().getString(R.string.data_name_my_pet_selected_pet_photo), null)
        }

        requireActivity().intent.putExtra("petBirth",
            FormattedDate(
                binding.petBirthInput.year,
                binding.petBirthInput.month + 1,
                binding.petBirthInput.dayOfMonth
            ).getFormattedString()
        )
        requireActivity().intent.putExtra("petPhotoRotation", viewModel.petPhotoRotation)
        requireActivity().intent.putExtra("petName", binding.petNameInput.text.toString())
        requireActivity().intent.putExtra("petSpecies", binding.petSpeciesInput.text.toString())
        requireActivity().intent.putExtra("petBreed", binding.petBreedInput.text.toString())
        requireActivity().intent.putExtra("petGender", binding.genderFemale.isChecked)
        requireActivity().intent.putExtra("petAge", LocalDate.now().year - binding.petBirthInput.year)
        requireActivity().intent.putExtra("petMessage", binding.petMessageInput.text.toString())
        requireActivity().intent.putExtra("yearOnly", binding.yearOnlyCheckbox.isChecked)
    }

    private fun deletePet() {
        // set api state/button to loading
        viewModel.isApiLoading = true

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .deletePetReq(DeletePetReqDto(requireActivity().intent.getLongExtra("petId", -1)))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            // set api state/button to normal
            viewModel.isApiLoading = false

            Toast.makeText(context, context?.getText(R.string.delete_pet_successful), Toast.LENGTH_LONG).show()
            activity?.finish()
        }, {
            viewModel.isApiLoading = false
        }, {
            viewModel.isApiLoading = false
        })
    }

    // for photo select
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // get + save pet photo value
        if (resultCode == AppCompatActivity.RESULT_OK && requestCode == PICK_PHOTO){
            if (data != null) {
                // check file size limit
                if (Util.isExceedsFileSizeLimit(requireContext(), data, FileType.FILE_SIZE_LIMIT_PHOTO)) {
                    Toast.makeText(context, context?.getText(R.string.file_size_limit_exception_message_20MB), Toast.LENGTH_SHORT).show()
                    return
                }

                // get file name
                val fileName = Util.getSelectedFileName(requireContext(), data.data!!)

                // copy selected photo and get real path
                val petPhotoPath = ServerUtil.createCopyAndReturnRealPathLocal(requireActivity(),
                    data.data!!, CREATE_UPDATE_PET_DIRECTORY, fileName)

                // file type exception -> delete copied file + show Toast message
                if (!Util.isUrlPhoto(petPhotoPath)) {
                    Toast.makeText(context, context?.getText(R.string.photo_file_type_exception_message), Toast.LENGTH_LONG).show()
                    File(petPhotoPath).delete()
                    return
                }

                // delete previously copied file(if any)
                if(viewModel.petPhotoPath != "") {
                    File(viewModel.petPhotoPath).delete()
                }

                // save path to ViewModel
                viewModel.petPhotoPath = petPhotoPath
                viewModel.isDeletePhoto = false

                // save rotation
                viewModel.petPhotoRotation = Util.getImageRotation(viewModel.petPhotoPath)

                // set photo to view
                Glide.with(requireContext()).load(BitmapFactory.decodeFile(viewModel.petPhotoPath)).into(binding.petPhotoInput)
                binding.petPhotoInput.rotation = viewModel.petPhotoRotation!!
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        isViewDestroyed = true

        // delete copied file(if any)
        if(isRemoving || requireActivity().isFinishing) {
            Util.deleteCopiedFiles(requireContext(), CREATE_UPDATE_PET_DIRECTORY)
        }

        viewModel.isApiLoading = false
    }
}