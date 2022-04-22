package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityCreateupdatepostBinding

class CreateUpdatePostHashtagAdapter(
    private val createUpdatePostViewModel: CreateUpdatePostViewModel,
    private val binding: ActivityCreateupdatepostBinding
    ) : RecyclerView.Adapter<CreateUpdatePostHashtagAdapter.HistoryListViewHolder>() {

    private var resultList = mutableListOf<String>()

    class HistoryListViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textview_createupdateposthashtag_name)
        val deleteButton: ImageView = view.findViewById(R.id.imageview_createupdateposthashtag_deletebutton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_createupdateposthashtag, parent, false)

        val holder = HistoryListViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    override fun onBindViewHolder(holder: HistoryListViewHolder, position: Int) {
        val hashtag = '#' + resultList[position]
        holder.name.text = hashtag
    }

    override fun getItemCount() = resultList.size

    private fun setListenerOnView(holder: HistoryListViewHolder) {
        holder.deleteButton.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            deleteItem(position)
        }
    }

    private fun deleteItem(position: Int) {
        createUpdatePostViewModel.hashtagList.removeAt(position)

        // for item remove animation
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, this.resultList.size)
    }

    public fun setResult(result: MutableList<String>){
        this.resultList = result
        notifyDataSetChanged()
    }
}