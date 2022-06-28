package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityCreateupdatepostBinding

class CreateUpdatePostActivity : AppCompatActivity() {

    // variable for view binding
    private lateinit var binding: ActivityCreateupdatepostBinding

    // variable for ViewModel
    private val createUpdatePostViewModel: CreateUpdatePostViewModel by lazy{
        ViewModelProvider(this, SavedStateViewModelFactory(application, this)).get(CreateUpdatePostViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // view binding
        binding = ActivityCreateupdatepostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // hide action bar
        supportActionBar?.hide()

        // show fragment
        if(supportFragmentManager.findFragmentById(R.id.framelayout_createupdatepost_fragmentcontainer) == null) {
            supportFragmentManager
                .beginTransaction()
                .add(R.id.framelayout_createupdatepost_fragmentcontainer, CreateUpdatePostFragment())
                .commit()
        }
    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(this.getString(R.string.cancel_dialog_message))
            .setPositiveButton(R.string.confirm) { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }
}