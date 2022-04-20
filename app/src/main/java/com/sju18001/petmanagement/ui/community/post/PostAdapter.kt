package com.sju18001.petmanagement.ui.community.post

import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.restapi.dao.Post
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.ItemPostBinding
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.ui.community.CommunityUtil
import de.hdodenhof.circleimageview.CircleImageView

interface PostAdapterInterface {
    fun startCommentActivity(postId: Long)
    fun createLike(holder: PostAdapter.ViewHolder, postId: Long, position: Int)
    fun deleteLike(holder: PostAdapter.ViewHolder, postId: Long, position: Int)
    fun onClickPostFunctionButton(post: Post, position: Int)
    fun setAccountPhoto(holder: PostAdapter.ViewHolder, id: Long)
    fun setAccountDefaultPhoto(holder: PostAdapter.ViewHolder)
    fun setPostMedia(holder: PostMediaAdapter.ViewPagerHolder, postId: Long, index: Int, url: String, dummyImageView: ConstraintLayout)
    fun startGeneralFileActivity(postId: Long, fileAttachments: String)
    fun getContextFromFragment(): Context
}

class PostAdapter(
    private var dataSet: ArrayList<Post>,
    private var likedCounts: ArrayList<Long>,
    private var isPostLiked: ArrayList<Boolean>,
    private val postAdapterInterface: PostAdapterInterface
    ) : RecyclerView.Adapter<PostAdapter.ViewHolder>() {
    class ViewHolder(
        private val adapter: PostAdapter,
        private val binding: ItemPostBinding,
        private val postAdapterInterface: PostAdapterInterface
    ): RecyclerView.ViewHolder(binding.root){
        val accountPhotoImage = binding.circleimageviewAccountphoto
        val viewPager = binding.viewpagerMedia
        val dummyLayout = binding.constraintlayoutDummy

        fun bind(post: Post, likedCount: Long, isPostLiked: Boolean) {
            binding.adapter = adapter
            binding.holder = this
            binding.postAdapterInterface = postAdapterInterface
            binding.post = post
            binding.likedCount = likedCount
            binding.isPostLiked = isPostLiked

            binding.executePendingBindings()

            bindDummyLayout(post)
            bindViewPager(post)
            Util.setViewMore(binding.textviewTextcontents, binding.textviewViewmore, 5)
        }

        private fun bindDummyLayout(post: Post) {
            binding.constraintlayoutDummy.visibility =
                if(!post.imageAttachments.isNullOrEmpty() || !post.videoAttachments.isNullOrEmpty()) View.VISIBLE
                else View.GONE
        }

        private fun bindViewPager(post: Post) {
            val imageAttachments = Util.getArrayFromMediaAttachments(post.imageAttachments)
            val videoAttachments = Util.getArrayFromMediaAttachments(post.videoAttachments)

            if(!post.imageAttachments.isNullOrEmpty() || !post.videoAttachments.isNullOrEmpty()){
                val mediaAttachments = imageAttachments + videoAttachments
                binding.viewpagerMedia.adapter =
                    PostMediaAdapter(postAdapterInterface, post.id, mediaAttachments, this)
                binding.viewpagerMedia.visibility = View.VISIBLE
                binding.viewpagerMedia.orientation = ViewPager2.ORIENTATION_HORIZONTAL
            }else{
                binding.viewpagerMedia.adapter =
                    PostMediaAdapter(postAdapterInterface, 0, arrayOf(), this)
                binding.viewpagerMedia.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ItemPostBinding>(LayoutInflater.from(parent.context),
            R.layout.item_post, parent, false)
        return ViewHolder(this, binding, postAdapterInterface)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position], likedCounts[position], isPostLiked[position])
    }

    override fun getItemCount(): Int = dataSet.size


    fun addItem(post: Post){
        dataSet.add(post)

        // 기본값으로 추가
        likedCounts.add(0)
        isPostLiked.add(false)
    }

    fun addItemToTop(post: Post){
        dataSet.add(0, post)

        // 기본값으로 추가
        likedCounts.add(0, 0)
        isPostLiked.add(0, false)
    }

    fun removeItem(index: Int){
        dataSet.removeAt(index)
        likedCounts.removeAt(index)
        isPostLiked.removeAt(index)
    }

    fun resetItem(){
        dataSet = arrayListOf()
        likedCounts = arrayListOf()
        isPostLiked = arrayListOf()
    }

    fun getLikedCount(position: Int): Long {
        return likedCounts[position]
    }

    fun setLikedCount(position: Int, value: Long){
        likedCounts[position] = value
    }

    fun setIsPostLiked(position: Int, flag: Boolean){
        isPostLiked[position] = flag
    }

    fun setPost(position: Int, post: Post){
        dataSet[position] = post
    }


    /** Databinding functions */
    fun startPetProfileFragmentFromCommunity(holder: ViewHolder) {
        val item = dataSet[holder.absoluteAdapterPosition]
        CommunityUtil.startPetProfileFragmentFromCommunity(holder.itemView.context, item.pet, item.author)
    }

    fun onClickCommentButton(holder: ViewHolder) {
        val item = dataSet[holder.absoluteAdapterPosition]
        postAdapterInterface.startCommentActivity(item.id)
    }

    fun onClickCreateLikeButton(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        val item = dataSet[position]
        postAdapterInterface.createLike(holder, item.id, position)
    }

    fun onClickDeleteLikeButton(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        val item = dataSet[position]
        postAdapterInterface.deleteLike(holder, item.id, position)
    }

    fun onClickGeneralFileButton(holder: ViewHolder) {
        val item = dataSet[holder.absoluteAdapterPosition]
        postAdapterInterface.startGeneralFileActivity(item.id, item.fileAttachments!!)
    }

    fun onClickDialogButton(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        val item = dataSet[position]
        postAdapterInterface.onClickPostFunctionButton(item, position)
    }
}