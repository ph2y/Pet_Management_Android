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
        resultList: List<Pet>,
        position: Int
    )
}

class PetManagerAdapter(
        private val startDragListener: OnStartDragListener,
        private val context: Context,
        private val petManagerAdapterInterface: PetManagerAdapterInterface
    ) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), PetManagerDragAdapter.Listener {

    private var resultList = emptyList<Pet>()

    private var clickable: Boolean = true

    class HistoryListViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val petPhoto: ImageView = itemView.findViewById(R.id.circleimageview_petmanager_petphoto)
        val representativeIcon: ImageView = itemView.findViewById(R.id.imageview_petmanager_representativeicon)
        val petName: TextView = itemView.findViewById(R.id.textview_petmanager_petname)
        val petBreed: TextView = itemView.findViewById(R.id.textview_petmanager_petbreed)
        val petAge: TextView = itemView.findViewById(R.id.textview_petmanager_petage)
        val petBirthLayout: LinearLayout = itemView.findViewById(R.id.linearlayout_petmanager_petbirth)
        val petBirth: TextView = itemView.findViewById(R.id.textview_petmanager_petbirth)
        val petGender: TextView = itemView.findViewById(R.id.textview_petmanager_petgender)
        val petMessage: TextView = itemView.findViewById(R.id.textview_petmanager_petmessage)
    }

    class CreatePetButtonViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val iconImageView: ImageView = view.findViewById(R.id.imageview_createpetbutton_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_petmanager -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_petmanager, parent, false)

                val holder = HistoryListViewHolder(view)
                setListenerOnView(holder)

                holder
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_createpetbutton, parent, false)

                val holder = CreatePetButtonViewHolder(view)
                setListenerOnView(holder)

                holder
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)){
            R.layout.item_petmanager -> {
                holder as HistoryListViewHolder

                val currentItem = resultList[position]

                // Set representative pet icon
                val isRepresentativePet = currentItem.id == SessionManager.fetchLoggedInAccount(context)?.representativePetId?: 0
                if (isRepresentativePet) {
                    holder.representativeIcon.setImageResource(R.drawable.crown)
                    holder.representativeIcon.scaleType = ImageView.ScaleType.FIT_XY
                    holder.representativeIcon.visibility = View.VISIBLE
                } else{
                    holder.representativeIcon.visibility = View.INVISIBLE
                }

                // Set pet photo
                if(currentItem.photoUrl != null) {
                    fetchPetPhoto(currentItem.id, holder.petPhoto)
                }
                else {
                    holder.petPhoto.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_pets_60_with_padding))
                }

                setPetInfoLayout(holder, currentItem)
            }
        }
    }

    override fun getItemCount() = resultList.size + 1

    override fun getItemViewType(position: Int): Int {
        return if(position == resultList.size) {
            R.layout.item_createpetbutton
        } else {
            R.layout.item_petmanager
        }
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(resultList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(resultList, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)

        // save order to device
        val petListIdOrder: MutableList<Long> = mutableListOf()
        resultList.map {
            petListIdOrder.add(it.id)
        }
        PetManagerFragment().savePetListOrder(context.getString(R.string.data_name_pet_list_id_order),
            petListIdOrder, context)
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

    private fun setListenerOnView(holder: HistoryListViewHolder) {
        // Long clicking the item to draging
        holder.itemView.setOnLongClickListener(View.OnLongClickListener {
            this.startDragListener.onStartDrag(holder)

            return@OnLongClickListener false
        })

        // click -> open pet profile
        holder.itemView.setOnClickListener {
            if(!clickable) return@setOnClickListener
            if(holder.absoluteAdapterPosition == -1) return@setOnClickListener
            petManagerAdapterInterface.onClickPetCard(holder, resultList, holder.absoluteAdapterPosition)
        }
    }

    private fun setListenerOnView(holder: CreatePetButtonViewHolder) {
        holder.iconImageView.setOnClickListener {
            petManagerAdapterInterface.onClickCreateButton()
        }
    }

    private fun fetchPetPhoto(id: Long, view: View) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .fetchPetPhotoReq(FetchPetPhotoReqDto(id))
        ServerUtil.enqueueApiCall(call, {false}, context, { response ->
            // set fetched photo to view
            (view as ImageView).setImageBitmap(BitmapFactory.decodeStream(response.body()!!.byteStream()))
        }, {}, {})
    }

    private fun setPetInfoLayout(holder: HistoryListViewHolder, item: Pet){
        holder.petName.text = item.name
        holder.petBreed.text = item.breed
        holder.petAge.text = Util.getAgeFromBirth(item.birth) + "살"

        // Set visibility of pet_birth_layout
        if(item.yearOnly!!){
            holder.petBirthLayout.visibility = View.GONE
        } else {
            holder.petBirth.text = getBirthString(item.birth!!)
        }

        holder.petGender.text = Util.getGenderSymbol(item.gender, context)
        // Set color of pet_gender
        if(item.gender) holder.petGender.setTextColor(context.getColor(R.color.pink))
        else holder.petGender.setTextColor(context.getColor(R.color.peter_river))

        holder.petMessage.text =
            if(item.message.isNullOrEmpty()) context.getString(R.string.filled_heart) else item.message
    }

    private fun getBirthString(birth: String): String {
        val localDate = LocalDate.parse(birth)
        return "${localDate.monthValue}월 ${localDate.dayOfMonth}일"
    }

    public fun setResult(result: List<Pet>){
        this.resultList = result
        notifyDataSetChanged()
    }
}