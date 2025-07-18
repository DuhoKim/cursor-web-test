package com.surround.bluetoothsystem.models

import android.bluetooth.BluetoothDevice

data class BluetoothDeviceInfo(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val rssi: Int = -50,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false
) {
    fun getDisplayName(): String {
        return if (name.isNotBlank()) name else "Unknown Device"
    }
    
    fun getSignalStrength(): String {
        return "Signal: $rssi dBm"
    }
    
    fun getSignalQuality(): SignalQuality {
        return when {
            rssi > -50 -> SignalQuality.EXCELLENT
            rssi > -60 -> SignalQuality.GOOD
            rssi > -70 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }
    }
}

enum class SignalQuality {
    EXCELLENT, GOOD, FAIR, POOR
}