package com.sju18001.petmanagement.ui.community.follow.following

import android.os.Bundle
import android.util.Log
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
import com.sju18001.petmanagement.databinding.FragmentFollowingBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.ui.community.follow.FollowActivity
import com.sju18001.petmanagement.ui.community.follow.FollowItem
import com.sju18001.petmanagement.ui.community.follow.FollowUnfollowButtonInterface
import com.sju18001.petmanagement.ui.community.follow.FollowViewModel

class FollowingFragment(private val initializeFollowerIdList: () -> Unit) : Fragment() {
    private lateinit var followViewModel: FollowViewModel

    private var _binding: FragmentFollowingBinding? = null
    private val binding get() = _binding!!

    private lateinit var followingAdapter: FollowingAdapter
    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFollowingBinding.inflate(inflater, container, false)
        isViewDestroyed = false

        initializeAdapter()
        setListenerOnView()

        return binding.root
    }

    private fun initializeAdapter() {
        followingAdapter = FollowingAdapter(requireContext(), followViewModel, object: FollowUnfollowButtonInterface {
            override fun updateFollowUnfollowButton() {
                initializeFollowerIdList.invoke()
            }
        })
        binding.followingRecyclerView.setHasFixedSize(true)
        binding.followingRecyclerView.adapter = followingAdapter
        binding.followingRecyclerView.layoutManager = LinearLayoutManager(activity)

        followingAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                setEmptyFollowingView(followingAdapter.itemCount)
            }
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                setEmptyFollowingView(followingAdapter.itemCount)
            }
        })
    }

    private fun setEmptyFollowingView(itemCount: Int){
        val visibility = if(itemCount != 0) View.GONE else View.VISIBLE
        binding.emptyFollowingList.visibility = visibility
    }

    private fun setListenerOnView() {
        binding.followingSwipeRefreshLayout.setOnRefreshListener {
            updateRecyclerView()
        }
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        followViewModel = ViewModelProvider(requireActivity(),
            SavedStateViewModelFactory(requireActivity().application, requireActivity())
        ).get(FollowViewModel::class.java)
    }


    override fun onResume() {
        super.onResume()
        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        var followingList = mutableListOf<FollowItem>()
        CustomProgressBar.addProgressBar(requireContext(), binding.fragmentFollowingParentLayout, 80, R.color.white)

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchFollowerReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            response.body()!!.followerList.map {
                val item = FollowItem(it.photoUrl != null, null, it.id, it.username,
                    it.nickname!!, true, it.representativePetId)
                followingList.add(item)
            }
            followingAdapter.setResult(followingList)

            // 추가로 TabLayout의 타이틀의 카운트도 변경합니다.
            followViewModel.followingTitle.value =
                    "${requireContext().getText(R.string.following_fragment_title)} ${followingList.size}"

            CustomProgressBar.removeProgressBar(binding.fragmentFollowingParentLayout)
            binding.followingSwipeRefreshLayout.isRefreshing = false
        }, {
            CustomProgressBar.removeProgressBar(binding.fragmentFollowingParentLayout)
            binding.followingSwipeRefreshLayout.isRefreshing = false
        }, {
            CustomProgressBar.removeProgressBar(binding.fragmentFollowingParentLayout)
            binding.followingSwipeRefreshLayout.isRefreshing = false
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        isViewDestroyed = true

        // for API cancel
        followingAdapter.onDestroy()
    }
}