package com.xyoye.local_component.ui.activities.play_history

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.xyoye.common_component.adapter.*
import com.xyoye.common_component.databinding.ItemStorageVideoBinding
import com.xyoye.common_component.databinding.ItemStorageVideoTagBinding
import com.xyoye.common_component.extension.*
import com.xyoye.common_component.utils.FastClickFilter
import com.xyoye.common_component.utils.PlayHistoryUtils
import com.xyoye.common_component.utils.formatDuration
import com.xyoye.common_component.utils.view.ItemDecorationOrientation
import com.xyoye.common_component.weight.BottomActionDialog
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.bean.SheetActionBean
import com.xyoye.data_component.bean.VideoTagBean
import com.xyoye.data_component.entity.PlayHistoryEntity
import com.xyoye.data_component.enums.MediaType
import com.xyoye.local_component.R
import java.io.File

/**
 * Created by xyoye on 2023/4/14
 */

class PlayHistoryAdapter(
    private val activity: PlayHistoryActivity,
    private val viewModel: PlayHistoryViewModel
) {

    private enum class EditHistory(val title: String, val icon: Int) {
        REMOVE_DANMU("Remove Danmu binding", R.drawable.ic_unbind_danmu),
        REMOVE_SUBTITLE("Remove subtitle binding", R.drawable.ic_unbind_subtitle),
        COPY_URL("copy playback link", R.drawable.ic_copy_url),
        DELETE_HISTORY("Delete playback records", R.drawable.ic_delete_history);

        fun toAction() = SheetActionBean(this, title, icon)
    }

    private val tagDecoration = ItemDecorationOrientation(5.dp(), 0, RecyclerView.HORIZONTAL)

    fun createAdapter(): BaseAdapter {
        return buildAdapter {

            setupDiffUtil {
                areItemsTheSame(isSameHistoryItem())
            }

            addEmptyView(R.layout.layout_empty) {
                initEmptyView {
                    itemBinding.emptyTv.text = "No play record"
                }
            }

            addItem(R.layout.item_storage_video) {
                initView(historyItem())
            }
        }
    }

    private fun isSameHistoryItem() = { old: Any, new: Any ->
        val oldItem = old as? PlayHistoryEntity
        val newItem = new as? PlayHistoryEntity
        oldItem?.uniqueKey == newItem?.uniqueKey && oldItem?.storageId == newItem?.storageId
    }

    private fun BaseViewHolderCreator<ItemStorageVideoBinding>.historyItem() =
        { data: PlayHistoryEntity ->
            itemBinding.coverIv.loadVideoCover(data.uniqueKey.toCoverFile())

            itemBinding.durationTv.text = getDuration(data)
            itemBinding.durationTv.isVisible = data.videoDuration > 0

            val isInvalid = isHistoryInvalid(data)
            val titleTextColor = if (isInvalid)
                R.color.text_gray
            else
                R.color.text_black

            itemBinding.titleTv.setTextColor(titleTextColor.toResColor(activity))
            itemBinding.titleTv.text = data.videoName

            setupVideoTag(itemBinding.tagRv, data)

            itemBinding.itemLayout.setOnClickListener {
                //防止快速点击
                if (FastClickFilter.isNeedFilter())
                    return@setOnClickListener

                if (isInvalid) {
                    ToastCenter.showError("The record has expired and cannot be played")
                    return@setOnClickListener
                }
                viewModel.openHistory(data)
            }

            itemBinding.moreActionIv.setOnClickListener {
                showEditDialog(data)
            }
            itemBinding.itemLayout.setOnLongClickListener {
                showEditDialog(data)
                return@setOnLongClickListener true
            }
        }

    private fun setupVideoTag(tagRv: RecyclerView, data: PlayHistoryEntity) {
        tagRv.apply {
            layoutManager = horizontal()
            adapter = buildAdapter {
                addItem(R.layout.item_storage_video_tag) { initView(tagItem()) }
            }
            removeItemDecoration(tagDecoration)
            addItemDecoration(tagDecoration)
            setData(generateVideoTags(data))
        }
    }

    private fun BaseViewHolderCreator<ItemStorageVideoTagBinding>.tagItem() =
        { data: VideoTagBean ->
            val background = R.drawable.background_video_tag.toResDrawable()
            background?.colorFilter = PorterDuffColorFilter(data.color, PorterDuff.Mode.SRC)
            itemBinding.textView.background = background
            itemBinding.textView.text = data.tag
        }

    private fun generateVideoTags(data: PlayHistoryEntity): List<VideoTagBean> {
        val tagList = mutableListOf<VideoTagBean>()
        if (data.danmuPath?.isNotEmpty() == true) {
            tagList.add(VideoTagBean("Barrage", R.color.theme.toResColor()))
        }
        if (data.subtitlePath?.isNotEmpty() == true) {
            tagList.add(VideoTagBean("subtitle", R.color.orange.toResColor()))
        }
        val progress = getProgress(data)
        if (progress.isNotEmpty()) {
            tagList.add(VideoTagBean(progress, R.color.black_alpha.toResColor()))
        }
        tagList.add(VideoTagBean(data.mediaType.storageName, R.color.black_alpha.toResColor()))
        tagList.add(VideoTagBean(PlayHistoryUtils.formatPlayTime(data.playTime), R.color.black_alpha.toResColor()))
        return tagList
    }

    private fun isHistoryInvalid(entity: PlayHistoryEntity): Boolean {
        return when (entity.mediaType) {
            MediaType.MAGNET_LINK -> {
                val torrentPath = entity.torrentPath
                //磁链种子文件丢失
                if (torrentPath.isNullOrEmpty() || entity.torrentIndex == -1) {
                    return true
                }
                val torrentFile = File(torrentPath)
                return !torrentFile.exists()
            }
            else -> entity.storageId == null
        }
    }

    private fun getProgress(data: PlayHistoryEntity): String {
        val position = data.videoPosition
        val duration = data.videoDuration
        if (position == 0L || duration == 0L) {
            return ""
        }

        var progress = (position * 100f / duration).toInt()
        if (progress == 0) {
            progress = 1
        }
        return "schedule $progress%"
    }

    private fun getDuration(data: PlayHistoryEntity): String {
        val position = data.videoPosition
        val duration = data.videoDuration
        return if (position > 0 && duration > 0) {
            "${formatDuration(position)}/${formatDuration(duration)}"
        } else if (duration > 0) {
            formatDuration(duration)
        } else {
            ""
        }
    }

    private fun showEditDialog(history: PlayHistoryEntity) {
        val actions = mutableListOf<SheetActionBean>()
        if (history.danmuPath.isNullOrEmpty().not()) {
            actions.add(EditHistory.REMOVE_DANMU.toAction())
        }
        if (history.subtitlePath.isNullOrEmpty().not()) {
            actions.add(EditHistory.REMOVE_SUBTITLE.toAction())
        }
        actions.add(EditHistory.COPY_URL.toAction())
        actions.add(EditHistory.DELETE_HISTORY.toAction())
        BottomActionDialog(activity, actions) {
            when (it.actionId) {
                EditHistory.REMOVE_DANMU -> viewModel.unbindDanmu(history)
                EditHistory.REMOVE_SUBTITLE -> viewModel.unbindSubtitle(history)
                EditHistory.DELETE_HISTORY -> viewModel.removeHistory(history)
                EditHistory.COPY_URL -> {
                    history.url.addToClipboard()
                    ToastCenter.showSuccess("link copied！")
                }
            }
            return@BottomActionDialog true
        }.show()
    }
}