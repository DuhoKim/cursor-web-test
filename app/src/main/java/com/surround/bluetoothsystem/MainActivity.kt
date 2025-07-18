package com.surround.bluetoothsystem

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.surround.bluetoothsystem.adapters.BluetoothDeviceAdapter
import com.surround.bluetoothsystem.adapters.ConnectedSpeakerAdapter
import com.surround.bluetoothsystem.managers.BluetoothManager
import com.surround.bluetoothsystem.managers.SurroundSoundManager
import com.surround.bluetoothsystem.models.BluetoothDeviceInfo
import com.surround.bluetoothsystem.models.ConnectedSpeaker
import com.surround.bluetoothsystem.models.SurroundChannel
import com.surround.bluetoothsystem.models.SurroundMode
import com.surround.bluetoothsystem.utils.PermissionHelper

class MainActivity : AppCompatActivity(), 
    BluetoothManager.BluetoothManagerListener,
    SurroundSoundManager.SurroundSoundManagerListener {

    // UI Components
    private lateinit var statusIndicator: ImageView
    private lateinit var scanButton: Button
    private lateinit var stopScanButton: Button
    private lateinit var testSoundButton: Button
    private lateinit var stopTestSoundButton: Button
    private lateinit var masterVolumeSeekBar: SeekBar
    private lateinit var volumeValueText: TextView
    private lateinit var systemStatusText: TextView
    private lateinit var availableDevicesRecyclerView: RecyclerView
    private lateinit var connectedSpeakersRecyclerView: RecyclerView
    
    // Adapters
    private lateinit var bluetoothDeviceAdapter: BluetoothDeviceAdapter
    private lateinit var connectedSpeakerAdapter: ConnectedSpeakerAdapter
    
    // Managers
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var surroundSoundManager: SurroundSoundManager
    
    // Permission handling
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            initializeBluetoothManager()
        } else {
            showPermissionDeniedMessage()
        }
    }
    
    // Bluetooth enable request
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            initializeBluetoothManager()
        } else {
            Toast.makeText(this, getString(R.string.bluetooth_not_enabled), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        setupAdapters()
        setupClickListeners()
        setupSeekBars()
        
        // Check permissions and initialize
        checkPermissionsAndInitialize()
    }
    
    private fun initializeViews() {
        statusIndicator = findViewById(R.id.statusIndicator)
        scanButton = findViewById(R.id.scanButton)
        stopScanButton = findViewById(R.id.stopScanButton)
        testSoundButton = findViewById(R.id.testSoundButton)
        stopTestSoundButton = findViewById(R.id.stopTestSoundButton)
        masterVolumeSeekBar = findViewById(R.id.masterVolumeSeekBar)
        volumeValueText = findViewById(R.id.volumeValueText)
        systemStatusText = findViewById(R.id.systemStatusText)
        availableDevicesRecyclerView = findViewById(R.id.availableDevicesRecyclerView)
        connectedSpeakersRecyclerView = findViewById(R.id.connectedSpeakersRecyclerView)
    }
    
    private fun setupAdapters() {
        // Bluetooth devices adapter
        bluetoothDeviceAdapter = BluetoothDeviceAdapter(
            devices = mutableListOf(),
            onConnectClick = { device ->
                connectToDevice(device)
            }
        )
        
        availableDevicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = bluetoothDeviceAdapter
        }
        
        // Connected speakers adapter
        connectedSpeakerAdapter = ConnectedSpeakerAdapter(
            speakers = mutableListOf(),
            onDisconnectClick = { speaker ->
                disconnectFromSpeaker(speaker)
            },
            onChannelChanged = { speaker, channel ->
                updateSpeakerChannel(speaker, channel)
            },
            onVolumeChanged = { speaker, volume ->
                updateSpeakerVolume(speaker, volume)
            }
        )
        
        connectedSpeakersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = connectedSpeakerAdapter
        }
    }
    
    private fun setupClickListeners() {
        scanButton.setOnClickListener {
            startBluetoothScan()
        }
        
        stopScanButton.setOnClickListener {
            stopBluetoothScan()
        }
        
        testSoundButton.setOnClickListener {
            playTestSound()
        }
        
        stopTestSoundButton.setOnClickListener {
            stopTestSound()
        }
    }
    
    private fun setupSeekBars() {
        masterVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateMasterVolume(progress)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun checkPermissionsAndInitialize() {
        val missingPermissions = PermissionHelper.getMissingPermissions(this)
        
        if (missingPermissions.isEmpty()) {
            initializeBluetoothManager()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    
    private fun initializeBluetoothManager() {
        try {
            // Initialize Bluetooth Manager
            bluetoothManager = BluetoothManager(this, this)
            
            // Initialize Surround Sound Manager
            surroundSoundManager = SurroundSoundManager(this, this)
            
            // Check if Bluetooth is supported
            if (!bluetoothManager.isBluetoothSupported()) {
                Toast.makeText(this, getString(R.string.bluetooth_not_supported), Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            // Check if Bluetooth is enabled
            if (!bluetoothManager.isBluetoothEnabled()) {
                requestEnableBluetooth()
            } else {
                updateBluetoothStatus(true)
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing Bluetooth: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun requestEnableBluetooth() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBluetoothIntent)
    }
    
    private fun showPermissionDeniedMessage() {
        Toast.makeText(
            this,
            getString(R.string.permission_required),
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun startBluetoothScan() {
        if (bluetoothManager.startDiscovery()) {
            scanButton.isEnabled = false
            stopScanButton.isEnabled = true
            scanButton.text = getString(R.string.scanning)
        }
    }
    
    private fun stopBluetoothScan() {
        bluetoothManager.stopDiscovery()
        scanButton.isEnabled = true
        stopScanButton.isEnabled = false
        scanButton.text = getString(R.string.scan_devices)
    }
    
    private fun connectToDevice(device: BluetoothDeviceInfo) {
        bluetoothManager.connectToDevice(device)
    }
    
    private fun disconnectFromSpeaker(speaker: ConnectedSpeaker) {
        bluetoothManager.disconnectFromDevice(speaker.address)
    }
    
    private fun updateSpeakerChannel(speaker: ConnectedSpeaker, channel: SurroundChannel) {
        bluetoothManager.updateSpeakerChannel(speaker.address, channel)
        surroundSoundManager.updateSpeakerChannel(speaker.address, channel)
    }
    
    private fun updateSpeakerVolume(speaker: ConnectedSpeaker, volume: Int) {
        bluetoothManager.updateSpeakerVolume(speaker.address, volume)
        surroundSoundManager.updateSpeakerVolume(speaker.address, volume)
    }
    
    private fun updateMasterVolume(volume: Int) {
        volumeValueText.text = "$volume%"
        surroundSoundManager.updateMasterVolume(volume)
    }
    
    private fun playTestSound() {
        surroundSoundManager.playTestSound()
        testSoundButton.isEnabled = false
        stopTestSoundButton.isEnabled = true
    }
    
    private fun stopTestSound() {
        surroundSoundManager.stopTestSound()
        testSoundButton.isEnabled = true
        stopTestSoundButton.isEnabled = false
    }
    
    private fun updateBluetoothStatus(isEnabled: Boolean) {
        if (isEnabled) {
            statusIndicator.setImageResource(R.drawable.ic_bluetooth_device)
            statusIndicator.setColorFilter(getColor(R.color.primary_blue))
        } else {
            statusIndicator.setImageResource(R.drawable.ic_bluetooth_disabled)
            statusIndicator.setColorFilter(getColor(R.color.error_red))
        }
    }
    
    private fun updateSystemStatus(isActive: Boolean, surroundMode: SurroundMode) {
        if (isActive) {
            systemStatusText.text = getString(R.string.surround_system_active) + " - ${surroundMode.name}"
            systemStatusText.setTextColor(getColor(R.color.success_green))
        } else {
            systemStatusText.text = getString(R.string.not_connected)
            systemStatusText.setTextColor(getColor(R.color.error_red))
        }
    }
    
    // BluetoothManagerListener implementation
    override fun onBluetoothStateChanged(isEnabled: Boolean) {
        runOnUiThread {
            updateBluetoothStatus(isEnabled)
        }
    }
    
    override fun onScanStarted() {
        runOnUiThread {
            scanButton.text = getString(R.string.scanning)
            scanButton.isEnabled = false
            stopScanButton.isEnabled = true
        }
    }
    
    override fun onScanFinished() {
        runOnUiThread {
            scanButton.text = getString(R.string.scan_devices)
            scanButton.isEnabled = true
            stopScanButton.isEnabled = false
        }
    }
    
    override fun onDeviceDiscovered(device: BluetoothDeviceInfo) {
        runOnUiThread {
            bluetoothDeviceAdapter.addDevice(device)
        }
    }
    
    override fun onDeviceConnectionStateChanged(device: BluetoothDeviceInfo) {
        runOnUiThread {
            bluetoothDeviceAdapter.updateDeviceConnectionState(
                device.address,
                device.isConnected,
                device.isConnecting
            )
        }
    }
    
    override fun onSpeakerConnected(speaker: ConnectedSpeaker) {
        runOnUiThread {
            connectedSpeakerAdapter.addSpeaker(speaker)
            surroundSoundManager.addSpeaker(speaker)
        }
    }
    
    override fun onSpeakerDisconnected(speakerAddress: String) {
        runOnUiThread {
            connectedSpeakerAdapter.removeSpeaker(speakerAddress)
            surroundSoundManager.removeSpeaker(speakerAddress)
        }
    }
    
    override fun onSpeakerChannelChanged(speaker: ConnectedSpeaker) {
        runOnUiThread {
            connectedSpeakerAdapter.notifyDataSetChanged()
        }
    }
    
    override fun onSpeakerVolumeChanged(speaker: ConnectedSpeaker) {
        runOnUiThread {
            connectedSpeakerAdapter.updateSpeakerVolume(speaker.address, speaker.volume)
        }
    }
    
    // SurroundSoundManagerListener implementation
    override fun onSystemStatusChanged(isActive: Boolean, surroundMode: SurroundMode) {
        runOnUiThread {
            updateSystemStatus(isActive, surroundMode)
        }
    }
    
    override fun onChannelConfigurationChanged(channelMapping: Map<SurroundChannel, ConnectedSpeaker>) {
        runOnUiThread {
            connectedSpeakerAdapter.notifyDataSetChanged()
        }
    }
    
    override fun onAudioProcessingStarted() {
        runOnUiThread {
            // Update UI to show audio processing is active
        }
    }
    
    override fun onAudioProcessingStopped() {
        runOnUiThread {
            // Update UI to show audio processing is stopped
        }
    }
    
    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up managers
        if (::bluetoothManager.isInitialized) {
            bluetoothManager.destroy()
        }
        
        if (::surroundSoundManager.isInitialized) {
            surroundSoundManager.destroy()
        }
    }
}