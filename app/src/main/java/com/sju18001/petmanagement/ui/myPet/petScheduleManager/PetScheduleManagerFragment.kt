package com.sju18001.petmanagement.ui.myPet.petScheduleManager

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.CustomProgressBar
import com.sju18001.petmanagement.databinding.FragmentPetschedulemanagerBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dao.PetSchedule
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.ui.myPet.MyPetViewModel
import com.sju18001.petmanagement.ui.myPet.petScheduleManager.createUpdatePetSchedule.CreateUpdatePetScheduleActivity

class PetScheduleManagerFragment : Fragment() {
    private var _binding: FragmentPetschedulemanagerBinding? = null
    private val binding get() = _binding!!

    private val myPetViewModel: MyPetViewModel by activityViewModels()
    private lateinit var adapter: PetScheduleAdapter

    private var isViewDestroyed = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        isViewDestroyed = false

        initializeAdapter()

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = FragmentPetschedulemanagerBinding.inflate(inflater, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@PetScheduleManagerFragment
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.adView.loadAd(AdRequest.Builder().build())
    }

    override fun onResume() {
        super.onResume()
        updateAdapterDataSetByFetchPetSchedule()
    }

    override fun onDestroy() {
        super.onDestroy()

        isViewDestroyed = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initializeAdapter(){
        adapter = PetScheduleAdapter(arrayListOf(), { myPetViewModel.petNameForId }, object: PetScheduleAdapterInterface {
            override fun startCreateUpdatePetScheduleFragmentForUpdate(data: PetSchedule) {
                val createUpdatePetScheduleActivityIntent = Intent(context, CreateUpdatePetScheduleActivity::class.java)
                createUpdatePetScheduleActivityIntent
                    .putExtra("activityType", CreateUpdatePetScheduleActivity.ActivityType.UPDATE_PET_SCHEDULE.ordinal)
                    .putExtra("id", data.id)
                    .putExtra("petIdList", data.petIdList)
                    .putExtra("time", data.time)
                    .putExtra("memo", data.memo)
                    .putExtra("enabled", data.enabled)
                startActivity(createUpdatePetScheduleActivityIntent)
                requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
            }

            override fun askForDeleteItem(position: Int, item: PetSchedule) {
                val builder = AlertDialog.Builder(activity)
                builder.setMessage("일정을 삭제하시겠습니까?")
                    .setPositiveButton(
                        R.string.confirm, DialogInterface.OnClickListener { _, _ ->
                            PetScheduleNotification.cancelAlarmManagerRepeating(
                                requireContext(),
                                item.id
                            )
                            deletePetSchedule(item.id)

                            adapter.removeItem(position)
                            adapter.notifyItemRemoved(position)
                            adapter.notifyItemRangeChanged(position, adapter.itemCount)
                        }
                    )
                    .setNegativeButton(
                        R.string.cancel, DialogInterface.OnClickListener { dialog, _ ->
                            dialog.cancel()
                        }
                    )
                    .create()
                    .show()
            }

            override fun deletePetSchedule(id: Long) {
                val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                    .deletePetScheduleReq(DeletePetScheduleReqDto(id))
                ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {}, {}, {})
            }

            override fun updatePetSchedule(data: PetSchedule){
                val updatePetScheduleReqDto = UpdatePetScheduleReqDto(
                    data.id, data.petIdList, data.time, data.memo, data.enabled
                )
                val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                    .updatePetScheduleReq(updatePetScheduleReqDto)
                ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {}, {}, {})
            }

            override fun getContext(): Context {
                return requireContext()
            }
        })

        binding.recyclerviewPetschedule.adapter = adapter
        binding.recyclerviewPetschedule.layoutManager = LinearLayoutManager(activity)

        adapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                setEmptyPetTextView(adapter.itemCount)
            }
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                setEmptyPetTextView(adapter.itemCount)
            }
        })
    }

    private fun updateAdapterDataSetByFetchPetSchedule(){
        CustomProgressBar.addProgressBar(requireContext(), binding.fragmentPetScheduleManagerParentLayout, 80, R.color.white)
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPetScheduleReq(ServerUtil.getEmptyBody())
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            val dataSet = arrayListOf<PetSchedule>()
            response.body()?.petScheduleList?.map{
                dataSet.add(PetSchedule(
                    it.id, it.username, it.petList, it.time, it.memo, it.enabled,
                    it.petIdList.replace(" ", "")
                ))
            }
            dataSet.sortBy{ it.time }

            adapter.setDataSet(dataSet)
            adapter.notifyDataSetChanged()

            CustomProgressBar.removeProgressBar(binding.fragmentPetScheduleManagerParentLayout)
        },{ CustomProgressBar.removeProgressBar(binding.fragmentPetScheduleManagerParentLayout) },
            { CustomProgressBar.removeProgressBar(binding.fragmentPetScheduleManagerParentLayout) }
        )
    }

    private fun setEmptyPetTextView(itemCount: Int) {
        val visibility = if(itemCount != 0) View.GONE else View.VISIBLE
        binding.textviewEmptypetschedule.visibility = visibility
    }


    /** databinding functions */
    fun onClickCreatePetScheduleButton() {
        checkIfAccountHasPetAndStartCreatePetSchedule()
    }

    private fun checkIfAccountHasPetAndStartCreatePetSchedule() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPetReq(FetchPetReqDto(null, null))
        ServerUtil.enqueueApiCall(call, { isViewDestroyed }, requireContext(), { response ->
            var petCount = 0
            response.body()?.petList?.map {
                petCount++
            }

            if (petCount > 0) {
                startCreatePetSchedule()
            } else {
                Toast.makeText(context, getString(R.string.pet_list_empty_for_schedule_exception_message), Toast.LENGTH_LONG).show()
            }
        }, {}, {})
    }

    private fun startCreatePetSchedule(){
        val createUpdatePetScheduleActivityIntent = Intent(context, CreateUpdatePetScheduleActivity::class.java)
        createUpdatePetScheduleActivityIntent.putExtra("activityType", CreateUpdatePetScheduleActivity.ActivityType.CREATE_PET_SCHEDULE.ordinal)

        startActivity(createUpdatePetScheduleActivityIntent)
        requireActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }
}