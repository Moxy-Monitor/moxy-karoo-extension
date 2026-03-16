package com.fortioridesign.moxykaroo

import io.hammerhead.karooext.models.ConnectionStatus

enum class MoxyMonitorConnectionState {
    SEARCHING,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    DISABLED
}

fun ConnectionStatus.mapToMoxyMonitorConnectionState(): MoxyMonitorConnectionState {
    return when (this) {
        ConnectionStatus.SEARCHING -> MoxyMonitorConnectionState.SEARCHING
        ConnectionStatus.CONNECTED -> MoxyMonitorConnectionState.CONNECTED
        ConnectionStatus.DISCONNECTED -> MoxyMonitorConnectionState.DISCONNECTED
        ConnectionStatus.DISABLED -> MoxyMonitorConnectionState.DISABLED
    }
}