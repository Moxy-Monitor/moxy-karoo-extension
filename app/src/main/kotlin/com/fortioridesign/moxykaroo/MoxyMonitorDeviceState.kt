package com.fortioridesign.moxykaroo

import io.hammerhead.karooext.models.BatteryStatus
import java.time.Instant

data class MoxyMonitorDeviceState(
    val name: String?,
    val deviceIndex: Int,
    val antId: Int?,
    val smo2: Double? = null,
    val thb: Double? = null,
    val connectionState: MoxyMonitorConnectionState = MoxyMonitorConnectionState.DISCONNECTED,
    val bodyPosition: SensorBodyPosition = SensorBodyPosition.UNKONWN,
    val batteryLevel: BatteryStatus? = null,
    val lastSensorDataReceivedAt: Instant? = null
)