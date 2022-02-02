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
import com.sju18001.petmanagement.restapi.dto.FetchReviewReqDto

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
        resetAndUpdateReviewRecyclerView()
        
        // TODO: 로딩

        return binding.root
    }

    private fun initializeAdapter() {
        // 빈 배열로 초기화
        adapter = ReviewListAdapter(arrayListOf(), requireContext())

        // 인터페이스 구현
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
                // TODO: page index 추가 이후에 작업
            })
        }

        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                setEmptyNotificationView(adapter.itemCount)
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                setEmptyNotificationView(adapter.itemCount)
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                setEmptyNotificationView(adapter.itemCount)
            }
        })
    }

    private fun setEmptyNotificationView(itemCount: Int?) {
        binding.emptyReviewListNotification.visibility =
            if(itemCount != 0) View.GONE
            else View.VISIBLE
    }

    private fun resetAndUpdateReviewRecyclerView() {
        resetReviewData()
        updateReviewRecyclerView(
            FetchReviewReqDto(null, placeId, null)
        )
    }

    private fun resetReviewData() {
        adapter.resetItem()
        binding.recyclerViewReview.post{
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateReviewRecyclerView(body: FetchReviewReqDto) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchReviewReq(body)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            response.body()!!.reviewList?.let {
                if(it.isNotEmpty()){
                    it.map { item ->
                        adapter.addItem(item)
                    }
                    adapter.notifyDataSetChanged()

                    setEmptyNotificationView(adapter.itemCount)
                }
            }
        }, {}, {})
    }


    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        isViewDestroyed = true
    }
}