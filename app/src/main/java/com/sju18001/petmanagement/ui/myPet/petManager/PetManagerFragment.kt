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
    // variable for ViewModel
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

        // Initialize RecyclerView
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

                // if pet photo not yet fetched
                if (currentItem.photoUrl != null && holder.petPhoto.drawable == null) {
                    return
                }

                // set pet values to Intent
                val petProfileIntent = Intent(holder.itemView.context, PetProfileActivity::class.java)
                if(currentItem.photoUrl != null) {
                    val photoByteArray = Util.getByteArrayFromDrawable(holder.petPhoto.drawable)
                    Util.saveByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
                        requireContext().getString(R.string.data_name_my_pet_selected_pet_photo), photoByteArray)
                }
                else {
                    Util.saveByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
                        requireContext().getString(R.string.data_name_my_pet_selected_pet_photo), null)
                }
                petProfileIntent.putExtra("petId", currentItem.id)
                petProfileIntent.putExtra("petName", currentItem.name)
                petProfileIntent.putExtra("petBirth", currentItem.birth)
                petProfileIntent.putExtra("petSpecies", currentItem.species)
                petProfileIntent.putExtra("petBreed", currentItem.breed)
                petProfileIntent.putExtra("petGender", currentItem.gender)
                petProfileIntent.putExtra("petAge", Util.getAgeFromBirth(currentItem.birth))
                petProfileIntent.putExtra("petMessage", currentItem.message)
                val isRepresentativePet = currentItem.id == SessionManager.fetchLoggedInAccount(requireContext())?.representativePetId?: 0
                petProfileIntent.putExtra("isRepresentativePet", isRepresentativePet)
                petProfileIntent.putExtra("yearOnly", currentItem.yearOnly)

                petProfileIntent.putExtra("activityType", PetProfileActivity.ActivityType.PET_PROFILE.ordinal)

                // open activity
                holder.itemView.context.startActivity(petProfileIntent)
                requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
            }
        })
        binding.myPetListRecyclerView.adapter = adapter

        // Initialize PagerSnapHelper
        snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.myPetListRecyclerView)

        // Initialize LayoutManager
        layoutManager = CustomLayoutManager.Builder(requireContext(), snapHelper)
            .setMode(CustomLayoutManager.Mode.SCALE_MODE)
            .build()
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.myPetListRecyclerView.layoutManager = layoutManager


        // set adapter item change observer
        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()

                setEmptyNotificationView(adapter.itemCount)
            }
        })


        touchHelper = ItemTouchHelper(PetManagerDragAdapter(adapter))
        touchHelper.attachToRecyclerView(binding.myPetListRecyclerView)

        return root
    }

    override fun onResume() {
        super.onResume()

        // 첫 Fetch가 끝나기 전까지 ProgressBar 표시
        CustomProgressBar.addProgressBar(requireContext(), binding.fragmentPetPetManagerParentLayout, 80, R.color.white)

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPetReq(FetchPetReqDto( null , null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            val petListApi: ArrayList<Pet> = ArrayList()
            response.body()?.petList?.map {
                petListApi.add(it)
                myPetViewModel.addPetNameForId(it.id, it.name)
            }

            // sync에 필요한 petNameForId에 값이 들어갔으므로
            synchronizeAlarmManager()


            // if RecyclerView items not yet added
            if(adapter.itemCount == 1) {
                updatePetListOrder(petListApi)
                reorderPetList(petListApi)

                // set result + restore last scrolled index
                adapter.setResult(petList)
                binding.myPetListRecyclerView.scrollToPosition(myPetViewModel.lastScrolledIndex)
            }
            // check for difference in lists(current RecyclerView vs API response)
            else {
                checkListDifference(petListApi)
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

    private fun updatePetListOrder(apiResponse: ArrayList<Pet>) {
        // get current saved pet list order
        val petListOrder = getPetListOrder(requireContext()
            .getString(R.string.data_name_pet_list_id_order), requireContext())

        // check for not deleted
        val notDeleted: MutableList<Long> = mutableListOf()
        for(id in petListOrder) {
            val deleted = apiResponse.find { it.id == id }
            if(deleted == null) {
                notDeleted.add(id)
            }
        }

        // check for not added
        val notAdded: MutableList<Long> = mutableListOf()
        for(pet in apiResponse) {
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

    private fun reorderPetList(apiResponse: ArrayList<Pet>) {
        // get saved pet list order
        val petListOrder = getPetListOrder(requireContext()
            .getString(R.string.data_name_pet_list_id_order), requireContext())

        // sort by order
        petList = mutableListOf()
        for(id in petListOrder) {
            val pet = apiResponse.find { it.id == id }
            petList.add(pet!!)
        }
    }

    private fun checkListDifference(apiResponse: ArrayList<Pet>) {
        // variables for id lists
        val apiResponseIdList: MutableList<Long> = mutableListOf()
        val recyclerViewIdList: MutableList<Long> = mutableListOf()

        // get id lists for API/RecyclerView
        apiResponse.map {
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
            petList.add(petList.size, apiResponse[apiResponse.size - 1])
            adapter.notifyItemInserted(petList.size)
            binding.myPetListRecyclerView.smoothScrollToPosition(petList.size - 1)
            updatePetListOrder(apiResponse)
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
            updatePetListOrder(apiResponse)
            return
        }

        // if there are multiple/no differences -> update list with all changes + no animation
        updatePetListOrder(apiResponse)
        reorderPetList(apiResponse)
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

    private fun setEmptyNotificationView(itemCount: Int) {
        // set notification view
        val visibility = if(itemCount != 1) View.GONE else View.VISIBLE
        binding.emptyPetListNotification.visibility = visibility
    }
}