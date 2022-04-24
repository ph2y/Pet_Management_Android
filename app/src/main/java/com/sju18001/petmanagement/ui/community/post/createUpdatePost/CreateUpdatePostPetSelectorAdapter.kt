package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sju18001.petmanagement.R
import de.hdodenhof.circleimageview.CircleImageView

class CreateUpdatePostPetSelectorAdapter(
    private val createUpdatePostViewModel: CreateUpdatePostViewModel,
    private val context: Context
    ): RecyclerView.Adapter<CreateUpdatePostPetSelectorAdapter.ViewHolder>() {
    private var dataSet = mutableListOf<CreateUpdatePostPetSelectorItem>()

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val layout: LinearLayout = view.findViewById(R.id.linearlayout_parent)
        val representativeIcon: ImageView = view.findViewById(R.id.imageview_representativeicon)
        val petPhoto: CircleImageView = view.findViewById(R.id.circleimageview_petphoto)
        val selectionIcon: CircleImageView = view.findViewById(R.id.circleimageview_selectionicon)
        val petName: TextView = view.findViewById(R.id.textview_petname)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_createupdatepostpetselector, parent, false)

        val holder = ViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // set item views
        if (dataSet[position].petPhotoUrl != null) {
            Glide.with(context).load(dataSet[position].petPhoto).into(holder.petPhoto)
        } else {
            Glide.with(context)
                .load(context.getDrawable(R.drawable.ic_baseline_pets_60_with_padding)).into(holder.petPhoto)
        }

        if (dataSet[position].isRepresentativePet) {
            holder.representativeIcon.visibility = View.VISIBLE
        } else {
            holder.representativeIcon.visibility = View.INVISIBLE
        }

        if (dataSet[position].isSelected) {
            holder.layout.alpha = 1f
            holder.selectionIcon.visibility = View.VISIBLE
        } else {
            holder.layout.alpha = .5f
            holder.selectionIcon.visibility = View.INVISIBLE
        }

        holder.petName.text = dataSet[position].petName
    }

    override fun getItemCount(): Int = dataSet.size

    private fun setListenerOnView(holder: ViewHolder) {
        holder.layout.setOnClickListener {
            val position = holder.absoluteAdapterPosition

            val previousSelectedIndex: Int = createUpdatePostViewModel.selectedPetIndex

            // update selected pet values
            createUpdatePostViewModel.selectedPetId.value = dataSet[position].petId
            createUpdatePostViewModel.selectedPetIndex = position

            // update previous and current pet's flags
            if (previousSelectedIndex != -1) {
                dataSet[previousSelectedIndex].isSelected = false
            }
            dataSet[position].isSelected = true

            // update view changes
            if (previousSelectedIndex != -1) {
                // passed object to not show the animation
                notifyItemChanged(previousSelectedIndex, dataSet[previousSelectedIndex])
            }
            notifyItemChanged(position)
        }
    }

    fun setDataSet(newDataSet: MutableList<CreateUpdatePostPetSelectorItem>) {
        dataSet = newDataSet
        notifyDataSetChanged()
    }
}