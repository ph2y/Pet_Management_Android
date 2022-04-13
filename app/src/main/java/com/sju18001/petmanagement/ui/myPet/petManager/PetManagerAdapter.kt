package com.sju18001.petmanagement.ui.myPet.petManager

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dao.Pet
import com.sju18001.petmanagement.restapi.dto.FetchPetPhotoReqDto
import java.time.LocalDate
import java.util.*

interface PetManagerAdapterInterface {
    fun onClickCreateButton()
    fun restoreScroll()
    fun onClickPetCard(
        holder: PetManagerAdapter.HistoryListViewHolder,
        dataSet: ArrayList<Pet>,
        position: Int
    )
}

/**
 * 아이템이 일반 펫과 CreatePetButton으로 나뉩니다. 이러한 이유로 ViewHolder가 2개가 존재합니다.
 * 또한 onCreateViewHolder(), onBindViewHolder()에서 viewType으로 구분하여 분기합니다.
 */

class PetManagerAdapter(
        private val startDragListener: OnStartDragListener,
        private val context: Context,
        private val petManagerAdapterInterface: PetManagerAdapterInterface
    ) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), PetManagerDragAdapter.Listener {

    private var dataSet = ArrayList<Pet>()

    private var clickable: Boolean = true

    class HistoryListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val petPhoto: ImageView = itemView.findViewById(R.id.circleimageview_petphoto)
        val representativeIcon: ImageView = itemView.findViewById(R.id.imageview_representativeicon)
        val petName: TextView = itemView.findViewById(R.id.textview_petname)
        val petBreed: TextView = itemView.findViewById(R.id.textview_petbreed)
        val petAge: TextView = itemView.findViewById(R.id.textview_petage)
        val petBirthLayout: LinearLayout = itemView.findViewById(R.id.linearlayout_petbirth)
        val petBirth: TextView = itemView.findViewById(R.id.textview_petbirth)
        val petGender: TextView = itemView.findViewById(R.id.textview_petgender)
        val petMessage: TextView = itemView.findViewById(R.id.textview_petmessage)
    }

    class CreatePetButtonViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val iconImageView: ImageView = view.findViewById(R.id.imageview_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_pet_petmanager -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet_petmanager, parent, false)

                val holder = HistoryListViewHolder(view)
                setListenerOnView(holder)

                holder
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_createpetbutton_petmanager, parent, false)

                val holder = CreatePetButtonViewHolder(view)
                setListenerOnView(holder)

                holder
            }
        }
    }

    private fun setListenerOnView(holder: HistoryListViewHolder) {
        holder.itemView.setOnLongClickListener(View.OnLongClickListener {
            this.startDragListener.onStartDrag(holder)
            return@OnLongClickListener false
        })

        holder.itemView.setOnClickListener {
            if(!clickable) return@setOnClickListener
            if(holder.absoluteAdapterPosition == -1) return@setOnClickListener

            petManagerAdapterInterface.onClickPetCard(holder, dataSet, holder.absoluteAdapterPosition)
        }
    }

    private fun setListenerOnView(holder: CreatePetButtonViewHolder) {
        holder.iconImageView.setOnClickListener {
            petManagerAdapterInterface.onClickCreateButton()
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)){
            R.layout.item_pet_petmanager -> {
                holder as HistoryListViewHolder

                val currentItem = dataSet[position]

                setViewsAboutRepresentativePet(holder, currentItem)
                setViewsOfPetInfoLayout(holder, currentItem)
                setViewsOfPhoto(holder, currentItem)
            }
        }
    }

    private fun setViewsAboutRepresentativePet(holder: HistoryListViewHolder, currentItem: Pet) {
        val isRepresentativePet = currentItem.id == SessionManager.fetchLoggedInAccount(context)?.representativePetId?: 0
        if (isRepresentativePet) {
            holder.representativeIcon.setImageResource(R.drawable.crown)
            holder.representativeIcon.scaleType = ImageView.ScaleType.FIT_XY
            holder.representativeIcon.visibility = View.VISIBLE
        } else{
            holder.representativeIcon.visibility = View.INVISIBLE
        }
    }

    private fun setViewsOfPetInfoLayout(holder: HistoryListViewHolder, item: Pet){
        holder.petName.text = item.name
        holder.petBreed.text = item.breed
        holder.petAge.text = Util.getAgeFromBirth(item.birth).toString() + "살"
        holder.petMessage.text = if(item.message.isNullOrEmpty()) context.getString(R.string.filled_heart) else item.message

        if(item.yearOnly!!) holder.petBirthLayout.visibility = View.GONE
        else holder.petBirth.text = getBirthString(item.birth!!)

        holder.petGender.text = Util.getGenderSymbol(item.gender, context)
        if(item.gender) holder.petGender.setTextColor(context.getColor(R.color.pink))
        else holder.petGender.setTextColor(context.getColor(R.color.peter_river))
    }

    private fun getBirthString(birth: String): String {
        val localDate = LocalDate.parse(birth)
        return "${localDate.monthValue}월 ${localDate.dayOfMonth}일"
    }

    private fun setViewsOfPhoto(holder: HistoryListViewHolder, currentItem: Pet) {
        if(currentItem.photoUrl != null) fetchPetPhotoToSetPetPhoto(currentItem.id, holder.petPhoto)
        else holder.petPhoto.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_pets_60_with_padding))
    }

    private fun fetchPetPhotoToSetPetPhoto(id: Long, view: View) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .fetchPetPhotoReq(FetchPetPhotoReqDto(id))
        ServerUtil.enqueueApiCall(call, {false}, context, { response ->
            // Set fetched photo to view
            (view as ImageView).setImageBitmap(BitmapFactory.decodeStream(response.body()!!.byteStream()))
        }, {}, {})
    }

    override fun getItemCount() = dataSet.size + 1

    override fun getItemViewType(position: Int): Int {
        return if(position == dataSet.size) {
            R.layout.item_createpetbutton_petmanager
        } else {
            R.layout.item_pet_petmanager
        }
    }


    /** DragAdapter */
    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(dataSet, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(dataSet, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)

        // Save order to device
        val orderedPetIdList: ArrayList<Long> = arrayListOf()
        dataSet.map { orderedPetIdList.add(it.id) }
        PetManagerFragment().putOrderedPetIdList(context.getString(R.string.data_name_pet_list_id_order), orderedPetIdList, context)
    }

    override fun onRowSelected(itemViewHolder: HistoryListViewHolder) {
        val anim = ValueAnimator.ofFloat(1f, 0.5f)
        anim.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Float

            itemViewHolder.itemView.scaleX = value
            itemViewHolder.itemView.scaleY = value
            itemViewHolder.itemView.tag = "DRAGGED"
        }
        anim.duration = 100
        anim.start()

        // LongClick 직후에 곧바로 발생하는 Click을 방지하기 위함
        clickable = false
    }

    override fun onRowClear(itemViewHolder: HistoryListViewHolder) {
        val anim = ValueAnimator.ofFloat(0.5f, 1f)
        anim.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Float

            itemViewHolder.itemView.scaleX = value
            itemViewHolder.itemView.scaleY = value
            itemViewHolder.itemView.tag = ""
        }
        anim.duration = 100
        anim.start()
        
        clickable = true

        // 드래그가 끝난 뒤, 스크롤을 PagerSnapHelper에 맞춰서 복구
        petManagerAdapterInterface.restoreScroll()
    }

    fun setItem(index: Int, item: Pet) {
        dataSet[index] = item
    }

    fun addItem(item: Pet) {
        dataSet.add(item)
    }

    fun removeItemAt(index: Int) {
        dataSet.removeAt(index)
    }

    fun setResult(result: ArrayList<Pet>){
        this.dataSet = result
        notifyDataSetChanged()
    }
}