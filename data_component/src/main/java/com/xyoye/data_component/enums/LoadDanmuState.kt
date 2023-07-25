package com.xyoye.data_component.enums

/**
 * Created by xyoye on 2022/1/2.
 */

enum class LoadDanmuState(val msg: String) {
    NOT_SUPPORTED("Does not support automatic matching barrage, please load it manually"),

    COLLECTING("Collecting the data required for automatic matching barrage"),

    MATCHING("Automatically matching barrage"),

    NO_MATCHED("Not matched to the barrage, please load it manually"),

    MATCH_SUCCESS("Match barrage successfully"),

    NO_MATCH_REQUIRE("No need to match barrage")
}