package com.surround.bluetoothsystem.models

import android.bluetooth.BluetoothDevice

data class ConnectedSpeaker(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    var channel: SurroundChannel = SurroundChannel.UNASSIGNED,
    var volume: Int = 50,
    var isConnected: Boolean = true,
    var delayCompensation: Int = 0, // in milliseconds
    var isActive: Boolean = true
) {
    fun getDisplayName(): String {
        return if (name.isNotBlank()) name else "Unknown Speaker"
    }
    
    fun getChannelColor(): Int {
        return channel.color
    }
    
    fun getChannelDisplayName(): String {
        return channel.displayName
    }
}

enum class SurroundChannel(val displayName: String, val color: Int) {
    UNASSIGNED("Unassigned", 0xFF757575.toInt()),
    FRONT_LEFT("Front Left", 0xFF2196F3.toInt()),
    FRONT_RIGHT("Front Right", 0xFF4CAF50.toInt()),
    REAR_LEFT("Rear Left", 0xFFFF9800.toInt()),
    REAR_RIGHT("Rear Right", 0xFF9C27B0.toInt()),
    CENTER("Center", 0xFFF44336.toInt()),
    SUBWOOFER("Subwoofer", 0xFF795548.toInt());
    
    companion object {
        fun getAvailableChannels(): List<SurroundChannel> {
            return values().toList()
        }
        
        fun getChannelByName(name: String): SurroundChannel {
            return values().find { it.displayName == name } ?: UNASSIGNED
        }
    }
}