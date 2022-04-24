package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityCreateupdatepostBinding
import com.sju18001.petmanagement.databinding.ItemCreateupdatepostgeneralfileBinding
import java.io.File

class CreateUpdatePostGeneralFileAdapter(
    private val createUpdatePostViewModel: CreateUpdatePostViewModel
    ) : RecyclerView.Adapter<CreateUpdatePostGeneralFileAdapter.ViewHolder>() {
    private var dataSet = mutableListOf<String>()

    class ViewHolder(
        private val adapter: CreateUpdatePostGeneralFileAdapter,
        private val binding: ItemCreateupdatepostgeneralfileBinding
    ): RecyclerView.ViewHolder(binding.root) {
        fun bind(generalFile: String) {
            binding.adapter = adapter
            binding.holder = this
            binding.generalFile = generalFile

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ItemCreateupdatepostgeneralfileBinding>(LayoutInflater.from(parent.context),
            R.layout.item_createupdatepostgeneralfile, parent, false)
        return ViewHolder(this, binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount() = dataSet.size

    fun addItem(item: String) {
        dataSet.add(item)
    }

    /** Databinding functions */
    fun onClickDeleteButton(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition

        File(createUpdatePostViewModel.generalFilePathList.value!![position]).delete()

        createUpdatePostViewModel.removeGeneralFileNameAt(position)
        createUpdatePostViewModel.removeGeneralFilePathAt(position)

        dataSet.removeAt(position)
        notifyItemRemoved(position)
    }
}