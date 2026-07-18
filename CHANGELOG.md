# Changelog

All notable changes to the SMS Forwarder project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned Features
- Webhook authentication methods (API key, OAuth)
- Message content filtering
- Multiple webhook endpoints support
- Message scheduling and delayed forwarding
- Configuration export/import
- Web dashboard for monitoring
- Integration with popular services (Telegram, Discord, Slack)

## [1.0.0] - 2024-06-12

### Added
- Initial release of SMS Forwarder
- **Core Features**
  - Automatic SMS forwarding to webhook URLs
  - Contact whitelisting system
  - Dual-SIM support with SIM identification
  - Message logging with status tracking
  - Custom HTTP headers for webhook authentication
- **Reliability Features**
  - WorkManager-based background processing
  - High reliability mode with foreground service
  - Automatic retry mechanism for failed webhooks
  - Boot receiver for auto-start on device reboot
- **User Interface**
  - Material Design 3 with Jetpack Compose
  - Bottom navigation with Contacts, Logs, and Settings screens
  - Real-time status monitoring
  - Dark theme support
  - Intuitive contact management with picker
- **Device Optimization**
  - Brand-specific optimization guides for 15+ manufacturers
  - Battery optimization detection and guidance
  - Deep links to manufacturer-specific settings
- **Technical Features**
  - MVVM architecture with Repository pattern
  - Room database for local storage
  - DataStore for settings persistence
  - Retrofit + OkHttp for HTTP requests
  - Kotlin Coroutines for asynchronous operations
  - Phone number normalization for international formats

### Security
- Secure DataStore for sensitive settings
- HTTPS support for webhook calls
- Proper permission handling
- No sensitive data logging

### Documentation
- Comprehensive README with usage instructions
- Architecture documentation
- Webhook integration examples
- Troubleshooting guide
- Development setup instructions

### Tested On
- Android 8.0 (API 26) through Android 15 (API 35)
- Various device brands (Xiaomi, Samsung, Oppo, Vivo, etc.)
- Dual-SIM devices
- Different Android skins (MIUI, OneUI, ColorOS, etc.)

---

## Version Format

The changelog uses the following format:

- `[Unreleased]` - Features planned but not yet released
- `[1.0.0]` - Current stable release
- Previous versions in descending order

### Categories

- **Added** - New features
- **Changed** - Changes in existing functionality
- **Deprecated** - Soon-to-be removed features
- **Removed** - Removed features
- **Fixed** - Bug fixes
- **Security** - Security vulnerability fixes