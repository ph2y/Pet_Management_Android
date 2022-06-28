package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ItemCreateupdatepostpetselectorBinding
import de.hdodenhof.circleimageview.CircleImageView

class CreateUpdatePostPetSelectorAdapter(
    private val createUpdatePostViewModel: CreateUpdatePostViewModel,
    private val context: Context
    ): RecyclerView.Adapter<CreateUpdatePostPetSelectorAdapter.ViewHolder>() {
    private var dataSet = mutableListOf<CreateUpdatePostPetSelectorItem>()

    class ViewHolder(
        private val adapter: CreateUpdatePostPetSelectorAdapter,
        private val binding: ItemCreateupdatepostpetselectorBinding,
        private val context: Context
    ): RecyclerView.ViewHolder(binding.root) {
        fun bind(petSelectorItem: CreateUpdatePostPetSelectorItem) {
            binding.adapter = adapter
            binding.holder = this
            binding.petSelectorItem = petSelectorItem
            binding.context = context

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ItemCreateupdatepostpetselectorBinding>(LayoutInflater.from(parent.context),
            R.layout.item_createupdatepostpetselector, parent, false)
        return ViewHolder(this, binding, context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount(): Int = dataSet.size

    fun addItem(item: CreateUpdatePostPetSelectorItem) {
        dataSet.add(item)
    }

    fun setPhoto(index: Int, photo: Bitmap?) {
        dataSet[index].petPhoto = photo
    }

    /** Databinding functions */
    fun onClickItem(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        val previousSelectedIndex: Int = createUpdatePostViewModel.selectedPetIndex

        createUpdatePostViewModel.selectedPetId.value = dataSet[position].petId
        createUpdatePostViewModel.selectedPetIndex = position

        if (previousSelectedIndex != -1) {
            dataSet[previousSelectedIndex].isSelected = false
            notifyItemChanged(previousSelectedIndex, dataSet[previousSelectedIndex])
        }
        dataSet[position].isSelected = true
        notifyItemChanged(position)
    }
}