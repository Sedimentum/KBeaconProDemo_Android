package com.kbeacon.ibeacondemo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kkmcn.kbeaconlib2.KBAdvPackage.KBAdvType;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBAdvMode;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBAdvTxPower;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvBase;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEBeacon;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEddyTLM;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEddyUID;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvEddyURL;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgAdvIBeacon;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgBase;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgCommon;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBCfgTrigger;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTriggerAction;
import com.kkmcn.kbeaconlib2.KBCfgPackage.KBTriggerType;
import com.kkmcn.kbeaconlib2.KBConnPara;
import com.kkmcn.kbeaconlib2.KBConnState;
import com.kkmcn.kbeaconlib2.KBConnectionEvent;
import com.kkmcn.kbeaconlib2.KBErrorCode;
import com.kkmcn.kbeaconlib2.KBException;
import com.kkmcn.kbeaconlib2.KBUtility;
import com.kkmcn.kbeaconlib2.KBeacon;
import com.kkmcn.kbeaconlib2.KBeaconsMgr;
import com.kbeacon.ibeacondemo.R;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

public class DevicePannelActivity extends AppBaseActivity implements View.OnClickListener, KBeacon.ConnStateDelegate{

    public final static String DEVICE_MAC_ADDRESS = "DEVICE_MAC_ADDRESS";
    private final static String LOG_TAG = "DevicePannel";
    public final static String DEFAULT_PASSWORD = "0000000000000000";   //16 zero ascii

    private final static boolean READ_DEFAULT_PARAMETERS = true;

    private final static int PERMISSION_CONNECT = 20;

    private KBeaconsMgr mBeaconMgr;
    private String mDeviceAddress;
    private KBeacon mBeacon;

    private static int minor = 123;

    //uiview
    private TextView mBeaconType, mBeaconStatus;
    private TextView mBeaconModel;
    private EditText mEditBeaconUUID;
    private EditText mEditBeaconMajor;
    private EditText mEditBeaconMinor;
    private EditText mEditSelectMinor;
    private EditText mEditBeaconAdvPeriod;
    private EditText mEditBeaconPassword;
    private EditText mEditBeaconTxPower;
    private EditText mEditBeaconName;
    private Button mDownloadButton, mResetButton;
    private String mNewPassword;
    SharePreferenceMgr mPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(DEVICE_MAC_ADDRESS);
        mBeaconMgr = KBeaconsMgr.sharedBeaconManager(this);
        mBeacon = mBeaconMgr.getBeacon(mDeviceAddress);
        if (mBeacon == null){
            toastShow("device is not exist");
            finish();
        }

        mPref = SharePreferenceMgr.shareInstance(this);
        setContentView(R.layout.device_pannel);
        mBeaconStatus = (TextView)findViewById(R.id.connection_states);
        mBeaconType = (TextView) findViewById(R.id.beaconType);
        mBeaconModel = (TextView) findViewById(R.id.beaconModle);
        mEditBeaconUUID = (EditText)findViewById(R.id.editIBeaconUUID);
        mEditBeaconMajor = (EditText)findViewById(R.id.editIBeaconMajor);
        mEditBeaconMinor = (EditText)findViewById(R.id.editIBeaconMinor);
        mEditSelectMinor = (EditText)findViewById(R.id.editSelectMinor);
        mEditBeaconAdvPeriod = (EditText)findViewById(R.id.editBeaconAdvPeriod);
        mEditBeaconTxPower = (EditText)findViewById(R.id.editBeaconTxPower);
        mEditBeaconName = (EditText)findViewById(R.id.editBeaconname);

        mDownloadButton = (Button) findViewById(R.id.buttonSaveData);
        mDownloadButton.setEnabled(false);
        mDownloadButton.setOnClickListener(this);

        mResetButton = (Button) findViewById(R.id.resetConfigruation);
        mResetButton.setEnabled(false);
        mResetButton.setOnClickListener(this);

