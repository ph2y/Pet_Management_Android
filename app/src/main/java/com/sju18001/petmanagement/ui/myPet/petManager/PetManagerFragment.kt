package com.sju18001.petmanagement.ui.myPet.petManager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.CustomProgressBar
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentPetmanagerBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dao.Pet
import com.sju18001.petmanagement.restapi.dto.FetchPetReqDto
import com.sju18001.petmanagement.ui.myPet.MyPetViewModel
import com.sju18001.petmanagement.ui.myPet.petManager.createUpdatePet.CreateUpdatePetActivity
import com.sju18001.petmanagement.ui.myPet.petManager.petProfile.PetProfileActivity
import com.sju18001.petmanagement.ui.myPet.petScheduleManager.PetScheduleNotification
import com.sju18001.petmanagement.ui.myPet.petScheduleManager.PetScheduleNotification.Companion.cancelAll
import java.lang.reflect.Type

/**
 * 해당 프래그먼트는 펫의 Create, Update, Delete에 영향을 받을 수 있습니다. 이를 위해서 registerForActivityResult를 이용하여
 * 액티비티를 실행한 뒤, 그 결과를 Intent로 전달 받습니다. ResultCode에는 위 3개의 경우에 대해 1개씩 존재합니다.(In companion object)
 * 펫의 정보는 계속해서 최신의 것으로 업데이트가 되며, 따라서 전체 Pet에 대한 Fetch 작업은 최초 1회만 수행합니다.
 *
 * 다음은 각각의 경우에 대한 작업 수행 과정입니다.
 * Create: CreateUpdatePet: 펫 생성 -> PetManager: (HAS_PET_CREATED, createdPetId)를 결과로 받음
 * Update: PetProfile -> CreateUpdatePet: 펫 수정 -> PetProfile -> PetManager: (HAS_PET_UPDATED, updatedPetId)를 결과로 받음
 *         펫 수정을 마친 뒤 PetProfile로 돌아오면 hasPetUpdated이라는 값에 true를 대입하고, 액티비티가 종료될 때 이 값으로 업데이트 여부를 파악합니다.
 * Delete: PetProfile -> CreateUpdatePet: 펫 삭제 -> PetProfile: 강제로 PetManager 이동 -> PetManager: (HAS_PET_DELETED)를 결과로 받음
 */

class PetManagerFragment : Fragment(), OnStartDragListener {
    companion object {
        // 펫이 생성/삭제/수정될 때 ActivityResult로 전달되는 코드입니다.
        const val HAS_PET_CREATED = 10
        const val HAS_PET_DELETED = 11
        const val HAS_PET_UPDATED = 12
    }

    private val myPetViewModel: MyPetViewModel by activityViewModels()

    private var _binding: FragmentPetmanagerBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PetManagerAdapter
    lateinit var touchHelper: ItemTouchHelper
    private lateinit var snapHelper: SnapHelper
    private lateinit var layoutManager: CustomLayoutManager

