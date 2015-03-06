package com.cuberob.wearspeedtest;

import android.util.Log;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by cuberob on 6-3-2015.
 */
public class DataLayer extends WearableListenerService {
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d("onMessageReceived", "Received message: " + messageEvent.toString());
    }
}
