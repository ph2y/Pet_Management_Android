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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.CustomProgressBar
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentReviewBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dao.Account
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

    private val viewModel: ReviewViewModel by viewModels()
    private var isViewDestroyed = false

    /**
     * fetch review를 위한 변수입니다. topReviewId를 기준으로 페이징하여
     * 순차적으로 fetch review 작업을 할 수 있습니다.
     */
    private var isLast = false
    private var topReviewId: Long? = null
    private var pageIndex: Int = 1

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
                        setRatingAndReviewCount(item.rating, 1)
                        viewModel.myReviewId.set(item.id)

                        adapter.addItemToTop(item)
                        adapter.notifyItemInserted(0)
                        adapter.notifyItemRangeChanged(0, adapter.itemCount)

                        binding.recyclerViewReview.scrollToPosition(adapter.itemCount-1)
                    }
                }
            }
        }
    }

    /**
     * rating 및 reviewCount가 변경될 때 이 함수를 통해, 평균 rating을 다시 계산하여
     * 뷰모델의 rating, reviewCount를 업데이트합니다.
     */
    private fun setRatingAndReviewCount(ratingDiff: Int, reviewCountDiff: Int) {
        val rating = viewModel.rating.get()!!
        val reviewCount = viewModel.reviewCount.get()!!

        // Divide by zero 방지
        if(reviewCount + reviewCountDiff == 0L){
            viewModel.rating.set(0.0)
            viewModel.reviewCount.set(0)
        }else{
            viewModel.rating.set((rating*reviewCount + ratingDiff) / (reviewCount + reviewCountDiff))
            viewModel.reviewCount.set(reviewCount + reviewCountDiff)
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
                        val prevRating = adapter.getItem(position).rating
                        setRatingAndReviewCount(item.rating - prevRating, 0)

                        adapter.setItem(position, item)
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        }
    }

    private fun fetchOneReviewAndInvoke(reviewId: Long, callback: ((Review)->Unit)) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchReviewReq(FetchReviewReqDto(reviewId, null, null, null, null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            response.body()?.reviewList?.get(0)?.let{
                callback.invoke(it)
            }
        }, {}, {})
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.placeId.set(requireActivity().intent.getLongExtra("placeId", -1))
        viewModel.rating.set(requireActivity().intent.getDoubleExtra("rating", 0.0))
        viewModel.reviewCount.set(requireActivity().intent.getLongExtra("reviewCount", 0))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        isViewDestroyed = false

        initializeAdapter()
        CustomProgressBar.addProgressBar(requireContext(), binding.fragmentReviewParentLayout, 80, R.color.white)
        doInitialFetch()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.adView.loadAd(AdRequest.Builder().build())
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentReviewBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@ReviewFragment
        binding.viewModel = viewModel
        binding.util = Util.Companion
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
        }
        
        // 스크롤하여 최하단에 위치할 시, review 추가 로드
        binding.nestedScrollView?.let{
            it.setOnScrollChangeListener { _, _, _, _, _ ->
                if(!it.canScrollVertically(1) && adapter.itemCount != 0 && !isLast){
                    updateReviewRecyclerView(
                        FetchReviewReqDto(
                            null, viewModel.placeId.get(), null, pageIndex, topReviewId
                        )
                    )
                    pageIndex += 1
                }
            }
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
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            setRatingAndReviewCount(- response.body()!!.deletedReviewRating, -1)
            viewModel.myReviewId.set(-1)

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
    private fun doInitialFetch() {
        resetReviewData()
        fetchMyReview{
            updateReviewRecyclerView(FetchReviewReqDto(
                null, viewModel.placeId.get(), null, null, null
            ))
        }
    }

    private fun resetReviewData() {
        topReviewId = null
        pageIndex = 1
        adapter.resetItem()
        viewModel.myReviewId.set(-1)

        binding.recyclerViewReview.post{
            adapter.notifyDataSetChanged()
        }
    }

    private fun fetchMyReview(callback: () -> Unit) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchReviewReq(
                FetchReviewReqDto(null, viewModel.placeId.get(), SessionManager.fetchLoggedInAccount(requireContext())!!.id, null, null)
            )
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            response.body()!!.reviewList?.let {
                if(it.isNotEmpty()){
                    adapter.addItem(it[0])
                    adapter.notifyDataSetChanged()

                    viewModel.myReviewId.set(it[0].id)

                    setEmptyNotificationView(adapter.itemCount)
                }

                callback.invoke()
            }
        }, {
            callback.invoke()
        }, {}
        )
    }

    private fun updateReviewRecyclerView(body: FetchReviewReqDto) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchReviewReq(body)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            isLast = response.body()!!.isLast == true

            response.body()!!.reviewList?.let {
                if(it.isNotEmpty()){
                    if(topReviewId == null){
                        topReviewId = it.first().id
                    }

                    it.map { item ->
                        if(item.id != viewModel.myReviewId.get()) adapter.addItem(item)
                    }

                    adapter.notifyDataSetChanged()

                    setEmptyNotificationView(adapter.itemCount)
                }

                CustomProgressBar.removeProgressBar(binding.fragmentReviewParentLayout)
            }
        }, { CustomProgressBar.removeProgressBar(binding.fragmentReviewParentLayout) },
            { CustomProgressBar.removeProgressBar(binding.fragmentReviewParentLayout) }
        )
    }

    private fun startCreateReviewActivity() {
        val createReviewActivityIntent = getCreateReviewActivityIntent()
        startForCreateResult.launch(createReviewActivityIntent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    private fun getCreateReviewActivityIntent(): Intent {
        val res = Intent(context, CreateUpdateReviewActivity::class.java)
        res.putExtra("fragmentType", CreateUpdateReviewActivity.CREATE_REVIEW)
        res.putExtra("placeId", viewModel.placeId.get())

        return res
    }


    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        isViewDestroyed = true
    }


    /**
     * Databinding functions
     */
    fun onBackButtonClicked() {
        activity?.finish()
    }

    fun onCreateReviewFabClicked() {
        startCreateReviewActivity()
    }
}