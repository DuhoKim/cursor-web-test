# Bluetooth Surround System Android App

A powerful Android application that connects multiple Bluetooth speakers to create a multi-channel surround sound system. Transform your mobile device into a surround sound hub with professional-grade audio processing capabilities.

## Features

### 🎵 Multi-Channel Audio Support
- **Surround Sound Modes**: Support for Stereo, 4.0, 4.1, 5.0, 5.1, and 7.1 surround configurations
- **Channel Assignment**: Assign speakers to specific channels (Front Left/Right, Rear Left/Right, Center, Subwoofer)
- **Real-time Processing**: Low-latency audio processing for synchronized playback

### 🔊 Advanced Audio Controls
- **Master Volume**: Global volume control for all connected speakers
- **Individual Volume**: Per-speaker volume adjustment
- **Equalizer**: 4-band equalizer with Bass, Mid, Treble, and Presence controls
- **Bass Boost**: Dedicated bass enhancement for supported speakers
- **Delay Compensation**: Automatic and manual delay adjustment for audio sync

### 📱 Bluetooth Management
- **Device Discovery**: Scan and discover nearby Bluetooth audio devices
- **Multi-Device Connection**: Connect to multiple speakers simultaneously
- **Connection Management**: Easy connect/disconnect with visual status indicators
- **Signal Strength**: Real-time signal quality monitoring

### 🎛️ Professional Features
- **Test Sound**: Play test tones through specific channels for speaker positioning
- **Audio Sync**: Intelligent synchronization across all connected speakers
- **Modern UI**: Beautiful Material Design interface with intuitive controls
- **Permission Handling**: Automatic permission management for Android 12+ compatibility

## Requirements

- **Android Version**: Android 5.0 (API 21) or higher
- **Bluetooth**: Bluetooth 4.0 or higher
- **Permissions**: Location, Bluetooth, Audio recording and modification
- **Hardware**: Device with Bluetooth capability and audio output

## Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/bluetooth-surround-system.git
   cd bluetooth-surround-system
   ```

2. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned directory and select it

3. **Build the project**:
   ```bash
   ./gradlew build
   ```

4. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```

## Usage

### Initial Setup
1. **Launch the app** and grant the required permissions
2. **Enable Bluetooth** if not already enabled
3. **Scan for devices** using the "Scan for Devices" button

### Connecting Speakers
1. **Discover speakers** by scanning for nearby Bluetooth devices
2. **Connect to speakers** by tapping the "Connect" button for each device
3. **Assign channels** using the dropdown menu for each connected speaker
4. **Adjust volumes** using the individual volume sliders

### Surround Sound Configuration
1. **Position speakers** according to your desired surround setup:
   - **Front Left/Right**: Primary stereo speakers
   - **Rear Left/Right**: Surround speakers behind the listening position
   - **Center**: Dialog and vocal enhancement speaker
   - **Subwoofer**: Low-frequency effects speaker

2. **Test the setup** using the "Play Test Sound" button
3. **Fine-tune** volume levels and channel assignments as needed

### Audio Controls
- **Master Volume**: Controls overall system volume
- **Individual Volume**: Fine-tune each speaker's output
- **Test Sound**: Plays audio through each channel sequentially
- **Channel Assignment**: Assign speakers to specific audio channels

## Technical Architecture

### Core Components

#### BluetoothManager
- Handles device discovery and connection management
- Manages multiple simultaneous Bluetooth connections
- Provides connection state monitoring and error handling

#### SurroundSoundManager
- Coordinates multi-channel audio processing
- Manages channel assignments and surround mode detection
- Handles audio effects and volume control

#### AudioProcessor
- Individual speaker audio processing
- Volume control and audio effects application
- Real-time audio synchronization

#### UI Components
- Modern Material Design interface
- RecyclerView adapters for device and speaker lists
- Custom controls for volume and channel management

### Audio Processing Pipeline
1. **Audio Input**: Capture audio from device sources
2. **Channel Separation**: Split audio into surround channels
3. **Processing**: Apply volume, EQ, and effects
4. **Synchronization**: Ensure all speakers play in sync
5. **Bluetooth Transmission**: Send processed audio to each speaker

## Supported Surround Modes

| Mode | Channels | Description |
|------|----------|-------------|
| Stereo | 2.0 | Front Left + Front Right |
| Surround 4.0 | 4.0 | Front L/R + Rear L/R |
| Surround 4.1 | 4.1 | 4.0 + Subwoofer |
| Surround 5.0 | 5.0 | 4.0 + Center |
| Surround 5.1 | 5.1 | 5.0 + Subwoofer |
| Surround 7.1 | 7.1 | 5.1 + Side L/R |

## Troubleshooting

### Common Issues

**Bluetooth connection fails**:
- Ensure speakers are in pairing mode
- Check Bluetooth permissions are granted
- Verify device compatibility

**Audio synchronization issues**:
- Use delay compensation settings
- Ensure speakers are within optimal range
- Check for interference sources

**Performance issues**:
- Limit number of simultaneous connections
- Close unnecessary background apps
- Check device audio processing capabilities

### Performance Tips
- **Optimal Range**: Keep speakers within 10 meters of the device
- **Interference**: Avoid WiFi routers and other 2.4GHz devices
- **Battery**: Use power-saving mode when possible
- **Audio Quality**: Balance quality settings with performance needs

## Contributing

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit changes**: `git commit -m 'Add amazing feature'`
4. **Push to branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Android Bluetooth API documentation
- Material Design guidelines
- Audio processing algorithms and best practices
- Open source audio libraries and frameworks

## Support

For support, please open an issue on GitHub or contact the development team.

---

**Note**: This application requires compatible Bluetooth speakers and may have limitations based on device capabilities and Bluetooth codec support. Performance may vary depending on the number of connected speakers and device specifications.