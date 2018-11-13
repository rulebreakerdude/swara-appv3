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

package org.cgnet.swara2.fragment;

import java.io.File;

import org.cgnet.swara2.BuildConfig;
import org.cgnet.swara2.Constants;
import org.cgnet.swara2.MainApplication;
import org.cgnet.swara2.MainApplication.TrackerName;
import org.cgnet.swara2.R;
import org.cgnet.swara2.activity.BaseActivity;
import org.cgnet.swara2.provider.FeedData;
import org.cgnet.swara2.provider.FeedData.EntryColumns;
import org.cgnet.swara2.provider.FeedData.FeedColumns;
import org.cgnet.swara2.receiver.ChooserReceiver;
import org.cgnet.swara2.service.FetcherService;
import org.cgnet.swara2.utils.PrefUtils;
import org.cgnet.swara2.utils.UiUtils;
import org.cgnet.swara2.view.EntryView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import static org.cgnet.swara2.Constants.ACTION_BULTOO_FILE_SELECTED;
import static org.cgnet.swara2.Constants.ACTION_FILE_SELECTED;

public class EntryFragment extends SwipeRefreshFragment implements BaseActivity.OnFullScreenListener, LoaderManager.LoaderCallbacks<Cursor>, EntryView.OnActionListener {

    private static final String STATE_BASE_URI = "STATE_BASE_URI";
    private static final String STATE_CURRENT_PAGER_POS = "STATE_CURRENT_PAGER_POS";
    private static final String STATE_ENTRIES_IDS = "STATE_ENTRIES_IDS";
    private static final String STATE_INITIAL_ENTRY_ID = "STATE_INITIAL_ENTRY_ID";

    private int mTitlePos = -1, mDatePos, mMobilizedHtmlPos, mAbstractPos, mLinkPos, mIsFavoritePos, mIsBultooPos, mIsReadPos, mEnclosurePos, mAuthorPos, mFeedNamePos, mFeedUrlPos, mFeedIconPos;

    private int mCurrentPagerPos = -1;
    private Uri mBaseUri;
    private long mInitialEntryId = -1;
    private long[] mEntriesIds;

    private boolean mFavorite, mPreferFullText = true;

    private ViewPager mEntryPager;
    private EntryPagerAdapter mEntryPagerAdapter;

    private View mCancelFullscreenBtn;

    private class EntryPagerAdapter extends PagerAdapter {

        private final SparseArray<EntryView> mEntryViews = new SparseArray<EntryView>();

        public EntryPagerAdapter() {
        }

        @Override
        public int getCount() {
            return mEntriesIds != null ? mEntriesIds.length : 0;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            EntryView view = new EntryView(getActivity());
            mEntryViews.put(position, view);
            container.addView(view);
            view.setListener(EntryFragment.this);
            getLoaderManager().restartLoader(position, null, EntryFragment.this);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            getLoaderManager().destroyLoader(position);
            container.removeView((View) object);
            mEntryViews.delete(position);
        }	

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        public void displayEntry(int pagerPos, Cursor newCursor, boolean forceUpdate) {  
        	
            EntryView view = mEntryViews.get(pagerPos);
            if (view != null) {
                if (newCursor == null) {
                    newCursor = (Cursor) view.getTag(); // get the old one
                }

                if (newCursor != null && newCursor.moveToFirst()) {
                    String contentText = newCursor.getString(mMobilizedHtmlPos);
                    if (contentText == null || (forceUpdate && !mPreferFullText)) {
                        mPreferFullText = false;
                        contentText = newCursor.getString(mAbstractPos);
                        
                    } else {
                        mPreferFullText = true;	
                    }
                    if (contentText == null) {
                        contentText = "";
                    } 
                     
                    String author = newCursor.getString(mAuthorPos);
                    long timestamp = newCursor.getLong(mDatePos);
                    String link = newCursor.getString(mLinkPos);
                    String title = newCursor.getString(mTitlePos);
                    String enclosure = newCursor.getString(mEnclosurePos);
                    
                    
                    t.setScreenName(title);
                	
                    
                    view.setHtml(mEntriesIds[pagerPos], title, link, contentText, enclosure, author, timestamp, mPreferFullText);
                    view.setTag(newCursor);

                    if (pagerPos == mCurrentPagerPos) {
                        refreshUI(newCursor);
                    }
                }
            }
        }

