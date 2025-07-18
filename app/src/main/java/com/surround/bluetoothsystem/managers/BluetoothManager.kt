package com.surround.bluetoothsystem.managers

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.surround.bluetoothsystem.models.BluetoothDeviceInfo
import com.surround.bluetoothsystem.models.ConnectedSpeaker
import com.surround.bluetoothsystem.models.SurroundChannel
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class BluetoothManager(
    private val context: Context,
    private val listener: BluetoothManagerListener
) {
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val handler = Handler(Looper.getMainLooper())
    private val discoveredDevices = ConcurrentHashMap<String, BluetoothDeviceInfo>()
    private val connectedSpeakers = ConcurrentHashMap<String, ConnectedSpeaker>()
    private val bluetoothGattConnections = ConcurrentHashMap<String, BluetoothGatt>()
    
    private var isScanning = false
    private var isBluetoothEnabled = false
    
    companion object {
        private const val TAG = "BluetoothManager"
        private const val SCAN_TIMEOUT_MS = 30000L // 30 seconds
    }
    
    // Broadcast receiver for device discovery
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, -50).toInt()
                    
                    device?.let { bluetoothDevice ->
                        if (hasBluetoothPermissions()) {
                            val deviceInfo = BluetoothDeviceInfo(
                                device = bluetoothDevice,
                                name = bluetoothDevice.name ?: "Unknown Device",
                                address = bluetoothDevice.address,
                                rssi = rssi
                            )
                            
                            discoveredDevices[bluetoothDevice.address] = deviceInfo
                            listener.onDeviceDiscovered(deviceInfo)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    isScanning = false
                    listener.onScanFinished()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    isBluetoothEnabled = state == BluetoothAdapter.STATE_ON
                    listener.onBluetoothStateChanged(isBluetoothEnabled)
                }
            }
        }
    }
    
    // GATT callback for individual device connections
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val deviceAddress = gatt?.device?.address ?: return
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Device connected: $deviceAddress")
                    handler.post {
                        handleDeviceConnected(gatt.device)
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Device disconnected: $deviceAddress")
                    handler.post {
                        handleDeviceDisconnected(deviceAddress)
                    }
                    gatt?.close()
                    bluetoothGattConnections.remove(deviceAddress)
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered for device: ${gatt?.device?.address}")
                // Handle service discovery if needed for audio services
            }
        }
    }
    
    init {
        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(discoveryReceiver, filter)
        
        // Check initial Bluetooth state
        isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
    }
    
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }
    
    fun isBluetoothEnabled(): Boolean {
        return isBluetoothEnabled
    }
    
    fun enableBluetooth(): Boolean {
        return if (bluetoothAdapter != null && hasBluetoothPermissions()) {
            bluetoothAdapter.enable()
        } else {
            false
        }
    }
    
    fun startDiscovery(): Boolean {
        if (!isBluetoothSupported() || !isBluetoothEnabled || !hasBluetoothPermissions()) {
            return false
        }
        
        if (isScanning) {
            stopDiscovery()
        }
        
        discoveredDevices.clear()
        listener.onScanStarted()
        
        isScanning = true
        val started = bluetoothAdapter?.startDiscovery() == true
        
        if (started) {
            // Auto-stop discovery after timeout
            handler.postDelayed({
                if (isScanning) {
                    stopDiscovery()
                }
            }, SCAN_TIMEOUT_MS)
        }
        
        return started
    }
    
    fun stopDiscovery() {
        if (isScanning && hasBluetoothPermissions()) {
            bluetoothAdapter?.cancelDiscovery()
            isScanning = false
            listener.onScanFinished()
        }
    }
    
    fun connectToDevice(deviceInfo: BluetoothDeviceInfo): Boolean {
        if (!hasBluetoothPermissions()) {
            return false
        }
        
        // Update device state to connecting
        val updatedDevice = deviceInfo.copy(isConnecting = true)
        discoveredDevices[deviceInfo.address] = updatedDevice
        listener.onDeviceConnectionStateChanged(updatedDevice)
        
        // Connect to GATT server
        val gatt = deviceInfo.device.connectGatt(context, false, gattCallback)
        if (gatt != null) {
            bluetoothGattConnections[deviceInfo.address] = gatt
            return true
        }
        
        return false
    }
    
    fun disconnectFromDevice(speakerAddress: String) {
        if (!hasBluetoothPermissions()) {
            return
        }
        
        bluetoothGattConnections[speakerAddress]?.let { gatt ->
            gatt.disconnect()
            gatt.close()
            bluetoothGattConnections.remove(speakerAddress)
        }
        
        connectedSpeakers.remove(speakerAddress)
        listener.onSpeakerDisconnected(speakerAddress)
    }
    
    fun getConnectedSpeakers(): List<ConnectedSpeaker> {
        return connectedSpeakers.values.toList()
    }
    
    fun getDiscoveredDevices(): List<BluetoothDeviceInfo> {
        return discoveredDevices.values.toList()
    }
    
    fun updateSpeakerChannel(speakerAddress: String, channel: SurroundChannel) {
        connectedSpeakers[speakerAddress]?.let { speaker ->
            speaker.channel = channel
            listener.onSpeakerChannelChanged(speaker)
        }
    }
    
    fun updateSpeakerVolume(speakerAddress: String, volume: Int) {
        connectedSpeakers[speakerAddress]?.let { speaker ->
            speaker.volume = volume
            listener.onSpeakerVolumeChanged(speaker)
        }
    }
    
    private fun handleDeviceConnected(device: BluetoothDevice) {
        if (!hasBluetoothPermissions()) {
            return
        }
        
        val speaker = ConnectedSpeaker(
            device = device,
            name = device.name ?: "Unknown Speaker",
            address = device.address,
            channel = SurroundChannel.UNASSIGNED,
            volume = 50,
            isConnected = true
        )
        
        connectedSpeakers[device.address] = speaker
        
        // Update discovered device state
        discoveredDevices[device.address]?.let { deviceInfo ->
            val updatedDevice = deviceInfo.copy(isConnected = true, isConnecting = false)
            discoveredDevices[device.address] = updatedDevice
            listener.onDeviceConnectionStateChanged(updatedDevice)
        }
        
        listener.onSpeakerConnected(speaker)
    }
    
    private fun handleDeviceDisconnected(deviceAddress: String) {
        connectedSpeakers.remove(deviceAddress)
        
        // Update discovered device state
        discoveredDevices[deviceAddress]?.let { deviceInfo ->
            val updatedDevice = deviceInfo.copy(isConnected = false, isConnecting = false)
            discoveredDevices[deviceAddress] = updatedDevice
            listener.onDeviceConnectionStateChanged(updatedDevice)
        }
        
        listener.onSpeakerDisconnected(deviceAddress)
    }
    
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun destroy() {
        try {
            context.unregisterReceiver(discoveryReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        
        stopDiscovery()
        
        // Close all GATT connections
        bluetoothGattConnections.values.forEach { gatt ->
            gatt.close()
        }
        bluetoothGattConnections.clear()
        
        connectedSpeakers.clear()
        discoveredDevices.clear()
    }
    
    interface BluetoothManagerListener {
        fun onBluetoothStateChanged(isEnabled: Boolean)
        fun onScanStarted()
        fun onScanFinished()
        fun onDeviceDiscovered(device: BluetoothDeviceInfo)
        fun onDeviceConnectionStateChanged(device: BluetoothDeviceInfo)
        fun onSpeakerConnected(speaker: ConnectedSpeaker)
        fun onSpeakerDisconnected(speakerAddress: String)
        fun onSpeakerChannelChanged(speaker: ConnectedSpeaker)
        fun onSpeakerVolumeChanged(speaker: ConnectedSpeaker)
    }
}