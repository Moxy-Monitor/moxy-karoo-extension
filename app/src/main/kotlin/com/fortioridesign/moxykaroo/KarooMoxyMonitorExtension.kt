package com.fortioridesign.moxykaroo

import android.util.Log
import com.fortioridesign.moxykaroo.ble.BleManager
import com.fortioridesign.moxykaroo.ble.ScanResult
import com.fortioridesign.moxykaroo.datatypes.Smo2DataType
import com.fortioridesign.moxykaroo.datatypes.ThbDataType
import com.fortioridesign.moxykaroo.fit.FitFileWriter
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.Device
import io.hammerhead.karooext.models.DeviceEvent
import io.hammerhead.karooext.models.FitEffect
import io.hammerhead.karooext.models.ReleaseBluetooth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.concurrent.ConcurrentHashMap

class KarooMoxyMonitorExtension : KarooExtension(TAG, "1.0") {

    companion object {
        const val TAG = "karoo-moxymonitor"
    }

    private val karooSystem: KarooSystemServiceProvider by inject()

    private val bleManager: BleManager by inject()

    private val devices = ConcurrentHashMap<ScanResult, MoxyMonitorSensorDevice>()

    override val types by lazy {
        (0..3).flatMap { n ->
            listOf(
                Smo2DataType(extension, n),
                ThbDataType(extension, n)
            )
        }
    }

    override fun startFit(emitter: Emitter<FitEffect>) {
        FitFileWriter(emitter, bleManager, karooSystem)
    }

    override fun startScan(emitter: Emitter<Device>) {
        val job = CoroutineScope(Dispatchers.IO).launch {
            bleManager.scan().map { scanResult ->
                Log.i(TAG, "BLE Scanned found ${scanResult.address}: ${scanResult.name}")

                MoxyMonitorSensorDevice(bleManager, extension, scanResult)
            }.collect { device ->
                val scanResult = decodeJson<ScanResult>(device.source.uid)
                devices.putIfAbsent(scanResult, device)
                emitter.onNext(device.source)
            }
        }
        emitter.setCancellable {
            job.cancel()
        }
    }

    override fun connectDevice(uid: String, emitter: Emitter<DeviceEvent>) {
        Log.d(TAG, "Connect to $uid")

        val scanResult = try {
            decodeJson<ScanResult>(uid)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to decode ScanResult from uid $uid", e)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            var assignedDeviceIndex: Int? = null
            bleManager.updateDeviceStates { deviceStates ->
                Log.i(TAG, "Updating device state for ${scanResult.address}")

                val usedIndexes = deviceStates.values.map { it.deviceIndex }.toSet()
                val lowestFreeIndex = generateSequence(0) { it + 1 }.first { it !in usedIndexes }

                val deviceState = deviceStates[scanResult]
                val previousDeviceState = deviceState ?: MoxyMonitorDeviceState(
                    name = scanResult.name,
                    deviceIndex = lowestFreeIndex,
                    antId = scanResult.antId,
                )

                assignedDeviceIndex = previousDeviceState.deviceIndex

                buildMap {
                    putAll(deviceStates)
                    put(
                        scanResult, previousDeviceState.copy(
                            name = scanResult.name,
                            antId = scanResult.antId,
                            connectionState = MoxyMonitorConnectionState.CONNECTING,
                        )
                    )
                }
            }

            devices.getOrPut(scanResult) {
                MoxyMonitorSensorDevice(bleManager, extension, scanResult)
            }.connect(emitter, assignedDeviceIndex ?: error("Device index must be assigned"))
        }
    }

    override fun onCreate() {
        super.onCreate()
        // LocalCrashLogger.init(this)

        CoroutineScope(Dispatchers.IO).launch {
            var savedDevicesScanResults = mapOf<ScanResult, MoxyMonitorDeviceState>()

            try {
                val savedDevices = applicationContext.streamDevices().first()
                savedDevicesScanResults = savedDevices.mapIndexed { index, device ->
                    device.scanResult to MoxyMonitorDeviceState(
                        name = device.scanResult.name,
                        deviceIndex = index,
                        antId = device.scanResult.antId,
                        batteryLevel = device.lastKnownBatteryLevel,
                        bodyPosition = device.bodyPosition
                    )
                }.toMap()

                Log.i(
                    TAG,
                    "Loaded ${savedDevicesScanResults.size} saved devices from Karoo: $savedDevicesScanResults"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load saved devices: ${e.message}")
            }

            bleManager.initLastKnownConnectionState(savedDevicesScanResults)

            // Stream known devices and remove them from the list if the user has removed them from the connected sensors
            karooSystem.karooSystemService.streamSavedDevices().distinctUntilChanged()
                .collect { (devices) ->
                    bleManager.updateDeviceStates { deviceStates ->
                        try {
                            val knownScanResults = devices.mapNotNull { device ->
                                val hasDataType = device.supportedDataTypes.contains(
                                    DataType.dataTypeId(
                                        TAG,
                                        "smo2_1"
                                    )
                                )
                                if (hasDataType) {
                                    val scanResult = try {
                                        decodeJson<ScanResult>(
                                            Device.fromDeviceUid(
                                                device.id
                                            )?.second ?: error("Invalid device uid")
                                        )
                                    } catch (e: Throwable) {
                                        Log.e(
                                            TAG,
                                            "Failed to decode saved device id ${device.id}: ${e.message}"
                                        )
                                        null
                                    }

                                    device to scanResult
                                } else {
                                    null
                                }
                            }.toMap()

                            val disabledDevicesScanResults =
                                knownScanResults.filter { (device, scanResult) ->
                                    !device.enabled
                                }.values.filterNotNull().toSet()

                            val toRemove =
                                deviceStates.filter { (scanResult, _) -> scanResult !in knownScanResults.values }
                            toRemove.forEach { (scanResult, _) ->
                                Log.i(
                                    TAG,
                                    "Removing device $scanResult as it is no longer saved in Karoo"
                                )
                            }

                            val deviceStatesDisabled = deviceStates.map { (scanResult, device) ->
                                if (disabledDevicesScanResults.contains(scanResult)) {
                                    Log.i(
                                        TAG,
                                        "Marking device $scanResult as disconnected as it has been disabled in Karoo"
                                    )
                                    scanResult to device.copy(
                                        connectionState = MoxyMonitorConnectionState.DISABLED
                                    )
                                } else {
                                    scanResult to device
                                }
                            }.toMap()

                            deviceStatesDisabled - toRemove.keys
                        } catch (t: Throwable) {
                            Log.e(TAG, "Failed to update known devices: ${t.message}")

                            deviceStates
                        }
                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (karooSystem.karooSystemService.connected) {
            karooSystem.karooSystemService.dispatch(ReleaseBluetooth(extension))
        }
    }
}