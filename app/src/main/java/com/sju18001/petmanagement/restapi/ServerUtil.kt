package com.sju18001.petmanagement.restapi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.fcm.FcmUtil
import com.sju18001.petmanagement.ui.myPet.petScheduleManager.PetScheduleNotification
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class ServerUtil {
    companion object{
        const val WRITE_REQUEST_CODE = 0

        fun <T> enqueueApiCall(
            call: Call<T>,
            getIsViewDestroyed: ()-> Boolean,
            context: Context,
            onSuccessful: (Response<T>)->Unit,
            onNotSuccessful: (Response<T>)->Unit,
            onFailure: (t: Throwable)->Unit
        ){
            call.enqueue(object: Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    if(getIsViewDestroyed()) return

                    if(response.isSuccessful){
                        onSuccessful.invoke(response)
                    }else{
                        onNotSuccessful.invoke(response)
                        Util.showToastAndLogForNotSuccessfulResponse(context, response.errorBody())
                    }
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    if(getIsViewDestroyed()) return

                    onFailure.invoke(t)
                    Util.showToastAndLog(context, t.message.toString())
                }
            })
        }

        /** TODO: remove this function. (Find the cause of the error and fix it.) */
        fun <T> enqueueApiCallWithoutErrorMessage(
            call: Call<T>,
            getIsViewDestroyed: ()-> Boolean,
            context: Context,
            onResponseOrFailure: ()->Unit
        ){
            call.enqueue(object: Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    if(getIsViewDestroyed()) return

                    onResponseOrFailure.invoke()

                    if (!response.isSuccessful && response.errorBody() != null) {
                        Util.getMessageFromErrorBody(response.errorBody()!!)
                            ?.let { Util.log(context, it) }
                    }
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    if(getIsViewDestroyed()) return

                    onResponseOrFailure.invoke()

                    Util.log(context, t.message.toString())
                }
            })
        }

        fun getEmptyBody(): RequestBody{
            return RequestBody.create(MediaType.parse("application/json; charset=UTF-8"), "{}")
        }

        fun createCopyAndReturnRealPathLocal(context: Context, uri: Uri, directory: String, fileName: String): String {
            val mimeTypeMap = MimeTypeMap.getSingleton()
            if (mimeTypeMap.getExtensionFromMimeType(context.contentResolver.getType(uri)) == null) {
                return ""
            }

            val baseDirectory = context.getExternalFilesDir(null).toString() + File.separator + directory
            if(!File(baseDirectory).exists()) { File(baseDirectory).mkdir() }

            val newFilePath = baseDirectory + File.separator + fileName
            val newFile = File(newFilePath)

            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(newFile)
            val buffer = ByteArray(1024)

            var len: Int
            while (inputStream!!.read(buffer).also { len = it } > 0) outputStream.write(buffer, 0, len)
            outputStream.close()
            inputStream.close()

            return newFile.absolutePath
        }

        fun createCopyAndReturnContentUri(context: Context, byteArray: ByteArray, extension: String, directory: String): Uri {
            val baseDirectory = context.getExternalFilesDir(null).toString() + File.separator + directory
            if(!File(baseDirectory).exists()) { File(baseDirectory).mkdir() }

            val newFilePath = baseDirectory + File.separator + System.currentTimeMillis() + '.' + extension
            val newFile = writeAndGetFile(byteArray, newFilePath)

            return FileProvider.getUriForFile(context, "${context.applicationInfo.packageName}.fileprovider", newFile)
        }

        fun createCopyAndReturnRealPathServer(context: Context, byteArray: ByteArray, extension: String, directory: String): String {
            val baseDirectory = context.getExternalFilesDir(null).toString() + File.separator + directory
            if(!File(baseDirectory).exists()) { File(baseDirectory).mkdir() }

            val newFilePath = baseDirectory + File.separator + System.currentTimeMillis() + '.' + extension
            val newFile = writeAndGetFile(byteArray, newFilePath)

            return newFile.absolutePath
        }

        fun writeAndGetFile(byteArray: ByteArray, filePath: String): File{
            val newFile = File(filePath)

            val outputStream = FileOutputStream(newFile)
            outputStream.write(byteArray)
            outputStream.close()

            return newFile
        }

        fun getUriFromUser(activity: Activity, fileName: String) {
            // get MIME type
            val mimeTypeMap = MimeTypeMap.getSingleton()
            val mimeType = mimeTypeMap.getMimeTypeFromExtension(fileName.split('.').last())

            // get Uri from user
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = mimeType
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            activity.startActivityForResult(intent, WRITE_REQUEST_CODE)
        }

        fun writeFileToUri(context: Context, downloadedFilePath: String, uri: Uri) {
            context.contentResolver.openFileDescriptor(uri, "w").use { pfd->
                FileOutputStream(pfd!!.fileDescriptor).use { fos ->
                    val inputStream = context.contentResolver.openInputStream(Uri.fromFile(File(downloadedFilePath)))
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (inputStream!!.read(buffer).also { len = it } > 0) fos.write(buffer, 0, len)
                    fos.close()
                    inputStream.close()
                }
            }
        }

        fun doLogout(context: Context) {
            // pet_management.account의 fcm registration token을 지우는 게 아니라, fcm 서버에서의 token을 지웁니다.
            // 이는 로그인 실패 시 pet_management로 api call을 보낼 수 없기 때문입니다. 따라서 자발적으로 로그아웃할 경우와
            // 로그인을 실패하여 로그아웃될 경우를 모두 커버할 수 있게끔, fcm 서버의 token을 제거하는 것입니다.
            FcmUtil.deleteFirebaseMessagingToken()

            PetScheduleNotification.cancelAll(context)

            SessionManager.removeUserToken(context)
            SessionManager.removeLoggedInAccount(context)
        }
    }
}