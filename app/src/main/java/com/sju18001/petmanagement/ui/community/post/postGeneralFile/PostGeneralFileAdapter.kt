package com.sju18001.petmanagement.ui.community.post.postGeneralFile

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.databinding.ItemPostgeneralfileBinding
import com.sju18001.petmanagement.restapi.dto.FetchPostFileReqDto

class PostGeneralFileAdapter(
    private val activity: Activity,
    private val generalFileViewModel: PostGeneralFileViewModel,
    private val GENERAL_FILE_ACTIVITY_DIRECTORY: String
    ): RecyclerView.Adapter<PostGeneralFileAdapter.ViewHolder>() {
    private var dataSet = mutableListOf<PostGeneralFile>()
    private var isViewDestroyed = false

    class ViewHolder(
        private val adapter: PostGeneralFileAdapter,
        private val binding: ItemPostgeneralfileBinding
    ): RecyclerView.ViewHolder(binding.root) {
        val nameTextView = binding.textviewName
        val downloadButton = binding.imagebuttonDownload
        val downloadProgressBar = binding.progressbarDownload

        fun bind(postGeneralFile: PostGeneralFile) {
            binding.adapter = adapter
            binding.holder = this
            binding.postGeneralFile = postGeneralFile

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ItemPostgeneralfileBinding>(LayoutInflater.from(parent.context),
            R.layout.item_postgeneralfile, parent, false)
        return ViewHolder(this, binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount() = dataSet.size

    fun setResult(result: MutableList<PostGeneralFile>){
        this.dataSet = result
        notifyDataSetChanged()
    }

    fun onDestroy() {
        isViewDestroyed = true
    }


    /** Databinding functions */
    fun onClickPostGeneralFileName(holder: ViewHolder, postGeneralFile: PostGeneralFile) {
        fetchGeneralFile(holder, postGeneralFile, true)
    }

    fun onClickDownloadButton(holder: ViewHolder, postGeneralFile: PostGeneralFile) {
        fetchGeneralFile(holder, postGeneralFile, false)
    }

    private fun fetchGeneralFile(holder: ViewHolder, postGeneralFile: PostGeneralFile, isExecutionOnly: Boolean) {
        val postId = postGeneralFile.postId
        val fileId = postGeneralFile.fileId
        val fileName = postGeneralFile.name

        lockViews(holder)
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(activity)!!)
            .fetchPostFileReq(FetchPostFileReqDto(postId, fileId))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, activity, { response ->
            val extension = fileName.split('.').last()

            if(isExecutionOnly){
                startActionView(extension, response.body()!!.byteStream().readBytes())
            }else{
                // 이후 ACTION_CREATE_DOCUMENT를 통해 사용자가 파일의 경로를 지정하게 되면,
                // 액티비티에서, 그 경로에 복사본 파일을 저장하게 됩니다.
                generalFileViewModel.copyFilePath =
                    ServerUtil.createCopyAndGetAbsolutePath(
                        activity, response.body()!!.byteStream().readBytes(),
                        extension, GENERAL_FILE_ACTIVITY_DIRECTORY
                    )
                startActionCreateDocument(activity, fileName)
            }

            unlockViews(holder)
        }, { unlockViews(holder) }, { unlockViews(holder) })
    }

    private fun startActionView(extension: String, byteArray: ByteArray) {
        val mimeTypeMap = MimeTypeMap.getSingleton()
        val mimeType = mimeTypeMap.getMimeTypeFromExtension(extension)
        val contentUri = ServerUtil.createCopyAndReturnContentUri(activity, byteArray, extension, GENERAL_FILE_ACTIVITY_DIRECTORY)

        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(contentUri, mimeType)

        try{
            activity.startActivity(intent)
        } catch (e:Exception){
            // 파일 타입을 지원하지 않는 경우(ex: .hwp)
            Toast.makeText(activity, activity.getText(R.string.general_file_type_exception_for_start_message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun startActionCreateDocument(activity: Activity, fileName: String) {
        val mimeTypeMap = MimeTypeMap.getSingleton()
        val mimeType = mimeTypeMap.getMimeTypeFromExtension(fileName.split('.').last())

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        activity.startActivityForResult(intent, ServerUtil.WRITE_REQUEST_CODE)
    }

    private fun lockViews(holder: ViewHolder) {
        holder.nameTextView.isClickable = false
        holder.downloadButton.visibility = View.INVISIBLE
        holder.downloadProgressBar.visibility = View.VISIBLE
    }

    private fun unlockViews(holder: ViewHolder) {
        holder.nameTextView.isClickable = true
        holder.downloadButton.visibility = View.VISIBLE
        holder.downloadProgressBar.visibility = View.INVISIBLE
    }
}