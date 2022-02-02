package com.sju18001.petmanagement.ui.map.review

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.FragmentReviewBinding
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dao.Review

class ReviewFragment : Fragment() {
    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReviewListAdapter

    private var placeId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        placeId = requireActivity().intent.getLongExtra("placeId", -1)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReviewBinding.inflate(inflater, container, false)

        initializeAdapter()

        return binding.root
    }

    private fun initializeAdapter() {
        adapter = ReviewListAdapter(arrayListOf(Review(
            1,
            Account(2, "asdasd", "fchopinof99@naver.com", "010-6426-1370", null, false, "asdasd", "/app/data/accounts/account_2/account_profile_photo_.jpg", null, 1, "fTKHB2AlQIWudipGmllj5c:APA91bE-eb4r-oayXX85aMJdrWXzsicmYdmc1mIXycF8t1o3LgJGegjT2uRd7gC2sOYCOTWwqSgGUljBKMKrIaheNh5abUdEUOjlqB9R_paSRRfpaWHm4Ghf_tpzZABNxs1Cr6Es-1OF", true),
            null,
            1,
            "무난한 동물병원이에요!",
            3,
            "2022-02-01T08:40:18",
            false,
            null
        )))
        binding.recyclerViewReview?.let{
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(activity)

            it.addOnScrollListener(object: RecyclerView.OnScrollListener() {

            })
        }

        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {

        })
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}