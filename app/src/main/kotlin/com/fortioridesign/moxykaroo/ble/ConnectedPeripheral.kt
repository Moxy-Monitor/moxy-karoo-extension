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