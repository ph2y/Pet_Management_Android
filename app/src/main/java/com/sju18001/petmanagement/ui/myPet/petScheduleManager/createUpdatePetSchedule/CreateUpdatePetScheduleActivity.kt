package com.sju18001.petmanagement.ui.myPet.petScheduleManager.createUpdatePetSchedule

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityCreateupdatepetscheduleBinding

class CreateUpdatePetScheduleActivity : AppCompatActivity() {
    enum class FragmentType {
        CREATE_PET_SCHEDULE, UPDATE_PET_SCHEDULE
    }

    // variable for view binding
    private lateinit var binding: ActivityCreateupdatepetscheduleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // view binding
        binding = ActivityCreateupdatepetscheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        // get fragment type and show it(for first launch)
        if(supportFragmentManager.findFragmentById(R.id.framelayout_createupdatepetschedule_fragmentcontainer) == null) {
            val fragment = CreateUpdatePetScheduleFragment()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.framelayout_createupdatepetschedule_fragmentcontainer, fragment)
                .commit()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }
}