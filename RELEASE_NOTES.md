# Flipverse v1.0.1 - Release Build

## Build Information
- **Version Code**: 2
- **Version Name**: 1.0.1
- **Build Date**: February 17, 2026
- **Bundle File**: `composeApp/build/outputs/bundle/release/composeApp-release.aab`
- **File Size**: 50MB
- **Signing**: Release keystore (flipverse-release.jks)

## Key Changes in v1.0.1

### iOS Improvements
- ✅ Fixed launch screen logo visibility in both light and dark modes
- ✅ Updated app icon label from "FlipVerse" to "Flipverse"
- ✅ Fixed notification badge clearing when notifications are read
- ✅ Enhanced launch screen design with adaptive logo

### Android Improvements
- ✅ Fixed TTS (Text-to-Speech) engine initialization on Android
- ✅ Improved error handling for TTS speak functionality
- ✅ Added proper activity context retrieval for TTS
- ✅ Enhanced logging for TTS debugging

### General
- ✅ Created notification icon assets for proper display
- ✅ Improved app stability

## Play Store Upload Instructions

1. **Navigate to Google Play Console**
   - Go to https://play.google.com/console

2. **Create New Release**
   - Select your app: "Flipverse"
   - Go to "Release" section
   - Click "Create new release"

3. **Upload Bundle**
   - Click "Browse files" under "Android App Bundle"
   - Select: `composeApp/build/outputs/bundle/release/composeApp-release.aab`

4. **Review Release**
   - App name: Flipverse
   - Version: 1.0.1 (Build 2)
   - Review all changes in release notes

5. **Submit for Review**
   - Add release notes describing the improvements
   - Check compliance and requirements
   - Submit for review

## Keystore Information
- **File**: `flipverse-release.jks`
- **Alias**: flipverse
- **Key Size**: 2048 bits RSA
- **Validity**: 10,000 days
- **DN**: CN=Flipverse,OU=Development,O=Flipverse,L=Global,ST=Global,C=US

## Next Steps for v1.0.2
- Monitor app reviews and crash reports
- Update TTS and Android compatibility based on user feedback
- Consider implementing additional features requested by users

---

For any issues or questions, refer to the main README.md
