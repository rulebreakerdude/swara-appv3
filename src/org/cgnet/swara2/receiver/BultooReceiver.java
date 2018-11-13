//package org.cgnet.swara.receiver;
//
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.util.Log;
//import org.cgnet.swara.R;
//import org.cgnet.swara.activity.HomeActivity;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import cgnet.swara.activity.MainActivity;
//
//import static org.cgnet.swara.Constants.ACTION_FILE_SELECTED;
//
//public class BultooReceiver extends BroadcastReceiver {
//
//    private static String TAG = "BultooReceiver";
//
//    private static boolean mConnected = false;
//
//    private static Long timeStartWhenConnected = 0L;
//
//    private static String mDeviceAddress = "";
//
//    private static String mDeviceName = "";
//
//    private static String mFileName = "";
//
//    private static boolean isApk = false;
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        String action = intent.getAction();
//        if (action.equals(ACTION_FILE_SELECTED)) {
//
//            mFileName = intent.getStringExtra("FILE_NAME");
//            Log.e(TAG, mFileName);
//
//            if (mFileName.equals(context.getString(R.string.apk_file_name))){
//                isApk = true;
//            }
//
//        } else if (action.equals("android.bluetooth.device.action.ACL_CONNECTED") && !mFileName.equals("")) {
//            Log.e(TAG, "Device connected");
//            mConnected = true;
//            timeStartWhenConnected = System.currentTimeMillis()/1000;
//            Log.e(TAG, "Time when started "+timeStartWhenConnected);
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//            mDeviceAddress = device.getAddress(); // MAC address
//            mDeviceName = device.getName();
//            Log.e(TAG, mDeviceAddress + " and " + mDeviceName);
//
//        } else if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED")) {
//
//            Log.e(TAG, "Device disconnected");
//            Long clientEpochTime = System.currentTimeMillis();
//            Long timeWhenDisconnected = clientEpochTime/1000;
//            Log.e(TAG, "Time when started "+timeWhenDisconnected);
//            Log.e(TAG,"TIME "+(timeWhenDisconnected-timeStartWhenConnected));
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//            Log.e(TAG, "Disconnected "+device.getAddress());
//            if (mConnected && device.getAddress().equals(mDeviceAddress) && (timeWhenDisconnected-timeStartWhenConnected)>15) {
//                mConnected = false;
//                Log.e(mDeviceAddress+" + "+ mDeviceName, mFileName);
//
//                String senderDeviceAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
//
//                JSONObject obj = new JSONObject();
//                try {
//                    obj.put("senderBTMAC", senderDeviceAddress);
//                    obj.put("receiverBTMAC", mDeviceAddress);
//                    obj.put("filename", mFileName);
//                    obj.put("appName",context.getString(R.string.app_name_for_topup_server));
//                    obj.put("clientEpoch", clientEpochTime );
//
//                    for(int i = 0; i<obj.names().length(); i++){
//                        Log.e(TAG, "key = " + obj.names().getString(i) + " value = " + obj.get(obj.names().getString(i)));
//                    }
//
//
//                } catch (JSONException e){
//                    throw new RuntimeException(e);
//                }
//
//                Intent promptIntent = new Intent(context, isApk ? MainActivity.class: HomeActivity.class);
//                promptIntent.putExtra(context.getString(R.string.json_object),obj.toString());
//                promptIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(promptIntent);
//
////                // Save the top up amount for the user to cah-in later
////                SharedPreferences sharedPref = context.getSharedPreferences(
////                        context.getString(R.string.top_up_preference_file_key),Context.MODE_PRIVATE);
////                float cashInAmount = sharedPref.getFloat(context.getString(R.string.cash_in_amount),0);
////                if (isApk) {
////                    cashInAmount += 10;
////                } else {
////                    cashInAmount += 1;
////                }
////
////                SharedPreferences.Editor editor = sharedPref.edit();
////                editor.putFloat(context.getString(R.string.cash_in_amount), cashInAmount);
////                editor.apply();
//
//            }
//            mDeviceName = "";
//            mDeviceAddress = "";
//            mFileName = "";
//        }
//    }
//
//
//
//}
