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

import java.util.UUID

object BleConsts {
    // UUIDs for the Device Information service (DIS)
    val DIS_SERVICE_UUID: UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
    val MANUFACTURER_NAME_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb")
    val SERIAL_NUMBER_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
    val MODEL_NUMBER_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")

    // UUIDs for the Battery Service (BAS)
    val BTS_SERVICE_UUID: UUID = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb")

    // UUIDs for the Smo2 service
    val SMO2_SERVICE: UUID = UUID.fromString("6404D801-4CB9-11E8-B566-0800200C9A66")
    val SMO2_CHARACTERISTIC: UUID = UUID.fromString("6404D804-4CB9-11E8-B566-0800200C9A66")
}