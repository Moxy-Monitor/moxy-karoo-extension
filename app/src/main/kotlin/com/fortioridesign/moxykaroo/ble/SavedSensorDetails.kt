package com.fortioridesign.moxykaroo.ble

import com.fortioridesign.moxykaroo.SensorBodyPosition
import io.hammerhead.karooext.models.BatteryStatus
import kotlinx.serialization.Serializable

@Serializable
data class SavedSensorDetails(
    val scanResult: ScanResult,
    val bodyPosition: SensorBodyPosition,
    val lastKnownBatteryLevel: BatteryStatus?,
)