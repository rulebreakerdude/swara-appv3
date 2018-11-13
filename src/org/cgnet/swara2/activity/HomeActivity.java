/**
 * FeedEx
 *
 * Copyright (c) 2012-2013 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cgnet.swara2.activity;
 
import org.cgnet.swara2.Constants;
import org.cgnet.swara2.MainApplication;
import org.cgnet.swara2.MainApplication.TrackerName;
import org.cgnet.swara2.R;
import org.cgnet.swara2.adapter.DrawerAdapter;
import org.cgnet.swara2.fragment.EntriesListFragment;
import org.cgnet.swara2.provider.FeedData;
import org.cgnet.swara2.provider.FeedData.EntryColumns;
import org.cgnet.swara2.provider.FeedData.FeedColumns;
import org.cgnet.swara2.provider.FeedDataContentProvider;
import org.cgnet.swara2.service.FetcherService;
import org.cgnet.swara2.service.RefreshService;
import org.cgnet.swara2.utils.PrefUtils;
import org.cgnet.swara2.utils.UiUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.LoaderManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.android.volley.Request;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import com.msr.sneakernetcommon.SneakernetCommon;
import com.msr.sneakernetcommon.network.DefaultListeners;
import com.msr.sneakernetcommon.network.NetworkRequest;
import com.msr.sneakernetcommon.network.RetryingNetworkRequest;
import com.msr.sneakernetcommon.utils.PhoneRetrieveListener;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import static org.cgnet.swara2.Constants.ACTION_BULTOO_FILE_SELECTED;

public class HomeActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "HomeActivity";
    private static int cnt = 0;

    private static final String STATE_CURRENT_DRAWER_POS = "STATE_CURRENT_DRAWER_POS";

    private static final String FEED_UNREAD_NUMBER = "(SELECT " + Constants.DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " +
            EntryColumns.IS_READ + " IS NULL AND " + EntryColumns.FEED_ID + '=' + FeedColumns.TABLE_NAME + '.' + FeedColumns._ID + ')';

    private static final String WHERE_UNREAD_ONLY = "(SELECT " + Constants.DB_COUNT + " FROM " + EntryColumns.TABLE_NAME + " WHERE " +
            EntryColumns.IS_READ + " IS NULL AND " + EntryColumns.FEED_ID + "=" + FeedColumns.TABLE_NAME + '.' + FeedColumns._ID + ") > 0" +
            " OR (" + FeedColumns.IS_GROUP + "=1 AND (SELECT " + Constants.DB_COUNT + " FROM " + FeedData.ENTRIES_TABLE_WITH_FEED_INFO +
            " WHERE " + EntryColumns.IS_READ + " IS NULL AND " + FeedColumns.GROUP_ID + '=' + FeedColumns.TABLE_NAME + '.' + FeedColumns._ID +
            ") > 0)";

    private static final int LOADER_ID = 0;

    private final SharedPreferences.OnSharedPreferenceChangeListener mShowReadListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PrefUtils.SHOW_READ.equals(key)) {
                getLoaderManager().restartLoader(LOADER_ID, null, HomeActivity.this);
            }
        }
    };

    private EntriesListFragment mEntriesFragment;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private DrawerAdapter mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mTitle;
    private BitmapDrawable mIcon;
    private int mCurrentDrawerPos;

    private boolean mIsDrawerMoving = false;
    private IntentFilter mFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);
        
        PrefUtils.putBoolean(PrefUtils.SHOW_READ, true);

        mFilter = new IntentFilter();
        mFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mFilter.addAction(ACTION_BULTOO_FILE_SELECTED);
        HomeActivity.this.registerReceiver(bultooReceiver, mFilter);
        
        getWindow().setBackgroundDrawableResource(R.color.light_entry_list_background);

//        Bundle extras = getIntent().getExtras();
//        String url = extras.getString("podcastURL");

        PrefUtils.putString(PrefUtils.KEEP_TIME, "15");

        // !!!!
        FeedDataContentProvider.addFeed(HomeActivity.this, getString(R.string.rss_feed_url), getResources().getString(R.string.main_title), true);
        
        setContentView(R.layout.activity_home);

        // Get tracker.
        Tracker t = ((MainApplication) getApplication()).getTracker(TrackerName.APP_TRACKER);
        
        t.setScreenName("View feed");
        
        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());
        
        mEntriesFragment = (EntriesListFragment) getFragmentManager().findFragmentById(R.id.entries_list_fragment);

        mTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        
        // TODO
      //  mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectDrawerItem(position);
                mDrawerLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDrawerLayout.closeDrawer(mDrawerList);
                    }
                }, 50);
            }
        });
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerStateChanged(int newState) {
                if (mIsDrawerMoving && newState == DrawerLayout.STATE_IDLE) {
                    mIsDrawerMoving = false;
                    invalidateOptionsMenu();
                } else if (!mIsDrawerMoving) {
                    mIsDrawerMoving = true;
                    invalidateOptionsMenu();
                }

                super.onDrawerStateChanged(newState);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState != null) {
            mCurrentDrawerPos = savedInstanceState.getInt(STATE_CURRENT_DRAWER_POS);
        }
        // TODO - how long saved
        getLoaderManager().initLoader(LOADER_ID, null, this);

        if (PrefUtils.getBoolean(PrefUtils.REFRESH_ENABLED, true)) {
            // starts the service independent to this activity
            startService(new Intent(this, RefreshService.class));
        } else {
            stopService(new Intent(this, RefreshService.class));
        }
        if (PrefUtils.getBoolean(PrefUtils.REFRESH_ON_OPEN_ENABLED, false)) {
            if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                Log.e("STARTING", "fetcher service");
                startService(new Intent(HomeActivity.this, FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS));
            }
        }

        // json object received from Bultoo Receiver
//        String jsonObj = getIntent().getStringExtra(getString(R.string.json_object));
//        if (jsonObj != null) {
//            try {
//                promptForPhoneNumber(this, false, new SendTopUpRequest(this, new JSONObject(jsonObj)));
//            } catch (JSONException e) {
//                throw new AssertionError("JSON error");
//            }
//        }
    }

    private void refreshTitleAndIcon() {
        getActionBar().setTitle(mTitle);
        switch (mCurrentDrawerPos) {
            case 0:
                getActionBar().setTitle(R.string.all);
                getActionBar().setIcon(R.drawable.ic_statusbar_rss);
                break;
            case 1:
                getActionBar().setTitle(R.string.favorites);
                getActionBar().setIcon(R.drawable.dimmed_rating_important);
                break;
            case 2:
                getActionBar().setTitle(R.string.bultoo);
                getActionBar().setIcon(R.drawable.ic_bluetooth);
                break;
            case 3:
                getActionBar().setTitle(R.string.search_entries);
                getActionBar().setIcon(R.drawable.action_search);
                break;
            default:
                getActionBar().setTitle(mTitle);
                if (mIcon != null) {
                    getActionBar().setIcon(mIcon);
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_DRAWER_POS, mCurrentDrawerPos);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PrefUtils.registerOnPrefChangeListener(mShowReadListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HomeActivity.this.unregisterReceiver(bultooReceiver);
    }

    @Override
    protected void onPause() {
        PrefUtils.unregisterOnPrefChangeListener(mShowReadListener);
        super.onPause();
    }

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) { 
        boolean isOpened = mDrawerLayout.isDrawerOpen(mDrawerList);
        if (isOpened && !mIsDrawerMoving || !isOpened && mIsDrawerMoving) {
            getActionBar().setTitle(R.string.app_name);
            getActionBar().setIcon(R.drawable.icon);

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.drawer, menu);

 
            mEntriesFragment.setHasOptionsMenu(false);
        } else {
            refreshTitleAndIcon();
            mEntriesFragment.setHasOptionsMenu(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) { 
            case R.id.menu_refresh_main:
                if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                    MainApplication.getContext().startService(new Intent(MainApplication.getContext(), FetcherService.class).setAction(FetcherService.ACTION_REFRESH_FEEDS));
                }
                return true; 
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader cursorLoader = new CursorLoader(this, FeedColumns.GROUPED_FEEDS_CONTENT_URI, new String[]{FeedColumns._ID, FeedColumns.URL, FeedColumns.NAME,
                FeedColumns.IS_GROUP, FeedColumns.GROUP_ID, FeedColumns.ICON, FeedColumns.LAST_UPDATE, FeedColumns.ERROR, FEED_UNREAD_NUMBER},
                PrefUtils.getBoolean(PrefUtils.SHOW_READ, true) ? "" : WHERE_UNREAD_ONLY, null, null
        );
        cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) { 
    	mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    	if (mDrawerAdapter != null) {
            mDrawerAdapter.setCursor(cursor);
        } else {
            mDrawerAdapter = new DrawerAdapter(this, cursor);
            mDrawerList.setAdapter(mDrawerAdapter);

            // We don't have any menu yet, we need to display it
            mDrawerList.post(new Runnable() {
                @Override
                public void run() {
                    selectDrawerItem(mCurrentDrawerPos);
                    refreshTitleAndIcon();
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mDrawerAdapter.setCursor(null);
    }

    private void selectDrawerItem(int position) {
        mCurrentDrawerPos = position;
        mIcon = null;

        Uri newUri;
        boolean showFeedInfo = true;

        switch (position) {
            case 0:
                newUri = EntryColumns.ALL_ENTRIES_CONTENT_URI;
                break;
            case 1:
                newUri = EntryColumns.FAVORITES_CONTENT_URI;
                break;
            case 2:
                newUri = EntryColumns.BULTOO_CONTENT_URI;
                break;
            case 3:
                newUri = EntryColumns.SEARCH_URI(mEntriesFragment.getCurrentSearch());
                break;
            default:
                long feedOrGroupId = mDrawerAdapter.getItemId(position);
                if (mDrawerAdapter.isItemAGroup(position)) {
                    newUri = EntryColumns.ENTRIES_FOR_GROUP_CONTENT_URI(feedOrGroupId);
                } else {
                    byte[] iconBytes = mDrawerAdapter.getItemIcon(position);
                    Bitmap bitmap = UiUtils.getScaledBitmap(iconBytes, 24);
                    if (bitmap != null) {
                        mIcon = new BitmapDrawable(getResources(), bitmap);
                    }

                    newUri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(feedOrGroupId);
                    showFeedInfo = false;
                }
                mTitle = mDrawerAdapter.getItemName(position); 
                break; 
        }
          
        if (!newUri.equals(mEntriesFragment.getUri())) {
            mEntriesFragment.setData(newUri, showFeedInfo);
        }

        mDrawerList.setItemChecked(position, true);

        // First open => we open the drawer for you
        if (PrefUtils.getBoolean(PrefUtils.FIRST_OPEN, true)) {
            PrefUtils.putBoolean(PrefUtils.FIRST_OPEN, false);
        }
    }

    public static final BroadcastReceiver bultooReceiver = new BroadcastReceiver() {

        private boolean mConnected = false;

        private Long timeStartWhenConnected = 0L;

        private String mDeviceAddress = "";

        private String mDeviceName = "";

        private String mFileName = "";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_BULTOO_FILE_SELECTED)) {

                mFileName = intent.getStringExtra("FILE_NAME");
                Log.e(TAG, mFileName);


            }else if (action.equals("android.bluetooth.device.action.ACL_CONNECTED") && !mFileName.equals("")) {
                Log.e(TAG, "Device connected");
                mConnected = true;
                timeStartWhenConnected = System.currentTimeMillis()/1000;
                Log.e(TAG, "Time when started "+timeStartWhenConnected);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceAddress = device.getAddress(); // MAC address
                mDeviceName = device.getName();
                Log.e(TAG, mDeviceAddress + " and " + mDeviceName);

            } else if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED")) {


                mConnected = false;
                Long clientEpochTime = System.currentTimeMillis();
                Long timeWhenDisconnected = clientEpochTime / 1000;
                Log.e(TAG, "Time when disconnected " + timeWhenDisconnected);
                Log.e(TAG, "TIME " + (timeWhenDisconnected - timeStartWhenConnected));
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e(TAG, "Disconnected " + device.getAddress());


                if (device.getAddress().equals(mDeviceAddress) && (timeWhenDisconnected - timeStartWhenConnected) > 10) {

                    Log.e(mDeviceAddress + " + " + mDeviceName, mFileName);
                    String senderDeviceAddress = "";
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        try {
                            Field mServiceField = bluetoothAdapter.getClass().getDeclaredField("mService");
                            mServiceField.setAccessible(true);

                            Object btManagerService = mServiceField.get(bluetoothAdapter);

                            if (btManagerService != null) {
                                Log.e(TAG, "Difficult MAC address access");
                                senderDeviceAddress = (String) btManagerService.getClass().getMethod("getAddress").invoke(btManagerService);

                            } else {
                                Log.e(TAG, "Bluetooth manager is null");
                            }
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                            //throw new RuntimeException(e);
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                            //throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            //throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                            //throw new RuntimeException(e);
                        }
                    } else {
                        Log.e(TAG, "Easily accessible MAC address");
                        try {
                            senderDeviceAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("senderBTMAC", senderDeviceAddress);
                        obj.put("receiverBTMAC", mDeviceAddress);
                        obj.put("filename", mFileName);
                        obj.put("appName", context.getString(R.string.app_name_for_topup_server));
//                        obj.put("clientEpoch", clientEpochTime );

                        for (int i = 0; i < obj.names().length(); i++) {
                            Log.e(TAG, "key = " + obj.names().getString(i) + " value = " + obj.get(obj.names().getString(i)));
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

//                    SharedPreferences sharedPref = context.getSharedPreferences(
//                            context.getString(R.string.first_file_transfer), Context.MODE_PRIVATE);
//                    SharedPreferences.Editor editor = sharedPref.edit();
//                    editor.putBoolean(context.getString(R.string.first_file_transfer), true);
//                    editor.apply();

                    UiUtils.promptForPhoneNumber(context, false, new Worker(context, obj));

                    mDeviceName = "";
                    mDeviceAddress = "";

                }

            }
        }
    };

    private static class Worker implements PhoneRetrieveListener {

        WeakReference<Context> ref;
        JSONObject body;

        public Worker(Context ref, JSONObject body) {
            this.ref = new WeakReference<>(ref);
            this.body = body;
        }

        @Override
        public void onPhoneRetrieved(String phone, int carrierCode) {
            Log.e(TAG, "Posting to server");

            Context ctx = ref.get();
            String url = ctx.getString(R.string.development_cloud_endpoint_bultoo);
            JSONObject installationObj;

            SneakernetCommon mainApplication = (SneakernetCommon) ctx.getApplicationContext();

            SharedPreferences sharedPref = ctx.getSharedPreferences(
                    ctx.getString(R.string.first_file_transfer), Context.MODE_PRIVATE);
            boolean firstFileTransfer = sharedPref.getBoolean(ctx.getString(R.string.first_file_transfer),
                    true);

            if (ctx == null) {
                return;
            }
            try {
                body.put("phoneNumber", phone);
                body.put("carrierCode", carrierCode);
            } catch (JSONException e) {
                throw new AssertionError("JSON Error");
            }

            if (firstFileTransfer) {
                Log.e(TAG, "Gets installation top up");
                String installationUrl = ctx.getString(R.string.development_cloud_endpoint_bultoo_installation);
                installationObj = new JSONObject();

                try {
                    installationObj.put("type", "requestInstallationCredits");
                    installationObj.put("phoneNumber", body.getString("phoneNumber"));
                    installationObj.put("carrierCode", body.getString("carrierCode"));
                    installationObj.put("receiverBTMAC", body.getString("senderBTMAC"));

                    NetworkRequest<JSONObject> request = new RetryingNetworkRequest<>(mainApplication, installationUrl,
                            Request.Method.POST, installationObj, DefaultListeners.emptyObjectListener);
                    mainApplication.getNetworkRequestor().issueJsonObjectRequest(ctx, request);

                } catch (JSONException e) {
                    throw new AssertionError("JSON Error");
                }

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(ctx.getString(R.string.first_file_transfer), false);
                editor.apply();
            }

            try {

                for(int i = 0; i<body.names().length(); i++){
                    Log.e(TAG, "key = " + body.names().getString(i) + " value = " + body.get(body.names().getString(i)));
                }
            } catch (JSONException e){
                throw new RuntimeException(e);
            }



            NetworkRequest<JSONObject> request = new RetryingNetworkRequest<>(mainApplication, url,
                    Request.Method.POST, body, DefaultListeners.emptyObjectListener);
            mainApplication.getNetworkRequestor().issueJsonObjectRequest(ctx, request);

            String url2="http://flask-aws-dev.ap-south-1.elasticbeanstalk.com/swaratoken";
            NetworkRequest<JSONObject> request2 = new RetryingNetworkRequest<>(mainApplication, url2,
                    Request.Method.POST, body, DefaultListeners.emptyObjectListener);
            mainApplication.getNetworkRequestor().issueJsonObjectRequest(ctx, request2);


            Log.e(TAG,"all requests sent");
        }
    }
}
