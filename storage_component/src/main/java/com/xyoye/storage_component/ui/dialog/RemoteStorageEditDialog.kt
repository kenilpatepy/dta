package com.xyoye.storage_component.ui.dialog

import com.xyoye.common_component.application.DanDanPlay
import com.xyoye.common_component.extension.setTextColorRes
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.data.RemoteScanData
import com.xyoye.data_component.entity.MediaLibraryEntity
import com.xyoye.data_component.enums.MediaType
import com.xyoye.storage_component.R
import com.xyoye.storage_component.databinding.DialogRemoteLoginBinding
import com.xyoye.storage_component.ui.activities.storage_plus.StoragePlusActivity
import com.xyoye.storage_component.utils.launcher.ScanActivityLauncher

/**
 * Created by xyoye on 2021/3/25.
 */

class RemoteStorageEditDialog(
    private val activity: StoragePlusActivity,
    private val originalStorage: MediaLibraryEntity?,
) : StorageEditDialog<DialogRemoteLoginBinding>(activity) {

    private var remoteData: MediaLibraryEntity
    private var tokenRequired = false

    private lateinit var binding: DialogRemoteLoginBinding

    private val scanActivityLauncher = ScanActivityLauncher(activity, onResult())

    init {
        remoteData = originalStorage ?: MediaLibraryEntity(
            0,
            "",
            "",
            MediaType.REMOTE_STORAGE,
            port = 80
        )
    }

    override fun getChildLayoutId() = R.layout.dialog_remote_login

    override fun initView(binding: DialogRemoteLoginBinding) {
        this.binding = binding
        val isEditStorage = originalStorage != null

        setTitle(if (isEditStorage) "Edit Remote Connection Account" else "Add Remote Connection Account")

        binding.remoteData = remoteData

        setGroupMode(remoteData.remoteAnimeGrouping)

        binding.scanLl.setOnClickListener {
            DanDanPlay.permission.camera.request(activity) {
                onGranted {
                    scanActivityLauncher.launch()
                }
                onDenied {
                    ToastCenter.showError("Failed to obtain camera permission, unable to scan code")                }
            }
        }

        binding.serverTestConnectTv.setOnClickListener {
            if (checkParams(remoteData)) {
                activity.testStorage(remoteData)
            }
        }

        binding.tvGroupByFile.setOnClickListener {
            remoteData.remoteAnimeGrouping = false
            setGroupMode(false)
        }

        binding.tvGroupByAnime.setOnClickListener {
            remoteData.remoteAnimeGrouping = true
            setGroupMode(true)
        }

        setPositiveListener {
            if (checkParams(remoteData)) {
                if (remoteData.displayName.isEmpty()) {
                    remoteData.displayName = "remote media library"
                }
                remoteData.describe = "http://${remoteData.url}:${remoteData.port}"
                activity.addStorage(remoteData)
            }
        }

        setNegativeListener {
            activity.finish()
        }
    }

    override fun onTestResult(result: Boolean) {
        if (result) {
            binding.serverStatusTv.text = "connection succeeded"
            binding.serverStatusTv.setTextColorRes(R.color.text_blue)
        } else {
            binding.serverStatusTv.text = "Connection failed"
            binding.serverStatusTv.setTextColorRes(R.color.text_red)
        }
    }

    private fun checkParams(remoteData: MediaLibraryEntity): Boolean {
        if (remoteData.url.isEmpty()) {
            ToastCenter.showWarning("Please fill in the IP address")
            return false
        }

        if (tokenRequired && remoteData.remoteSecret.isNullOrEmpty()) {
            ToastCenter.showWarning("Please fill in the API key")
            return false
        }
        return true
    }

    private fun setGroupMode(isGroupByAnime: Boolean) {
        binding.tvGroupByAnime.isSelected = isGroupByAnime
        binding.tvGroupByAnime.setTextColorRes(
            if (isGroupByAnime) R.color.text_white else R.color.text_black
        )

        binding.tvGroupByFile.isSelected = !isGroupByAnime
        binding.tvGroupByFile.setTextColorRes(
            if (!isGroupByAnime) R.color.text_white else R.color.text_black
        )
    }

    private fun onResult() = block@{ data: RemoteScanData? ->
        if (data == null) {
            return@block
        }
        remoteData.url = data.selectedIP ?: ""
        remoteData.port = data.port
        remoteData.displayName = data.machineName ?: ""
        tokenRequired = data.tokenRequired
        binding.remoteData = remoteData
    }
}