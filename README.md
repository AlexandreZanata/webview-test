# WebView Test - Enhanced Android WebView Application

## Overview
This project is a high-performance Android WebView application with enhanced photo capabilities, real-time chat notifications, and optimized performance features.

## Features

### 🚀 WebView Performance Optimization
- **Hardware acceleration** enabled for maximum performance
- **Advanced caching strategies** with optimized cache settings
- **Resource preloading** capabilities
- **High render priority** for smooth scrolling and interactions
- **Mixed content support** for broader compatibility
- **Optimized JavaScript engine** settings

### 📸 Enhanced Photo/Gallery Functionality
- **Dual capture modes**: Camera capture and gallery selection
- **Multiple file selection** support from gallery
- **Graceful permission handling** with fallback options
- **Unified JavaScript API** for seamless web integration
- **Screenshot to PDF** conversion functionality
- **File provider integration** for secure file sharing

### 🔔 Real-time Chat Notifications
- **Background message polling** service
- **Native Android notifications** for new messages
- **Real-time unread count** updates
- **Configurable polling intervals** (3 seconds to 5 minutes)
- **Custom notification channels** for different message types
- **JavaScript event integration** for web app notifications

### 🌉 JavaScript-Native Bridge
- **Enhanced WebView interface** with comprehensive API
- **Custom DOM events** for photo selection and message notifications
- **Device information** exposure to web layer
- **Permission status** checking from JavaScript
- **Location services** integration

## Architecture

### Modular Components
- **MainActivity.kt** - Main activity with enhanced WebView configuration
- **WebViewInterface.kt** - JavaScript bridge for web-native communication
- **PhotoManager.kt** - Photo capture and gallery management
- **CustomNotificationManager.kt** - Advanced notification handling
- **MessagePollingService.kt** - Background message polling
- **PermissionHelper.kt** - Centralized permission management
- **FileUtils.kt** - File handling utilities

### Key Technologies
- **Kotlin** for modern Android development
- **WebView** with optimized settings
- **Android Work Manager** for background tasks
- **Notification Channels** for categorized notifications
- **File Provider** for secure file access
- **Location Services** for GPS integration

## Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24 (Android 7.0) or higher
- Kotlin 1.8+

### Build Instructions
1. Clone or download the project
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and run on device or emulator

```bash
# Using Gradle command line
./gradlew assembleDebug
```

### Permissions Required
The app requests the following permissions:
- **CAMERA** - For photo capture
- **READ_MEDIA_IMAGES/READ_EXTERNAL_STORAGE** - For gallery access
- **ACCESS_FINE_LOCATION/ACCESS_COARSE_LOCATION** - For location services
- **POST_NOTIFICATIONS** - For Android 13+ notification support
- **RECORD_AUDIO** - For audio recording in WebView
- **INTERNET/ACCESS_NETWORK_STATE** - For network connectivity
- **WAKE_LOCK** - For background polling
- **FOREGROUND_SERVICE** - For persistent background operations

## JavaScript API

### Photo Management
```javascript
// Take photo using camera
AndroidInterface.takePhoto();

// Select from gallery (single)
AndroidInterface.selectFromGallery(false);

// Select from gallery (multiple)
AndroidInterface.selectFromGallery(true);

// Capture WebView screenshot
AndroidInterface.captureScreenshot();
```

### Notification System
```javascript
// Show native notification
AndroidInterface.showNotification("Title", "Message", "info");

// Start message polling (interval in minutes)
AndroidInterface.startMessagePolling(1);

// Stop message polling
AndroidInterface.stopMessagePolling();

// Update unread count
AndroidInterface.updateUnreadCount(5);
```

### Device Information
```javascript
// Get device information
const deviceInfo = JSON.parse(AndroidInterface.getDeviceInfo());

// Check permissions status
const permissions = JSON.parse(AndroidInterface.checkPermissions());

// Request permissions
AndroidInterface.requestPermissions();

// Get current location
const location = JSON.parse(AndroidInterface.getLocation());
```

### Custom Events
Listen for these custom events in your web application:

```javascript
// Photo selected from camera
document.addEventListener('photoSelected', function(event) {
    console.log('Photo selected:', event.detail.data);
});

// Images selected from gallery
document.addEventListener('gallerySelected', function(event) {
    console.log('Gallery selection:', event.detail.data);
});

// New message notification received
document.addEventListener('newMessageNotification', function(event) {
    console.log('New message:', event.detail);
    // Update UI to show new message
});
```

## Configuration

### WebView Settings
The WebView is configured with optimal performance settings:
- JavaScript enabled with DOM storage
- Hardware acceleration enabled
- High render priority
- Optimized caching with app cache
- Mixed content allowed for compatibility
- Media playback without user gesture

### Notification Configuration
- **General notifications**: Default priority with blue light
- **Message notifications**: High priority with green light and vibration
- **Polling interval**: Configurable from 3 seconds to 5 minutes
- **Badge counts**: Automatic unread message counting

### File Handling
- **Supported formats**: PDF, CSV, Excel, Word, Images, Text files
- **Download location**: External Downloads directory
- **File security**: FileProvider for secure access
- **Cache management**: Automatic cleanup and size optimization

## Testing

### Unit Testing
```bash
./gradlew test
```

### Integration Testing
```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist
- [ ] Camera capture functionality
- [ ] Gallery selection (single and multiple)
- [ ] WebView performance and scrolling
- [ ] Notification display and interaction
- [ ] Background message polling
- [ ] Permission request flows
- [ ] File download functionality
- [ ] Location services integration

## Performance Considerations

### WebView Optimization
- **Cache size**: Automatically calculated based on available storage
- **Image loading**: Optimized for performance
- **JavaScript execution**: High priority rendering
- **Memory usage**: Efficient cleanup on lifecycle events

### Background Processing
- **Polling service**: Configurable intervals to balance battery and responsiveness
- **Network requests**: Optimized with proper timeouts and error handling
- **Notification throttling**: Prevents spam with intelligent grouping

## Troubleshooting

### Common Issues
1. **Permissions not granted**: Check device settings and ensure all required permissions are enabled
2. **Camera/Gallery not working**: Verify CAMERA and storage permissions are granted
3. **Notifications not showing**: Check notification permissions on Android 13+
4. **WebView performance**: Ensure hardware acceleration is enabled and sufficient memory available

### Debug Logging
Enable debug logging by checking Android Studio Logcat for:
- `MainActivity` - General app lifecycle and errors
- `PhotoManager` - Photo capture and gallery operations
- `CustomNotificationManager` - Notification handling
- `MessagePollingService` - Background polling operations
- `PermissionHelper` - Permission management

## Contributing
1. Fork the repository
2. Create a feature branch
3. Make your changes with proper testing
4. Submit a pull request with detailed description

## License
This project is licensed under the MIT License - see the LICENSE file for details.

## Support
For issues and questions:
1. Check the troubleshooting section
2. Review Android Studio Logcat for error messages
3. Create an issue with detailed reproduction steps