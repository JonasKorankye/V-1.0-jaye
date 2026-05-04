/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const nodemailer = require('nodemailer');
// const {defineSecret} = require('firebase-functions/v2/core');
const { defineSecret } = require('firebase-functions/params');



// Initialize Firebase Admin SDK
if (!admin.apps.length) {
  admin.initializeApp();
}

const flipEmail = defineSecret("FLIPVERSE_MAIL");     
const flipPassword = defineSecret("FLIPVERSE_PASSWORD");

// Configure email transporter (using Gmail as example)
const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: process.env.EMAIL, 
    pass: process.env.PASSWORD
  }
});

// Alternative configuration for other email providers
// const transporter = nodemailer.createTransporter({
//   host: 'smtp.your-provider.com',
//   port: 587,
//   secure: false,
//   auth: {
//     user: functions.config().email.user,
//     pass: functions.config().email.pass
//   }
// });

exports.FireOtpEmail = functions.https.onCall(
  async (data, context) => {
  try {
    // Validate input
    if (!data.email || !data.otp) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Email and OTP are required'
      );
    }

    // Optional: Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(data.email)) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Invalid email format'
      );
    }

    // Optional: Rate limiting - check if user has requested OTP recently
    const rateLimitRef = admin.firestore()
      .collection('mail')
      .doc(data.email);
    
    const rateLimitDoc = await rateLimitRef.get();
    if (rateLimitDoc.exists) {
      const lastRequest = rateLimitDoc.data().timestamp;
      const timeDiff = Date.now() - lastRequest;
      const cooldownPeriod = 60000; // 1 minute cooldown
      
      if (timeDiff < cooldownPeriod) {
        throw new functions.https.HttpsError(
          'resource-exhausted',
          'Please wait before requesting another OTP'
        );
      }
    }

    // Generate OTP if not provided (optional)
    const otp = data.otp || Math.floor(100000 + Math.random() * 900000).toString();

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

    // Email options
    const mailOptions = {
      from: {
        name: 'FlipVerse',
        address: process.env.EMAIL
      },
      to: data.email,
      subject: 'Email Verification - OTP',
      html: htmlTemplate,
      text: `Your OTP for email verification is: ${otp}. This code is valid for 6 minutes.`
    };

    // Send email
    await transporter.sendMail(mailOptions);

    // Store OTP in Firestore for verification (with expiration)
    const otpRef = admin.firestore().collection('email_otps').doc(data.email);
    await otpRef.set({
      otp: otp,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      expiresAt: new Date(Date.now() + 6 * 60 * 1000), // 10 minutes
      verified: false
    });

    // Update rate limiting document
    await rateLimitRef.set({
      timestamp: Date.now(),
      email: data.email
    });

    functions.logger.info(`OTP email sent successfully to ${data.email}`);

    return {
      success: true,
      message: 'OTP sent successfully',
      email: data.email
    };

  } catch (error) {
    functions.logger.error('Error sending OTP email:', error);
    
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    
    throw new functions.https.HttpsError(
      'internal',
      'Failed to send OTP email'
    );
  }
});

// Optional: Function to verify OTP
exports.VerifyOtpEmail = functions.https.onCall(async (data, context) => {
  try {
    if (!data.email || !data.otp) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Email and OTP are required'
      );
    }

    const otpRef = admin.firestore().collection('email_otps').doc(data.email);
    const otpDoc = await otpRef.get();

    if (!otpDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'No OTP found for this email'
      );
    }

    const otpData = otpDoc.data();
    
    // Check if OTP has expired
    if (new Date() > otpData.expiresAt.toDate()) {
      await otpRef.delete();
      throw new functions.https.HttpsError(
        'deadline-exceeded',
        'OTP has expired'
      );
    }

    // Check if OTP matches
    if (otpData.otp !== data.otp) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Invalid OTP'
      );
    }

    // Mark as verified and clean up
    await otpRef.update({ verified: true });
    
    return {
      success: true,
      message: 'OTP verified successfully',
      email: data.email
    };

  } catch (error) {
    functions.logger.error('Error verifying OTP:', error);
    
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    
    throw new functions.https.HttpsError(
      'internal',
      'Failed to verify OTP'
    );
  }
});