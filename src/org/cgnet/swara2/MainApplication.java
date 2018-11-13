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

package org.cgnet.swara2;

import java.util.HashMap;

import org.cgnet.swara2.utils.PrefUtils;
import org.json.JSONObject;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.Request;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.msr.sneakernetcommon.SneakernetCommon;
import com.msr.sneakernetcommon.network.DefaultListeners;
import com.msr.sneakernetcommon.network.NetworkRequest;
import com.msr.sneakernetcommon.network.RetryingNetworkRequest;
import com.msr.sneakernetcommon.sqlite.DatabaseConnection;
import com.msr.sneakernetcommon.sqlite.FailedVolleyRequestsDbHelper;
import com.msr.sneakernetcommon.sqlite.FeedsDbHelper;
import com.msr.sneakernetcommon.sqlite.FileMD5SumDBHelper;
import com.msr.sneakernetcommon.sqlite.MatchInfoCacheDbHelper;
import com.msr.sneakernetcommon.sqlite.SponsoredSignaturesDbHelper;
import com.msr.sneakernetcommon.sqlite.SyncedSponsoredDbHelper;
import com.msr.sneakernetcommon.transfer.protocols.ConnectorTransferProtocol;

import cgnet.swara.activity.MainActivity;

// Update
public class MainApplication extends SneakernetCommon {
	HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

	private static Context context;

	private final static String TAG = "MainApplication";

	public DatabaseConnection databaseConnection;

	FailedVolleyRequestsDbHelper failedVolleyDb;
	
	public MainApplication() {
		super(); 
	}
	 
	public enum TrackerName {
		APP_TRACKER, 		// Tracker used only in this app.
		GLOBAL_TRACKER, 	// Tracker used by all the apps from a company. eg: roll-up tracking.
		ECOMMERCE_TRACKER, 	// Tracker used by all ecommerce transactions from a company.
	}
	
	public synchronized Tracker getTracker(TrackerName appTracker) {
		if (!mTrackers.containsKey(appTracker)) {
			GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);

			if (appTracker == TrackerName.APP_TRACKER) {
				Log.e(TAG, "INSIDE TRACKER");
//				Tracker t = analytics.newTracker(R.xml.analytics);
				Tracker t = analytics.newTracker(R.xml.global_tracker);
			    mTrackers.put(appTracker, t);
            } else {
				Log.e(TAG, appTracker+"");
			}
		}  
		return mTrackers.get(appTracker);
	}

	public static Context getContext() {
		return context;
	}

	public String getDatabaseName() {
		return DatabaseConnection.DATABASE_NAME;
	}

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false); // init

//		databaseConnection = new DatabaseConnection(getApplicationContext(), getDatabaseName());
//		databaseConnection.registerDatabaseTable(new FileMD5SumDBHelper());
//		databaseConnection.registerDatabaseTable(new SponsoredSignaturesDbHelper());
//		databaseConnection.registerDatabaseTable(new MatchInfoCacheDbHelper());
//		failedVolleyDb = new FailedVolleyRequestsDbHelper();
//		databaseConnection.registerDatabaseTable(failedVolleyDb);
//		databaseConnection.registerDatabaseTable(new SyncedSponsoredDbHelper());
//		databaseConnection.registerDatabaseTable(new FeedsDbHelper());

    }


    @Override
    public int getTransparentIconID() {
        return 0;
    }

    @Override
    public int getGcmSenderID() {
        return 0;
    }

    @Override
    public ConnectorTransferProtocol getHubProtocol() {
        return null;
    }

    @Override
    public Class getHomeActivity() {
        return MainActivity.class;
    }

    @Override
    public Intent getReceivedFilesIntent() {
        return null;
    }

    @Override
	public void onTerminate() {
		databaseConnection.close();
		super.onTerminate();
	}
}
