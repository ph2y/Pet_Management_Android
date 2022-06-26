package com.sju18001.petmanagement.ui.community.post

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.CustomProgressBar
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentPostBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dao.Post
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.restapi.global.FileType
import com.sju18001.petmanagement.ui.community.comment.CommentActivity
import com.sju18001.petmanagement.ui.community.post.createUpdatePost.CreateUpdatePostActivity
import com.sju18001.petmanagement.ui.community.post.postGeneralFile.PostGeneralFileActivity
import java.net.URLEncoder

class PostFragment : Fragment(), PostAdapterInterface {
    /**
     * 외부에서 petId를 지정해줄 수 있습니다. PetProfile 내부에 PostFragment가
     * 존재할 때 petId를 지정하여 값을 참조합니다.
     */
    companion object{
        private const val COMMUNITY_DIRECTORY = "community"

        @JvmStatic
        fun newInstance(petId: Long) = PostFragment().apply{
            arguments = Bundle().apply{
                putLong("petId", petId)
            }
        }
    }

    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PostAdapter
    private var isViewDestroyed = false

    /**
     * fetch post를 위한 변수입니다. topPostId를 기준으로 페이징하여
     * 순차적으로 fetch post 작업을 할 수 있습니다.
     */
    private var isLast = false
    private var topPostId: Long? = null
    private var pageIndex: Int = 1

