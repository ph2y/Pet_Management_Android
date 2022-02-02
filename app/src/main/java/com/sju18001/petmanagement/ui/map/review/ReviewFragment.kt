package com.sju18001.petmanagement.ui.map.review

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.CustomProgressBar
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
    private var rating: Float = 3.7f // TODO
    private var reviewCount: Int = 0

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
        CustomProgressBar.addProgressBar(requireContext(), binding.fragmentReviewParentLayout, 80, R.color.white)
        resetAndUpdateReviewRecyclerView()

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
                    // TODO: pagination

                    it.map { item -> adapter.addItem(item) }
                    adapter.notifyDataSetChanged()

                    setEmptyNotificationView(adapter.itemCount)

                    // TODO: set rating
                    reviewCount = it.size

                    setViewsAfterFetch()
                }
            }
        }, { setViewsAfterFetch() }, { setViewsAfterFetch() })
    }

    // Fetch한 뒤에 호출해야함에 유의하라.
    private fun setViewsAfterFetch() {
        Util.setRatingStars(getStarImages(), rating, requireContext())
        binding.textRating.text = "$rating"
        binding.textReviewCount.text = "$reviewCount"

        CustomProgressBar.removeProgressBar(binding.fragmentReviewParentLayout)
    }

    private fun getStarImages(): ArrayList<ImageView> {
        val starImages = arrayListOf<ImageView>()
        for(i in 1..5){
            // View id: image_star1 ~ image_star5
            val id = resources.getIdentifier("image_star$i", "id", requireContext().packageName)
            val elem: ImageView = binding.layoutStars.findViewById(id)
            starImages.add(elem)
        }
        return starImages
    }


    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        isViewDestroyed = true
    }
}