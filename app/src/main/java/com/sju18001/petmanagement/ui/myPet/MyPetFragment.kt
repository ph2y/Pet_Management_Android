package com.sju18001.petmanagement.ui.myPet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.FragmentMypetBinding
import com.sju18001.petmanagement.ui.myPet.petManager.PetManagerFragment
import com.sju18001.petmanagement.ui.myPet.petScheduleManager.PetScheduleManagerFragment


class MyPetFragment : Fragment() {
    private var _binding: FragmentMypetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMypetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = binding.tablayout
        val viewPager = binding.viewpager.also{
            it.adapter = MyPetCollectionAdapter(this)
            it.currentItem = requireActivity().intent.getIntExtra("pageIndex", 0)
            it.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    requireActivity().intent.putExtra("pageIndex", position)
                }
            })
            it.isUserInputEnabled = false
        }

        val tabElements = listOf(requireContext().getText(R.string.pet_manager_title), requireContext().getText(R.string.pet_schedule_manager_title))
        TabLayoutMediator(tabLayout, viewPager){ tab, position ->
            tab.text = tabElements[position]
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class MyPetCollectionAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when(position) {
                0 -> PetManagerFragment()
                else -> PetScheduleManagerFragment()
            }
        }
    }
}