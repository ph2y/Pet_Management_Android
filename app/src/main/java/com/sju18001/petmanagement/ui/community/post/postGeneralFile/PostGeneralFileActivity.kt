package com.sju18001.petmanagement.ui.community.post.postGeneralFile

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.ActivityPostgeneralfileBinding
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.global.FileMetaData

class PostGeneralFileActivity : AppCompatActivity() {
    companion object{
        private const val GENERAL_FILE_ACTIVITY_DIRECTORY: String = "general_file_activity"
    }

    private lateinit var binding: ActivityPostgeneralfileBinding

    private val viewModel: PostGeneralFileViewModel by lazy {
        ViewModelProvider(this, SavedStateViewModelFactory(application, this)).get(PostGeneralFileViewModel::class.java)
    }
    private lateinit var adapter: PostGeneralFileAdapter

    private var isViewDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE) // No title bar
        setBinding()

        isViewDestroyed = false

        initializeAdapter()
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_postgeneralfile)

        binding.lifecycleOwner = this
        binding.activity = this@PostGeneralFileActivity
    }

    private fun initializeAdapter() {
        adapter = PostGeneralFileAdapter(this, viewModel, GENERAL_FILE_ACTIVITY_DIRECTORY)
        binding.recyclerviewPostgeneralfile.adapter = adapter
        binding.recyclerviewPostgeneralfile.layoutManager = LinearLayoutManager(this)

        initializeAdapterDataByFileAttachments()
    }

    private fun initializeAdapterDataByFileAttachments() {
        val postId = this.intent.getLongExtra("postId", -1)
        val postGeneralFile = Gson().fromJson(this.intent.getStringExtra("fileAttachments"), Array<FileMetaData>::class.java)

        var postGeneralFileList: MutableList<PostGeneralFile> = mutableListOf()
        for (i in postGeneralFile.indices) {
            val name = postGeneralFile[i].name.split("post_${postId}_").last()
            postGeneralFileList.add(PostGeneralFile(postId, name, i))
        }
        adapter.setResult(postGeneralFileList)
    }


    // 다운로드 버튼을 눌러 ACTION_CREATE_DOCUMENT을 수행한 이후
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ServerUtil.WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                // 이전에 Adapter에서 저장했던 복사본의 경로를 참조하여 파일을 생성합니다.
                viewModel.copyFilePath?.let { ServerUtil.writeFileToUri(this, it, uri) }
                Toast.makeText(baseContext, getText(R.string.download_complete_message), Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        adapter.onDestroy()
        isViewDestroyed = true

        if(isFinishing) Util.deleteCopiedFiles(this, GENERAL_FILE_ACTIVITY_DIRECTORY)
    }


    fun onClickCloseButton() {
        finish()
    }
}