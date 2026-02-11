# Decentralized Access Loan (DALS) - Kotlin Multiplatform

A modern DeFi lending platform built with Kotlin Multiplatform and Compose Multiplatform, enabling users to access
decentralized loans across multiple platforms.

## 🏗️ Architecture

This project uses Kotlin Multiplatform to share business logic across:

- **Android** (Compose for Android)
- **iOS** (Compose for iOS)
- **Desktop** (Compose for Desktop - JVM)
- **Web** (Compose for Web - JS/WASM)

## 📱 Features

### Authentication & Security

- **User Registration & Login** with form validation
- **Country Picker for Phone Numbers** - Searchable dropdown with 70+ countries
- **Remember Me & Auto-Login** - Persistent credential storage across platforms
- **Multi-platform Secure Storage**:
    - Android: SharedPreferences
    - iOS: NSUserDefaults
    - Desktop: Properties file
    - Web: localStorage

### Core Functionality

- **Loan Management** - Apply, track, and manage decentralized loans
- **Transaction System** - Send, receive, and track transactions
- **Investment Portfolio** - Manage DeFi investments
- **Real-time Notifications** - Push and in-app notifications
- **KYC Verification** - Complete identity verification process

### User Experience

- **Responsive Design** - Optimized for all screen sizes
- **Dark/Light Theme** - System-aware theme switching
- **Multi-language Support** - Internationalization ready
- **Offline Capability** - Core features work offline

## 🔐 Credential Storage

The app implements secure credential storage across all platforms:

### Registration Flow

1. User completes registration with country picker for phone number
2. Credentials are automatically saved securely
3. Auto-login is enabled by default for new users

### Login Flow

1. **Auto-Login**: App attempts automatic login on startup if enabled
2. **Remember Me**: Manual login with option to save credentials
3. **Credential Management**: Users can clear saved credentials in Settings

### Platform-Specific Storage

- **Android**: Uses `SharedPreferences` with encryption
- **iOS**: Uses `NSUserDefaults` with keychain security
- **Desktop**: Stores in user home directory as encrypted properties
- **Web**: Uses `localStorage` with client-side encryption

## 🛠️ Technical Stack

- **UI Framework**: Compose Multiplatform
- **Architecture**: MVVM with Repository pattern
- **State Management**: StateFlow & Compose State
- **Navigation**: Custom navigation system
- **Dependency Injection**: Manual DI with factory patterns
- **Storage**: Platform-specific secure storage implementations

## 📂 Project Structure

```
composeApp/
├── src/
│   ├── commonMain/kotlin/           # Shared business logic
│   │   ├── org/dals/project/
│   │   │   ├── ui/                  # UI components & screens
│   │   │   │   ├── components/      # Reusable components
│   │   │   │   │   └── CountryPicker.kt  # Country selector with phone input
│   │   │   │   └── screens/         # App screens
│   │   │   ├── viewmodel/          # ViewModels
│   │   │   ├── repository/         # Data repositories
│   │   │   ├── model/              # Data models
│   │   │   ├── storage/            # Multi-platform storage
│   │   │   └── navigation/         # Navigation logic
│   ├── androidMain/kotlin/         # Android-specific code
│   │   └── storage/PreferencesStorage.android.kt
│   ├── iosMain/kotlin/            # iOS-specific code
│   │   └── storage/PreferencesStorage.ios.kt
│   ├── jvmMain/kotlin/            # Desktop-specific code
│   │   └── storage/PreferencesStorage.jvm.kt
│   └── jsMain/kotlin/             # Web-specific code
│       └── storage/PreferencesStorage.js.kt
```

## 🚀 Getting Started

### Prerequisites

- **Android Studio** or **IntelliJ IDEA** with Kotlin Multiplatform plugin
- **Xcode** (for iOS development)
- **JDK 11+** for desktop and server components

### Setup

1. Clone the repository
2. Open in Android Studio/IntelliJ IDEA
3. Sync Gradle dependencies
4. Run platform-specific configurations

### Running the App

#### Android

```bash
./gradlew :composeApp:installDebug
```

#### iOS

1. Open in Xcode via the generated iOS project
2. Run on simulator or device

#### Desktop

```bash
./gradlew :composeApp:run
```

#### Web

```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

## 🔧 Configuration

### Demo Credentials

For testing purposes, use these demo credentials:

- **Username**: `johndoe`
- **Password**: `password123`

### Storage Settings

The app automatically:

- Saves credentials after successful registration
- Enables auto-login for new users
- Allows users to manage login preferences in Settings

## 🌍 Country Picker

The app includes a comprehensive country picker with:

- **70+ Countries** with flags and dial codes
- **Search Functionality** - Search by country name, code, or dial code
- **Auto-format Phone Numbers** - Automatically adds country dial codes
- **Persistent Selection** - Remembers selected country across sessions

## 📱 Platform Features

### Android

- Material 3 Design System
- Edge-to-edge display support
- SharedPreferences for secure storage

### iOS

- Native iOS UI patterns
- NSUserDefaults integration
- iOS-specific navigation

### Desktop

- Native window management
- File system access for storage
- Desktop-optimized layouts

### Web

- Progressive Web App capabilities
- localStorage for persistence
- Responsive web design

## 🔒 Security

- **Encrypted Storage**: All sensitive data is encrypted at rest
- **Secure Transmission**: API calls use HTTPS
- **Input Validation**: Comprehensive form validation
- **Authentication**: Secure login with credential storage

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Kotlin Multiplatform team for the amazing framework
- Compose Multiplatform for cross-platform UI
- Community contributors and feedback

---