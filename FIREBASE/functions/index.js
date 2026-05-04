/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */
const functions = require('firebase-functions');
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require('firebase-admin');
const {onSchedule} = require('firebase-functions/v2/scheduler');

// Initialize Firebase Admin SDK
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const messaging = admin.messaging();

// Generate a random 6-digit OTP
function generateOTP() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

// Function to send OTP verification email
async function sendOTPVerificationEmail(email) {
  try {
    // Generate OTP and set expiration (e.g., 10 minutes from now)
    const otp = generateOTP();
    const expiresAt = new Date();
    expiresAt.setMinutes(expiresAt.getMinutes() + 10);

    // Email template
    const htmlTemplate = `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px;">
          <h2 style="color: #333; text-align: center;">Email Verification</h2>
          <p style="font-size: 16px; color: #555;">Hello,</p>
          <p style="font-size: 16px; color: #555;">
            Your One-Time Password (OTP) for email verification is:
          </p>
          <div style="text-align: center; margin: 30px 0;">
            <span style="font-size: 32px; font-weight: bold; color: #007bff; 
                         background-color: #e9ecef; padding: 15px 30px; 
                         border-radius: 8px; letter-spacing: 8px;">
              ${otp}
            </span>
          </div>
          <p style="font-size: 14px; color: #666;">
            This OTP is valid for 6 minutes. Please do not share this code with anyone.
          </p>
          <p style="font-size: 14px; color: #666;">
            If you didn't request this verification, please ignore this email.
          </p>
        </div>
      </div>
    `;

    // Store OTP and email in Firestore to trigger the email extension
    await db.collection('mail').add({
      to: [email], // Array of recipient emails
      message: {
        subject: 'Your OTP Verification Code',
        text: `Your OTP code is ${otp}. It will expire at ${expiresAt.toISOString()}.`,
        html: htmlTemplate,
      },
      createdAt: new Date(), // Optional: For tracking or TTL
      expiresAt: expiresAt, // Optional: For expiration handling
    });

    console.log(`OTP ${otp} sent to ${email} at ${new Date().toISOString()}`);
    return { success: true, message: 'OTP sent successfully', email: email, otp: otp  };
  } catch (error) {
    console.error('Error sending OTP:', error);
    throw new Error('Failed to send OTP verification email');
  }
}

// Example callable function to trigger OTP sending
exports.postOtpEmail = onCall(async (request) => {
    const { email, otp } = request.data; // Access data directly from request.data

  // if (!data.email) {
  //   throw new functions.https.HttpsError('invalid-argument', 'Email is required');
  // }

   // Validate input
    // if (!email || !otp) {
    //   throw new functions.https.HttpsError(
    //     'invalid-argument',
    //     'Email and OTP are required'
    //   );
    // }

    // Optional: Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Invalid email format'
      );
    }

  return await sendOTPVerificationEmail(email);
});

// Optional: Scheduled function to clean up expired OTPs
exports.cleanupExpiredOTPs = onSchedule({
  schedule: 'every 1 hours',
}, async (event) => {
  try {
    const snapshot = await db.collection('mail')
      .where('expiresAt', '<', new Date())
      .get();

    const batch = db.batch();
    snapshot.forEach(doc => batch.delete(doc.ref));
    await batch.commit();
    
    console.log(`Cleaned up ${snapshot.size} expired OTPs at ${new Date().toISOString()}`);
    return { success: true, deletedCount: snapshot.size };
  } catch (error) {
    console.error('Error during OTP cleanup:', error);
    throw new Error('Cleanup failed');
  }
});

// =================================================================
// PUSH NOTIFICATION FUNCTIONS
// =================================================================

/**
 * Cloud Function to send push notification when a new message is created
 * Triggered by Firestore document creation in the 'messages' collection
 */