        public Cursor getCursor(int pagerPos) {
            EntryView view = mEntryViews.get(pagerPos);
            if (view != null) {
                return (Cursor) view.getTag();
            }
            return null;
        }

        public void setUpdatedCursor(int pagerPos, Cursor newCursor) {
            EntryView view = mEntryViews.get(pagerPos);
            if (view != null) {
                Cursor previousUpdatedOne = (Cursor) view.getTag(R.id.updated_cursor);
                if (previousUpdatedOne != null) {
                    previousUpdatedOne.close();
                }
                view.setTag(newCursor);
                view.setTag(R.id.updated_cursor, newCursor);
            }
        }

        public void onResume() {
            if (mEntriesIds != null) {
                EntryView view = mEntryViews.get(mCurrentPagerPos);
                if (view != null) {
                    view.onResume();
                }
            }
        }

        public void onPause() {
            for (int i = 0; i < mEntryViews.size(); i++) {
                mEntryViews.valueAt(i).onPause();
            }
        }
    }
    Tracker t;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        t =  ((MainApplication) this.getActivity().getApplication()).getTracker(TrackerName.APP_TRACKER);
        
        
        mEntryPagerAdapter = new EntryPagerAdapter();

//        mFilter = new IntentFilter();
//        mFilter.addAction("android.intent.action.CHOOSER");
//        getActivity().registerReceiver(BootCompletedBroadcastReceiver, mFilter);

