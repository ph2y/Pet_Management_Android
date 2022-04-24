package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityCreateupdatepostBinding
import com.sju18001.petmanagement.databinding.ItemCreateupdateposthashtagBinding

class CreateUpdatePostHashtagAdapter: RecyclerView.Adapter<CreateUpdatePostHashtagAdapter.ViewHolder>() {
    private var dataSet = mutableListOf<String>()

    class ViewHolder(
        private val adapter: CreateUpdatePostHashtagAdapter,
        private val binding: ItemCreateupdateposthashtagBinding
    ): RecyclerView.ViewHolder(binding.root) {
        fun bind(hashtag: String) {
            binding.adapter = adapter
            binding.holder = this
            binding.hashtag = hashtag

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ItemCreateupdateposthashtagBinding>(LayoutInflater.from(parent.context),
            R.layout.item_createupdateposthashtag, parent, false)
        return ViewHolder(this, binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount() = dataSet.size

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


    /** Databinding functions */
    fun onClickDeleteButton(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        deleteItem(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, dataSet.size)
    }
}