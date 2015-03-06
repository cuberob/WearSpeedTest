package com.cuberob.wearspeedtest;

import android.app.ProgressDialog;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.w3c.dom.Text;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;


public class MainActivity extends ActionBarActivity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {

    private GoogleApiClient mGoogleApiClient;

    //Views
    private Spinner mSpinner;
    private ProgressDialog dialog;
    private SeekBar mSeekbar; //TODO: Use to set runs
    private TextView mLogTextView;

    //Transmission Data
    private long transmitTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Connect to Google Api Client for message api
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
        .addApi(Wearable.API)
        .build();

        setupViews();
    }

    private void setupViews(){
        mSeekbar = (SeekBar) findViewById(R.id.seekBar);
        mLogTextView = (TextView) findViewById(R.id.log_textview);
        mSpinner = (Spinner) findViewById(R.id.spinner);
    }

    private int getTestSize(){
        return getResources().getIntArray(R.array.test_sizes)[mSpinner.getSelectedItemPosition()];
    }

    /**
     * Called from xml layout
     * @param v
     */
    public void start(final View v){
        showLoadingDialog();

        final byte[] payload = new byte[getTestSize()];
        Log.d("onClick", "Payload size: " + payload.length);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Collection<String> nodes = getNodes();
                for(String node : nodes){
                    Log.d("Node", "Sending payload to: " + node);
                    if(v.getId() == R.id.button) {
                        sendPayloadAsRequest(payload);
                    }else{
                        sendPayloadAsMessage(node, payload);
                    }
                }
            }
        }).start();
    }

    private void showLoadingDialog() {
        dialog = ProgressDialog.show(MainActivity.this, "",
                "Loading. Please wait...", true);
        dialog.setCancelable(true);
    }

    private void sendPayloadAsRequest(byte[] payload) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/data");
        putDataMapReq.getDataMap().putByteArray("payload", payload);
        putDataMapReq.getDataMap().putLong("time", System.currentTimeMillis()); //If the dataMap hasn't changed it won't get synced
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();

        transmitTime = System.currentTimeMillis();

        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                if(!dataItemResult.getStatus().isSuccess()){
                    Log.e("putDataItem", "Failed to send message with status code: "
                            + dataItemResult.getStatus().getStatusCode());
                    dialog.dismiss();
                    if(dataItemResult.getStatus().getStatusCode() == 4003){
                        notifyUser("Payload was to large...");
                    }else{
                        notifyUser("Failed to putDataRequest");
                    }
                }
            }
        });
    }

    private void notifyUser(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    private void sendPayloadAsMessage(String nodeId, byte[] payload) {
        transmitTime = System.currentTimeMillis();

        PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                mGoogleApiClient, nodeId, "/data", payload);

        result.setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e("sendMessage", "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                            notifyUser("Failed to send message");
                            dialog.dismiss();
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
    protected void onResume() {
        super.onResume();
        if(mGoogleApiClient != null) {
            mGoogleApiClient.connect();
            Wearable.MessageApi.addListener(mGoogleApiClient, this);

        }else{
            Log.e("GoogleApiClient", "mGoogleApiClient null in onResume()!");
            notifyUser("Could not connect to GoogleApi & Watch");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mGoogleApiClient != null){
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        dialog.dismiss();

        String time = new String(messageEvent.getData());
        Long totalTime = Long.parseLong(time) - transmitTime;

        final String log = "Time: " + totalTime + "ms\n" + mLogTextView.getText().toString();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLogTextView.setText(log);
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("GoogleApiClient", "connected!");

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("GoogleApiClient", "suspended!");
    }
}
