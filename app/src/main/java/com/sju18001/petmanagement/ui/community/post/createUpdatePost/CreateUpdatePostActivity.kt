package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.gson.Gson
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import androidx.activity.viewModels
import com.sju18001.petmanagement.controller.*
import com.sju18001.petmanagement.controller.Util.Companion.getExtensionFromUrl
import com.sju18001.petmanagement.databinding.ActivityCreateupdatepostBinding
import com.sju18001.petmanagement.restapi.dao.Pet
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.restapi.global.FileMetaData
import com.sju18001.petmanagement.restapi.global.FileType
import com.sju18001.petmanagement.ui.myPet.petManager.CustomLayoutManager
import com.sju18001.petmanagement.ui.myPet.petManager.PetManagerFragment
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class CreateUpdatePostActivity : AppCompatActivity() {
    companion object{
        private const val PICK_PHOTO = 0
        private const val PICK_VIDEO = 1
        private const val PICK_GENERAL_FILE = 2

        const val DISCLOSURE_PUBLIC: String = "PUBLIC"
        const val DISCLOSURE_PRIVATE: String = "PRIVATE"
        const val DISCLOSURE_FRIEND: String = "FRIEND"

        private const val CREATE_UPDATE_POST_DIRECTORY: String = "create_update_post"
    }

    private lateinit var binding: ActivityCreateupdatepostBinding

    private val viewModel: CreateUpdatePostViewModel by viewModels()
    private var isViewDestroyed = false

    private lateinit var petSelectorAdapter: CreateUpdatePostPetSelectorAdapter
    private lateinit var mediaAdapter: CreateUpdatePostMediaAdapter
    private lateinit var generalFileAdapter: CreateUpdatePostGeneralFileAdapter
    private lateinit var hashtagAdapter: CreateUpdatePostHashtagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBinding()
        isViewDestroyed = false

        initializeRecyclerViews()
        initializeViewModelByIntent()

        if(viewModel.isActivityTypeCreatePost()) fetchPostPetSelectorRecyclerView()
        else fetchPostAndPostPetSelectorRecyclerView()

        supportActionBar?.hide()
        binding.adView.loadAd(AdRequest.Builder().build())

        Util.setupViewsForHideKeyboard(
            this,
            binding.fragmentCreateUpdatePostParentLayout,
            listOf(binding.footerLayout)
        )
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_createupdatepost)

        binding.lifecycleOwner = this
        binding.activity = this@CreateUpdatePostActivity
        binding.viewModel = viewModel
    }

    private fun initializeRecyclerViews() {
        initializePetSelectorRecyclerView()
        initializeMediaRecyclerView()
        initializeGeneralFileRecyclerView()
        initializeHashtagRecyclerView()
    }

    private fun initializePetSelectorRecyclerView() {
        petSelectorAdapter = CreateUpdatePostPetSelectorAdapter(viewModel, baseContext)
        binding.recyclerviewCreateupdatepostPostpetselector.adapter = petSelectorAdapter
        binding.recyclerviewCreateupdatepostPostpetselector.layoutManager = LinearLayoutManager(this)
        (binding.recyclerviewCreateupdatepostPostpetselector.layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL
    }

    private fun initializeMediaRecyclerView() {
        mediaAdapter = CreateUpdatePostMediaAdapter(baseContext, viewModel)
        binding.mediaRecyclerView.adapter = mediaAdapter
        binding.mediaRecyclerView.layoutManager = LinearLayoutManager(this)
        (binding.mediaRecyclerView.layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL
    }

    private fun initializeGeneralFileRecyclerView() {
        generalFileAdapter = CreateUpdatePostGeneralFileAdapter(viewModel)
        binding.generalRecyclerView.adapter = generalFileAdapter
        binding.generalRecyclerView.layoutManager = LinearLayoutManager(this)
        (binding.generalRecyclerView.layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.VERTICAL
    }

    private fun initializeHashtagRecyclerView() {
        hashtagAdapter = CreateUpdatePostHashtagAdapter()
        binding.hashtagRecyclerView.adapter = hashtagAdapter
        binding.hashtagRecyclerView.layoutManager = FlexboxLayoutManager(this)
        (binding.hashtagRecyclerView.layoutManager as FlexboxLayoutManager).flexWrap = FlexWrap.WRAP
    }

    private fun initializeViewModelByIntent() {
        viewModel.activityType.value = intent.getStringExtra("activityType")
        viewModel.postId = intent.getLongExtra("postId", -1)
        viewModel.position = intent.getIntExtra("position", -1)
        viewModel.originalImageCount = intent.getIntExtra("originalImageCount", 0)
        viewModel.originalVideoCount = intent.getIntExtra("originalVideoCount", 0)
        viewModel.originalGeneralFileCount = intent.getIntExtra("originalGeneralFileCount", 0)
    }

    // FetchPost를 먼저 하여 선택된 펫의 정보를 알게 된 다음에,
    // FetchPostPetSelectorRecyclerView에서 이 정보를 이용하여 펫을 선택합니다.
    private fun fetchPostAndPostPetSelectorRecyclerView() {
        increaseApiCallCountForFetch()
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchPostReq(FetchPostReqDto(null, null, null, viewModel.postId, null, null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            val post = response.body()?.postList!![0]

            viewModel.selectedPetId.value = post.pet.id
            viewModel.isUsingLocation.value = post.geoTagLat != 0.0
            viewModel.disclosure.value = post.disclosure
            viewModel.contents.value = post.contents

            if(post.serializedHashTags != "") {
                hashtagAdapter.setDataSet(post.serializedHashTags.split(',').toMutableList())
            }

            if(post.imageAttachments != null){
                val postImage = Gson().fromJson(post.imageAttachments, Array<FileMetaData>::class.java)
                fetchPostImageData(postImage)
            }

            if(post.videoAttachments != null){
                val postVideo = Gson().fromJson(post.videoAttachments, Array<FileMetaData>::class.java)
                fetchPostVideoData(postVideo)
            }

            val postGeneral = Gson().fromJson(post.fileAttachments, Array<FileMetaData>::class.java)
            if(postGeneral != null) fetchPostGeneralData(post.id, postGeneral)

            fetchPostPetSelectorRecyclerView()

            decreaseApiCallCountForFetch()
        }, { finish() }, { finish() })
    }

    private fun fetchPostImageData(postImage: Array<FileMetaData>) {
        for(index in postImage.indices) {
            increaseApiCallCountForFetch()
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .fetchPostImageReq(FetchPostImageReqDto(viewModel.postId!!, index, FileType.ORIGINAL_IMAGE))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
                val extension = getExtensionFromUrl(postImage[index].url)
                val imageByteArray = response.body()!!.byteStream().readBytes()
                val photoPath =
                    ServerUtil.createCopyAndGetAbsolutePath(baseContext, imageByteArray, extension, CREATE_UPDATE_POST_DIRECTORY)
                viewModel.addPhotoPath(photoPath)
                mediaAdapter.addItem(CreateUpdatePostMedia(false, index, photoPath))

                decreaseApiCallCountForFetch()
            }, { decreaseApiCallCountForFetch() }, { decreaseApiCallCountForFetch() })
        }
    }

    private fun fetchPostVideoData(postVideo: Array<FileMetaData>) {
        for(index in postVideo.indices) {
            increaseApiCallCountForFetch()

            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .fetchPostVideoReq(postVideo[index].url)
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
                val extension = getExtensionFromUrl(postVideo[index].url)
                val videoByteArray = response.body()!!.byteStream().readBytes()
                val videoPath = ServerUtil.createCopyAndGetAbsolutePath(baseContext, videoByteArray, extension, CREATE_UPDATE_POST_DIRECTORY)
                viewModel.addVideoPath(videoPath)
                mediaAdapter.addItem(CreateUpdatePostMedia(true, index, videoPath))

                decreaseApiCallCountForFetch()
            }, { decreaseApiCallCountForFetch() }, { decreaseApiCallCountForFetch() })
        }
    }

    private fun fetchPostGeneralData(postId: Long, postGeneral: Array<FileMetaData>) {
        for(index in postGeneral.indices) {
            increaseApiCallCountForFetch()
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .fetchPostFileReq(FetchPostFileReqDto(viewModel.postId!!, index))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
                val extension = getExtensionFromUrl(postGeneral[index].url)
                val generalFileByteArray = response.body()!!.byteStream().readBytes()
                val generalFilePath =
                    ServerUtil.createCopyAndGetAbsolutePath(baseContext, generalFileByteArray, extension, CREATE_UPDATE_POST_DIRECTORY)
                val generalFileName = postGeneral[index].url.split("post_${postId}_").last()

                viewModel.addGeneralFileName(generalFileName)
                viewModel.addGeneralFilePath(generalFilePath)
                generalFileAdapter.addItem(generalFileName)

                decreaseApiCallCountForFetch()
            }, { decreaseApiCallCountForFetch() }, { decreaseApiCallCountForFetch() })
        }
    }

    private fun fetchPostPetSelectorRecyclerView() {
        increaseApiCallCountForFetch()
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchPetReq(FetchPetReqDto(null , null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            initializeSelectedPetIndexAndPetSelector(response.body()!!.petList)

            decreaseApiCallCountForFetch()
        }, { decreaseApiCallCountForFetch() }, { decreaseApiCallCountForFetch() })
    }

    private fun initializeSelectedPetIndexAndPetSelector(petList: List<Pet>) {
        val orderedPetIdList = PetManagerFragment()
            .getOrderedPetIdList(getString(R.string.data_name_pet_list_id_order), baseContext)
        for (i in orderedPetIdList.indices) {
            val id = orderedPetIdList[i]

            val currentPet = petList.find { it.id == id }
            val item = CreateUpdatePostPetSelectorItem(
                currentPet!!.id, currentPet!!.photoUrl, null, currentPet!!.name,
                currentPet!!.id == SessionManager.fetchLoggedInAccount(baseContext)?.representativePetId,
                currentPet!!.id == viewModel.selectedPetId.value
            )

            if (item.isSelected) viewModel.selectedPetIndex = i

            addPetSelector(item)
        }
    }

    private fun addPetSelector(item: CreateUpdatePostPetSelectorItem) {
        if (item.petPhotoUrl == null) {
            petSelectorAdapter.addItem(item)
        }else{
            increaseApiCallCountForFetch()
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .fetchPetPhotoReq(FetchPetPhotoReqDto(item.petId))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
                item.petPhoto = BitmapFactory.decodeStream(response.body()!!.byteStream())
                petSelectorAdapter.addItem(item)

                decreaseApiCallCountForFetch()
            }, { decreaseApiCallCountForFetch() }, { decreaseApiCallCountForFetch() })
        }
    }

    private fun increaseApiCallCountForFetch() {
        // fetch를 처음 시작할 때
        if(viewModel.apiCallCountForFetch == 0){
            CustomProgressBar.addProgressBar(baseContext, binding.fragmentCreateUpdatePostParentLayout, 80, R.color.white)
        }

        viewModel.apiCallCountForFetch += 1
    }

    private fun decreaseApiCallCountForFetch() {
        viewModel.apiCallCountForFetch -= 1

        // fetch가 모두 끝났을 때
        if(viewModel.apiCallCountForFetch == 0){
            petSelectorAdapter.notifyDataSetChanged()
            generalFileAdapter.notifyDataSetChanged()
            mediaAdapter.notifyDataSetChanged()

            CustomProgressBar.removeProgressBar(binding.fragmentCreateUpdatePostParentLayout)
        }
    }


    override fun onStart() {
        super.onStart()

        binding.hashtagInputEditText.setOnEditorActionListener{ _, _, _ ->
            addHashtag()
            true
        }

        viewModel.remainingApiCallCountForCreateUpdate.observe(this, {
            if(it == 0) finishAfterSuccess()
        })
    }

    private fun addHashtag(){
        if (viewModel.hashtagEditText.value!!.isEmpty()) return

        if(!PatternRegex.checkHashtagRegex(viewModel.hashtagEditText.value)) {
            Toast.makeText(baseContext, getText(R.string.hashtag_regex_exception_message), Toast.LENGTH_SHORT).show()
        }else if(hashtagAdapter.itemCount == 5){
            Toast.makeText(baseContext, getText(R.string.hashtag_usage_full_message), Toast.LENGTH_SHORT).show()
        }else{
            val hashtag = binding.hashtagInputEditText.text.toString()
            hashtagAdapter.addItem(hashtag)
            hashtagAdapter.notifyItemInserted(hashtagAdapter.itemCount)

            binding.hashtagRecyclerView.smoothScrollToPosition(hashtagAdapter.itemCount - 1)
            binding.hashtagInputEditText.setText("")
        }
    }

    private fun finishAfterSuccess() {
        if(isFinishing) Util.deleteCopiedFiles(baseContext, CREATE_UPDATE_POST_DIRECTORY)

        val message = if(viewModel.isActivityTypeCreatePost()) getText(R.string.create_post_successful)
        else getText(R.string.update_post_successful)
        Toast.makeText(baseContext, message, Toast.LENGTH_LONG).show()

        val resultIntent = Intent()
        resultIntent.putExtra("postId", viewModel.postId)
        resultIntent.putExtra("position", viewModel.position)
        setResult(Activity.RESULT_OK, resultIntent)

        finish()
    }


    // 사진, 동영상, 파일을 선택한 뒤에 호출된다.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode != RESULT_OK) { return }
        if(data == null){
            Toast.makeText(baseContext, getText(R.string.file_null_exception_message), Toast.LENGTH_LONG).show()
            return
        }

        when(requestCode) {
            PICK_PHOTO -> {
                // Exception: Exceeds size limit
                if (Util.isExceedsFileSizeLimit(baseContext, data!!, FileType.FILE_SIZE_LIMIT_PHOTO)) {
                    Toast.makeText(baseContext, getText(R.string.file_size_limit_exception_message_20MB), Toast.LENGTH_SHORT).show()
                    return
                }

                val fileName = Util.getSelectedFileName(baseContext, data.data!!)

                // Exception: Duplicated file
                for (item in mediaAdapter.getDataSet()) {
                    if (fileName == item.path.substring(item.path.lastIndexOf("/") + 1)) {
                        Toast.makeText(baseContext, baseContext.getText(R.string.duplicate_file_exception_message), Toast.LENGTH_SHORT).show()
                        return
                    }
                }

                val photoPath = ServerUtil.createCopyAndReturnRealPathLocal(this, data.data!!, CREATE_UPDATE_POST_DIRECTORY, fileName)

                // Exception: Not supported extension
                if (photoPath.isEmpty()) {
                    Toast.makeText(baseContext, getText(R.string.photo_file_type_exception_message), Toast.LENGTH_LONG).show()
                    File(photoPath).delete()
                    return
                }

                // Exception: Not a photo
                if (!Util.isUrlPhoto(photoPath)) {
                    Toast.makeText(baseContext, getText(R.string.photo_file_type_exception_message), Toast.LENGTH_LONG).show()
                    File(photoPath).delete()
                    return
                }

                viewModel.addPhotoPath(photoPath)
                mediaAdapter.addItem(CreateUpdatePostMedia(false, viewModel.photoPathList.value!!.size - 1, photoPath))
                mediaAdapter.notifyItemInserted(mediaAdapter.itemCount)

                binding.mediaRecyclerView.smoothScrollToPosition(mediaAdapter.itemCount - 1)
            }
            PICK_VIDEO -> {
                // Exception: Exceeds size limit
                if (Util.isExceedsFileSizeLimit(baseContext, data, FileType.FILE_SIZE_LIMIT_VIDEO)) {
                    Toast.makeText(baseContext, getText(R.string.file_size_limit_exception_message_100MB), Toast.LENGTH_SHORT).show()
                    return
                }

                val fileName = Util.getSelectedFileName(baseContext, data.data!!)

                // Exception: Duplicated file
                for (item in mediaAdapter.getDataSet()) {
                    if (fileName == item.path.substring(item.path.lastIndexOf("/") + 1)) {
                        Toast.makeText(baseContext, baseContext.getText(R.string.duplicate_file_exception_message), Toast.LENGTH_SHORT).show()
                        return
                    }
                }

                val videoPath = ServerUtil.createCopyAndReturnRealPathLocal(this,
                    data.data!!, CREATE_UPDATE_POST_DIRECTORY, fileName)

                // Exception: Not supported extension
                if (videoPath.isEmpty()) {
                    Toast.makeText(baseContext, getText(R.string.video_file_type_exception_message), Toast.LENGTH_LONG).show()
                    File(videoPath).delete()
                    return
                }

                // Exception: Not a photo
                if (!Util.isUrlVideo(videoPath)) {
                    Toast.makeText(baseContext, getText(R.string.video_file_type_exception_message), Toast.LENGTH_LONG).show()
                    File(videoPath).delete()
                    return
                }

                viewModel.addVideoPath(videoPath)
                mediaAdapter.addItem(CreateUpdatePostMedia(true, viewModel.videoPathList.value!!.size - 1, videoPath))
                mediaAdapter.notifyItemInserted(mediaAdapter.itemCount - 1)

                binding.mediaRecyclerView.smoothScrollToPosition(mediaAdapter.itemCount)
            }
            PICK_GENERAL_FILE -> {
                // Exception: Exceeds size limit
                if (Util.isExceedsFileSizeLimit(baseContext, data, FileType.FILE_SIZE_LIMIT_GENERAL)) {
                    Toast.makeText(baseContext, getText(R.string.file_size_limit_exception_message_100MB), Toast.LENGTH_SHORT).show()
                    return
                }

                val fileName = Util.getSelectedFileName(baseContext, data.data!!)

                // Exception: Duplicated file
                if (fileName in viewModel.generalFileNameList.value!!) {
                    Toast.makeText(baseContext, baseContext.getText(R.string.duplicate_file_exception_message), Toast.LENGTH_SHORT).show()
                    return
                }

                val generalFilePath = ServerUtil.createCopyAndReturnRealPathLocal(this,
                    data.data!!, CREATE_UPDATE_POST_DIRECTORY,fileName)

                // Exception: Not supported extension
                if (!Util.isUrlGeneralFile(generalFilePath)) {
                    Toast.makeText(baseContext, getText(R.string.general_file_type_exception_message), Toast.LENGTH_LONG).show()
                    File(generalFilePath).delete()
                    return
                }

                val generalFileName = generalFilePath.substring(generalFilePath.lastIndexOf("/") + 1)
                viewModel.addGeneralFileName(generalFileName)
                viewModel.addGeneralFilePath(generalFilePath)
                generalFileAdapter.addItem(generalFileName)
                generalFileAdapter.notifyItemInserted(generalFileAdapter.itemCount)

                binding.generalRecyclerView.smoothScrollToPosition(viewModel.generalFileNameList.value!!.size - 1)
            }
            else -> {
                Toast.makeText(baseContext, getText(R.string.file_type_exception_message), Toast.LENGTH_LONG).show()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        
        isViewDestroyed = true
        if(isFinishing) Util.deleteCopiedFiles(baseContext, CREATE_UPDATE_POST_DIRECTORY)
    }


    override fun onBackPressed() {
        AlertDialog.Builder(this).setMessage(getString(R.string.cancel_dialog_message))
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


    /** Databinding functions */
    fun onClickLocationButton() {
        viewModel.isUsingLocation.value = !viewModel.isUsingLocation.value!!

        val message = if (viewModel.isUsingLocation.value!!) getText(R.string.location_on)
        else getText(R.string.location_off)
        Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
    }

    fun onClickDisclosureButton() {
        when (viewModel.disclosure.value) {
            DISCLOSURE_PUBLIC -> {
                viewModel.disclosure.value = DISCLOSURE_PRIVATE
                binding.disclosureButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_lock_30))
                Toast.makeText(baseContext, getText(R.string.disclosure_private), Toast.LENGTH_SHORT).show()
            }
            DISCLOSURE_PRIVATE -> {
                viewModel.disclosure.value = DISCLOSURE_FRIEND
                binding.disclosureButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_group_30))
                Toast.makeText(baseContext, getText(R.string.disclosure_friend), Toast.LENGTH_SHORT).show()
            }
            DISCLOSURE_FRIEND -> {
                viewModel.disclosure.value = DISCLOSURE_PUBLIC
                binding.disclosureButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_public_30))
                Toast.makeText(baseContext, getText(R.string.disclosure_public), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onClickHashtagClearButton() {
        viewModel.hashtagEditText.value = ""
    }

    fun onClickPhotoAttachmentButton() {
        if(viewModel.photoPathList.value!!.size == 10) {
            Toast.makeText(baseContext, getText(R.string.photo_video_usage_full_message), Toast.LENGTH_LONG).show()
        } else {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "사진 선택"), PICK_PHOTO)
        }
    }

    fun onClickVideoAttachmentButton() {
        if (viewModel.videoPathList.value!!.size == 10) {
            Toast.makeText(baseContext, getText(R.string.photo_video_usage_full_message), Toast.LENGTH_LONG).show()
        } else {
            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "동영상 선택"), PICK_VIDEO)
        }
    }

    fun onClickGeneralFileButton() {
        if(viewModel.generalFilePathList.value!!.size == 10) {
            Toast.makeText(baseContext, getText(R.string.general_usage_full_message), Toast.LENGTH_LONG).show()
        } else {
            val intent = Intent()
            intent.type = "*/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "파일 선택"), PICK_GENERAL_FILE)
        }
    }

    fun onClickBackButton() {
        AlertDialog.Builder(this).setMessage(getString(R.string.cancel_dialog_message))
            .setPositiveButton(R.string.confirm) { _, _ -> finish() }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }


    fun onClickPostButton() {
        viewModel.contents.value = viewModel.contents.value!!.trim()

        AlertDialog.Builder(this).setMessage(getString(R.string.post_dialog_message))
            .setPositiveButton(R.string.confirm) { _, _ ->
                if(viewModel.isActivityTypeCreatePost()) createPost()
                else updatePost()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }

    private fun createPost() {
        val latAndLong = if (!viewModel.isUsingLocation.value!!) mutableListOf(0.0, 0.0) else Util.getGeolocation(baseContext)

        // 위치 정보 사용에 동의했지만, 권한이 없는 경우
        if(latAndLong[0] == (-1.0)){
            Permission.requestNotGrantedPermissions(baseContext, Permission.requiredPermissionsForLocation)
            unlockViews()
            return
        }

        // 총 3개의 API call을 수행합니다: 사진, 동영상, 파일 업데이트
        viewModel.remainingApiCallCountForCreateUpdate.value = 3

        val createPostReqDto = CreatePostReqDto(viewModel.selectedPetId.value!!, viewModel.contents.value,
            hashtagAdapter.getDataSet(), viewModel.disclosure.value!!, latAndLong[0], latAndLong[1])
        lockViews()

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .createPostReq(createPostReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            viewModel.postId = response.body()!!.id

            updatePostPhoto(response.body()!!.id)
            updatePostVideo(response.body()!!.id)
            updatePostGeneralFile(response.body()!!.id)
        }, { unlockViews() }, { unlockViews() })
    }

    private fun updatePostPhoto(id: Long) {
        if(viewModel.photoPathList.value!!.size == 0){
            viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
        }else{
            val fileList = makeFileListFromPathList(viewModel.photoPathList.value!!, false)
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .updatePostFileReq(id, fileList, FileType.IMAGE_FILE)
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
                viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
            }, { unlockViews() }, { unlockViews() })
        }
    }

    private fun makeFileListFromPathList(pathList: MutableList<String>, isGeneralFile: Boolean): ArrayList<MultipartBody.Part>{
        val fileList: ArrayList<MultipartBody.Part> = ArrayList()
        for(i in 0 until pathList.size) {
            val fileName = if(isGeneralFile) viewModel.generalFileNameList.value!![i]
                else "file_$i" + pathList[i].substring(pathList[i].lastIndexOf("."))
            val body = RequestBody.create(MediaType.parse("multipart/form-data"), File(pathList[i]))
            fileList.add(MultipartBody.Part.createFormData("fileList", fileName, body))
        }
        return fileList
    }

    private fun updatePostVideo(id: Long) {
        if(viewModel.videoPathList.value!!.size == 0) {
            viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
        }else{
            val fileList = makeFileListFromPathList(viewModel.videoPathList.value!!, false)
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .updatePostFileReq(id, fileList, FileType.VIDEO_FILE)
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
                viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
            }, { unlockViews() }, { unlockViews() })
        }
    }

    private fun updatePostGeneralFile(id: Long) {
        if (viewModel.generalFilePathList.value!!.size == 0) {
            viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
        }else{
            val fileList = makeFileListFromPathList(viewModel.generalFilePathList.value!!, true)
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .updatePostFileReq(id, fileList, FileType.GENERAL_FILE)
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
                viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
            }, { unlockViews() }, { unlockViews() })
        }
    }

    private fun lockViews() {
        viewModel.isApiLoading.value = true

        binding.recyclerviewCreateupdatepostPostpetselector.let {
            for(i in 0..petSelectorAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.isClickable = false
            }
        }
        binding.mediaRecyclerView.let {
            for(i in 0..mediaAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<ImageView>(R.id.imageview_deletebutton)?.isEnabled = false
            }
        }
        binding.generalRecyclerView.let {
            for(i in 0..generalFileAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<ImageView>(R.id.imageview_deletebutton)?.isEnabled = false
            }
        }
        binding.hashtagRecyclerView.let {
            for(i in 0..hashtagAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<ImageView>(R.id.imageview_deletebutton)?.isEnabled = false
            }
        }
    }

    private fun unlockViews() {
        viewModel.isApiLoading.value = false

        binding.recyclerviewCreateupdatepostPostpetselector.let {
            for(i in 0..petSelectorAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.isClickable = true
            }
        }
        binding.mediaRecyclerView.let {
            for(i in 0..mediaAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<ImageView>(R.id.imageview_deletebutton)?.isEnabled = true
            }
        }
        binding.generalRecyclerView.let {
            for(i in 0..generalFileAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<ImageView>(R.id.imageview_deletebutton)?.isEnabled = true
            }
        }
        binding.hashtagRecyclerView.let {
            for(i in 0..hashtagAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<ImageView>(R.id.imageview_deletebutton)?.isEnabled = true
            }
        }
    }

    private fun updatePost() {
        val latAndLong = if (!viewModel.isUsingLocation.value!!) mutableListOf(0.0, 0.0) else Util.getGeolocation(baseContext)

        // 위치 정보 사용에 동의했지만, 권한이 없는 경우
        if(latAndLong[0] == (-1.0)){
            Permission.requestNotGrantedPermissions(baseContext, Permission.requiredPermissionsForLocation)
            unlockViews()
            return
        }

        // 총 6개의 API call을 수행합니다: 사진, 동영상, 파일 업데이트 및 삭제
        viewModel.remainingApiCallCountForCreateUpdate.value = 6

        val updatePostReqDto = UpdatePostReqDto(viewModel.postId!!, viewModel.selectedPetId.value!!,
            viewModel.contents.value, hashtagAdapter.getDataSet(), viewModel.disclosure.value!!)
        lockViews()

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .updatePostReq(updatePostReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            updateOrDeletePhoto()
            updateOrDeleteVideo()
            updateOrDeleteGeneralFile()
        }, { unlockViews() }, { unlockViews() })
    }

    private fun updateOrDeletePhoto() {
        if(viewModel.photoPathList.value!!.size == 0){
            // 기존에도 사진이 없었는가?
            if(viewModel.originalImageCount == 0){
                viewModel.decreaseRemainingApiCallCountForCreateUpdate(2)
            }else{
                deletePostFile(viewModel.postId!!, FileType.IMAGE_FILE)
                viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
            }
        }else{
            updatePostPhoto(viewModel.postId!!)
            viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
        }
    }

    private fun deletePostFile(id: Long, fileType: String) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .deletePostFileReq(DeletePostFileReqDto(id, fileType))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
        }, { unlockViews() }, { unlockViews() })
    }

    private fun updateOrDeleteVideo() {
        if(viewModel.videoPathList.value!!.size == 0) {
            // 기존에도 동영상이 없었는가?
            if(viewModel.originalVideoCount == 0){
                viewModel.decreaseRemainingApiCallCountForCreateUpdate(2)
            }else{
                deletePostFile(viewModel.postId!!, FileType.VIDEO_FILE)
                viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
            }
        }else{
            updatePostVideo(viewModel.postId!!)
            viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
        }
    }

    private fun updateOrDeleteGeneralFile() {
        if(viewModel.generalFilePathList.value!!.size == 0) {
            // 기존에도 파일이 없었는가?
            if(viewModel.originalGeneralFileCount == 0){
                viewModel.decreaseRemainingApiCallCountForCreateUpdate(2)
            }else{
                deletePostFile(viewModel.postId!!, FileType.GENERAL_FILE)
                viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
            }
        }else{
            updatePostGeneralFile(viewModel.postId!!)
            viewModel.decreaseRemainingApiCallCountForCreateUpdate(1)
        }
    }
}