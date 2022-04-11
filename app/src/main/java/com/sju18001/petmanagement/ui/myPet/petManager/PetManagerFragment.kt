package com.sju18001.petmanagement.ui.myPet.petManager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.CustomProgressBar
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentPetManagerBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dao.Pet
import com.sju18001.petmanagement.restapi.dto.FetchPetReqDto
import com.sju18001.petmanagement.ui.myPet.MyPetViewModel
import com.sju18001.petmanagement.ui.myPet.petManager.petProfile.CreateUpdatePetActivity
import com.sju18001.petmanagement.ui.myPet.petManager.petProfile.PetProfileActivity
import com.sju18001.petmanagement.ui.myPet.petScheduleManager.PetScheduleNotification
import com.sju18001.petmanagement.ui.myPet.petScheduleManager.PetScheduleNotification.Companion.cancelAll
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type

class PetManagerFragment : Fragment(), OnStartDragListener {
    private val myPetViewModel: MyPetViewModel by activityViewModels()

    // variables for view binding
    private var _binding: FragmentPetManagerBinding? = null
    private val binding get() = _binding!!

    // variables for RecyclerView
    private lateinit var adapter: PetManagerAdapter
    private var petList: MutableList<Pet> = mutableListOf()
    lateinit var touchHelper: ItemTouchHelper

    private lateinit var snapHelper: SnapHelper
    private lateinit var layoutManager: CustomLayoutManager

    private var isViewDestroyed = false

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        touchHelper.startDrag(viewHolder)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // view binding
        _binding = FragmentPetManagerBinding.inflate(inflater, container, false)
        isViewDestroyed = false

        val root: View = binding.root

        initializeRecyclerView()