    private var isViewDestroyed = false

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        when(result.resultCode){
            HAS_PET_CREATED -> {
                // 생성된 펫을 Fetch한 뒤 RecyclerView에 추가합니다.
                val createdPetId = result.data?.getLongExtra("createdPetId", -1)
                val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                    .fetchPetReq(FetchPetReqDto(createdPetId, null))
                ServerUtil.enqueueApiCall(call, { isViewDestroyed }, requireContext(), {
                    it.body()?.petList?.get(0)?.let { createdPet ->
                        adapter.addItem(createdPet)
                        adapter.notifyItemInserted(adapter.itemCount)
                        binding.recyclerviewPet.smoothScrollToPosition(adapter.itemCount - 2)

                        val petListOrder = getOrderedPetIdList(requireContext()
                            .getString(R.string.data_name_pet_list_id_order), requireContext())
                        petListOrder.add(createdPetId!!)
                        putOrderedPetIdList(requireContext().getString(R.string
                            .data_name_pet_list_id_order), petListOrder, requireContext())


                        myPetViewModel.setPetNameForId(createdPetId!!, it.body()!!.petList[0].name)
                    }
                },{},{})
            }
            HAS_PET_DELETED -> {
                val centerPosition = getCenterPosition()
                if (centerPosition != null) {
                    adapter.removeItemAt(centerPosition)
                    adapter.notifyItemRemoved(centerPosition)
                    adapter.notifyItemRangeChanged(centerPosition, adapter.itemCount)
                }
            }
            HAS_PET_UPDATED -> {
                // 수정된 펫을 Fetch한 뒤 RecyclerView에 적용합니다.
                val updatedPetId = result.data?.getLongExtra("updatedPetId", -1)
                val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                    .fetchPetReq(FetchPetReqDto(updatedPetId, null))
                ServerUtil.enqueueApiCall(call, { isViewDestroyed }, requireContext(), {
                    it.body()?.petList?.get(0)?.let { updatedPet ->
                        val centerPosition = getCenterPosition()
                        if(centerPosition != null) {
                            adapter.setItem(centerPosition, updatedPet)
                            adapter.notifyItemChanged(centerPosition)
                        }


                        myPetViewModel.setPetNameForId(updatedPetId!!, it.body()!!.petList[0].name)
                    }
                },{},{})
            }
        }
    }

    private fun getCenterPosition(): Int? {
        return snapHelper.findSnapView(layoutManager)?.let { layoutManager.getPosition(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPetmanagerBinding.inflate(inflater, container, false)
        isViewDestroyed = false

        initializeRecyclerView()
        fetchPet()

        return binding.root
    }

    /** initializeRecyclerView() */
    private fun initializeRecyclerView() {
        adapter = PetManagerAdapter(this, requireActivity(), object: PetManagerAdapterInterface{
            override fun onClickCreateButton() {
                val createUpdatePetIntent = Intent(context, CreateUpdatePetActivity::class.java)
                createUpdatePetIntent.putExtra("activityType", CreateUpdatePetActivity.ActivityType.CREATE_PET.ordinal)
                startForResult.launch(createUpdatePetIntent)
            }

            override fun restoreScroll() {
                snapHelper.findSnapView(layoutManager)?.let {
                    val position = layoutManager.getPosition(it)
                    binding.recyclerviewPet.smoothScrollToPosition(position)
                }
            }

            override fun onClickPetCard(
                holder: PetManagerAdapter.HistoryListViewHolder,
                dataSet: ArrayList<Pet>,
                position: Int
            ) {
                val centerPosition = getCenterPosition()
                // 옆에 있는 펫을 누른 경우: 그쪽으로 이동
                if(position != centerPosition){
                    binding.recyclerviewPet.smoothScrollToPosition(position)
                }else{
                    val currentItem = dataSet[position]
                    // 사진을 아직 불러오지 못한 경우
                    if (currentItem.photoUrl != null && holder.petPhoto.drawable == null) return

                    savePetPhotoByteArrayToSharedPreferences(holder, currentItem)

                    val intent = makePetProfileIntent(currentItem)
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), holder.petPhoto, "petPhoto")
                    startForResult.launch(intent, options)
                }
            }
        })
        binding.recyclerviewPet.adapter = adapter

        // PagerSnapHelper: ViewPager처럼 움직이는 RecyclerView 구현
        snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerviewPet)

        // CustomLayoutManager: 스크롤 애니메이션 구현
        layoutManager = CustomLayoutManager.Builder(requireContext(), snapHelper)
            .setMode(CustomLayoutManager.Mode.SCALE_MODE)
            .build()
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.recyclerviewPet.layoutManager = layoutManager

        // ItemTouchHelper: Drag & drop 구현
        touchHelper = ItemTouchHelper(PetManagerDragAdapter(adapter))
        touchHelper.attachToRecyclerView(binding.recyclerviewPet)

        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                setEmptyNotificationView(adapter.itemCount)
            }
        })
    }

    private fun makePetProfileIntent(currentItem: Pet): Intent {
        val intent = Intent(context, PetProfileActivity::class.java)
        intent.putExtra("petId", currentItem.id)
        intent.putExtra("petName", currentItem.name)
        intent.putExtra("petBirth", currentItem.birth)
        intent.putExtra("petSpecies", currentItem.species)
        intent.putExtra("petBreed", currentItem.breed)
        intent.putExtra("petGender", currentItem.gender)
        intent.putExtra("petAge", Util.getAgeFromBirth(currentItem.birth))
        intent.putExtra("petMessage", currentItem.message)
        intent.putExtra("yearOnly", currentItem.yearOnly)
        intent.putExtra("activityType", PetProfileActivity.ActivityType.PET_PROFILE.ordinal)
        val isRepresentativePet = currentItem.id == SessionManager.fetchLoggedInAccount(requireContext())?.representativePetId?: 0
        intent.putExtra("isRepresentativePet", isRepresentativePet)

        return intent
    }

    private fun savePetPhotoByteArrayToSharedPreferences(holder: PetManagerAdapter.HistoryListViewHolder, currentItem: Pet) {
        if(currentItem.photoUrl != null) {
            val photoByteArray = Util.getByteArrayFromDrawable(holder.petPhoto.drawable)
            Util.putByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
                requireContext().getString(R.string.data_name_my_pet_selected_pet_photo), photoByteArray)
        }
        else {
            Util.putByteArrayToSharedPreferences(requireContext(), requireContext().getString(R.string.pref_name_byte_arrays),
                requireContext().getString(R.string.data_name_my_pet_selected_pet_photo), null)
        }
    }

    private fun setEmptyNotificationView(itemCount: Int) {
        val visibility = if(itemCount != 1) View.GONE else View.VISIBLE
        binding.textviewEmptypet.visibility = visibility
    }


    /** fetchPet() */
    private fun fetchPet() {
        CustomProgressBar.addProgressBar(requireContext(), binding.fragmentPetManagerParentLayout, 80, R.color.white)
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPetReq(FetchPetReqDto( null , null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            val fetchedPetList: ArrayList<Pet> = ArrayList()
            response.body()?.petList?.map {
                fetchedPetList.add(it)
                myPetViewModel.setPetNameForId(it.id, it.name)
            }

            synchronizeAlarmManager()

            val orderedPetIdList = updateOrderedPetIdList(fetchedPetList)
            val petList = getPetListByOrderedPetIdList(orderedPetIdList, fetchedPetList)

            adapter.setResult(petList)
            adapter.notifyDataSetChanged()

            binding.recyclerviewPet.scrollToPosition(myPetViewModel.lastScrolledIndex)

            CustomProgressBar.removeProgressBar(binding.fragmentPetManagerParentLayout)
        }, { CustomProgressBar.removeProgressBar(binding.fragmentPetManagerParentLayout) },
            { CustomProgressBar.removeProgressBar(binding.fragmentPetManagerParentLayout) }
        )
    }

    private fun synchronizeAlarmManager() {
        cancelAll(requireContext())

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPetScheduleReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            response.body()?.petScheduleList?.map{
                // 알람 ON?
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

    private fun updateOrderedPetIdList(fetchedPetList: ArrayList<Pet>): ArrayList<Long> {
        val orderedPetIdList = getOrderedPetIdList(requireContext()
            .getString(R.string.data_name_pet_list_id_order), requireContext())

        // 사라진 펫 제거 ... fetchedPetList: 2,3,4 / orderedPetIdList: 2,4,(5) -> 2,4
        // 리스트를 제거하는 동작을 수행하기 때문에 반복문을 거꾸로 진행한다.
        for(i in orderedPetIdList.count()-1 downTo 0){
            if(fetchedPetList.find{ it.id == orderedPetIdList[i] } == null){
                orderedPetIdList.remove(orderedPetIdList[i])
            }
        }

        // 새로 생긴 펫 추가 ... fetchedPetList: 2,(3),4 / orderedPetIdList: 2,4 -> 2,4,3
        for(pet in fetchedPetList){
            if(orderedPetIdList.find{ it == pet.id } == null) orderedPetIdList.add(pet.id)
        }

        putOrderedPetIdList(requireContext().getString(R.string
            .data_name_pet_list_id_order), orderedPetIdList, requireContext())

        return orderedPetIdList
    }

    fun getOrderedPetIdList(prefKey: String?, context: Context): ArrayList<Long> {
        val preferences: SharedPreferences = context.getSharedPreferences(
            context.getString(R.string.pref_name_pet_list_id_order), Context.MODE_PRIVATE)
        val json: String? = preferences.getString(prefKey, null)
        val type: Type = object : TypeToken<ArrayList<Long>>() {}.type

        if(json == null) { return arrayListOf() }
        return Gson().fromJson(json, type)
    }

    fun putOrderedPetIdList(prefKey: String, orderedPetIdList: ArrayList<Long>, context: Context) {
        val preferences: SharedPreferences = context.getSharedPreferences(
            context.getString(R.string.pref_name_pet_list_id_order), Context.MODE_PRIVATE)
        val json: String = Gson().toJson(orderedPetIdList)

        preferences.edit().putString(prefKey, json).apply()
    }

    private fun getPetListByOrderedPetIdList(orderedPetIdList: ArrayList<Long>, fetchedPetList: ArrayList<Pet>): ArrayList<Pet> {
        val petList = arrayListOf<Pet>()
        for(id in orderedPetIdList) {
            val pet = fetchedPetList.find { it.id == id }
            petList.add(pet!!)
        }

        return petList
    }


    override fun onResume() {
        super.onResume()
        // 대표펫 아이콘 업데이트를 위함임
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }


    /** OnStartDragListener */
    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        touchHelper.startDrag(viewHolder)
    }
}