//exports.sendMessageNotification = onDocumentCreated("messages/{messageId}", async (event) => {
//        try {
//            const messageData = event.data.data();
//            const messageId = event.params.messageId;
//
//            console.log('New message created:', messageId, messageData);
//
//            // Get recipient user data to fetch their FCM token
//            const recipientId = messageData.recipientId;
//
//            // Query user collection to get recipient's FCM token
//            const recipientQuery = await db.collection('user')
//                .where('email', '==', recipientId)
//                .limit(1)
//                .get();
//
//            if (recipientQuery.empty) {
//                console.log('Recipient not found:', recipientId);
//                return null;
//            }
//
//            const recipientDoc = recipientQuery.docs[0];
//            const recipientData = recipientDoc.data();
//            const fcmToken = recipientData.senderMessageToken;
//
//            if (!fcmToken) {
//                console.log('No FCM token found for recipient:', recipientId);
//                return null;
//            }
//
//            // Get sender information
//            const senderId = messageData.senderId;
//            const senderQuery = await db.collection('user')
//                .where('email', '==', senderId)
//                .limit(1)
//                .get();
//
//            let senderName = 'Someone';
//            if (!senderQuery.empty) {
//                const senderData = senderQuery.docs[0].data();
//                senderName = senderData.fullname || senderData.firstName || 'Someone';
//            }
//
//            // Get the actual message content
//            const content = messageData.content || messageData.text || 'New message';
//
//            // Create the notification payload
//            const payload = {
//                token: fcmToken,
//                notification: {
//                    title: senderName,
//                    body: content
//                },
//                data: {
//                    title: senderName,
//                    body: content,
//                    type: 'chat_message',
//                    senderId: senderId,
//                    conversationId: messageData.conversationId,
//                    messageId: messageId,
//                    customData: 'chat_notification'
//                },
//                android: {
//                    notification: {
//                        icon: 'ic_notification',
//                        color: '#FFFFFF',
//                        sound: 'default',
//                        clickAction: 'NOTIFICATION_CLICK'
//                    }
//                },
//                apns: {
//                    payload: {
//                        aps: {
//                            sound: 'default',
//                            badge: 1
//                        }
//                    }
//                }
//            };
//
//            // Send the notification
//            const response = await messaging.send(payload);
//            console.log('Successfully sent message notification:', response);
//
//            return response;
//
//        } catch (error) {
//            console.error('Error sending message notification:', error);
//            return null;
//        }
//    });

/**
 * HTTP Cloud Function to manually send push notifications
 * Can be called via REST API
 */
exports.sendPushNotification = onCall(async (request) => {
    try {
        // Verify authentication (optional, remove if not needed)
        // if (!request.auth) {
        //     throw new HttpsError('unauthenticated', 'User must be authenticated');
        // }

        const { recipientId, title, body, type, senderId, conversationId, customData, messageId } = request.data;

        if (!recipientId) {
            throw new HttpsError('invalid-argument', 'recipientId is required');
        }

        // Get recipient's FCM token
        const recipientQuery = await db.collection('user')
            .where('email', '==', recipientId)
            .limit(1)
            .get();

        if (recipientQuery.empty) {
            throw new HttpsError('not-found', 'Recipient not found');
        }

        const recipientDoc = recipientQuery.docs[0];
        const recipientData = recipientDoc.data();
        const fcmToken = recipientData.senderMessageToken;

        if (!fcmToken) {
            throw new HttpsError('not-found', 'No FCM token found for recipient');
        }

        // Create the notification payload
        const payload = {
            token: fcmToken,
            notification: {
                title: title || 'FlipVerse Notification',
                body: body || 'You have a new notification'
            },
            data: {
                title: title || 'FlipVerse Notification',
                body: body || 'You have a new notification',
                type: type || 'general',
                senderId: senderId || '',
                recipientId: recipientId || '',
                conversationId: conversationId || '',
                messageId: messageId || '',
                customData: customData || 'manual_notification'
            },
            android: {
                notification: {
                    icon: 'ic_notification',
                    color: '#FFFFFF',
                    sound: 'default',
                    clickAction: 'NOTIFICATION_CLICK'
                }
            },
            apns: {
                payload: {
                    aps: {
                        sound: 'default',
                        badge: 1
                    }
                }
            }
        };

        // Send the notification
        const response = await messaging.send(payload);
        console.log('Successfully sent push notification:', response);

        return {
            success: true,
            messageId: response,
            payload: payload.data
        };

    } catch (error) {
        console.error('Error sending push notification:', error);
        throw new HttpsError('internal', 'Failed to send notification');
    }
});

/**
 * HTTP Cloud Function to send notification to multiple users
 */
