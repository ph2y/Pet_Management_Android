package com.sju18001.petmanagement.ui.map

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.se.omapi.Session
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Permission
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.FragmentMapBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.kakaoapi.KakaoApi
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dao.Bookmark
import com.sju18001.petmanagement.restapi.dao.Place
import com.sju18001.petmanagement.restapi.dto.CreateBookmarkReqDto
import com.sju18001.petmanagement.restapi.dto.DeleteBookmarkReqDto
import com.sju18001.petmanagement.restapi.dto.FetchBookmarkReqDto
import com.sju18001.petmanagement.restapi.dto.FetchPlaceReqDto
import com.sju18001.petmanagement.ui.map.review.ReviewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.daum.mf.map.api.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal
import java.util.*

class MapFragment : Fragment(), MapView.CurrentLocationEventListener, MapView.MapViewEventListener, MapView.POIItemEventListener {
    companion object {
        private const val API_KEY = "KakaoAK fcb50b998a702691c31e6e2b3a4555be"
        private const val BASE_URL = "https://dapi.kakao.com/"

        private const val CURRENT_LOCATION_BUTTON_MARGIN: Int = 16
        private const val NAV_VIEW_HEIGHT: Int = 56
        private const val PLACE_CARD_HEIGHT: Int = 138

        private const val ANIMATION_DURATION: Long = 200

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

    private lateinit var bookmarkTreeAdapter: BookmarkTreeAdapter

    // 카카오맵 관련
    private var mapView: MapView? = null
    private var currentMapPoint: MapPoint? = null
    private var isLoadingCurrentMapPoint: Boolean = false

    private var currentPlaces: ArrayList<Place> = arrayListOf()

    // 애니메이션의 진행 여부를 파악하고자 멤버 변수로 둡니다.
    private var showingNavViewAnim: ValueAnimator? = null
    private var hidingNavViewAnim: ValueAnimator? = null
    private var increasingCurrentLocationButtonMarginAnim: ValueAnimator? = null
    private var decreasingCurrentLocationButtonMarginAnim: ValueAnimator? = null
    private var showingPlaceCardAnim: ValueAnimator? = null
    private var hidingPlaceCardAnim: ValueAnimator? = null

    // MainActivity에서 PlaceCard가 열려있는지 확인할 필요가 있습니다.
    // 애니메이션이 모두 끝나고 PlaceCard가 열려있는 상태일 때만 true를 가집니다.
    var isPlaceCardShowing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setBinding(inflater, container)
        isViewDestroyed = false

        Permission.requestNotGrantedPermissions(requireActivity(), Permission.requiredPermissionsForLocation)

        initializeMapView(inflater)
        initializeDrawerLayout()
        initializeAnimations()

        setupViews()

        return binding.root
    }

    private fun setBinding(inflater: LayoutInflater, container: ViewGroup?) {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false)

