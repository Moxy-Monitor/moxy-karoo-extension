package com.fortioridesign.moxykaroo.fit

import android.util.Log
import com.fortioridesign.moxykaroo.KarooMoxyMonitorExtension.Companion.TAG
import com.fortioridesign.moxykaroo.KarooSystemServiceProvider
import com.fortioridesign.moxykaroo.SensorBodyPosition
import com.fortioridesign.moxykaroo.ble.BleManager
import com.fortioridesign.moxykaroo.screens.getDeviceIndexLabel
import com.fortioridesign.moxykaroo.throttle
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DeveloperField
import io.hammerhead.karooext.models.FieldValue
import io.hammerhead.karooext.models.FitEffect
import io.hammerhead.karooext.models.WriteToRecordMesg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2

class FitFileWriter(private val emitter: Emitter<FitEffect>, private val bleManager: BleManager, private val karooSystem: KarooSystemServiceProvider) {
    /* fun writeDeviceInfoField(
        deviceIndex: Int,
        antDeviceId: Int?,
        sensorPosition: SensorBodyPosition?
    ): FieldValue {
        val field = DeveloperField(
            fieldDefinitionNumber = (2 + deviceIndex * 32).toShort(),
            fitBaseTypeId = 137,
            fieldName = "CIQ_device_info",
            units = "",
        )
        /*val testField = DeveloperField(
            fieldDefinitionNumber = (3 + deviceIndex * 32).toShort(),
            fitBaseTypeId = 136,
            fieldName = "CIQ_test_device_info",
            units = "%",
        ) */

        val value = ByteArray(8)
        value[0] = 31
        if (sensorPosition != null) {
            value[1] = sensorPosition.index
        }
        if (antDeviceId != null) {
            value[2] = (antDeviceId and 0xFF).toByte()
            value[3] = ((antDeviceId shr 8) and 0xFF).toByte()
            value[4] = ((antDeviceId shr 16) and 0xFF).toByte()
            value[5] = ((antDeviceId shr 24) and 0xFF).toByte()
        }
        value[6] = 76 // Moxy
        value[7] = 98 // BSX

        val doubleValue = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).getDouble(0)

        Log.i(
            TAG,
            "Writing device info for device index $deviceIndex (ANT ID: $antDeviceId, Body location: $sensorPosition) to FIT field ${field.fieldName} (${field.fieldDefinitionNumber}) with raw value ${
                value.joinToString(", ") { it.toUByte().toString() }
            }, double value $doubleValue"
        )

        return FieldValue(field, doubleValue)
    } */

    fun writeFitValue(
        deviceIndex: Int,
        sensorType: FitValueSensorType,
        antDeviceId: Int?,
        bodyLocation: SensorBodyPosition?,
        value: Double
    ): FieldValue {
        val indexName = getDeviceIndexLabel(deviceIndex)

        val sensorTypeName = when (sensorType) {
            FitValueSensorType.SMO2 -> "SmO2"
            FitValueSensorType.THB -> "THb"
        }

        val fieldDefinitionNumber = when (sensorType) {
            FitValueSensorType.SMO2 -> deviceIndex * 32
            FitValueSensorType.THB -> 1 + deviceIndex * 32
        }.toShort()

        val fieldUnits = when (sensorType) {
            FitValueSensorType.SMO2 -> "%"
            FitValueSensorType.THB -> "THb"
        }

        val bodyLocationName = bodyLocation?.displayName ?: "Unknown"

        val field = DeveloperField(
            fieldDefinitionNumber = fieldDefinitionNumber,
            fitBaseTypeId = 136,
            fieldName = "$indexName $sensorTypeName Sensor $antDeviceId on $bodyLocationName",
            units = fieldUnits,
        )

        Log.i(
            TAG,
            "Writing $sensorTypeName value for device index $deviceIndex (ANT ID: $antDeviceId, Body location: $bodyLocation) to FIT field ${field.fieldName} (${field.fieldDefinitionNumber}) with value $value"
        )

        return FieldValue(field, value)
    }

    init {
        val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        /* FIXME write device info values once possible with the SDK
        coroutineScope.launch {
            val deviceStatusFlow = bleManager.deviceConnectionStatus

            combine(
                karooSystem.karooSystemService.streamRideState(),
                deviceStatusFlow
            ) { rideState, deviceStates ->
                rideState to deviceStates
            }.map { (_, deviceStates) ->
                deviceStates.map { (scanResult, deviceState) ->
                    scanResult to Pair(deviceState.deviceIndex, deviceState.antId)
                }
            }.distinctUntilChanged().collect {
                val currentDevices = bleManager.deviceConnectionStatus.value
                Log.i(
                    TAG,
                    "Writing device info for ${currentDevices.size} connected devices to fit file"
                )

                val fitValues = currentDevices.map { (scanResult, device) ->
                    writeDeviceInfoField(device.deviceIndex, scanResult.antId, device.bodyPosition)
                }

                emitter.onNext(WriteToSessionMesg(fitValues))
            }
        } */

        coroutineScope.launch {
            bleManager.deviceConnectionStatus.throttle(500).collect { devices ->
                val fieldValues = buildList {
                    devices.forEach { (_, deviceStatus) ->
                        val wasRecentlyUpdated = deviceStatus.lastSensorDataReceivedAt?.isAfter(
                            Instant.now().minusSeconds(10)
                        ) == true

                        if (deviceStatus.smo2 != null && deviceStatus.antId != null && wasRecentlyUpdated) {
                            add(
                                writeFitValue(
                                    deviceStatus.deviceIndex,
                                    FitValueSensorType.SMO2,
                                    deviceStatus.antId,
                                    deviceStatus.bodyPosition,
                                    deviceStatus.smo2
                                )
                            )
                        }

                        if (deviceStatus.thb != null && deviceStatus.antId != null && wasRecentlyUpdated) {
                            add(
                                writeFitValue(
                                    deviceStatus.deviceIndex,
                                    FitValueSensorType.THB,
                                    deviceStatus.antId,
                                    deviceStatus.bodyPosition,
                                    deviceStatus.thb
                                )
                            )
                        }
                    }
                }

                if (fieldValues.isNotEmpty()) {
                    Log.d(
                        TAG,
                        "Writing ${fieldValues.size} sensor data field values to fit file: $fieldValues"
                    )
                    emitter.onNext(WriteToRecordMesg(fieldValues))
                }

            }
        }

        emitter.setCancellable {
            coroutineScope.cancel()
        }
    }
}