exports.sendMulticastNotification = onCall(async (request) => {
    try {
        // Verify authentication (optional)
        // if (!request.auth) {
        //     throw new HttpsError('unauthenticated', 'User must be authenticated');
        // }

        const { recipientIds, title, body, type, senderId,conversationId, customData } = request.data;
        console.log('recipientIds:: ', recipientIds);

        // Get FCM tokens for all recipients
        const tokens = [];
        for (const recipientId of recipientIds) {
            const recipientQuery = await db.collection('user')
                .where('email', '==', recipientId)
                .limit(1)
                .get();

            if (!recipientQuery.empty) {
                const recipientData = recipientQuery.docs[0].data();
                const fcmToken = recipientData.senderMessageToken;
                if (fcmToken) {
                    tokens.push(fcmToken);
                }
            }
        }

        if (tokens.length === 0) {
            console.log('No valid FCM tokens found for the recipients');
            throw new HttpsError('not-found', 'No valid FCM tokens found');
        }
        console.log('tokens:: ', tokens);

        // Send individual notifications
        const responses = [];
        let successCount = 0;
        let failureCount = 0;
        for (const token of tokens) {
            const message = {
                token: token,
                notification: {
                    title: title || 'FlipVerse Notification',
                    body: body || 'You have a new notification'
                },
                data: {
                    title: title || 'FlipVerse Notification',
                    body: body || 'You have a new notification',
                    conversationId: conversationId || '',
                    type: type || 'general',
                    senderId: senderId || '',
                    customData: customData || 'multicast_notification'
                },
                android: {
                    notification: {
                        icon: 'ic_notification',
                        color: '#FFFFFF',
                        sound: 'default'
                    }
                },
                apns: {
                    payload: {
                        aps: {
                            sound: 'default',
                            badge: 1
                        }
                    }
                }
            };
            try {
                const response = await messaging.send(message);
                responses.push({ token: token, success: true, response });
                successCount++;
                console.log(`Successfully sent notification to token:`, token, response);
            } catch (error) {
                responses.push({ token: token, success: false, error: error.message });
                failureCount++;
                console.error(`Error sending notification to token:`, token, error);
            }
        }

        return {
            success: true,
            successCount: successCount,
            failureCount: failureCount,
            responses: responses
        };

    } catch (error) {
        console.error('Error sending notifications:', error);
        throw new HttpsError('internal', 'Failed to send notifications');
    }
});

/**
 * Utility function to clean up expired FCM tokens
 */
exports.cleanupExpiredTokens = onSchedule({
    schedule: 'every 24 hours'
}, async (context) => {
    try {
        console.log('Starting FCM token cleanup...');
        
        // This would require additional logic to track and remove invalid tokens
        // For now, just log the cleanup attempt
        console.log('FCM token cleanup completed');
        
        return null;
    } catch (error) {
        console.error('Error during FCM token cleanup:', error);
        return null;
    }
});

// =================================================================
// LIVEBOOK TURN-BASED NOTIFICATION FUNCTIONS
// =================================================================

/**
 * Scheduled function to check for turn changes and send notifications
 * Runs every hour to check if it's someone's turn to contribute
 */
