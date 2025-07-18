package com.surround.bluetoothsystem.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.surround.bluetoothsystem.R
import com.surround.bluetoothsystem.models.ConnectedSpeaker
import com.surround.bluetoothsystem.models.SurroundChannel

class ConnectedSpeakerAdapter(
    private val speakers: MutableList<ConnectedSpeaker>,
    private val onDisconnectClick: (ConnectedSpeaker) -> Unit,
    private val onChannelChanged: (ConnectedSpeaker, SurroundChannel) -> Unit,
    private val onVolumeChanged: (ConnectedSpeaker, Int) -> Unit
) : RecyclerView.Adapter<ConnectedSpeakerAdapter.SpeakerViewHolder>() {

    class SpeakerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val speakerIcon: ImageView = itemView.findViewById(R.id.speakerIcon)
        val speakerName: TextView = itemView.findViewById(R.id.speakerName)
        val speakerAddress: TextView = itemView.findViewById(R.id.speakerAddress)
        val connectionStatus: TextView = itemView.findViewById(R.id.connectionStatus)
        val disconnectButton: Button = itemView.findViewById(R.id.disconnectButton)
        val channelSpinner: Spinner = itemView.findViewById(R.id.channelSpinner)
        val speakerVolumeSeekBar: SeekBar = itemView.findViewById(R.id.speakerVolumeSeekBar)
        val speakerVolumeText: TextView = itemView.findViewById(R.id.speakerVolumeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeakerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_connected_speaker, parent, false)
        return SpeakerViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpeakerViewHolder, position: Int) {
        val speaker = speakers[position]
        
        holder.speakerName.text = speaker.getDisplayName()
        holder.speakerAddress.text = speaker.address
        
        // Set connection status
        if (speaker.isConnected) {
            holder.connectionStatus.text = holder.itemView.context.getString(R.string.connected)
            holder.connectionStatus.setTextColor(holder.itemView.context.getColor(R.color.success_green))
        } else {
            holder.connectionStatus.text = holder.itemView.context.getString(R.string.not_connected)
            holder.connectionStatus.setTextColor(holder.itemView.context.getColor(R.color.error_red))
        }
        
        // Setup channel spinner
        val channelAdapter = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_item,
            SurroundChannel.getAvailableChannels().map { it.displayName }
        )
        channelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.channelSpinner.adapter = channelAdapter
        
        // Set current channel selection
        val currentChannelIndex = SurroundChannel.getAvailableChannels().indexOf(speaker.channel)
        holder.channelSpinner.setSelection(currentChannelIndex)
        
        holder.channelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedChannel = SurroundChannel.getAvailableChannels()[position]
                if (selectedChannel != speaker.channel) {
                    speaker.channel = selectedChannel
                    onChannelChanged(speaker, selectedChannel)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Setup volume control
        holder.speakerVolumeSeekBar.progress = speaker.volume
        holder.speakerVolumeText.text = "${speaker.volume}%"
        
        holder.speakerVolumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    speaker.volume = progress
                    holder.speakerVolumeText.text = "$progress%"
                    onVolumeChanged(speaker, progress)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Setup disconnect button
        holder.disconnectButton.setOnClickListener {
            onDisconnectClick(speaker)
        }
        
        // Set channel color indicator
        val channelColor = speaker.getChannelColor()
        if (channelColor != 0) {
            holder.speakerIcon.setColorFilter(channelColor)
        }
    }

    override fun getItemCount(): Int = speakers.size

    fun updateSpeakers(newSpeakers: List<ConnectedSpeaker>) {
        speakers.clear()
        speakers.addAll(newSpeakers)
        notifyDataSetChanged()
    }
    
    fun addSpeaker(speaker: ConnectedSpeaker) {
        // Check if speaker already exists
        val existingIndex = speakers.indexOfFirst { it.address == speaker.address }
        if (existingIndex >= 0) {
            speakers[existingIndex] = speaker
            notifyItemChanged(existingIndex)
        } else {
            speakers.add(speaker)
            notifyItemInserted(speakers.size - 1)
        }
    }
    
    fun removeSpeaker(speakerAddress: String) {
        val index = speakers.indexOfFirst { it.address == speakerAddress }
        if (index >= 0) {
            speakers.removeAt(index)
            notifyItemRemoved(index)
        }
    }
    
    fun updateSpeakerConnectionState(speakerAddress: String, isConnected: Boolean) {
        val index = speakers.indexOfFirst { it.address == speakerAddress }
        if (index >= 0) {
            speakers[index].isConnected = isConnected
            notifyItemChanged(index)
        }
    }
    
    fun updateSpeakerVolume(speakerAddress: String, volume: Int) {
        val index = speakers.indexOfFirst { it.address == speakerAddress }
        if (index >= 0) {
            speakers[index].volume = volume
            notifyItemChanged(index)
        }
    }
}