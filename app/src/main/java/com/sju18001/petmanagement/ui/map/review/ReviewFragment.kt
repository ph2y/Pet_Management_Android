package com.sju18001.petmanagement.ui.map.review

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.sju18001.petmanagement.restapi.dao.Post
import com.sju18001.petmanagement.restapi.dao.Review
import com.sju18001.petmanagement.restapi.dto.DeleteReviewReqDto
import com.sju18001.petmanagement.restapi.dto.FetchAccountPhotoReqDto
import com.sju18001.petmanagement.restapi.dto.FetchReviewReqDto
import com.sju18001.petmanagement.ui.community.CommunityUtil
import com.sju18001.petmanagement.ui.map.review.createUpdateReview.CreateUpdateReviewActivity

class ReviewFragment : Fragment() {
    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReviewListAdapter

    private var isViewDestroyed = false

    private var placeId: Long = -1
    private var rating: Float = 3.7f // TODO
    private var reviewCount: Int = 0

    /**
     * CreateUpdateReviewActivity에서 리뷰를 생성하거나 수정합니다. 아래의 변수는
     * 위 행동을 한 뒤에, 다시 ReviewFragment로 돌아와서 수행할 코드를 지정해줍니다.
     */
    private val startForCreateResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            result.data?.let{
                val reviewId = it.getLongExtra("reviewId", -1)
                if(reviewId != -1L){
                    fetchOneReviewAndInvoke(reviewId) { item ->
                        adapter.addItem(item)
                        adapter.notifyItemInserted(adapter.itemCount)

                        binding.recyclerViewReview.scrollToPosition(adapter.itemCount-1)
                    }
                }
            }
        }
    }

    private val startForUpdateResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            result.data?.let{
                val reviewId = it.getLongExtra("reviewId", -1)
                val position = it.getIntExtra("position", -1)

                if(reviewId != -1L && position != -1){
                    fetchOneReviewAndInvoke(reviewId) { item ->
                        adapter.setItem(position, item)
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        }
    }

    private fun fetchOneReviewAndInvoke(reviewId: Long, callback: ((Review)->Unit)) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchReviewReq(FetchReviewReqDto(reviewId, null, null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            response.body()?.reviewList?.get(0)?.let{
                callback.invoke(it)
            }
        }, {}, {})
    }


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

        setListenerOnViews()

        return binding.root
    }

    /**
     * 리싸이클러뷰 초기화
     */
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

            override fun onClickReviewFunctionButton(review: Review, position: Int) {
                val loggedInAccount = SessionManager.fetchLoggedInAccount(requireContext())!!

                // 자기 자신이 쓴 글인가?
                if(loggedInAccount.id == review.author.id){
                    showReviewDialogForAuthor(review, position)
                }else{
                    showReviewDialogForNonAuthor()
                }
            }

            override fun startPetProfile(author: Account) {
                CommunityUtil.fetchRepresentativePetAndStartPetProfile(requireContext(), author, isViewDestroyed)
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

    /**
     * Review function button 관련
     */
    private fun showReviewDialogForAuthor(review: Review, position: Int){
        val builder = AlertDialog.Builder(requireActivity())
        builder.setItems(arrayOf("수정", "삭제")) { _, which ->
            when (which) {
                0 -> {
                    startUpdateReviewActivity(review, position)
                }
                1 -> {
                    showDeleteReviewDialog(review.id, position)
                }
            }
        }
            .create().show()
    }

    private fun startUpdateReviewActivity(review: Review, position: Int) {
        val updateReviewActivityIntent = getUpdateReviewActivityIntent(review, position)
        startForUpdateResult.launch(updateReviewActivityIntent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    private fun getUpdateReviewActivityIntent(review: Review, position: Int): Intent {
        val res = Intent(context, CreateUpdateReviewActivity::class.java)
        res.putExtra("fragmentType", CreateUpdateReviewActivity.UPDATE_REVIEW)
        res.putExtra("reviewId", review.id)
        res.putExtra("position", position)

        return res
    }

    private fun showDeleteReviewDialog(reviewId: Long, position: Int) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage(getString(R.string.delete_review_dialog))
            .setPositiveButton(R.string.confirm) { _, _ ->
                deleteReview(reviewId, position)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .create().show()
    }

    private fun deleteReview(id: Long, position: Int){
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .deleteReviewReq(DeleteReviewReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            adapter.removeItem(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, adapter.itemCount)

            Toast.makeText(context, getString(R.string.delete_review_successful), Toast.LENGTH_LONG).show()
        }, {}, {})
    }

    private fun showReviewDialogForNonAuthor(){
        val builder = AlertDialog.Builder(requireActivity())
        builder.setItems(arrayOf("신고"), DialogInterface.OnClickListener{ _, which ->
            when(which){
                0 -> {
                    // TODO
                }
            }
        })
            .create().show()
    }

    private fun setEmptyNotificationView(itemCount: Int?) {
        binding.emptyReviewListNotification.visibility =
            if(itemCount != 0) View.GONE
            else View.VISIBLE
    }


    /**
     * 리싸이클러뷰 함수
     */
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
                }

                setViewsAfterFetch()
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

    private fun setListenerOnViews() {
        binding.buttonBack.setOnClickListener {
            activity?.finish()
        }
        binding.createReviewFab.setOnClickListener {
            startCreateReviewActivity()
        }
    }

    private fun startCreateReviewActivity() {
        val createReviewActivityIntent = getCreateReviewActivityIntent()
        startForCreateResult.launch(createReviewActivityIntent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    private fun getCreateReviewActivityIntent(): Intent {
        val res = Intent(context, CreateUpdateReviewActivity::class.java)
        res.putExtra("fragmentType", CreateUpdateReviewActivity.CREATE_REVIEW)
        res.putExtra("placeId", placeId)

        return res
    }


    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        isViewDestroyed = true
    }
}