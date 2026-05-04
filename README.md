# Welcome To FlipVerse!

FlipVerse is a Kotlin Multiplatform Mobile (KMM) application featuring real-time chat, LiveBooks, and social features powered by Firebase.

## 🚀 Quick Start

### iOS Development (Xcode)

**New to the project?** Use our automated setup:

```bash
chmod +x setup_xcode.sh
./setup_xcode.sh
```

Then open in Xcode:
```bash
open iosApp/iosApp.xcworkspace
```

📖 **Detailed guides**:
- [Xcode Quick Start](XCODE_QUICK_START.md) - Fast setup in 5 commands
- [Xcode Setup Guide](XCODE_SETUP_GUIDE.md) - Complete step-by-step instructions
- [iOS Crashlytics Setup](IOS_CRASHLYTICS_SETUP.md) - Firebase Crashlytics integration

### Android Development

```bash
./gradlew :composeApp:assembleDebug
```

Or open the project in Android Studio and run the `composeApp` configuration.

## 📋 Prerequisites

### For iOS Development
- macOS 12.0+ (Monterey or later)
- Xcode 14.0+
- JDK 17
- CocoaPods

### For Android Development
- JDK 17
- Android Studio Hedgehog or later
- Android SDK 34+

## 🏗️ Project Structure

```
FlipVerse/
├── composeApp/          # Compose Multiplatform UI
├── data/                # Data layer (KMM module)
├── shared/              # Shared business logic
├── feature/             # Feature modules
│   ├── auth/           # Authentication
│   ├── chat/           # Real-time chat
│   ├── dashboard/      # Main dashboard
│   ├── explore/        # Explore content
│   ├── livebook/       # LiveBook feature
│   ├── notification/   # Push notifications
│   └── userprofile/    # User profiles
├── iosApp/             # iOS-specific code
├── navigation/         # Navigation module
└── di/                 # Dependency injection
```

## ✨ Features

- 🔐 **Authentication** - Firebase Auth integration
- 💬 **Real-time Chat** - Instant messaging
- 📚 **LiveBooks** - Collaborative storytelling
- 🔍 **Explore** - Discover content
- 📱 **Push Notifications** - Firebase Cloud Messaging
- 👤 **User Profiles** - Profile management
- 📊 **Dashboard** - User activity feed
- 🔥 **Firebase Integration** - Firestore, Storage, Functions, Crashlytics

## 🛠️ Technology Stack

- **Kotlin Multiplatform** - Shared business logic
- **Compose Multiplatform** - Cross-platform UI
- **Firebase** - Backend services (Firestore, Auth, Storage, Functions, Crashlytics, Messaging)
- **Koin** - Dependency injection
- **Ktor** - Networking
- **Coil** - Image loading
- **Kotlinx Serialization** - JSON parsing
- **Room** - Local database

## 📱 Supported Platforms

- ✅ **iOS** 14.0+
- ✅ **Android** API 24+ (Android 7.0+)

## 🔧 Build Commands

### Gradle Commands

