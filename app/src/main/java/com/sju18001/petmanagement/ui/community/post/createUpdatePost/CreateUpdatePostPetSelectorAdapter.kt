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

class CreateUpdatePostPetSelectorAdapter(private val createUpdatePostViewModel: CreateUpdatePostViewModel, private val context: Context,
                     private val confirmButtonAndUsageInterface: ConfirmButtonAndUsageInterface):
    RecyclerView.Adapter<CreateUpdatePostPetSelectorAdapter.ViewHolder>() {

    private var dataSet = mutableListOf<CreateUpdatePostPetSelectorItem>()

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val linearLayout: LinearLayout = view.findViewById(R.id.linearlayout_createupdatepostpetselector)
        val representativeIconImageView: ImageView = view.findViewById(R.id.imageview_createupdatepostpetselector_representativeicon)
        val photoCircleImageView: CircleImageView = view.findViewById(R.id.circleimageview_createupdatepostpetselector_photo)
        val selectionIconCircleImageView: CircleImageView = view.findViewById(R.id.circleimageview_createupdatepostpetselector_selectionicon)
        val nameTextView: TextView = view.findViewById(R.id.textview_createupdatepostpetselector_name)
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
            Glide.with(context).load(dataSet[position].petPhoto).into(holder.photoCircleImageView)
        } else {
            Glide.with(context)
                .load(context.getDrawable(R.drawable.ic_baseline_pets_60_with_padding)).into(holder.photoCircleImageView)
        }

        if (dataSet[position].isRepresentativePet) {
            holder.representativeIconImageView.visibility = View.VISIBLE
        } else {
            holder.representativeIconImageView.visibility = View.INVISIBLE
        }

        if (dataSet[position].isSelected) {
            holder.linearLayout.alpha = 1f
            holder.selectionIconCircleImageView.visibility = View.VISIBLE
        } else {
            holder.linearLayout.alpha = .5f
            holder.selectionIconCircleImageView.visibility = View.INVISIBLE
        }

        holder.nameTextView.text = dataSet[position].petName
    }

    override fun getItemCount(): Int = dataSet.size

    private fun setListenerOnView(holder: ViewHolder) {
        holder.linearLayout.setOnClickListener {
            val position = holder.absoluteAdapterPosition

            val previousSelectedIndex: Int = createUpdatePostViewModel.selectedPetIndex

            // update selected pet values
            createUpdatePostViewModel.selectedPetId = dataSet[position].petId
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

            confirmButtonAndUsageInterface.verifyAndEnableConfirmButton()
        }
    }

    fun updateDataSet(newDataSet: MutableList<CreateUpdatePostPetSelectorItem>) {
        dataSet = newDataSet
        notifyDataSetChanged()
    }
}