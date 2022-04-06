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
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dto.FetchPostFileReqDto

class PostGeneralFileAdapter(private val activity: Activity, private val generalFileViewModel: PostGeneralFileViewModel,
                             private val GENERAL_FILE_ACTIVITY_DIRECTORY: String):
    RecyclerView.Adapter<PostGeneralFileAdapter.HistoryListViewHolder>() {

    private var resultList = mutableListOf<PostGeneralFileItem>()

    private var isViewDestroyed = false

    class HistoryListViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textview_postgeneralfile_name)
        val downloadButton: ImageButton = view.findViewById(R.id.imagebutton_postgeneralfile_download)
        val downloadProgressBar: ProgressBar = view.findViewById(R.id.progressbar_postgeneralfile_download)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_postgeneralfile, parent, false)

        val holder = HistoryListViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    override fun onBindViewHolder(holder: HistoryListViewHolder, position: Int) {
        holder.name.text = resultList[position].name
    }

    private fun setListenerOnView(holder: HistoryListViewHolder) {
        holder.name.setOnClickListener {
            val position = holder.absoluteAdapterPosition

            // set button to downloading
            holder.name.isClickable = false
            holder.downloadButton.visibility = View.INVISIBLE
            holder.downloadProgressBar.visibility = View.VISIBLE

            fetchGeneralFile(resultList[position].postId, resultList[position].fileId, resultList[position].name, true) {
                holder.name.isClickable = true
                holder.downloadButton.visibility = View.VISIBLE
                holder.downloadProgressBar.visibility = View.INVISIBLE
            }
        }

        holder.downloadButton.setOnClickListener {
            val position = holder.absoluteAdapterPosition

            // set button to downloading
            holder.downloadButton.visibility = View.INVISIBLE
            holder.downloadProgressBar.visibility = View.VISIBLE

            // fetch file + write
            fetchGeneralFile(resultList[position].postId, resultList[position].fileId, resultList[position].name, false) {
                holder.downloadButton.visibility = View.VISIBLE
                holder.downloadProgressBar.visibility = View.INVISIBLE
            }
        }
    }

    private fun fetchGeneralFile(postId: Long, fileId: Int, fileName: String, isExecutionOnly: Boolean, callback:()->Unit) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(activity)!!)
            .fetchPostFileReq(FetchPostFileReqDto(postId, fileId))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, activity, { response ->
            val extension = fileName.split('.').last()

            if(isExecutionOnly){
                executeByteArrayAsFile(extension, response.body()!!.byteStream().readBytes())
            }else{
                // download file + save downloaded path -> write file
                generalFileViewModel.downloadedFilePath = ServerUtil
                    .createCopyAndReturnRealPathServer(activity, response.body()!!.byteStream().readBytes(),
                        extension, GENERAL_FILE_ACTIVITY_DIRECTORY)

                // get Uri from user + write file
                ServerUtil.getUriFromUser(activity, fileName)
            }

            callback.invoke()
        }, {callback.invoke()}, {callback.invoke()})
    }

    private fun executeByteArrayAsFile(extension: String, byteArray: ByteArray) {
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
            // 파일 타입을 지원하지 않는 경우(ex. hwp)
            Toast.makeText(activity, activity.getText(R.string.general_file_type_exception_for_start_message), Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = resultList.size

    public fun setResult(result: MutableList<PostGeneralFileItem>){
        this.resultList = result
        notifyDataSetChanged()
    }

    public fun onDestroy() {
        isViewDestroyed = true
    }
}