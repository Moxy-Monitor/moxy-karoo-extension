/*
 * Copyright 2026 Fortiori Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fortioridesign.moxykaroo.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.ktx.asFlow
import no.nordicsemi.android.ble.ktx.suspend
import java.util.UUID

/**
 * Nordic BLE v1 manager subclass for a single Moxy device connection.
 * Discovers all services and allows dynamic access to any characteristic.
 */
class MoxyDeviceBleManager(context: Context) : BleManager(context) {
    private var bluetoothGatt: BluetoothGatt? = null

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        bluetoothGatt = gatt
        // Check that the SMO2 service is present (required)
        val smo2Service = gatt.getService(BleConsts.SMO2_SERVICE)
        return smo2Service != null
    }

    override fun onServicesInvalidated() {
        bluetoothGatt = null
    }

    fun getGattCharacteristic(serviceUuid: UUID, characteristicUuid: UUID): BluetoothGattCharacteristic? {
        return bluetoothGatt?.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
    }

    /**
     * Public wrapper around the protected [setNotificationCallback] and [enableNotifications].
     * Returns a Flow of raw byte arrays from notifications on the given characteristic.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeNotifications(characteristic: BluetoothGattCharacteristic): Flow<ByteArray> {
        return setNotificationCallback(characteristic)
            .asFlow()
            .map { data -> data.value ?: ByteArray(0) }
            .also {
                // Enable notifications must be called after setting the callback
                enableNotifications(characteristic).enqueue()
            }
    }

    /**
     * Public wrapper around the protected [readCharacteristic].
     * Suspends until the characteristic value is read and returns the raw bytes.
     */
    suspend fun readGattCharacteristic(characteristic: BluetoothGattCharacteristic): ByteArray {
        val data = readCharacteristic(characteristic).suspend()
        return data.value ?: ByteArray(0)
    }
}