    /**
     * CreateUpdatePostFragment에서 글 생성 또는 수정을 합니다. 여기에 있는 변수는,
     * 위 행동을 한 뒤에, 다시 PostFragment로 돌아와서 수행할 코드를 지정해줍니다.
     */
    private val startForCreateResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            result.data?.let{
                val postId = it.getLongExtra("postId", -1)
                if(postId != -1L){
                    fetchOnePostAndInvoke(postId) { item ->
                        adapter.addItemToTop(item)
                        adapter.notifyItemInserted(0)
                        adapter.notifyItemRangeChanged(0, adapter.itemCount)

                        binding.recyclerviewPost.scrollToPosition(0)

                        setEmptyNotificationView(1)
                    }
                }
            }
        }
    }

    private val startForUpdateResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if(result.resultCode == Activity.RESULT_OK){
            result.data?.let{
                val postId = it.getLongExtra("postId", -1)
                val position = it.getIntExtra("position", -1)

                if(postId != -1L && position != -1){
                    fetchOnePostAndInvoke(postId) { item ->
                        adapter.setPost(position, item)
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        }
    }

    /**
     * recycler view의 item을 추가하거나 수정하려면, 해당 post의 내용을 알아야만
     * 합니다. 그 내용이 매우 클 수도 있으므로, 프래그먼트 간 내용 전달이 아니라, API
     * 호출을 이용하여 글의 내용을 얻습니다.
     */
    private fun fetchOnePostAndInvoke(postId: Long, callback: ((Post)->Unit)){
        val latAndLong = Util.getGeolocation(requireContext())
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPostReq(FetchPostReqDto(null, null, null, postId, latAndLong[0], latAndLong[1]))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            response.body()?.postList?.get(0)?.let{ item ->
                /**
                 * petId가 지정되어있지만 pet id가 다를 경우, 즉 PetProfile에서 글 수정을
                 * 할 때, 펫을 다른 펫으로 지정한 경우에는 글을 따로 업데이트 하지 않습니다.
                 */
                val petId = arguments?.getLong("petId")
                if(!(petId != null && petId != item.pet.id)) {
                    callback.invoke(item)
                }
            }
        }, {}, {})
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        isViewDestroyed = false

        initializeRecyclerView()
        resetAndUpdatePostRecyclerView()

        return binding.root
    }


    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
    }


    /** initializeRecyclerView() */
    private fun initializeRecyclerView(){
        adapter = PostAdapter(arrayListOf(), arrayListOf(), arrayListOf(), this)
        binding.recyclerviewPost?.let{
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(activity)

            it.setItemViewCacheSize(30)

            // 스크롤하여 최하단에 위치할 시, post 추가 로드
            it.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if(!recyclerView.canScrollVertically(1) && adapter.itemCount != 0 && !isLast){
                        val latAndLong = Util.getGeolocation(requireContext())
                        updatePostRecyclerView(FetchPostReqDto(pageIndex, topPostId,
                            arguments?.getLong("petId"), null, latAndLong[0], latAndLong[1]))
                        pageIndex += 1
                    }
                }
            })
        }

        // 데이터의 변경 시, setEmptyNotificationView()
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
        binding.textviewEmptypost.post {
            binding.textviewEmptypost.visibility =
                if(itemCount != 0) View.GONE
                else View.VISIBLE
        }
    }


    /** resetAndUpdatePostRecyclerView() */
    private fun resetAndUpdatePostRecyclerView(){
        resetPostData()

        val latAndLong = Util.getGeolocation(requireContext())
        updatePostRecyclerView(FetchPostReqDto(null, null,
            getPetIdFromArguments(), null, latAndLong[0], latAndLong[1]))
    }

    private fun resetPostData(){
        topPostId = null
        pageIndex = 1
        adapter.resetItem()

        adapter.notifyDataSetChanged()
    }

    private fun getPetIdFromArguments(): Long? {
        return arguments?.getLong("petId")
    }

    private fun updatePostRecyclerView(body: FetchPostReqDto) {
        CustomProgressBar.addProgressBar(requireContext(), binding.framelayoutParent, 80, R.color.white)
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPostReq(body)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            isLast = response.body()!!.isLast == true

            response.body()!!.postList?.let {
                if(it.isNotEmpty()){
                    if(topPostId == null) topPostId = it.first().id

                    it.map { item ->
                        adapter.addItem(item)
                        fetchLikeAndSetLikeViews(adapter.itemCount-1, item.id)
                    }
                    adapter.notifyDataSetChanged()
                }

                CustomProgressBar.removeProgressBar(binding.framelayoutParent)
                binding.swiperefreshlayoutPost.isRefreshing = false
            }
        }, {
            CustomProgressBar.removeProgressBar(binding.framelayoutParent)
            binding.swiperefreshlayoutPost.isRefreshing = false
        }, {
            CustomProgressBar.removeProgressBar(binding.framelayoutParent)
            binding.swiperefreshlayoutPost.isRefreshing = false
        })
    }

    private fun fetchLikeAndSetLikeViews(position: Int, postId: Long){
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchLikeReq(FetchLikeReqDto(postId, null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            adapter.setLikedCount(position, response.body()!!.likedCount!!)

            // 본인이 좋아요를 눌렀는가?
            val flag = response.body()!!.likedAccountIdList?.contains(SessionManager.fetchLoggedInAccount(requireContext())!!.id)?: false
            adapter.setIsPostLiked(position, flag)

            adapter.notifyItemChanged(position)
        }, {}, {})
    }


    override fun onStart() {
        super.onStart()

        binding.swiperefreshlayoutPost.setOnRefreshListener {
            resetAndUpdatePostRecyclerView()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        isViewDestroyed = true

        Util.deleteCopiedFiles(requireContext(), COMMUNITY_DIRECTORY)
    }


    /** 공개 함수 */
    // 네비게이션 바를 통해 프래그먼트 전환 시 필요한 함수입니다.
    fun startAllVideos(){
        val layoutManager = (binding.recyclerviewPost.layoutManager as LinearLayoutManager)
        val firstIndex = layoutManager.findFirstVisibleItemPosition()
        val lastIndex = layoutManager.findLastVisibleItemPosition()

        for(i in firstIndex..lastIndex){
            val videoPostMedia = layoutManager.findViewByPosition(i)?.findViewById<VideoView>(R.id.videoview_postmedia)
            videoPostMedia?.start()
        }
    }

    fun pauseAllVideos(){
        val layoutManager = (binding.recyclerviewPost.layoutManager as LinearLayoutManager)
        val firstIndex = layoutManager.findFirstVisibleItemPosition()
        val lastIndex = layoutManager.findLastVisibleItemPosition()

        for(i in firstIndex..lastIndex){
            val videoPostMedia = layoutManager.findViewByPosition(i)?.findViewById<VideoView>(R.id.videoview_postmedia)
            videoPostMedia?.pause()
        }
    }

    // CommunityFragment의 create_post_fab를 위한 함수입니다.
    fun checkIfAccountHasPetAndStartCreatePostFragment() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .fetchPetReq(FetchPetReqDto(null, null))
        ServerUtil.enqueueApiCall(call, { isViewDestroyed }, requireContext(), { response ->
            var petCount = 0
            response.body()?.petList?.map {
                petCount++
            }

            if (petCount > 0) {
                startCreatePostFragment()
            } else {
                Toast.makeText(context, getString(R.string.pet_list_empty_for_post_exception_message), Toast.LENGTH_LONG).show()
            }
        }, {}, {})
    }

    private fun startCreatePostFragment(){
        val createUpdatePostActivityIntent = Intent(context, CreateUpdatePostActivity::class.java)
        createUpdatePostActivityIntent.putExtra("activityType", "create_post")

        startForCreateResult.launch(createUpdatePostActivityIntent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }


    /** PostAdapterInterface 관련 */
    override fun startCommentActivity(postId: Long) {
        val commentActivityIntent = Intent(context, CommentActivity::class.java)
        commentActivityIntent.putExtra("postId", postId)

        startActivity(commentActivityIntent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun createLike(holder: PostAdapter.ViewHolder, postId: Long, position: Int){
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .createLikeReq(CreateLikeReqDto(postId, null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            adapter.setLikedCount(position, adapter.getLikedCount(position) + 1)
            adapter.setIsPostLiked(position, true)
            adapter.bindViewHolder(holder, position)
        }, {}, {})
    }

    override fun deleteLike(holder: PostAdapter.ViewHolder, postId: Long, position: Int) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .deleteLikeReq(DeleteLikeReqDto(postId, null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            adapter.setLikedCount(position, adapter.getLikedCount(position) - 1)
            adapter.setIsPostLiked(position, false)
            adapter.bindViewHolder(holder, position)
        }, {}, {})
    }

    override fun setAccountPhoto(holder: PostAdapter.ViewHolder, id: Long){
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchAccountPhotoReq(FetchAccountPhotoReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            val photoBitmap = Util.getBitmapFromInputStream(response.body()!!.byteStream())
            holder.accountPhotoImage.setImageBitmap(photoBitmap)
        }, {}, {})
    }

    override fun setAccountDefaultPhoto(holder: PostAdapter.ViewHolder) {
        holder.accountPhotoImage.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_account_circle_24))
    }

    override fun startGeneralFileActivity(postId: Long, fileAttachments: String) {
        val postGeneralFileActivityIntent = Intent(context, PostGeneralFileActivity::class.java)
        postGeneralFileActivityIntent.putExtra("postId", postId)
        postGeneralFileActivityIntent.putExtra("fileAttachments", fileAttachments)
        startActivity(postGeneralFileActivityIntent)
    }

    // Fragment의 getContext 함수와 중복되지 않도록 유의할 것
    override fun getContextFromFragment(): Context {
        return requireContext()
    }


    override fun onClickPostFunctionButton(post: Post, position: Int) {
        val loggedInAccount = SessionManager.fetchLoggedInAccount(requireContext())!!

        // 자기 자신이 쓴 글인가?
        if(loggedInAccount.id == post.author.id){
            showPostDialogForAuthor(post, position)
        }else{
            showPostDialogForNonAuthor(post)
        }
    }

    private fun showPostDialogForAuthor(post: Post, position: Int){
        val builder = AlertDialog.Builder(requireActivity())
        builder.setItems(arrayOf("수정", "삭제")) { _, which ->
            when (which) {
                0 -> startUpdatePostActivity(post, position)
                1 -> showDeletePostDialog(post.id, position)
            }
        }
            .create().show()
    }

    private fun startUpdatePostActivity(post: Post, position: Int){
        startForUpdateResult.launch(makeUpdatePostActivityIntent(post, position))
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    private fun makeUpdatePostActivityIntent(post: Post, position: Int): Intent {
        val res = Intent(context, CreateUpdatePostActivity::class.java)
        res.putExtra("activityType", "update_post")
        res.putExtra("postId", post.id)
        res.putExtra("position", position)
        res.putExtra(
            "originalImageCount",
            Util.getArrayFromMediaAttachments(post.imageAttachments).size
        )
        res.putExtra(
            "originalVideoCount",
            Util.getArrayFromMediaAttachments(post.videoAttachments).size
        )
        res.putExtra(
            "originalGeneralFileCount",
            Util.getArrayFromMediaAttachments(post.fileAttachments).size
        )

        return res
    }

    private fun showDeletePostDialog(postId: Long, position: Int) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage(getString(R.string.delete_post_dialog))
            .setPositiveButton(R.string.confirm) { _, _ ->
                deletePost(postId, position)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .create().show()
    }

    private fun deletePost(id: Long, position: Int){
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .deletePostReq(DeletePostReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            adapter.removeItem(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, adapter.itemCount)

            Toast.makeText(context, getString(R.string.delete_post_successful), Toast.LENGTH_LONG).show()
        }, {}, {})
    }

    private fun showPostDialogForNonAuthor(post: Post){
        val builder = AlertDialog.Builder(requireActivity())
        builder.setItems(arrayOf("신고", "차단")){ _, which ->
            when(which){
                0 -> reportPost(post.id)
                1 -> createBlock(post.author.id)
            }
        }
            .create().show()
    }

    private fun reportPost(id: Long){
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .reportPostReq(ReportPostReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            Toast.makeText(context, getString(R.string.report_post_successful), Toast.LENGTH_LONG).show()
        }, {}, {})
    }

    private fun createBlock(id: Long) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .createBlockReq(CreateBlockReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            Toast.makeText(context, getString(R.string.create_block_successful), Toast.LENGTH_LONG).show()
            resetAndUpdatePostRecyclerView()
        }, {}, {})
    }


    override fun setPostMedia(
        holder: PostMediaAdapter.ViewPagerHolder,
        postId: Long,
        index: Int,
        url: String,
        dummyImageView: ConstraintLayout
    ) {
        if(!Util.isUrlVideo(url)) setPostMediaImage(holder, postId, index, dummyImageView)
        else setPostMediaVideo(holder, url, dummyImageView)
    }

    private fun setPostMediaImage(
        holder: PostMediaAdapter.ViewPagerHolder,
        postId: Long,
        index: Int,
        dummyImageView: ConstraintLayout
    ) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPostImageReq(FetchPostImageReqDto(postId, index, FileType.GENERAL_IMAGE))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            val photoBitmap = Util.getBitmapFromInputStream(response.body()!!.byteStream())
            holder.postMediaImage.setImageBitmap(photoBitmap)

            // 사진의 사이즈를 가로로 꽉 채우되, 비율을 유지합니다.
            val screenWidth = Util.getScreenWidthInPixel(requireActivity())
            val ratio: Float = screenWidth.toFloat() / photoBitmap.width.toFloat()
            holder.postMediaImage.layoutParams.height = (photoBitmap.height.toFloat() * ratio).toInt()

            holder.postMediaImage.visibility = View.VISIBLE
            holder.postMediaVideo.visibility = View.GONE
            dummyImageView.visibility = View.GONE
        }, { dummyImageView.visibility = View.GONE }, { dummyImageView.visibility = View.GONE })
    }

    private fun setPostMediaVideo(
        holder: PostMediaAdapter.ViewPagerHolder,
        url: String,
        dummyImageView: ConstraintLayout
    ) {
        holder.postMediaVideo.apply{
            val encodedUrl = RetrofitBuilder.BASE_URL + "/api/post/video/fetch?url=" + URLEncoder.encode(url, "UTF8")
            setVideoPath(encodedUrl)
            requestFocus()

            setOnCompletionListener { start() } // 반복 재생
            setOnPreparedListener {
                // Set layout height
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                alpha = 1f
                requestLayout()

                start()

                visibility = View.VISIBLE
                holder.postMediaImage.visibility = View.GONE
                dummyImageView.visibility = View.GONE
            }
        }
    }
}