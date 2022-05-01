package com.sju18001.petmanagement

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.databinding.ActivityMainBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dto.UpdateFcmRegistrationTokenReqDto
import com.sju18001.petmanagement.restapi.fcm.FcmUtil
import com.sju18001.petmanagement.ui.community.CommunityFragment
import com.sju18001.petmanagement.ui.community.follow.FollowActivity
import com.sju18001.petmanagement.ui.map.MapFragment
import com.sju18001.petmanagement.ui.setting.SettingFragment
import com.sju18001.petmanagement.ui.myPet.MyPetFragment
import java.security.MessageDigest
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var myPetFragment: Fragment = MyPetFragment()
    private var mapFragment: Fragment = MapFragment()
    private var communityFragment: Fragment = CommunityFragment()
    private var settingFragment: Fragment = SettingFragment()
    private var fragmentManager: FragmentManager = supportFragmentManager
    private var activeFragment: Fragment? = null
    private var activeFragmentIndex: Int = 0

    private var isViewDestroyed = false

    private var interstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // for fragment reset(after activity destruction)
        fragmentManager.findFragmentByTag("myPet")?.let { fragmentManager.beginTransaction().remove(it).commitNow() }
        fragmentManager.findFragmentByTag("map")?.let { fragmentManager.beginTransaction().remove(it).commitNow() }
        fragmentManager.findFragmentByTag("community")?.let { fragmentManager.beginTransaction().remove(it).commitNow() }
        fragmentManager.findFragmentByTag("setting")?.let { fragmentManager.beginTransaction().remove(it).commitNow() }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.elevation = 0f

        // get current selected item + set title
        when(savedInstanceState?.getInt("active_fragment_index")) {
            0 -> {
                addFragmentWhenFragmentIsNotAdded(myPetFragment, "myPet", 0)
                supportActionBar?.setTitle(R.string.title_my_pet)
                supportActionBar?.show()
            }
            1 -> {
                addFragmentWhenFragmentIsNotAdded(mapFragment, "map", 1)
                supportActionBar?.hide()
            }
            2 -> {
                addFragmentWhenFragmentIsNotAdded(communityFragment, "community", 2)
                supportActionBar?.setTitle(R.string.title_community)
                supportActionBar?.show()
            }
            3 -> {
                addFragmentWhenFragmentIsNotAdded(settingFragment, "setting", 3)
                supportActionBar?.setTitle(R.string.title_setting)
                supportActionBar?.show()
            }
            // 최초 실행 시에는 해당 값에 데이터가 없다
            else -> {
                addFragmentWhenFragmentIsNotAdded(myPetFragment, "myPet", 0)
                supportActionBar?.setTitle(R.string.title_my_pet)
                supportActionBar?.show()
            }
        }


        FcmUtil.getFirebaseMessagingToken { token -> sendRegistrationToServer(token) }

        MobileAds.initialize(this) {}
        loadInterstitialAd()
    }

    private fun addFragmentWhenFragmentIsNotAdded(fragment: Fragment, tag: String, index: Int) {
        if(fragmentManager.findFragmentByTag(tag) == null){
            fragmentManager.beginTransaction().add(R.id.framelayout_main_navigationhost, fragment, tag).commitNow()
        }
        if(activeFragment == null) fragmentManager.beginTransaction().show(fragment).commitNow()
        else fragmentManager.beginTransaction().hide(activeFragment!!).show(fragment).commitNow()

        binding.bottomnavigationviewMain.menu.getItem(index).isChecked = true
        invalidateOptionsMenu()
        activeFragment = fragment
        activeFragmentIndex = index
    }

    private fun sendRegistrationToServer(p0: String) {
        SessionManager.fetchLoggedInAccount(baseContext)?.let {
            // 최신 토큰과, 계정 토큰이 다를 경우에만 업데이트한다.
            if(it.fcmRegistrationToken != p0) {
                updateFcmRegistrationToken(p0)
            }
        }
    }

    private fun updateFcmRegistrationToken(fcmRegistrationToken: String) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(baseContext)!!)
            .updateFcmRegistrationTokenReq(UpdateFcmRegistrationTokenReqDto(fcmRegistrationToken))
        ServerUtil.enqueueApiCall(call, {false}, baseContext, {},{},{})
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        // TODO: change adUnitId (current: sample ad unit id)
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                interstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                this@MainActivity.interstitialAd = interstitialAd
                interstitialAd.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        loadInterstitialAd()

                        val builder = AlertDialog.Builder(this@MainActivity)
                        builder.setMessage(baseContext.getString(R.string.exit_dialog))
                            .setPositiveButton(
                                R.string.confirm
                            ) { _, _ ->
                                finish()
                            }
                            .setNegativeButton(
                                R.string.cancel
                            ) { dialog, _ ->
                                dialog.cancel()
                            }
                            .create().show()
                    }
                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}
                    override fun onAdShowedFullScreenContent() {
                        this@MainActivity.interstitialAd = null
                    }
                }
            }
        })
    }


    override fun onStart() {
        super.onStart()
        binding.bottomnavigationviewMain.setOnNavigationItemSelectedListener {
            // 커뮤니티에서 다른 탭으로 이동 시
            if(activeFragment?.tag == "community"){
                (activeFragment as CommunityFragment).pauseAllVideos()
            }

            when(it.itemId){
                R.id.navigation_my_pet -> {
                    addFragmentWhenFragmentIsNotAdded(myPetFragment, "myPet", 0)
                    supportActionBar?.setTitle(R.string.title_my_pet)
                    supportActionBar?.show()

                    true
                }
                R.id.navigation_map -> {
                    addFragmentWhenFragmentIsNotAdded(mapFragment, "map", 1)
                    supportActionBar?.setShowHideAnimationEnabled(false)
                    supportActionBar?.hide()

                    true
                }
                R.id.navigation_community -> {
                    addFragmentWhenFragmentIsNotAdded(communityFragment, "community", 2)
                    supportActionBar?.setTitle(R.string.title_community)
                    supportActionBar?.show()

                    (activeFragment as CommunityFragment).startAllVideos()

                    true
                }
                R.id.navigation_setting -> {
                    addFragmentWhenFragmentIsNotAdded(settingFragment, "setting", 3)
                    supportActionBar?.setTitle(R.string.title_setting)
                    supportActionBar?.show()

                    true
                }
            }
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
    }

    override fun onBackPressed() {
        // MapFragment에서 PlaceCard가 열려있을 때는 PlaceCard를 닫습니다.
        if(activeFragmentIndex == 1){
            if((activeFragment as MapFragment).isPlaceCardShowing){
                (activeFragment as MapFragment).hidePlaceCard()
                return
            }
        }

        if (interstitialAd != null) {
            interstitialAd?.show(this)
        }
    }

    // for saving currently active fragment index
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
        outState.putInt("active_fragment_index", activeFragmentIndex)
    }

    // for action bar menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        when(activeFragmentIndex) {
            0 -> { return false }
            1 -> { return false }
            2 -> {
                menuInflater.inflate(R.menu.follow_menu, menu)
                return true
            }
            3 -> { return false }
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(activeFragmentIndex) {
            0 -> { return false }
            1 -> { return false }
            2 -> {
                // start follower following activity
                startActivity(Intent(this, FollowActivity::class.java))
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)

                return super.onOptionsItemSelected(item)
            }
            3 -> { return false }
        }
        return false
    }


    // 디버그 전용 Key
    private fun getAppKeyHash() {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for(i in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(i.toByteArray())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val encoder = Base64.getEncoder()
                    Log.e("Debug key", encoder.encodeToString(md.digest()))
                }
            }
        } catch(e: Exception) {
            Log.e("Not found", e.toString())
        }
    }
}