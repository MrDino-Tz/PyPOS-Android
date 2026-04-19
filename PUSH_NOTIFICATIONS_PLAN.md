# Push Notifications Implementation Plan

## Current Version
**PyPOS Android App v1.0** (versionCode: 1)

---

## Overview
This document outlines the implementation plan for adding push notifications to the PyPOS Android app. Push notifications will enable real-time alerts for:
- New sales made
- Low stock alerts
- Daily sales summaries
- User activity notifications

---

## Architecture

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────┐
│   Android App   │────▶│   Supabase   │────▶│    Firebase │
│  (FCM Token)    │     │   Database   │     │    Cloud    │
│                 │     │  (Edge Func) │     │  Messaging  │
└─────────────────┘     └──────────────┘     └─────────────┘
```

---

## Implementation Steps

### Phase 1: Firebase Setup (Required)

1. **Create Firebase Project**
   - Go to https://console.firebase.google.com
   - Create new project (e.g., "pypos-app")
   - Enable Firebase for Android

2. **Add Android App**
   - Package name: `com.dtcteam.pypos`
   - Download `google-services.json`
   - Place in: `app/google-services.json`

3. **Get Firebase Configuration**
   - Project ID
   - Private Key (for Edge Functions)
   - Client Email

---

### Phase 2: Android App Changes

#### 2.1 Update build.gradle

```gradle
// app/build.gradle - Add dependencies
plugins {
    id 'com.google.gms.google-services'
}

dependencies {
    // Firebase BOM
    implementation platform('com.google.firebase:firebase-bom:33.0.0')
    implementation 'com.google.firebase:firebase-messaging-ktx'
    
    // Also add to dependencies (not BOM)
    implementation 'com.google.firebase:firebase-analytics-ktx'
}
```

#### 2.2 Create google-services.json
Add your Firebase config file to `app/google-services.json`

#### 2.3 Create FCM Service

```java
// MyFirebaseMessagingService.java
package com.dtcteam.pypos.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.dtcteam.pypos.MainActivity;
import com.dtcteam.pypos.R;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Send token to Supabase
        sendTokenToSupabase(token);
    }
    
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();
        
        showNotification(title, body);
    }
    
    private void sendTokenToSupabase(String token) {
        // TODO: Implement API call to save token
    }
    
    private void showNotification(String title, String body) {
        // Create notification channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "pypos_notifications",
                "PyPOS Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "pypos_notifications")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true);
        
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }
}
```

#### 2.4 Update AndroidManifest.xml

```xml
<service
    android:name=".service.MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>

<meta-data
    android:name="com.google.firebase.messaging.default_notification_icon"
    android:resource="@drawable/ic_notification" />
```

---

### Phase 3: Supabase Backend

#### 3.1 Database Tables

```sql
-- Store user FCM tokens
CREATE TABLE public.fcm_tokens (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id),
    token TEXT NOT NULL UNIQUE,
    device_name TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Notifications log
CREATE TABLE public.notification_log (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    type TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### 3.2 Edge Function (send-notification)

```typescript
// supabase/functions/send-notification/index.ts
import { createClient } from '@supabase/supabase-js'
import { GoogleAuth } from 'google-auth-library'

const supabase = createClient(
  Deno.env.get('SUPABASE_URL')!,
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
)

Deno.serve(async (req) => {
  const { user_id, title, body, type } = await req.json()
  
  // Get user's FCM token
  const { data: tokenData } = await supabase
    .from('fcm_tokens')
    .select('token')
    .eq('user_id', user_id)
    .single()
  
  if (!tokenData?.token) {
    return new Response(JSON.stringify({ error: 'No token found' }), { status: 404 })
  }
  
  // Get Firebase access token
  const auth = new GoogleAuth({
    scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
  })
  
  const client = await auth.getClient()
  const accessToken = await client.fetchAccessTokenForClient(
    `https://oauth2.googleapis.com/token`
  )
  
  // Send to FCM
  const fcmResponse = await fetch(
    `https://fcm.googleapis.com/v1/projects/${Deno.env.get('FIREBASE_PROJECT_ID')}/messages:send`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken.token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        message: {
          token: tokenData.token,
          notification: { title, body },
          data: { type: type || 'general' }
        }
      })
    }
  )
  
  // Log notification
  await supabase.from('notification_log').insert({
    user_id,
    title,
    body,
    type
  })
  
  return new Response(JSON.stringify({ success: true }))
})
```

#### 3.3 Database Trigger (Optional)

Automatically send notifications when new sale occurs:

```sql
CREATE OR REPLACE FUNCTION notify_new_sale()
RETURNS TRIGGER AS $$
BEGIN
  -- Call Edge Function via HTTP (or use pg_net extension)
  PERFORM net.http_post(
    'https://your-project.supabase.co/functions/v1/send-notification',
    jsonb_build_object(
      'user_id', NEW.user_id,
      'title', 'New Sale!',
      'body', 'A new sale was completed: TSH ' || NEW.final_amount,
      'type', 'new_sale'
    )::text,
    'application/json'
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER on_new_sale
  AFTER INSERT ON sales
  FOR EACH ROW
  EXECUTE FUNCTION notify_new_sale();
```

---

### Phase 4: API Integration

Update `ApiService.java` to handle FCM tokens:

```java
public void saveFcmToken(String token, Callback<Void> callback) {
    // POST to Supabase to save token
}

public void registerDevice(String userId, String deviceName, Callback<Void> callback) {
    // Register device with FCM token
}
```

---

## Notification Types

| Type | Title | Body | Trigger |
|------|-------|------|---------|
| new_sale | New Sale! | A new sale was made: TSH X | On sale create |
| low_stock | Low Stock Alert | X items are running low | Daily check |
| daily_summary | Daily Summary | Today: X sales, TSH Y | Daily cron |
| new_user | New User | New user registered | On user create |

---

## Testing Checklist

- [ ] Firebase project created
- [ ] google-services.json added
- [ ] FCM dependency added
- [ ] MyFirebaseMessagingService created
- [ ] Manifest updated
- [ ] Supabase Edge Function deployed
- [ ] Database tables created
- [ ] Token saving works
- [ ] Test notification sends successfully

---

## Notes

- Push notifications require Google Play Services on device
- Test on real device (emulator may not receive FCM)
- Edge Functions require Pro plan for some features (check Supabase pricing)
- Consider notification preferences per user

---

## References

- [Supabase Push Notifications Guide](https://supabase.com/docs/guides/functions/examples/push-notifications)
- [Firebase Cloud Messaging Docs](https://firebase.google.com/docs/cloud-messaging)
- [Google Auth Library for FCM](https://github.com/googleapis/google-auth-library-nodejs)
