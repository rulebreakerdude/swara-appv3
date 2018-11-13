/**
 * FeedEx
 *
 * Copyright (c) 2012-2013 Frederic Julian
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 *     Permission is hereby granted, free of charge, to any person obtaining a copy
 *     of this software and associated documentation files (the "Software"), to deal
 *     in the Software without restriction, including without limitation the rights
 *     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *     furnished to do so, subject to the following conditions:
 *
 *     The above copyright notice and this permission notice shall be included in
 *     all copies or substantial portions of the Software.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *     THE SOFTWARE.
 */

package org.cgnet.swara2.adapter;

import java.io.File;
import java.util.Vector;

import org.cgnet.swara2.BuildConfig;
import org.cgnet.swara2.Constants;
import org.cgnet.swara2.MainApplication;
import org.cgnet.swara2.R;
import org.cgnet.swara2.provider.FeedData;
import org.cgnet.swara2.provider.FeedData.EntryColumns;
import org.cgnet.swara2.provider.FeedData.FeedColumns;
import org.cgnet.swara2.utils.StringUtils;
import org.cgnet.swara2.utils.UiUtils;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import static org.cgnet.swara2.Constants.ACTION_BULTOO_FILE_SELECTED;


public class EntriesCursorAdapter extends ResourceCursorAdapter {

    private static String TAG = "EntriesCursorAdapter";

    private static class ViewHolder {
        public TextView titleTextView;
        public TextView dateTextView;
        public ImageView starImgView;
        public ImageView shareButton;
        public CheckBox isReadCb;
        public ImageView rupeeView;
        String guid;
    }
    private Context context;
    private int mTitlePos, mDatePos, mIsReadPos, mFavoritePos, mBultooPos, mIdPos, mFeedIdPos, mFeedIconPos, mFeedNamePos, mGuid;

    private final Uri mUri;
    private final boolean mShowFeedInfo;

    private final Vector<Long> mMarkedAsReadEntries = new Vector<Long>();
    private final Vector<Long> mMarkedAsUnreadEntries = new Vector<Long>();
    private final Vector<Long> mFavoriteEntries = new Vector<Long>();
    private final Vector<Long> mBultooEntries = new Vector<Long>();
    private final Vector<Long> mNotFavoriteEntries = new Vector<Long>();


