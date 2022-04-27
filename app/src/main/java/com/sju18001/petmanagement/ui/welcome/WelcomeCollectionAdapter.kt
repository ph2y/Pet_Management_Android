package com.sju18001.petmanagement.ui.welcome

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class WelcomeCollectionAdapter(fragmentActivity: FragmentActivity)
    : FragmentStateAdapter(fragmentActivity){
    override fun getItemCount(): Int = 1

    override fun createFragment(position: Int): Fragment {
        return WelcomeProfileFragment()
    }
}