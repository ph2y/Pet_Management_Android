package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.gson.Gson
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.PatternRegex
import com.sju18001.petmanagement.controller.Permission
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentCreateUpdatePostBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.restapi.global.FileMetaData
import com.sju18001.petmanagement.restapi.global.FileType
import com.sju18001.petmanagement.ui.myPet.petManager.PetManagerFragment
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class CreateUpdatePostFragment : Fragment() {

    // constant variables
    private val PICK_PHOTO = 0
    private val PICK_VIDEO = 1
    private val PICK_GENERAL_FILE = 2
    private var DISCLOSURE_PUBLIC: String = "PUBLIC"
    private var DISCLOSURE_PRIVATE: String = "PRIVATE"
    private var DISCLOSURE_FRIEND: String = "FRIEND"
    private var CREATE_UPDATE_POST_DIRECTORY: String = "create_update_post"

    // variable for ViewModel
    private val createUpdatePostViewModel: CreateUpdatePostViewModel by activityViewModels()

    // variables for view binding
    private var _binding: FragmentCreateUpdatePostBinding? = null
    private val binding get() = _binding!!

    private var isViewDestroyed = false

    // variables for RecyclerView
    private lateinit var petAdapter: PetListAdapter
    private lateinit var mediaAdapter: MediaListAdapter
    private lateinit var generalFilesAdapter: GeneralFileListAdapter
    private lateinit var hashtagAdapter: HashtagListAdapter

    /*
     * onStart에서, 펫 정보 또는 글 정보(update_post fragment)를 로드합니다.
     * 여러 곳에서 진행되는 api call이 모두 끝나면 로드가 완료된 것이며, 이것을
     * 감지하기 위해 사용하는 변수입니다. 정보 로드에 쓰이는 api call을 시작할 때,
     * increaseApiCallCountForFetch()를 호출하여 값을 1 증가시키고, api call이
     * 끝날 때, decreaseApiCallCountForFetch()를 호출하여 값을 1 감소시킵니다.
     */
    private var apiCallCountForFetch = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCreateUpdatePostBinding.inflate(inflater, container, false)
        isViewDestroyed = false

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeRecyclerViews()

        binding.adView.loadAd(AdRequest.Builder().build())
    }

    override fun onStart() {
        super.onStart()

        if(!createUpdatePostViewModel.isFetched && !createUpdatePostViewModel.isApiLoading) {
            when(requireActivity().intent.getStringExtra("fragmentType")) {
                // fetch post data for update
                "update_post" -> {
                    // save post id
                    createUpdatePostViewModel.postId = requireActivity().intent.getLongExtra("postId", -1)

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
        if(requireActivity().intent.getStringExtra("fragmentType") == "update_post") {
            binding.backButtonTitle.text = context?.getText(R.string.update_post_title)
        }

        // for location button
        binding.locationButton.setOnClickListener {
            createUpdatePostViewModel.isUsingLocation = !createUpdatePostViewModel.isUsingLocation

            if (createUpdatePostViewModel.isUsingLocation) {
                Toast.makeText(context, context?.getText(R.string.location_on), Toast.LENGTH_SHORT).show()
                binding.locationButton.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_location_on_30))
            } else {
                Toast.makeText(context, context?.getText(R.string.location_off), Toast.LENGTH_SHORT).show()
                binding.locationButton.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_location_off_30))
            }
        }

        // for disclosure button
        binding.disclosureButton.setOnClickListener {
            when (createUpdatePostViewModel.disclosure) {
                DISCLOSURE_PUBLIC -> {
                    createUpdatePostViewModel.disclosure = DISCLOSURE_PRIVATE

                    Toast.makeText(context, context?.getText(R.string.disclosure_private), Toast.LENGTH_SHORT).show()
                    binding.disclosureButton.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_lock_30))
                }
                DISCLOSURE_PRIVATE -> {
                    createUpdatePostViewModel.disclosure = DISCLOSURE_FRIEND

                    Toast.makeText(context, context?.getText(R.string.disclosure_friend), Toast.LENGTH_SHORT).show()
                    binding.disclosureButton.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_group_30))
                }
                DISCLOSURE_FRIEND -> {
                    createUpdatePostViewModel.disclosure = DISCLOSURE_PUBLIC

                    Toast.makeText(context, context?.getText(R.string.disclosure_public), Toast.LENGTH_SHORT).show()
                    binding.disclosureButton.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_public_30))
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
                createUpdatePostViewModel.hashtagEditText = s.toString()

                if (createUpdatePostViewModel.hashtagEditText.isNotEmpty()) {
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
            createUpdatePostViewModel.hashtagEditText = ""
            binding.hashtagInputEditText.setText(createUpdatePostViewModel.hashtagEditText)
        }

        // for post EditText listener
        binding.postEditText.addTextChangedListener(object: TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                createUpdatePostViewModel.postEditText = s.toString()

                verifyAndEnableConfirmButton()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        // for photo attachment button
        binding.photoAttachmentButton.setOnClickListener {
            if(createUpdatePostViewModel.photoPathList.size == 10) {
                Toast.makeText(context, context?.getText(R.string.photo_video_usage_full_message), Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "사진 선택"), PICK_PHOTO)
            }
        }

        // for video attachment button
        binding.videoAttachmentButton.setOnClickListener {
            if (createUpdatePostViewModel.videoPathList.size == 10) {
                Toast.makeText(context, context?.getText(R.string.photo_video_usage_full_message), Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent()
                intent.type = "video/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "동영상 선택"), PICK_VIDEO)
            }
        }

        // for general attachment button
        binding.generalAttachmentButton.setOnClickListener {
            if(createUpdatePostViewModel.generalFilePathList.size == 10) {
                Toast.makeText(context, context?.getText(R.string.general_usage_full_message), Toast.LENGTH_LONG).show()
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
            createUpdatePostViewModel.postEditText = createUpdatePostViewModel.postEditText.trim()
            binding.postEditText.setText(createUpdatePostViewModel.postEditText)

            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage(requireContext().getString(R.string.post_dialog_message))
                .setPositiveButton(R.string.confirm) { _, _ ->
                    if(requireActivity().intent.getStringExtra("fragmentType") == "create_post") {
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
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage(requireContext().getString(R.string.cancel_dialog_message))
                .setPositiveButton(R.string.confirm) { _, _ ->
                    activity?.finish()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }.create().show()
        }

        Util.setupViewsForHideKeyboard(requireActivity(), binding.fragmentCreateUpdatePostParentLayout)
    }

    // 글, 펫 정보 fetch 관련 로직
    private fun fetchPostData() {
        increaseApiCallCountForFetch()

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPostReq(FetchPostReqDto(null, null, null, createUpdatePostViewModel.postId, null, null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            // fetch post data (excluding files) and save to ViewModel
            val post = response.body()?.postList!![0]

            createUpdatePostViewModel.selectedPetId = post.pet.id
            createUpdatePostViewModel.isUsingLocation = post.geoTagLat != 0.0
            createUpdatePostViewModel.disclosure = post.disclosure
            if(post.serializedHashTags != "") {
                createUpdatePostViewModel.hashtagList = post.serializedHashTags.split(',').toMutableList()
                hashtagAdapter.setResult(createUpdatePostViewModel.hashtagList)
            }
            createUpdatePostViewModel.postEditText = post.contents

            // fetch post media (photos) data
            if (post.imageAttachments != null) {
                val postImage =
                    Gson().fromJson(post.imageAttachments, Array<FileMetaData>::class.java)

                // initialize lists
                for (i in postImage.indices) {
                    createUpdatePostViewModel.photoPathList.add("")
                    createUpdatePostViewModel.mediaList.add(MediaListItem(false, i))
                }

                fetchPostImageData(postImage)
            }

            // fetch post media (videos) data
            if (post.videoAttachments != null) {
                val postVideo =
                    Gson().fromJson(post.videoAttachments, Array<FileMetaData>::class.java)

                // initialize lists
                for (i in postVideo.indices) {
                    createUpdatePostViewModel.videoPathList.add("")
                    createUpdatePostViewModel.mediaList.add(MediaListItem(true, i))
                }

                fetchPostVideoData(postVideo)
            }

            // fetch post general data
            if (post.fileAttachments != null) {
                val postGeneral = Gson().fromJson(post.fileAttachments, Array<FileMetaData>::class.java)

                // initialize lists
                for(i in postGeneral.indices) {
                    createUpdatePostViewModel.generalFilePathList.add("")
                    createUpdatePostViewModel.generalFileNameList.add("")
                }

                fetchPostGeneralData(post.id, postGeneral)
            }

            fetchPetDataAndSetRecyclerView()

            verifyAndEnableConfirmButton()
            decreaseApiCallCountForFetch()
        }, {
            requireActivity().finish()
        }, {
            requireActivity().finish()
        })
    }

    private fun fetchPostImageData(postImage: Array<FileMetaData>) {
        for(index in postImage.indices) {
            increaseApiCallCountForFetch()

            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .fetchPostImageReq(FetchPostImageReqDto(createUpdatePostViewModel.postId!!, index, FileType.ORIGINAL_IMAGE))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                // get file extension
                val extension = postImage[index].url.split('.').last()

                // copy file and get real path
                val imageByteArray = response.body()!!.byteStream().readBytes()
                createUpdatePostViewModel.photoPathList[index] =
                    ServerUtil.createCopyAndReturnRealPathServer(requireContext(), imageByteArray, extension, CREATE_UPDATE_POST_DIRECTORY)

                // if all is done fetching -> set RecyclerView + set usage + show main ScrollView
                if("" !in createUpdatePostViewModel.photoPathList) {
                    // update RecyclerView and photo usage
                    mediaAdapter.setResult(createUpdatePostViewModel.mediaList)
                    updatePhotoUsage()
                }

                decreaseApiCallCountForFetch()
            }, { decreaseApiCallCountForFetch() }, { decreaseApiCallCountForFetch() })
        }
    }

    private fun fetchPostVideoData(postVideo: Array<FileMetaData>) {
        for(index in postVideo.indices) {
            increaseApiCallCountForFetch()

            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .fetchPostVideoReq(postVideo[index].url)
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                // get file extension
                val extension = postVideo[index].url.split('.').last()

                // copy file and get real path
                val videoByteArray = response.body()!!.byteStream().readBytes()
                createUpdatePostViewModel.videoPathList[index] =
                    ServerUtil.createCopyAndReturnRealPathServer(requireContext(), videoByteArray, extension, CREATE_UPDATE_POST_DIRECTORY)

                // if all is done fetching -> set RecyclerView + set usage + show main ScrollView
                if("" !in createUpdatePostViewModel.videoPathList) {
                    // update RecyclerView and photo usage
                    mediaAdapter.setResult(createUpdatePostViewModel.mediaList)
                    updateVideoUsage()
                }
                decreaseApiCallCountForFetch()
            }, { decreaseApiCallCountForFetch() }, { decreaseApiCallCountForFetch() })
        }
    }

    private fun fetchPostGeneralData(postId: Long, postGeneral: Array<FileMetaData>) {
        for(index in postGeneral.indices) {
            increaseApiCallCountForFetch()

            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .fetchPostFileReq(FetchPostFileReqDto(createUpdatePostViewModel.postId!!, index))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                // get file extension and name
                val extension = postGeneral[index].url.split('.').last()

                // copy file and get real path
                val generalFileByteArray = response.body()!!.byteStream().readBytes()
                createUpdatePostViewModel.generalFilePathList[index] =
                    ServerUtil.createCopyAndReturnRealPathServer(requireContext(), generalFileByteArray, extension, CREATE_UPDATE_POST_DIRECTORY)

                // save general file name
                val generalFileName = postGeneral[index].url.split("post_${postId}_").last()
                createUpdatePostViewModel.generalFileNameList[index] = generalFileName

                // if all is done fetching -> set RecyclerView + set usage + show main ScrollView
                if("" !in createUpdatePostViewModel.generalFilePathList) {
                    // update RecyclerView and general usage
                    generalFilesAdapter.setResult(createUpdatePostViewModel.generalFileNameList)
                    updateGeneralUsage()
                }
                decreaseApiCallCountForFetch()
            }, { decreaseApiCallCountForFetch() }, { decreaseApiCallCountForFetch() })
        }
    }

    private fun fetchPetDataAndSetRecyclerView() {
        increaseApiCallCountForFetch()

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchPetReq(FetchPetReqDto(null , null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            // fetch pet info (unsorted)
            val unsortedPetList: MutableList<PetListItem> = mutableListOf()
            response.body()?.petList?.map {
                val item = PetListItem(
                    it.id, it.photoUrl, null, it.name,
                    it.id == SessionManager.fetchLoggedInAccount(requireContext())?.representativePetId,
                    it.id == createUpdatePostViewModel.selectedPetId
                )
                unsortedPetList.add(item)
            }

            reorderPetList(unsortedPetList)
            fetchPetPhotos()

            decreaseApiCallCountForFetch()
        }, { decreaseApiCallCountForFetch() }, { decreaseApiCallCountForFetch() })
    }

    private fun reorderPetList(apiResponse: MutableList<PetListItem>) {
        // get saved pet list order
        val petListOrder = PetManagerFragment()
            .getPetListOrder(requireContext().getString(R.string.data_name_pet_list_id_order), requireContext())

        // sort by order
        for (id in petListOrder) {
            val item = apiResponse.find { it.petId == id }

            if (item!!.isSelected) {
                createUpdatePostViewModel.selectedPetIndex = createUpdatePostViewModel.petList.size
            }
            createUpdatePostViewModel.petList.add(item)
        }
    }

    private fun fetchPetPhotos() {
        val fetchedFlags = BooleanArray(createUpdatePostViewModel.petList.size) { false }

        for (i in 0 until createUpdatePostViewModel.petList.size) {
            // if no photo
            if (createUpdatePostViewModel.petList[i].petPhotoUrl == null) {
                fetchedFlags[i] = true

                continue
            }

            // fetch pet photo
            increaseApiCallCountForFetch()

            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .fetchPetPhotoReq(FetchPetPhotoReqDto(createUpdatePostViewModel.petList[i].petId))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                createUpdatePostViewModel.petList[i].petPhoto = BitmapFactory.decodeStream(response.body()!!.byteStream())

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
            createUpdatePostViewModel.isFetched = true
            petAdapter.updateDataSet(createUpdatePostViewModel.petList)

            hideLoadingScreen()
            restoreState()
        }
    }

    private fun showLoadingScreen() {
        createUpdatePostViewModel.isApiLoading = true

        binding.locationButton.visibility = View.INVISIBLE
        binding.disclosureButton.visibility = View.INVISIBLE
        binding.dividerD.visibility = View.INVISIBLE
        binding.attachmentButtonsLayout.visibility = View.INVISIBLE
        binding.createUpdatePostMainScrollView.visibility = View.INVISIBLE
        binding.postDataLoadingLayout.visibility = View.VISIBLE
        binding.postButton.isEnabled = false
    }

    private fun hideLoadingScreen() {
        createUpdatePostViewModel.isApiLoading = false

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
                this@CreateUpdatePostFragment.verifyAndEnableConfirmButton()
            }
            override fun updatePhotoUsage() {
                this@CreateUpdatePostFragment.updatePhotoUsage()
            }
            override fun updateVideoUsage() {
                this@CreateUpdatePostFragment.updateVideoUsage()
            }
        }

        // initialize RecyclerView (for pet)
        petAdapter = PetListAdapter(createUpdatePostViewModel, requireContext(), confirmButtonAndUsageInterface)
        binding.petRecyclerView.adapter = petAdapter
        binding.petRecyclerView.layoutManager = LinearLayoutManager(activity)
        (binding.petRecyclerView.layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL
        petAdapter.updateDataSet(createUpdatePostViewModel.petList)

        // initialize RecyclerView (for media)
        mediaAdapter = MediaListAdapter(createUpdatePostViewModel, requireContext(), binding, confirmButtonAndUsageInterface)
        binding.mediaRecyclerView.adapter = mediaAdapter
        binding.mediaRecyclerView.layoutManager = LinearLayoutManager(activity)
        (binding.mediaRecyclerView.layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL
        mediaAdapter.setResult(createUpdatePostViewModel.mediaList)

        // initialize RecyclerView (for general files)
        generalFilesAdapter = GeneralFileListAdapter(createUpdatePostViewModel, requireContext(), binding, confirmButtonAndUsageInterface)
        binding.generalRecyclerView.adapter = generalFilesAdapter
        binding.generalRecyclerView.layoutManager = LinearLayoutManager(activity)
        (binding.generalRecyclerView.layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.VERTICAL
        generalFilesAdapter.setResult(createUpdatePostViewModel.generalFileNameList)

        // initialize RecyclerView (for hashtags)
        hashtagAdapter = HashtagListAdapter(createUpdatePostViewModel, binding)
        binding.hashtagRecyclerView.adapter = hashtagAdapter
        binding.hashtagRecyclerView.layoutManager = FlexboxLayoutManager(activity)
        (binding.hashtagRecyclerView.layoutManager as FlexboxLayoutManager).flexWrap = FlexWrap.WRAP
        hashtagAdapter.setResult(createUpdatePostViewModel.hashtagList)
    }

    private fun updatePhotoUsage() {
        val uploadedPhotoCount = createUpdatePostViewModel.photoPathList.size
        if (uploadedPhotoCount + createUpdatePostViewModel.videoPathList.size == 0) {
            binding.mediaRecyclerView.visibility = View.GONE
        }
        else {
            binding.mediaRecyclerView.visibility = View.VISIBLE
        }
        val photoUsageText = "$uploadedPhotoCount/10"
        binding.photoUsage.text = photoUsageText
    }

    private fun updateVideoUsage() {
        val uploadedVideoCount = createUpdatePostViewModel.videoPathList.size
        if (uploadedVideoCount + createUpdatePostViewModel.photoPathList.size == 0) {
            binding.mediaRecyclerView.visibility = View.GONE
        }
        else {
            binding.mediaRecyclerView.visibility = View.VISIBLE
        }
        val videoUsageText = "$uploadedVideoCount/10"
        binding.videoUsage.text = videoUsageText
    }

    private fun updateGeneralUsage() {
        val uploadedCount = createUpdatePostViewModel.generalFileNameList.size

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
        if (createUpdatePostViewModel.hashtagEditText.isEmpty()) return

        if(!PatternRegex.checkHashtagRegex(createUpdatePostViewModel.hashtagEditText)) {
            Toast.makeText(context, context?.getText(R.string.hashtag_regex_exception_message), Toast.LENGTH_SHORT).show()
        }
        else if(createUpdatePostViewModel.hashtagList.size == 5) {
            Toast.makeText(context, context?.getText(R.string.hashtag_usage_full_message), Toast.LENGTH_SHORT).show()
        }
        else {
            // save hashtag
            val hashtag = binding.hashtagInputEditText.text.toString()
            createUpdatePostViewModel.hashtagList.add(hashtag)

            // update RecyclerView
            hashtagAdapter.notifyItemInserted(createUpdatePostViewModel.hashtagList.size)
            binding.hashtagRecyclerView.smoothScrollToPosition(createUpdatePostViewModel.hashtagList.size - 1)

            // reset hashtag EditText
            binding.hashtagInputEditText.setText("")
        }
    }


    private fun lockViews() {
        createUpdatePostViewModel.isApiLoading = true

        binding.postButton.isEnabled = false
        binding.postButton.text = ""
        binding.createUpdatePostProgressBar.visibility = View.VISIBLE

        binding.petRecyclerView.let {
            for(i in 0..petAdapter.itemCount) {
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
            for(i in 0..generalFilesAdapter.itemCount) {
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
        createUpdatePostViewModel.isApiLoading = false

        binding.postButton.isEnabled = true
        binding.postButton.text = requireContext().getText(R.string.confirm)
        binding.createUpdatePostProgressBar.visibility = View.GONE

        binding.petRecyclerView.let {
            for(i in 0..petAdapter.itemCount) {
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
            for(i in 0..generalFilesAdapter.itemCount) {
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
        return createUpdatePostViewModel.updatedPostPhotoData && createUpdatePostViewModel.updatedPostVideoData
                && createUpdatePostViewModel.updatedPostGeneralFileData && createUpdatePostViewModel.deletedPostPhotoData
                && createUpdatePostViewModel.deletedPostVideoData && createUpdatePostViewModel.updatedPostGeneralFileData
    }

    private fun verifyAndEnableConfirmButton() {
        binding.postButton.isEnabled = !(createUpdatePostViewModel.mediaList.size == 0
                && createUpdatePostViewModel.generalFileNameList.size == 0
                && createUpdatePostViewModel.postEditText.trim().isEmpty())
                && createUpdatePostViewModel.selectedPetId != null
                && binding.postDataLoadingLayout.visibility == View.GONE
                && !createUpdatePostViewModel.isApiLoading
    }

    private fun createPost() {
        lockViews()

        // get location data(if enabled)
        val latAndLong = if (!createUpdatePostViewModel.isUsingLocation) mutableListOf(0.0, 0.0) else Util.getGeolocation(requireContext())

        // 위치 정보 사용에 동의했지만, 권한이 없는 경우
        if(latAndLong[0] == (-1.0)){
            Permission.requestNotGrantedPermissions(requireContext(), Permission.requiredPermissionsForLocation)

            // 권한 요청이 비동기적이기 때문에, 권한 요청 이후에 CreatePost 버튼을 다시 눌러야한다.
            unlockViews()
            return
        }

        // create DTO
        val createPostReqDto = CreatePostReqDto(
            createUpdatePostViewModel.selectedPetId!!,
            createUpdatePostViewModel.postEditText,
            createUpdatePostViewModel.hashtagList,
            createUpdatePostViewModel.disclosure,
            latAndLong[0],
            latAndLong[1]
        )

        // set deleted to true (because there is nothing to delete)
        createUpdatePostViewModel.deletedPostPhotoData = true
        createUpdatePostViewModel.deletedPostVideoData = true
        createUpdatePostViewModel.deletedPostGeneralFileData = true

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .createPostReq(createPostReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            requireActivity().intent.putExtra("postId", response.body()!!.id)
            updatePostPhoto(response.body()!!.id)
            updatePostVideo(response.body()!!.id)
            updatePostGeneralFiles(response.body()!!.id)
        }, {
            unlockViews()
        }, {
            unlockViews()
        })
    }

    private fun updatePost() {
        lockViews()

        // get location data(if enabled)
        val latAndLong = if (!createUpdatePostViewModel.isUsingLocation) mutableListOf(0.0, 0.0) else Util.getGeolocation(requireContext())

        // 위치 정보 사용에 동의했지만, 권한이 없는 경우
        if(latAndLong[0] == (-1.0)){
            Permission.requestNotGrantedPermissions(requireContext(), Permission.requiredPermissionsForLocation)

            // 권한 요청이 비동기적이기 때문에, 권한 요청 이후에 CreatePost 버튼을 다시 눌러야한다.
            unlockViews()
            return
        }

        // create DTO
        val updatePostReqDto = UpdatePostReqDto(
            createUpdatePostViewModel.postId!!,
            createUpdatePostViewModel.selectedPetId!!,
            createUpdatePostViewModel.postEditText,
            createUpdatePostViewModel.hashtagList,
            createUpdatePostViewModel.disclosure,
            latAndLong[0],
            latAndLong[1]
        )

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .updatePostReq(updatePostReqDto)
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            // update photos
            if(createUpdatePostViewModel.photoPathList.size == 0) {
                // 기존에 사진이 0개였다면 FileType.IMAGE_FILE에 대해 DeletePostFile를 호출하지 않는다
                if(requireActivity().intent.getIntExtra("originalImageCount", 0) > 0){
                    createUpdatePostViewModel.updatedPostPhotoData = true
                    deletePostFile(createUpdatePostViewModel.postId!!, FileType.IMAGE_FILE)
                }else{
                    createUpdatePostViewModel.updatedPostPhotoData = true
                    createUpdatePostViewModel.deletedPostPhotoData = true

                    if (isApiLoadComplete()) {
                        passDataToCommunity()
                        closeAfterSuccess()
                    }
                }
            } else {
                createUpdatePostViewModel.deletedPostPhotoData = true
                updatePostPhoto(createUpdatePostViewModel.postId!!)
            }

            // update videos
            if(createUpdatePostViewModel.videoPathList.size == 0) {
                // 기존에 동영상이 0개였다면 FileType.VIDEO_FILE에 대해 DeletePostFile를 호출하지 않는다
                if(requireActivity().intent.getIntExtra("originalVideoCount", 0) > 0){
                    createUpdatePostViewModel.updatedPostVideoData = true
                    deletePostFile(createUpdatePostViewModel.postId!!, FileType.VIDEO_FILE)
                }else{
                    createUpdatePostViewModel.updatedPostVideoData = true
                    createUpdatePostViewModel.deletedPostVideoData = true

                    if (isApiLoadComplete()) {
                        passDataToCommunity()
                        closeAfterSuccess()
                    }
                }
            } else {
                createUpdatePostViewModel.deletedPostVideoData = true
                updatePostVideo(createUpdatePostViewModel.postId!!)
            }

            // update general files
            if(createUpdatePostViewModel.generalFilePathList.size == 0) {
                // 기존에 General Files가 0개였다면 FileType.GENERAL_FILE에 대해 DeletePostFile를 호출하지 않는다
                if(requireActivity().intent.getIntExtra("originalGeneralFilesCount", 0) > 0){
                    createUpdatePostViewModel.updatedPostGeneralFileData = true
                    deletePostFile(createUpdatePostViewModel.postId!!, FileType.GENERAL_FILE)
                }else{
                    createUpdatePostViewModel.updatedPostGeneralFileData = true
                    createUpdatePostViewModel.deletedPostGeneralFileData = true

                    if (isApiLoadComplete()) {
                        passDataToCommunity()
                        closeAfterSuccess()
                    }
                }
            } else {
                createUpdatePostViewModel.deletedPostGeneralFileData = true
                updatePostGeneralFiles(createUpdatePostViewModel.postId!!)
            }
        }, {
            unlockViews()
        }, {
            unlockViews()
        })
    }

    private fun deletePostFile(id: Long, fileType: String) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .deletePostFileReq(DeletePostFileReqDto(id, fileType))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            if (fileType == FileType.IMAGE_FILE) {
                createUpdatePostViewModel.deletedPostPhotoData = true
            }
            if (fileType == FileType.GENERAL_FILE) {
                createUpdatePostViewModel.deletedPostGeneralFileData = true
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
        if(createUpdatePostViewModel.photoPathList.size == 0) {
            createUpdatePostViewModel.updatedPostPhotoData = true

            if (isApiLoadComplete()) {
                passDataToCommunity()
                closeAfterSuccess()
            }
        } else {
            // create file list
            val fileList: ArrayList<MultipartBody.Part> = ArrayList()
            for(i in 0 until createUpdatePostViewModel.photoPathList.size) {
                val fileName = "file_$i" + createUpdatePostViewModel.photoPathList[i]
                    .substring(createUpdatePostViewModel.photoPathList[i].lastIndexOf("."))

                fileList.add(MultipartBody.Part.createFormData("fileList", fileName,
                    RequestBody.create(MediaType.parse("multipart/form-data"), File(createUpdatePostViewModel.photoPathList[i]))))
            }

            // API call
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .updatePostFileReq(id, fileList, FileType.IMAGE_FILE)
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
                createUpdatePostViewModel.updatedPostPhotoData = true

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
        if(createUpdatePostViewModel.videoPathList.size == 0) {
            createUpdatePostViewModel.updatedPostVideoData = true

            if (isApiLoadComplete()) {
                passDataToCommunity()
                closeAfterSuccess()
            }
        } else {
            // create file list
            val fileList: ArrayList<MultipartBody.Part> = ArrayList()
            for(i in 0 until createUpdatePostViewModel.videoPathList.size) {
                val fileName = "file_$i" + createUpdatePostViewModel.videoPathList[i]
                    .substring(createUpdatePostViewModel.videoPathList[i].lastIndexOf("."))

                fileList.add(MultipartBody.Part.createFormData("fileList", fileName,
                    RequestBody.create(MediaType.parse("multipart/form-data"), File(createUpdatePostViewModel.videoPathList[i]))))
            }

            // API call
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .updatePostFileReq(id, fileList, FileType.VIDEO_FILE)
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
                createUpdatePostViewModel.updatedPostVideoData = true

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

    private fun updatePostGeneralFiles(id: Long) {
        // exception (no general files)
        if (createUpdatePostViewModel.generalFilePathList.size == 0) {
            createUpdatePostViewModel.updatedPostGeneralFileData = true

            if (isApiLoadComplete()) {
                passDataToCommunity()
                closeAfterSuccess()
            }
        } else {
            // create file list
            val fileList: ArrayList<MultipartBody.Part> = ArrayList()
            for(i in 0 until createUpdatePostViewModel.generalFilePathList.size) {
                val fileName = createUpdatePostViewModel.generalFileNameList[i]

                fileList.add(MultipartBody.Part.createFormData("fileList", fileName,
                    RequestBody.create(MediaType.parse("multipart/form-data"), File(createUpdatePostViewModel.generalFilePathList[i]))))
            }

            // API call
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .updatePostFileReq(id, fileList, FileType.GENERAL_FILE)
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
                createUpdatePostViewModel.updatedPostGeneralFileData = true

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
        val intent = Intent()
        intent.putExtra("postId", requireActivity().intent.getLongExtra("postId", -1))
        intent.putExtra("position", requireActivity().intent.getIntExtra("position", -1))
        requireActivity().setResult(Activity.RESULT_OK, intent)
    }

    // close after success
    private fun closeAfterSuccess() {
        unlockViews()

        // delete copied files(if any)
        if(isRemoving || requireActivity().isFinishing) {
            Util.deleteCopiedFiles(requireContext(), CREATE_UPDATE_POST_DIRECTORY)
        }

        // show message + return to previous activity
        if(requireActivity().intent.getStringExtra("fragmentType") == "create_post") {
            Toast.makeText(context, context?.getText(R.string.create_post_successful), Toast.LENGTH_LONG).show()
        }
        else {
            Toast.makeText(context, context?.getText(R.string.update_post_successful), Toast.LENGTH_LONG).show()
        }

        activity?.finish()
    }

    // for view restore
    private fun restoreState() {
        // restore usages
        updatePhotoUsage()
        updateVideoUsage()
        updateGeneralUsage()

        // restore location button
        if (createUpdatePostViewModel.isUsingLocation) {
            binding.locationButton.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_location_on_30))
        } else {
            binding.locationButton.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_location_off_30))
        }

        // restore disclosure button
        when (createUpdatePostViewModel.disclosure) {
            DISCLOSURE_PUBLIC -> {
                binding.disclosureButton.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_public_30))
            }
            DISCLOSURE_PRIVATE -> {
                binding.disclosureButton.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_lock_30))
            }
            DISCLOSURE_FRIEND -> {
                binding.disclosureButton.setImageDrawable(requireContext().getDrawable(R.drawable.ic_baseline_group_30))
            }
        }

        // restore hashtag layout
        binding.hashtagInputEditText.setText(createUpdatePostViewModel.hashtagEditText)
        if (createUpdatePostViewModel.hashtagEditText.isNotEmpty()) {
            binding.hashtagClearButton.visibility = View.VISIBLE
        }

        // restore post EditText
        binding.postEditText.setText(createUpdatePostViewModel.postEditText)

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
                    if (Util.isExceedsFileSizeLimit(requireContext(), data, FileType.FILE_SIZE_LIMIT_PHOTO)) {
                        Toast.makeText(context, context?.getText(R.string.file_size_limit_exception_message_20MB), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // get file name
                    val fileName = Util.getSelectedFileName(requireContext(), data.data!!)

                    // duplicate file exception
                    for (path in createUpdatePostViewModel.photoPathList) {
                        if (fileName == path.substring(path.lastIndexOf("/") + 1)) {
                            Toast.makeText(requireContext(), requireContext()
                                .getText(R.string.duplicate_file_exception_message), Toast.LENGTH_SHORT).show()
                            return
                        }
                    }

                    // copy selected photo and get real path
                    val postPhotoPathValue = ServerUtil.createCopyAndReturnRealPathLocal(requireActivity(),
                        data.data!!, CREATE_UPDATE_POST_DIRECTORY, fileName)

                    // no extension exception
                    if (postPhotoPathValue.isEmpty()) {
                        Toast.makeText(context, context?.getText(R.string.photo_file_type_exception_message), Toast.LENGTH_LONG).show()
                        return
                    }

                    // file type exception -> delete copied file + show Toast message
                    if (!Util.isUrlPhoto(postPhotoPathValue)) {
                        Toast.makeText(context, context?.getText(R.string.photo_file_type_exception_message), Toast.LENGTH_LONG).show()
                        File(postPhotoPathValue).delete()
                        return
                    }

                    // add path to list
                    createUpdatePostViewModel.photoPathList.add(postPhotoPathValue)

                    // save media list item
                    createUpdatePostViewModel.mediaList
                        .add(MediaListItem(false, createUpdatePostViewModel.photoPathList.size - 1))

                    verifyAndEnableConfirmButton()

                    // update RecyclerView
                    mediaAdapter.notifyItemInserted(createUpdatePostViewModel.mediaList.size)
                    binding.mediaRecyclerView.smoothScrollToPosition(createUpdatePostViewModel.mediaList.size - 1)

                    updatePhotoUsage()
                } else {
                    Toast.makeText(context, context?.getText(R.string.file_null_exception_message), Toast.LENGTH_LONG).show()
                }
            }
            PICK_VIDEO -> {
                if(data != null) {
                    // check file size limit
                    if (Util.isExceedsFileSizeLimit(requireContext(), data, FileType.FILE_SIZE_LIMIT_VIDEO)) {
                        Toast.makeText(context, context?.getText(R.string.file_size_limit_exception_message_100MB), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // get file name
                    val fileName = Util.getSelectedFileName(requireContext(), data.data!!)

                    // duplicate file exception
                    for (path in createUpdatePostViewModel.videoPathList) {
                        if (fileName == path.substring(path.lastIndexOf("/") + 1)) {
                            Toast.makeText(requireContext(), requireContext()
                                .getText(R.string.duplicate_file_exception_message), Toast.LENGTH_SHORT).show()
                            return
                        }
                    }

                    // copy selected video and get real path
                    val postVideoPathValue = ServerUtil.createCopyAndReturnRealPathLocal(requireActivity(),
                        data.data!!, CREATE_UPDATE_POST_DIRECTORY, fileName)

                    // no extension exception
                    if (postVideoPathValue.isEmpty()) {
                        Toast.makeText(context, context?.getText(R.string.video_file_type_exception_message), Toast.LENGTH_LONG).show()
                        return
                    }

                    // file type exception -> delete copied file + show Toast message
                    if (!Util.isUrlVideo(postVideoPathValue)) {
                        Toast.makeText(context, context?.getText(R.string.video_file_type_exception_message), Toast.LENGTH_LONG).show()
                        File(postVideoPathValue).delete()
                        return
                    }

                    // add path to list
                    createUpdatePostViewModel.videoPathList.add(postVideoPathValue)

                    // save media list item
                    createUpdatePostViewModel.mediaList
                        .add(MediaListItem(true, createUpdatePostViewModel.videoPathList.size - 1))

                    verifyAndEnableConfirmButton()

                    // update RecyclerView
                    mediaAdapter.notifyItemInserted(createUpdatePostViewModel.mediaList.size)
                    binding.mediaRecyclerView.smoothScrollToPosition(createUpdatePostViewModel.mediaList.size - 1)

                    updateVideoUsage()
                } else {
                    Toast.makeText(context, context?.getText(R.string.file_null_exception_message), Toast.LENGTH_LONG).show()
                }
            }
            PICK_GENERAL_FILE -> {
                if(data != null) {
                    // check file size limit
                    if (Util.isExceedsFileSizeLimit(requireContext(), data, FileType.FILE_SIZE_LIMIT_GENERAL)) {
                        Toast.makeText(context, context?.getText(R.string.file_size_limit_exception_message_100MB), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // get file name
                    val fileName = Util.getSelectedFileName(requireContext(), data.data!!)

                    // duplicate file exception
                    if (fileName in createUpdatePostViewModel.generalFileNameList) {
                        Toast.makeText(requireContext(), requireContext()
                            .getText(R.string.duplicate_file_exception_message), Toast.LENGTH_SHORT).show()
                        return
                    }

                    // copy selected general file and get real path
                    val postGeneralFilePathValue = ServerUtil.createCopyAndReturnRealPathLocal(requireActivity(),
                        data.data!!, CREATE_UPDATE_POST_DIRECTORY,fileName)

                    // file type exception -> delete copied file + show Toast message
                    if (!Util.isUrlGeneralFile(postGeneralFilePathValue)) {
                        Toast.makeText(context, context?.getText(R.string.general_file_type_exception_message), Toast.LENGTH_LONG).show()
                        File(postGeneralFilePathValue).delete()
                        return
                    }

                    // add path to list
                    createUpdatePostViewModel.generalFilePathList.add(postGeneralFilePathValue)

                    // save file name
                    val generalFileName = createUpdatePostViewModel.generalFilePathList.last()
                        .substring(createUpdatePostViewModel.generalFilePathList.last().lastIndexOf("/") + 1)
                    createUpdatePostViewModel.generalFileNameList.add(generalFileName)

                    verifyAndEnableConfirmButton()

                    // update RecyclerView
                    generalFilesAdapter.notifyItemInserted(createUpdatePostViewModel.generalFileNameList.size)
                    binding.generalRecyclerView.smoothScrollToPosition(createUpdatePostViewModel.generalFileNameList.size - 1)

                    updateGeneralUsage()
                } else {
                    Toast.makeText(context, context?.getText(R.string.file_null_exception_message), Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                Toast.makeText(context, context?.getText(R.string.file_type_exception_message), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        isViewDestroyed = true

        // delete copied files(if any)
        if(isRemoving || requireActivity().isFinishing) {
            Util.deleteCopiedFiles(requireContext(), CREATE_UPDATE_POST_DIRECTORY)
        }
    }
}

interface ConfirmButtonAndUsageInterface {
    fun verifyAndEnableConfirmButton()
    fun updatePhotoUsage()
    fun updateVideoUsage()
}