package com.sju18001.petmanagement.ui.map

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Permission
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentMapBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.kakaoapi.KakaoApi
import com.sju18001.petmanagement.restapi.kakaoapi.Place
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dto.CreateBookmarkReqDto
import com.sju18001.petmanagement.restapi.dto.DeleteBookmarkReqDto
import com.sju18001.petmanagement.ui.map.review.ReviewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.daum.mf.map.api.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class MapFragment : Fragment(), MapView.CurrentLocationEventListener, MapView.MapViewEventListener, MapView.POIItemEventListener {
    companion object {
        private const val API_KEY = "KakaoAK fcb50b998a702691c31e6e2b3a4555be"
        private const val BASE_URL = "https://dapi.kakao.com/"

        private const val CURRENT_LOCATION_BUTTON_MARGIN: Int = 16
        private const val NAV_VIEW_HEIGHT: Int = 56
        private const val PLACE_CARD_HEIGHT: Int = 138

        private const val ANIMATION_DURATION: Long = 200

        private const val SEARCH_METER_RADIUS: Int = 3000

        // search_shortcut 버튼의 쿼리 키워드
        const val CAFE_KEYWORD = "애견카페"
        const val GROOMING_KEYWORD = "애견미용"
        const val SUPPLY_KEYWORD = "애견용품"
        const val HOSPITAL_KEYWORD = "동물병원"
    }

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()
    private var isViewDestroyed = false

    // 지도 관련
    private var mapView: MapView? = null
    private var currentMapPoint: MapPoint? = null
    private var isLoadingCurrentMapPoint: Boolean = false

    private var currentPlaces: List<Place>? = null

    // 애니메이션의 진행 여부를 파악하고자 멤버 변수로 둡니다.
    private var showingNavViewAnim: ValueAnimator? = null
    private var hidingNavViewAnim: ValueAnimator? = null
    private var increasingCurrentLocationButtonMarginAnim: ValueAnimator? = null
    private var decreasingCurrentLocationButtonMarginAnim: ValueAnimator? = null
    private var showingPlaceCardAnim: ValueAnimator? = null
    private var hidingPlaceCardAnim: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        isViewDestroyed = false

        Permission.requestNotGrantedPermissions(requireActivity(), Permission.requiredPermissionsForLocation)
        initializeMapView(inflater)

        initializeAnimations()
        setupViews()

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@MapFragment
        binding.viewModel = viewModel
    }


    /**
     * initializeMapView
     */
    private fun initializeMapView(inflater: LayoutInflater) {
        mapView = MapView(this.activity)
        binding.mapView.addView(mapView)

        mapView!!.setCurrentLocationEventListener(this)
        mapView!!.setMapViewEventListener(this)
        mapView!!.setPOIItemEventListener(this)

        mapView!!.setCustomCurrentLocationMarkerTrackingImage(R.drawable.marker_current_location, MapPOIItem.ImageOffset(16, 16))
        mapView!!.setCalloutBalloonAdapter(CustomCalloutBalloonAdapter(inflater))

        // 위치 권한이 없을 때, 아래에서 에러가 발생함
        try{
            mapView!!.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving
            setMapCenterPointToCurrentLocation()
        }catch(e: Exception){
            Toast.makeText(requireContext(), getString(R.string.map_permission_error_message), Toast.LENGTH_LONG).show()
            mapView = null // nullify함으로써 mapView 관련 작업이 수행되는 것을 방지한다.
        }
    }

    fun setMapCenterPointToCurrentLocation(){
        if(mapView == null) return

        // 현위치를 얻은 적이 있는 경우
        if(currentMapPoint != null){
            mapView!!.setMapCenterPoint(currentMapPoint, true)
        }else{
            // 중복 실행을 허용하지 않습니다.
            if(!isLoadingCurrentMapPoint){
                isLoadingCurrentMapPoint = true

                GlobalScope.launch(Dispatchers.IO){
                    while(currentMapPoint == null){
                        // Wait for Loading
                        delay(100L)
                    }

                    mapView!!.setMapCenterPoint(currentMapPoint, true)
                }
            }
        }
    }


    /**
     * initializeAnimations
     */
    fun doSearch(keyword: String){
        if(mapView == null) return

        searchKeyword(keyword, mapView!!)

        setMapCenterPointToCurrentLocation()
        val searchAreaCircle = addCircleCenteredAtCurrentLocation(mapView!!, SEARCH_METER_RADIUS)
        moveCameraOnCircle(mapView!!, searchAreaCircle!!, 50)
    }

    private fun searchKeyword(keyword: String, mapView: MapView){
        // TODO: 자체 Place 검색이 구현되면 그것으로 대체할 것
        // Retrofit 구성
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(KakaoApi::class.java)

        // GET 요청
        try{
            val call = api.getSearchKeyword(
                API_KEY,
                keyword,
                currentMapPoint!!.mapPointGeoCoord.longitude.toString(),
                currentMapPoint!!.mapPointGeoCoord.latitude.toString(),
                SEARCH_METER_RADIUS
            )
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                val body = response.body()
                if(body != null){
                    currentPlaces = body.documents
                    addPOIItems(currentPlaces!!, mapView)
                }
            }, {}, {})
        }catch(e: Exception){
            // currentMapPoint가 아직 초기화되지 않았을 경우
            Log.e("MapFragment", e.message.toString())
        }
    }

    private fun addPOIItems(places: List<Place>, mapView: MapView){
        mapView.removeAllPOIItems()

        for(i: Int in 0 until places.count()){
            var iconId: Int = when(places[i].category_group_code){
                "MT1", "CS2" -> R.drawable.marker_store
                "PS3", "SC4", "AC5" -> R.drawable.marker_school
                "PK6", "OL7" -> R.drawable.marker_car
                "SW8" -> R.drawable.marker_train
                "CE7" -> R.drawable.marker_cafe
                "HP8", "PM9" -> R.drawable.marker_hospital
                else -> R.drawable.marker_default
            }

            val newMarker: MapPOIItem = MapPOIItem().apply{
                itemName = places[i].place_name
                tag = i
                mapPoint = MapPoint.mapPointWithGeoCoord(places[i].y.toDouble(), places[i].x.toDouble())

                markerType = MapPOIItem.MarkerType.CustomImage
                isCustomImageAutoscale = false
                setCustomImageAnchor(0.5f, 0.5f)
                customImageResourceId = iconId
            }

            mapView.addPOIItem(newMarker)
        }
    }

    private fun addCircleCenteredAtCurrentLocation(mapView: MapView, radius: Int): MapCircle?{
        var searchAreaCircle: MapCircle? = null
        mapView.removeAllCircles()

        try{
            searchAreaCircle = MapCircle(
                currentMapPoint,
                radius,
                Color.argb(128, 255, 0, 0),
                Color.argb(0, 0, 0, 0)
            )
            searchAreaCircle.tag = 1000
            mapView.addCircle(searchAreaCircle)
        }catch(e: Exception){
            // currentMapPoint가 아직 초기화되지 않았을 경우
            Log.e("MapFragment", e.message.toString())
        }

        return searchAreaCircle
    }

    private fun moveCameraOnCircle(mapView: MapView, circle: MapCircle, padding: Int){
        try{
            val mapPointBoundsArray = arrayOf(circle.bound, circle.bound)
            val mapPointBounds = MapPointBounds(mapPointBoundsArray)
            mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding))
        }catch(e:Exception){
            Log.i("MapFragment", e.message.toString())
        }
    }


    /**
     * initializeAnimations
     */
    private fun initializeAnimations(){
        showingNavViewAnim = ValueAnimator.ofInt(0, Util.convertDpToPixel(NAV_VIEW_HEIGHT))
        showingNavViewAnim!!.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            val navView = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)

            navView.layoutParams.height = value
            navView.requestLayout()
        }
        showingNavViewAnim!!.duration = ANIMATION_DURATION

        hidingNavViewAnim = ValueAnimator.ofInt(Util.convertDpToPixel(NAV_VIEW_HEIGHT), 0)
        hidingNavViewAnim!!.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            val navView = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)

            navView.layoutParams.height = value
            navView.requestLayout()
        }
        hidingNavViewAnim!!.duration = ANIMATION_DURATION

        increasingCurrentLocationButtonMarginAnim = ValueAnimator.ofInt(Util.convertDpToPixel(CURRENT_LOCATION_BUTTON_MARGIN), Util.convertDpToPixel(NAV_VIEW_HEIGHT + CURRENT_LOCATION_BUTTON_MARGIN))
        increasingCurrentLocationButtonMarginAnim!!.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            val params = binding.currentLocationButton.layoutParams as (ViewGroup.MarginLayoutParams)

            params.bottomMargin = value
            binding.currentLocationButton.requestLayout()
        }
        increasingCurrentLocationButtonMarginAnim!!.duration = ANIMATION_DURATION

        decreasingCurrentLocationButtonMarginAnim = ValueAnimator.ofInt(Util.convertDpToPixel(NAV_VIEW_HEIGHT + CURRENT_LOCATION_BUTTON_MARGIN), Util.convertDpToPixel(CURRENT_LOCATION_BUTTON_MARGIN))
        decreasingCurrentLocationButtonMarginAnim!!.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            val params = binding.currentLocationButton.layoutParams as (ViewGroup.MarginLayoutParams)

            params.bottomMargin = value
            binding.currentLocationButton.requestLayout()
        }
        decreasingCurrentLocationButtonMarginAnim!!.duration = ANIMATION_DURATION

        showingPlaceCardAnim = ValueAnimator.ofInt(1, Util.convertDpToPixel(PLACE_CARD_HEIGHT))
        showingPlaceCardAnim!!.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int

            binding.placeCard.layoutParams.height = value
            binding.placeCard.requestLayout()
        }
        showingPlaceCardAnim!!.duration = ANIMATION_DURATION

        hidingPlaceCardAnim = ValueAnimator.ofInt(Util.convertDpToPixel(PLACE_CARD_HEIGHT), 1)
        hidingPlaceCardAnim!!.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int

            binding.placeCard.layoutParams.height = value
            binding.placeCard.requestLayout()
        }
        hidingPlaceCardAnim!!.duration = ANIMATION_DURATION
    }

    private fun isAnimationRunning(): Boolean{
        return showingNavViewAnim!!.isRunning ||
                hidingNavViewAnim!!.isRunning ||
                increasingCurrentLocationButtonMarginAnim!!.isRunning ||
                decreasingCurrentLocationButtonMarginAnim!!.isRunning ||
                showingPlaceCardAnim!!.isRunning ||
                hidingPlaceCardAnim!!.isRunning
    }

    
    /**
     * setupViews
     */
    private fun setupViews() {
        Util.setupViewsForHideKeyboard(requireActivity(), binding.fragmentMapParentLayout)
        addMarginBottomToCurrentLocationButton()
        setEditorActionListenerToSearchTextInput()
    }

    private fun addMarginBottomToCurrentLocationButton() {
        binding.currentLocationButton.apply {
            val currentLocationButtonParams = layoutParams as ViewGroup.MarginLayoutParams
            currentLocationButtonParams.bottomMargin += Util.convertDpToPixel(NAV_VIEW_HEIGHT)

            layoutParams = currentLocationButtonParams
        }
    }

    // 검색바에 포커스된 채로 키보드의 '확인'을 누를 때의 이벤트 등록
    private fun setEditorActionListenerToSearchTextInput() {
        binding.searchTextInput.setOnEditorActionListener { textView, _, _ ->
            doSearch(textView.text.toString())
            Util.hideKeyboard(requireActivity())

            true
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isViewDestroyed = true
    }


    /**
     * CurrentLocationEventListener 인터페이스 구현
     */
    override fun onCurrentLocationUpdate(p0: MapView?, p1: MapPoint?, p2: Float) {
        if (p1 != null) {
            currentMapPoint = p1
        }
    }

    override fun onCurrentLocationDeviceHeadingUpdate(p0: MapView?, p1: Float) {
    }

    override fun onCurrentLocationUpdateFailed(p0: MapView?) {
    }

    override fun onCurrentLocationUpdateCancelled(p0: MapView?) {
    }


    /**
     * MapViewEventListener 인터페이스 구현
     */
    override fun onMapViewInitialized(p0: MapView?) {
    }

    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {
    }

    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {
    }

    override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {
        if (isPlaceCardOpened()){
            hidePlaceCard()
        }
    }

    private fun isPlaceCardOpened(): Boolean {
        val navView = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        return navView.height == 0 && binding.placeCard.height > 0
    }

    private fun hidePlaceCard(){
        try{
            if(isAnimationRunning()) return

            hidingPlaceCardAnim!!.doOnEnd { anim ->
                showingNavViewAnim!!.start()
                increasingCurrentLocationButtonMarginAnim!!.start()

                anim.removeAllListeners()
            }

            hidingPlaceCardAnim!!.start()
        }catch(e: Exception){
            Log.e("MapFragment", "Failed to show place card: " + e.message)
        }
    }

    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {
    }

    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {
    }

    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {
    }

    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {
    }

    override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {
    }


    /**
     * MapView.POIItemEventListener 인터페이스 구현
     */
    override fun onPOIItemSelected(p0: MapView?, p1: MapPOIItem?) {
        if(p1 != null){
            showPlaceCard(p1!!)
        }
    }

    private fun showPlaceCard(item: MapPOIItem){
        try{
            if(isAnimationRunning()) return

            // 이미 다른 곳의 정보창이 열려있을 경우
            if(isPlaceCardOpened()){
                hidingPlaceCardAnim!!.doOnEnd { anim ->
                    updatePlaceCard(item)

                    showingPlaceCardAnim!!.start()
                    anim.removeAllListeners()
                }

                hidingPlaceCardAnim!!.start()
            }else{
                hidingNavViewAnim!!.doOnEnd { anim ->
                    showingPlaceCardAnim!!.start()
                    anim.removeAllListeners()
                }

                updatePlaceCard(item)

                hidingNavViewAnim!!.start()
                decreasingCurrentLocationButtonMarginAnim!!.start()
            }
        }catch(e: Exception){
            Log.e("MapFragment", "Failed to show place card: " + e.message)
        }
    }

    private fun updatePlaceCard(item: MapPOIItem){
        currentPlaces?.get(item.tag)?.let { place ->
            viewModel.placeCard.set(
                PlaceCard(place, false) // TODO: bookmarkedAccountIdList 생긴 뒤에 값을 조절할 것
            )
            setPlaceCardRating(4.3f) // TODO: place에 rating이 추가되면 databinding으로 변경: 'place_rating' item
        }
    }

    private fun setPlaceCardRating(rating: Float){
        binding.textRating.text = rating.toString()
        Util.setRatingStars(getStarImages(), rating, requireContext())
    }

    private fun getStarImages(): ArrayList<ImageView> {
        val starImages = arrayListOf<ImageView>()
        for(i in 1..5){
            // View id: image_star1 ~ image_star5
            val id = resources.getIdentifier("image_star$i", "id", requireContext().packageName)
            val elem: ImageView = binding.placeRating.findViewById(id)
            starImages.add(elem)
        }
        return starImages
    }

    override fun onCalloutBalloonOfPOIItemTouched(p0: MapView?, p1: MapPOIItem?) {
    }

    override fun onCalloutBalloonOfPOIItemTouched(p0: MapView?, p1: MapPOIItem?, p2: MapPOIItem.CalloutBalloonButtonType?) {
    }

    override fun onDraggablePOIItemMoved(p0: MapView?, p1: MapPOIItem?, p2: MapPoint?) {
    }


    /**
     * Databinding functions
     */
    fun onDrawerButtonClicked() {
        binding.layoutDrawer.open()
    }

    fun onSearchTextCancelClicked() {
        binding.searchTextInput!!.setText("")
    }

    fun onPlaceCardClicked(placeId: Long) {
        startReviewActivity(placeId)
    }

    private fun startReviewActivity(placeId: Long) {
        val reviewActivityIntent = Intent(context, ReviewActivity::class.java)
        reviewActivityIntent.putExtra("placeId", placeId)

        startActivity(reviewActivityIntent)
        requireActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.exit_to_top)
    }

    fun getPlaceDistanceTextColor(distance: String): Int{
        return when (distance.toInt()){
            in 0..750 -> ContextCompat.getColor(requireContext(), R.color.emerald)
            in 751..1500 -> ContextCompat.getColor(requireContext(), R.color.sunflower)
            in 1501..2250 -> ContextCompat.getColor(requireContext(), R.color.carrot)
            else -> ContextCompat.getColor(requireContext(), R.color.alizarin)
        }
    }

    fun onPathfindingButtonClicked(placeId: String){
        AlertDialog.Builder(context)
            .setTitle("길찾기")
            .setMessage("길찾기를 위해 카카오맵 웹페이지로 이동합니다.")
            .setPositiveButton("확인") { _, _ ->
                Util.openWebPage(requireActivity(), "https://map.kakao.com/link/to/$placeId")
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.cancel()
            }
            .create().show()
    }

    fun onCallButtonClicked(place: Place){
        val buttonStrings: Array<CharSequence> = arrayOf("전화하기", "연락처 저장하기", "클립보드에 복사하기")
        AlertDialog.Builder(context)
            .setTitle(place.phone)
            .setItems(buttonStrings){ _, which ->
                when(which){
                    0 -> {
                        if(Permission.isAllPermissionsGranted(requireContext(), Permission.requiredPermissionsForCall)){
                            Util.doCall(requireActivity(), place.phone)
                        }else{
                            Permission.requestNotGrantedPermissions(requireActivity(), Permission.requiredPermissionsForCall)
                        }
                    }
                    1 -> {
                        if(Permission.isAllPermissionsGranted(requireContext(), Permission.requiredPermissionsForContacts)){
                            Util.insertContactsContract(requireActivity(), place)
                        }else{
                            Permission.requestNotGrantedPermissions(requireActivity(), Permission.requiredPermissionsForContacts)
                        }
                    }
                    2 -> Util.doCopy(requireActivity(), place.phone)
                }
            }
            .setNegativeButton("취소"){ dialog, _ ->
                dialog.cancel()
            }
            .create().show()
    }

    fun onShareButtonClicked(place: Place){
        Util.shareText(requireActivity(), place.place_url)
    }

    fun onBookmarkButtonClicked(place: Place){
        when(viewModel.getIsBookmarked()){
            false -> createBookmark(place)
            true -> deleteBookmark(1) // TODO: 1 -> place.id
        }

        viewModel.setIsBookmarked(!viewModel.getIsBookmarked())
    }

    private fun createBookmark(place: Place) {
        // TODO: 자체 Place로 바꿀 것
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .createBookmarkReq(CreateBookmarkReqDto(
                1, // TODO: place.id로 바꿀 것
                place.place_name,
                place.category_group_name,
                getString(R.string.bookmark_default_folder) // 우선 기본 폴더에 등록한다.
            ))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {}, {}, {})
    }

    private fun deleteBookmark(placeId: Long) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .deleteBookmarkReq(DeleteBookmarkReqDto(placeId))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {}, {}, {})
    }
}