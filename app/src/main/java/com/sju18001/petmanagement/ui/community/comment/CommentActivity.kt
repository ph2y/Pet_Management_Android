package com.sju18001.petmanagement.ui.community.comment

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.CustomProgressBar
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.ActivityCommentBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.ui.community.CommunityUtil
import com.sju18001.petmanagement.ui.community.comment.updateComment.UpdateCommentActivity

class CommentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommentBinding

    private val viewModel: CommentViewModel by viewModels()
    private lateinit var adapter: CommentAdapter

    private var isViewDestroyed = false

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            result.data?.let{
                val newContents = it.getStringExtra("contents")?: ""
                val position = it.getIntExtra("position", -1)

                adapter.updateCommentContents(newContents, position)
                adapter.notifyItemChanged(position)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBinding()

        isViewDestroyed = false
        supportActionBar?.hide()

        initializeViewModel()
        initializeAdapter()

        resetCommentDataAndFetchComment()
        fetchAccountPhoto()

        Util.setupViewsForHideKeyboard(
            this,
            binding.fragmentCommentParentLayout,
            listOf(binding.layoutCommentInput)
        )

        binding.adView.loadAd(AdRequest.Builder().build())
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_comment)

        binding.lifecycleOwner = this
        binding.activity = this@CommentActivity
        binding.viewModel = viewModel
    }

    private fun initializeViewModel() {
        viewModel.postId = intent.getLongExtra("postId", -1)
        viewModel.loggedInAccount = SessionManager.fetchLoggedInAccount(baseContext)!!
    }

    private fun initializeAdapter(){
        adapter = CommentAdapter(arrayListOf(), arrayListOf(), arrayListOf(), object: CommentAdapterInterface{
            override fun getActivity() = this@CommentActivity

            override fun onClickReply(id: Long, nickname: String) {
                viewModel.replyId.value = id
                viewModel.replyNickname.value = nickname
            }

            override fun onLongClickComment(authorId: Long, commentId: Long, commentContents: String, position: Int){
                // 자기 자신이 쓴 글인가?
                if(viewModel.loggedInAccount?.id == authorId){
                    showCommentDialogForAuthor(commentId, commentContents, position)
                }else{
                    showCommentDialogForNonAuthor(commentId)
                }
            }

            override fun onClickLoadReply(pageIndex: Int, topReplyId: Long?, parentCommentId: Long, position: Int){
                fetchReply(pageIndex, topReplyId, parentCommentId, position)
            }

            override fun startPetProfile(author: Account) {
                CommunityUtil.fetchRepresentativePetAndStartPetProfile(baseContext, author, isViewDestroyed)
            }
        })

        binding.recyclerViewComment?.let{
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(this)

            // 스크롤하여, 최하단에 위치할 시 댓글을 추가로 불러온다.
            it.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if(!recyclerView.canScrollVertically(1) && !viewModel.isLast){
                        fetchComment(FetchCommentReqDto(
                            viewModel.pageIndex, viewModel.topCommentId, viewModel.postId, null, null
                        ))
                        viewModel.pageIndex += 1
                    }
                }
            })
        }

        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                setEmptyNotificationView(adapter.itemCount)
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                setEmptyNotificationView(adapter.itemCount)
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                setEmptyNotificationView(adapter.itemCount)
            }
        })
    }

    private fun showCommentDialogForAuthor(id: Long, contents: String, position: Int){
        val builder = AlertDialog.Builder(this)
        builder.setItems(arrayOf("수정", "삭제"), DialogInterface.OnClickListener{ _, which ->
            when(which){
                0 -> {
                    // 수정
                    val updateCommentActivityIntent = Intent(baseContext, UpdateCommentActivity::class.java)
                    updateCommentActivityIntent.putExtra("id", id)
                    updateCommentActivityIntent.putExtra("contents", contents)
                    updateCommentActivityIntent.putExtra("position", position)

                    startForResult.launch(updateCommentActivityIntent)
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
                }
                1 -> {
                    // 삭제
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage(getString(R.string.delete_comment_dialog))
                        .setPositiveButton(R.string.confirm) { _, _ ->
                            deleteComment(id, position)
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ ->
                            dialog.cancel()
                        }
                        .create().show()
                }
            }
        })
            .create().show()
    }

    private fun deleteComment(id: Long, position: Int){
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .deleteCommentReq(DeleteCommentReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            Toast.makeText(baseContext, getText(R.string.delete_comment_success), Toast.LENGTH_SHORT).show()

            adapter.removeItem(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, adapter.itemCount)
        }, {}, {})
    }

    private fun showCommentDialogForNonAuthor(commentId: Long){
        val builder = AlertDialog.Builder(this)
        builder.setItems(arrayOf("신고"), DialogInterface.OnClickListener{ _, which ->
            when(which){
                0 -> reportComment(commentId)
            }
        })
            .create().show()
    }

    private fun reportComment(id: Long){
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .reportCommentReq(ReportCommentReqDto(id))
        ServerUtil.enqueueApiCallWithoutErrorMessage(call, {isViewDestroyed}, baseContext, {
            Toast.makeText(baseContext, getString(R.string.report_comment_successful), Toast.LENGTH_LONG).show()
        })
    }

    private fun fetchReply(pageIndex: Int, topReplyId: Long?, parentCommentId: Long, position: Int) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchCommentReq(FetchCommentReqDto(pageIndex, topReplyId, null, parentCommentId, null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            response.body()?.let{
                // 더이상 불러올 답글이 없을 시
                if(it.isLast == true){
                    adapter.setTopReplyIdList(CommentAdapter.NO_REPLY, position)
                    adapter.notifyItemChanged(position)

                    Toast.makeText(baseContext, getString(R.string.no_more_reply), Toast.LENGTH_SHORT).show()
                }

                it.commentList?.let{ item ->
                    val replyCount = item.count()
                    for(i in 0 until replyCount){
                        item[i].contents = item[i].contents.replace("\n", "")
                        adapter.addItemOnPosition(item[i], position+1)
                    }

                    adapter.addPageIndices(position)
                    adapter.notifyItemRangeInserted(position + 1, replyCount)
                }
            }
        }, {}, {})
    }

    private fun fetchComment(body: FetchCommentReqDto){
        CustomProgressBar.addProgressBar(baseContext, binding.fragmentCommentParentLayout, 80, R.color.white)
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchCommentReq(body)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            viewModel.isLast = response.body()!!.isLast == true

            response.body()!!.commentList?.let {
                if(it.isNotEmpty()){
                    if(viewModel.topCommentId == null){
                        viewModel.topCommentId = it.first().id
                    }

                    it.map { item ->
                        item.contents = item.contents.replace("\n", "")
                        adapter.addItem(item)

                        if(item.childCommentCnt > 0){
                            adapter.setTopReplyIdList(CommentAdapter.REPLY_NOT_FETCHED, adapter.itemCount-1)
                            adapter.notifyItemChanged(adapter.itemCount-1)
                        }
                    }

                    binding.recyclerViewComment.post{ adapter.notifyDataSetChanged() }
                }
            }

            CustomProgressBar.removeProgressBar(binding.fragmentCommentParentLayout)
            binding.layoutSwipeRefresh.isRefreshing = false
        }, {
            CustomProgressBar.removeProgressBar(binding.fragmentCommentParentLayout)
            binding.layoutSwipeRefresh.isRefreshing = false
        }, {
            CustomProgressBar.removeProgressBar(binding.fragmentCommentParentLayout)
            binding.layoutSwipeRefresh.isRefreshing = false
        })
    }

    private fun setEmptyNotificationView(itemCount: Int?) {
        binding.emptyCommentListNotification.visibility =
            if (itemCount != 0) View.GONE else View.VISIBLE
    }


    private fun resetCommentDataAndFetchComment() {
        resetCommentData()
        fetchComment(FetchCommentReqDto(
            null, null, viewModel.postId, null, null
        ))
    }

    private fun resetCommentData(){
        viewModel.isLast = false
        viewModel.topCommentId = null
        viewModel.pageIndex = 1

        adapter.resetDataSet()
        binding.recyclerViewComment.post{ adapter.notifyDataSetChanged() }
    }


    private fun fetchAccountPhoto(){
        if(!viewModel.loggedInAccount!!.photoUrl.isNullOrEmpty()){
            fetchAccountPhotoToAccountPhoto(viewModel.loggedInAccount!!.id)
        }else{
            binding.imageProfile.setImageDrawable(getDrawable(R.drawable.ic_baseline_account_circle_24))
        }
    }

    private fun fetchAccountPhotoToAccountPhoto(id: Long) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchAccountPhotoReq(FetchAccountPhotoReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            val photoBitmap = Util.getBitmapFromInputStream(response.body()!!.byteStream())
            binding.imageProfile.setImageBitmap(photoBitmap)
        }, {}, {})
    }


    override fun onStart() {
        super.onStart()

        setLiveDataObservers()
        setListenerOnViews() // Databinding가 지원되지 않는 경우에 대해
    }

    private fun setLiveDataObservers() {
        viewModel.replyId.observe(this, { replyId ->
            if(replyId != null) {
                binding.layoutReplyDescription.visibility = View.VISIBLE
                Util.showKeyboard(this@CommentActivity, binding.editTextComment)
            }else{
                binding.layoutReplyDescription.visibility = View.GONE
            }
        })
    }

    private fun setListenerOnViews(){
        // 키보드 엔터 -> 댓글 생성
        binding.editTextComment.setOnEditorActionListener{ _, _, _ ->
            createComment(CreateCommentReqDto(viewModel.postId, viewModel.replyId.value, viewModel.contents.value!!))
            true
        }

        binding.layoutSwipeRefresh.setOnRefreshListener {
            resetCommentDataAndFetchComment()
        }
    }

    private fun createComment(body: CreateCommentReqDto){
        viewModel.isApiLoading.value = true
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .createCommentReq(body)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            // 생성된 댓글의 id로 FetchComment
            val callForFetchingComment = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .fetchCommentReq(FetchCommentReqDto(null, null, null, null, it.body()!!.id))
            ServerUtil.enqueueApiCall(callForFetchingComment, {isViewDestroyed}, baseContext, { it2 ->
                it2.body()!!.commentList?.get(0)?.let { item ->
                    // 생성된 댓글이 답글일 경우
                    if(item.parentCommentId != null){
                        resetCommentDataAndFetchComment()
                    }else{
                        adapter.addItemOnPosition(item, 0)
                        adapter.notifyItemInserted(0)
                        adapter.notifyItemRangeChanged(0, adapter.itemCount)

                        binding.recyclerViewComment.scrollToPosition(0)
                    }
                }

                viewModel.contents.value = null
                viewModel.isApiLoading.value = false
                viewModel.replyId.value = null

                Toast.makeText(baseContext, getText(R.string.create_comment_success), Toast.LENGTH_SHORT).show()
            }, {}, {})
        }, {
            viewModel.isApiLoading.value = false
            viewModel.replyId.value = null
        }, {
            viewModel.isApiLoading.value = false
            viewModel.replyId.value = null
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
    }


    /** Databinding functions */
    fun onClickReplyCancelButton() {
        viewModel.replyId.value = null
    }

    fun onClickBackButton() {
        finish()
    }

    fun onClickCreateCommentButton() {
        if (binding.editTextComment.text.isNullOrBlank()) {
            Toast.makeText(baseContext, getText(R.string.empty_comment_exception_message), Toast.LENGTH_SHORT).show()
        } else {
            createComment(CreateCommentReqDto(viewModel.postId, viewModel.replyId.value, viewModel.contents.value!!))
        }
    }
}