package com.gathin.tvhome;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
/** Starts the ordinary app when Android reports a completed boot. */
public class TclBootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context, Intent intent) {
  if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
  try { context.startActivity(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
  catch (RuntimeException ignored) { }
 }
}
