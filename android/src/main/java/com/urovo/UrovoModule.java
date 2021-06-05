// UrovoModule.java

package com.urovo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.device.scanner.configuration.Triggering;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.module.interaction.ModuleConnector;
import com.module.interaction.RXTXListener;
import com.rfid.RFIDReaderHelper;
import com.rfid.ReaderConnector;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.rfid.bean.MessageTran;
import com.rfid.config.CMD;
import com.rfid.config.ERROR;
import com.rfid.rxobserver.RXObserver;
import com.rfid.rxobserver.ReaderSetting;
import com.rfid.rxobserver.bean.RXInventoryTag;
import com.rfid.rxobserver.bean.RXOperationTag;
import com.util.StringTool;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.device.ScanManager.ACTION_DECODE;
import static android.device.ScanManager.BARCODE_LENGTH_TAG;
import static android.device.ScanManager.BARCODE_STRING_TAG;
import static android.device.ScanManager.DECODE_DATA_TAG;

public class UrovoModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private final String LOG = "[UROVO]";
    private final String READER_STATUS = "READER_STATUS";
    private final String TRIGGER_STATUS = "TRIGGER_STATUS";
    private final String WRITE_TAG_STATUS = "WRITE_TAG_STATUS";
    private final String BATTERY_STATUS = "BATTERY_STATUS";
    private final String TAG = "TAG";
    private final String BARCODE = "BARCODE";
    private final ReactApplicationContext reactContext;
    private static UrovoModule instance = null;

    private static final ArrayList<String> cacheTags = new ArrayList<>();
    private static boolean isSingleRead = false;
    private static boolean isReadBarcode = false;
    private static boolean isReading = false;

    private static String newTag;

    //RFID
    private static final ModuleConnector mConnector = new ReaderConnector();
    private static RFIDReaderHelper mReaderHelper;

    private static ReaderSetting m_curReaderSetting;
    //Barcode
    private ScanManager mScanManager = null;

    public UrovoModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.reactContext.addLifecycleEventListener(this);
        instance = this;
    }

    public static UrovoModule getInstance() {
        return instance;
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        this.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private void sendEvent(String eventName, String msg) {
        this.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, msg);
    }

    public void onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 523) {
            if (event.getRepeatCount() == 0) {
                if (isReadBarcode) {
                    barcodeRead();
                } else {
                    read();
                }

                WritableMap map = Arguments.createMap();
                map.putBoolean("status", true);
                sendEvent(TRIGGER_STATUS, map);
            }
        }
    }

    public void onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 523) {
            if (event.getRepeatCount() == 0) {
                if (isReadBarcode) {
                    barcodeCancel();
                } else {
                    cancel();
                }

                WritableMap map = Arguments.createMap();
                map.putBoolean("status", false);
                sendEvent(TRIGGER_STATUS, map);
            }
        }
    }

    @Override
    public String getName() {
        return "Urovo";
    }

    @Override
    public void onHostResume() {
        if (mReaderHelper != null) {
            mReaderHelper.startWith();
        }
    }

    @Override
    public void onHostPause() {
        //
    }

    @Override
    public void onHostDestroy() {
        doDisconnect();
    }

    @ReactMethod
    public void isConnected(Promise promise) {
        try {
            promise.resolve(mConnector.isConnected());
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void connect(Promise promise) {
        try {
            doConnect();
            promise.resolve(true);
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void reconnect(Promise promise) {
        try {
            doConnect();
            promise.resolve(true);
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void disconnect(Promise promise) {
        try {
            doDisconnect();
            promise.resolve(true);
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void clear() {
        cacheTags.clear();
    }

    @ReactMethod
    public void setSingleRead(boolean enable) {
        isSingleRead = enable;
    }

    @ReactMethod
    public void getDeviceDetails(Promise promise) {
        try {
            if (mConnector.isConnected()) {
                int antenna = 0;

                if (m_curReaderSetting.btAryOutputPower != null && m_curReaderSetting.btAryOutputPower.length > 0)
                    antenna = m_curReaderSetting.btAryOutputPower[0];

                WritableMap map = Arguments.createMap();
                map.putString("name", "Urovo");
                map.putString("mac", "");
                map.putInt("antennaLevel", antenna);

                promise.resolve(map);
            } else {
                throw new Exception("Reader is not connected");
            }
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void setAntennaLevel(int antennaLevel, Promise promise) {
        try {
            if (mConnector.isConnected()) {
                int result = mReaderHelper.setOutputPower(m_curReaderSetting.btReadId, (byte) antennaLevel);

                if (result == 0)
                    m_curReaderSetting.btAryOutputPower = new byte[]{(byte) antennaLevel};

                promise.resolve(result);
            } else {
                throw new Exception("Reader is not connected");
            }
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void programTag(String oldTag, String newTag2, Promise promise) {
        try {
            if (mConnector.isConnected()) {
                newTag = newTag2;

                byte[] btAryEpc = StringToBytes(oldTag.toUpperCase());
                int result = mReaderHelper.setAccessEpcMatch(m_curReaderSetting.btReadId, (byte) btAryEpc.length, btAryEpc);

                promise.resolve(result == 0);
            } else {
                throw new Exception("Reader is not connected");
            }
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    @ReactMethod
    public void setEnabled(boolean enable, Promise promise) {
        try {
            if (mConnector.isConnected()) {
                if (enable) {
                    mScanManager.lockTrigger();
                    isReadBarcode = false;
                } else {
                    mScanManager.unlockTrigger();
                    isReadBarcode = true;
                }
                promise.resolve(true);
            } else {
                throw new Exception("Reader is not connected");
            }
        } catch (Exception err) {
            promise.reject(err);
        }

    }

    @ReactMethod
    public void softReadCancel(boolean enable, Promise promise) {
        try {
            if (mConnector.isConnected()) {
                if (enable) {
                    read();
                } else {
                    cancel();
                }

                promise.resolve(true);
            } else {
                throw new Exception("Reader is not connected");
            }
        } catch (Exception err) {
            promise.reject(err);
        }
    }

    private void doConnect() throws Exception {
        if (mConnector.isConnected()) {
            doDisconnect();
        }

        //RFID
        set53CGPIOEnabled(true);

        boolean result = mConnector.connectCom("/dev/ttyHSL0", 115200, this.reactContext);

        if (!result) throw new Exception("Failed to power on reader");

        mReaderHelper = RFIDReaderHelper.getDefaultHelper();
        mReaderHelper.registerObserver(rxObserver);
        mReaderHelper.setRXTXListener(mListener);

        m_curReaderSetting = ReaderSetting.newInstance();

        mReaderHelper.getOutputPower(m_curReaderSetting.btReadId);

        //Barcode
        mScanManager = new ScanManager();
        boolean powerOn = mScanManager.getScannerState();
        if (!powerOn) {
            powerOn = mScanManager.openScanner();

            if (!powerOn) throw new Exception("Failed to power on barcode scanner");
        }

        mScanManager.enableAllSymbologies(true);
        mScanManager.setTriggerMode(Triggering.HOST);
        //Set output mode to 0=Intent, 1=TextInput
        mScanManager.switchOutputMode(0);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DECODE);
        filter.addAction(ACTION_BATTERY_CHANGED);
        this.reactContext.registerReceiver(mReceiver, filter);

        WritableMap map = Arguments.createMap();
        map.putBoolean("status", true);
        map.putString("error", null);

        sendEvent(READER_STATUS, map);
    }

    private void doDisconnect() {
        set53CGPIOEnabled(false);
        try {
            this.reactContext.unregisterReceiver(mReceiver);
        } catch (Exception err) {
            err.printStackTrace();
        }

        mConnector.disConnect(this.reactContext);

        if (mReaderHelper != null) {
            mReaderHelper.unRegisterObservers();
            mReaderHelper.signOut();

            mReaderHelper = null;
        }


        if (m_curReaderSetting != null) {
            m_curReaderSetting = null;
        }

        //Barcode
        if (mScanManager != null) {
            mScanManager.stopDecode();
            mScanManager.closeScanner();

            mScanManager = null;
        }

        WritableMap map = Arguments.createMap();
        map.putBoolean("status", false);
        map.putString("error", null);

        sendEvent(READER_STATUS, map);
    }

    private void read() {
        if (mConnector.isConnected()) {
            isReading = true;
            mLoopRunnable.run();
        }
    }

    private void cancel() {
        isReading = false;
        mLoopHandler.removeCallbacks(mLoopRunnable);
    }

    private void barcodeRead() {
        if (mScanManager != null) {
            mScanManager.startDecode();
        }
    }

    private void barcodeCancel() {
        if (mScanManager != null) {
            mScanManager.stopDecode();
        }
    }

    private final Handler mLoopHandler = new Handler(Looper.getMainLooper());
    private final Runnable mLoopRunnable = new Runnable() {
        @Override
        public void run() {
            mLoopHandler.removeCallbacks(this);

            mReaderHelper.customizedSessionTargetInventory(m_curReaderSetting.btReadId, (byte) 1, (byte) 0, (byte) 1);
        }
    };

    private final RXObserver rxObserver = new RXObserver() {

        @Override
        protected void onExeCMDStatus(byte cmd, byte status) {
            String strLog = FormatUtils.format(cmd, status);
            Log.d(LOG, "onExeCMDStatus: " + strLog);

            if (cmd == CMD.SET_ACCESS_EPC_MATCH) {
                if (status == ERROR.SUCCESS) {
                    //Tag memory bank(0x00:RESERVED, 0x01:EPC, 0x02:TID, 0x03:USER)
                    byte btMemBank = 0x01;
                    byte btWordAdd = 0x02;
                    byte[] btAryPassWord = StringToBytes("00000000");
                    byte[] btAryData = StringToBytes(newTag.toUpperCase());
                    byte btWordCnt = (byte) ((btAryData.length / 2 + btAryData.length % 2) & 0xFF);

                    mReaderHelper.blockWriteTag(m_curReaderSetting.btReadId, btAryPassWord, btMemBank, btWordAdd, btWordCnt, btAryData);
                } else {
                    WritableMap map = Arguments.createMap();
                    map.putBoolean("status", false);
                    map.putString("error", strLog);
                    sendEvent(WRITE_TAG_STATUS, map);
                }
            } else if (cmd == CMD.WRITE_TAG || cmd == CMD.BLOCK_WRITE_TAG) {
                WritableMap map = Arguments.createMap();
                if (status == ERROR.SUCCESS) {
                    map.putBoolean("status", true);
                    map.putString("error", null);
                } else {
                    map.putBoolean("status", false);
                    map.putString("error", strLog);
                }
                sendEvent(WRITE_TAG_STATUS, map);
            }
        }

        @Override
        protected void refreshSetting(ReaderSetting readerSetting) {
            m_curReaderSetting = readerSetting;
            Log.d(LOG, "refreshSetting");
        }

        @Override
        protected void onInventoryTag(RXInventoryTag tag) {
            String epc = tag.strEPC.replaceAll(" ", "");

            Log.d(LOG, "epc:" + epc);
            int rssi = Integer.parseInt(tag.strRSSI);

            if (isSingleRead) {
                if (addTagToList(epc) && cacheTags.size() == 1) {
                    cancel();

                    sendEvent(TAG, epc);
                }
            } else {
                if (addTagToList(epc)) {
                    sendEvent(TAG, epc);
                }
            }
        }

        @Override
        protected void onInventoryTagEnd(RXInventoryTag.RXInventoryTagEnd tagEnd) {
            Log.d(LOG, "onInventoryTagEnd");

            if (isReading) {
                mLoopRunnable.run();
            }
        }

        @Override
        protected void onFastSwitchAntInventoryTagEnd(RXInventoryTag.RXFastSwitchAntInventoryTagEnd tagEnd) {
            Log.d(LOG, "onFastSwitchAntInventoryTagEnd");
        }

        @Override
        protected void onGetInventoryBufferTagCount(int nTagCount) {
            Log.d(LOG, "onGetInventoryBufferTagCount");
        }

        @Override
        protected void onOperationTag(RXOperationTag tag) {
            Log.d(LOG, "onOperationTag:::" + tag.strEPC + " " + tag.cmd);

            if (tag.cmd == CMD.WRITE_TAG || tag.cmd == CMD.BLOCK_WRITE_TAG) {
                WritableMap map = Arguments.createMap();
                map.putBoolean("status", true);
                map.putString("error", null);
                sendEvent(WRITE_TAG_STATUS, map);
            }
        }

        @Override
        protected void onInventory6BTag(byte nAntID, String strUID) {
            Log.d(LOG, "onInventory6BTag:::" + strUID);
        }

        @Override
        protected void onInventory6BTagEnd(int nTagCount) {
            Log.d(LOG, "onInventory6BTagEnd");
        }

        @Override
        protected void onRead6BTag(byte antID, String strData) {
            Log.d(LOG, "onRead6BTag:::" + strData);
        }

        @Override
        protected void onWrite6BTag(byte nAntID, byte nWriteLen) {
            Log.d(LOG, "onWrite6BTag");
        }

        @Override
        protected void onLock6BTag(byte nAntID, byte nStatus) {
            Log.d(LOG, "onLock6BTag");
        }

        @Override
        protected void onLockQuery6BTag(byte nAntID, byte nStatus) {
            Log.d(LOG, "onLockQuery6BTag");
        }

        @Override
        protected void onConfigTagMask(MessageTran msgTran) {
            Log.d(LOG, "onConfigTagMask");
        }
    };

    private final RXTXListener mListener = new RXTXListener() {
        @Override
        public void reciveData(byte[] bytes) {
            String strLog = StringTool.byteArrayToString(bytes, 0, bytes.length);

            Log.d(LOG, "reciveData: " + strLog);
        }

        @Override
        public void sendData(byte[] bytes) {
            String strLog = StringTool.byteArrayToString(bytes, 0, bytes.length);

            Log.d(LOG, "sendData: " + strLog);
        }

        @Override
        public void onLostConnect() {
            Log.d(LOG, "onLostConnect");
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOG, "onReceive , action:" + action);

            if (action.equals(ACTION_BATTERY_CHANGED)) {
                //获取当前电量,范围是 0～100
                int level = intent.getIntExtra("level", 0);
                WritableMap map = Arguments.createMap();
                map.putInt("level", level);
                sendEvent(BATTERY_STATUS, map);
            } else if (action.equals(ACTION_DECODE)) {
                String barcode = intent.getStringExtra(BARCODE_STRING_TAG);

                sendEvent(BARCODE, barcode);
            }

        }
    };

    private void set53CGPIOEnabled(boolean enable) {
        FileOutputStream f = null;
        FileOutputStream f1 = null;
        try {
            Log.i("urovo", "enable:" + enable);
            f = new FileOutputStream("/sys/devices/soc/soc:sectrl/ugp_ctrl/gp_pogo_5v_ctrl/enable");
            f.write(enable ? "1".getBytes() : "0".getBytes());
            f1 = new FileOutputStream("/sys/devices/soc/soc:sectrl/ugp_ctrl/gp_otg_en_ctrl/enable");
            f1.write(enable ? "1".getBytes() : "0".getBytes());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (f1 != null) {
                try {
                    f1.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private byte[] StringToBytes(String s) {
        int var1 = s.length();
        byte[] var2 = new byte[var1 / 2];

        for (int var3 = 0; var3 < var1; var3 += 2) {
            var2[var3 / 2] = (byte) ((Character.digit(s.charAt(var3), 16) << 4) + Character.digit(s.charAt(var3 + 1), 16));
        }

        return var2;
    }

    private boolean addTagToList(String strEPC) {
        if (strEPC != null) {
            if (!cacheTags.contains(strEPC)) {
                cacheTags.add(strEPC);
                return true;
            }
        }
        return false;
    }
}
