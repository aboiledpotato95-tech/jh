package com.quickans;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

// Keeps the app alive while answers are being fetched, so it keeps working with the screen off or another app open.
public class KeepAliveService extends Service {
  PowerManager.WakeLock wl;
  boolean started = false;

  @Override public IBinder onBind(Intent i) { return null; }

  void goForeground() {
    if (started) return;
    started = true;
    try {
      NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      if (Build.VERSION.SDK_INT >= 26) {
        nm.createNotificationChannel(new NotificationChannel("work", "Getting answers", NotificationManager.IMPORTANCE_LOW));
      }
      Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "work") : new Notification.Builder(this);
      b.setContentTitle("A.I - Ans by Mystery");
      b.setContentText("Getting your answers...");
      b.setSmallIcon(android.R.drawable.stat_notify_sync);
      b.setOngoing(true);
      b.setContentIntent(MainActivity.openIntent(this, 0));
      Notification n = b.build();
      if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
      else startForeground(1, n);
    } catch (Throwable e) { }
    try {
      PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
      wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "quickans:work");
      wl.setReferenceCounted(false);
      wl.acquire(30L * 60L * 1000L);   // never longer than 30 minutes
    } catch (Throwable e) { }
  }

  @Override public void onCreate() { super.onCreate(); goForeground(); }
  @Override public int onStartCommand(Intent i, int flags, int id) { goForeground(); return START_NOT_STICKY; }
  @Override public void onDestroy() {
    try { if (wl != null && wl.isHeld()) wl.release(); } catch (Throwable e) { }
    super.onDestroy();
  }
}
