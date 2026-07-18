# Security Policy

## Supported Versions

| Version | Supported |
|---------|------------|
| 1.0.0   | ✅        |

## Reporting a Vulnerability

If you discover a security vulnerability in SMS Forwarder, please report it responsibly.

### How to Report

**DO NOT** open a public issue for security vulnerabilities.

Instead, please send an email to the project maintainers:

- Email: ringlabs3@gmail.com
- Subject: Security Vulnerability in SMS Forwarder

### What to Include

Please include the following information in your report:

- Description of the vulnerability
- Steps to reproduce the issue
- Potential impact of the vulnerability
- Any proof-of-concept code or screenshots
- Suggested fix (if known)

### Response Timeline

- **Initial Response**: Within 48 hours
- **Investigation**: Within 1 week
- **Resolution**: As soon as feasible, based on severity

### Security Updates

When a security vulnerability is reported and fixed:

1. A new release will be published with the fix
2. The vulnerability will be documented in the release notes
3. Users will be encouraged to update to the latest version

## Security Best Practices

### For Users

1. **Keep the App Updated**: Always use the latest version from the official repository
2. **Secure Webhook Endpoints**: Ensure your webhook URLs use HTTPS and proper authentication
3. **Review Permissions**: Only grant necessary permissions
4. **Device Security**: Keep your device locked and updated
5. **Network Security**: Use secure networks when configuring the app

### For Developers

1. **Code Review**: All code changes should undergo security review
2. **Dependency Updates**: Keep dependencies updated to patch known vulnerabilities
3. **Input Validation**: Validate all user inputs and external data
4. **Secure Storage**: Use secure storage for sensitive data
5. **Network Security**: Use HTTPS for all network communications

## Known Security Considerations

### Data Privacy

- **SMS Content**: The app processes SMS content to forward it to configured webhooks
- **Contact Access**: Contact access is used only for whitelisting functionality
- **Local Storage**: Message logs are stored locally on the device
- **Network Transmission**: SMS data is transmitted to user-configured webhook URLs

### Permissions

The app requires the following sensitive permissions:

- `RECEIVE_SMS`: To receive incoming SMS messages
- `READ_SMS`: To read SMS content for forwarding
- `READ_CONTACTS`: To access contacts for whitelisting
- `INTERNET`: To send data to webhook URLs

### Recommendations

- Use authenticated webhook endpoints
- Implement rate limiting on your webhook servers
- Monitor webhook endpoint for suspicious activity
- Regularly review and rotate webhook authentication tokens
- Keep the app and device OS updated

## Security Features

### Implemented

- ✅ HTTPS support for webhook calls
- ✅ Secure DataStore for settings persistence
- ✅ No hardcoded sensitive information
- ✅ Key store excluded from version control
- ✅ Runtime permission handling
- ✅ Input validation for phone numbers and URLs

### Planned

- 🔲 Webhook authentication (API keys, OAuth)
- 🔲 End-to-end encryption options
- 🔲 Biometric authentication for app access
- 🔲 Certificate pinning for webhook calls

## Disclosure Policy

This project follows a coordinated disclosure process:

1. Security researchers report vulnerabilities privately
2. Maintainers investigate and develop fixes
3. Security fixes are released in new versions
4. Vulnerability details are disclosed after a grace period

This approach allows users to update before vulnerability details become public knowledge.

## Contact

For security-related questions or concerns:

- **Security Issues**: ringlabs3@gmail.com
- **General Issues**: [GitHub Issues](https://github.com/rabden/webhook-smsforwarder/issues)
- **Project Maintainers**: Hossain Jahed

---

Thank you for helping keep SMS Forwarder secure!