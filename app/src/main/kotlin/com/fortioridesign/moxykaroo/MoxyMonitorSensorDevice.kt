package com.fortioridesign.moxykaroo

import android.annotation.SuppressLint
import android.util.Log
import com.fortioridesign.moxykaroo.ble.BleConsts
import com.fortioridesign.moxykaroo.ble.BleConsts.BATTERY_LEVEL_CHARACTERISTIC_UUID
import com.fortioridesign.moxykaroo.ble.BleConsts.BTS_SERVICE_UUID
import com.fortioridesign.moxykaroo.ble.BleConsts.DIS_SERVICE_UUID
import com.fortioridesign.moxykaroo.ble.BleConsts.MANUFACTURER_NAME_CHARACTERISTIC_UUID
import com.fortioridesign.moxykaroo.ble.BleConsts.MODEL_NUMBER_CHARACTERISTIC_UUID
import com.fortioridesign.moxykaroo.ble.BleConsts.SERIAL_NUMBER_CHARACTERISTIC_UUID
import com.fortioridesign.moxykaroo.ble.BleManager
import com.fortioridesign.moxykaroo.ble.ScanResult
import com.fortioridesign.moxykaroo.datatypes.Smo2DataType
import com.fortioridesign.moxykaroo.datatypes.ThbDataType
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.BatteryStatus
import io.hammerhead.karooext.models.ConnectionStatus
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.Device
import io.hammerhead.karooext.models.DeviceEvent
import io.hammerhead.karooext.models.ManufacturerInfo
import io.hammerhead.karooext.models.OnBatteryStatus
import io.hammerhead.karooext.models.OnConnectionStatus
import io.hammerhead.karooext.models.OnDataPoint
import io.hammerhead.karooext.models.OnManufacturerInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.time.Instant

