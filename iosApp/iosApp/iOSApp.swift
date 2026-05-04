import SwiftUI
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    
    var body: some Scene {
        WindowGroup {
            ContentView()
            .ignoresSafeArea()
        }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {
    
    // Shared FCM token accessible from KMP via IOSPushTokenProvider
    static var fcmToken: String? = nil
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        // 1. Configure Firebase
        FirebaseApp.configure()
        
        // 2. Set Firebase Messaging delegate
        Messaging.messaging().delegate = self
        
        // 3. Set notification center delegate for foreground notifications
        UNUserNotificationCenter.current().delegate = self
        
        // 4. Request notification permission
        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .badge, .sound]
        ) { granted, error in
            print("🔔 Push notification permission granted: \(granted)")
            if let error = error {
                print("❌ Push notification permission error: \(error)")
            }
        }
        
        // 5. Register for remote notifications (triggers APNs token request)
        application.registerForRemoteNotifications()
        
        // 6. Clear badge count on app launch
        // applicationIconBadgeNumber is deprecated in iOS 16+ — use setBadgeCount instead
        if #available(iOS 16.0, *) {
            UNUserNotificationCenter.current().setBadgeCount(0, withCompletionHandler: nil)
        } else {
            application.applicationIconBadgeNumber = 0
        }
        
        return true
    }
    
    // MARK: - APNs Token
    
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let tokenString = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("🔔 APNs device token: \(tokenString)")
        // Pass APNs token to Firebase Messaging (maps to FCM token)
        Messaging.messaging().apnsToken = deviceToken
    }
    
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("❌ Failed to register for remote notifications: \(error)")
    }
    
    // MARK: - Firebase MessagingDelegate
    
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("🔔 FCM token received: \(fcmToken ?? "nil")")
        AppDelegate.fcmToken = fcmToken
        
        // Notify KMP layer about the new token
        if let token = fcmToken {
            IOSPushTokenProvider.shared.onTokenReceived(token: token)
        }
    }
    
    // MARK: - UNUserNotificationCenterDelegate
    
    // Show notifications even when app is in foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        print("🔔 Foreground notification received: \(userInfo)")
        completionHandler([.banner, .badge, .sound])
    }
    
    // Handle notification tap
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        print("🔔 Notification tapped: \(userInfo)")
        
        // Clear the badge count when notification is tapped
        if #available(iOS 16.0, *) {
            UNUserNotificationCenter.current().setBadgeCount(0, withCompletionHandler: nil)
        } else {
            UIApplication.shared.applicationIconBadgeNumber = 0
        }
        
        // Convert userInfo to [String: Any?] for KMP bridge
        var payload: [String: Any] = [:]
        for (key, value) in userInfo {
            if let stringKey = key as? String {
                payload[stringKey] = value
            }
        }
        
        // Forward to KMP notification navigation bridge
        NotificationNavigationBridge.shared.handleNotificationClick(data: payload)
        
        completionHandler()
    }
}
