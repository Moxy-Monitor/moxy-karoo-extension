package com.fortioridesign.moxykaroo.ble

import kotlinx.serialization.Serializable

@Serializable
data class ScanResult(
    val address: String,
    val name: String?,
    val antId: Int?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScanResult) return false

        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }
}