        super.onCreate(savedInstanceState);
    }

    @Override
    public View inflateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_entry, container, true);

        mCancelFullscreenBtn = rootView.findViewById(R.id.cancelFullscreenBtn);
        mCancelFullscreenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFullScreen();
            }
        });

        mEntryPager = (ViewPager) rootView.findViewById(R.id.pager);
        //mEntryPager.setPageTransformer(true, new DepthPageTransformer());
        mEntryPager.setAdapter(mEntryPagerAdapter);

        if (savedInstanceState != null) {
            mBaseUri = savedInstanceState.getParcelable(STATE_BASE_URI);
            mEntriesIds = savedInstanceState.getLongArray(STATE_ENTRIES_IDS);
            mInitialEntryId = savedInstanceState.getLong(STATE_INITIAL_ENTRY_ID);
            mCurrentPagerPos = savedInstanceState.getInt(STATE_CURRENT_PAGER_POS);
            mEntryPager.getAdapter().notifyDataSetChanged();
            mEntryPager.setCurrentItem(mCurrentPagerPos);
        }

        mEntryPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int i) {
                mCurrentPagerPos = i;
                mEntryPagerAdapter.onPause(); // pause all webviews
                mEntryPagerAdapter.onResume(); // resume the current webview

                refreshUI(mEntryPagerAdapter.getCursor(i));
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        disableSwipe();

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_BASE_URI, mBaseUri);
        outState.putLongArray(STATE_ENTRIES_IDS, mEntriesIds);
        outState.putLong(STATE_INITIAL_ENTRY_ID, mInitialEntryId);
        outState.putInt(STATE_CURRENT_PAGER_POS, mCurrentPagerPos);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        ((BaseActivity) activity).setOnFullscreenListener(this);
    }

    @Override
    public void onDetach() {
        ((BaseActivity) getActivity()).setOnFullscreenListener(null);

        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        mEntryPagerAdapter.onResume();

        if (((BaseActivity) getActivity()).isFullScreen()) {
            mCancelFullscreenBtn.setVisibility(View.VISIBLE);
        } else {
            mCancelFullscreenBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mEntryPagerAdapter.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.entry, menu);

        if (mFavorite) {
            MenuItem item = menu.findItem(R.id.menu_star);
            item.setTitle(R.string.menu_unstar).setIcon(R.drawable.rating_important);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mEntriesIds != null) {

            switch (item.getItemId()) {
                case R.id.menu_star: {
                    mFavorite = !mFavorite;

                    if (mFavorite) {
                        item.setTitle(R.string.menu_unstar).setIcon(R.drawable.rating_important);
                    } else {
                        item.setTitle(R.string.menu_star).setIcon(R.drawable.rating_not_important);
                    }

                    final Uri uri = ContentUris.withAppendedId(mBaseUri, mEntriesIds[mCurrentPagerPos]);
                    new Thread() {
                        @Override
                        public void run() {
                            ContentValues values = new ContentValues();
                            values.put(EntryColumns.IS_FAVORITE, mFavorite ? 1 : 0);
                            ContentResolver cr = MainApplication.getContext().getContentResolver();
                            cr.update(uri, values, null, null);

                            // Update the cursor
                            Cursor updatedCursor = cr.query(uri, null, null, null, null);
                            updatedCursor.moveToFirst();
                            mEntryPagerAdapter.setUpdatedCursor(mCurrentPagerPos, updatedCursor);
                        }
                    }.start();
                    break;
                }
                case R.id.menu_share: {
                    Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
                    String enclose = cursor.getString(mEnclosurePos);
                     
                    String temp = enclose.split("http://cgnetswara.org//audio/")[1];
                    String fileName = temp.split("\\[")[0];
                     

                    // Creates a folder for the app's recordings
                    String path_audio = Environment.getExternalStorageDirectory().getAbsolutePath();
                 	path_audio += "/CGNet_Swara";
                  	
                 	String audio_recording = path_audio + "/" + fileName;
                 	File child = new File(audio_recording);
                      
                 	
                    if(child.exists()) { 
                        Log.e("enclose", "INSDIE" + audio_recording);

                        Intent fileIntent = new Intent();
                        fileIntent.setAction(ACTION_FILE_SELECTED);
                        fileIntent.putExtra("FILE_NAME", fileName);
                        getActivity().sendBroadcast(fileIntent);

                        if (cursor.getString(mTitlePos).toLowerCase().contains("bultoo")) {
                            //Broadcast an intent to denote that a file has been chosen to transfer over bluetooth
                            Intent bultooFileIntent = new Intent();
                            bultooFileIntent.setAction(ACTION_BULTOO_FILE_SELECTED);
                            bultooFileIntent.putExtra("FILE_NAME", fileName);
                            getActivity().sendBroadcast(bultooFileIntent);
                            Log.e("sHAREiNTENT broadcast", audio_recording);
                        }
                      
                        t.send(new HitBuilders.EventBuilder()
                        .setCategory("Audio file shared.")
                        .setLabel(fileName)
                        .build());

                        Uri shareUri;
                        if (Build.VERSION.SDK_INT >= 24) {
                            shareUri = FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID + ".provider", child);
                        }
                        else {
                            shareUri = Uri.fromFile(child);
                        }

                        Intent receiver = new Intent(getActivity(), ChooserReceiver.class);
//                        receiver.putExtra("test", "test");
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, receiver, PendingIntent.FLAG_UPDATE_CURRENT);

                        if (Build.VERSION.SDK_INT >= 22) {
                            startActivity(Intent.createChooser(
                                    new Intent(Intent.ACTION_SEND).setType("audio/*").putExtra(Intent.EXTRA_STREAM, shareUri),
                                             getString(R.string.menu_share), pendingIntent.getIntentSender()
                            ));
                        } else {
                            startActivity(Intent.createChooser(
                                    new Intent(Intent.ACTION_SEND).setType("audio/*").putExtra(Intent.EXTRA_STREAM, shareUri),
                                            getString(R.string.menu_share)
                            ));
                        }
                    }
                    break;
                }
 
                 
            }
        }

        return true;
    }

    public void setData(Uri uri) {
        mCurrentPagerPos = -1;

        mBaseUri = FeedData.EntryColumns.PARENT_URI(uri.getPath());
        try {
            mInitialEntryId = Long.parseLong(uri.getLastPathSegment());	
        } catch (Exception unused) {
            mInitialEntryId = -1;
        }

        if (mBaseUri != null) {
            Bundle b = getActivity().getIntent().getExtras();

            String whereClause = FeedData.shouldShowReadEntries(mBaseUri) ||
                    (b != null && b.getBoolean(Constants.INTENT_FROM_WIDGET, false)) ? null : EntryColumns.WHERE_UNREAD;
            String entriesOrder = PrefUtils.getBoolean(PrefUtils.DISPLAY_OLDEST_FIRST, false) ? Constants.DB_ASC : Constants.DB_DESC;

            // Load the entriesIds list. Should be in a loader... but I was too lazy to do so
            Cursor entriesCursor = MainApplication.getContext().getContentResolver().query(mBaseUri, EntryColumns.PROJECTION_ID,
                    whereClause, null, EntryColumns.DATE + entriesOrder);

            if (entriesCursor != null && entriesCursor.getCount() > 0) {
                mEntriesIds = new long[entriesCursor.getCount()];
                int i = 0;
                while (entriesCursor.moveToNext()) {
                    mEntriesIds[i] = entriesCursor.getLong(0);
                    if (mEntriesIds[i] == mInitialEntryId) {
                        mCurrentPagerPos = i; // To immediately display the good entry
                    }
                    i++;
                }

                entriesCursor.close();
            }
        } else {
            mEntriesIds = null;
        }

        mEntryPagerAdapter.notifyDataSetChanged();
        if (mCurrentPagerPos != -1) {
            mEntryPager.setCurrentItem(mCurrentPagerPos);
        }
    }
