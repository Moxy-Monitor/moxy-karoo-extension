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
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.RequestBluetooth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class KarooSystemServiceProvider(private val context: Context) {
    val karooSystemService: KarooSystemService = KarooSystemService(context)
    private val connectionStateMutableFlow: MutableSharedFlow<Boolean> = MutableSharedFlow(replay = 1)
    val connectionStateFlow: Flow<Boolean> = connectionStateMutableFlow

    init {
        karooSystemService.connect { connected ->
            if (connected) {
                Log.d(KarooMoxyMonitorExtension.TAG, "Connected to Karoo system")
                karooSystemService.dispatch(RequestBluetooth(KarooMoxyMonitorExtension.TAG))
            }
            connectionStateMutableFlow.tryEmit(connected)
        }
    }
}