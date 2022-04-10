package com.sju18001.petmanagement.ui.myPet.petManager.petProfile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.databinding.ActivityPetProfileBinding
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dao.Pet
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.ui.community.post.PostFragment

class PetProfileActivity : AppCompatActivity(){
    companion object {
        const val POST_FRAGMENT_TAG = "post_fragment"
        const val TRANSITION_DURATION = 300L
    }

    enum class ActivityType {
        PET_PROFILE, COMMUNITY
    }

    private lateinit var binding: ActivityPetProfileBinding

    private val viewModel: PetProfileViewModel by viewModels()
    private var isViewDestroyed = false

    private val startForUpdateResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            result.data?.let{
                viewModel.petPhotoByteArray.value = Util.getByteArrayFromSharedPreferences(baseContext,
                    baseContext.getString(R.string.pref_name_byte_arrays),
                    baseContext.getString(R.string.data_name_my_pet_selected_pet_photo)
                )

                viewModel.petPhotoRotation.value = it.getFloatExtra("petPhotoRotation", 0f)
                viewModel.petName.value = it.getStringExtra("petName")
                viewModel.petBirth.value = it.getStringExtra("petBirth")
                viewModel.petSpecies.value = it.getStringExtra("petSpecies")
                viewModel.petBreed.value = it.getStringExtra("petBreed")
                viewModel.petGender.value = it.getBooleanExtra("petGender", true)
                viewModel.petAge.value =  it.getIntExtra("petAge", 0)
                viewModel.petMessage.value = it.getStringExtra("petMessage")
                viewModel.yearOnly.value = it.getBooleanExtra("yearOnly", false)
            }
        }else if(result.resultCode == Activity.RESULT_CANCELED){
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBinding()
        initializeViewModel()
        isViewDestroyed = false

        supportActionBar?.hide()

        // PetProfile의 출처가 PetManager? Community?
        if (viewModel.activityType.value == ActivityType.PET_PROFILE.ordinal) {
            replacePostFragment()
        }else{
            // Spinner의 onItemSelected()에서 PostFragment를 추가하며,
            // Spinner를 초기화할 때 onItemSelected()가 호출되므로 따로 추가할 필요가 없습니다.
            setPetSpinner()
        }

        setObserversOfLiveData()
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pet_profile)

        binding.lifecycleOwner = this
        binding.activity = this@PetProfileActivity
        binding.viewModel = viewModel
    }

    private fun setObserversOfLiveData() {
        viewModel.isViewsDetailed.observe(this, { flag ->
            // ConstraintSet이 적용되는 내부 뷰는 Databinding이 적용되지 않으므로 따로 처리를 해줍니다.
            if(flag) setViewsDetailed()
            else setViewsNotDetailed()
        })

        viewModel.petPhotoByteArray.observe(this, { byteArray ->
            if(byteArray != null) {
                val bitmap = Util.getBitmapFromByteArray(viewModel.petPhotoByteArray.value!!)
                Glide.with(baseContext).load(bitmap).into(binding.circleimageviewPetprofilePetphoto)
                binding.circleimageviewPetprofilePetphoto.rotation = viewModel.petPhotoRotation.value!!
            }
            else {
                binding.circleimageviewPetprofilePetphoto.setImageDrawable(getDrawable(R.drawable.ic_baseline_pets_60_with_padding))
            }
        })

        viewModel.accountPhotoByteArray.observe(this, { byteArray ->
            if(byteArray != null) {
                val bitmap = Util.getBitmapFromByteArray(viewModel.accountPhotoByteArray.value!!)
                binding.accountPhoto.setImageBitmap(bitmap)
            }
            else {
                binding.accountPhoto.setImageDrawable(getDrawable(R.drawable.ic_baseline_account_circle_24))
            }
        })

        viewModel.petBirth.observe(this, {
            binding.textviewPetprofilePetbirth.text =
                if(viewModel.yearOnly.value == true) viewModel.petBirth.value!!.substring(0, 4) + "년생"
                else viewModel.petBirth.value!!.substring(2).replace('-', '.')
        })
    }

    private fun setViewsDetailed() {
        // Landscape(가로 모드)일 때는 Detailed view를 사용하지 않습니다.
        if(!isOrientationPortrait()) return

        // pet_info_layout 애니메이션
        TransitionManager.beginDelayedTransition(
            binding.constraintlayoutPetprofilePetinfo,
            AutoTransition().setDuration(TRANSITION_DURATION).setInterpolator(AccelerateDecelerateInterpolator())
        )
        ConstraintSet().apply{
            clone(baseContext, R.layout.layout_petprofiledetailedpetinfo)
        }.applyTo(binding.constraintlayoutPetprofilePetinfo)

        setViewsByViewModel()
    }

    private fun isOrientationPortrait(): Boolean{
        return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    private fun setViewsByViewModel() {
        binding.imageviewPetprofileRepresentativeicon.visibility =
            if (viewModel.isPetRepresentative.value == true) { View.VISIBLE } else { View.INVISIBLE }

        if (viewModel.isActivityTypePetProfile()) {
            binding.constraintlayoutPetprofilePetinfo.setBackgroundResource(R.drawable.pet_info_layout_background)
        }else{
            binding.constraintlayoutPetprofilePetinfo.setBackgroundResource(0)
        }
    }

    private fun setViewsNotDetailed() {
        // pet_info_layout 애니메이션
        TransitionManager.beginDelayedTransition(
            binding.constraintlayoutPetprofilePetinfo,
            ChangeBounds().setDuration(TRANSITION_DURATION).setInterpolator(AccelerateDecelerateInterpolator())
        )
        ConstraintSet().apply{
            clone(baseContext, R.layout.layout_petprofilepetinfo)
        }.applyTo(binding.constraintlayoutPetprofilePetinfo)

        setViewsByViewModel()
    }

    private fun initializeViewModel() {
        viewModel.activityType.value = intent.getIntExtra("activityType", 0)
        viewModel.isOrientationPortrait.value = isOrientationPortrait()
        viewModel.isViewsDetailed.value = viewModel.isOrientationPortrait.value

        initializeAuthorDataOfViewModel()
        initializePetDataOfViewModel()
    }

    private fun initializeAuthorDataOfViewModel() {
        viewModel.accountId = intent.getLongExtra("accountId", -1)
        viewModel.accountUsername = intent.getStringExtra("accountUsername").toString()
        viewModel.accountNickname = intent.getStringExtra("accountNickname").toString()
        viewModel.accountRepresentativePetId = intent.getLongExtra("representativePetId", -1)

        initializeAccountPhotoByteArrayOfViewModel()
        initializeIsFollowingOfViewModel()
    }

    private fun initializeIsFollowingOfViewModel() {
        // 현재 펫 프로필 페이지가 내 계정의 것일 경우: 팔로우 기능 없음
        if (viewModel.accountId == SessionManager.fetchLoggedInAccount(baseContext)!!.id) return

        binding.followUnfollowButton.visibility = View.VISIBLE
        viewModel.isApiLoading = true

        // 해당 계정을 내가 팔로잉하는지 체크 -> isFollowing 초기화
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchFollowerReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            for (follower in response.body()!!.followerList) {
                if (follower.id == viewModel.accountId) {
                    viewModel.isFollowing.value = true
                    viewModel.isApiLoading = false

                    return@enqueueApiCall
                }
            }

            viewModel.isFollowing.value = false
            viewModel.isApiLoading = false
        }, {
            viewModel.isApiLoading = false
        }, {
            viewModel.isApiLoading = false
        })
    }

    private fun initializeAccountPhotoByteArrayOfViewModel() {
        // 사진이 있는지 확인
        if (intent.getStringExtra("accountPhotoUrl").isNullOrEmpty()){
            viewModel.accountPhotoByteArray.value = null
        }else{
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .fetchAccountPhotoReq(FetchAccountPhotoReqDto(viewModel.accountId))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
                viewModel.accountPhotoByteArray.value = response.body()!!.bytes()
            }, {}, {})
        }
    }

    private fun initializePetDataOfViewModel() {
        viewModel.petId = intent.getLongExtra("petId", -1)
        viewModel.petPhotoRotation.value = intent.getFloatExtra("petPhotoRotation", 0f)
        viewModel.petName.value = intent.getStringExtra("petName").toString()
        viewModel.petBirth.value = intent.getStringExtra("petBirth").toString()
        viewModel.petSpecies.value = intent.getStringExtra("petSpecies").toString()
        viewModel.petBreed.value = intent.getStringExtra("petBreed").toString()
        viewModel.petGender.value = intent.getBooleanExtra("petGender", false)
        viewModel.petAge.value = intent.getIntExtra("petAge", 0)
        viewModel.petMessage.value = intent.getStringExtra("petMessage").toString()
        viewModel.yearOnly.value = intent.getBooleanExtra("yearOnly", false)
        viewModel.isPetRepresentative.value =
            if (viewModel.isActivityTypePetProfile()) intent.getBooleanExtra("isRepresentativePet", false)
            else (viewModel.petId == viewModel.accountRepresentativePetId)

        initializePetPhotoByteArrayOfViewModel()
    }

    private fun initializePetPhotoByteArrayOfViewModel() {
        if (viewModel.activityType.value == ActivityType.PET_PROFILE.ordinal) {
            viewModel.petPhotoByteArray.value = Util.getByteArrayFromSharedPreferences(baseContext,
                baseContext.getString(R.string.pref_name_byte_arrays),
                baseContext.getString(R.string.data_name_my_pet_selected_pet_photo)
            )
        }
        else {
            val petPhotoUrl = intent.getStringExtra("petPhotoUrl")
            if (petPhotoUrl.isNullOrEmpty()){
                viewModel.petPhotoByteArray.value = null
            }else{
                val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                    .fetchPetPhotoReq(FetchPetPhotoReqDto(intent.getLongExtra("petId", -1)))
                ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
                    viewModel.petPhotoByteArray.value = response.body()!!.bytes()
                }, {}, {})
            }
        }
    }


    private fun replacePostFragment() {
        // Remove previous fragment
        supportFragmentManager.findFragmentByTag(POST_FRAGMENT_TAG)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }

        // Fragment 추가
        val fragment = PostFragment.newInstance(viewModel.petId)
        supportFragmentManager
            .beginTransaction()
            .add(R.id.post_fragment_container, fragment, POST_FRAGMENT_TAG)
            .commit()

        binding.postFragmentContainer.post{
            addListenerOnRecyclerView()
        }
    }

    private fun addListenerOnRecyclerView(){
        val recyclerView = binding.postFragmentContainer.findViewById<RecyclerView>(R.id.recycler_view_post)

        // 터치를 시작할 때의 좌표를 기록함
        var x = 0f
        var y = 0f

        recyclerView.setOnTouchListener { v, event ->
            when (event.action){
                MotionEvent.ACTION_DOWN -> {
                    x = event.x
                    y = event.y
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 클릭 시(== 터치 이동 반경이 짧을 때)
                    if(kotlin.math.abs(x - event.x) < 10 && kotlin.math.abs(y - event.y) < 10){
                        viewModel.isViewsDetailed.value = viewModel.isViewsDetailed.value == false
                    }
                    // 스크롤 다운
                    else if(y > event.y){
                        viewModel.isViewsDetailed.value = false
                    }
                    // 스크롤 업 && 최상단에 위치
                    else if (y < event.y && !recyclerView.canScrollVertically(-1)) {
                        viewModel.isViewsDetailed.value = true
                    }
                    true
                }
            }

            v.performClick()
            v.onTouchEvent(event) ?: true
        }

        // 최상단에 위치할 시 VISIBLE
        recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
            if(!recyclerView.canScrollVertically(-1) && recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE){
                viewModel.isViewsDetailed.value = true
            }
        }
    }

    private fun setPetSpinner() {
        // 해당 계정의 Pet 정보를 불러옵니다.
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchPetReq(FetchPetReqDto(null, viewModel.accountUsername))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            response.body()?.petList?.let{ petList ->
                initializeSpinner(petList)

                // 해당 페이지의 펫을 선택합니다.
                binding.petNameSpinner.setSelection(
                    petList.indexOfFirst { it.id == viewModel.petId }
                )

                // 화면이 가로일 때에는 스피너가 안 보이기 때문에,
                // replacePostFragment()를 내부에서 호출하는, onItemSelected()가 호출되지 않습니다.
                if(viewModel.isOrientationPortrait.value == false) replacePostFragment()
            }
        }, {}, {})
    }

    private fun initializeSpinner(petList: List<Pet>) {
        val spinnerArray: ArrayList<String> = ArrayList()
        petList.map {
            // 대표펫인 경우에는 강조하여 표기합니다.
            if (it.id == viewModel.accountRepresentativePetId) { spinnerArray.add("[★] " + it.name) }
            else { spinnerArray.add(it.name) }
        }

        binding.petNameSpinner.adapter = ArrayAdapter(baseContext, R.layout.pet_name_spinner_item, spinnerArray)
        binding.petNameSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                replacePetProfile(petList[position])
                replacePostFragment()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun replacePetProfile(pet: Pet) {
        viewModel.petId = pet.id
        if (pet.photoUrl.isNullOrEmpty()) {
            viewModel.petPhotoByteArray.value = null
        }
        else {
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .fetchPetPhotoReq(FetchPetPhotoReqDto(viewModel.petId))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
                viewModel.petPhotoByteArray.value = response.body()!!.bytes()
            }, {}, {})
        }
        viewModel.petName.value = pet.name
        viewModel.petBirth.value = pet.birth.toString()
        viewModel.petSpecies.value = pet.species
        viewModel.petBreed.value = pet.breed
        viewModel.petGender.value = pet.gender
        viewModel.petAge.value = Util.getAgeFromBirth(pet.birth)
        viewModel.petMessage.value = pet.message.toString()
        viewModel.isPetRepresentative.value =
            (viewModel.petId == viewModel.accountRepresentativePetId)
        viewModel.yearOnly.value = (pet.yearOnly == true)
    }


    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
        viewModel.isApiLoading = false
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    /**
     * Databinding functions
     */
    fun onClickSetRepresentativeButton() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(viewModel.petName.value + baseContext?.getString(R.string.set_representative_message))
            .setPositiveButton(R.string.confirm) { _, _ -> setRepresentativePet() }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create().show()
    }

    private fun setRepresentativePet() {
        viewModel.isApiLoading = true

        // create DTO for API call
        val accountData = SessionManager.fetchLoggedInAccount(baseContext)!!
        val updateAccountReqDto = UpdateAccountReqDto(
            accountData.email,
            accountData.phone,
            accountData.nickname,
            accountData.marketing,
            accountData.userMessage,
            viewModel.petId,
            accountData.notification,
            accountData.mapSearchRadius
        )

        // update account
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .updateAccountReq(updateAccountReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            if (response.body()?._metadata?.status == true) {
                // update session(update representative pet id value)
                val account = Account(
                    accountData.id, accountData.username, accountData.email, accountData.phone, accountData.password,
                    accountData.marketing, accountData.nickname, accountData.photoUrl, accountData.userMessage,
                    viewModel.petId, accountData.fcmRegistrationToken, accountData.notification, accountData.mapSearchRadius
                )
                SessionManager.saveLoggedInAccount(baseContext, account)

                viewModel.isPetRepresentative.value = true
                viewModel.isApiLoading = false
            }
        }, {
            viewModel.isApiLoading = false
        }, {
            viewModel.isApiLoading = false
        })
    }

    fun onClickUpdatePetButton() {
        val intent = makeCreateUpdatePetActivityIntent()
        startForUpdateResult.launch(intent)
        overridePendingTransition(0, 0)
    }

    private fun makeCreateUpdatePetActivityIntent(): Intent {
        val intent = Intent(baseContext, CreateUpdatePetActivity::class.java)
        intent.putExtra("activityType", CreateUpdatePetActivity.ActivityType.UPDATE_PET.ordinal)
        intent.putExtra("petId", viewModel.petId)
        intent.putExtra("petPhotoByteArray", viewModel.petPhotoByteArray.value)
        intent.putExtra("petPhotoRotation", viewModel.petPhotoRotation.value)
        intent.putExtra("petMessage", viewModel.petMessage.value)
        intent.putExtra("petName", viewModel.petName.value)
        intent.putExtra("petGender", viewModel.petGender.value)
        intent.putExtra("petSpecies", viewModel.petSpecies.value)
        intent.putExtra("petBreed", viewModel.petBreed.value)
        intent.putExtra("yearOnly", viewModel.yearOnly.value)
        intent.putExtra("petBirth", viewModel.petBirth.value)
        intent.putExtra("originalPetPhotoSize", viewModel.petPhotoByteArray.value?.size)

        return intent
    }

    fun onClickFollowUnFollowButton() {
        if (viewModel.isFollowing.value == false) createFollow()
        else deleteFollow()
    }

    private fun createFollow() {
        viewModel.isApiLoading = true

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .createFollowReq(CreateFollowReqDto(viewModel.accountId!!))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            viewModel.isFollowing.value = true
            viewModel.isApiLoading = false
        }, {
            viewModel.isApiLoading = false
        }, {
            viewModel.isApiLoading = false
        })
    }

    private fun deleteFollow() {
        viewModel.isApiLoading = true

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .deleteFollowReq(DeleteFollowReqDto(viewModel.accountId!!))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            viewModel.isFollowing.value = false
            viewModel.isApiLoading = false
        }, {
            viewModel.isApiLoading = false
        }, {
            viewModel.isApiLoading = false
        })
    }

    fun onClickBackButton() {
        finish()
    }

    fun onClickHistoryText() {
        viewModel.isViewsDetailed.value = (viewModel.isViewsDetailed.value == false)
    }
}