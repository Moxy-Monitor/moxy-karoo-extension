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

import android.bluetooth.BluetoothGattCharacteristic
import java.util.UUID

/**
 * Wrapper around a connected BLE device that provides access to its GATT characteristics.
 */
class ConnectedPeripheral(
    internal val manager: MoxyDeviceBleManager,
) {
    fun getCharacteristic(serviceUuid: UUID, characteristicUuid: UUID): BluetoothGattCharacteristic? {
        return manager.getGattCharacteristic(serviceUuid, characteristicUuid)
    }
}