package com.sju18001.petmanagement.ui.myPet.petScheduleManager.createUpdatePetSchedule

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.ActivityCreateupdatepetscheduleBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.ui.myPet.petScheduleManager.PetScheduleNotification
import java.time.LocalTime

class CreateUpdatePetScheduleActivity : AppCompatActivity() {
    enum class ActivityType {
        CREATE_PET_SCHEDULE, UPDATE_PET_SCHEDULE
    }

    private lateinit var binding: ActivityCreateupdatepetscheduleBinding

    private var petNameForId = HashMap<Long, String>()
    private lateinit var adapter: PetNameAdapter

    private val viewModel: CreateUpdatePetScheduleViewModel by viewModels()
    private var isViewDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBinding()
        isViewDestroyed = false

        supportActionBar?.hide()

        initializeViewModelByIntent()
        setObserversOfLiveData()

        initializeAdapter()
        Util.setupViewsForHideKeyboard(this, binding.constraintlayoutParent, listOf(binding.buttonConfirm))
        binding.adView.loadAd(AdRequest.Builder().build())
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_createupdatepetschedule)

        binding.lifecycleOwner = this
        binding.activity = this@CreateUpdatePetScheduleActivity
        binding.viewModel = viewModel
    }

    private fun initializeViewModelByIntent(){
        viewModel.activityType.value = intent.getIntExtra("activityType", 0)

        if(viewModel.isActivityTypeUpdatePetSchedule()){
            val localTime = LocalTime.parse(intent.getStringExtra("time"))
            viewModel.hour.value = localTime.hour
            viewModel.minute.value = localTime.minute
            viewModel.memo.value = intent.getStringExtra("memo")
        }
    }

    private fun setObserversOfLiveData() {
        viewModel.isApiLoading.observe(this, { isApiLoading ->
            val recyclerView = binding.recyclerviewPetname
            if(isApiLoading){
                for(i in 0..adapter.itemCount) {
                    val checkBox = recyclerView.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<CheckBox>(R.id.pet_name_check_box)
                    checkBox?.isEnabled = false
                    checkBox?.buttonTintList = resources?.getColorStateList(R.color.gray)
                }
            }else{
                for(i in 0..adapter.itemCount) {
                    val checkBox = recyclerView.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<CheckBox>(R.id.pet_name_check_box)
                    checkBox?.isEnabled = true
                    checkBox?.buttonTintList = resources?.getColorStateList(R.color.carrot)
                }
            }
        })
    }

    private fun initializeAdapter(){
        adapter = PetNameAdapter(arrayListOf(), object : PetNameListAdapterInterface {
            override fun updateCheckBoxByViewModel(checkBox: CheckBox, position: Int) {
                viewModel.isPetChecked.value?.let {
                    if (position <= it.size - 1) checkBox.isChecked = it[position]
                }
            }

            override fun onClickCheckBox(position: Int) {
                viewModel.isPetChecked.value?.let {
                    if (it.size >= position + 1) {
                        it[position] = !it[position]

                        if(it[position]) viewModel.checkedPetCount.value = viewModel.checkedPetCount.value!! + 1
                        else viewModel.checkedPetCount.value = viewModel.checkedPetCount.value!! - 1
                    }
                }
            }
        })

        binding.recyclerviewPetname.adapter = adapter
        binding.recyclerviewPetname.layoutManager = LinearLayoutManager(baseContext)

        initializeDataByFetchingPet()
    }

    /**
     * FetchPet을 수행하여 얻은 정보를 바탕으로, recycler view에 item을 추가하고 petNameForId,
     * isPetChecked, checkedPetCount를 초기화한다. 특히 체크 여부를 파악하기 위해서, intent로
     * 전달 받은 petIdList를 이용하게 된다.
     */
    private fun initializeDataByFetchingPet() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchPetReq(FetchPetReqDto( null , null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            var checkedPetIdList = listOf<String>()
            intent.getStringExtra("petIdList")?.let{
                checkedPetIdList = it.replace(" ", "").split(",")
            }

            response.body()?.petList?.map {
                adapter.addItem(PetNameItem(it.name, it.id))
                petNameForId[it.id] = it.name

                if(it.id.toString() in checkedPetIdList){
                    viewModel.isPetChecked.value!!.add(true)
                    viewModel.checkedPetCount.value = viewModel.checkedPetCount.value!! + 1
                }else{
                    viewModel.isPetChecked.value!!.add(false)
                }
            }
            adapter.notifyDataSetChanged()
        }, {}, {})
    }


    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }


    /** Databinding functions */
    fun onClickBackButton() {
        finish()
    }

    fun onClickConfirmButton() {
        if (viewModel.isActivityTypeCreatePetSchedule()){
            createPetSchedule()
        }else{
            val enabled = intent.getBooleanExtra("enabled", false)
            if (enabled) replacePetScheduleNotification()
            updatePetSchedule(intent.getLongExtra("id", 0), enabled)
        }
    }

    private fun createPetSchedule(){
        viewModel.isApiLoading.value = true
        val createPetScheduleReqDto = CreatePetScheduleReqDto(
            getCheckedPetIdList(), LocalTime.of(binding.timepicker.hour, binding.timepicker.minute).toString(), viewModel.memo.value
        )
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .createPetScheduleReq(createPetScheduleReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            finish()
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }

    private fun getCheckedPetIdList(): String{
        var checkedPetIdList = ""

        viewModel.isPetChecked.value?.let {
            for(i in it.indices){
                if(it[i]) checkedPetIdList += "${adapter.getItem(i).id},"
            }
            checkedPetIdList = checkedPetIdList.dropLast(1) // "1,2,3," -> "1,2,3"
        }

        return checkedPetIdList
    }

    // 기존에 등록되어 있던 알람을 제거하고 새로 등록한다.
    private fun replacePetScheduleNotification() {
        PetScheduleNotification.cancelAlarmManagerRepeating(
            baseContext,
            intent.getLongExtra("id", -1)
        )

        PetScheduleNotification.setAlarmManagerRepeating(
            baseContext,
            intent.getLongExtra("timepicker", -1),
            LocalTime.of(binding.timepicker.hour, binding.timepicker.minute)
                .toString() + ":00",
            Util.getPetNamesFromPetIdList(petNameForId, getCheckedPetIdList()),
            viewModel.memo.value
        )
    }

    private fun updatePetSchedule(id: Long, enabled: Boolean){
        val updatePetScheduleReqDto = UpdatePetScheduleReqDto(
            id, getCheckedPetIdList(), LocalTime.of(binding.timepicker.hour, binding.timepicker.minute).toString(), viewModel.memo.value, enabled
        )

        viewModel.isApiLoading.value = true
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .updatePetScheduleReq(updatePetScheduleReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            finish()
        }, { viewModel.isApiLoading.value = false }, { viewModel.isApiLoading.value = false })
    }
}