// TODO
    private void refreshUI(Cursor entryCursor) {
        if (entryCursor != null) {
            String feedTitle = entryCursor.isNull(mFeedNamePos) ? entryCursor.getString(mFeedUrlPos) : entryCursor.getString(mFeedNamePos);
            BaseActivity activity = (BaseActivity) getActivity();
            activity.setTitle(feedTitle);

            byte[] iconBytes = entryCursor.getBlob(mFeedIconPos);
            Bitmap bitmap = UiUtils.getScaledBitmap(iconBytes, 24);
            if (bitmap != null) {
                activity.getActionBar().setIcon(new BitmapDrawable(getResources(), bitmap));
            } else {
                activity.getActionBar().setIcon(R.drawable.icon);
            }

            mFavorite = entryCursor.getInt(mIsFavoritePos) == 1;
            activity.invalidateOptionsMenu();

            // Listen the mobilizing task
            if (FetcherService.hasMobilizationTask(mEntriesIds[mCurrentPagerPos])) {
                showSwipeProgress();
            } else {
                hideSwipeProgress();
            }

            // Mark the article as read
            if (entryCursor.getInt(mIsReadPos) != 1) {
                final Uri uri = ContentUris.withAppendedId(mBaseUri, mEntriesIds[mCurrentPagerPos]);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ContentResolver cr = MainApplication.getContext().getContentResolver();
                        cr.update(uri, FeedData.getReadContentValues(), null, null);

                        // Update the cursor
                        Cursor updatedCursor = cr.query(uri, null, null, null, null);
                        updatedCursor.moveToFirst();
                        mEntryPagerAdapter.setUpdatedCursor(mCurrentPagerPos, updatedCursor);
                    }
                }).start();
            }
        }
    }

    private void showEnclosure(Uri uri, String enclosure, int position1, int position2) { 
        try {
            startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, enclosure.substring(position1 + 3, position2)), 0);
        } catch (Exception e) {
            try {
                startActivityForResult(new Intent(Intent.ACTION_VIEW, uri), 0); // fallbackmode - let the browser handle this
            } catch (Throwable t) {
                Toast.makeText(getActivity(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void toggleFullScreen() {
        BaseActivity activity = (BaseActivity) getActivity();
        activity.toggleFullScreen();
    }

    @Override
    public void onClickOriginalText() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPreferFullText = false;
                mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
            }
        });
    }

    @Override
    public void onClickFullText() {
        final BaseActivity activity = (BaseActivity) getActivity();

        if (!isRefreshing()) {
            Cursor cursor = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
            final boolean alreadyMobilized = !cursor.isNull(mMobilizedHtmlPos);

            if (alreadyMobilized) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {  
                        mPreferFullText = true;
                        mEntryPagerAdapter.displayEntry(mCurrentPagerPos, null, true);
                    }
                });
            } else {
                ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

                // since we have acquired the networkInfo, we use it for basic checks
                if (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    FetcherService.addEntriesToMobilize(new long[]{mEntriesIds[mCurrentPagerPos]});
                    activity.startService(new Intent(activity, FetcherService.class).setAction(FetcherService.ACTION_MOBILIZE_FEEDS));
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showSwipeProgress();
                        }
                    });
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, R.string.network_error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
    }