        return root
    }

    private fun initializeRecyclerView() {
        adapter = PetManagerAdapter(this, requireActivity(), object: PetManagerAdapterInterface{
            override fun onClickCreateButton() {
                val createUpdatePetIntent = Intent(context, CreateUpdatePetActivity::class.java)
                createUpdatePetIntent.putExtra("activityType", CreateUpdatePetActivity.ActivityType.CREATE_PET.ordinal)
                startActivity(createUpdatePetIntent)
            }

            override fun restoreScroll() {
                snapHelper.findSnapView(layoutManager)?.let {
                    val position = layoutManager.getPosition(it)
                    binding.myPetListRecyclerView.smoothScrollToPosition(position)
                }
            }

            override fun onClickPetCard(
                holder: PetManagerAdapter.HistoryListViewHolder,
                dataSet: List<Pet>,
                position: Int
            ) {
                val currentItem = dataSet[position]

                // 사진을 아직 불러오지 못한 경우에는 PetProfile을 열지 않습니다.
                // 이 프래그먼트에서 불러온 사진을 PetProfile로 전달하는 방식을 사용하기 때문입니다.
                if (currentItem.photoUrl != null && holder.petPhoto.drawable == null) return

                // 사진 정보는 SharedPreferences를 이용하여 전달합니다.
                if(currentItem.photoUrl != null) {
                    val photoByteArray = Util.getByteArrayFromDrawable(holder.petPhoto.drawable)
                    Util.saveByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
                        requireContext().getString(R.string.data_name_my_pet_selected_pet_photo), photoByteArray)
                }
                else {
                    Util.saveByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
                        requireContext().getString(R.string.data_name_my_pet_selected_pet_photo), null)
                }

                // PetProfile로 전달할 정보들
                val petProfileIntent = Intent(holder.itemView.context, PetProfileActivity::class.java)

                petProfileIntent.putExtra("petId", currentItem.id)
                petProfileIntent.putExtra("petName", currentItem.name)
                petProfileIntent.putExtra("petBirth", currentItem.birth)
                petProfileIntent.putExtra("petSpecies", currentItem.species)
                petProfileIntent.putExtra("petBreed", currentItem.breed)
                petProfileIntent.putExtra("petGender", currentItem.gender)
                petProfileIntent.putExtra("petAge", Util.getAgeFromBirth(currentItem.birth))
                petProfileIntent.putExtra("petMessage", currentItem.message)
                petProfileIntent.putExtra("yearOnly", currentItem.yearOnly)
                petProfileIntent.putExtra("activityType", PetProfileActivity.ActivityType.PET_PROFILE.ordinal)

                val isRepresentativePet = currentItem.id == SessionManager.fetchLoggedInAccount(requireContext())?.representativePetId?: 0
                petProfileIntent.putExtra("isRepresentativePet", isRepresentativePet)

                // 인텐드와 함께 PetProfileActivity를 실행합니다.
                holder.itemView.context.startActivity(petProfileIntent)
                requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
            }
        })
        binding.myPetListRecyclerView.adapter = adapter

        // PagerSnapHelper: ViewPager처럼 움직이는 RecyclerView 구현
        snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.myPetListRecyclerView)

        // CustomLayoutManager: 스크롤 애니메이션 구현
        layoutManager = CustomLayoutManager.Builder(requireContext(), snapHelper)
            .setMode(CustomLayoutManager.Mode.SCALE_MODE)
            .build()
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.myPetListRecyclerView.layoutManager = layoutManager

        // ItemTouchHelper: Drag & drop 구현
        touchHelper = ItemTouchHelper(PetManagerDragAdapter(adapter))
        touchHelper.attachToRecyclerView(binding.myPetListRecyclerView)

        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                setEmptyNotificationView(adapter.itemCount)
            }
        })
    }

    private fun setEmptyNotificationView(itemCount: Int) {
        val visibility = if(itemCount != 1) View.GONE else View.VISIBLE
        binding.emptyPetListNotification.visibility = visibility
    }


    override fun onResume() {
        super.onResume()

        CustomProgressBar.addProgressBar(requireContext(), binding.fragmentPetPetManagerParentLayout, 80, R.color.white)
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPetReq(FetchPetReqDto( null , null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            val fetchedPetList: ArrayList<Pet> = ArrayList()
            response.body()?.petList?.map {
                fetchedPetList.add(it)
                myPetViewModel.addPetNameForId(it.id, it.name)
            }

            synchronizeAlarmManager()

            // item에 CreatePetButton만 있을 경우(즉, 펫이 없을 경우)
            if(adapter.itemCount == 1) {
                updatePetListOrder(fetchedPetList)
                reorderPetList(fetchedPetList)

                // set result + restore last scrolled index
                adapter.setResult(petList)
                binding.myPetListRecyclerView.scrollToPosition(myPetViewModel.lastScrolledIndex)
            }else{
                checkListDifference(fetchedPetList)
            }

            adapter.notifyDataSetChanged()

            CustomProgressBar.removeProgressBar(binding.fragmentPetPetManagerParentLayout)
        }, { CustomProgressBar.removeProgressBar(binding.fragmentPetPetManagerParentLayout) },
            { CustomProgressBar.removeProgressBar(binding.fragmentPetPetManagerParentLayout) })
    }

    private fun synchronizeAlarmManager() {
        cancelAll(requireContext())

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPetScheduleReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            // ON인 것들에 대해 알림 설정
            response.body()?.petScheduleList?.map{
                if(it.enabled){
                    PetScheduleNotification.setAlarmManagerRepeating(
                        requireContext(),
                        it.id,
                        it.time,
                        Util.getPetNamesFromPetIdList(myPetViewModel.petNameForId, it.petIdList),
                        it.memo
                    )
                }
            }
        }, {}, {})
    }

    private fun updatePetListOrder(fetchedPetList: ArrayList<Pet>) {
        // get current saved pet list order
        val petListOrder = getPetListOrder(requireContext()
            .getString(R.string.data_name_pet_list_id_order), requireContext())

        // check for not deleted
        val notDeleted: MutableList<Long> = mutableListOf()
        for(id in petListOrder) {
            val deleted = fetchedPetList.find { it.id == id }
            if(deleted == null) {
                notDeleted.add(id)
            }
        }

        // check for not added
        val notAdded: MutableList<Long> = mutableListOf()
        for(pet in fetchedPetList) {
            val added = petListOrder.find { it == pet.id }
            if(added == null) {
                notAdded.add(pet.id)
            }
        }

        // update pet list order
            // delete not deleted
        for(id in notDeleted) {
            petListOrder.remove(id)
        }
            // add not added
        for(id in notAdded) {
            petListOrder.add(id)
        }

        // save to device(SharedPreferences)
        savePetListOrder(requireContext().getString(R.string
            .data_name_pet_list_id_order), petListOrder, requireContext())
    }

    private fun reorderPetList(fetchedPetList: ArrayList<Pet>) {
        // get saved pet list order
        val petListOrder = getPetListOrder(requireContext()
            .getString(R.string.data_name_pet_list_id_order), requireContext())

        // sort by order
        petList = mutableListOf()
        for(id in petListOrder) {
            val pet = fetchedPetList.find { it.id == id }
            petList.add(pet!!)
        }
    }

    private fun checkListDifference(fetchedPetList: ArrayList<Pet>) {
        // variables for id lists
        val apiResponseIdList: MutableList<Long> = mutableListOf()
        val recyclerViewIdList: MutableList<Long> = mutableListOf()

        // get id lists for API/RecyclerView
        fetchedPetList.map {
            apiResponseIdList.add(it.id)
        }
        petList.map {
            recyclerViewIdList.add(it.id)
        }

        // get added/deleted id
        val addedId = apiResponseIdList.minus(recyclerViewIdList)
        val deletedId = recyclerViewIdList.minus(apiResponseIdList)

        // show add/delete animation(if difference size is 1)
            // if added
        if(addedId.size == 1 && deletedId.isEmpty()) {
            petList.add(petList.size, fetchedPetList[fetchedPetList.size - 1])
            adapter.notifyItemInserted(petList.size)
            binding.myPetListRecyclerView.smoothScrollToPosition(petList.size - 1)
            updatePetListOrder(fetchedPetList)
            return
        }
            // if deleted
        if(deletedId.size == 1 && addedId.isEmpty()) {
            for(i in 0 until petList.size) {
                if(petList[i].id == deletedId[0]) {
                    petList.removeAt(i)
                    adapter.notifyItemRemoved(i)
                    adapter.notifyItemRangeChanged(i, adapter.itemCount)
                    break
                }
            }
            val position = if(petList.size >= 1) petList.size else 0
            binding.myPetListRecyclerView.smoothScrollToPosition(position)
            updatePetListOrder(fetchedPetList)
            return
        }

        // if there are multiple/no differences -> update list with all changes + no animation
        updatePetListOrder(fetchedPetList)
        reorderPetList(fetchedPetList)
        adapter.setResult(petList)
    }

    public fun savePetListOrder(dataName: String, list: MutableList<Long>, context: Context) {
        val preferences: SharedPreferences = context.getSharedPreferences(
            context.getString(R.string.pref_name_pet_list_id_order), Context.MODE_PRIVATE)
        val json: String = Gson().toJson(list)

        preferences.edit().putString(dataName, json).apply()
    }

    public fun getPetListOrder(dataName: String?, context: Context): MutableList<Long> {
        val preferences: SharedPreferences = context.getSharedPreferences(
            context.getString(R.string.pref_name_pet_list_id_order), Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String? = preferences.getString(dataName, null)
        val type: Type = object : TypeToken<MutableList<Long>>() {}.type

        if(json == null) { return mutableListOf() }
        return gson.fromJson(json, type)
    }
}