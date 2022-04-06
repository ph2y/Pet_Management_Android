package com.sju18001.petmanagement.ui.community.post.generalFile

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.ActivityGeneralfileBinding
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.global.FileMetaData

class GeneralFileActivity : AppCompatActivity() {

    // const
    private var GENERAL_FILE_ACTIVITY_DIRECTORY: String = "general_file_activity"

    // variable for view binding
    private lateinit var binding: ActivityGeneralfileBinding

    // variable for ViewModel
    private val generalFileViewModel: GeneralFileViewModel by lazy {
        ViewModelProvider(this, SavedStateViewModelFactory(application, this)).get(GeneralFileViewModel::class.java)
    }

    // variables for RecyclerView
    private lateinit var generalFileAdapter: GeneralFileAdapter
    private var generalFileList: MutableList<GeneralFileListItem> = mutableListOf()

    private var isViewDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // no title bar
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        // view binding
        binding = ActivityGeneralfileBinding.inflate(layoutInflater)
        isViewDestroyed = false

        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()

        // initialize RecyclerView
        generalFileAdapter = GeneralFileAdapter(this, generalFileViewModel, GENERAL_FILE_ACTIVITY_DIRECTORY)
        binding.recyclerviewGeneralfile.adapter = generalFileAdapter
        binding.recyclerviewGeneralfile.layoutManager = LinearLayoutManager(this)

        // set RecyclerView values
        generalFileList = mutableListOf()
        val postId = this.intent.getLongExtra("postId", -1)
        val postGeneralFile = Gson().fromJson(this.intent.getStringExtra("fileAttachments"), Array<FileMetaData>::class.java)
        for (i in postGeneralFile.indices) {
            generalFileList.add(GeneralFileListItem(postId, postGeneralFile[i].name.split("post_${postId}_").last(), i))
        }
        generalFileAdapter.setResult(generalFileList)

        binding.imagebuttonGeneralfileClose.setOnClickListener { finish() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ServerUtil.WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                // save Uri + write
                generalFileViewModel.userSelectedUri = uri
                ServerUtil.writeFileToUri(this,
                    generalFileViewModel.downloadedFilePath!!, generalFileViewModel.userSelectedUri!!)

                // reset download button
                generalFileAdapter.setResult(generalFileList)

                Toast.makeText(this, this.getText(R.string.download_complete_message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        generalFileAdapter.onDestroy()
        isViewDestroyed = true

        if(isFinishing) {
            Util.deleteCopiedFiles(this, GENERAL_FILE_ACTIVITY_DIRECTORY)
        }
    }
}