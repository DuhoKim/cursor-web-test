package com.surround.bluetoothsystem.managers

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.surround.bluetoothsystem.models.AudioConfiguration
import com.surround.bluetoothsystem.models.ConnectedSpeaker
import com.surround.bluetoothsystem.models.SurroundChannel
import com.surround.bluetoothsystem.models.SurroundMode
import java.util.concurrent.ConcurrentHashMap

class SurroundSoundManager(
    private val context: Context,
    private val listener: SurroundSoundManagerListener
) {
    
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private val connectedSpeakers = ConcurrentHashMap<String, ConnectedSpeaker>()
    private val channelMapping = ConcurrentHashMap<SurroundChannel, ConnectedSpeaker>()
    private val audioProcessors = ConcurrentHashMap<String, AudioProcessor>()
    
    private var audioConfiguration = AudioConfiguration()
    private var currentSurroundMode = SurroundMode.STEREO
    private var isSystemActive = false
    private var testSoundPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    
    companion object {
        private const val TAG = "SurroundSoundManager"
        private const val TEST_SOUND_DURATION_MS = 5000L
    }
    
    fun addSpeaker(speaker: ConnectedSpeaker) {
        connectedSpeakers[speaker.address] = speaker
        updateChannelMapping()
        checkSystemStatus()
        
        // Create audio processor for this speaker
        val processor = AudioProcessor(speaker, audioConfiguration)
        audioProcessors[speaker.address] = processor
        
        Log.d(TAG, "Added speaker: ${speaker.name} (${speaker.address})")
    }
    
    fun removeSpeaker(speakerAddress: String) {
        connectedSpeakers.remove(speakerAddress)?.let { speaker ->
            channelMapping.remove(speaker.channel)
            audioProcessors.remove(speakerAddress)?.destroy()
            updateChannelMapping()
            checkSystemStatus()
            
            Log.d(TAG, "Removed speaker: ${speaker.name} (${speaker.address})")
        }
    }
    
    fun updateSpeakerChannel(speakerAddress: String, channel: SurroundChannel) {
        connectedSpeakers[speakerAddress]?.let { speaker ->
            // Remove old channel mapping
            channelMapping.remove(speaker.channel)
            
            // Update speaker channel
            speaker.channel = channel
            
            // Add new channel mapping
            if (channel != SurroundChannel.UNASSIGNED) {
                channelMapping[channel] = speaker
            }
            
            updateChannelMapping()
            checkSystemStatus()
            
            Log.d(TAG, "Updated speaker channel: ${speaker.name} -> ${channel.displayName}")
        }
    }
    
    fun updateSpeakerVolume(speakerAddress: String, volume: Int) {
        connectedSpeakers[speakerAddress]?.let { speaker ->
            speaker.volume = volume
            audioProcessors[speakerAddress]?.updateVolume(volume)
            
            Log.d(TAG, "Updated speaker volume: ${speaker.name} -> $volume%")
        }
    }
    
    fun updateMasterVolume(volume: Int) {
        audioConfiguration.masterVolume = volume
        
        // Update all audio processors with new master volume
        audioProcessors.values.forEach { processor ->
            processor.updateMasterVolume(volume)
        }
        
        Log.d(TAG, "Updated master volume: $volume%")
    }
    
    fun updateAudioConfiguration(config: AudioConfiguration) {
        audioConfiguration = config
        
        // Update all audio processors with new configuration
        audioProcessors.values.forEach { processor ->
            processor.updateConfiguration(config)
        }
        
        Log.d(TAG, "Updated audio configuration")
    }
    
    fun playTestSound() {
        if (testSoundPlayer != null) {
            stopTestSound()
        }
        
        try {
            // Play test sound through each channel sequentially
            playTestSoundForChannels()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing test sound", e)
        }
    }
    
    fun stopTestSound() {
        testSoundPlayer?.let { player ->
            player.stop()
            player.release()
            testSoundPlayer = null
        }
    }
    
    fun getCurrentSurroundMode(): SurroundMode {
        return currentSurroundMode
    }
    
    fun isSystemActive(): Boolean {
        return isSystemActive
    }
    
    fun getConnectedSpeakers(): List<ConnectedSpeaker> {
        return connectedSpeakers.values.toList()
    }
    
    fun getChannelMapping(): Map<SurroundChannel, ConnectedSpeaker> {
        return channelMapping.toMap()
    }
    
    fun getAudioConfiguration(): AudioConfiguration {
        return audioConfiguration
    }
    
    fun startAudioProcessing() {
        if (!isSystemActive) {
            return
        }
        
        // Initialize audio effects
        initializeAudioEffects()
        
        // Start audio processing for all speakers
        audioProcessors.values.forEach { processor ->
            processor.startProcessing()
        }
        
        Log.d(TAG, "Started audio processing for surround system")
    }
    
    fun stopAudioProcessing() {
        // Stop audio processing for all speakers
        audioProcessors.values.forEach { processor ->
            processor.stopProcessing()
        }
        
        // Release audio effects
        releaseAudioEffects()
        
        Log.d(TAG, "Stopped audio processing for surround system")
    }
    
    private fun updateChannelMapping() {
        channelMapping.clear()
        
        connectedSpeakers.values.forEach { speaker ->
            if (speaker.channel != SurroundChannel.UNASSIGNED) {
                channelMapping[speaker.channel] = speaker
            }
        }
    }
    
    private fun checkSystemStatus() {
        val previousStatus = isSystemActive
        
        // Determine if system is active based on connected speakers and their channels
        isSystemActive = when {
            connectedSpeakers.isEmpty() -> false
            connectedSpeakers.size == 1 -> true // Mono/Single speaker
            channelMapping.size >= 2 -> true // At least stereo
            else -> false
        }
        
        // Update surround mode based on connected channels
        currentSurroundMode = determineSurroundMode()
        
        if (previousStatus != isSystemActive) {
            listener.onSystemStatusChanged(isSystemActive, currentSurroundMode)
        }
    }
    
    private fun determineSurroundMode(): SurroundMode {
        val assignedChannels = channelMapping.keys
        
        return when {
            assignedChannels.contains(SurroundChannel.FRONT_LEFT) &&
            assignedChannels.contains(SurroundChannel.FRONT_RIGHT) &&
            assignedChannels.contains(SurroundChannel.REAR_LEFT) &&
            assignedChannels.contains(SurroundChannel.REAR_RIGHT) &&
            assignedChannels.contains(SurroundChannel.CENTER) &&
            assignedChannels.contains(SurroundChannel.SUBWOOFER) -> SurroundMode.SURROUND_5_1
            
            assignedChannels.contains(SurroundChannel.FRONT_LEFT) &&
            assignedChannels.contains(SurroundChannel.FRONT_RIGHT) &&
            assignedChannels.contains(SurroundChannel.REAR_LEFT) &&
            assignedChannels.contains(SurroundChannel.REAR_RIGHT) &&
            assignedChannels.contains(SurroundChannel.CENTER) -> SurroundMode.SURROUND_5_0
            
            assignedChannels.contains(SurroundChannel.FRONT_LEFT) &&
            assignedChannels.contains(SurroundChannel.FRONT_RIGHT) &&
            assignedChannels.contains(SurroundChannel.REAR_LEFT) &&
            assignedChannels.contains(SurroundChannel.REAR_RIGHT) &&
            assignedChannels.contains(SurroundChannel.SUBWOOFER) -> SurroundMode.SURROUND_4_1
            
            assignedChannels.contains(SurroundChannel.FRONT_LEFT) &&
            assignedChannels.contains(SurroundChannel.FRONT_RIGHT) &&
            assignedChannels.contains(SurroundChannel.REAR_LEFT) &&
            assignedChannels.contains(SurroundChannel.REAR_RIGHT) -> SurroundMode.SURROUND_4_0
            
            assignedChannels.contains(SurroundChannel.FRONT_LEFT) &&
            assignedChannels.contains(SurroundChannel.FRONT_RIGHT) -> SurroundMode.STEREO
            
            else -> SurroundMode.STEREO
        }
    }
    
    private fun playTestSoundForChannels() {
        val channels = channelMapping.keys.toList()
        if (channels.isEmpty()) {
            return
        }
        
        var currentChannelIndex = 0
        
        fun playNextChannel() {
            if (currentChannelIndex < channels.size) {
                val channel = channels[currentChannelIndex]
                val speaker = channelMapping[channel]
                
                speaker?.let {
                    // Play test sound for this channel
                    playTestSoundForChannel(channel)
                    
                    // Schedule next channel
                    handler.postDelayed({
                        currentChannelIndex++
                        playNextChannel()
                    }, TEST_SOUND_DURATION_MS / channels.size)
                }
            }
        }
        
        playNextChannel()
    }
    
    private fun playTestSoundForChannel(channel: SurroundChannel) {
        try {
            // Create a simple test tone or use a predefined test sound
            // For now, we'll use a simple beep sound
            
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            
            // Here you would implement channel-specific audio routing
            // This is a simplified version
            
            Log.d(TAG, "Playing test sound for channel: ${channel.displayName}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing test sound for channel ${channel.displayName}", e)
        }
    }
    
    private fun initializeAudioEffects() {
        try {
            // Initialize equalizer
            equalizer = Equalizer(0, 0).apply {
                enabled = true
                // Set equalizer bands based on configuration
                val eqSettings = audioConfiguration.equalizer
                if (numberOfBands >= 4) {
                    setBandLevel(0, (eqSettings.bass * 100).toShort()) // Bass
                    setBandLevel(1, (eqSettings.mid * 100).toShort())  // Mid
                    setBandLevel(2, (eqSettings.treble * 100).toShort()) // Treble
                    if (numberOfBands > 3) {
                        setBandLevel(3, (eqSettings.presence * 100).toShort()) // Presence
                    }
                }
            }
            
            // Initialize bass boost
            bassBoost = BassBoost(0, 0).apply {
                enabled = audioConfiguration.bassBoost > 0
                setStrength((audioConfiguration.bassBoost * 100).toShort())
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing audio effects", e)
        }
    }
    
    private fun releaseAudioEffects() {
        equalizer?.release()
        equalizer = null
        
        bassBoost?.release()
        bassBoost = null
    }
    
    fun destroy() {
        stopTestSound()
        stopAudioProcessing()
        
        // Clean up all audio processors
        audioProcessors.values.forEach { processor ->
            processor.destroy()
        }
        audioProcessors.clear()
        
        releaseAudioEffects()
        
        connectedSpeakers.clear()
        channelMapping.clear()
    }
    
    interface SurroundSoundManagerListener {
        fun onSystemStatusChanged(isActive: Boolean, surroundMode: SurroundMode)
        fun onChannelConfigurationChanged(channelMapping: Map<SurroundChannel, ConnectedSpeaker>)
        fun onAudioProcessingStarted()
        fun onAudioProcessingStopped()
        fun onError(error: String)
    }
}

// Simple audio processor for individual speakers
class AudioProcessor(
    private val speaker: ConnectedSpeaker,
    private var audioConfiguration: AudioConfiguration
) {
    private var isProcessing = false
    
    fun updateVolume(volume: Int) {
        speaker.volume = volume
        // Update audio output volume for this speaker
    }
    
    fun updateMasterVolume(masterVolume: Int) {
        audioConfiguration.masterVolume = masterVolume
        // Apply master volume to this speaker's output
    }
    
    fun updateConfiguration(config: AudioConfiguration) {
        audioConfiguration = config
        // Apply new configuration to this speaker's audio processing
    }
    
    fun startProcessing() {
        if (!isProcessing) {
            isProcessing = true
            // Start audio processing for this speaker
        }
    }
    
    fun stopProcessing() {
        if (isProcessing) {
            isProcessing = false
            // Stop audio processing for this speaker
        }
    }
    
    fun destroy() {
        stopProcessing()
        // Clean up resources
    }
}