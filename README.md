# SMS Forwarder

<div align="center">

![Android](https://img.shields.io/badge/Android-8.0%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.06.01-purple?logo=jetpackcompose)
![License](https://img.shields.io/badge/License-MIT-yellow)

*Android app that forwards incoming SMS messages from a whitelist of contacts to a configurable webhook via HTTP POST. Built with Kotlin and Jetpack Compose.*

</div>

## 🚀 Overview

SMS Forwarder is a robust Android application that automatically forwards incoming SMS messages to configured webhook URLs. Built with modern Android development practices, it provides reliable message forwarding with advanced features like contact whitelisting, dual-SIM support, and device brand optimization.

## ✨ Features

### Core Functionality
- **Automatic SMS Forwarding**: Real-time forwarding of incoming SMS messages to webhook endpoints
- **Contact Whitelisting**: Select specific contacts whose messages should be forwarded
- **Dual-SIM Support**: Handles multiple SIM cards with proper SIM identification
- **Message Logging**: Complete history of all forwarded messages with status tracking
- **Custom HTTP Headers**: Add custom headers to webhook requests for authentication

### Reliability & Performance
- **Background Processing**: Uses WorkManager for reliable background task execution
- **High Reliability Mode**: Foreground service to prevent app termination by aggressive battery optimizations
- **Device Brand Optimization**: Brand-specific settings for Xiaomi, Samsung, Oppo, Vivo, and other manufacturers
- **Boot Receiver**: Auto-restart on device reboot when enabled
- **Retry Mechanism**: Automatic retry with exponential backoff for failed webhooks

### User Experience
- **Material Design 3**: Modern UI with Jetpack Compose
- **Intuitive Navigation**: Bottom navigation with Contacts, Logs, and Settings screens
- **Real-time Status**: Live monitoring of forwarding status and battery optimization
- **Country Code Normalization**: Smart phone number formatting for international formats
- **Dark Theme**: Supports system theme settings

## 📋 Requirements

- **Android Version**: 8.0 (API level 26) or higher
- **Permissions Required**:
  - `RECEIVE_SMS` - To receive incoming SMS messages
  - `READ_SMS` - To read SMS content
  - `READ_CONTACTS` - For contact whitelisting
  - `INTERNET` - For webhook forwarding
  - `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - For background reliability
  - `POST_NOTIFICATIONS` - For service notifications
  - `FOREGROUND_SERVICE` - For high reliability mode
  - `RECEIVE_BOOT_COMPLETED` - For auto-start on boot

## 🛠️ Installation

### From Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/rabden/webhook-smsforwarder.git
   cd webhook-smsforwarder
   ```

2. **Open in Android Studio**:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Build the project**:
   - Click "Build" > "Make Project"
   - Or use the command line: `./gradlew assembleDebug`

4. **Install on device**:
   - Connect your Android device via USB
   - Click "Run" > "Run 'app'"
   - Or install the APK: `./gradlew installDebug`

### From APK

Download the latest release APK from the [Releases](https://github.com/rabden/webhook-smsforwarder/releases) section and install it on your device.

## 📖 Usage

### Initial Setup

1. **Grant Permissions**: On first launch, grant all required permissions
2. **Configure Webhook**: Enter your webhook URL in Settings
3. **Add Contacts**: Add phone numbers or select contacts to whitelist
4. **Enable Forwarding**: Toggle the forwarding service on

### Webhook Payload Format

The application sends SMS data in the following JSON format:

```json
{
  "sender": "+1234567890",
  "message": "Your SMS content here",
  "sim": "SIM-1",
  "device": "Pixel 7 Pro"
}
```

### Configuration Options

#### Webhook Settings
- **Webhook URL**: The endpoint where SMS data will be sent
- **Device Name**: Custom identifier for your device
- **Custom Headers**: Add HTTP headers for authentication (e.g., `Authorization: Bearer token`)

#### Reliability Settings
- **High Reliability Mode**: Enables foreground service to prevent app termination
- **Battery Optimization**: Guides users to disable battery optimizations for better reliability

#### Contact Management
- **Manual Entry**: Add phone numbers manually
- **Contact Picker**: Select from device contacts
- **Country Code Support**: Automatic normalization for international formats

## 🏗️ Architecture

### Technology Stack

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose with Material3
- **Architecture**: MVVM with Repository pattern
- **Build System**: Gradle with Kotlin DSL
- **Dependency Management**: Version Catalog (libs.versions.toml)

### Key Libraries

- **Jetpack Compose**: Modern UI toolkit
- **Navigation Compose**: In-app navigation
- **Room Database**: Local data persistence
- **Retrofit + OkHttp**: HTTP client for webhook forwarding
- **DataStore**: Settings persistence
- **WorkManager**: Background task scheduling
- **Kotlin Coroutines**: Asynchronous programming

### Project Structure

```
app/src/main/java/com/rabden/smsforwarder/
├── data/                    # Data layer
│   ├── AppDatabase.kt      # Room database
│   ├── MessageLog.kt       # Database entity
│   └── MessageLogDao.kt    # Database access object
├── network/                # Network layer
│   └── WebhookService.kt   # Retrofit webhook client
├── repository/             # Repository layer
│   ├── ContactRepository.kt
│   └── SettingsRepository.kt
├── receiver/               # Broadcast receivers
│   ├── BootReceiver.kt
│   └── SmsReceiver.kt
├── service/                # Services
│   └── SmsForwardingService.kt
├── ui/                     # UI layer
│   ├── components/
│   ├── contacts/
│   ├── logs/
│   ├── navigation/
│   ├── permission/
│   ├── settings/
│   └── theme/
├── util/                   # Utilities
│   ├── BrandHelper.kt
│   └── PermissionHelper.kt
├── worker/                 # WorkManager workers
│   ├── ForwardSmsWorker.kt
│   └── LogSmsWorker.kt
├── MainActivity.kt
└── SmsForwarderApp.kt
```

### Data Flow

1. **SMS Reception**: `SmsReceiver` intercepts incoming SMS
2. **WorkManager Chain**: 
   - `LogSmsWorker` validates and logs to database
   - `ForwardSmsWorker` sends to webhook with retry logic
3. **UI Updates**: ViewModels observe database and settings changes
4. **User Interaction**: Compose UI updates via StateFlow

### Key Components

#### SmsReceiver
- Broadcast receiver for SMS_RECEIVED events
- Extracts message content and sender information
- Identifies SIM card for dual-SIM devices
- Chains WorkManager tasks for processing

#### WorkManager Workers
- **LogSmsWorker**: Validates whitelisting, logs to database
- **ForwardSmsWorker**: Sends webhook requests with retry logic

#### BrandHelper
- Device brand detection (Xiaomi, Samsung, etc.)
- Brand-specific battery optimization settings
- Deep links to manufacturer-specific settings screens

#### PermissionHelper
- Centralized permission management
- Battery optimization exemption requests
- Runtime permission handling

## 🔧 Development

### Build Configuration

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

### Key Build Files

- `build.gradle.kts` - Project-level build configuration
- `app/build.gradle.kts` - App-level build configuration
- `gradle/libs.versions.toml` - Dependency version catalog
- `gradle.properties` - Gradle optimization settings

### Testing

The project includes:
- Unit tests in `app/src/test/`
- Instrumented tests in `app/src/androidTest/`

## 🔒 Security Considerations

- **Webhook URLs**: Stored securely in DataStore
- **Permissions**: Only requests necessary permissions
- **Network Traffic**: Uses HTTPS for webhook calls
- **Data Storage**: Local database with Room encryption capabilities

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Development Guidelines

- Follow Kotlin coding conventions
- Write unit tests for new features
- Update documentation as needed
- Ensure the app works on minimum SDK (API 26)

### Repository Information

- **Repository**: https://github.com/rabden/webhook-smsforwarder
- **Issues**: https://github.com/rabden/webhook-smsforwarder/issues
- **Discussions**: https://github.com/rabden/webhook-smsforwarder/discussions

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Android Jetpack** - For modern Android development tools
- **Jetpack Compose** - For the declarative UI framework
- **Retrofit** - For the HTTP client
- **WorkManager** - For background task management

## 🐛 Troubleshooting

### SMS not forwarding
- Check if forwarding is enabled in Settings
- Verify webhook URL is correct
- Ensure sender is in the whitelist
- Check message logs for error details

### App killed in background
- Enable High Reliability Mode in Settings
- Disable battery optimization for the app
- Follow brand-specific optimization instructions

### Permission issues
- Go to Settings > Apps > SMS Forwarder > Permissions
- Grant all required permissions
- Reboot the device if needed

## 📄 Webhook Integration Example

### Node.js Example Server

```javascript
const express = require('express');
const app = express();

app.use(express.json());

app.post('/webhook', (req, res) => {
  const { sender, message, sim, device } = req.body;
  
  console.log(`Received SMS from ${sender} on ${device} (${sim})`);
  console.log(`Message: ${message}`);
  
  // Process the SMS data
  // Send to database, trigger alerts, etc.
  
  res.status(200).send('OK');
});

app.listen(3000, () => {
  console.log('Webhook server running on port 3000');
});
```

### Python Example Server

```python
from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/webhook', methods=['POST'])
def webhook():
    data = request.json
    sender = data.get('sender')
    message = data.get('message')
    sim = data.get('sim')
    device = data.get('device')
    
    print(f"Received SMS from {sender} on {device} ({sim})")
    print(f"Message: {message}")
    
    # Process the SMS data
    # Send to database, trigger alerts, etc.
    
    return jsonify({'status': 'OK'}), 200

if __name__ == '__main__':
    app.run(port=3000)
```

## 📱 Brand-Specific Optimization

The app includes brand-specific optimization guides for:

- **Xiaomi**: Autostart, battery saver settings, app locking
- **Samsung**: Battery unrestricted, background usage limits
- **Oppo**: Auto-launch, background activity permissions
- **Vivo**: Background power consumption management
- **Huawei/Honor**: App launch settings, background permissions
- **Infinix/Tecno**: Phone Master autostart, app locking
- **And many more...**

Access these guides in Settings > Reliability & Optimization > Brand Optimization.

## 🗺️ Roadmap

- [ ] Add webhook authentication methods (API key, OAuth)
- [ ] Support for message filtering by content
- [ ] Multiple webhook endpoints support
- [ ] Message scheduling and delayed forwarding
- [ ] Export/import configuration
- [ ] Web dashboard for monitoring
- [ ] Integration with popular services (Telegram, Discord, Slack)

## 📞 Support

For issues, questions, or contributions:
- Open an issue on [GitHub](https://github.com/rabden/webhook-smsforwarder/issues)
- Check existing documentation
- Review the troubleshooting section
- Contact: ringlabs3@gmail.com

---

<div align="center">

**Built with ❤️ using Kotlin and Jetpack Compose**

</div>
