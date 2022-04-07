package com.sju18001.petmanagement.ui.myPet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityPetprofileBinding
import com.sju18001.petmanagement.ui.myPet.petScheduleManager.createUpdatePetSchedule.CreateUpdatePetScheduleFragment
import com.sju18001.petmanagement.ui.myPet.petManager.petProfile.CreateUpdatePetFragment
import com.sju18001.petmanagement.ui.myPet.petManager.petProfile.PetProfileFragment

class PetProfileActivity : AppCompatActivity() {
    enum class FragmentType {
        CREATE_PET, PET_PROFILE_FROM_MY_PET, PET_PROFILE_FROM_COMMUNITY
    }

    // variable for view binding
    private lateinit var binding: ActivityPetprofileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // view binding
        binding = ActivityPetprofileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        // get fragment type and show it(for first launch)
        val fragmentType = intent.getIntExtra("fragmentType", 0)

        if(supportFragmentManager.findFragmentById(R.id.framelayout_petprofile_fragmentcontainer) == null) {
            val fragment = when(fragmentType){
                FragmentType.CREATE_PET.ordinal -> CreateUpdatePetFragment()
                FragmentType.PET_PROFILE_FROM_MY_PET.ordinal -> PetProfileFragment()
                else -> PetProfileFragment()
            }
            supportFragmentManager
                .beginTransaction()
                .add(R.id.framelayout_petprofile_fragmentcontainer, fragment)
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