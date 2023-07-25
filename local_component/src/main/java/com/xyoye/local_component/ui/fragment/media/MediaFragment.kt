package com.xyoye.local_component.ui.fragment.media

import androidx.core.view.isVisible
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.xyoye.common_component.adapter.addItem
import com.xyoye.common_component.adapter.buildAdapter
import com.xyoye.common_component.application.DanDanPlay
import com.xyoye.common_component.base.BaseFragment
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.extension.deletable
import com.xyoye.common_component.extension.setData
import com.xyoye.common_component.extension.vertical
import com.xyoye.common_component.services.ScreencastProvideService
import com.xyoye.common_component.weight.BottomActionDialog
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.common_component.weight.dialog.CommonDialog
import com.xyoye.data_component.bean.SheetActionBean
import com.xyoye.data_component.entity.MediaLibraryEntity
import com.xyoye.data_component.enums.MediaType
import com.xyoye.local_component.BR
import com.xyoye.local_component.R
import com.xyoye.local_component.databinding.FragmentMediaBinding
import com.xyoye.local_component.databinding.ItemMediaLibraryBinding

/**
 * Created by xyoye on 2020/7/27.
 */

@Route(path = RouteTable.Local.MediaFragment)
class MediaFragment : BaseFragment<MediaViewModel, FragmentMediaBinding>() {

    @Autowired
    lateinit var provideService: ScreencastProvideService

    override fun initViewModel() = ViewModelInit(
        BR.viewModel,
        MediaViewModel::class.java
    )

    override fun getLayoutId() = R.layout.fragment_media

    override fun initView() {
        ARouter.getInstance().inject(this)

        viewModel.initLocalStorage()

        initRv()

        dataBinding.addMediaStorageBt.setOnClickListener {
            showAddStorageDialog()
        }

        viewModel.mediaLibWithStatusLiveData.observe(this) {
            dataBinding.mediaLibRv.setData(it)
        }
    }

    private fun initRv() {
        dataBinding.mediaLibRv.apply {
            layoutManager = vertical()

            adapter = buildAdapter {
                addItem<MediaLibraryEntity, ItemMediaLibraryBinding>(R.layout.item_media_library) {
                    initView { data, _, _ ->
                        itemBinding.apply {
                            libraryNameTv.text = data.displayName
                            libraryUrlTv.text = when (data.mediaType) {
                                MediaType.STREAM_LINK,
                                MediaType.MAGNET_LINK,
                                MediaType.REMOTE_STORAGE,
                                MediaType.SMB_SERVER,
                                MediaType.EXTERNAL_STORAGE -> data.describe
                                else -> data.url
                            }
                            libraryCoverIv.setImageResource(data.mediaType.cover)

                            screencastStatusTv.isVisible =
                                data.mediaType == MediaType.SCREEN_CAST && data.running
                            screencastStatusTv.setOnClickListener {
                                showStopServiceDialog()
                            }

                            itemLayout.setOnClickListener {
                                DanDanPlay.permission.storage.request(this@MediaFragment) {
                                    onGranted {
                                        launchMediaStorage(data)
                                    }
                                    onDenied {
                                        ToastCenter.showError("Failed to obtain file read permission, unable to open media library")                                    }
                                }
                            }
                            itemLayout.setOnLongClickListener {
                                if (data.mediaType.deletable) {
                                    showManageStorageDialog(data)
                                }
                                true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun launchMediaStorage(data: MediaLibraryEntity) {
        when (data.mediaType) {
            MediaType.STREAM_LINK, MediaType.MAGNET_LINK, MediaType.OTHER_STORAGE -> {
                ARouter.getInstance()
                    .build(RouteTable.Local.PlayHistory)
                    .withSerializable("typeValue", data.mediaType.value)
                    .navigation()
            }
            MediaType.SCREEN_CAST -> {
                viewModel.checkScreenDeviceRunning(data)
            }
            MediaType.LOCAL_STORAGE,
            MediaType.FTP_SERVER,
            MediaType.SMB_SERVER,
            MediaType.WEBDAV_SERVER,
            MediaType.REMOTE_STORAGE,
            MediaType.EXTERNAL_STORAGE -> {
                ARouter.getInstance()
                    .build(RouteTable.Stream.StorageFile)
                    .withParcelable("storageLibrary", data)
                    .navigation()
            }
        }
    }

    private fun showAddStorageDialog() {
        val actionList = MediaType.values()
            .filter { it.deletable }
            .map { it.toAction() }

        BottomActionDialog(
            requireActivity(),
            actionList,
            "New Web Media Library"
        ) {
            val mediaType = it.actionId as MediaType
            ARouter.getInstance()
                .build(RouteTable.Stream.StoragePlus)
                .withSerializable("mediaType", mediaType)
                .navigation()
            return@BottomActionDialog true
        }.show()
    }

    private fun showManageStorageDialog(data: MediaLibraryEntity) {
        val actions = mutableListOf<SheetActionBean>()
        actions.add(ManageStorage.Edit.toAction())
        actions.add(ManageStorage.Delete.toAction())

        BottomActionDialog(requireActivity(), actions) {
            if (it.actionId == ManageStorage.Edit) {
                ARouter.getInstance()
                    .build(RouteTable.Stream.StoragePlus)
                    .withSerializable("mediaType", data.mediaType)
                    .withParcelable("editData", data)
                    .navigation()
            } else if (it.actionId == ManageStorage.Delete) {
                showDeleteStorageDialog(data)
            }
            return@BottomActionDialog true
        }.show()
    }

    private fun showDeleteStorageDialog(data: MediaLibraryEntity) {
        CommonDialog.Builder(requireActivity())
            .apply {
                content = "Are you sure you want to delete the following library?\n\n${data.displayName}"
                positiveText = "Confirm"
                addPositive { dialog ->
                    dialog.dismiss()
                    viewModel.deleteStorage(data)
                }
                addNegative()
            }.build().show()
    }

    private fun showStopServiceDialog() {
        CommonDialog.Builder(requireActivity())
            .apply {
                content = "Are you sure to stop the screencasting service?"
                positiveText = "Confirm"
                addPositive { dialog ->
                    dialog.dismiss()
                    provideService.stopService(requireActivity())
                }
                addNegative()
            }.build().show()
    }

    private enum class ManageStorage(val title: String, val icon: Int) {
        Edit("Edit Media Library", R.drawable.ic_edit_storage),
        Delete("Delete Media Library", R.drawable.ic_delete_storage);

        fun toAction() = SheetActionBean(this, title, icon)
    }
}