exports.checkLiveBookTurns = onSchedule({
    schedule: 'every 1 hours',
}, async (event) => {
    try {
        console.log('Starting LiveBook turn check...');

        // Get all active LiveBooks
        const liveBooksSnapshot = await db.collection('livebooks')
            .where('status', '==', 'active')
            .get();

        if (liveBooksSnapshot.empty) {
            console.log('No active LiveBooks found');
            return { success: true, notificationsSent: 0 };
        }

        let notificationsSent = 0;

        for (const liveBookDoc of liveBooksSnapshot.docs) {
            const liveBook = liveBookDoc.data();
            const liveBookId = liveBookDoc.id;

            // Skip if no turn order is set
            if (!liveBook.contributorTurnOrder || liveBook.contributorTurnOrder.length === 0) {
                continue;
            }

            // Get current turn holder
            const currentTurnIndex = liveBook.currentTurnIndex || 0;
            const currentTurnHolderId = liveBook.contributorTurnOrder[currentTurnIndex];

            if (!currentTurnHolderId) {
                continue;
            }

            // Check if current turn holder has already contributed
            const contributedIds = [
                liveBook.paragraph1ContributorId,
                liveBook.paragraph2ContributorId,
                liveBook.paragraph3ContributorId,
                liveBook.paragraph4ContributorId,
                liveBook.paragraph5ContributorId,
                liveBook.paragraph6ContributorId,
            ].filter(id => id && id.trim() !== '');

            if (contributedIds.includes(currentTurnHolderId)) {
                continue; // Already contributed
            }

            // Find the current turn holder in tagged users
            const currentTurnHolder = liveBook.taggedUsers.find(user => user.id === currentTurnHolderId);

            if (!currentTurnHolder || !currentTurnHolder.email) {
                continue;
            }

            // Check if we've already sent a notification for this turn
            // We'll use a subcollection to track sent notifications
            const notificationCheckRef = await db.collection('livebooks')
                .doc(liveBookId)
                .collection('turn_notifications')
                .doc(`turn_${currentTurnIndex}`)
                .get();

            if (notificationCheckRef.exists) {
                // Already sent notification for this turn
                continue;
            }

            // Get user's FCM token
            const userQuery = await db.collection('user')
                .where('email', '==', currentTurnHolder.email)
                .limit(1)
                .get();

            if (userQuery.empty) {
                continue;
            }

            const userData = userQuery.docs[0].data();
            const fcmToken = userData.senderMessageToken;

            if (!fcmToken) {
                console.log(`No FCM token for user: ${currentTurnHolder.email}`);
                continue;
            }

            // Send notification
            const payload = {
                token: fcmToken,
                notification: {
                    title: "It's Your Turn! ✍️",
                    body: `Continue the story: "${liveBook.title}"`
                },
                data: {
                    title: "It's Your Turn! ✍️",
                    body: `Continue the story: "${liveBook.title}"`,
                    type: 'livebook_turn',
                    liveBookId: liveBookId,
                    liveBookTitle: liveBook.title || '',
                    customData: 'livebook_turn_notification'
                },
                android: {
                    notification: {
                        icon: 'ic_notification',
                        color: '#FFFFFF',
                        sound: 'default',
                        clickAction: 'NOTIFICATION_CLICK'
                    }
                },
                apns: {
                    payload: {
                        aps: {
                            sound: 'default',
                            badge: 1
                        }
                    }
                }
            };

            try {
                const response = await messaging.send(payload);
                console.log(`Turn notification sent to ${currentTurnHolder.email}:`, response);

                // Mark notification as sent
                await db.collection('livebooks')
                    .doc(liveBookId)
                    .collection('turn_notifications')
                    .doc(`turn_${currentTurnIndex}`)
                    .set({
                        sentAt: new Date(),
                        recipientId: currentTurnHolderId,
                        recipientEmail: currentTurnHolder.email,
                        turnIndex: currentTurnIndex
                    });

                notificationsSent++;
            } catch (error) {
                console.error(`Failed to send turn notification to ${currentTurnHolder.email}:`, error);
            }
        }

        console.log(`LiveBook turn check completed. Notifications sent: ${notificationsSent}`);
        return { success: true, notificationsSent };

    } catch (error) {
        console.error('Error checking LiveBook turns:', error);
        throw new Error('Turn check failed');
    }
});

/**
 * Scheduled function to send 30-minute reminder notifications
 * Runs every 30 minutes to check if users have 30 minutes or less remaining
 */
