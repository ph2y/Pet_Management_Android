package com.sju18001.petmanagement

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.databinding.ActivityMainBinding
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.restapi.dto.UpdateFcmRegistrationTokenReqDto
import com.sju18001.petmanagement.restapi.fcm.FcmUtil
import com.sju18001.petmanagement.ui.community.CommunityFragment
import com.sju18001.petmanagement.ui.community.followerFollowing.FollowerFollowingActivity
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
    private lateinit var activeFragment: Fragment
    private var activeFragmentIndex: Int = 0

    private var isViewDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isViewDestroyed = false

        // for fragment reset(after activity destruction)
        fragmentManager.findFragmentByTag("myPet")?.let {
            fragmentManager.beginTransaction().remove(it).commitNow()
        }
        fragmentManager.findFragmentByTag("map")?.let {
            fragmentManager.beginTransaction().remove(it).commitNow()
        }
        fragmentManager.findFragmentByTag("community")?.let {
            fragmentManager.beginTransaction().remove(it).commitNow()
        }
        fragmentManager.findFragmentByTag("setting")?.let {
            fragmentManager.beginTransaction().remove(it).commitNow()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val actionBar: ActionBar? = supportActionBar
        val navView: BottomNavigationView = binding.navView

        // get current selected item + set title
        when(savedInstanceState?.getInt("active_fragment_index")) {
            0 -> {
                addFragmentWhenFragmentIsNull(myPetFragment, "myPet")
                activeFragment = myPetFragment

                navView.menu.getItem(0).isChecked = true
                activeFragmentIndex = 0
                invalidateOptionsMenu()
                actionBar?.setTitle(R.string.title_my_pet)
                actionBar?.show()
            }
            1 -> {
                addFragmentWhenFragmentIsNull(mapFragment, "map")
                activeFragment = mapFragment

                navView.menu.getItem(1).isChecked = true
                activeFragmentIndex = 1
                invalidateOptionsMenu()
                actionBar?.hide()
            }
            2 -> {
                addFragmentWhenFragmentIsNull(communityFragment, "community")
                activeFragment = communityFragment

                navView.menu.getItem(2).isChecked = true
                activeFragmentIndex = 2
                invalidateOptionsMenu()
                actionBar?.setTitle(R.string.title_community)
                actionBar?.show()
            }
            3 -> {
                addFragmentWhenFragmentIsNull(settingFragment, "setting")
                activeFragment = settingFragment

                navView.menu.getItem(3).isChecked = true
                activeFragmentIndex = 3
                invalidateOptionsMenu()
                actionBar?.setTitle(R.string.title_setting)
                actionBar?.show()
            }
            else -> {
                addFragmentWhenFragmentIsNull(myPetFragment, "myPet")
                activeFragment = myPetFragment

                navView.menu.getItem(0).isChecked = true
                activeFragmentIndex = 0
                invalidateOptionsMenu()
                actionBar?.setTitle(R.string.title_my_pet)
                actionBar?.show()
            }
        }

        // Show active fragment
        fragmentManager.beginTransaction().show(activeFragment).commitNow()

        navView.setOnNavigationItemSelectedListener {
            // 커뮤니티에서 다른 탭으로 이동 시
            if(activeFragment.tag == "community"){
                (activeFragment as CommunityFragment).pauseAllVideos()
            }

            when(it.itemId){
                R.id.navigation_my_pet -> {
                    addFragmentWhenFragmentIsNull(myPetFragment, "myPet")
                    fragmentManager.beginTransaction().hide(activeFragment).show(myPetFragment).commitNow()

                    navView.menu.getItem(0).isChecked = true

                    invalidateOptionsMenu()
                    actionBar?.setTitle(R.string.title_my_pet)
                    actionBar?.show()

                    activeFragmentIndex = 0
                    activeFragment = myPetFragment

                    true
                }
                R.id.navigation_map -> {
                    addFragmentWhenFragmentIsNull(mapFragment, "map")
                    fragmentManager.beginTransaction().hide(activeFragment).show(mapFragment).commitNow()

                    navView.menu.getItem(1).isChecked = true

                    invalidateOptionsMenu()
                    actionBar?.setShowHideAnimationEnabled(false)
                    actionBar?.hide()

                    activeFragmentIndex = 1
                    activeFragment = mapFragment

                    true
                }
                R.id.navigation_community -> {
                    addFragmentWhenFragmentIsNull(communityFragment, "community")
                    fragmentManager.beginTransaction().hide(activeFragment).show(communityFragment).commitNow()

                    navView.menu.getItem(2).isChecked = true

                    invalidateOptionsMenu()
                    actionBar?.setTitle(R.string.title_community)
                    actionBar?.show()

                    activeFragmentIndex = 2
                    activeFragment = communityFragment

                    (activeFragment as CommunityFragment).startAllVideos()

                    true
                }
                R.id.navigation_setting -> {
                    addFragmentWhenFragmentIsNull(settingFragment, "setting")
                    fragmentManager.beginTransaction().hide(activeFragment).show(settingFragment).commitNow()

                    navView.menu.getItem(3).isChecked = true

                    invalidateOptionsMenu()
                    actionBar?.setTitle(R.string.title_setting)
                    actionBar?.show()

                    activeFragmentIndex = 3
                    activeFragment = settingFragment

                    true
                }
            }
            false
        }

        FcmUtil.getFirebaseMessagingToken { token -> sendRegistrationToServer(token) }
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

        val builder = AlertDialog.Builder(this)
        builder.setMessage(baseContext.getString(R.string.exit_dialog))
            .setPositiveButton(
                R.string.confirm
            ) { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton(
                R.string.cancel
            ) { dialog, _ ->
                dialog.cancel()
            }
            .create().show()
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
                menuInflater.inflate(R.menu.follower_following_menu, menu)
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
                startActivity(Intent(this, FollowerFollowingActivity::class.java))
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

    private fun addFragmentWhenFragmentIsNull(fragment: Fragment, tag: String) {
        if(fragmentManager.findFragmentByTag(tag) == null){
            fragmentManager.beginTransaction().add(R.id.nav_host_fragment_activity_main, fragment, tag).commitNow()
        }
    }


    // For fcm token
    private fun sendRegistrationToServer(p0: String) {
        SessionManager.fetchLoggedInAccount(baseContext)?.let {
            // 최신 토큰과, 계정 토큰이 다를 경우 업데이트한다.
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
}