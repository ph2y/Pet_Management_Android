package com.sju18001.petmanagement.ui.community.follow.follower

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.CustomProgressBar
import com.sju18001.petmanagement.databinding.FragmentFollowerBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.ui.community.follow.FollowActivity
import com.sju18001.petmanagement.ui.community.follow.FollowItem
import com.sju18001.petmanagement.ui.community.follow.FollowUnfollowButtonInterface
import com.sju18001.petmanagement.ui.community.follow.FollowViewModel

class FollowerFragment(private val initializeFollowerIdList: () -> Unit) : Fragment() {
    private lateinit var followViewModel: FollowViewModel

    private var _binding: FragmentFollowerBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: FollowerAdapter
    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFollowerBinding.inflate(inflater, container, false)
        isViewDestroyed = false

        followViewModel = ViewModelProvider(requireActivity(),
            SavedStateViewModelFactory(requireActivity().application, requireActivity())
        ).get(FollowViewModel::class.java)

        initializeAdapter()
        setListenerOnView()

        return binding.root
    }

    private fun initializeAdapter() {
        adapter = FollowerAdapter(requireContext(), object: FollowUnfollowButtonInterface {
            override fun updateFollowUnfollowButton() {
                initializeFollowerIdList.invoke()
            }
        })
        binding.followerRecyclerView.setHasFixedSize(true)
        binding.followerRecyclerView.adapter = adapter
        binding.followerRecyclerView.layoutManager = LinearLayoutManager(activity)

        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                setEmptyFollowerView(adapter.itemCount)
            }
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                setEmptyFollowerView(adapter.itemCount)
            }
        })
    }

    private fun setEmptyFollowerView(itemCount: Int){
        val visibility = if(itemCount != 0) View.GONE else View.VISIBLE
        binding.emptyFollowerList.visibility = visibility
    }

    private fun setListenerOnView(){
        binding.followerSwipeRefreshLayout.setOnRefreshListener {
            updateRecyclerView()
        }
    }


    override fun onResume() {
        super.onResume()
        updateRecyclerView()
    }

    // 팔로잉하는 유저들을 먼저 불러온 뒤 팔로워를 불러와야 합니다.
    // 내가 팔로잉 하는 유저의 리스트에 따라, 팔로워에 해당하는 아이템의 버튼이 달라져야 하기 때문입니다.
    fun updateRecyclerView() {
        // 최초 1회만 로딩바를 띄웁니다.
        if(adapter.itemCount == 0){
            CustomProgressBar.addProgressBar(requireContext(), binding.fragmentFollowerParentLayout, 80, R.color.white)
        }
        var followingIdList = mutableListOf<Long>()

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchFollowerReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            response.body()!!.followerList.map { followingIdList.add(it.id) }
            updateRecyclerViewByFollowingIdList(followingIdList)
        }, {}, {})
    }

    private fun updateRecyclerViewByFollowingIdList(followingIdList: MutableList<Long>) {
        var followerList = mutableListOf<FollowItem>()
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchFollowingReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            response.body()!!.followingList.map {
                val item = FollowItem(
                    it.photoUrl != null, null, it.id, it.username, it.nickname!!,
                    it.id in followingIdList, it.representativePetId
                )
                followerList.add(item)
            }
            adapter.setResult(followerList)

            // 추가로 TabLayout의 타이틀의 카운트도 변경합니다.
            followViewModel.followerTitle.value = "${requireContext().getText(R.string.follower_fragment_title)} ${followerList.size}"

            CustomProgressBar.removeProgressBar(binding.fragmentFollowerParentLayout)
            binding.followerSwipeRefreshLayout.isRefreshing = false
        }, {
            CustomProgressBar.removeProgressBar(binding.fragmentFollowerParentLayout)
            binding.followerSwipeRefreshLayout.isRefreshing = false
        }, {
            CustomProgressBar.removeProgressBar(binding.fragmentFollowerParentLayout)
            binding.followerSwipeRefreshLayout.isRefreshing = false
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        isViewDestroyed = true

        // for API cancel
        adapter.onDestroy()
    }
}