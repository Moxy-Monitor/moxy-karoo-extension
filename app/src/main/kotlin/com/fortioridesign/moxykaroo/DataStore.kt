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

package com.fortioridesign.moxykaroo

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fortioridesign.moxykaroo.ble.SavedSensorDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings", corruptionHandler = ReplaceFileCorruptionHandler {
    Log.w(KarooMoxyMonitorExtension.TAG, "Error reading settings, using default values")
    emptyPreferences()
})

val knownDevicesKey = stringPreferencesKey("devices")

suspend fun Context.updateSavedDevices(updateFunc: (List<SavedSensorDetails>) -> List<SavedSensorDetails>) {
    dataStore.edit { t ->
        val knownDevices = try {
            if (t.contains(knownDevicesKey)){
                jsonWithUnknownKeys.decodeFromString<List<SavedSensorDetails>>(t[knownDevicesKey]!!)
            } else {
                listOf()
            }
        } catch(e: Throwable){
            Log.e(KarooMoxyMonitorExtension.TAG, "Failed to read known devices", e)
            listOf()
        }
        t[knownDevicesKey] = Json.encodeToString(updateFunc(knownDevices))
    }
}

fun Context.streamDevices(): Flow<List<SavedSensorDetails>> {
    return dataStore.data.map { knownDevicesJson ->
        try {
            if (knownDevicesJson.contains(knownDevicesKey)){
                jsonWithUnknownKeys.decodeFromString<List<SavedSensorDetails>>(knownDevicesJson[knownDevicesKey]!!)
            } else {
                listOf()
            }
        } catch(e: Throwable){
            Log.e(KarooMoxyMonitorExtension.TAG, "Failed to read known devices", e)
            listOf()
        }
    }.distinctUntilChanged()
}

val jsonWithUnknownKeys = Json { ignoreUnknownKeys = true }