         mEditSelectMinor.setText(String.valueOf(minor));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_connect, menu);
        if (mBeacon.getState() == KBConnState.Connected)
        {
            menu.findItem(R.id.menu_connect).setEnabled(true);
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_connecting).setVisible(false);
            menu.findItem(R.id.menu_connecting).setActionView(null);
            mBeaconStatus.setText("Connected");
        }
        else if (mBeacon.getState() == KBConnState.Connecting)
        {
            mBeaconStatus.setText("Connecting");
            menu.findItem(R.id.menu_connect).setEnabled(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        else
        {
            mBeaconStatus.setText("Disconnected");
            menu.findItem(R.id.menu_connect).setEnabled(true);
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setVisible(false);
            menu.findItem(R.id.menu_connecting).setActionView(null);
        }
        return true;
    }


    @Override
    public void onClick(View v)
    {
        int id = v.getId();
        if (id == R.id.buttonSaveData) {
            updateViewToDevice();
        }else if (id == R.id.resetConfigruation) {
            resetParameters();
        }
    }

    void updateViewToDevice()
    {
        if (!mBeacon.isConnected())
        {
            return;
        }

        KBCfgAdvIBeacon iBeaconCfg = new KBCfgAdvIBeacon();

        //slot index
        iBeaconCfg.setSlotIndex(0);
        // (not sure what this is)
        iBeaconCfg.setAdvMode(KBAdvMode.Legacy);
        // lower advertising interval
        iBeaconCfg.setAdvPeriod(400.0f);
        // higher tx power
        iBeaconCfg.setTxPower(4);
        // shouldn't be connectable
        iBeaconCfg.setAdvConnectable(true); // NOTE: not set to false
        // always advertise
        iBeaconCfg.setAdvTriggerOnly(false);
        // iBeacon uuid data
        iBeaconCfg.setUuid("9cbaf2ab-a69b-4e71-b622-fbf7d211e969");
        // iBeacon major id data
        iBeaconCfg.setMajorID(1);
        // iBeacon minor id data
        iBeaconCfg.setMinorID(Integer.valueOf(mEditSelectMinor.getText().toString()));

        //add periodically trigger
        KBCfgTrigger periodicTrigger = new KBCfgTrigger(0, KBTriggerType.BtnSingleClick);
        periodicTrigger.setTriggerAction(KBTriggerAction.Advertisement);
        periodicTrigger.setTriggerAdvSlot(0);
        periodicTrigger.setTriggerAdvTime(10);

        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(iBeaconCfg);
        cfgList.add(periodicTrigger);
        mDownloadButton.setEnabled(false);
        mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDownloadButton.setEnabled(true);
                if (bConfigSuccess)
                {
                    toastShow("Beacon configured");
                    updateDeviceToView();
                    minor = Integer.valueOf(mEditSelectMinor.getText().toString()) + 1;
                    mEditSelectMinor.setText(String.valueOf(minor));
                }
                else
                {
                    if (error.errorCode == KBErrorCode.CfgBusy)
                    {
                        toastShow("Beacon configuration already in progress");
                    }
                    else
                    {
                        toastShow("Failed to configure beacon: " + error.errorCode);
                    }
                }
            }
        });
    }

    public void resetParameters() {
        if (!mBeacon.isConnected()) {
            return;
        }

        JSONObject cmdPara = new JSONObject();
        try {
            cmdPara.put("msg", "admin");
            cmdPara.put("stype", "reset");
        }catch (JSONException except)
        {
            except.printStackTrace();
            return;
        }
        mResetButton.setEnabled(false);
        mBeacon.sendCommand(cmdPara, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mResetButton.setEnabled(true);
                if (bConfigSuccess)
                {
                    //disconnect with device to make sure the new parameters take effect
                    mBeacon.disconnect();
                    toastShow("Beacon config reset");
                }
                else
                {
                    toastShow("Failed to reset beacon config: " + error.errorCode);
                }
            }
        });
    }

    public void updateDeviceToView()
    {
        KBCfgCommon commonCfg = mBeacon.getCommonCfg();
        KBCfgAdvBase slot0Adv = mBeacon.getSlotCfg(0);

        if (commonCfg != null) {
            //print basic capibility
            Log.v(LOG_TAG, "support iBeacon:" + commonCfg.isSupportIBeacon());
            Log.v(LOG_TAG, "support eddy url:" + commonCfg.isSupportEddyURL());
            Log.v(LOG_TAG, "support eddy tlm:" + commonCfg.isSupportEddyTLM());
            Log.v(LOG_TAG, "support eddy uid:" + commonCfg.isSupportEddyUID());
            Log.v(LOG_TAG, "support ksensor:" + commonCfg.isSupportKBSensor());
            Log.v(LOG_TAG, "beacon has button:" + commonCfg.isSupportButton());
            Log.v(LOG_TAG, "beacon can beep:" + commonCfg.isSupportBeep());
            Log.v(LOG_TAG, "support acceleration sensor:" + commonCfg.isSupportAccSensor());
            Log.v(LOG_TAG, "support humidity sensor:" + commonCfg.isSupportHumiditySensor());
            Log.v(LOG_TAG, "support PIR sensor:" + commonCfg.isSupportPIRSensor());
            Log.v(LOG_TAG, "support CO2 sensor:" + commonCfg.isSupportCO2Sensor());
            Log.v(LOG_TAG, "support light sensor:" + commonCfg.isSupportLightSensor());
            Log.v(LOG_TAG, "support VOC sensor:" + commonCfg.isSupportVOCSensor());
            Log.v(LOG_TAG, "support max tx power:" + commonCfg.getMaxTxPower());
            Log.v(LOG_TAG, "support min tx power:" + commonCfg.getMinTxPower());
            Log.v(LOG_TAG, "device battery:" + commonCfg.getBatteryPercent());

            //slot adv type list
            ArrayList<KBCfgAdvBase> advArrays = mBeacon.getSlotCfgList();
            String strAdvArrays = "";
            if (advArrays != null) {
                for (KBCfgAdvBase adv : advArrays) {
                    strAdvArrays = strAdvArrays + "Slot:" + adv.getSlotIndex() +
                            ":" + KBAdvType.getAdvTypeString(adv.getAdvType()) + "|";
                }
            }
            mBeaconType.setText(strAdvArrays);

            //device model
            mBeaconModel.setText(commonCfg.getModel());

            //device name
            mEditBeaconName.setText(String.valueOf(commonCfg.getName()));

            //slot 0 parameters
            ArrayList<KBCfgAdvBase> allIBeaconAdvs = mBeacon.getSlotCfgByAdvType(KBAdvType.IBeacon);
            if (allIBeaconAdvs != null) {
                KBCfgAdvIBeacon iBeaconPara = (KBCfgAdvIBeacon)allIBeaconAdvs.get(0);
                mEditBeaconUUID.setText(iBeaconPara.getUuid());
                mEditBeaconMajor.setText(String.valueOf(iBeaconPara.getMajorID()));
                 mEditBeaconMinor.setText(String.valueOf(iBeaconPara.getMinorID()));

                if (slot0Adv != null) {
                    mEditBeaconAdvPeriod.setText(String.valueOf(slot0Adv.getAdvPeriod()));
                    mEditBeaconTxPower.setText(String.valueOf(slot0Adv.getTxPower()));
                }
            }
        }
    }


    private KBConnState nDeviceConnState = KBConnState.Disconnected;

    public void onConnStateChange(KBeacon beacon, KBConnState state, int nReason)
    {
        if (state == KBConnState.Connected)
        {
            Log.v(LOG_TAG, "device has connected");
            invalidateOptionsMenu();

            mDownloadButton.setEnabled(true);
            mResetButton.setEnabled(true);

            updateDeviceToView();

            nDeviceConnState = state;
        }
        else if (state == KBConnState.Connecting)
        {
            Log.v(LOG_TAG, "device start connecting");
            invalidateOptionsMenu();

            nDeviceConnState = state;
        }
        else if (state == KBConnState.Disconnecting) {
            Log.e(LOG_TAG, "connection error, now disconnecting");
        }
        else
        {
            if (nDeviceConnState == KBConnState.Connecting)
            {
                if (nReason == KBConnectionEvent.ConnAuthFail)
                {
                    final EditText inputServer = new EditText(this);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.auth_error_title));
                    builder.setView(inputServer);
                    builder.setNegativeButton(R.string.Dialog_Cancel, null);
                    builder.setPositiveButton(R.string.Dialog_OK, null);
                    final AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String strNewPassword = inputServer.getText().toString().trim();
                            if (strNewPassword.length() < 8|| strNewPassword.length() > 16)
                            {
                                Toast.makeText(DevicePannelActivity.this,
                                        R.string.connect_error_auth_format,
                                        Toast.LENGTH_SHORT).show();
                            }else {
                                mPref.setPassword(mDeviceAddress, strNewPassword);
                                alertDialog.dismiss();
                            }
                        }
                    });
                }
                else
                {
                    toastShow("connect to device failed, reason:" + nReason);
                }
            }

            mDownloadButton.setEnabled(false);
            mResetButton.setEnabled(false);
            Log.e(LOG_TAG, "device has disconnected:" +  nReason);
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBeacon.getState() == KBConnState.Connected
            || mBeacon.getState() == KBConnState.Connecting){
            mBeacon.disconnect();
            invalidateOptionsMenu();
        }
    }


    public boolean check2RequestPermission()
    {
        boolean bHasPermission = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        PERMISSION_CONNECT);
                bHasPermission = false;
            }
        }
        return bHasPermission;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mBeacon.connect(mPref.getPassword(mDeviceAddress),
                        20 * 1000,
                        this);
            } else {
                toastShow("The app need ble connection permission for start ble scanning");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_connect){

            if (DevicePannelActivity.READ_DEFAULT_PARAMETERS)
            {
                //connect to device with default parameters
                if (check2RequestPermission()) {
                    mBeacon.connect(mPref.getPassword(mDeviceAddress), 20 * 1000, this);
                }
            }
            else
            {
                //connect to device with specified parameters
                //When the app is connected to the KBeacon device, the app can specify which the configuration parameters to be read,
                //The parameter that can be read include: common parameters, advertisement parameters, trigger parameters, and sensor parameters
                KBConnPara connPara = new KBConnPara();
                connPara.syncUtcTime = true;
                connPara.readCommPara = true;
                connPara.readSlotPara = true;
                connPara.readTriggerPara = false;
                connPara.readSensorPara = false;
                mBeacon.connectEnhanced(mPref.getPassword(mDeviceAddress), 20 * 1000,
                        connPara,
                        this);
            }

            invalidateOptionsMenu();
        }
        else if(id == R.id.menu_disconnect){
            mBeacon.disconnect();
            invalidateOptionsMenu();
        }

        return super.onOptionsItemSelected(item);
    }
}
