package com.sju18001.petmanagement.ui.myPet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityMyPetBinding
import com.sju18001.petmanagement.ui.myPet.petScheduleManager.CreateUpdatePetScheduleFragment
import com.sju18001.petmanagement.ui.myPet.petManager.CreateUpdatePetFragment
import com.sju18001.petmanagement.ui.myPet.petManager.PetProfileFragment

object MyPetActivityFragmentTypes {
    const val CREATE_PET = "create_pet"
    const val PET_PROFILE_PET_MANAGER = "pet_profile_pet_manager"
    const val PET_PROFILE_COMMUNITY = "pet_profile_community"
    const val CREATE_PET_SCHEDULE = "create_pet_schedule"
    const val UPDATE_PET_SCHEDULE = "update_pet_schedule"
}

class MyPetActivity : AppCompatActivity() {

    // variable for view binding
    private lateinit var binding: ActivityMyPetBinding

    // variable for ViewModel
    private val myPetViewModel: MyPetViewModel by lazy{
        ViewModelProvider(this, SavedStateViewModelFactory(application, this)).get(MyPetViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // view binding
        binding = ActivityMyPetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        // get fragment type and show it(for first launch)
        val fragmentType = intent.getStringExtra("fragmentType")

        if(supportFragmentManager.findFragmentById(R.id.my_pet_activity_fragment_container) == null) {
            val fragment = when(fragmentType){
                MyPetActivityFragmentTypes.CREATE_PET -> CreateUpdatePetFragment()
                MyPetActivityFragmentTypes.PET_PROFILE_PET_MANAGER -> PetProfileFragment()
                MyPetActivityFragmentTypes.PET_PROFILE_COMMUNITY -> PetProfileFragment()
                else -> CreateUpdatePetScheduleFragment()
            }
            supportFragmentManager
                .beginTransaction()
                .add(R.id.my_pet_activity_fragment_container, fragment)
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