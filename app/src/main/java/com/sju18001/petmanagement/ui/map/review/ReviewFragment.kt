package com.sju18001.petmanagement.ui.map.review

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentReviewBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dao.Review
import com.sju18001.petmanagement.restapi.dto.FetchAccountPhotoReqDto

class ReviewFragment : Fragment() {
    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReviewListAdapter

    private var isViewDestroyed = false

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
        isViewDestroyed = false

        initializeAdapter()

        return binding.root
    }

    private fun initializeAdapter() {
        val tmp = Review(
            1,
            Account(2, "asdasd", "fchopinof99@naver.com", "010-6426-1370", null, false, "asdasd", "/app/data/accounts/account_2/account_profile_photo_.jpg", null, 1, "fTKHB2AlQIWudipGmllj5c:APA91bE-eb4r-oayXX85aMJdrWXzsicmYdmc1mIXycF8t1o3LgJGegjT2uRd7gC2sOYCOTWwqSgGUljBKMKrIaheNh5abUdEUOjlqB9R_paSRRfpaWHm4Ghf_tpzZABNxs1Cr6Es-1OF", true),
            null,
            1,
            "무난한 동물병원이에요!",
            3,
            "2022-02-01T08:40:18",
            false,
            null
        )
        adapter = ReviewListAdapter(arrayListOf(tmp, tmp), requireContext())

        adapter.reviewListAdapterInterface = object: ReviewListAdapterInterface {
            override fun setAccountPhoto(id: Long, holder: ReviewListAdapter.ViewHolder) {
                val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                    .fetchAccountPhotoReq(FetchAccountPhotoReqDto(id))
                ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                    val photoBitmap = Util.getBitmapFromInputStream(response.body()!!.byteStream())
                    holder.profileImage.setImageBitmap(photoBitmap)
                }, {}, {})
            }

            override fun setAccountDefaultPhoto(holder: ReviewListAdapter.ViewHolder) {
                holder.profileImage.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_account_circle_24))
            }
        }

        binding.recyclerViewReview?.let{
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(activity)

            it.addOnScrollListener(object: RecyclerView.OnScrollListener() {

            })
        }

        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {

        })
    }


    override fun onDestroyView() {
        super.onDestroy()

        _binding = null
        isViewDestroyed = true
    }
}