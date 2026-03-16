package com.fortioridesign.moxykaroo

import io.hammerhead.karooext.models.BatteryStatus

fun getBatteryLevelLabel(status: BatteryStatus?): String {
    return when (status) {
        BatteryStatus.NEW -> "NEW"
        BatteryStatus.GOOD -> "GOOD"
        BatteryStatus.OK -> "OK"
        BatteryStatus.LOW -> "LOW"
        BatteryStatus.CRITICAL -> "CRIT"
        else -> "?"
    }
}