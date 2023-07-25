package com.xyoye.anime_component.ui.activities.anime_detail

import android.graphics.Color
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.gyf.immersionbar.ImmersionBar
import com.xyoye.anime_component.BR
import com.xyoye.anime_component.R
import com.xyoye.anime_component.databinding.ActivityAnimeDetailBinding
import com.xyoye.anime_component.ui.fragment.anime_episode.AnimeEpisodeFragment
import com.xyoye.anime_component.ui.fragment.anime_intro.AnimeIntroFragment
import com.xyoye.anime_component.ui.fragment.anime_recommend.AnimeRecommendFragment
import com.xyoye.common_component.base.BaseActivity
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.extension.*
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.data.BangumiData
import kotlin.math.abs
import kotlin.math.max

@Route(path = RouteTable.Anime.AnimeDetail)
class AnimeDetailActivity : BaseActivity<AnimeDetailViewModel, ActivityAnimeDetailBinding>() {

    @Autowired
    @JvmField
    var animeId: Int = -1

    override fun initViewModel() = ViewModelInit(
        BR.viewModel,
        AnimeDetailViewModel::class.java
    )

    override fun getLayoutId() = R.layout.activity_anime_detail

    override fun initStatusBar() {
        ImmersionBar.with(this)
            .transparentBar()
            .statusBarDarkFont(false)
            .init()
    }

    override fun initView() {
        ARouter.getInstance().inject(this)

        postponeEnterTransition()

        title = ""

        dataBinding.toolbar.setNavigationOnClickListener { finishAfterTransition() }

        if (animeId == -1) {
            ToastCenter.showError("Failed to get drama information")
            return
        }

        dataBinding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            //用于计算的偏移及高度，从1/2位置开始计算
            val calcRange = appBarLayout.totalScrollRange / 2f
            val calcOffset = max(0f, abs(verticalOffset) - calcRange)
            val offsetPercent = calcOffset / calcRange

            //返回图标颜色
            val color = getBackIconColor(offsetPercent)
            val colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    color,
                    BlendModeCompat.SRC_IN
                )
            dataBinding.toolbar.navigationIcon?.colorFilter = colorFilter

            //标题颜色
            val alpha = (255 * offsetPercent).toInt()
            val titleTextColor = R.color.text_theme.toResColor()
            val titleColor = Color.argb(
                alpha,
                Color.red(titleTextColor),
                Color.green(titleTextColor),
                Color.blue(titleTextColor)
            )
            dataBinding.toolbar.setTitleTextColor(titleColor)
            dataBinding.followTv.background?.alpha = 255 - alpha

            //状态栏文字颜色
            //tips: MIUI深色模式下状态栏字体颜色不受此控制
            val isDarkFont = calcOffset > 0 && !isNightMode()
            ImmersionBar.with(this)
                .transparentBar()
                .statusBarDarkFont(isDarkFont)
                .init()
        }

        dataBinding.tabLayout.setupWithViewPager(dataBinding.viewpager)

        initObserver()

        viewModel.animeIdField.set(animeId.toString())
        viewModel.getAnimeDetail(animeId.toString())
    }

    private fun getBackIconColor(percent: Float): Int {
        //颜色由白->蓝变化
        val startColor = R.color.text_white_immutable.toResColor()
        val endColor = R.color.text_theme.toResColor()

        val startRed = Color.red(startColor)
        val startGreen = Color.green(startColor)
        val startBlue = Color.blue(startColor)

        val endRed = Color.red(endColor)
        val endGreen = Color.green(endColor)
        val endBlue = Color.blue(endColor)

        //红绿蓝统一变化即得到相应比例颜色
        val diffRed = startRed + ((endRed - startRed) * percent).toInt()
        val diffGreen = startRed + ((endGreen - startGreen) * percent).toInt()
        val diffBlue = startRed + ((endBlue - startBlue) * percent).toInt()

        return Color.rgb(diffRed, diffGreen, diffBlue)
    }

    private fun initObserver() {

        viewModel.followLiveData.observe(this) { followed ->
            if (followed) {
                dataBinding.followTv.text = "Has been chased"
                dataBinding.followTv.isSelected = true
                dataBinding.followTv.setTextColorRes(R.color.text_theme)
            } else {
                dataBinding.followTv.text = "serial number"
                dataBinding.followTv.isSelected = false
                dataBinding.followTv.setTextColorRes(R.color.text_orange)
            }
        }

        viewModel.animeDetailLiveData.observe(this) { bangumiData ->
            bangumiData.apply {
                dataBinding.collapsingToolbarLayout.title = animeTitle

                dataBinding.backgroundCoverIv.loadImage(imageUrl)

                dataBinding.coverIv.loadImageWithCallback(
                    source = imageUrl,
                    dpRadius = 3f,
                    errorRes = R.drawable.ic_load_image_failed
                ) { supportStartPostponedEnterTransition() }

                dataBinding.viewpager.apply {
                    adapter = AnimeDetailAdapter(supportFragmentManager, bangumiData)
                    offscreenPageLimit = 2
                    currentItem = 0
                }
            }

        }

        viewModel.transitionFailedLiveData.observe(this) {
            finishAfterTransition()
        }
    }

    inner class AnimeDetailAdapter(
        fragmentManager: FragmentManager,
        private val bangumiData: BangumiData
    ) : FragmentPagerAdapter(
        fragmentManager,
        BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {
        private var titles = if (bangumiData.similars.size == 0 && bangumiData.relateds.size == 0) {
            arrayOf("information", "episode")
        } else {
            arrayOf("Information", "Series", "Recommendation")
        }

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> AnimeIntroFragment.newInstance(bangumiData)
                1 -> AnimeEpisodeFragment.newInstance(bangumiData)
                2 -> AnimeRecommendFragment.newInstance(bangumiData)
                else -> AnimeIntroFragment.newInstance(bangumiData)
            }
        }

        override fun getCount() = titles.size

        override fun getPageTitle(position: Int): CharSequence {
            return titles[position]
        }
    }
}