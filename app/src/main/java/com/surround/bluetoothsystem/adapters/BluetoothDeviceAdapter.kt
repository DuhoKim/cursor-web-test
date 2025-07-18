package com.surround.bluetoothsystem.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.surround.bluetoothsystem.R
import com.surround.bluetoothsystem.models.BluetoothDeviceInfo
import com.surround.bluetoothsystem.models.SignalQuality

class BluetoothDeviceAdapter(
    private val devices: MutableList<BluetoothDeviceInfo>,
    private val onConnectClick: (BluetoothDeviceInfo) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceIcon: ImageView = itemView.findViewById(R.id.deviceIcon)
        val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        val deviceAddress: TextView = itemView.findViewById(R.id.deviceAddress)
        val deviceRssi: TextView = itemView.findViewById(R.id.deviceRssi)
        val connectButton: Button = itemView.findViewById(R.id.connectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bluetooth_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        
        holder.deviceName.text = device.getDisplayName()
        holder.deviceAddress.text = device.address
        holder.deviceRssi.text = device.getSignalStrength()
        
        // Set signal quality color
        val signalColor = when (device.getSignalQuality()) {
            SignalQuality.EXCELLENT -> R.color.success_green
            SignalQuality.GOOD -> R.color.primary_blue
            SignalQuality.FAIR -> R.color.warning_yellow
            SignalQuality.POOR -> R.color.error_red
        }
        holder.deviceRssi.setTextColor(holder.itemView.context.getColor(signalColor))
        
        // Handle connection state
        when {
            device.isConnected -> {
                holder.connectButton.text = holder.itemView.context.getString(R.string.connected)
                holder.connectButton.isEnabled = false
                holder.connectButton.setBackgroundTintList(
                    holder.itemView.context.getColorStateList(R.color.success_green)
                )
            }
            device.isConnecting -> {
                holder.connectButton.text = holder.itemView.context.getString(R.string.connecting)
                holder.connectButton.isEnabled = false
                holder.connectButton.setBackgroundTintList(
                    holder.itemView.context.getColorStateList(R.color.warning_yellow)
                )
            }
            else -> {
                holder.connectButton.text = holder.itemView.context.getString(R.string.connect)
                holder.connectButton.isEnabled = true
                holder.connectButton.setBackgroundTintList(
                    holder.itemView.context.getColorStateList(R.color.primary_blue)
                )
            }
        }
        
        holder.connectButton.setOnClickListener {
            if (!device.isConnected && !device.isConnecting) {
                onConnectClick(device)
            }
        }
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: List<BluetoothDeviceInfo>) {
        devices.clear()
        devices.addAll(newDevices)
        notifyDataSetChanged()
    }
    
    fun addDevice(device: BluetoothDeviceInfo) {
        // Check if device already exists
        val existingIndex = devices.indexOfFirst { it.address == device.address }
        if (existingIndex >= 0) {
            devices[existingIndex] = device
            notifyItemChanged(existingIndex)
        } else {
            devices.add(device)
            notifyItemInserted(devices.size - 1)
        }
    }
    
    fun removeDevice(deviceAddress: String) {
        val index = devices.indexOfFirst { it.address == deviceAddress }
        if (index >= 0) {
            devices.removeAt(index)
            notifyItemRemoved(index)
        }
    }
    
    fun updateDeviceConnectionState(deviceAddress: String, isConnected: Boolean, isConnecting: Boolean) {
        val index = devices.indexOfFirst { it.address == deviceAddress }
        if (index >= 0) {
            devices[index] = devices[index].copy(
                isConnected = isConnected,
                isConnecting = isConnecting
            )
            notifyItemChanged(index)
        }
    }
}