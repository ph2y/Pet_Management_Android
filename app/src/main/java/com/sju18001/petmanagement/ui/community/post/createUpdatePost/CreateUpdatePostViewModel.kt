package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class CreateUpdatePostViewModel(private val handle: SavedStateHandle) : ViewModel() {
    var activityType = MutableLiveData<String?>(null)
    var isApiLoading = MutableLiveData(false)

    fun isActivityTypeCreatePost() = activityType.value == "create_post"

    /*
     * 글 수정을 위해 액티비티에 진입했을 때, 여러 곳에서 진행되는 api call이 모두 끝나면 글 정보 패치가 완료된 것이며,
     * 이것을 감지하기 위해 사용하는 변수입니다.
     * 정보 로드에 쓰이는 api call을 시작할 때마다 1을 증가시키며 api call이 끝날 때마다 반대로 1씩 감소시킵니다.
     * 예를 들어, 이 변수의 값이 3이라면 3개의 API Call이 진행중인 것입니다.
     */
    var apiCallCountForFetch = 0

    /*
     * create 및 update 시 각각 api 호출을 3, 6번을 하며, 모든 호출이 끝나는 시점에 액티비티를 종료합니다.
     * 이를 감지하기 위한 함수로서, create, update를 할 때 각각 3, 6으로 초기화한 뒤, api 호출이 끝날 때마다
     * 1씩 감소 시킵니다. 이 값이 0일 경우에 액티비티를 종료하므로 뷰모델에서는 1로 초기화하였습니다.
     */
    var remainingApiCallCountForCreateUpdate = MutableLiveData(1)
    fun decreaseRemainingApiCallCountForCreateUpdate(by: Int){
        remainingApiCallCountForCreateUpdate.value = remainingApiCallCountForCreateUpdate.value!! - by
    }

    var originalImageCount = 0
    var originalVideoCount = 0
    var originalGeneralFileCount = 0

    var postId = -1L
    var position = -1

    var selectedPetId = MutableLiveData<Long?>(null)
    var isUsingLocation = MutableLiveData(true)
    var disclosure = MutableLiveData(CreateUpdatePostActivity.DISCLOSURE_PUBLIC)

    var contents = MutableLiveData("")
    var hashtagEditText = MutableLiveData("")

    var selectedPetIndex = -1

    var photoPathList: MutableLiveData<MutableList<String>> = MutableLiveData(mutableListOf())
    var videoPathList: MutableLiveData<MutableList<String>> = MutableLiveData(mutableListOf())
    var generalFilePathList: MutableLiveData<MutableList<String>> = MutableLiveData(mutableListOf())
    var generalFileNameList = MutableLiveData<MutableList<String>>(mutableListOf())

    // a.value = a.value: add, remove 등의 함수는, 호출해도 observe되지 않기 때문에 대입 수행해야 한다.
    fun addPhotoPath(photoPath: String) {
        photoPathList.value!!.add(photoPath)
        photoPathList.value = photoPathList.value
    }

    fun addVideoPath(videoPath: String) {
        videoPathList.value!!.add(videoPath)
        videoPathList.value = videoPathList.value
    }

    fun addGeneralFilePath(generalFilePath: String) {
        generalFilePathList.value!!.add(generalFilePath)
        generalFilePathList.value = generalFilePathList.value
    }

    fun addGeneralFileName(generalFileName: String) {
        generalFileNameList.value!!.add(generalFileName)
        generalFileNameList.value = generalFileNameList.value
    }

    fun removePhotoPath(path: String) {
        photoPathList.value!!.remove(path)
        photoPathList.value = photoPathList.value
    }

    fun removeVideoPath(path: String) {
        videoPathList.value!!.remove(path)
        videoPathList.value = videoPathList.value
    }

    fun removeGeneralFilePathAt(at: Int) {
        generalFilePathList.value!!.removeAt(at)
        generalFilePathList.value = generalFilePathList.value
    }

    fun removeGeneralFileNameAt(at: Int) {
        generalFileNameList.value!!.removeAt(at)
        generalFileNameList.value = generalFileNameList.value
    }
}