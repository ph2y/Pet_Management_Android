package com.sju18001.petmanagement.ui.myPet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class MyPetViewModel(private val handle: SavedStateHandle) : ViewModel() {
    // variables for pet manager
    var lastScrolledIndex = handle.get<Int>("lastScrolledIndex")?: 0
        set(value){
            handle.set("lastScrolledIndex", value)
            field = value
        }
    var fragmentType = handle.get<Int>("fragmentType")
        set(value){
            handle.set("fragmentType", value)
            field = value
        }

    // variables for pet id - name
    var petNameForId = handle.get<HashMap<Long, String>>("petNameForId")?: HashMap()
        set(value){
            handle.set("petNameForId", value)
            field = value
        }

    fun setPetNameForId(id: Long, name: String){
        petNameForId[id] = name
    }

    // variables for schedule manager
    var time = handle.get<String>("time")?: 0
        set(value){
            handle.set("time", value)
            field = value
        }

    var memo = handle.get<String>("memo")?: ""
        set(value){
            handle.set("memo", value)
            field = value
        }
}