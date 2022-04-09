package com.sju18001.petmanagement.ui.myPet.petManager.petProfile

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
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
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentPetProfileBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dao.Pet
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.ui.community.post.PostFragment
import com.sju18001.petmanagement.ui.myPet.MyPetViewModel

class PetProfileFragment : Fragment(){
    companion object {
        const val POST_FRAGMENT_TAG = "post_fragment"
        const val TRANSITION_DURATION = 300L
    }

    // variables for view binding
    private var _binding: FragmentPetProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PetProfileViewModel by viewModels()
    private var isViewDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setObserversOfLiveData()
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
                Glide.with(requireContext()).load(bitmap).into(binding.circleimageviewPetprofilePetphoto)
                binding.circleimageviewPetprofilePetphoto.rotation = viewModel.petPhotoRotation
            }
            else {
                binding.circleimageviewPetprofilePetphoto.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_baseline_pets_60_with_padding))
            }
        })

        viewModel.accountPhotoByteArray.observe(this, { byteArray ->
            if(byteArray != null) {
                val bitmap = Util.getBitmapFromByteArray(viewModel.accountPhotoByteArray.value!!)
                binding.accountPhoto.setImageBitmap(bitmap)
            }
            else {
                binding.accountPhoto.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_baseline_account_circle_24))
            }
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
            clone(context, R.layout.layout_petprofiledetailedpetinfo)
        }.applyTo(binding.constraintlayoutPetprofilePetinfo)

        setViewsByViewModel()
    }

    private fun isOrientationPortrait(): Boolean{
        return requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    private fun setViewsByViewModel() {
        binding.imageviewPetprofileRepresentativeicon.visibility =
            if (viewModel.isPetRepresentative.value == true) { View.VISIBLE } else { View.INVISIBLE }

        if (viewModel.isFragmentTypePetProfileFromMyPet()) {
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
            clone(context, R.layout.layout_petprofilepetinfo)
        }.applyTo(binding.constraintlayoutPetprofilePetinfo)

        setViewsByViewModel()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        setViewModel()
        isViewDestroyed = false

        loadAuthorDataByIntent()
        loadPetDataByIntent()

        // PetProfile의 출처가 PetManager? Community?
        if (viewModel.fragmentType.value == PetProfileActivity.FragmentType.PET_PROFILE_FROM_MY_PET.ordinal) {
            replacePostFragment()
        }else{
            // Spinner의 onItemSelected()에서 PostFragment를 추가하며,
            // Spinner를 초기화할 때 onItemSelected()가 호출되므로 따로 추가할 필요가 없습니다.
            setPetSpinner()
        }

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentPetProfileBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@PetProfileFragment
        binding.viewModel = viewModel
    }

    private fun setViewModel() {
        viewModel.fragmentType.value = requireActivity().intent.getIntExtra("fragmentType", 0)
        viewModel.isOrientationPortrait.value = isOrientationPortrait()
        viewModel.isViewsDetailed.value = viewModel.isOrientationPortrait.value
        viewModel.isFollowing.value = false
    }

    private fun loadAuthorDataByIntent() {
        viewModel.accountId = requireActivity().intent.getLongExtra("accountId", -1)
        viewModel.accountUsername = requireActivity().intent.getStringExtra("accountUsername").toString()
        val accountPhotoUrl = requireActivity().intent.getStringExtra("accountPhotoUrl")
        if (accountPhotoUrl.isNullOrEmpty()) {
            viewModel.accountPhotoByteArray.value = null
        }
        else {
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .fetchAccountPhotoReq(FetchAccountPhotoReqDto(viewModel.accountId))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                viewModel.accountPhotoByteArray.value = response.body()!!.bytes()
            }, {}, {})
        }
        viewModel.accountNickname = requireActivity().intent.getStringExtra("accountNickname").toString()
        viewModel.accountRepresentativePetId = requireActivity().intent.getLongExtra("representativePetId", -1)
    }

    private fun loadPetDataByIntent() {
        viewModel.petId = requireActivity().intent.getLongExtra("petId", -1)
        if (viewModel.fragmentType.value == PetProfileActivity.FragmentType.PET_PROFILE_FROM_MY_PET.ordinal) {
            viewModel.petPhotoByteArray.value = Util.getByteArrayFromSharedPreferences(requireContext(),
                requireContext().getString(R.string.pref_name_byte_arrays),
                requireContext().getString(R.string.data_name_my_pet_selected_pet_photo))
        }
        else {
            val petPhotoUrl = requireActivity().intent.getStringExtra("petPhotoUrl")
            if (petPhotoUrl.isNullOrEmpty()) {
                viewModel.petPhotoByteArray.value = null
            }
            else {
                val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                    .fetchPetPhotoReq(FetchPetPhotoReqDto(requireActivity().intent.getLongExtra("petId", -1)))
                ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                    viewModel.petPhotoByteArray.value = response.body()!!.bytes()
                }, {}, {})
            }
        }
        viewModel.petPhotoRotation = requireActivity().intent.getFloatExtra("petPhotoRotation", 0f)
        viewModel.petName = requireActivity().intent.getStringExtra("petName").toString()
        viewModel.petBirth = requireActivity().intent.getStringExtra("petBirth").toString()
        viewModel.petSpecies = requireActivity().intent.getStringExtra("petSpecies").toString()
        viewModel.petBreed = requireActivity().intent.getStringExtra("petBreed").toString()
        viewModel.petGender = requireActivity().intent.getBooleanExtra("petGender", false)
        viewModel.petAge = requireActivity().intent.getIntExtra("petAge", 0)
        viewModel.petMessage = requireActivity().intent.getStringExtra("petMessage").toString()
        viewModel.yearOnly = requireActivity().intent.getBooleanExtra("yearOnly", false)
        if (viewModel.fragmentType.value == PetProfileActivity.FragmentType.PET_PROFILE_FROM_MY_PET.ordinal) {
            viewModel.isPetRepresentative.value = requireActivity().intent.getBooleanExtra("isRepresentativePet", false)
        }
        else {
            viewModel.isPetRepresentative.value =
                (viewModel.petId == viewModel.accountRepresentativePetId)
        }
    }

    private fun replacePostFragment() {
        // Remove previous fragment
        childFragmentManager.findFragmentByTag(POST_FRAGMENT_TAG)?.let {
            childFragmentManager.beginTransaction().remove(it).commit()
        }

        // Fragment 추가
        val fragment = PostFragment.newInstance(viewModel.petId)
        childFragmentManager
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
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPetReq(FetchPetReqDto(null, viewModel.accountUsername))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
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

        binding.petNameSpinner.adapter = ArrayAdapter(requireContext(), R.layout.pet_name_spinner_item, spinnerArray)
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
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .fetchPetPhotoReq(FetchPetPhotoReqDto(viewModel.petId))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                viewModel.petPhotoByteArray.value = response.body()!!.bytes()
            }, {}, {})
        }
        viewModel.petName = pet.name
        viewModel.petBirth = pet.birth.toString()
        viewModel.petSpecies = pet.species
        viewModel.petBreed = pet.breed
        viewModel.petGender = pet.gender
        viewModel.petAge = Util.getAgeFromBirth(pet.birth)
        viewModel.petMessage = pet.message.toString()
        viewModel.isPetRepresentative.value =
            (viewModel.petId == viewModel.accountRepresentativePetId)
        viewModel.yearOnly = (pet.yearOnly == true)
    }


    override fun onResume() {
        super.onResume()
        initializeFollowUnfollowButton()
    }

    private fun initializeFollowUnfollowButton() {
        // 현재 펫 프로필 페이지가 내 계정의 것일 경우
        if (viewModel.accountId == SessionManager.fetchLoggedInAccount(requireContext())!!.id) return

        binding.followUnfollowButton.visibility = View.VISIBLE
        viewModel.isApiLoading = true

        // 해당 계정을 내가 팔로잉하는지 체크 -> isFollowing 세팅
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchFollowerReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        isViewDestroyed = true
        viewModel.isApiLoading = false
    }


    /**
     * Databinding functions
     */
    fun onClickSetRepresentativeButton() {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(viewModel.petName + context?.getString(R.string.set_representative_message))
            .setPositiveButton(R.string.confirm) { _, _ -> setRepresentativePet() }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create().show()
    }

    private fun setRepresentativePet() {
        viewModel.isApiLoading = true

        // create DTO for API call
        val accountData = SessionManager.fetchLoggedInAccount(requireContext())!!
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
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .updateAccountReq(updateAccountReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            if (response.body()?._metadata?.status == true) {
                // update session(update representative pet id value)
                val account = Account(
                    accountData.id, accountData.username, accountData.email, accountData.phone, accountData.password,
                    accountData.marketing, accountData.nickname, accountData.photoUrl, accountData.userMessage,
                    viewModel.petId, accountData.fcmRegistrationToken, accountData.notification, accountData.mapSearchRadius
                )
                SessionManager.saveLoggedInAccount(requireContext(), account)

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
        val fragment = CreateUpdatePetFragment()
        fragment.arguments = makeBundleOfUpdatePetFragment()

        activity?.supportFragmentManager?.beginTransaction()!!
            .replace(R.id.framelayout_petprofile_fragmentcontainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun makeBundleOfUpdatePetFragment(): Bundle {
        val bundle = Bundle()
        bundle.putByteArray("petPhotoByteArray", viewModel.petPhotoByteArray.value)
        bundle.putFloat("petPhotoRotation", viewModel.petPhotoRotation)
        bundle.putString("petMessage", viewModel.petMessage)
        bundle.putString("petName", viewModel.petName)
        bundle.putBoolean("petGender", viewModel.petGender)
        bundle.putString("petSpecies", viewModel.petSpecies)
        bundle.putString("petBreed", viewModel.petBreed)
        bundle.putBoolean("yearOnly", viewModel.yearOnly)
        bundle.putString("petBirth", viewModel.petBirth)

        return bundle
    }

    fun onClickFollowUnFollowButton() {
        if (viewModel.isFollowing.value == false) createFollow()
        else deleteFollow()
    }

    private fun createFollow() {
        viewModel.isApiLoading = true

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .createFollowReq(CreateFollowReqDto(viewModel.accountId!!))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
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

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .deleteFollowReq(DeleteFollowReqDto(viewModel.accountId!!))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            viewModel.isFollowing.value = false
            viewModel.isApiLoading = false
        }, {
            viewModel.isApiLoading = false
        }, {
            viewModel.isApiLoading = false
        })
    }

    fun onClickBackButton() {
        activity?.finish()
    }

    fun onClickHistoryText() {
        viewModel.isViewsDetailed.value = (viewModel.isViewsDetailed.value == false)
    }

    fun getBirthString(): String {
        return if(viewModel.yearOnly) viewModel.petBirth.substring(0, 4) + "년생"
        else viewModel.petBirth.substring(2).replace('-', '.')
    }

    fun getPetGenderString() = Util.getGenderSymbol(viewModel.petGender, requireContext())

    fun getPetAgeString() = "${viewModel.petAge}살"

    fun getPetMessageString(): String {
        return if(viewModel.petMessage.isNullOrEmpty()) getString(R.string.filled_heart)
        else viewModel.petMessage
    }
}