        binding.lifecycleOwner = this
        binding.fragment = this@MapFragment
        binding.viewModel = viewModel
        binding.util = Util.Companion
    }


    /**
     * initializing logic
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


    private fun initializeDrawerLayout() {
        initializeBookmarkRecyclerView()

        binding.layoutDrawer.addDrawerListener(object: DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }
            override fun onDrawerOpened(drawerView: View) {
                fetchBookmarkIfBookmarkIsNotFetched()
            }
            override fun onDrawerClosed(drawerView: View) {
            }
            override fun onDrawerStateChanged(newState: Int) {
            }
        })
    }

    private fun initializeBookmarkRecyclerView() {
        bookmarkTreeAdapter = BookmarkTreeAdapter(arrayListOf(), object: BookmarkTreeAdapterInterface{
            override fun addBookmarkPOIItem(place: Place) {
                val poiItemForSamePlace = mapView!!.poiItems.find{ it.itemName == place.name}
                if(poiItemForSamePlace != null){
                    // poiItemForSamePlace의 멤버 변수를 set하여도 자동으로 적용되지 않으므로
                    // 직접 remove & add를 하여 replace 해줍니다.
                    mapView!!.removePOIItem(poiItemForSamePlace)
                    poiItemForSamePlace.customImageResourceId = R.drawable.marker_pet
                    mapView!!.addPOIItem(poiItemForSamePlace)
                }else{
                    val newMarker: MapPOIItem = MapPOIItem().apply{
                        itemName = place.name
                        tag = currentPlaces.count()
                        mapPoint = MapPoint.mapPointWithGeoCoord(place.latitude, place.longitude)

                        markerType = MapPOIItem.MarkerType.CustomImage
                        isCustomImageAutoscale = false
                        setCustomImageAnchor(0.5f, 0.5f)
                        customImageResourceId = R.drawable.marker_pet
                    }

                    currentPlaces.add(place)
                    mapView!!.addPOIItem(newMarker)
                }
            }

            override fun closeDrawer() {
                binding.layoutDrawer.close()
            }
        })
        binding.recyclerViewBookmarkTree?.let{
            it.adapter = bookmarkTreeAdapter
            it.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun fetchBookmarkIfBookmarkIsNotFetched() {
        if(viewModel.isBookmarkFetched.get() == true) return

        bookmarkTreeAdapter.resetDataSet()
        binding.textEmptyBookmark.visibility = View.GONE

        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchBookmarkReq(FetchBookmarkReqDto(null, null, null))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
            response.body()?.bookmarkList?.map { bookmark ->
                // 해당 폴더에 처음 접근할 경우
                if(!bookmarkTreeAdapter.folderToBookmarks.containsKey(bookmark.folder)){
                    bookmarkTreeAdapter.addItem(BookmarkTreeItem(false, null, bookmark.folder, false))
                    bookmarkTreeAdapter.folderToBookmarks[bookmark.folder] = arrayListOf()
                }
                bookmarkTreeAdapter.folderToBookmarks[bookmark.folder]!!.add(bookmark)
            }

            bookmarkTreeAdapter.notifyDataSetChanged()

            viewModel.isBookmarkFetched.set(true)

            if(bookmarkTreeAdapter.folderToBookmarks.count() == 0) {
                binding.textEmptyBookmark.visibility = View.VISIBLE
            }
        }, {}, {})
    }


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

    fun doSearch(keyword: String){
        if(mapView == null) return

        val radius = SessionManager.fetchLoggedInAccount(requireContext())!!.mapSearchRadius
        searchKeyword(keyword, radius.toBigDecimal(), mapView!!)

        setMapCenterPointToCurrentLocation()
        val searchAreaCircle = addCircleCenteredAtCurrentLocation(mapView!!, radius.toInt())
        moveCameraOnCircle(mapView!!, searchAreaCircle!!, 50)
    }

    private fun searchKeyword(keyword: String, radius: BigDecimal, mapView: MapView){
        try{
            val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
                .fetchPlaceReq(FetchPlaceReqDto(
                    null, keyword,
                    currentMapPoint!!.mapPointGeoCoord.latitude.toBigDecimal(),
                    currentMapPoint!!.mapPointGeoCoord.longitude.toBigDecimal(),
                    radius
                ))
            ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), { response ->
                currentPlaces = arrayListOf()
                response.body()?.placeList?.map{ currentPlaces.add(it) }
                addPOIItems(currentPlaces, mapView)
            }, {}, {})
        }catch(e: Exception){
            // currentMapPoint가 아직 초기화되지 않았을 경우
            Log.e("MapFragment", e.message.toString())
        }
    }

    private fun addPOIItems(places: List<Place>, mapView: MapView){
        mapView.removeAllPOIItems()

        for(i: Int in 0 until places.count()){
            var iconId: Int = when(places[i].categoryCode){
                "SHOP" -> R.drawable.marker_store
                "HOSPITAL" -> R.drawable.marker_hospital
                "HOTEL" -> R.drawable.marker_default
                "CAFE" -> R.drawable.marker_cafe
                "SALON" -> R.drawable.marker_default
                "PARK" -> R.drawable.marker_default
                "SHELTER" -> R.drawable.marker_default
                else -> R.drawable.marker_default
            }

            val newMarker: MapPOIItem = MapPOIItem().apply{
                itemName = places[i].name
                tag = i
                mapPoint = MapPoint.mapPointWithGeoCoord(places[i].latitude, places[i].longitude)

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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isViewDestroyed = true
    }


    /**
     * CurrentLocationEventListener 인터페이스 구현
     */
    override fun onCurrentLocationDeviceHeadingUpdate(p0: MapView?, p1: Float) {}
    override fun onCurrentLocationUpdateFailed(p0: MapView?) {}
    override fun onCurrentLocationUpdateCancelled(p0: MapView?) {}
    override fun onCurrentLocationUpdate(p0: MapView?, p1: MapPoint?, p2: Float) {
        if (p1 != null) {
            currentMapPoint = p1
        }
    }


    /**
     * MapViewEventListener 인터페이스 구현
     */
    override fun onMapViewInitialized(p0: MapView?) {}
    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {}
    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {}

    override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {
        if (isPlaceCardOpened()){
            hidePlaceCard()
        }
    }

    private fun isPlaceCardOpened(): Boolean {
        val navView = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        return navView.height == 0 && binding.placeCard.height > 0
    }

    fun hidePlaceCard(){
        isPlaceCardShowing = false

        try{
            if(isAnimationRunning()) return

            hidingPlaceCardAnim!!.start()
            hidingPlaceCardAnim!!.doOnEnd { anim ->
                showingNavViewAnim!!.start()
                increasingCurrentLocationButtonMarginAnim!!.start()

                anim.removeAllListeners()
            }
        }catch(e: Exception){
            Log.e("MapFragment", "Failed to show place card: " + e.message)
        }
    }


    /**
     * MapView.POIItemEventListener 인터페이스 구현
     */
    override fun onCalloutBalloonOfPOIItemTouched(p0: MapView?, p1: MapPOIItem?) {}
    override fun onCalloutBalloonOfPOIItemTouched(p0: MapView?, p1: MapPOIItem?, p2: MapPOIItem.CalloutBalloonButtonType?) {}
    override fun onDraggablePOIItemMoved(p0: MapView?, p1: MapPOIItem?, p2: MapPoint?) {}

    override fun onPOIItemSelected(p0: MapView?, p1: MapPOIItem?) {
        if(p1 != null){
            showPlaceCard(p1!!)
        }
    }

    private fun showPlaceCard(item: MapPOIItem){
        try{
            if(isAnimationRunning()) return

            isPlaceCardShowing = false

            // 이미 다른 곳의 정보창이 열려있을 경우
            if(isPlaceCardOpened()){
                hidingPlaceCardAnim!!.start()
                hidingPlaceCardAnim!!.doOnEnd { anim ->
                    updatePlaceCard(item)

                    showingPlaceCardAnim!!.start()
                    anim.removeAllListeners()
                }
            }else{
                updatePlaceCard(item)

                hidingNavViewAnim!!.start()
                hidingNavViewAnim!!.doOnEnd { anim ->
                    showingPlaceCardAnim!!.start()
                    anim.removeAllListeners()
                }

                decreasingCurrentLocationButtonMarginAnim!!.start()
            }

            showingPlaceCardAnim!!.doOnEnd {
                isPlaceCardShowing = true
            }
        }catch(e: Exception){
            Log.e("MapFragment", "Failed to show place card: " + e.message)
        }
    }

    private fun updatePlaceCard(item: MapPOIItem) {
        currentPlaces?.get(item.tag)?.let { place ->
            val distance = Util.getDistanceUsingLatitudeAndLongitude(
                currentMapPoint!!.mapPointGeoCoord.latitude, currentMapPoint!!.mapPointGeoCoord.longitude,
                place.latitude, place.longitude
            ).toInt()

            viewModel.placeCard.set(
                PlaceCard(place, "$distance", false)
            )

            checkAndUpdateIsBookmarked()
        }
    }

    private fun checkAndUpdateIsBookmarked() {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .fetchBookmarkReq(FetchBookmarkReqDto(null, null, viewModel.placeCard.get()!!.place.id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(),{ response ->
            viewModel.setIsBookmarked(
                response.body()!!.bookmarkedAccountIdList?.contains(SessionManager.fetchLoggedInAccount(requireContext())!!.id)
            )
        }, {}, {})
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

    fun onPlaceCardClicked(place: Place) {
        startReviewActivity(place)
    }

    private fun startReviewActivity(place: Place) {
        val reviewActivityIntent = Intent(context, ReviewActivity::class.java)
        reviewActivityIntent.putExtra("placeId", place.id)
        reviewActivityIntent.putExtra("rating", place.averageRating)
        reviewActivityIntent.putExtra("reviewCount", place.reviewCount)

        startActivity(reviewActivityIntent)
        requireActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.exit_to_top)
    }

    fun getCategoryNameFromCategoryCode(categoryCode: String): String {
        return when(categoryCode){
            "SHOP" -> "펫샵"
            "HOSPITAL" -> "동물병원"
            "HOTEL" -> "반려동물 동반 숙박시설"
            "CAFE" -> "반려동물 동반 카페"
            "SALON" -> "반려동물 미용실"
            "PARK" -> "공원"
            "SHELTER" -> "동물 보호소"
            else -> ""
        }
    }

    fun getPlaceDistanceTextColor(distance: String): Int{
        return when (distance.toInt()){
            in 0..750 -> ContextCompat.getColor(requireContext(), R.color.emerald)
            in 751..1500 -> ContextCompat.getColor(requireContext(), R.color.sunflower)
            in 1501..2250 -> ContextCompat.getColor(requireContext(), R.color.carrot)
            else -> ContextCompat.getColor(requireContext(), R.color.alizarin)
        }
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

    fun onBookmarkButtonClicked(place: Place){
        when(viewModel.getIsBookmarked()){
            false -> createBookmark(place)
            true -> deleteBookmark(place.id)
        }

        viewModel.setIsBookmarked(!viewModel.getIsBookmarked())
    }

    private fun createBookmark(place: Place) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .createBookmarkReq(CreateBookmarkReqDto(
                place.id,
                place.name,
                place.categoryCode,
                getString(R.string.bookmark_default_folder) // 우선 기본 폴더에 등록한다.
            ))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            viewModel.isBookmarkFetched.set(false) // 북마크를 다시 fetch하도록 유도
        }, {}, {})
    }

    private fun deleteBookmark(placeId: Long) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(requireContext())!!)
            .deleteBookmarkReq(DeleteBookmarkReqDto(null, placeId))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, requireContext(), {
            viewModel.isBookmarkFetched.set(false) // 북마크를 다시 fetch하도록 유도
        }, {}, {})
    }
}