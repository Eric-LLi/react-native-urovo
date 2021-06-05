// UrovoModule.java

package com.urovo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.nativec.tools.ModuleManager;
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

    private static ArrayList<String> cacheTags = new ArrayList<>();
    private static boolean isSingleRead = false;
    private static boolean isReadBarcode = false;
    private static boolean isReading = false;
    //    private static boolean loopFlag = false;
//    private final byte btReadId = (byte) 0xFF;

    //RFID
    private static final ModuleConnector mConnector = new ReaderConnector();
    private static RFIDReaderHelper mReaderHelper;

    private static ReaderSetting m_curReaderSetting;
    //Barcode

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
//                    barcodeRead();
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
//                    barcodeCancel();
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
        if (mReaderHelper != null) {
            //
        }
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
                int result = mReaderHelper.setOutputPower((byte) m_curReaderSetting.btReadId, (byte) antennaLevel);

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
    public void programTag(String oldTag, String newTag, Promise promise) {
        try {
            if (mConnector.isConnected()) {
//            String strPWD = "00000000";
//            int cntStr = 6;
//            int filterPtr = 32;
//            int strPtr = 2;
//
//            boolean result = mReader.writeData(
//                    strPWD,
//                    IUHF.Bank_EPC,
//                    filterPtr,
//                    oldTag.length() * 4,
//                    oldTag,
//                    IUHF.Bank_EPC,
//                    strPtr,
//                    cntStr,
//                    newTag
//            );
//
//            WritableMap map = Arguments.createMap();
//            map.putBoolean("status", result);
//            map.putString("error", result ? null : "Program tag fail");
//
//            sendEvent(WRITE_TAG_STATUS, map);
            } else {
                throw new Exception("Reader is not connected");
            }
        } catch (Exception err) {
            promise.reject(err);
        }

    }

    @ReactMethod
    public void setEnabled(boolean enable, Promise promise) {
        if (mConnector.isConnected()) {
            if (enable) {
                isReadBarcode = false;
            } else {
                isReadBarcode = true;
            }
        }
        promise.resolve(true);
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

        set53CGPIOEnabled(true);

        boolean result = mConnector.connectCom("/dev/ttyHSL0", 115200, this.reactContext);

        mReaderHelper = RFIDReaderHelper.getDefaultHelper();
        mReaderHelper.registerObserver(rxObserver);
        mReaderHelper.setRXTXListener(mListener);

        m_curReaderSetting = ReaderSetting.newInstance();
        if (result) mReaderHelper.getOutputPower(m_curReaderSetting.btReadId);

        this.reactContext.registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        WritableMap map = Arguments.createMap();
        map.putBoolean("status", result);
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

        WritableMap map = Arguments.createMap();
        map.putBoolean("status", false);
        map.putString("error", null);

        sendEvent(READER_STATUS, map);
    }

    private void read() {
        if (mConnector.isConnected()) {
            isReading = true;

            if (isSingleRead) {
                mReaderHelper.realTimeInventory(m_curReaderSetting.btReadId, (byte) 1);
            } else {
                mLoopRunnable.run();
            }
        }
    }

    private void cancel() {
        isReading = false;
        mLoopHandler.removeCallbacks(mLoopRunnable);
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
            Log.d(LOG, "onOperationTag:::" + tag);
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
            //获取当前电量,范围是 0～100

            int level = intent.getIntExtra("level", 0);
            Log.d(LOG, "mReceiver");

            WritableMap map = Arguments.createMap();
            map.putInt("level", level);
            sendEvent(BATTERY_STATUS, map);
        }
    };

    private void set53GPIOEnabled(boolean enable) {
        FileOutputStream f = null;
        FileOutputStream f1 = null;
        try {
            Log.i("ubx", "set53GPIOcEnabled: " + enable);
            f = new FileOutputStream("/sys/devices/soc/c170000.serial/pogo_uart");
            f.write(enable ? "1".getBytes() : "0".getBytes());
            f1 = new FileOutputStream("/sys/devices/virtual/Usb_switch/usbswitch/function_otg_en");
            f1.write(enable ? "2".getBytes() : "0".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (f != null) {
                try {
                    f.close();
                    f1.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

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
                    f1.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
