package com.fortioridesign.moxykaroo.ble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fortioridesign.moxykaroo.KarooMoxyMonitorExtension
import com.fortioridesign.moxykaroo.MoxyMonitorDeviceState
import com.fortioridesign.moxykaroo.updateSavedDevices
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nordicsemi.android.ble.ktx.stateAsFlow
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult as NordicScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class BleManager(private val context: Context) {
    private val deviceConnectionStatusFlow = MutableStateFlow<Map<ScanResult, MoxyMonitorDeviceState>>(mapOf())

    val deviceConnectionStatus: StateFlow<Map<ScanResult, MoxyMonitorDeviceState>> = deviceConnectionStatusFlow
    private val lastKnownConnectionState = mutableMapOf<ScanResult, MoxyMonitorDeviceState>()
    private val lastKnownConnectionStateMutex = Mutex(locked = true)

    private var lastSavedSensorDetails: List<SavedSensorDetails>? = null

    suspend fun saveDeviceStates() {
        try {
            lastKnownConnectionStateMutex.withLock {
                val knownDevices = lastKnownConnectionState.map { (scanResult, state) ->
                    SavedSensorDetails(
                        scanResult = scanResult,
                        bodyPosition = state.bodyPosition,
                        lastKnownBatteryLevel = state.batteryLevel,
                    )
                }

                if (knownDevices != lastSavedSensorDetails) {
                    lastSavedSensorDetails = knownDevices
                    Log.i(KarooMoxyMonitorExtension.Companion.TAG, "Saving device states: $knownDevices")

                    context.updateSavedDevices {
                        knownDevices
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(KarooMoxyMonitorExtension.Companion.TAG, "Failed to save device states", t)
        }
    }

    suspend fun updateDeviceState(scanResult: ScanResult, update: (MoxyMonitorDeviceState?) -> MoxyMonitorDeviceState?): MoxyMonitorDeviceState? {
        val newState = lastKnownConnectionStateMutex.withLock {
            val currentState = lastKnownConnectionState[scanResult]
            val newState = update(currentState)

            if (newState == null) {
                lastKnownConnectionState.remove(scanResult)
            } else {
                lastKnownConnectionState[scanResult] = newState
            }

            val mapCopy = lastKnownConnectionState.toMap()
            deviceConnectionStatusFlow.emit(mapCopy)
            newState
        }

        saveDeviceStates()

        return newState
    }

    suspend fun updateDeviceStates(update: (Map<ScanResult, MoxyMonitorDeviceState>) -> Map<ScanResult, MoxyMonitorDeviceState>): Map<ScanResult, MoxyMonitorDeviceState> {
        val newStates = lastKnownConnectionStateMutex.withLock {
            val newStates = update(lastKnownConnectionState)
            lastKnownConnectionState.clear()
            lastKnownConnectionState.putAll(newStates)

            val mapCopy = lastKnownConnectionState.toMap()
            deviceConnectionStatusFlow.emit(mapCopy)
            newStates
        }

        saveDeviceStates()

        return newStates
    }

    suspend fun initLastKnownConnectionState(scanResults: Map<ScanResult, MoxyMonitorDeviceState>) {
        try {
            if (!lastKnownConnectionStateMutex.isLocked) {
                Log.w(KarooMoxyMonitorExtension.Companion.TAG, "initLastKnownConnectionState called but already initialized")
                return
            }
            lastKnownConnectionState.clear()
            lastKnownConnectionState.putAll(scanResults)

            Log.i(KarooMoxyMonitorExtension.Companion.TAG, "Initialized last known connection state with ${scanResults.size} devices: $scanResults")
            deviceConnectionStatusFlow.emit(lastKnownConnectionState.toMap())
        } finally {
            if (lastKnownConnectionStateMutex.isLocked) {
                lastKnownConnectionStateMutex.unlock()
            }
        }
    }

    fun requestPermissions(activity: Activity) {
        // Request Bluetooth permissions based on Android version
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            // Android 6-11 (API 23-30) - requires location permission for BLE scanning
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                0,
            )
        }
    }

    fun hasBleScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 6-11 (API 23-30) - requires location permission
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasBleConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            // Older versions don't need explicit connect permission
            true
        }
    }

    fun observeBlePermissions(): Flow<Boolean> = flow {
        while (true) {
            emit(hasBleScanPermission() && hasBleConnectPermission())
            delay(1.seconds)
        }
    }

    @SuppressLint("MissingPermission")
    fun scan(): Flow<ScanResult> {
        if (!hasBleScanPermission()) {
            Log.e(KarooMoxyMonitorExtension.Companion.TAG, "Missing BLE scan permission")
            return emptyFlow()
        }

        val seenAddresses = mutableSetOf<String>()

        return callbackFlow {
            val scanner = BluetoothLeScannerCompat.getScanner()

            val scanFilter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConsts.SMO2_SERVICE))
                .build()

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setReportDelay(0)
                .build()

            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: NordicScanResult) {
                    try {
                        val device = result.device
                        val localName = result.scanRecord?.deviceName

                        if (localName?.startsWith("Moxy5") != true) return
                        if (!seenAddresses.add(device.address)) return

                        val name = localName.substring(6)
                        val nameParts = name.split(":")
                        val deviceId = nameParts.getOrNull(0)?.toIntOrNull()

                        Log.i(
                            KarooMoxyMonitorExtension.Companion.TAG,
                            "BLE Scan found device: ${device.address} Name=$localName DeviceId=$deviceId"
                        )

                        trySend(ScanResult(device.address, localName, deviceId))
                    } catch (e: Throwable) {
                        Log.w(KarooMoxyMonitorExtension.Companion.TAG, "Error processing scan result: ${e.message}")
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.w(KarooMoxyMonitorExtension.Companion.TAG, "BLE scan failed with error code: $errorCode")
                    close()
                }
            }

            Log.i(KarooMoxyMonitorExtension.Companion.TAG, "Starting BLE scan...")
            scanner.startScan(listOf(scanFilter), scanSettings, callback)

            awaitClose {
                scanner.stopScan(callback)
            }
        }.catch {
            Log.w(KarooMoxyMonitorExtension.Companion.TAG, "Error in BLE scan:", it)
        }
    }

    /**
     * Connects to the given device. Emits a [ConnectedPeripheral] when connected and null when disconnected.
     * Retries connection up to 10 times.
     */
    fun connect(scanResult: ScanResult): Flow<ConnectedPeripheral?> {
        return flow {
            while (true) {
                if (!hasBleScanPermission() || !hasBleConnectPermission()) {
                    Log.e(KarooMoxyMonitorExtension.Companion.TAG, "Missing BLE permissions for connection")
                    delay(5.seconds)
                } else {
                    break
                }
            }

            // First scan until we find the device
            @SuppressLint("MissingPermission")
            val bluetoothDevice = try {
                findBluetoothDevice(scanResult)
            } catch (e: Exception) {
                Log.w(KarooMoxyMonitorExtension.Companion.TAG, "Failed to find Bluetooth device for ${scanResult.address}: ${e.message}")
                emit(null)
                return@flow
            }

            try {
                // Connect to the device (including retries)
                repeat(5) { i ->
                    if (i == 0) {
                        Log.i(KarooMoxyMonitorExtension.Companion.TAG, "Found ${scanResult.address}, connecting...")
                    } else {
                        delay(3.seconds)
                        Log.i(KarooMoxyMonitorExtension.Companion.TAG, "Reconnecting to ${scanResult.address}...")
                    }

                    val deviceManager = MoxyDeviceBleManager(context)
                    try {
                        deviceManager.connect(bluetoothDevice)
                            .retry(3, 2000)
                            .timeout(30_000)
                            .suspend()

                        Log.i(KarooMoxyMonitorExtension.Companion.TAG, "Connected to ${scanResult.address}")

                        val peripheral = ConnectedPeripheral(deviceManager)
                        emit(peripheral)

                        // Block while connected — wait for disconnect
                        deviceManager.stateAsFlow().filter { !it.isConnected }.first()

                        Log.i(KarooMoxyMonitorExtension.Companion.TAG, "Device disconnected from ${scanResult.address}")
                        emit(null)
                    } catch (e: Exception) {
                        Log.w(KarooMoxyMonitorExtension.Companion.TAG, "Connection attempt $i failed for ${scanResult.address}: ${e.message}")
                        emit(null)
                    } finally {
                        try {
                            deviceManager.disconnect().suspend()
                        } catch (_: Exception) {}
                        deviceManager.close()
                    }
                }
            } finally {
                Log.i(KarooMoxyMonitorExtension.Companion.TAG, "Finished connection attempts to ${scanResult.address}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun findBluetoothDevice(scanResult: ScanResult): BluetoothDevice {
        return callbackFlow {
            val scanner = BluetoothLeScannerCompat.getScanner()

            val scanFilter = ScanFilter.Builder()
                .setDeviceAddress(scanResult.address)
                .build()

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setReportDelay(0)
                .build()

            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: NordicScanResult) {
                    trySend(result.device)
                }

                override fun onScanFailed(errorCode: Int) {
                    close(Exception("Scan failed with error code $errorCode"))
                }
            }

            scanner.startScan(listOf(scanFilter), scanSettings, callback)

            awaitClose {
                scanner.stopScan(callback)
            }
        }.first()
    }

    fun <T> observeCharacteristic(peripheral: ConnectedPeripheral, service: UUID, characteristic: UUID, parser: (ByteArray) -> T): Flow<T> {
        val gattCharacteristic = peripheral.getCharacteristic(service, characteristic)
        if (gattCharacteristic == null) {
            Log.w(KarooMoxyMonitorExtension.Companion.TAG, "Characteristic $service:$characteristic not found")
            return emptyFlow()
        }

        return peripheral.manager.observeNotifications(gattCharacteristic)
            .map { data ->
                parser(data)
            }
            .onCompletion {
                Log.d(KarooMoxyMonitorExtension.Companion.TAG, "Stopped observing $service:$characteristic")
            }
            .catch {
                Log.w(KarooMoxyMonitorExtension.Companion.TAG, "Characteristic $service:$characteristic error: ${it.message}")
            }
    }

    suspend fun <T> readCharacteristic(peripheral: ConnectedPeripheral, service: UUID, characteristic: UUID, parser: (ByteArray) -> T): T? {
        val gattCharacteristic = peripheral.getCharacteristic(service, characteristic)
        if (gattCharacteristic == null) {
            Log.w(KarooMoxyMonitorExtension.Companion.TAG, "Characteristic $service:$characteristic not found for reading")
            return null
        }

        return try {
            val data = peripheral.manager.readGattCharacteristic(gattCharacteristic)
            parser(data)
        } catch (e: Exception) {
            Log.w(KarooMoxyMonitorExtension.Companion.TAG, "Failed to read characteristic $service:$characteristic: ${e.message}")
            null
        }
    }
}

