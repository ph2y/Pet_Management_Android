package com.sju18001.petmanagement.ui.community.follow

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.AdRequest
import com.google.android.material.tabs.TabLayoutMediator
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.PatternRegex
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.ActivityFollowBinding
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.ui.community.CommunityUtil
import com.sju18001.petmanagement.ui.community.follow.follower.FollowerFragment
import com.sju18001.petmanagement.ui.community.follow.following.FollowingFragment

class FollowActivity : AppCompatActivity() {
    private val viewModel: FollowViewModel by viewModels()
    private lateinit var binding: ActivityFollowBinding

    private var isViewDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBinding()

        isViewDestroyed = false
        supportActionBar?.hide()

        initializeViewPager()
        initializeFollowerIdList()

        Util.setupViewsForHideKeyboard(this, binding.constraintlayoutParent)

        binding.adView.loadAd(AdRequest.Builder().build())
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_follow)

        binding.lifecycleOwner = this
        binding.activity = this@FollowActivity
        binding.viewModel = viewModel
    }

    private fun initializeViewPager() {
        val tabLayout = binding.tablayoutFollow
        val viewPager = binding.viewpagerFollow.also{
            it.adapter = FollowCollectionAdapter(this) { initializeFollowerIdList() }
            it.currentItem = viewModel.pageIndex
            it.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    viewModel.pageIndex = position
                }
            })
        }

        val tabElements = listOf(getText(R.string.follower_fragment_title), getText(R.string.following_fragment_title))
        TabLayoutMediator(tabLayout, viewPager){ tab, position ->
            tab.text = tabElements[position]
        }.attach()
    }

    private fun initializeFollowerIdList() {
        viewModel.followerIdList.value = mutableListOf()
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchFollowerReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            response.body()!!.followerList.map { viewModel.followerIdList.value!!.add(it.id) }
        }, {}, {})
    }


    override fun onStart() {
        super.onStart()

        initializeFollowerCount()
        initializeFollowingCount()

        setListeners()
    }

    private fun initializeFollowerCount() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchFollowingReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            val followerCount = response.body()!!.followingList.size
            viewModel.followerTitle.value = "${getText(R.string.follower_fragment_title)} $followerCount"
        }, {}, {})
    }

    private fun initializeFollowingCount() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchFollowerReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            val followingCount = response.body()!!.followerList.size
            viewModel.followingTitle.value = "${getText(R.string.following_fragment_title)} $followingCount"
        }, {}, {})
    }

    private fun setListeners() {
        binding.edittextSearch.setOnEditorActionListener{ _, _, _ ->
            checkSearchKeywordAndSearchAccount()
            true
        }

        viewModel.followerTitle.observe(this, { followerTitle ->
            binding.tablayoutFollow.getTabAt(0)!!.text = followerTitle
        })
        viewModel.followingTitle.observe(this, { followingTitle ->
            binding.tablayoutFollow.getTabAt(1)!!.text = followingTitle
        })

        viewModel.accountPhotoUrl.observe(this, {
            if(it.isNullOrEmpty()) viewModel.accountPhotoByteArray.value = null
            else fetchAccountPhoto(viewModel.accountId.value!!)
        })

        viewModel.accountPhotoByteArray.observe(this, {
            if(it == null){
                val drawable = getDrawable(R.drawable.ic_baseline_account_circle_24)
                binding.circleimageviewPhoto.setImageDrawable(drawable)
            }else{
                val bitmap = Util.getBitmapFromByteArray(viewModel.accountPhotoByteArray.value!!)
                binding.circleimageviewPhoto.setImageBitmap(bitmap)
            }
        })
    }

    private fun checkSearchKeywordAndSearchAccount(){
        if(!PatternRegex.checkNicknameRegex(viewModel.searchKeyword.value)){
            Toast.makeText(baseContext, getString(R.string.nickname_regex_exception_message), Toast.LENGTH_LONG).show()
        }else{
            searchAccount(viewModel.searchKeyword.value!!)
        }
    }

    private fun searchAccount(nickname: String) {
        viewModel.isApiLoading.value = true
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchAccountByNicknameReq(FetchAccountReqDto(null, null, nickname))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            response.body()?.let{
                viewModel.accountId.value = it.id
                viewModel.accountUsername.value = it.username
                viewModel.accountNickname.value = it.nickname
                viewModel.accountRepresentativePetId.value = it.representativePetId
                viewModel.accountPhotoUrl.value = it.photoUrl
            }
            viewModel.isApiLoading.value = false
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }

    private fun fetchAccountPhoto(id: Long) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchAccountPhotoReq(FetchAccountPhotoReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            viewModel.accountPhotoByteArray.value = response.body()!!.byteStream().readBytes()
        }, {}, {})
    }


    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
    }


    /** Databinding Functions */
    fun onClickClearKeywordButton() {
        viewModel.searchKeyword.value = ""
    }

    fun onClickFollowUnfollowButton() {
        if(viewModel.accountId.value !in viewModel.followerIdList.value!!) createFollow()
        else deleteFollow()
    }

    private fun createFollow() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .createFollowReq(CreateFollowReqDto(viewModel.accountId.value!!))

        viewModel.isApiLoading.value = true
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            updateViewPager()

            viewModel.followerIdList.value!!.add(viewModel.accountId.value!!)
            // 리스너 호출용: add, remove는 데이터 변경 감지가 안 된다
            viewModel.followerIdList.value = viewModel.followerIdList.value

            viewModel.isApiLoading.value = false
        }, { viewModel.isApiLoading.value = false }, {})
    }

    private fun updateViewPager() {
        (supportFragmentManager.fragments[0] as FollowerFragment).updateRecyclerView()
        // 두번째 Fragment에 진입한 적이 없어서 초기화가 되지 않았을 경우를 대비한다
        if(supportFragmentManager.fragments.size-1 >= 1){
            (supportFragmentManager.fragments[1] as FollowingFragment).updateRecyclerView()
        }
    }

    private fun deleteFollow() {
        viewModel.isApiLoading.value = true
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .deleteFollowReq(DeleteFollowReqDto(viewModel.accountId.value!!))

        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            updateViewPager()

            viewModel.followerIdList.value!!.remove(viewModel.accountId.value)
            // 리스너 호출용: add, remove는 데이터 변경 감지가 안 된다
            viewModel.followerIdList.value = viewModel.followerIdList.value

            viewModel.isApiLoading.value = false
        }, { viewModel.isApiLoading.value = false }, {})
    }

    fun onClickAccountInfoCardView() {
        CommunityUtil.fetchRepresentativePetAndStartPetProfile(baseContext, Account(
            viewModel.accountId.value!!, viewModel.accountUsername.value!!,
            "", "", "", null, viewModel.accountNickname.value,
            viewModel.accountPhotoUrl.value, "", viewModel.accountRepresentativePetId.value,
            null, null, 0.0), isViewDestroyed
        )
    }

    fun onClickBackButton() {
        finish()
    }
}