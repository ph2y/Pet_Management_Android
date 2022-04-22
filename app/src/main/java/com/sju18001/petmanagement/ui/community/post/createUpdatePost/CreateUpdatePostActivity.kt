package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.sju18001.petmanagement.controller.PatternRegex
import com.sju18001.petmanagement.controller.Permission
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import androidx.activity.viewModels
import com.sju18001.petmanagement.databinding.ActivityCreateupdatepostBinding
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.restapi.global.FileMetaData
import com.sju18001.petmanagement.restapi.global.FileType
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
        private const val DISCLOSURE_PUBLIC: String = "PUBLIC"
        private const val DISCLOSURE_PRIVATE: String = "PRIVATE"
        private const val DISCLOSURE_FRIEND: String = "FRIEND"
        private const val CREATE_UPDATE_POST_DIRECTORY: String = "create_update_post"
    }

    private lateinit var binding: ActivityCreateupdatepostBinding

    private val viewModel: CreateUpdatePostViewModel by viewModels()
    private var isViewDestroyed = false

    // variables for RecyclerView
    private lateinit var postPetSelectorAdapter: CreateUpdatePostPetSelectorAdapter
    private lateinit var mediaAdapter: MediaListAdapter
    private lateinit var generalFileAdapter: CreateUpdatePostGeneralFileAdapter
    private lateinit var hashtagAdapter: CreateUpdatePostHashtagAdapter

    /*
     * onStart에서, 펫 정보 또는 글 정보(update_post fragment)를 로드합니다.
     * 여러 곳에서 진행되는 api call이 모두 끝나면 로드가 완료된 것이며, 이것을
     * 감지하기 위해 사용하는 변수입니다. 정보 로드에 쓰이는 api call을 시작할 때,
     * increaseApiCallCountForFetch()를 호출하여 값을 1 증가시키고, api call이
     * 끝날 때, decreaseApiCallCountForFetch()를 호출하여 값을 1 감소시킵니다.
     */
    private var apiCallCountForFetch = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBinding()
        isViewDestroyed = false

        initializeRecyclerViews()

        supportActionBar?.hide()
        binding.adView.loadAd(AdRequest.Builder().build())
    }

    private fun setBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_createupdatepost)

        binding.lifecycleOwner = this
    }


    override fun onStart() {
        super.onStart()

        if(!viewModel.isFetched && !viewModel.isApiLoading) {
            when(intent.getStringExtra("fragmentType")) {
                // fetch post data for update
                "update_post" -> {
                    // save post id
                    viewModel.postId = intent.getLongExtra("postId", -1)

                    fetchPostData()
                }
                // fetch pet data + set pet recyclerview for create
                "create_post" -> {
                    fetchPetDataAndSetRecyclerView()
                }
            }
        }

        restoreState()

        // for title
        if(intent.getStringExtra("fragmentType") == "update_post") {
            binding.backButtonTitle.text = getText(R.string.update_post_title)
        }

        // for location button
        binding.locationButton.setOnClickListener {
            viewModel.isUsingLocation = !viewModel.isUsingLocation

            if (viewModel.isUsingLocation) {
                Toast.makeText(baseContext, getText(R.string.location_on), Toast.LENGTH_SHORT).show()
                binding.locationButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_location_on_30))
            } else {
                Toast.makeText(baseContext, getText(R.string.location_off), Toast.LENGTH_SHORT).show()
                binding.locationButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_location_off_30))
            }
        }

        // for disclosure button
        binding.disclosureButton.setOnClickListener {
            when (viewModel.disclosure) {
                DISCLOSURE_PUBLIC -> {
                    viewModel.disclosure = DISCLOSURE_PRIVATE

                    Toast.makeText(baseContext, getText(R.string.disclosure_private), Toast.LENGTH_SHORT).show()
                    binding.disclosureButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_lock_30))
                }
                DISCLOSURE_PRIVATE -> {
                    viewModel.disclosure = DISCLOSURE_FRIEND

                    Toast.makeText(baseContext, getText(R.string.disclosure_friend), Toast.LENGTH_SHORT).show()
                    binding.disclosureButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_group_30))
                }
                DISCLOSURE_FRIEND -> {
                    viewModel.disclosure = DISCLOSURE_PUBLIC

                    Toast.makeText(baseContext, getText(R.string.disclosure_public), Toast.LENGTH_SHORT).show()
                    binding.disclosureButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_public_30))
                }
            }
        }

        // for hashtag EditText listener
        binding.hashtagInputEditText.setOnEditorActionListener{ _, _, _ ->
            addHashtag()
            true
        }
        binding.hashtagInputEditText.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.hashtagEditText = s.toString()

                if (viewModel.hashtagEditText.isNotEmpty()) {
                    binding.hashtagClearButton.visibility = View.VISIBLE
                } else {
                    binding.hashtagClearButton.visibility = View.INVISIBLE
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        // for hashtag clear button
        binding.hashtagClearButton.setOnClickListener {
            viewModel.hashtagEditText = ""
            binding.hashtagInputEditText.setText(viewModel.hashtagEditText)
        }

        // for post EditText listener
        binding.postEditText.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.postEditText = s.toString()

                verifyAndEnableConfirmButton()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        // for photo attachment button
        binding.photoAttachmentButton.setOnClickListener {
            if(viewModel.photoPathList.size == 10) {
                Toast.makeText(baseContext, getText(R.string.photo_video_usage_full_message), Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "사진 선택"), PICK_PHOTO)
            }
        }

        // for video attachment button
        binding.videoAttachmentButton.setOnClickListener {
            if (viewModel.videoPathList.size == 10) {
                Toast.makeText(baseContext, getText(R.string.photo_video_usage_full_message), Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent()
                intent.type = "video/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "동영상 선택"), PICK_VIDEO)
            }
        }

        // for general attachment button
        binding.generalAttachmentButton.setOnClickListener {
            if(viewModel.generalFilePathList.size == 10) {
                Toast.makeText(baseContext, getText(R.string.general_usage_full_message), Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent()
                intent.type = "*/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "파일 선택"), PICK_GENERAL_FILE)
            }
        }

        // for confirm button
        binding.postButton.setOnClickListener {
            // trim post content
            viewModel.postEditText = viewModel.postEditText.trim()
            binding.postEditText.setText(viewModel.postEditText)

            AlertDialog.Builder(this).setMessage(getString(R.string.post_dialog_message))
                .setPositiveButton(R.string.confirm) { _, _ ->
                    if(intent.getStringExtra("fragmentType") == "create_post") {
                        createPost()
                    }
                    else {
                        updatePost()
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }.create().show()
        }

        // for back button
        binding.backButton.setOnClickListener {
            AlertDialog.Builder(this).setMessage(getString(R.string.cancel_dialog_message))
                .setPositiveButton(R.string.confirm) { _, _ ->
                    finish()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }.create().show()
        }

        Util.setupViewsForHideKeyboard(
            this,
            binding.fragmentCreateUpdatePostParentLayout,
            listOf(binding.footerLayout)
        )
    }

    // 글, 펫 정보 fetch 관련 로직
    private fun fetchPostData() {
        increaseApiCallCountForFetch()

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchPostReq(FetchPostReqDto(null, null, null, viewModel.postId, null, null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            // fetch post data (excluding files) and save to ViewModel
            val post = response.body()?.postList!![0]

            viewModel.selectedPetId = post.pet.id
            viewModel.isUsingLocation = post.geoTagLat != 0.0
            viewModel.disclosure = post.disclosure
            if(post.serializedHashTags != "") {
                viewModel.hashtagList = post.serializedHashTags.split(',').toMutableList()
                hashtagAdapter.setResult(viewModel.hashtagList)
            }
            viewModel.postEditText = post.contents

            // fetch post media (photos) data
            if (post.imageAttachments != null) {
                val postImage =
                    Gson().fromJson(post.imageAttachments, Array<FileMetaData>::class.java)

                // initialize lists
                for (i in postImage.indices) {
                    viewModel.photoPathList.add("")
                    viewModel.mediaList.add(MediaListItem(false, i))
                }

                fetchPostImageData(postImage)
            }

            // fetch post media (videos) data
            if (post.videoAttachments != null) {
                val postVideo =
                    Gson().fromJson(post.videoAttachments, Array<FileMetaData>::class.java)

                // initialize lists
                for (i in postVideo.indices) {
                    viewModel.videoPathList.add("")
                    viewModel.mediaList.add(MediaListItem(true, i))
                }

                fetchPostVideoData(postVideo)
            }

            // fetch post general data
            if (post.fileAttachments != null) {
                val postGeneral = Gson().fromJson(post.fileAttachments, Array<FileMetaData>::class.java)

                // initialize lists
                for(i in postGeneral.indices) {
                    viewModel.generalFilePathList.add("")
                    viewModel.generalFileNameList.add("")
                }

                fetchPostGeneralData(post.id, postGeneral)
            }

            fetchPetDataAndSetRecyclerView()

            verifyAndEnableConfirmButton()
            decreaseApiCallCountForFetch()
        }, {
            finish()
        }, {
            finish()
        })
    }

    private fun fetchPostImageData(postImage: Array<FileMetaData>) {
        for(index in postImage.indices) {
            increaseApiCallCountForFetch()

            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .fetchPostImageReq(FetchPostImageReqDto(viewModel.postId!!, index, FileType.ORIGINAL_IMAGE))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
                // get file extension
                val extension = postImage[index].url.split('.').last()

                // copy file and get real path
                val imageByteArray = response.body()!!.byteStream().readBytes()
                viewModel.photoPathList[index] =
                    ServerUtil.createCopyAndGetAbsolutePath(baseContext, imageByteArray, extension, CREATE_UPDATE_POST_DIRECTORY)

                // if all is done fetching -> set RecyclerView + set usage + show main ScrollView
                if("" !in viewModel.photoPathList) {
                    // update RecyclerView and photo usage
                    mediaAdapter.setResult(viewModel.mediaList)
                    updatePhotoUsage()
                }

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
                // get file extension
                val extension = postVideo[index].url.split('.').last()

                // copy file and get real path
                val videoByteArray = response.body()!!.byteStream().readBytes()
                viewModel.videoPathList[index] =
                    ServerUtil.createCopyAndGetAbsolutePath(baseContext, videoByteArray, extension, CREATE_UPDATE_POST_DIRECTORY)

                // if all is done fetching -> set RecyclerView + set usage + show main ScrollView
                if("" !in viewModel.videoPathList) {
                    // update RecyclerView and photo usage
                    mediaAdapter.setResult(viewModel.mediaList)
                    updateVideoUsage()
                }
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
                // get file extension and name
                val extension = postGeneral[index].url.split('.').last()

                // copy file and get real path
                val generalFileByteArray = response.body()!!.byteStream().readBytes()
                viewModel.generalFilePathList[index] =
                    ServerUtil.createCopyAndGetAbsolutePath(baseContext, generalFileByteArray, extension, CREATE_UPDATE_POST_DIRECTORY)

                // save general file name
                val generalFileName = postGeneral[index].url.split("post_${postId}_").last()
                viewModel.generalFileNameList[index] = generalFileName

                // if all is done fetching -> set RecyclerView + set usage + show main ScrollView
                if("" !in viewModel.generalFilePathList) {
                    // update RecyclerView and general usage
                    generalFileAdapter.setResult(viewModel.generalFileNameList)
                    updateGeneralUsage()
                }
                decreaseApiCallCountForFetch()
            }, { decreaseApiCallCountForFetch() }, { decreaseApiCallCountForFetch() })
        }
    }

    private fun fetchPetDataAndSetRecyclerView() {
        increaseApiCallCountForFetch()

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .fetchPetReq(FetchPetReqDto(null , null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            // fetch pet info (unsorted)
            val unsortedPetList: MutableList<CreateUpdatePostPetSelectorItem> = mutableListOf()
            response.body()?.petList?.map {
                val item = CreateUpdatePostPetSelectorItem(
                    it.id, it.photoUrl, null, it.name,
                    it.id == SessionManager.fetchLoggedInAccount(baseContext)?.representativePetId,
                    it.id == viewModel.selectedPetId
                )
                unsortedPetList.add(item)
            }

            reorderPetList(unsortedPetList)
            fetchPetPhotos()

            decreaseApiCallCountForFetch()
        }, { decreaseApiCallCountForFetch() }, { decreaseApiCallCountForFetch() })
    }

    private fun reorderPetList(apiResponse: MutableList<CreateUpdatePostPetSelectorItem>) {
        // get saved pet list order
        val petListOrder = PetManagerFragment()
            .getOrderedPetIdList(getString(R.string.data_name_pet_list_id_order), baseContext)

        // sort by order
        for (id in petListOrder) {
            val item = apiResponse.find { it.petId == id }

            if (item!!.isSelected) {
                viewModel.selectedPetIndex = viewModel.petList.size
            }
            viewModel.petList.add(item)
        }
    }

    private fun fetchPetPhotos() {
        val fetchedFlags = BooleanArray(viewModel.petList.size) { false }

        for (i in 0 until viewModel.petList.size) {
            // if no photo
            if (viewModel.petList[i].petPhotoUrl == null) {
                fetchedFlags[i] = true

                continue
            }

            // fetch pet photo
            increaseApiCallCountForFetch()

            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .fetchPetPhotoReq(FetchPetPhotoReqDto(viewModel.petList[i].petId))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
                viewModel.petList[i].petPhoto = BitmapFactory.decodeStream(response.body()!!.byteStream())

                fetchedFlags[i] = true

                decreaseApiCallCountForFetch()
            }, { decreaseApiCallCountForFetch() }, { decreaseApiCallCountForFetch() })
        }
    }

    private fun increaseApiCallCountForFetch() {
        // fetch를 처음 시작할 때
        if(apiCallCountForFetch == 0){
            showLoadingScreen()
        }

        apiCallCountForFetch += 1
    }

    private fun decreaseApiCallCountForFetch() {
        apiCallCountForFetch -= 1

        // fetch가 모두 끝났을 때
        if(apiCallCountForFetch == 0){
            viewModel.isFetched = true
            postPetSelectorAdapter.updateDataSet(viewModel.petList)

            hideLoadingScreen()
            restoreState()
        }
    }

    private fun showLoadingScreen() {
        viewModel.isApiLoading = true

        binding.locationButton.visibility = View.INVISIBLE
        binding.disclosureButton.visibility = View.INVISIBLE
        binding.dividerD.visibility = View.INVISIBLE
        binding.attachmentButtonsLayout.visibility = View.INVISIBLE
        binding.createUpdatePostMainScrollView.visibility = View.INVISIBLE
        binding.postDataLoadingLayout.visibility = View.VISIBLE
        binding.postButton.isEnabled = false
    }

    private fun hideLoadingScreen() {
        viewModel.isApiLoading = false

        binding.locationButton.visibility = View.VISIBLE
        binding.disclosureButton.visibility = View.VISIBLE
        binding.dividerD.visibility = View.VISIBLE
        binding.attachmentButtonsLayout.visibility = View.VISIBLE
        binding.createUpdatePostMainScrollView.visibility = View.VISIBLE
        binding.postDataLoadingLayout.visibility = View.GONE
        verifyAndEnableConfirmButton()
    }
    

    private fun initializeRecyclerViews() {
        // interface for confirm button verification inside RecyclerView adapter
        val confirmButtonAndUsageInterface = object: ConfirmButtonAndUsageInterface {
            override fun verifyAndEnableConfirmButton() {
                this@CreateUpdatePostActivity.verifyAndEnableConfirmButton()
            }
            override fun updatePhotoUsage() {
                this@CreateUpdatePostActivity.updatePhotoUsage()
            }
            override fun updateVideoUsage() {
                this@CreateUpdatePostActivity.updateVideoUsage()
            }
        }

        // initialize RecyclerView (for pet)
        postPetSelectorAdapter = CreateUpdatePostPetSelectorAdapter(viewModel, baseContext, confirmButtonAndUsageInterface)
        binding.recyclerviewCreateupdatepostPostpetselector.adapter = postPetSelectorAdapter
        binding.recyclerviewCreateupdatepostPostpetselector.layoutManager = LinearLayoutManager(this)
        (binding.recyclerviewCreateupdatepostPostpetselector.layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL
        postPetSelectorAdapter.updateDataSet(viewModel.petList)

        // initialize RecyclerView (for media)
        mediaAdapter = MediaListAdapter(viewModel, baseContext, binding, confirmButtonAndUsageInterface)
        binding.mediaRecyclerView.adapter = mediaAdapter
        binding.mediaRecyclerView.layoutManager = LinearLayoutManager(this)
        (binding.mediaRecyclerView.layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL
        mediaAdapter.setResult(viewModel.mediaList)

        // initialize RecyclerView (for general files)
        generalFileAdapter = CreateUpdatePostGeneralFileAdapter(viewModel, baseContext, binding, confirmButtonAndUsageInterface)
        binding.generalRecyclerView.adapter = generalFileAdapter
        binding.generalRecyclerView.layoutManager = LinearLayoutManager(this)
        (binding.generalRecyclerView.layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.VERTICAL
        generalFileAdapter.setResult(viewModel.generalFileNameList)

        // initialize RecyclerView (for hashtags)
        hashtagAdapter = CreateUpdatePostHashtagAdapter(viewModel, binding)
        binding.hashtagRecyclerView.adapter = hashtagAdapter
        binding.hashtagRecyclerView.layoutManager = FlexboxLayoutManager(this)
        (binding.hashtagRecyclerView.layoutManager as FlexboxLayoutManager).flexWrap = FlexWrap.WRAP
        hashtagAdapter.setResult(viewModel.hashtagList)
    }

    private fun updatePhotoUsage() {
        val uploadedPhotoCount = viewModel.photoPathList.size
        if (uploadedPhotoCount + viewModel.videoPathList.size == 0) {
            binding.mediaRecyclerView.visibility = View.GONE
        }
        else {
            binding.mediaRecyclerView.visibility = View.VISIBLE
        }
        val photoUsageText = "$uploadedPhotoCount/10"
        binding.photoUsage.text = photoUsageText
    }

    private fun updateVideoUsage() {
        val uploadedVideoCount = viewModel.videoPathList.size
        if (uploadedVideoCount + viewModel.photoPathList.size == 0) {
            binding.mediaRecyclerView.visibility = View.GONE
        }
        else {
            binding.mediaRecyclerView.visibility = View.VISIBLE
        }
        val videoUsageText = "$uploadedVideoCount/10"
        binding.videoUsage.text = videoUsageText
    }

    private fun updateGeneralUsage() {
        val uploadedCount = viewModel.generalFileNameList.size

        if (uploadedCount == 0) {
            binding.generalRecyclerView.visibility = View.GONE
        }
        else {
            binding.generalRecyclerView.visibility = View.VISIBLE
        }
        val generalUsageText = "$uploadedCount/10"
        binding.generalUsage.text = generalUsageText
    }


    private fun addHashtag(){
        if (viewModel.hashtagEditText.isEmpty()) return

        if(!PatternRegex.checkHashtagRegex(viewModel.hashtagEditText)) {
            Toast.makeText(baseContext, getText(R.string.hashtag_regex_exception_message), Toast.LENGTH_SHORT).show()
        }
        else if(viewModel.hashtagList.size == 5) {
            Toast.makeText(baseContext, getText(R.string.hashtag_usage_full_message), Toast.LENGTH_SHORT).show()
        }
        else {
            // save hashtag
            val hashtag = binding.hashtagInputEditText.text.toString()
            viewModel.hashtagList.add(hashtag)

            // update RecyclerView
            hashtagAdapter.notifyItemInserted(viewModel.hashtagList.size)
            binding.hashtagRecyclerView.smoothScrollToPosition(viewModel.hashtagList.size - 1)

            // reset hashtag EditText
            binding.hashtagInputEditText.setText("")
        }
    }


    private fun lockViews() {
        viewModel.isApiLoading = true

        binding.postButton.isEnabled = false
        binding.postButton.text = ""
        binding.createUpdatePostProgressBar.visibility = View.VISIBLE

        binding.recyclerviewCreateupdatepostPostpetselector.let {
            for(i in 0..postPetSelectorAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.isClickable = false
            }
        }
        binding.locationButton.isEnabled = false
        binding.photoAttachmentButton.isEnabled = false
        binding.videoAttachmentButton.isEnabled = false
        binding.generalAttachmentButton.isEnabled = false
        binding.disclosureButton.isEnabled = false
        binding.hashtagInputEditText.isEnabled = false
        binding.hashtagClearButton.isEnabled = false
        binding.postEditText.isEnabled = false
        binding.mediaRecyclerView.let {
            for(i in 0..mediaAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<ImageView>(R.id.delete_button)?.visibility = View.GONE
            }
        }
        binding.generalRecyclerView.let {
            for(i in 0..generalFileAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<ImageView>(R.id.delete_button)?.visibility = View.GONE
            }
        }
        binding.hashtagRecyclerView.let {
            for(i in 0..hashtagAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<ImageView>(R.id.delete_button)?.visibility = View.GONE
            }
        }
        binding.backButton.isEnabled = false
    }

    private fun unlockViews() {
        viewModel.isApiLoading = false

        binding.postButton.isEnabled = true
        binding.postButton.text = getText(R.string.confirm)
        binding.createUpdatePostProgressBar.visibility = View.GONE

        binding.recyclerviewCreateupdatepostPostpetselector.let {
            for(i in 0..postPetSelectorAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.isClickable = true
            }
        }
        binding.locationButton.isEnabled = true
        binding.photoAttachmentButton.isEnabled = true
        binding.videoAttachmentButton.isEnabled = true
        binding.generalAttachmentButton.isEnabled = true
        binding.disclosureButton.isEnabled = true
        binding.hashtagInputEditText.isEnabled = true
        binding.hashtagClearButton.isEnabled = true
        binding.postEditText.isEnabled = true
        binding.mediaRecyclerView.let {
            for(i in 0..mediaAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<ImageView>(R.id.delete_button)?.visibility = View.VISIBLE
            }
        }
        binding.generalRecyclerView.let {
            for(i in 0..generalFileAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<ImageView>(R.id.delete_button)?.visibility = View.VISIBLE
            }
        }
        binding.hashtagRecyclerView.let {
            for(i in 0..hashtagAdapter.itemCount) {
                it.findViewHolderForLayoutPosition(i)?.itemView?.findViewById<ImageView>(R.id.delete_button)?.visibility = View.VISIBLE
            }
        }
        binding.backButton.isEnabled = true
    }

    private fun isApiLoadComplete(): Boolean {
        return viewModel.updatedPostPhotoData && viewModel.updatedPostVideoData
                && viewModel.updatedPostGeneralFileData && viewModel.deletedPostPhotoData
                && viewModel.deletedPostVideoData && viewModel.updatedPostGeneralFileData
    }

    private fun verifyAndEnableConfirmButton() {
        binding.postButton.isEnabled = !(viewModel.mediaList.size == 0
                && viewModel.generalFileNameList.size == 0
                && viewModel.postEditText.trim().isEmpty())
                && viewModel.selectedPetId != null
                && binding.postDataLoadingLayout.visibility == View.GONE
                && !viewModel.isApiLoading
    }

    private fun createPost() {
        lockViews()

        // get location data(if enabled)
        val latAndLong = if (!viewModel.isUsingLocation) mutableListOf(0.0, 0.0) else Util.getGeolocation(baseContext)

        // 위치 정보 사용에 동의했지만, 권한이 없는 경우
        if(latAndLong[0] == (-1.0)){
            Permission.requestNotGrantedPermissions(baseContext, Permission.requiredPermissionsForLocation)

            // 권한 요청이 비동기적이기 때문에, 권한 요청 이후에 CreatePost 버튼을 다시 눌러야한다.
            unlockViews()
            return
        }

        // create DTO
        val createPostReqDto = CreatePostReqDto(
            viewModel.selectedPetId!!,
            viewModel.postEditText,
            viewModel.hashtagList,
            viewModel.disclosure,
            latAndLong[0],
            latAndLong[1]
        )

        // set deleted to true (because there is nothing to delete)
        viewModel.deletedPostPhotoData = true
        viewModel.deletedPostVideoData = true
        viewModel.deletedPostGeneralFileData = true

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .createPostReq(createPostReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, { response ->
            intent.putExtra("postId", response.body()!!.id)
            updatePostPhoto(response.body()!!.id)
            updatePostVideo(response.body()!!.id)
            updatePostGeneralFile(response.body()!!.id)
        }, {
            unlockViews()
        }, {
            unlockViews()
        })
    }

    private fun updatePost() {
        lockViews()

        // get location data(if enabled)
        val latAndLong = if (!viewModel.isUsingLocation) mutableListOf(0.0, 0.0) else Util.getGeolocation(baseContext)

        // 위치 정보 사용에 동의했지만, 권한이 없는 경우
        if(latAndLong[0] == (-1.0)){
            Permission.requestNotGrantedPermissions(baseContext, Permission.requiredPermissionsForLocation)

            // 권한 요청이 비동기적이기 때문에, 권한 요청 이후에 CreatePost 버튼을 다시 눌러야한다.
            unlockViews()
            return
        }

        // create DTO
        val updatePostReqDto = UpdatePostReqDto(
            viewModel.postId!!,
            viewModel.selectedPetId!!,
            viewModel.postEditText,
            viewModel.hashtagList,
            viewModel.disclosure
        )

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .updatePostReq(updatePostReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            // update photos
            if(viewModel.photoPathList.size == 0) {
                // 기존에 사진이 0개였다면 FileType.IMAGE_FILE에 대해 DeletePostFile를 호출하지 않는다
                if(intent.getIntExtra("originalImageCount", 0) > 0){
                    viewModel.updatedPostPhotoData = true
                    deletePostFile(viewModel.postId!!, FileType.IMAGE_FILE)
                }else{
                    viewModel.updatedPostPhotoData = true
                    viewModel.deletedPostPhotoData = true

                    if (isApiLoadComplete()) {
                        passDataToCommunity()
                        closeAfterSuccess()
                    }
                }
            } else {
                viewModel.deletedPostPhotoData = true
                updatePostPhoto(viewModel.postId!!)
            }

            // update videos
            if(viewModel.videoPathList.size == 0) {
                // 기존에 동영상이 0개였다면 FileType.VIDEO_FILE에 대해 DeletePostFile를 호출하지 않는다
                if(intent.getIntExtra("originalVideoCount", 0) > 0){
                    viewModel.updatedPostVideoData = true
                    deletePostFile(viewModel.postId!!, FileType.VIDEO_FILE)
                }else{
                    viewModel.updatedPostVideoData = true
                    viewModel.deletedPostVideoData = true

                    if (isApiLoadComplete()) {
                        passDataToCommunity()
                        closeAfterSuccess()
                    }
                }
            } else {
                viewModel.deletedPostVideoData = true
                updatePostVideo(viewModel.postId!!)
            }

            // update general files
            if(viewModel.generalFilePathList.size == 0) {
                // 기존에 General Files가 0개였다면 FileType.GENERAL_FILE에 대해 DeletePostFile를 호출하지 않는다
                if(intent.getIntExtra("originalGeneralFileCount", 0) > 0){
                    viewModel.updatedPostGeneralFileData = true
                    deletePostFile(viewModel.postId!!, FileType.GENERAL_FILE)
                }else{
                    viewModel.updatedPostGeneralFileData = true
                    viewModel.deletedPostGeneralFileData = true

                    if (isApiLoadComplete()) {
                        passDataToCommunity()
                        closeAfterSuccess()
                    }
                }
            } else {
                viewModel.deletedPostGeneralFileData = true
                updatePostGeneralFile(viewModel.postId!!)
            }
        }, {
            unlockViews()
        }, {
            unlockViews()
        })
    }

    private fun deletePostFile(id: Long, fileType: String) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .deletePostFileReq(DeletePostFileReqDto(id, fileType))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
            if (fileType == FileType.IMAGE_FILE) {
                viewModel.deletedPostPhotoData = true
            }
            if (fileType == FileType.GENERAL_FILE) {
                viewModel.deletedPostGeneralFileData = true
            }

            if (isApiLoadComplete()) {
                passDataToCommunity()
                closeAfterSuccess()
            }
        }, {
            unlockViews()
        }, {
            unlockViews()
        })
    }

    private fun updatePostPhoto(id: Long) {
        // exception (no media files)
        if(viewModel.photoPathList.size == 0) {
            viewModel.updatedPostPhotoData = true

            if (isApiLoadComplete()) {
                passDataToCommunity()
                closeAfterSuccess()
            }
        } else {
            // create file list
            val fileList: ArrayList<MultipartBody.Part> = ArrayList()
            for(i in 0 until viewModel.photoPathList.size) {
                val fileName = "file_$i" + viewModel.photoPathList[i]
                    .substring(viewModel.photoPathList[i].lastIndexOf("."))

                fileList.add(MultipartBody.Part.createFormData("fileList", fileName,
                    RequestBody.create(MediaType.parse("multipart/form-data"), File(viewModel.photoPathList[i]))))
            }

            // API call
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .updatePostFileReq(id, fileList, FileType.IMAGE_FILE)
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
                viewModel.updatedPostPhotoData = true

                if (isApiLoadComplete()) {
                    passDataToCommunity()
                    closeAfterSuccess()
                }
            }, {
                unlockViews()
            }, {
                unlockViews()
            })
        }
    }

    private fun updatePostVideo(id: Long) {
        // exception (no media files)
        if(viewModel.videoPathList.size == 0) {
            viewModel.updatedPostVideoData = true

            if (isApiLoadComplete()) {
                passDataToCommunity()
                closeAfterSuccess()
            }
        } else {
            // create file list
            val fileList: ArrayList<MultipartBody.Part> = ArrayList()
            for(i in 0 until viewModel.videoPathList.size) {
                val fileName = "file_$i" + viewModel.videoPathList[i]
                    .substring(viewModel.videoPathList[i].lastIndexOf("."))

                fileList.add(MultipartBody.Part.createFormData("fileList", fileName,
                    RequestBody.create(MediaType.parse("multipart/form-data"), File(viewModel.videoPathList[i]))))
            }

            // API call
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .updatePostFileReq(id, fileList, FileType.VIDEO_FILE)
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
                viewModel.updatedPostVideoData = true

                if (isApiLoadComplete()) {
                    passDataToCommunity()
                    closeAfterSuccess()
                }
            }, {
                unlockViews()
            }, {
                unlockViews()
            })
        }
    }

    private fun updatePostGeneralFile(id: Long) {
        // exception (no general files)
        if (viewModel.generalFilePathList.size == 0) {
            viewModel.updatedPostGeneralFileData = true

            if (isApiLoadComplete()) {
                passDataToCommunity()
                closeAfterSuccess()
            }
        } else {
            // create file list
            val fileList: ArrayList<MultipartBody.Part> = ArrayList()
            for(i in 0 until viewModel.generalFilePathList.size) {
                val fileName = viewModel.generalFileNameList[i]

                fileList.add(MultipartBody.Part.createFormData("fileList", fileName,
                    RequestBody.create(MediaType.parse("multipart/form-data"), File(viewModel.generalFilePathList[i]))))
            }

            // API call
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
                .updatePostFileReq(id, fileList, FileType.GENERAL_FILE)
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, baseContext, {
                viewModel.updatedPostGeneralFileData = true

                if (isApiLoadComplete()) {
                    passDataToCommunity()
                    closeAfterSuccess()
                }
            }, {
                unlockViews()
            }, {
                unlockViews()
            })
        }
    }


    // Pass post id, position to Community(for post edit)
    private fun passDataToCommunity() {
        val resultIntent = Intent()
        resultIntent.putExtra("postId", intent.getLongExtra("postId", -1))
        resultIntent.putExtra("position", intent.getIntExtra("position", -1))
        setResult(Activity.RESULT_OK, resultIntent)
    }

    // close after success
    private fun closeAfterSuccess() {
        unlockViews()

        // delete copied files(if any)
        if(isFinishing) {
            Util.deleteCopiedFiles(baseContext, CREATE_UPDATE_POST_DIRECTORY)
        }

        // show message + return to previous activity
        if(intent.getStringExtra("fragmentType") == "create_post") {
            Toast.makeText(baseContext, getText(R.string.create_post_successful), Toast.LENGTH_LONG).show()
        }
        else {
            Toast.makeText(baseContext, getText(R.string.update_post_successful), Toast.LENGTH_LONG).show()
        }

        finish()
    }

    // for view restore
    private fun restoreState() {
        // restore usages
        updatePhotoUsage()
        updateVideoUsage()
        updateGeneralUsage()

        // restore location button
        if (viewModel.isUsingLocation) {
            binding.locationButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_location_on_30))
        } else {
            binding.locationButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_location_off_30))
        }

        // restore disclosure button
        when (viewModel.disclosure) {
            DISCLOSURE_PUBLIC -> {
                binding.disclosureButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_public_30))
            }
            DISCLOSURE_PRIVATE -> {
                binding.disclosureButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_lock_30))
            }
            DISCLOSURE_FRIEND -> {
                binding.disclosureButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_group_30))
            }
        }

        // restore hashtag layout
        binding.hashtagInputEditText.setText(viewModel.hashtagEditText)
        if (viewModel.hashtagEditText.isNotEmpty()) {
            binding.hashtagClearButton.visibility = View.VISIBLE
        }

        // restore post EditText
        binding.postEditText.setText(viewModel.postEditText)

        // restore confirm button
        verifyAndEnableConfirmButton()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // exception
        if(resultCode != AppCompatActivity.RESULT_OK) { return }

        // get selected file value
        when(requestCode) {
            PICK_PHOTO -> {
                if(data != null) {
                    // check file size limit
                    if (Util.isExceedsFileSizeLimit(baseContext, data, FileType.FILE_SIZE_LIMIT_PHOTO)) {
                        Toast.makeText(baseContext, getText(R.string.file_size_limit_exception_message_20MB), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // get file name
                    val fileName = Util.getSelectedFileName(baseContext, data.data!!)

                    // duplicate file exception
                    for (path in viewModel.photoPathList) {
                        if (fileName == path.substring(path.lastIndexOf("/") + 1)) {
                            Toast.makeText(baseContext, baseContext
                                .getText(R.string.duplicate_file_exception_message), Toast.LENGTH_SHORT).show()
                            return
                        }
                    }

                    // copy selected photo and get real path
                    val postPhotoPathValue = ServerUtil.createCopyAndReturnRealPathLocal(this,
                        data.data!!, CREATE_UPDATE_POST_DIRECTORY, fileName)

                    // no extension exception
                    if (postPhotoPathValue.isEmpty()) {
                        Toast.makeText(baseContext, getText(R.string.photo_file_type_exception_message), Toast.LENGTH_LONG).show()
                        return
                    }

                    // file type exception -> delete copied file + show Toast message
                    if (!Util.isUrlPhoto(postPhotoPathValue)) {
                        Toast.makeText(baseContext, getText(R.string.photo_file_type_exception_message), Toast.LENGTH_LONG).show()
                        File(postPhotoPathValue).delete()
                        return
                    }

                    // add path to list
                    viewModel.photoPathList.add(postPhotoPathValue)

                    // save media list item
                    viewModel.mediaList
                        .add(MediaListItem(false, viewModel.photoPathList.size - 1))

                    verifyAndEnableConfirmButton()

                    // update RecyclerView
                    mediaAdapter.notifyItemInserted(viewModel.mediaList.size)
                    binding.mediaRecyclerView.smoothScrollToPosition(viewModel.mediaList.size - 1)

                    updatePhotoUsage()
                } else {
                    Toast.makeText(baseContext, getText(R.string.file_null_exception_message), Toast.LENGTH_LONG).show()
                }
            }
            PICK_VIDEO -> {
                if(data != null) {
                    // check file size limit
                    if (Util.isExceedsFileSizeLimit(baseContext, data, FileType.FILE_SIZE_LIMIT_VIDEO)) {
                        Toast.makeText(baseContext, getText(R.string.file_size_limit_exception_message_100MB), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // get file name
                    val fileName = Util.getSelectedFileName(baseContext, data.data!!)

                    // duplicate file exception
                    for (path in viewModel.videoPathList) {
                        if (fileName == path.substring(path.lastIndexOf("/") + 1)) {
                            Toast.makeText(baseContext, baseContext
                                .getText(R.string.duplicate_file_exception_message), Toast.LENGTH_SHORT).show()
                            return
                        }
                    }

                    // copy selected video and get real path
                    val postVideoPathValue = ServerUtil.createCopyAndReturnRealPathLocal(this,
                        data.data!!, CREATE_UPDATE_POST_DIRECTORY, fileName)

                    // no extension exception
                    if (postVideoPathValue.isEmpty()) {
                        Toast.makeText(baseContext, getText(R.string.video_file_type_exception_message), Toast.LENGTH_LONG).show()
                        return
                    }

                    // file type exception -> delete copied file + show Toast message
                    if (!Util.isUrlVideo(postVideoPathValue)) {
                        Toast.makeText(baseContext, getText(R.string.video_file_type_exception_message), Toast.LENGTH_LONG).show()
                        File(postVideoPathValue).delete()
                        return
                    }

                    // add path to list
                    viewModel.videoPathList.add(postVideoPathValue)

                    // save media list item
                    viewModel.mediaList
                        .add(MediaListItem(true, viewModel.videoPathList.size - 1))

                    verifyAndEnableConfirmButton()

                    // update RecyclerView
                    mediaAdapter.notifyItemInserted(viewModel.mediaList.size)
                    binding.mediaRecyclerView.smoothScrollToPosition(viewModel.mediaList.size - 1)

                    updateVideoUsage()
                } else {
                    Toast.makeText(baseContext, getText(R.string.file_null_exception_message), Toast.LENGTH_LONG).show()
                }
            }
            PICK_GENERAL_FILE -> {
                if(data != null) {
                    // check file size limit
                    if (Util.isExceedsFileSizeLimit(baseContext, data, FileType.FILE_SIZE_LIMIT_GENERAL)) {
                        Toast.makeText(baseContext, getText(R.string.file_size_limit_exception_message_100MB), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // get file name
                    val fileName = Util.getSelectedFileName(baseContext, data.data!!)

                    // duplicate file exception
                    if (fileName in viewModel.generalFileNameList) {
                        Toast.makeText(baseContext, baseContext
                            .getText(R.string.duplicate_file_exception_message), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // copy selected general file and get real path
                    val postGeneralFilePathValue = ServerUtil.createCopyAndReturnRealPathLocal(this,
                        data.data!!, CREATE_UPDATE_POST_DIRECTORY,fileName)

                    // file type exception -> delete copied file + show Toast message
                    if (!Util.isUrlGeneralFile(postGeneralFilePathValue)) {
                        Toast.makeText(baseContext, getText(R.string.general_file_type_exception_message), Toast.LENGTH_LONG).show()
                        File(postGeneralFilePathValue).delete()
                        return
                    }

                    // add path to list
                    viewModel.generalFilePathList.add(postGeneralFilePathValue)

                    // save file name
                    val generalFileName = viewModel.generalFilePathList.last()
                        .substring(viewModel.generalFilePathList.last().lastIndexOf("/") + 1)
                    viewModel.generalFileNameList.add(generalFileName)

                    verifyAndEnableConfirmButton()

                    // update RecyclerView
                    generalFileAdapter.notifyItemInserted(viewModel.generalFileNameList.size)
                    binding.generalRecyclerView.smoothScrollToPosition(viewModel.generalFileNameList.size - 1)

                    updateGeneralUsage()
                } else {
                    Toast.makeText(baseContext, getText(R.string.file_null_exception_message), Toast.LENGTH_LONG).show()
                }
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
}

interface ConfirmButtonAndUsageInterface {
    fun verifyAndEnableConfirmButton()
    fun updatePhotoUsage()
    fun updateVideoUsage()
}