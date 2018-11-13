package cgnet.swara.activity;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.msr.sneakernetcommon.SneakernetCommon;
import com.msr.sneakernetcommon.network.DefaultListeners;
import com.msr.sneakernetcommon.network.NetworkRequest;
import com.msr.sneakernetcommon.network.RetryingNetworkRequest;
import com.msr.sneakernetcommon.utils.PhoneRetrieveListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class SendTopUpRequest implements PhoneRetrieveListener {

    private  String TAG = "SendTopUpRequest";
    WeakReference<Context> ref;
    JSONObject body;

    public SendTopUpRequest(Context ref, JSONObject body) {
        this.ref = new WeakReference<>(ref);
        this.body = body;
    }

    @Override
    public void onPhoneRetrieved(String phone, int carrierCode) {
        Log.e(TAG, "Posting to server");
        Context ctx = ref.get();
        String url = "";//http://sneakernet.southindia.cloudapp.azure.com:3978/sentBluetooth//context.getString(R.string.development_cloud_endpoint)+"/sentBluetooth/";
        if (ctx == null) {
            return;
        }
        try {
            body.put("phoneNumber", phone);
            body.put("carrierCode", carrierCode);
        } catch (JSONException e) {
            throw new AssertionError("JSON Error");
        }
        Log.e(TAG, body.toString());
//        SneakernetCommon mainApplication = (SneakernetCommon) ctx.getApplicationContext();
//        NetworkRequest<JSONObject> request = new RetryingNetworkRequest<>(mainApplication, url,
//                Request.Method.POST, body, DefaultListeners.emptyObjectListener);
//        mainApplication.getNetworkRequestor().issueJsonObjectRequest(ctx, request);
    }
}
