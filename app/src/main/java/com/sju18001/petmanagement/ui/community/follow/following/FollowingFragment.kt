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
    // for shared ViewModel
    private lateinit var followViewModel: FollowViewModel

    // variables for view binding
    private var _binding: FragmentFollowingBinding? = null
    private val binding get() = _binding!!

    // variables for RecyclerView
    private lateinit var followingAdapter: FollowingAdapter
    private var followingList: MutableList<FollowItem> = mutableListOf()

    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // view binding
        _binding = FragmentFollowingBinding.inflate(inflater, container, false)
        isViewDestroyed = false

        val root = binding.root

        // for swipe refresh
        binding.followingSwipeRefreshLayout.setOnRefreshListener {
            fetchFollowing()
        }

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // initialize ViewModel
        followViewModel = ViewModelProvider(requireActivity(),
            SavedStateViewModelFactory(requireActivity().application, requireActivity())
        ).get(FollowViewModel::class.java)

        // initialize RecyclerView
        followingAdapter = FollowingAdapter(requireContext(), followViewModel, object: FollowUnfollowButtonInterface {
            override fun updateFollowUnfollowButton() {
                initializeFollowerIdList.invoke()
            }
        })
        binding.followingRecyclerView.setHasFixedSize(true)
        binding.followingRecyclerView.adapter = followingAdapter
        binding.followingRecyclerView.layoutManager = LinearLayoutManager(activity)

        // Set adapter item change observer
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

    override fun onResume() {
        super.onResume()

        // show progressbar for the first fetch
        if (followingList.isEmpty()) {
            CustomProgressBar.addProgressBar(requireContext(), binding.fragmentFollowingParentLayout, 80, R.color.white)
        }

        fetchFollowing()
    }

    private fun setEmptyFollowingView(itemCount: Int){
        val visibility = if(itemCount != 0) View.GONE else View.VISIBLE
        binding.emptyFollowingList.visibility = visibility
    }

    fun fetchFollowing() {
        // reset list
        followingList = mutableListOf()

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchFollowerReq(ServerUtil.getEmptyBody())

        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            response.body()!!.followerList.map {
                val hasPhoto = it.photoUrl != null
                val id = it.id
                val username = it.username
                val nickname = it.nickname
                val representativePetId = it.representativePetId

                val item = FollowItem()
                item.setValues(hasPhoto, null, id, username, nickname!!, true, representativePetId)
                followingList.add(item)
            }

            // set following count
            followViewModel.followingTitle.value =
                    "${requireContext().getText(R.string.following_fragment_title)} ${followingList.size}"

            // set RecyclerView
            followingAdapter.setResult(followingList)

            // set swipe isRefreshing to false
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

        // call onDestroy inside adapter(for API cancel)
        followingAdapter.onDestroy()

        isViewDestroyed = true
    }
}