package com.cuberob.wearspeedtest;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * Created by cuberob on 6-3-2015.
 */
public class DataLayer extends WearableListenerService{

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        long time = System.currentTimeMillis();
        Log.d("onMessageReceived", "Received DataEvent");
        replyTime(time, "");
    }

    private String getNode(GoogleApiClient mGoogleApiClient) {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        return nodes.getNodes().get(0).getId(); //Assumes single node
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        long time = System.currentTimeMillis();

        Log.d("onMessageReceived", "Received message: " + messageEvent.toString());
        replyTime(time, messageEvent.getSourceNodeId());
    }

    private void replyTime(long time, String nodeId) {
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.blockingConnect(5, TimeUnit.SECONDS);

        if(!mGoogleApiClient.isConnected()){
            Log.e("GoogleApiClient", "Failed to connect!");
            mGoogleApiClient.disconnect(); //Dunno if necessary
            return;
        }

        if(nodeId.isEmpty()){
            nodeId = getNode(mGoogleApiClient);
        }

        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, nodeId, "/data", Long.toString(time).getBytes()).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e("sendMessage", "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );

    }
}
