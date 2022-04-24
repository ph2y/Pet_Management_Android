package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityCreateupdatepostBinding
import com.sju18001.petmanagement.databinding.ItemCreateupdatepostmediaBinding
import java.io.File

class CreateUpdatePostMediaAdapter(
    private val context: Context,
    private val createUpdatePostViewModel: CreateUpdatePostViewModel
    )
    : RecyclerView.Adapter<CreateUpdatePostMediaAdapter.ViewHolder>() {
    private var dataSet = mutableListOf<CreateUpdatePostMedia>()

    class ViewHolder(
        private val adapter: CreateUpdatePostMediaAdapter,
        private val binding: ItemCreateupdatepostmediaBinding,
        private val context: Context,
        private val createUpdatePostViewModel: CreateUpdatePostViewModel
    ): RecyclerView.ViewHolder(binding.root) {
        fun bind(createUpdatePostMedia: CreateUpdatePostMedia) {
            binding.adapter = adapter
            binding.holder = this

            binding.executePendingBindings()

            if (createUpdatePostMedia.isVideo){
                Glide.with(context)
                    .load(createUpdatePostViewModel.videoPathList.value!![createUpdatePostMedia.indexInList])
                    .placeholder(R.drawable.ic_baseline_video_library_36)
                    .into(binding.imageviewThumbnail)
            }else{
                Glide.with(context)
                    .load(File(createUpdatePostViewModel.photoPathList.value!![createUpdatePostMedia.indexInList]))
                    .into(binding.imageviewThumbnail)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ItemCreateupdatepostmediaBinding>(LayoutInflater.from(parent.context),
            R.layout.item_createupdatepostmedia, parent, false)
        return ViewHolder(this, binding, context, createUpdatePostViewModel)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount() = dataSet.size

    fun getDataSet() = dataSet

    fun addItem(item: CreateUpdatePostMedia) {
        dataSet.add(item)
    }


    /** Databinding functions */
    fun onClickDeleteButton(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        if (dataSet[position].isVideo) deleteVideo(position)
        else deletePhoto(position)
    }

    private fun deleteVideo(position: Int) {
        File(dataSet[position].path).delete()

        createUpdatePostViewModel.removeVideoPath(dataSet[position].path)
        dataSet.removeAt(position)
        notifyItemRemoved(position)

        // Video들의 index를 다시 구성합니다.
        var newIndex = 0
        for (i in 0 until itemCount) {
            if (dataSet[i].isVideo) dataSet[i].indexInList = newIndex++
        }
    }

    private fun deletePhoto(position: Int) {
        File(dataSet[position].path).delete()

        createUpdatePostViewModel.removePhotoPath(dataSet[position].path)
        dataSet.removeAt(position)
        notifyItemRemoved(position)

        // Photo들의 index를 다시 구성합니다.
        var newIndex = 0
        for (i in 0 until itemCount) {
            if (!dataSet[i].isVideo) dataSet[i].indexInList = newIndex++
        }
    }
}