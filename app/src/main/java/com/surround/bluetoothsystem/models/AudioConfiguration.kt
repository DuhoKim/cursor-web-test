package com.surround.bluetoothsystem.models

data class AudioConfiguration(
    var masterVolume: Int = 50,
    var bassBoost: Int = 0,
    var trebleBoost: Int = 0,
    var audioSync: Boolean = true,
    var globalDelayCompensation: Int = 0,
    var equalizer: EqualizerSettings = EqualizerSettings()
) {
    fun getVolumeMultiplier(): Float {
        return masterVolume / 100.0f
    }
    
    fun isValidConfiguration(): Boolean {
        return masterVolume in 0..100 &&
               bassBoost in -10..10 &&
               trebleBoost in -10..10 &&
               globalDelayCompensation in 0..500
    }
}

data class EqualizerSettings(
    var bass: Int = 0,      // -10 to 10
    var mid: Int = 0,       // -10 to 10
    var treble: Int = 0,    // -10 to 10
    var presence: Int = 0   // -10 to 10
) {
    fun reset() {
        bass = 0
        mid = 0
        treble = 0
        presence = 0
    }
    
    fun isFlat(): Boolean {
        return bass == 0 && mid == 0 && treble == 0 && presence == 0
    }
}

enum class AudioFormat {
    PCM_16BIT,
    PCM_24BIT,
    PCM_32BIT,
    AAC,
    SBC,
    LDAC
}

enum class SurroundMode {
    STEREO,
    SURROUND_4_0,
    SURROUND_4_1,
    SURROUND_5_0,
    SURROUND_5_1,
    SURROUND_7_1
}