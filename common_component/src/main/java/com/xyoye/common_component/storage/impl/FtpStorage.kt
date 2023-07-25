package com.xyoye.common_component.storage.impl

import android.net.Uri
import com.xyoye.common_component.storage.AbstractStorage
import com.xyoye.common_component.storage.file.StorageFile
import com.xyoye.common_component.storage.file.helper.FtpPlayServer
import com.xyoye.common_component.storage.file.impl.FtpStorageFile
import com.xyoye.common_component.utils.IOUtils
import com.xyoye.common_component.weight.ToastCenter
import com.xyoye.data_component.entity.MediaLibraryEntity
import com.xyoye.data_component.entity.PlayHistoryEntity
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.InputStream

/**
 * Created by XYJ on 2023/1/16.
 */

class FtpStorage(library: MediaLibraryEntity) : AbstractStorage(library) {
    private val mFtpClient = FTPClient()
    private var playingInputStream: InputStream? = null

    override suspend fun listFiles(file: StorageFile): List<StorageFile> {
        //检测FTP通讯是否正常
        if (checkConnection().not()) {
            return emptyList()
        }
        //获取文件列表
        try {
            checkWorkDirectory()
            return mFtpClient.listFiles(file.filePath()).map {
                FtpStorageFile(this, file.filePath(), it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showErrorToast("Failed to get file list", e)
            close()
        }
        return emptyList()
    }

    override suspend fun getRootFile(): StorageFile {
        val ftpFile = FTPFile().apply {
            name = ""
            type = FTPFile.DIRECTORY_TYPE
        }
        return FtpStorageFile(this, "/", ftpFile)
    }

    override suspend fun openFile(file: StorageFile): InputStream? {
        if (file !is FtpStorageFile) {
            return null
        }
        if (checkConnection().not()) {
            return null
        }
        try {
            playingInputStream = mFtpClient.retrieveFileStream(file.filePath())
        } catch (e: Exception) {
            e.printStackTrace()
            close()
        }
        return playingInputStream
    }

    suspend fun openFile(file: StorageFile, offset: Long): InputStream? {
        if (offset > 0) {
            mFtpClient.restartOffset = offset
        }
        return openFile(file)
    }

    override suspend fun pathFile(path: String, isDirectory: Boolean): StorageFile? {
        if (checkConnection().not()) {
            return null
        }

        val pathUri = Uri.parse(path)
        val fileName = pathUri.lastPathSegment
        val parentPath = pathUri.path?.removeSuffix("/$fileName") ?: "/"

        try {
            val ftpFile = mFtpClient.mlistFile(path)
            return FtpStorageFile(this, parentPath, ftpFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val fileType = if (isDirectory) FTPFile.DIRECTORY_TYPE else FTPFile.FILE_TYPE
        val ftpFile = FTPFile().apply {
            name = fileName
            type = fileType
        }
        return FtpStorageFile(this, parentPath, ftpFile)
    }

    override suspend fun historyFile(history: PlayHistoryEntity): StorageFile? {
        val storagePath = history.storagePath ?: return null
        return pathFile(storagePath, false)?.also {
            it.playHistory = history
        }
    }

    override suspend fun createPlayUrl(file: StorageFile): String? {
        if (file !is FtpStorageFile) {
            return null
        }
        val playServer = FtpPlayServer.getInstance()
        val serverStarted = playServer.startSync()
        if (serverStarted.not()) {
            return null
        }
        return playServer.generatePlayUrl(this, file)
    }

    /**
     * 检查FTP通讯
     */
    private fun checkConnection(): Boolean {
        if (mFtpClient.isAvailable) {
            return true
        }

        try {
            mFtpClient.controlEncoding = library.ftpEncoding
            mFtpClient.connect(library.ftpAddress, library.port)
            if (checkLogin().not()) {
                showErrorToast("Login failed, please check account password")
                return false
            }
            if (library.isActiveFTP) {
                mFtpClient.enterLocalActiveMode()
            } else {
                mFtpClient.enterLocalPassiveMode()
            }
            mFtpClient.setFileType(FTP.BINARY_FILE_TYPE)
            mFtpClient.listHiddenFiles = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            showErrorToast("Failed to connect to FTP service", e)
            close()
        }
        return false
    }

    /**
     * 检查工作目录
     */
    private fun checkWorkDirectory(switch: Boolean = true) {
        // 当执行retrieveFileStream后，工作目录会发生变化
        // 检查工作目录是否为根目录，不是则切换至根目录
        val workDirectory = mFtpClient.printWorkingDirectory()
        if (workDirectory == "/") {
            return
        }

        // 切换目录失败，直接重置连接
        if (switch.not()) {
            close()
            checkConnection()
            return
        }

        try {
            mFtpClient.changeWorkingDirectory("/")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (switch) {
            checkWorkDirectory(false)
        }
    }

    private fun checkLogin(): Boolean {
        try {
            return if (library.isAnonymous) {
                mFtpClient.login("anonymous", "anonymous")
            } else {
                mFtpClient.login(library.account, library.password)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showErrorToast("Failed to log in to the FTP service", e)
            close()
        }
        return false
    }

    override suspend fun test(): Boolean {
        return checkConnection()
    }

    override fun close() {
        if (playingInputStream != null) {
            IOUtils.closeIO(playingInputStream)
            playingInputStream = null
        }

        try {
            mFtpClient.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun completePending() {
        if (playingInputStream == null) {
            return
        }

        IOUtils.closeIO(playingInputStream)
        playingInputStream = null

        try {
            if (!mFtpClient.completePendingCommand()) {
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            close()
        }
    }

    private fun showErrorToast(message: String, e: Exception? = null) {
        if (e == null) {
            ToastCenter.showError(message)
            return
        }
        ToastCenter.showError("$message: ${e.message}")
    }
}