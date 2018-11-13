package org.cgnet.swara2.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.cgnet.swara2.MainApplication;
import org.cgnet.swara2.R;

import static org.cgnet.swara2.Constants.ACTION_FILE_SELECTED;

public class ChooserReceiver extends BroadcastReceiver {

    private final String TAG = "ChooseReceiver";

    Tracker t;

    private String mAudioFile;

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (action==null) {

            ComponentName component = intent.getParcelableExtra("android.intent.extra.CHOSEN_COMPONENT");
            String name = component.getPackageName();
            Log.e(TAG, name);
            if (name.equals(context.getString(R.string.whatsapp_package))) {
                t =  ((MainApplication) context.getApplicationContext()).getTracker(MainApplication.TrackerName.APP_TRACKER);
                t.send(new HitBuilders.EventBuilder()
                        .setCategory("WhatsApp chosen for audio file share.")
                        .setLabel(mAudioFile)
                        .build());
            }

        } else if (action.equals(ACTION_FILE_SELECTED)) {

            mAudioFile = intent.getStringExtra("FILE_NAME");
            Log.e(TAG, mAudioFile);

        }

    }
}
