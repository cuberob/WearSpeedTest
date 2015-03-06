package com.cuberob.wearspeedtest;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;


public class MainActivity extends ActionBarActivity implements MessageApi.MessageListener {

    private byte[] payload = new byte[1024];
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle connectionHint) {
                Log.d("GoogleApiClient", "onConnected: " + connectionHint);
            }

            @Override
            public void onConnectionSuspended(int cause) {
                Log.d("GoogleApiClient", "onConnectionSuspended: " + cause);
            }
        })
        .addApi(Wearable.API)
        .build();
    }


    public void start(View v){
        Log.d("onClick", "Start button");
        Collection<String> nodes = getNodes();
        for(String node : nodes){
            Log.d("Node", "Sending payload to: " + node);
            sendPayload(node, payload);
        }
    }

    private void sendPayload(String nodeId, byte[] payload) {
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, nodeId, "/data", payload).setResultCallback(
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

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }else{
            Log.e("GoogleApiClient", "mGoogleApiClient null in onResume()!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mGoogleApiClient != null){
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("MessageAPI", "Received message: " + messageEvent.toString());
    }
}
