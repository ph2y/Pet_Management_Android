package com.sju18001.petmanagement.ui.community.followerFollowing

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.PatternRegex
import com.sju18001.petmanagement.databinding.FragmentFollowerFollowingBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.ui.community.CommunityUtil

class FollowerFollowingFragment : Fragment() {

    // for shared ViewModel
    private lateinit var followerFollowingViewModel: FollowerFollowingViewModel

    // variables for view binding
    private var _binding: FragmentFollowerFollowingBinding? = null
    private val binding get() = _binding!!

    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // view binding
        _binding = FragmentFollowerFollowingBinding.inflate(inflater, container, false)
        isViewDestroyed = false
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initialize tab elements
        val TAB_ELEMENTS = listOf(requireContext().getText(R.string.follower_fragment_title),
            requireContext().getText(R.string.following_fragment_title))

        // initialize ViewPager2
        val tabLayout = binding.tabLayout
        val viewPager = binding.viewPager.also{
            it.adapter = FollowerFollowingCollectionAdapter(this)
            it.currentItem = requireActivity().intent.getIntExtra("pageIndex", 0)
            it.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    requireActivity().intent.putExtra("pageIndex", position)
                }
            })
        }

        TabLayoutMediator(tabLayout, viewPager){ tab, position ->
            tab.text = TAB_ELEMENTS[position]
        }.attach()

        // searchEditText listeners
        binding.searchEditText.setOnEditorActionListener{ _, _, _ ->
            checkPatternNicknameAndSearchAccount()
            true
        }
        binding.searchEditText.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                followerFollowingViewModel.searchEditText = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        // for follow unfollow button
        binding.followUnfollowButton.setOnClickListener {
            // set API/button state to loading
            followerFollowingViewModel.apiIsLoading = true
            setButtonState()

            // API call
            if(followerFollowingViewModel.accountId !in followerFollowingViewModel.followerIdList!!) {
                createFollow()
            }
            else {
                deleteFollow()
            }
        }

        // for hiding keyboard
        Util.setupViewsForHideKeyboard(requireActivity(), binding.fragmentFollowerFollowingParentLayout)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // initialize ViewModel
        followerFollowingViewModel = ViewModelProvider(requireActivity(),
            SavedStateViewModelFactory(requireActivity().application, requireActivity()))
            .get(FollowerFollowingViewModel::class.java)

        // observe live data and set title when change
        followerFollowingViewModel.getFollowerTitle().observe(viewLifecycleOwner, object: Observer<String> {
            override fun onChanged(followerTitle: String?) {
                binding.tabLayout.getTabAt(0)!!.text = followerTitle
            }
        })
        followerFollowingViewModel.getFollowingTitle().observe(viewLifecycleOwner, object: Observer<String> {
            override fun onChanged(followingTitle: String?) {
                binding.tabLayout.getTabAt(1)!!.text = followingTitle
            }
        })
    }

    override fun onStart() {
        super.onStart()

        // for search button
        binding.searchButton.setOnClickListener {
            // start search activity
            val searchActivityIntent = Intent(context, SearchActivity::class.java)
            startActivity(searchActivityIntent)
            requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }

        // for back button
        binding.backButton.setOnClickListener {
            activity?.finish()
        }

        // set tab layout titles
        setFollowerCount()
        setFollowingCount()

        // initialize follower id list
        if(followerFollowingViewModel.followerIdList == null) {
            updateFollowerIdList()
        }

        restoreState()
    }

    class FollowerFollowingCollectionAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when(position) {
                0 -> FollowerFragment()
                else -> FollowingFragment()
            }
        }
    }

    private fun setFollowerCount() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchFollowingReq(ServerUtil.getEmptyBody())

        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            val followerCount = response.body()!!.followingList.size

            // set follower count
            val followerText = requireContext().getText(R.string.follower_fragment_title).toString() +
                    ' ' + followerCount.toString()
            followerFollowingViewModel.setFollowerTitle(followerText)
        }, {}, {})
    }

    private fun setFollowingCount() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchFollowerReq(ServerUtil.getEmptyBody())

        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            val followingCount = response.body()!!.followerList.size

            // set following count
            val followingText = requireContext().getText(R.string.following_fragment_title).toString() +
                    ' ' + followingCount.toString()
            followerFollowingViewModel.setFollowingTitle(followingText)
        }, {}, {})
    }

    private fun checkPatternNicknameAndSearchAccount(){
        if(!PatternRegex.checkNicknameRegex(followerFollowingViewModel.searchEditText)) {
            Toast.makeText(requireContext(), getString(R.string.nickname_regex_exception_message), Toast.LENGTH_LONG).show()
        }
        else {
            // set api state/button to loading
            followerFollowingViewModel.apiIsLoading = true
            lockViews()

            searchAccount(followerFollowingViewModel.searchEditText)
        }
    }

    private fun searchAccount(nickname: String) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchAccountByNicknameReq(FetchAccountReqDto(null, null, nickname))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            // set api state/button to normal
            followerFollowingViewModel.apiIsLoading = false
            unlockViews()

            setAccountInfoViews(response.body()!!)
        }, {
            // set api state/button to normal
            followerFollowingViewModel.apiIsLoading = false
            unlockViews()
        }, {
            // set api state/button to normal
            followerFollowingViewModel.apiIsLoading = false
            unlockViews()
        })
    }

    private fun setAccountInfoViews(fetchAccountResDto: FetchAccountResDto) {
        // set layout's visibility to visible(if not already done)
        if(binding.accountInfoCardView.visibility != View.VISIBLE) {
            binding.accountInfoCardView.visibility = View.VISIBLE
        }

        // start pet profile
        binding.accountInfoCardView.setOnClickListener {
            CommunityUtil.fetchRepresentativePetAndStartPetProfile(requireContext(), Account(
                fetchAccountResDto.id, fetchAccountResDto.username, fetchAccountResDto.email, fetchAccountResDto.phone,
                "", fetchAccountResDto.marketing, fetchAccountResDto.nickname, fetchAccountResDto.photoUrl,
                fetchAccountResDto.userMessage, fetchAccountResDto.representativePetId, fetchAccountResDto.fcmRegistrationToken, fetchAccountResDto.notification), isViewDestroyed
            )
        }

        // if url is not null -> fetch photo and set it
        if(fetchAccountResDto.photoUrl != null) {
            followerFollowingViewModel.accountPhotoUrl = fetchAccountResDto.photoUrl
            fetchAccountPhoto(fetchAccountResDto.id)
        }
        // else -> reset photo related values
        else {
            followerFollowingViewModel.accountPhotoUrl = null
            followerFollowingViewModel.accountPhotoByteArray = null
            setAccountPhoto()
        }

        // save id value
        followerFollowingViewModel.accountId = fetchAccountResDto.id

        // save and set nickname value
        followerFollowingViewModel.accountNickname = fetchAccountResDto.nickname
        val nicknameText = followerFollowingViewModel.accountNickname + '님'
        binding.accountNickname.text = nicknameText

        setButtonState()
    }

    private fun fetchAccountPhoto(id: Long) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchAccountPhotoReq(FetchAccountPhotoReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            // save photo as byte array
            followerFollowingViewModel.accountPhotoByteArray = response.body()!!.byteStream().readBytes()

            setAccountPhoto()
        }, {}, {})
    }

    private fun setAccountPhoto() {
        if(followerFollowingViewModel.accountPhotoUrl != null) {
            val bitmap = Util.getBitmapFromByteArray(followerFollowingViewModel.accountPhotoByteArray!!)
            binding.accountPhoto.setImageBitmap(bitmap)
        }
        else {
            binding.accountPhoto.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_account_circle_24))
        }
    }

    private fun setButtonState() {
        // exception
        if(followerFollowingViewModel.accountId == null) { return }

        // for follow/unfollow button
        if (followerFollowingViewModel.accountId in followerFollowingViewModel.followerIdList!!) {
            binding.followUnfollowButton.setBackgroundColor(requireContext().getColor(R.color.border_line))
            binding.followUnfollowButton.setTextColor(requireContext().resources.getColor(R.color.black))
            binding.followUnfollowButton.text = getText(R.string.unfollow_button)
        } else {
            binding.followUnfollowButton.setBackgroundColor(requireContext().getColor(R.color.carrot))
            binding.followUnfollowButton.setTextColor(requireContext().resources.getColor(R.color.white))
            binding.followUnfollowButton.text = getText(R.string.follow_button)
        }

        // if API is loading -> set button to loading, else -> set button to normal
        binding.followUnfollowButton.isEnabled = !followerFollowingViewModel.apiIsLoading
    }

    private fun createFollow() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .createFollowReq(CreateFollowReqDto(followerFollowingViewModel.accountId!!))

        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            updateFollowerIdList()

            followerFollowingViewModel.apiIsLoading = false
        }, {
            // set api state/button to normal
            followerFollowingViewModel.apiIsLoading = false
            setButtonState()
        }, {})
    }

    private fun deleteFollow() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .deleteFollowReq(DeleteFollowReqDto(followerFollowingViewModel.accountId!!))

        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            updateFollowerIdList()

            followerFollowingViewModel.apiIsLoading = false
        }, {
            // set api state/button to normal
            followerFollowingViewModel.apiIsLoading = false
            setButtonState()
        }, {})
    }

    private fun updateFollowerIdList() {
        // reset list
        followerFollowingViewModel.followerIdList = mutableListOf()

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchFollowerReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            response.body()!!.followerList.map {
                followerFollowingViewModel.followerIdList!!.add(it.id)
            }

            // update button state
            setButtonState()
        }, {}, {})
    }

    private fun lockViews() {
        binding.searchEditText.isEnabled = false
        binding.searchButton.isEnabled = false
    }

    private fun unlockViews() {
        binding.searchEditText.isEnabled = true
        binding.searchButton.isEnabled = true
    }

    private fun restoreState() {
        // restore EditText
        binding.searchEditText.setText(followerFollowingViewModel.searchEditText)

        // restore account info layout
        if(followerFollowingViewModel.accountId != null) {
            binding.accountInfoCardView.visibility = View.VISIBLE

            // account photo
            if(followerFollowingViewModel.accountPhotoUrl != null) {
                val bitmap = Util.getBitmapFromByteArray(followerFollowingViewModel.accountPhotoByteArray!!)
                binding.accountPhoto.setImageBitmap(bitmap)
            }

            // account nickname
            val nicknameText = followerFollowingViewModel.accountNickname + '님'
            binding.accountNickname.text = nicknameText

            // follow unfollow button
            setButtonState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        isViewDestroyed = true
    }
}