```bash
# Clean build
./gradlew clean

# Build all modules
./gradlew build

# Build iOS frameworks
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# Install CocoaPods dependencies
./gradlew podInstall

# Build Android debug APK
./gradlew :composeApp:assembleDebug

# Build Android release APK
./gradlew :composeApp:assembleRelease
# Welcome To FlipVerse! 🎉

FlipVerse is a Kotlin Multiplatform application built with Compose Multiplatform, supporting both Android and iOS platforms.

---

## 📚 **New to the Project? Start Here!**

👉 **[GETTING_STARTED.md](GETTING_STARTED.md)** - Complete step-by-step guide for iOS setup

👉 **[DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)** - Find all documentation

---

## 🚀 Quick Start

### For iOS Development (Xcode)

**Brand New?** Follow the **[Getting Started Guide](GETTING_STARTED.md)** for detailed instructions.

**Experienced Developer?** Use the **[Quick Start Checklist](QUICK_START_CHECKLIST.md)**.

**Essential Commands:**
```bash
# Build iOS framework (M1/M2/M3 Mac)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Open in Xcode
open iosApp/iosApp.xcodeproj
```

### For Android Development
Open the project in Android Studio and sync Gradle. The project is ready to run!

## 📱 Project Structure

This is a **Kotlin Multiplatform (KMP)** project with the following modules:

- **`composeApp/`** - Main Compose Multiplatform application
- **`iosApp/`** - iOS-specific Swift code and Xcode project
- **`shared/`** - Shared business logic
- **`data/`** - Data layer with repository pattern
- **`feature/`** - Feature modules:
  - `auth/` - Authentication
  - `chat/` - Chat functionality
  - `dashboard/` - Main dashboard
  - `explore/` - Explore features
  - `livebook/` - Livebook functionality
  - `notification/` - Notifications
  - `userprofile/` - User profile management
- **`navigation/`** - App navigation logic
- **`di/`** - Dependency injection setup
- **`FIREBASE/`** - Firebase functions

## 🛠 Tech Stack

- **Kotlin Multiplatform** - Share code between platforms
- **Compose Multiplatform** - Modern declarative UI
- **Firebase** - Authentication, Crashlytics, Cloud Functions
- **Ktor** - Networking
- **Room** - Local database
- **Koin** - Dependency injection
- **Coroutines** - Async programming

## 📚 Documentation

| Document | Description | Best For |
|----------|-------------|----------|
| **[Getting Started](GETTING_STARTED.md)** ⭐ | Complete step-by-step tutorial | First-time setup |
| **[Quick Reference](QUICK_REFERENCE.md)** 🚀 | Essential commands & shortcuts | Daily development |
| **[Quick Start Checklist](QUICK_START_CHECKLIST.md)** | Fast setup with checkboxes | Experienced developers |
| **[Documentation Index](DOCUMENTATION_INDEX.md)** | Complete documentation map | Finding the right guide |
| **[Setup Flowchart](SETUP_FLOWCHART.md)** | Visual decision tree | Choosing your path |
| **[Xcode Setup Guide](XCODE_SETUP_GUIDE.md)** | Comprehensive reference | Troubleshooting |
| **[iOS Crashlytics Setup](IOS_CRASHLYTICS_SETUP.md)** | Firebase Crashlytics config | After initial setup |
| **[Livebook Changes](LIVEBOOK_CHANGES_SUMMARY.md)** | Feature implementation details | Understanding the code |

## 🔧 Common Commands

```bash
# Build iOS framework for simulator (M1/M2/M3 Mac)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Build iOS framework for physical device
./gradlew :composeApp:linkDebugFrameworkIosArm64

# Clean build
./gradlew clean

# Run tests
./gradlew test
```

### Xcode Commands

```bash
# Open workspace in Xcode
open iosApp/iosApp.xcworkspace

# Build from command line
xcodebuild -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  build
```

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run iOS simulator tests
./gradlew iosSimulatorArm64Test

# Run Android tests
./gradlew :composeApp:testDebugUnitTest
```

## 📖 Documentation

- [Xcode Quick Start](XCODE_QUICK_START.md) - Get started with iOS in 5 minutes
- [Xcode Setup Guide](XCODE_SETUP_GUIDE.md) - Comprehensive iOS setup
- [iOS Crashlytics Setup](IOS_CRASHLYTICS_SETUP.md) - Firebase Crashlytics for iOS
- [LiveBook Changes Summary](LIVEBOOK_CHANGES_SUMMARY.md) - LiveBook feature documentation

## 🤝 Contributing

1. Create a feature branch from `mvp/phase-1`
2. Make your changes
3. Test on both iOS and Android
4. Submit a pull request

## 📄 License

[License information to be added]

## 🆘 Support

For issues and questions:
- Check the [Xcode Setup Guide](XCODE_SETUP_GUIDE.md) for iOS issues
- Check the [Quick Start](XCODE_QUICK_START.md) for common fixes
- Review project documentation in the respective feature modules

---

**Happy Coding! 🚀**
## 🐛 Troubleshooting

Having issues? Check the [Troubleshooting section](XCODE_SETUP_GUIDE.md#troubleshooting) in the iOS Setup Guide.

## 📄 License

[Add your license here]

---

**Built with ❤️ using Kotlin Multiplatform**
