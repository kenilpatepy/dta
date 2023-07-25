package com.xyoye.dandanplay.ui.main

import android.view.KeyEvent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.MutableLiveData
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.xyoye.common_component.base.BaseActivity
import com.xyoye.common_component.bridge.LoginObserver
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.config.ScreencastConfig
import com.xyoye.common_component.config.UserConfig
import com.xyoye.common_component.extension.findAndRemoveFragment
import com.xyoye.common_component.extension.hideFragment
import com.xyoye.common_component.extension.showFragment
import com.xyoye.common_component.services.ScreencastProvideService
import com.xyoye.common_component.services.ScreencastReceiveService
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.common_component.weight.dialog.CommonDialog
import com.xyoye.dandanplay.BR
import com.xyoye.dandanplay.R
import com.xyoye.dandanplay.databinding.ActivityMainBinding
import com.xyoye.data_component.data.LoginData
import kotlin.random.Random
import kotlin.system.exitProcess

class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>(),
    LoginObserver {
    companion object {
        private const val TAG_FRAGMENT_HOME = "tag_fragment_home"
        private const val TAG_FRAGMENT_MEDIA = "tag_fragment_media"
        private const val TAG_FRAGMENT_PERSONAL = "tag_fragment_personal"
    }

    private lateinit var homeFragment: Fragment
    private lateinit var mediaFragment: Fragment
    private lateinit var personalFragment: Fragment
    private lateinit var previousFragment: Fragment

    @Autowired
    lateinit var provideService: ScreencastProvideService

    @Autowired
    lateinit var receiveService: ScreencastReceiveService

    private var fragmentTag = ""
    private var touchTime = 0L

    override fun initViewModel() =
        ViewModelInit(
            BR.viewModel,
            MainViewModel::class.java
        )

    override fun getLayoutId() = R.layout.activity_main

    override fun initView() {
        ARouter.getInstance().inject(this)
        //隐藏返回按钮
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowTitleEnabled(true)
        }

        //默认显示媒体库页面
        //标题
        title = "Media Library"
        //移除所有已添加的fragment，防止如旋转屏幕后导致的屏幕错乱
        supportFragmentManager.findAndRemoveFragment(
            TAG_FRAGMENT_HOME,
            TAG_FRAGMENT_MEDIA,
            TAG_FRAGMENT_PERSONAL
        )
        //切换到媒体库页面
        switchFragment(TAG_FRAGMENT_MEDIA)
        //底部导航栏设置选中
        dataBinding.navigationView.post {
            dataBinding.navigationView.selectedItemId = R.id.navigation_media
        }

        //设置底部导航栏事件
        dataBinding.navigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    title = "bounce play"
                    switchFragment(TAG_FRAGMENT_HOME)
                }
                R.id.navigation_media -> {
                    title = "Media Library"
                    switchFragment(TAG_FRAGMENT_MEDIA)
                }
                R.id.navigation_personal -> {
                    title = "personal center"
                    switchFragment(TAG_FRAGMENT_PERSONAL)
                }
            }
            return@setOnItemSelectedListener true
        }

        viewModel.initDatabase()
        viewModel.initCloudBlockData()

        initScreencastReceive()

        if (UserConfig.isUserLoggedIn()) {
            viewModel.reLogin()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (checkServiceExit()) {
                return true
            }

            if (System.currentTimeMillis() - touchTime > 1500) {
                ToastCenter.showToast("Press again to exit the app")
                touchTime = System.currentTimeMillis()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun getLoginLiveData(): MutableLiveData<LoginData> {
        return viewModel.reLoginLiveData
    }

    private fun switchFragment(tag: String) {
        //重复打开当前页面，不进行任何操作
        if (tag == fragmentTag) {
            return
        }

        //隐藏上一个布局，fragmentTag不为空代表上一个布局已存在
        if (fragmentTag.isNotEmpty()) {
            supportFragmentManager.hideFragment(previousFragment)
        }

        when (tag) {
            TAG_FRAGMENT_HOME -> {
                //根据TAG寻找页面
                val fragment = supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_HOME)
                if (fragment == null) {
                    //根据TAG无法找到页面，通过路由寻找页面，找到页面则添加
                    getFragment(RouteTable.Anime.HomeFragment)?.also {
                        addFragment(it, TAG_FRAGMENT_HOME)
                        homeFragment = it
                        previousFragment = it
                        fragmentTag = tag
                    }
                } else {
                    //根据TAG找到页面，显示
                    supportFragmentManager.showFragment(fragment)
                    homeFragment = fragment
                    previousFragment = fragment
                    fragmentTag = tag
                }
            }
            TAG_FRAGMENT_MEDIA -> {
                val fragment = supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_MEDIA)
                if (fragment == null) {
                    getFragment(RouteTable.Local.MediaFragment)?.also {
                        addFragment(it, TAG_FRAGMENT_MEDIA)
                        mediaFragment = it
                        previousFragment = it
                        fragmentTag = tag
                    }
                } else {
                    supportFragmentManager.showFragment(fragment)
                    mediaFragment = fragment
                    previousFragment = fragment
                    fragmentTag = tag
                }
            }
            TAG_FRAGMENT_PERSONAL -> {
                val fragment = supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_PERSONAL)
                if (fragment == null) {
                    getFragment(RouteTable.User.PersonalFragment)?.also {
                        addFragment(it, TAG_FRAGMENT_PERSONAL)
                        personalFragment = it
                        previousFragment = it
                        fragmentTag = tag
                    }
                } else {
                    supportFragmentManager.showFragment(fragment)
                    personalFragment = fragment
                    previousFragment = fragment
                    fragmentTag = tag
                }
            }
            else -> {
                throw RuntimeException("no match fragment")
            }
        }
    }

    private fun addFragment(fragment: Fragment, tag: String) {
        supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .add(R.id.fragment_container, fragment, tag)
            .commit()
    }

    private fun getFragment(path: String) =
        ARouter.getInstance()
            .build(path)
            .navigation() as Fragment?

    private fun initScreencastReceive() {
        if (ScreencastConfig.isStartReceiveWhenLaunch().not()) {
            return
        }
        if (receiveService.isRunning(this)) {
            return
        }

        var httpPort = ScreencastConfig.getReceiverPort()
        if (httpPort == 0) {
            httpPort = Random.nextInt(20000, 30000)
            ScreencastConfig.putReceiverPort(httpPort)
        }
        val receiverPwd = ScreencastConfig.getReceiverPassword()
        receiveService.startService(this, httpPort, receiverPwd)
    }

    private fun checkServiceExit(): Boolean {
        val isProvideServiceRunning = provideService.isRunning(this)
        val isReceiveServiceRunning = receiveService.isRunning(this)

        val serviceName = when {
            isProvideServiceRunning && isReceiveServiceRunning -> "Screencasting service"
            isProvideServiceRunning -> "Casting service"
            isReceiveServiceRunning -> "Screencast receiving service"
            else -> return false
        }

        CommonDialog.Builder(this).run {
            tips = "confirm exit？"
            content = "${serviceName}running，Exit will interrupt the casting"
            addNegative()
            addPositive("quit") {
                it.dismiss()
                provideService.stopService(this@MainActivity)
                receiveService.stopService(this@MainActivity)
                finish()
                exitProcess(0)
            }
            build()
        }.show()
        return true
    }
}