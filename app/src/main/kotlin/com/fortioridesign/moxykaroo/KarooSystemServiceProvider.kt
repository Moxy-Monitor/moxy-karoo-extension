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