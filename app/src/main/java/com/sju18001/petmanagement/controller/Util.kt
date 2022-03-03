package com.sju18001.petmanagement.controller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.restapi.dao.Place
import com.sju18001.petmanagement.restapi.global.FileMetaData
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.time.*
import java.util.*
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin


class Util {
    companion object{
        const val LOG_FILE_NAME = "client_log_file"

        fun convertDpToPixel(dp: Int): Int{
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), Resources.getSystem().displayMetrics).toInt()
        }

        fun convertPixelToDp(context: Context, px: Int): Int{
            return px / (context.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
        }

        fun hideKeyboard(activity: Activity){
            activity.currentFocus?.let{
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                imm.hideSoftInputFromWindow(it.windowToken, 0)
                it.clearFocus()
            }
        }

        fun showKeyboard(activity: Activity, view: EditText){
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            view.requestFocus()
            imm.showSoftInput(view, 0)

            // Set cursor at the end of view
            view.setSelection(view.text.toString().length)
        }

        fun setupViewsForHideKeyboard(activity: Activity, view: View) {
            // Set up touch listener for non-text box views to hide keyboard
            if(view !is EditText) {
                view.setOnTouchListener { _, _ ->
                    hideKeyboard(activity)
                    false
                }
            }

            // If a layout container, iterate over children and seed recursion
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    val innerView = view.getChildAt(i)
                    setupViewsForHideKeyboard(activity, innerView)
                }
            }
        }

        // * Location Information
        fun openWebPage(activity: Activity, url: String){
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }

            activity.startActivity(intent)
        }

        fun doCall(activity: Activity, phone: String){
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }

            activity.startActivity(intent)
        }

        fun insertContactsContract(activity: Activity, document: Place) {
            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE

                putExtra(ContactsContract.Intents.Insert.NAME, document.name)
                putExtra(ContactsContract.Intents.Insert.PHONE, document.phone)
            }

            activity.startActivity(intent)
        }

        fun doCopy(context: Context, str: String){
            // 복사
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData: ClipData = ClipData.newPlainText("A phone number", str)

            clipboard.setPrimaryClip(clipData)

            // 토스트 메시지
            Toast.makeText(context, "클립보드에 복사되었습니다.", Toast.LENGTH_LONG).show()
        }

        fun shareText(activity: Activity, value: String){
            val sendIntent: Intent = Intent().apply{
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, value)
                type= "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            activity.startActivity(shareIntent)
        }

        fun getMessageFromErrorBody(errorBody: ResponseBody): String?{
            var res:String? = null
            try {
                val metadata = JSONObject(errorBody.string().trim()).getString("_metadata")
                res = JSONObject(metadata).getString("message")
            }catch(e: Exception) {
                e.printStackTrace()
            }

            return res
        }

        fun isUrlPhoto(url: String): Boolean{
            return url.endsWith(".jpg") || url.endsWith(".png") || url.endsWith(".jpeg") ||
                    url.endsWith(".gif") || url.endsWith(".webp")
        }

        fun isUrlVideo(url: String): Boolean{
            return url.endsWith(".mp4") || url.endsWith(".webm")
        }

        fun isUrlGeneralFile(url: String): Boolean{
            return url.endsWith(".doc") || url.endsWith(".docx") || url.endsWith(".hwp") || url.endsWith(".pdf") ||
                    url.endsWith(".txt") || url.endsWith(".ppt") || url.endsWith(".pptx") || url.endsWith(".psd") ||
                    url.endsWith(".ai") || url.endsWith(".xls") || url.endsWith(".xlsx") || url.endsWith(".rar") ||
                    url.endsWith(".tar") || url.endsWith(".zip") || url.endsWith(".exe") || url.endsWith(".apk")
        }

        fun getScreenWidthInPixel(activity: Activity) : Int{
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = activity.windowManager.currentWindowMetrics
                val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                return windowMetrics.bounds.width() - insets.left - insets.right
            } else {
                val displayMetrics = DisplayMetrics()
                activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
                displayMetrics.widthPixels
            }
        }

        fun getScreenHeightInPixel(activity: Activity) : Int{
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = activity.windowManager.currentWindowMetrics
                val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                windowMetrics.bounds.height() - insets.top - insets.bottom
            } else {
                val displayMetrics = DisplayMetrics()
                activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
                displayMetrics.heightPixels
            }
        }

        fun deleteCopiedFiles(context: Context, directory: String) {
            val dir = File(context.getExternalFilesDir(null).toString() + File.separator + directory)
            dir.deleteRecursively()
        }

        fun getSelectedFileName(context: Context, uri: Uri): String {
            var fileName = ""
            context.contentResolver.query(uri, null, null, null, null).use {
                if (it != null && it.moveToFirst()) {
                    var result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    if (result == null) {
                        result = uri.path
                        val cut = result.lastIndexOf('/')
                        if (cut != -1) {
                            result = result.substring(cut + 1)
                        }
                    }
                    fileName = result
                }
            }

            return fileName
        }

        fun getTemporaryFilesSize(context: Context): Long {
            // set File path
            val file = File(context.getExternalFilesDir(null).toString())

            // no such file/directory exception
            if (!file.exists()) {
                return 0
            }
            // if file
            if (!file.isDirectory) {
                return file.length()
            }

            // if directory
            val dirs: MutableList<File> = LinkedList()
            dirs.add(file)

            var result: Long = 0
            while (dirs.isNotEmpty()) {
                val dir = dirs.removeAt(0)
                if (!dir.exists()) {
                    continue
                }
                val listFiles = dir.listFiles()
                if (listFiles.isNullOrEmpty()) {
                    continue
                }
                for (child in listFiles) {
                    result += child.length()
                    if (child.isDirectory) dirs.add(child)
                }
            }

            return result
        }

        fun saveByteArrayToSharedPreferences(context: Context, prefName: String, dataName: String, data: ByteArray?) {
            val preferences: SharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

            if (data == null) {
                preferences.edit().putString(dataName, "").apply()
            }
            else {
                preferences.edit().putString(dataName,
                    android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)).apply()
            }
        }

        fun getByteArrayFromSharedPreferences(context: Context, prefName: String, dataName: String): ByteArray? {
            val preferences: SharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val data = preferences.getString(dataName, null)

            return if (data.isNullOrEmpty()) {
                null
            } else {
                android.util.Base64.decode(data, android.util.Base64.NO_WRAP)
            }
        }

        fun getArrayFromMediaAttachments(mediaAttachments: String?): Array<FileMetaData> {
            return if (mediaAttachments != null) Gson().fromJson(mediaAttachments, Array<FileMetaData>::class.java) else arrayOf()
        }

        fun showToastAndLogForFailedResponse(context: Context, errorBody: ResponseBody?) {
            if(errorBody == null) return

            getMessageFromErrorBody(errorBody)?.let{
                showToastAndLog(context, it)
            }
        }

        fun showToastAndLog(context: Context, errorMessage: String){
            // get server error message + handle exceptions
            val toastMessage: String = when(errorMessage) {
                // Account
                "Email already exists" -> {
                    context.getString(R.string.email_message_overlap)
                }
                "Username already exists" -> {
                    context.getString(R.string.id_message_overlap)
                }
                "Phone number already exists" -> {
                    context.getString(R.string.phone_message_overlap)
                }
                "Verification code send failed" -> {
                    context.getString(R.string.verification_code_send_fail_exception_message)
                }
                "Wrong password" -> {
                    context.getString(R.string.wrong_password_exception_message)
                }
                // Pet
                "Pet entity does not exist" -> {
                    context.getString(R.string.pet_not_exist_exception_message)
                }
                "PetSchedule entity does not exist" -> {
                    context.getString(R.string.petSchedule_not_exist_exception_message)
                }
                // Follow
                "Account not exists" -> {
                    context.getString(R.string.account_does_not_exist_exception_message)
                }
                "Fetched self" -> {
                    context.getString(R.string.fetched_self_exception_message)
                }
                "Self follow is not allowed" -> {
                    context.getString(R.string.fetched_self_exception_message)
                }
                // Post
                "Post entity does not exist" -> {
                    context.getString(R.string.post_not_exist_exception_message)
                }
                "Post attachment file type is invalid" -> {
                    context.getString(R.string.post_attachment_type_type_invalid_exception_message)
                }
                "Comment entity does not exist" -> {
                    context.getString(R.string.comment_not_exist_exception_message)
                }
                "Like entity does not exist" -> {
                    context.getString(R.string.like_not_exist_exception_message)
                }
                "Already liked" -> {
                    context.getString(R.string.like_duplicate_exception_message)
                }
                else -> {
                    context.getString(R.string.default_error_message)
                }
            }

            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            log(context, errorMessage)
            Log.d("error", errorMessage)
        }

        fun log(context: Context, text: String) {
            // set File
            val logFilePath =
                context.getExternalFilesDir(null).toString() + File.separator + LOG_FILE_NAME
            val logFile = File(logFilePath)

            if (!logFile.exists()) {
                try {
                    logFile.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            try {
                val calendar: Calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val dateString: String = dateFormat.format(calendar.time)


                val buffer = BufferedWriter(FileWriter(logFile, true))
                buffer.append("[$dateString]$text")
                buffer.newLine()
                buffer.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun getAgeFromBirth(birth: String?): String {
            return Period.between(LocalDate.parse(birth), LocalDate.now()).years.toString()
        }

        fun getGenderSymbol(gender: Boolean, context: Context): String {
            return if(gender) context.getString(R.string.pet_gender_female_symbol)
            else context.getString(R.string.pet_gender_male_symbol)
        }

        fun getImageRotation(path: String): Float {
            val exifInterface = ExifInterface(path)
            val exifRotation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            return when (exifRotation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }

        fun isExceedsFileSizeLimit(context: Context, intent: Intent, fileSizeLimit: Long): Boolean {
            intent.data?.let { returnUri ->
                context.contentResolver.query(returnUri, null, null, null, null)
            }?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()

                val fileSize = cursor.getLong(sizeIndex)
                if (fileSize >= fileSizeLimit) {
                    return true
                }
            }

            return false
        }

        fun getPetNamesFromPetIdList(petNameForId: HashMap<Long, String>, petIdList: String?): String {
            if(petIdList.isNullOrEmpty()) return ""

            var petNames = ""
            val petIdListOfString: List<String> = petIdList.replace(" ", "").split(",")

            // "이름, 이름, " 식으로 String 추가함
            for(id in petIdListOfString) {
                id.toLongOrNull()?.let{
                    petNames += petNameForId[it] ?: id
                    petNames += ", "
                }
            }

            // ", " 제거
            if(petNames.length >= 2){
                petNames = petNames.dropLast(2)
            }

            return petNames
        }

        fun getBitmapFromInputStream(photoByte: InputStream): Bitmap {
            val byteArray = photoByte.readBytes()
            return getBitmapFromByteArray(byteArray)
        }

        fun getBitmapFromByteArray(byteArray: ByteArray): Bitmap {
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }

        fun setViewMore(contentsTextView: TextView, viewMoreTextView: TextView, maxLine: Int){
            // 더보기를 없는 것으로 초기화를 한다. 조건에 맞을 시 VISIBLE
            contentsTextView.maxLines = maxLine
            viewMoreTextView.visibility = View.GONE

            // getEllipsisCount()을 통한 더보기 표시 및 구현
            contentsTextView.post {
                val lineCount = contentsTextView.layout.lineCount
                if (lineCount > 0) {
                    if (contentsTextView.layout.getEllipsisCount(lineCount - 1) > 0) {
                        // 더보기 표시
                        viewMoreTextView.visibility = View.VISIBLE

                        // 더보기 클릭 이벤트
                        viewMoreTextView.setOnClickListener {
                            contentsTextView.maxLines = Int.MAX_VALUE
                            viewMoreTextView.visibility = View.GONE
                        }
                    }
                }
            }
        }

        fun getTimestampForDisplay(timestamp: String): String{
            val secondDiff: Long = getSecondDifferenceInLocalDateTime(LocalDateTime.parse(timestamp))
            val minuteDiff: Long = secondDiff / 60
            val hourDiff: Long = minuteDiff / 60
            val dateDiff: Long = hourDiff / 24
            val monthDiff: Long = dateDiff / 30
            val yearDiff: Long = monthDiff / 12

            return when {
                yearDiff > 0 -> "${yearDiff}년"
                monthDiff > 0 -> "${monthDiff}달"
                dateDiff > 0 -> "${dateDiff}일"
                hourDiff > 0 -> "${hourDiff}시간"
                minuteDiff > 0 -> "${minuteDiff}분"
                else -> "${secondDiff}초"
            }
        }

        private fun getSecondDifferenceInLocalDateTime(localDateTime: LocalDateTime): Long{
            val zone = ZoneId.of("Asia/Seoul")
            val time = ZonedDateTime.of(localDateTime.plusHours(9), zone)
            val now = ZonedDateTime.of(LocalDateTime.now(), zone)

            return kotlin.math.abs(now.toEpochSecond() - time.toEpochSecond())
        }

        fun setRatingStars(starImages: ArrayList<ImageView>, rating: Float, context: Context) {
            for(i in 0 until starImages.size){
                val drawableId = getDrawableIdOfStarImage(rating, i)
                val drawable = context.resources.getDrawable(drawableId, context.theme)
                starImages[i].setImageDrawable(drawable)
            }
        }

        private fun getDrawableIdOfStarImage(rating: Float, index: Int): Int{
            return when {
                rating > index + 0.75 -> R.drawable.ic_baseline_star_16
                rating > index + 0.25 -> R.drawable.ic_baseline_star_half_16
                else -> R.drawable.ic_baseline_star_border_16
            }
        }

        @SuppressLint("MissingPermission")
        fun getGeolocation(context: Context): MutableList<Double> {
            val latAndLong: MutableList<Double> = mutableListOf()

            if (Permission.isAllPermissionsGranted(context, Permission.requiredPermissionsForLocation)) {
                val location = (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER)

                if(location != null){
                    latAndLong.add(location.latitude)
                    latAndLong.add(location.longitude)
                }else{
                    // 정보 로드 실패 예외처리
                    latAndLong.add(0.0)
                    latAndLong.add(0.0)
                }
            }else{
                // 특수 처리를 위해 (-1, -1)을 넣음
                latAndLong.add((-1.0))
                latAndLong.add((-1.0))
            }

            return latAndLong
        }

        fun getDistanceUsingLatitudeAndLongitude(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): Double {
            val theta = longitude1 - longitude2
            var dist = sin(convertDegreeToRadian(latitude1)) * sin(convertDegreeToRadian(latitude2)) +
                    cos(convertDegreeToRadian(latitude1)) * cos(convertDegreeToRadian(latitude2)) * cos(convertDegreeToRadian(theta))
            dist = convertRadianToDegree(acos(dist))
            dist *= 60 * 1.1515 * 1.609344 * 1000

            return dist
        }

        private fun convertDegreeToRadian(degree: Double): Double {
            return degree * Math.PI / 180.0
        }

        private fun convertRadianToDegree(radian: Double): Double {
            return radian * 180.0 / Math.PI
        }
    }
}