// TODO
    @Override
    public void onClickEnclosure() {
        getActivity().runOnUiThread(new Runnable() {
        	//TODO
    		
        	@Override
            public void run() {
                final String enclosure = mEntryPagerAdapter.getCursor(mCurrentPagerPos).getString(mEnclosurePos);
                
                
                final int position1 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR);
                final int position2 = enclosure.indexOf(Constants.ENCLOSURE_SEPARATOR, position1 + 3);

                final Uri uri = Uri.parse(enclosure.substring(0, position1));
                final String filename = uri.getLastPathSegment();
                
                t.send(new HitBuilders.EventBuilder()
	   	        .setCategory("Button to download audio file was clicked on") 
	   	        .setAction(filename)
	   	        .build());
                
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.open_enclosure)
//                        .setMessage(getString(R.string.file) + ": " + filename)
                        .setNegativeButton(R.string.cancel_phone, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) { 
                            	// showEnclosure(uri, enclosure, position1, position2);
                            	t.send(new HitBuilders.EventBuilder()
	            	   	        .setCategory("Button to download audio file was clicked on") 
	            	   	        .setAction(filename)
	            	   	        .setValue(0)
	            	   	        .build());
                            }
                        }).setPositiveButton(R.string.download_and_save, new DialogInterface.OnClickListener() {
		                    @Override
		                    public void onClick(DialogInterface dialog, int which) {
		                        try { 
		                        	t.send(new HitBuilders.EventBuilder()
		            	   	        .setCategory("Button to download audio file was clicked on") 
		            	   	        .setAction(filename)
		            	   	        .setValue(1)
		            	   	        .build());
		                        	
		                        	DownloadManager.Request r = new DownloadManager.Request(uri);
		                            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
		                            r.allowScanningByMediaScanner();
		                            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		                            r.setVisibleInDownloadsUi(true);
		                             
		                            String name = new File(uri.toString()).getName(); 
		                             
		                    		String path = Environment.getExternalStorageDirectory().getAbsolutePath();
		                    		path += "/CGNet_Swara";
		                    		File dir = new File(path); 
		                    		if (!dir.exists()|| !dir.isDirectory()) {
		                    			dir.mkdirs();
		                    		}
  
		                            r.setDestinationInExternalPublicDir("CGNet_Swara", name); 
		                            
		                            DownloadManager dm = (DownloadManager) MainApplication.getContext().getSystemService(Context.DOWNLOAD_SERVICE);
		                            dm.enqueue(r);
		                             
		                             
		                            Cursor c = mEntryPagerAdapter.getCursor(mCurrentPagerPos);
		                             
		                            refreshUI(c);
		                            
		                            
		                            // TODO
		                        } catch (Exception e) {
		                            Log.e("DownloadError",e+"");
		                            Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_LONG).show();
		                        }
		                    }
                }).show();
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = new CursorLoader(getActivity(), EntryColumns.CONTENT_URI(mEntriesIds[id]), null, null, null, null);
        cursorLoader.setUpdateThrottle(1000);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    	 
        if (mBaseUri != null && cursor != null) { // can be null if we do a setData(null) before
            cursor.moveToFirst();

            if (mTitlePos == -1) {
                mTitlePos = cursor.getColumnIndex(EntryColumns.TITLE);
                mDatePos = cursor.getColumnIndex(EntryColumns.DATE);
                mAbstractPos = cursor.getColumnIndex(EntryColumns.ABSTRACT);
                mMobilizedHtmlPos = cursor.getColumnIndex(EntryColumns.MOBILIZED_HTML);
                mLinkPos = cursor.getColumnIndex(EntryColumns.LINK);
                mIsFavoritePos = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
                mIsBultooPos = cursor.getColumnIndex(EntryColumns.IS_BULTOO);
                mIsReadPos = cursor.getColumnIndex(EntryColumns.IS_READ);
                mEnclosurePos = cursor.getColumnIndex(EntryColumns.ENCLOSURE);
                mAuthorPos = cursor.getColumnIndex(EntryColumns.AUTHOR);
                mFeedNamePos = cursor.getColumnIndex(FeedColumns.NAME);
                mFeedUrlPos = cursor.getColumnIndex(FeedColumns.URL);
                mFeedIconPos = cursor.getColumnIndex(FeedColumns.ICON);
            }

            int position = loader.getId();
            if (position != -1) {
                mEntryPagerAdapter.displayEntry(position, cursor, false);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mEntryPagerAdapter.setUpdatedCursor(loader.getId(), null);
    }

    @Override
    public void onFullScreenEnabled(boolean isImmersive) {
        if (!isImmersive) {
            mCancelFullscreenBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onFullScreenDisabled() {
        mCancelFullscreenBtn.setVisibility(View.GONE);
    }

    @Override
    public void onRefresh() {
        // Nothing to do
    }

}