class MoxyMonitorSensorDevice(
    private val bleManager: BleManager,
    private val extension: String,
    private val deviceIdentifier: ScanResult,
) {
    val source = Device(extension,
        encodeJson(deviceIdentifier),
        buildList {
            for (i in 0..3) {
                add(Smo2DataType(extension, i).dataTypeId)
                add(ThbDataType(extension, i).dataTypeId)
            }
        },
        deviceIdentifier.name ?: "Moxy Monitor")

    val smo2DataTypeIds = buildList {
        for (i in 0..3) {
            add(Smo2DataType(extension, i).dataTypeId)
        }
    }

    val thbDataTypeIds = buildList {
        for (i in 0..3) {
            add(ThbDataType(extension, i).dataTypeId)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    fun connect(emitter: Emitter<DeviceEvent>, deviceIndex: Int) {
        val smo2DataTypeId = Smo2DataType(extension, deviceIndex).dataTypeId
        val thbDataTypeId = ThbDataType(extension, deviceIndex).dataTypeId
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val encodedDeviceIdentifier = encodeJson(deviceIdentifier)

        scope.launch {
            bleManager.connect(deviceIdentifier)
                .flatMapLatest { peripheral ->
                    peripheral?.let {
                        val connected = flowOf(OnConnectionStatus(ConnectionStatus.CONNECTED))
                        val sensorDataChanges = bleManager.observeCharacteristic(peripheral, BleConsts.SMO2_SERVICE, BleConsts.SMO2_CHARACTERISTIC) { data ->
                            if (data.size < 8) {
                                Log.w(KarooMoxyMonitorExtension.TAG, "Received invalid sensor data of length ${data.size}")
                                return@observeCharacteristic null
                            }

                            val smo2Concentration = ((data[3].toInt() and 0xFF) shl 8) or (data[2].toInt() and 0xFF)
                            // val previousSmo2Concentration = ((data[5].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
                            val thbConcentration = ((data[7].toInt() and 0xFF) shl 8) or (data[6].toInt() and 0xFF)

                            Pair(smo2Concentration / 10.0, thbConcentration / 100.0)
                        }.filterNotNull().flatMapLatest { (smo2, thb) ->
                            Log.i(KarooMoxyMonitorExtension.TAG, "Received sensor data on $deviceIndex: SmO2=$smo2%, THb=$thb g/dL")

                            data class DeviceScanResultAndStatus(
                                val scanResult: ScanResult,
                                val device: MoxyMonitorDeviceState,
                            )

                            val otherDeviceValues = bleManager.deviceConnectionStatus.value.filter { (scanResult, _) ->
                                scanResult != deviceIdentifier
                            }.map { (scanResult, device) ->
                                DeviceScanResultAndStatus(scanResult, device)
                            }.associateBy { it.device.deviceIndex }

                            val otherDeviceValuesDataPoints = buildList {
                                for (i in 0..3) {
                                    if (i == deviceIndex) continue

                                    val device = otherDeviceValues[i]?.device

                                    val wasRecentlyUpdated = device?.lastSensorDataReceivedAt?.isAfter(Instant.now().minusSeconds(10)) == true

                                    val smo2Value = if (device?.smo2 != null && wasRecentlyUpdated) {
                                        device.smo2
                                    } else {
                                        0.0
                                    }

                                    add(OnDataPoint(DataPoint(
                                        dataTypeId = smo2DataTypeIds[i],
                                        values = mapOf(DataType.Field.SINGLE to smo2Value),
                                        sourceId = encodedDeviceIdentifier,
                                    )))

                                    val thbValue = if (device?.thb != null && wasRecentlyUpdated) {
                                        device.thb
                                    } else {
                                        0.0
                                    }

                                    add(OnDataPoint(DataPoint(
                                        dataTypeId = thbDataTypeIds[i],
                                        values = mapOf(DataType.Field.SINGLE to thbValue),
                                        sourceId = encodedDeviceIdentifier,
                                    )))
                                }
                            }

                            flow {
                                otherDeviceValuesDataPoints.forEach { emit(it) }
                                emit(OnDataPoint(
                                    DataPoint(
                                        dataTypeId = smo2DataTypeId,
                                        values = mapOf(DataType.Field.SINGLE to smo2),
                                        sourceId = source.uid,
                                    ),
                                ))
                                emit(OnDataPoint(
                                    DataPoint(
                                        dataTypeId = thbDataTypeId,
                                        values = mapOf(DataType.Field.SINGLE to thb),
                                        sourceId = source.uid,
                                    ),
                                ))
                            }
                        }

                        // Read initial battery level
                        val initialBatteryLevelFlow = try {
                            bleManager.readCharacteristic(peripheral, BTS_SERVICE_UUID, BATTERY_LEVEL_CHARACTERISTIC_UUID) { data ->
                                if (data.isNotEmpty()) data[0].toInt() and 0xFF else -1
                            }.let { percent ->
                                Log.i(KarooMoxyMonitorExtension.TAG, "Initial battery level: $percent%")

                                if (percent != null) {
                                    flowOf(OnBatteryStatus(BatteryStatus.fromPercentage(percent)))
                                } else {
                                    emptyFlow()
                                }
                            }
                        } catch (t: Throwable){
                            Log.w(KarooMoxyMonitorExtension.TAG, "Failed to read initial battery level", t)

                            emptyFlow()
                        }

                        // Subscribe to battery level changes
                        val batteryChanges = bleManager.observeCharacteristic(peripheral, BTS_SERVICE_UUID, BATTERY_LEVEL_CHARACTERISTIC_UUID) { data ->
                            if (data.isNotEmpty()) data[0].toInt() and 0xFF else -1
                        }.map { percent ->
                            OnBatteryStatus(BatteryStatus.fromPercentage(percent))
                        }
                        val manufacturerInfo = flow {
                            val manufacturer =
                                bleManager.readCharacteristic(peripheral, DIS_SERVICE_UUID, MANUFACTURER_NAME_CHARACTERISTIC_UUID) { data ->
                                    data.decodeToString()
                                }
                            val serialNumber = bleManager.readCharacteristic(peripheral, DIS_SERVICE_UUID, SERIAL_NUMBER_CHARACTERISTIC_UUID) { data ->
                                data.decodeToString()
                            }
                            val modelNumber = bleManager.readCharacteristic(peripheral, DIS_SERVICE_UUID, MODEL_NUMBER_CHARACTERISTIC_UUID) { data ->
                                data.decodeToString()
                            }
                            emit(OnManufacturerInfo(ManufacturerInfo(manufacturer, serialNumber, modelNumber)))
                        }
                        merge(connected, sensorDataChanges, initialBatteryLevelFlow, batteryChanges, manufacturerInfo)
                    } ?: run {
                        flowOf(OnConnectionStatus(ConnectionStatus.SEARCHING))
                    }
                }.collect { event ->
                    when (event) {
                        is OnConnectionStatus -> {
                            bleManager.updateDeviceState(deviceIdentifier) { currentState ->
                                currentState?.copy(connectionState = event.status.mapToMoxyMonitorConnectionState())
                            }
                        }

                        is OnDataPoint -> {
                            bleManager.updateDeviceState(deviceIdentifier) { currentState ->
                                when(event.dataPoint.dataTypeId) {
                                    smo2DataTypeId -> {
                                        currentState?.copy(smo2 = event.dataPoint.singleValue, lastSensorDataReceivedAt = Instant.now())
                                    }

                                    thbDataTypeId -> {
                                        currentState?.copy(thb = event.dataPoint.singleValue, lastSensorDataReceivedAt = Instant.now())
                                    }

                                    else -> currentState
                                }
                            }
                        }

                        is OnBatteryStatus -> {
                            bleManager.updateDeviceState(deviceIdentifier) { currentState ->
                                currentState?.copy(batteryLevel = event.status)
                            }
                        }

                        else -> {}
                    }

                    emitter.onNext(event)
                }
        }
        emitter.setCancellable {
            scope.cancel()
        }
    }
}