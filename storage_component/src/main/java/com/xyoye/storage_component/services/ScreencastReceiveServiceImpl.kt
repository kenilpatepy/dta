package com.xyoye.storage_component.services

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.services.ScreencastReceiveService

/**
 * Created by xyoye on 2022/9/15
 */

@Route(path = RouteTable.Stream.ScreencastReceive, name = "Casting content receiving service")
class ScreencastReceiveServiceImpl : ScreencastReceiveService {

    override fun init(context: Context?) {

    }

    override fun isRunning(context: Context): Boolean {
        return com.xyoye.storage_component.services.ScreencastReceiveService.isRunning(context)
    }

    override fun stopService(context: Context) {
        com.xyoye.storage_component.services.ScreencastReceiveService.stop(context)
    }

    override fun startService(context: Context, port: Int, password: String?) {
        com.xyoye.storage_component.services.ScreencastReceiveService.start(context, port, password)
    }
}