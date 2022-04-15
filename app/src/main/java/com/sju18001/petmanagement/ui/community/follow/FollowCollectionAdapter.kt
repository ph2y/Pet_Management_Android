package com.sju18001.petmanagement.ui.community.follow

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sju18001.petmanagement.ui.community.follow.follower.FollowerFragment
import com.sju18001.petmanagement.ui.community.follow.following.FollowingFragment

/**
 * initializeFollowerIdList: 하위 프래그먼트에서 이 작업을 수행할 일이 있기 때문에 전달해야 합니다.
 */

class FollowCollectionAdapter(
    fragmentActivity: FragmentActivity,
    private val initializeFollowerIdList: () -> Unit
): FragmentStateAdapter(fragmentActivity){
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> FollowerFragment(initializeFollowerIdList)
            else -> FollowingFragment(initializeFollowerIdList)
        }
    }
}