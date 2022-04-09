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

    private var isFollowing: Boolean = false


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

        // Pet manager / Community에 따라 초기 뷰 세팅을 달리 함
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
    }

    private fun loadAuthorDataByIntent() {
        viewModel.accountId = requireActivity().intent.getLongExtra("accountId", -1)
        viewModel.accountUsername = requireActivity().intent.getStringExtra("accountUsername").toString()
        val accountPhotoUrl = requireActivity().intent.getStringExtra("accountPhotoUrl")
        if (accountPhotoUrl.isNullOrEmpty()) {
            viewModel.accountPhotoByteArray = null
        }
        else {
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .fetchAccountPhotoReq(FetchAccountPhotoReqDto(viewModel.accountId))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                viewModel.accountPhotoByteArray = response.body()!!.bytes()
                setPhotoViews()
            }, {}, {})
        }
        viewModel.accountNickname = requireActivity().intent.getStringExtra("accountNickname").toString()
        viewModel.accountRepresentativePetId = requireActivity().intent.getLongExtra("representativePetId", -1)
    }

    private fun loadPetDataByIntent() {
        viewModel.petId = requireActivity().intent.getLongExtra("petId", -1)
        if (viewModel.fragmentType.value == PetProfileActivity.FragmentType.PET_PROFILE_FROM_MY_PET.ordinal) {
            viewModel.petPhotoByteArray = Util.getByteArrayFromSharedPreferences(requireContext(),
                requireContext().getString(R.string.pref_name_byte_arrays),
                requireContext().getString(R.string.data_name_my_pet_selected_pet_photo))

            setPhotoViews()
        }
        else {
            val petPhotoUrl = requireActivity().intent.getStringExtra("petPhotoUrl")
            if (petPhotoUrl.isNullOrEmpty()) {
                viewModel.petPhotoByteArray = null
            }
            else {
                val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                    .fetchPetPhotoReq(FetchPetPhotoReqDto(requireActivity().intent.getLongExtra("petId", -1)))
                ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                    viewModel.petPhotoByteArray = response.body()!!.bytes()
                    setPhotoViews()
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
        // remove previous fragment
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

    private fun setPetSpinner() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPetReq(FetchPetReqDto(null, viewModel.accountUsername))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            // get pet id and name
            val pets: MutableList<Pet> = mutableListOf()
            response.body()?.petList?.map {
                val item = Pet(
                    it.id, "", it.name, it.species, it.breed, it.birth,
                    it.yearOnly, it.gender, it.message, it.photoUrl
                )
                pets.add(item)
            }

            // set spinner and pet id values
            val spinnerArray: ArrayList<String> = ArrayList()
            for (pet in pets) {
                if (pet.id == viewModel.accountRepresentativePetId) {
                    spinnerArray.add("[★] " + pet.name)
                }
                else {
                    spinnerArray.add(pet.name)
                }
            }

            // set spinner adapter
            val spinnerArrayAdapter: ArrayAdapter<String> =
                ArrayAdapter<String>(requireContext(), R.layout.pet_name_spinner_item, spinnerArray)
            binding.petNameSpinner.adapter = spinnerArrayAdapter

            // set spinner listener
            binding.petNameSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.petId = pets[position].id
                    replacePetProfile(pets[position])
                    replacePostFragment()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // set spinner position
            for(i in 0 until pets.size) {
                if (viewModel.petId == pets[i].id) {
                    binding.petNameSpinner.setSelection(i)
                    break
                }
            }

            // 화면이 가로일 때에는 스피너가 안 보이므로, onItemSelected이 호출되지 않는다.
            if(viewModel.isOrientationPortrait.value == false) replacePostFragment()
        }, {}, {})
    }

    override fun onStart() {
        super.onStart()

        viewModel.isViewsDetailed.observe(this, { flag ->
            setViewsForDetail(flag)
        })

        viewModel.isPetRepresentative.observe(this, { flag ->
            binding.imageviewPetprofileRepresentativeicon.visibility =
                if (flag) { View.VISIBLE } else { View.INVISIBLE }
        })

        viewModel.fragmentType.observe(this, {
            if (it == PetProfileActivity.FragmentType.PET_PROFILE_FROM_MY_PET.ordinal) {
                binding.constraintlayoutPetprofilePetinfo.setBackgroundResource(R.drawable.pet_info_layout_background)
            }else{
                binding.constraintlayoutPetprofilePetinfo.setBackgroundResource(0)
            }
        })

        // for pet update button
        if (viewModel.fragmentType.value == PetProfileActivity.FragmentType.PET_PROFILE_FROM_MY_PET.ordinal) {
            binding.updatePetButton.setOnClickListener {
                val fragment = CreateUpdatePetFragment()
                fragment.arguments = makeBundleOfUpdatePetFragment()

                activity?.supportFragmentManager?.beginTransaction()!!
                    .replace(R.id.framelayout_petprofile_fragmentcontainer, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        // for follow/unfollow button
        if (viewModel.fragmentType.value == PetProfileActivity.FragmentType.PET_PROFILE_FROM_COMMUNITY.ordinal) {
            binding.followUnfollowButton.setOnClickListener {
                if (isFollowing) {
                    deleteFollow()
                } else {
                    createFollow()
                }
            }
        }

        // for back button
        binding.backButton.setOnClickListener {
            activity?.finish()
        }

        // for history text
        binding.textHistory.setOnClickListener {
            viewModel.isViewsDetailed.value = viewModel.isViewsDetailed.value == false
        }

        // for pet information layout
        binding.constraintlayoutPetprofilePetinfo.setOnClickListener {
            viewModel.isViewsDetailed.value = true
        }
    }

    override fun onResume() {
        super.onResume()

        // set views with data from ViewModel
        setViewsWithPetData()
        setViewsWithAuthorData()

        // set follow/unfollow button
        // placed here (onResume) for consistency when returning to the same account's pet profile
        if (viewModel.fragmentType.value == PetProfileActivity.FragmentType.PET_PROFILE_FROM_COMMUNITY.ordinal) {
            setFollowUnfollowButton()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        isViewDestroyed = true
        viewModel.isApiLoading = false
    }

    private fun replacePetProfile(pet: Pet) {
        if (pet.photoUrl.isNullOrEmpty()) {
            viewModel.petPhotoByteArray = null
        }
        else {
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .fetchPetPhotoReq(FetchPetPhotoReqDto(viewModel.petId))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                viewModel.petPhotoByteArray = response.body()!!.bytes()
                setPhotoViews()
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

        setViewsWithPetData()
    }

    private fun setFollowUnfollowButton() {
        if (viewModel.accountId == SessionManager.fetchLoggedInAccount(requireContext())!!.id) {
            return
        }

        viewModel.isApiLoading = true
        binding.followUnfollowButton.isEnabled = false

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchFollowerReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            for (follower in response.body()!!.followerList) {
                if (follower.id == viewModel.accountId) {
                    isFollowing = true

                    binding.followUnfollowButton.setBackgroundColor(requireContext().getColor(R.color.border_line))
                    binding.followUnfollowButton.setTextColor(requireContext().resources.getColor(R.color.black))
                    binding.followUnfollowButton.text = requireContext().getText(R.string.unfollow_button)

                    viewModel.isApiLoading = false
                    binding.followUnfollowButton.isEnabled = true
                    binding.followUnfollowButton.visibility = View.VISIBLE

                    return@enqueueApiCall
                }
            }

            isFollowing = false

            binding.followUnfollowButton.setBackgroundColor(requireContext().getColor(R.color.carrot))
            binding.followUnfollowButton.setTextColor(requireContext().resources.getColor(R.color.white))
            binding.followUnfollowButton.text = requireContext().getText(R.string.follow_button)

            viewModel.isApiLoading = false
            binding.followUnfollowButton.isEnabled = true
            binding.followUnfollowButton.visibility = View.VISIBLE
        }, {
            viewModel.isApiLoading = false
            binding.followUnfollowButton.isEnabled = true
        }, {
            viewModel.isApiLoading = false
            binding.followUnfollowButton.isEnabled = true
        })
    }

    private fun createFollow() {
        viewModel.isApiLoading = true
        binding.followUnfollowButton.isEnabled = false

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .createFollowReq(CreateFollowReqDto(viewModel.accountId!!))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            isFollowing = true
            setFollowUnfollowButton()

            viewModel.isApiLoading = false
            binding.followUnfollowButton.isEnabled = true
        }, {
            viewModel.isApiLoading = false
            binding.followUnfollowButton.isEnabled = true
        }, {
            viewModel.isApiLoading = false
            binding.followUnfollowButton.isEnabled = true
        })
    }

    private fun deleteFollow() {
        viewModel.isApiLoading = true
        binding.followUnfollowButton.isEnabled = false

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .deleteFollowReq(DeleteFollowReqDto(viewModel.accountId!!))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            isFollowing = false
            setFollowUnfollowButton()

            viewModel.isApiLoading = false
            binding.followUnfollowButton.isEnabled = true
        }, {
            viewModel.isApiLoading = false
            binding.followUnfollowButton.isEnabled = true
        }, {
            viewModel.isApiLoading = false
            binding.followUnfollowButton.isEnabled = true
        })
    }

    private fun setPhotoViews() {
        if(viewModel.petPhotoByteArray != null) {
            val bitmap = Util.getBitmapFromByteArray(viewModel.petPhotoByteArray!!)
            Glide.with(requireContext()).load(bitmap).into(binding.circleimageviewPetprofilePetphoto)
            binding.circleimageviewPetprofilePetphoto.rotation = viewModel.petPhotoRotation
        }
        else {
            binding.circleimageviewPetprofilePetphoto.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_baseline_pets_60_with_padding))
        }

        if(viewModel.accountPhotoByteArray != null) {
            val bitmap = Util.getBitmapFromByteArray(viewModel.accountPhotoByteArray!!)
            binding.accountPhoto.setImageBitmap(bitmap)
        }
        else {
            binding.accountPhoto.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_baseline_account_circle_24))
        }
    }

    private fun setViewsWithPetData() {
        setPhotoViews()
        binding.textviewPetprofilePetname.text = viewModel.petName
        binding.textviewPetprofilePetbirth.text =
            if(viewModel.yearOnly) viewModel.petBirth.substring(0, 4) + "년생"
            else viewModel.petBirth.substring(2).replace('-', '.')
        binding.textviewPetprofilePetbreed.text = viewModel.petBreed
        binding.textviewPetprofilePetgender.text = Util.getGenderSymbol(viewModel.petGender, requireContext())
        binding.textviewPetprofilePetage.text = "${viewModel.petAge}살"
        val message = if(viewModel.petMessage.isNullOrEmpty()) getString(R.string.filled_heart) else viewModel.petMessage
        binding.textviewPetprofilePetmessage.text = "\" $message \""
    }

    private fun setViewsWithAuthorData() {
        setPhotoViews()
        binding.accountNickname.text = viewModel.accountNickname
    }

    private fun makeBundleOfUpdatePetFragment(): Bundle {
        val bundle = Bundle()
        bundle.putByteArray("petPhotoByteArray", viewModel.petPhotoByteArray)
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

    private fun setRepresentativePet() {
        // set api state/button to loading
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

                // update flag and related views
                viewModel.isPetRepresentative.value = true

                binding.imageviewPetprofileRepresentativeicon.visibility = View.VISIBLE
                binding.imageviewPetprofileRepresentativeicon.setImageResource(R.drawable.crown)
                binding.imageviewPetprofileRepresentativeicon.scaleType = ImageView.ScaleType.FIT_XY

                // set api state/button to normal
                viewModel.isApiLoading = false
            }
        }, {
            // set api state/button to normal
            viewModel.isApiLoading = false
        }, {
            // set api state/button to normal
            viewModel.isApiLoading = false
        })
    }

    @SuppressLint("ClickableViewAccessibility")
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

    private fun setViewsForDetail(flag: Boolean) {
        if(flag){
            setViewsDetailed()
        }else{
            setViewsNotDetailed()
        }
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

    // ConstraintSet이 적용되는 내부 뷰는 Databinding이 적용되지 않으므로
    // 따로 처리를 해주어야 합니다.
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


    /**
     * Databinding functions
     */
    fun onClickSetRepresentativeButton() {
        val builder = AlertDialog.Builder(activity)
        builder.setMessage(viewModel.petName + context?.getString(R.string.set_representative_message))
            .setPositiveButton(
                R.string.confirm
            ) { _, _ ->
                setRepresentativePet()
            }
            .setNegativeButton(
                R.string.cancel
            ) { dialog, _ ->
                dialog.cancel()
            }
            .create().show()
    }
}