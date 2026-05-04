/**
 * CocoaPods pod directory names for framework search paths (-F).
 * Each entry is a subdirectory under PODS_CONFIGURATION_BUILD_DIR.
 */
private val podSearchDirs = listOf(
    "FirebaseCore", "FirebaseAuth", "FirebaseAuthInterop",
    "FirebaseFirestore", "FirebaseFirestoreInternal",
    "FirebaseFunctions", "FirebaseStorage", "FirebaseCrashlytics",
    "FirebaseMessaging",
    "FirebaseCoreInternal", "FirebaseCoreExtension",
    "FirebaseInstallations", "FirebaseSessions",
    "FirebaseSharedSwift", "FirebaseAppCheckInterop",
    "FirebaseMessagingInterop", "FirebaseRemoteConfigInterop",
    "GoogleUtilities", "GoogleDataTransport", "GoogleSignIn",
    "GTMAppAuth", "GTMSessionFetcher", "AppAuth", "AppCheckCore",
    "PromisesObjC", "PromisesSwift", "RecaptchaInterop",
    "nanopb", "abseil", "gRPC-C++", "gRPC-Core",
    "BoringSSL-GRPC", "leveldb-library"
)

/**
 * Framework names to actually link (-framework).
 * These are the product names inside the CocoaPods framework bundles.
 * Note: some differ from directory names (e.g. PromisesObjC -> FBLPromises).
 */
private val frameworkLinks = listOf(
    "FirebaseCore", "FirebaseAuth", "FirebaseAuthInterop",
    "FirebaseFirestore", "FirebaseFirestoreInternal",
    "FirebaseFunctions", "FirebaseStorage", "FirebaseCrashlytics",
    "FirebaseMessaging",
    "FirebaseCoreInternal", "FirebaseCoreExtension",
    "FirebaseInstallations", "FirebaseSessions",
    "FirebaseSharedSwift", "FirebaseAppCheckInterop",
    "FirebaseMessagingInterop", "FirebaseRemoteConfigInterop",
    "GoogleUtilities", "GoogleDataTransport", "GoogleSignIn",
    "GTMAppAuth", "GTMSessionFetcher", "AppAuth", "AppCheckCore",
    "FBLPromises", "Promises", "RecaptchaInterop",
    "nanopb", "absl", "grpcpp", "grpc",
    "openssl_grpc", "leveldb"
)

/**
 * Returns linker opts for finding AND linking CocoaPods Firebase frameworks.
 * Only returns non-empty list when PODS_CONFIGURATION_BUILD_DIR is set (i.e., building from Xcode).
 */
fun cocoapodsLinkerOpts(): List<String> {
    val podsBuildDir = System.getenv("PODS_CONFIGURATION_BUILD_DIR") ?: return emptyList()
    val platformName = System.getenv("PLATFORM_NAME") ?: "iphonesimulator"
    val toolchainDir = System.getenv("TOOLCHAIN_DIR")
        ?: "/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain"

    val searchPaths = podSearchDirs.map { "-F$podsBuildDir/$it" }
    val linkFlags = frameworkLinks.flatMap { listOf("-framework", it) }
    // Swift toolchain library paths needed for Swift-based CocoaPods frameworks
    val swiftLibPaths = listOf(
        "-L$toolchainDir/usr/lib/swift/$platformName",
        "-L/usr/lib/swift"
    )
    return searchPaths + linkFlags + swiftLibPaths + listOf("-ObjC", "-lc++", "-lz")
}
