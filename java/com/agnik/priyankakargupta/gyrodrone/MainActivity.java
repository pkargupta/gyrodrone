package com.agnik.priyankakargupta.gyrodrone;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView speechtext;
    private ImageButton speechbtn;
    private ImageButton emgbtn;
    protected static final int RESULT_SPEECH = 1;
    private String currentcommand;
    private MiniDrone mMiniDrone;
    private ProgressDialog mConnectionProgressDialog;
    private ProgressDialog mDownloadProgressDialog;

    private TextView mBatteryLabel;
    //private Button mTakeOffLandBt;
    //private Button mDownloadBt;

    //private int mNbMaxDownload;
    //private int mCurrentDownloadIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        speechtext = (TextView) findViewById(R.id.spchtxt);
        speechbtn = (ImageButton) findViewById(R.id.spkbtn);
        emgbtn = (ImageButton) findViewById(R.id.emg);
        mBatteryLabel = (TextView) findViewById(R.id.battery);
        emgbtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                switch (mMiniDrone.getFlyingState()) {
                    case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mMiniDrone.land();
                        break;
                    default:
                }/*
                mMiniDrone.setYaw((byte) 0);
                mMiniDrone.setGaz((byte) 0);
                mMiniDrone.setPitch((byte) 0);
                mMiniDrone.setFlag((byte)0);*/
            }
        });
        speechbtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

                try {
                    startActivityForResult(intent, RESULT_SPEECH);
                    speechtext.setText("");
                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Oops! Your device doesn't support Speech to Text",
                            Toast.LENGTH_SHORT);
                    t.show();
                }
            }
        });
        Intent intent = getIntent();
        ARDiscoveryDeviceService service = intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
        mMiniDrone = new MiniDrone(this, service);
        mMiniDrone.addListener(mMiniDroneListener);
    }
    @Override
    protected void onStart() {
        super.onStart();

        // show a loading view while the minidrone is connecting
        if ((mMiniDrone != null) && !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mMiniDrone.getConnectionState())))
        {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AlertDialog_AppCompat);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Connecting ...");
            Toast.makeText(getApplicationContext(), "Connecting..", Toast.LENGTH_SHORT);
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            // if the connection to the MiniDrone fails, finish the activity
            if (!mMiniDrone.connect()) {
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mMiniDrone != null) {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AlertDialog_AppCompat);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Disconnecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            if (!mMiniDrone.disconnect()) {
                finish();
            }
        } else {
            finish();
        }
    }
    private final MiniDrone.Listener mMiniDroneListener = new MiniDrone.Listener() {
        @Override
        public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            switch (state) {
                case ARCONTROLLER_DEVICE_STATE_RUNNING:
                    mConnectionProgressDialog.dismiss();
                    break;

                case ARCONTROLLER_DEVICE_STATE_STOPPED:
                    // if the deviceController is stopped, go back to the previous activity
                    mConnectionProgressDialog.dismiss();
                    finish();
                    break;

                default:
                    break;
            }
        }

        public void onBatteryChargeChanged(int batteryPercentage) {
            mBatteryLabel.setText(String.format("%d%%", batteryPercentage));
        }

        @Override
        public void onPilotingStateChanged(ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
            switch (state) {
                case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    //mTakeOffLandBt.setEnabled(true);
                    //mDownloadBt.setEnabled(true);
                    break;
                case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    //mTakeOffLandBt.setEnabled(true);
                    //mDownloadBt.setEnabled(false);
                    break;
                default:
                    //mTakeOffLandBt.setEnabled(false);
                    //mDownloadBt.setEnabled(false);
            }
        }
    };
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    speechtext.setText(text.get(0));
                    currentcommand = text.get(0);
                    if(currentcommand.toLowerCase().trim().equals("turn right")) {
                        mMiniDrone.setYaw((byte) 25);
                    }
                    if(currentcommand.toLowerCase().trim().equals("turn left")){
                        mMiniDrone.setYaw((byte) -25);
                    }
                    if(currentcommand.toLowerCase().trim().equals("stop turning")){
                        mMiniDrone.setYaw((byte) 0);
                    }
                    if(currentcommand.toLowerCase().trim().equals("forward")){
                        mMiniDrone.setFlag((byte) 1);
                        mMiniDrone.setPitch((byte) 25);
                    }
                    if(currentcommand.toLowerCase().trim().equals("backward")){
                        mMiniDrone.setFlag((byte) 1);
                        mMiniDrone.setPitch((byte) -25);
                    }
                    if(currentcommand.toLowerCase().trim().equals("stop moving")){
                        mMiniDrone.setFlag((byte) 0);
                        mMiniDrone.setPitch((byte) 0);
                    }
                    if(currentcommand.toLowerCase().trim().equals("up")){
                        mMiniDrone.setGaz((byte) 25);
                    }
                    if(currentcommand.toLowerCase().trim().equals("down")){
                        mMiniDrone.setGaz((byte) -25);
                    }
                    if(currentcommand.toLowerCase().trim().equals("stay")){
                        mMiniDrone.setGaz((byte) 0);
                    }
                    if(currentcommand.toLowerCase().trim().equals("stop")){
                        mMiniDrone.setYaw((byte) 0);
                        mMiniDrone.setGaz((byte) 0);
                        mMiniDrone.setPitch((byte) 0);
                        mMiniDrone.setFlag((byte)0);
                    }
                    if(currentcommand.toLowerCase().trim().equals("emergency")){
                        mMiniDrone.emergency();
                    }
                    if(currentcommand.toLowerCase().trim().equals("take off") ||
                            currentcommand.toLowerCase().trim().equals("land")){
                        switch (mMiniDrone.getFlyingState()) {
                            case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                                mMiniDrone.takeOff();
                                break;
                            case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                            case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING: {
                                mMiniDrone.land();
                                /*mMiniDrone.setYaw((byte) 0);
                                mMiniDrone.setGaz((byte) 0);
                                mMiniDrone.setPitch((byte) 0);
                                mMiniDrone.setFlag((byte) 0);*/
                                break;
                            }
                            default:
                        }
                    }
                }
                break;
            }

        }
    }
}