    public EntriesCursorAdapter(Context context, Uri uri, Cursor cursor, boolean showFeedInfo) {
        super(context, R.layout.item_entry_list, cursor, 0);
        mUri = uri;
        mShowFeedInfo = showFeedInfo;
        this.context = context;
        reinit(cursor);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        if (view.getTag() == null) {
            ViewHolder holder = new ViewHolder();
            holder.titleTextView = (TextView) view.findViewById(android.R.id.text1);
            holder.dateTextView = (TextView) view.findViewById(android.R.id.text2);
            holder.starImgView = (ImageView) view.findViewById(android.R.id.icon);
            holder.shareButton = (ImageView) view.findViewById(android.R.id.icon2);
            holder.isReadCb = (CheckBox) view.findViewById(android.R.id.checkbox);
            holder.rupeeView = (ImageView) view.findViewById(android.R.id.icon1);
            view.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) view.getTag();

        final String titleText = cursor.getString(mTitlePos);
        holder.titleTextView.setText(titleText);

        final long id = cursor.getLong(mIdPos);
        holder.guid = cursor.getString(mGuid);
        final boolean favorite = !mNotFavoriteEntries.contains(id) && (cursor.getInt(mFavoritePos) == 1 || mFavoriteEntries.contains(id));
        final boolean isBultoo =  cursor.getInt(mBultooPos) == 1 || mBultooEntries.contains(id);

        holder.shareButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String audioFileName = holder.guid.substring(holder.guid.length()-6)+".mp3";
                        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                        path += "/CGNet_Swara/"+audioFileName;
                        Log.i("AUDIO FILE", path);
                        File audioFile = new File(path);

                        if (audioFile.exists()){

                            if (1==1 || titleText.toLowerCase().contains("bultoo")) {
                                //Broadcast an intent to denote that a file has been chosen to transfer over bluetooth
                                Intent fileIntent = new Intent();
                                fileIntent.setAction(ACTION_BULTOO_FILE_SELECTED);
                                fileIntent.putExtra("FILE_NAME", path.substring(path.length() - 10));
                                context.sendBroadcast(fileIntent);
                                Log.i("AUDIO FILE", path.substring(path.length() - 10));
                            }

                            Log.i("AUDIO FILE", "file exists");
                            context.grantUriPermission("com.android.bluetooth", Uri.fromFile(audioFile),
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);


                            Uri shareUri;
                            if (Build.VERSION.SDK_INT >= 24) {
                                shareUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", audioFile);
                            }
                            else {
                                shareUri = Uri.fromFile(audioFile);
                            }

                            //Following intent enables the selection of device as well as transfer of file over bluetooth
                            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                            sharingIntent.setType("audio/mpeg");
                            sharingIntent.setClassName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity");
                            sharingIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
                            context.startActivity(sharingIntent);
                        }
                        else {//
                            //Dialog appears if the file was not downloaded
                            new AlertDialog.Builder(context)
                                    .setTitle(R.string.download_alert_before_share)
                                    .setPositiveButton(R.string.ok_phone,new DialogInterface.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {}
                                    })
                                    .show();
                        }
                    }
                }
        );

        holder.starImgView.setImageResource(favorite ? R.drawable.dimmed_rating_important : R.drawable.dimmed_rating_not_important);
        holder.starImgView.setTag(favorite ? Constants.TRUE : Constants.FALSE);
        holder.starImgView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean newFavorite = !Constants.TRUE.equals(view.getTag());

                if (newFavorite) {
                    view.setTag(Constants.TRUE);
                    holder.starImgView.setImageResource(R.drawable.dimmed_rating_important);
                    mFavoriteEntries.add(id);
                    mNotFavoriteEntries.remove(id);
                } else {
                    view.setTag(Constants.FALSE);
                    holder.starImgView.setImageResource(R.drawable.dimmed_rating_not_important);
                    mNotFavoriteEntries.add(id);
                    mFavoriteEntries.remove(id);
                }

                ContentValues values = new ContentValues();
                values.put(EntryColumns.IS_FAVORITE, newFavorite ? 1 : 0);

                ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = ContentUris.withAppendedId(mUri, id);
                cr.update(entryUri, values, null, null);
            }
        });

        holder.shareButton.setImageResource(isBultoo ? R.drawable.share_money3 : R.drawable.share_black);

        if (mShowFeedInfo && mFeedIconPos > -1) {
            final long feedId = cursor.getLong(mFeedIdPos);
            Bitmap bitmap = UiUtils.getFaviconBitmap(feedId, cursor, mFeedIconPos);

            if (bitmap != null) {
                holder.dateTextView.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(context.getResources(), bitmap), null, null, null);
            } else {
                holder.dateTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            }
        }

        if (mShowFeedInfo && mFeedNamePos > -1) {
            String feedName = cursor.getString(mFeedNamePos);
            if (feedName != null) {
                holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)));
            } else {
                holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)));
            }
        } else {
            holder.dateTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)));
        }

        holder.isReadCb.setOnCheckedChangeListener(null);
        if (mMarkedAsUnreadEntries.contains(id) || (cursor.isNull(mIsReadPos) && !mMarkedAsReadEntries.contains(id))) {
            holder.titleTextView.setEnabled(true);
            holder.dateTextView.setEnabled(true);
            holder.isReadCb.setChecked(false);
        } else {
            holder.titleTextView.setEnabled(true); // items are now never "read"
            holder.dateTextView.setEnabled(true);
            holder.isReadCb.setChecked(false);
        }

        holder.isReadCb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    markAsRead(id);
                    holder.titleTextView.setEnabled(false);
                    holder.dateTextView.setEnabled(false);
                } else {
                    markAsUnread(id);
                    holder.titleTextView.setEnabled(true);
                    holder.dateTextView.setEnabled(true);
                }
            }
        });
    }

    public void markAllAsRead(final long untilDate) {
        mMarkedAsReadEntries.clear();
        mMarkedAsUnreadEntries.clear();

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                String where = EntryColumns.WHERE_UNREAD + Constants.DB_AND + '(' + EntryColumns.FETCH_DATE + Constants.DB_IS_NULL + Constants.DB_OR + EntryColumns.FETCH_DATE + "<=" + untilDate + ')';
                cr.update(mUri, FeedData.getReadContentValues(), where, null);
            }
        }.start();
    }

    private void markAsRead(final long id) {
        mMarkedAsReadEntries.add(id);
        mMarkedAsUnreadEntries.remove(id);

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = ContentUris.withAppendedId(mUri, id);
                cr.update(entryUri, FeedData.getReadContentValues(), null, null);
            }
        }.start();
    }

    private void markAsUnread(final long id) {
        mMarkedAsUnreadEntries.add(id);
        mMarkedAsReadEntries.remove(id);

        new Thread() {
            @Override
            public void run() {
                ContentResolver cr = MainApplication.getContext().getContentResolver();
                Uri entryUri = ContentUris.withAppendedId(mUri, id);
                cr.update(entryUri, FeedData.getUnreadContentValues(), null, null);
            }
        }.start();
    }

    @Override
    public void changeCursor(Cursor cursor) {
        reinit(cursor);
        super.changeCursor(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        reinit(newCursor);
        return super.swapCursor(newCursor);
    }

    @Override
    public void notifyDataSetChanged() {
        reinit(null);
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        reinit(null);
        super.notifyDataSetInvalidated();
    }

    private void reinit(Cursor cursor) {
        mMarkedAsReadEntries.clear();
        mMarkedAsUnreadEntries.clear();
        mFavoriteEntries.clear();
        mBultooEntries.clear();
        mNotFavoriteEntries.clear();

        if (cursor != null) {
            mTitlePos = cursor.getColumnIndex(EntryColumns.TITLE);
            mDatePos = cursor.getColumnIndex(EntryColumns.DATE);
            mIsReadPos = cursor.getColumnIndex(EntryColumns.IS_READ);
            mFavoritePos = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
            mBultooPos = cursor.getColumnIndex(EntryColumns.IS_BULTOO);
            mIdPos = cursor.getColumnIndex(EntryColumns._ID);
            mGuid = cursor.getColumnIndex(EntryColumns.GUID);
            if (mShowFeedInfo) {
                mFeedIdPos = cursor.getColumnIndex(EntryColumns.FEED_ID);
                mFeedIconPos = cursor.getColumnIndex(FeedColumns.ICON);
                mFeedNamePos = cursor.getColumnIndex(FeedColumns.NAME);
            }
        }
    }
}
