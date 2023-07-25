package com.xyoye.user_component.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.xyoye.common_component.config.DanmuConfig
import com.xyoye.data_component.enums.DanmakuLanguage
import com.xyoye.user_component.R

/**
 * Created by xyoye on 2021/2/6.
 */

class DanmuSettingFragment : PreferenceFragmentCompat() {

    companion object {
        fun newInstance() = DanmuSettingFragment()

        private val languageData = mapOf(
            Pair("Original", DanmakuLanguage.ORIGINAL.value.toString()),
            Pair("Simplified Chinese", DanmakuLanguage.SC.value.toString()),
            Pair("Traditional Chinese", DanmakuLanguage.TC.value.toString())
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = DanmuSettingDataStore()
        addPreferencesFromResource(R.xml.preference_danmu_setting)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        findPreference<SwitchPreference>("show_dialog_before_play")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                findPreference<SwitchPreference>("auto_launch_danmu_before_play")?.isVisible =
                    !(newValue as Boolean)
                return@setOnPreferenceChangeListener true
            }

            findPreference<SwitchPreference>("auto_launch_danmu_before_play")?.isVisible =
                !isChecked
        }
        //播放器类型
        findPreference<ListPreference>("danmu_language")?.apply {
            entries = languageData.keys.toTypedArray()
            entryValues = languageData.values.toTypedArray()
            summary = "Current configuration: $entry, specify the language of the barrage when playing"
            setOnPreferenceChangeListener { _, newValue ->
                val key = languageData.entries.first { it.value == newValue }.key
                summary = "Current configuration: $key, specify the language of the barrage when playing"
                return@setOnPreferenceChangeListener true
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }


    inner class DanmuSettingDataStore : PreferenceDataStore() {
        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (key) {
                "auto_load_same_name_danmu" -> DanmuConfig.isAutoLoadSameNameDanmu()
                "auto_match_danmu" -> DanmuConfig.isAutoMatchDanmu()
                "danmu_update_in_choreographer" -> DanmuConfig.isDanmuUpdateInChoreographer()
                "danmu_cloud_block" -> DanmuConfig.isCloudDanmuBlock()
                "danmu_debug" -> DanmuConfig.isDanmuDebug()
                else -> super.getBoolean(key, defValue)
            }
        }

        override fun putBoolean(key: String?, value: Boolean) {
            when (key) {
                "auto_load_same_name_danmu" -> DanmuConfig.putAutoLoadSameNameDanmu(value)
                "auto_match_danmu" -> DanmuConfig.putAutoMatchDanmu(value)
                "danmu_update_in_choreographer" -> DanmuConfig.putDanmuUpdateInChoreographer(value)
                "danmu_cloud_block" -> DanmuConfig.putCloudDanmuBlock(value)
                "danmu_debug" -> DanmuConfig.putDanmuDebug(value)
                else -> super.putBoolean(key, value)
            }
        }

        override fun getString(key: String?, defValue: String?): String? {
            return when (key) {
                "danmu_language" -> DanmuConfig.getDanmuLanguage().toString()
                else -> super.getString(key, defValue)
            }
        }

        override fun putString(key: String?, value: String?) {
            when (key) {
                "danmu_language" -> DanmuConfig.putDanmuLanguage(value?.toInt() ?: 0)
                else -> super.putString(key, value)
            }
        }
    }
}