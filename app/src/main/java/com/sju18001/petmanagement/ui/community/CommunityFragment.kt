package com.sju18001.petmanagement.ui.community

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.ads.AdRequest
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.FragmentCommunityBinding
import com.sju18001.petmanagement.ui.community.post.PostFragment


class CommunityFragment : Fragment() {
    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private var postFragment: PostFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)

        if(childFragmentManager.findFragmentById(R.id.framelayout_postfragment) == null){
            postFragment = PostFragment()
            childFragmentManager
                .beginTransaction()
                .add(R.id.framelayout_postfragment, postFragment!!)
                .commit()
        }

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.fragment = this@CommunityFragment
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.adView.loadAd(AdRequest.Builder().build())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // 네비게이션 탭 전환 시 MainActivity에서 아래 전역 함수를 실행하게 됩니다.
    fun startAllVideos(){
        postFragment?.let{ it.startAllVideos() }
    }

    fun pauseAllVideos(){
        postFragment?.let{ it.pauseAllVideos() }
    }


    /** Databinding functions */
    fun onClickCreatePostButton() {
        postFragment?.let{ it.checkIfAccountHasPetAndStartCreatePostFragment() }
    }
}