exports.sendLiveBookTurnReminders = onSchedule({
    schedule: 'every 30 minutes',
}, async (event) => {
    try {
        console.log('Starting LiveBook turn reminder check...');

        // Get all active LiveBooks
        const liveBooksSnapshot = await db.collection('livebooks')
            .where('status', '==', 'active')
            .get();

        if (liveBooksSnapshot.empty) {
            console.log('No active LiveBooks found');
            return { success: true, remindersSent: 0 };
        }

        let remindersSent = 0;
        const now = new Date();

        for (const liveBookDoc of liveBooksSnapshot.docs) {
            const liveBook = liveBookDoc.data();
            const liveBookId = liveBookDoc.id;

            // Skip if no turn order is set
            if (!liveBook.contributorTurnOrder || liveBook.contributorTurnOrder.length === 0) {
                continue;
            }

            // Get current turn holder
            const currentTurnIndex = liveBook.currentTurnIndex || 0;
            const currentTurnHolderId = liveBook.contributorTurnOrder[currentTurnIndex];

            if (!currentTurnHolderId) {
                continue;
            }

            // Check if current turn holder has already contributed
            const contributedIds = [
                liveBook.paragraph1ContributorId,
                liveBook.paragraph2ContributorId,
                liveBook.paragraph3ContributorId,
                liveBook.paragraph4ContributorId,
                liveBook.paragraph5ContributorId,
                liveBook.paragraph6ContributorId,
            ].filter(id => id && id.trim() !== '');

            if (contributedIds.includes(currentTurnHolderId)) {
                continue; // Already contributed
            }

            // Parse turn start time
            let turnStartTime;
            try {
                // Try parsing ISO format
                turnStartTime = new Date(liveBook.currentTurnStartTime);
            } catch (e) {
                console.log(`Invalid turn start time for LiveBook ${liveBookId}`);
                continue;
            }

            // Calculate time remaining
            const turnDurationHours = liveBook.turnDurationHours || 3;
            const turnDurationMs = turnDurationHours * 60 * 60 * 1000;
            const elapsedMs = now.getTime() - turnStartTime.getTime();
            const remainingMs = turnDurationMs - elapsedMs;
            const remainingMinutes = remainingMs / (60 * 1000);

            // Send reminder if between 20 and 40 minutes remaining (to catch the 30-minute mark)
            if (remainingMinutes < 20 || remainingMinutes > 40) {
                continue;
            }

            // Check if we've already sent a reminder for this turn
            const reminderCheckRef = await db.collection('livebooks')
                .doc(liveBookId)
                .collection('turn_reminders')
                .doc(`turn_${currentTurnIndex}_30min`)
                .get();

            if (reminderCheckRef.exists) {
                continue; // Already sent reminder
            }

            // Find the current turn holder in tagged users
            const currentTurnHolder = liveBook.taggedUsers.find(user => user.id === currentTurnHolderId);

            if (!currentTurnHolder || !currentTurnHolder.email) {
                continue;
            }

            // Get user's FCM token
            const userQuery = await db.collection('user')
                .where('email', '==', currentTurnHolder.email)
                .limit(1)
                .get();

            if (userQuery.empty) {
                continue;
            }

            const userData = userQuery.docs[0].data();
            const fcmToken = userData.senderMessageToken;

            if (!fcmToken) {
                console.log(`No FCM token for user: ${currentTurnHolder.email}`);
                continue;
            }

            // Send reminder notification
            const payload = {
                token: fcmToken,
                notification: {
                    title: "⏰ 30 Minutes Left!",
                    body: `Your turn to write for "${liveBook.title}" expires soon!`
                },
                data: {
                    title: "⏰ 30 Minutes Left!",
                    body: `Your turn to write for "${liveBook.title}" expires soon!`,
                    type: 'livebook_turn_reminder',
                    liveBookId: liveBookId,
                    liveBookTitle: liveBook.title || '',
                    customData: 'livebook_turn_reminder_notification'
                },
                android: {
                    notification: {
                        icon: 'ic_notification',
                        color: '#FF9800',
                        sound: 'default',
                        clickAction: 'NOTIFICATION_CLICK'
                    }
                },
                apns: {
                    payload: {
                        aps: {
                            sound: 'default',
                            badge: 1
                        }
                    }
                }
            };

            try {
                const response = await messaging.send(payload);
                console.log(`Turn reminder sent to ${currentTurnHolder.email}:`, response);

                // Mark reminder as sent
                await db.collection('livebooks')
                    .doc(liveBookId)
                    .collection('turn_reminders')
                    .doc(`turn_${currentTurnIndex}_30min`)
                    .set({
                        sentAt: new Date(),
                        recipientId: currentTurnHolderId,
                        recipientEmail: currentTurnHolder.email,
                        turnIndex: currentTurnIndex,
                        remainingMinutes: Math.round(remainingMinutes)
                    });

                remindersSent++;
            } catch (error) {
                console.error(`Failed to send reminder to ${currentTurnHolder.email}:`, error);
            }
        }

        console.log(`LiveBook turn reminder check completed. Reminders sent: ${remindersSent}`);
        return { success: true, remindersSent };

    } catch (error) {
        console.error('Error sending LiveBook turn reminders:', error);
        throw new Error('Turn reminder check failed');
    }
});
