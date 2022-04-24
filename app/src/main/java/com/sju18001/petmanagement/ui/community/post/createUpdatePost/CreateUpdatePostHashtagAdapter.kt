package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityCreateupdatepostBinding

class CreateUpdatePostHashtagAdapter: RecyclerView.Adapter<CreateUpdatePostHashtagAdapter.ViewHolder>() {
    private var dataSet = mutableListOf<String>()

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textview_name)
        val deleteButton: ImageView = view.findViewById(R.id.imageview_deletebutton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_createupdateposthashtag, parent, false)

        val holder = ViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hashtag = '#' + dataSet[position]
        holder.name.text = hashtag
    }

    override fun getItemCount() = dataSet.size

    private fun setListenerOnView(holder: ViewHolder) {
        holder.deleteButton.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            deleteItem(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, dataSet.size)
        }
    }

    private fun deleteItem(position: Int) {
        dataSet.removeAt(position)
    }

    fun addItem(hashtag: String) {
        dataSet.add(hashtag)
    }

    fun setDataSet(result: MutableList<String>) {
        dataSet = result
        notifyDataSetChanged()
    }

    fun getDataSet() = dataSet
}