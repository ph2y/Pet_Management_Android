package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityCreateupdatepostBinding
import java.io.File

class CreateUpdatePostMediaAdapter(
    private val context: Context,
    private val createUpdatePostViewModel: CreateUpdatePostViewModel
    )
    : RecyclerView.Adapter<CreateUpdatePostMediaAdapter.HistoryListViewHolder>() {
    private var dataSet = mutableListOf<CreateUpdatePostMedia>()

    class HistoryListViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.imageview_thumbnail)
        val deleteButton: ImageView = view.findViewById(R.id.imageview_deletebutton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryListViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_createupdatepostmedia, parent, false)

        val holder = HistoryListViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    override fun onBindViewHolder(holder: HistoryListViewHolder, position: Int) {
        if (dataSet[position].isVideo){
            Glide.with(context)
                .load(createUpdatePostViewModel.videoPathList.value!![dataSet[position].indexInList])
                .placeholder(R.drawable.ic_baseline_video_library_36)
                .into(holder.thumbnail)
        }else{
            Glide.with(context)
                .load(File(createUpdatePostViewModel.photoPathList.value!![dataSet[position].indexInList]))
                .into(holder.thumbnail)
        }
    }

    override fun getItemCount() = dataSet.size

    private fun setListenerOnView(holder: HistoryListViewHolder) {
        holder.deleteButton.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            if (dataSet[position].isVideo) deleteVideo(position)
            else deletePhoto(position)
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

    fun getDataSet() = dataSet

    fun addItem(item: CreateUpdatePostMedia) {
        dataSet.add(item)
    }
}