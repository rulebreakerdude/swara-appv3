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

package org.cgnet.swara2.utils;

import org.cgnet.swara2.MainApplication;
import org.cgnet.swara2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LongSparseArray;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.msr.sneakernetcommon.utils.CancelDialogListener;
import com.msr.sneakernetcommon.utils.PhoneRetrieveListener;

import java.lang.ref.WeakReference;

import cgnet.swara.activity.MainActivity;

import static com.msr.sneakernetcommon.R.string.phone_input;
import static com.msr.sneakernetcommon.R.string.phone_prompt_message;

public class UiUtils {

    static private LongSparseArray<Bitmap> sFaviconCache = new LongSparseArray<Bitmap>();

    static public void setPreferenceTheme(Activity a) {
        if (!PrefUtils.getBoolean(PrefUtils.LIGHT_THEME, true)) {
            a.setTheme(R.style.Theme_Dark);
        }
    }

    static public int dpToPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, MainApplication.getContext().getResources().getDisplayMetrics());
    }

    static public Bitmap getFaviconBitmap(long feedId, Cursor cursor, int iconCursorPos) {
        Bitmap bitmap = UiUtils.sFaviconCache.get(feedId);
        if (bitmap == null) {
            byte[] iconBytes = cursor.getBlob(iconCursorPos);
            if (iconBytes != null && iconBytes.length > 0) {
                bitmap = UiUtils.getScaledBitmap(iconBytes, 18);
                UiUtils.sFaviconCache.put(feedId, bitmap);
            }
        }
        return bitmap;
    }

    static public Bitmap getScaledBitmap(byte[] iconBytes, int sizeInDp) {
        if (iconBytes != null && iconBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
            if (bitmap != null && bitmap.getWidth() != 0 && bitmap.getHeight() != 0) {
                int bitmapSizeInDip = UiUtils.dpToPixel(sizeInDp);
                if (bitmap.getHeight() != bitmapSizeInDip) {
                    Bitmap tmp = bitmap;
                    bitmap = Bitmap.createScaledBitmap(tmp, bitmapSizeInDip, bitmapSizeInDip, false);
                    tmp.recycle();
                }

                return bitmap;
            }
        }

        return null;
    }

    private static void promptForCarrier(Context ctx, final SharedPreferences.Editor editor,
                                         final String phone, final PhoneRetrieveListener callback) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(ctx);
        builderSingle.setTitle(ctx.getString(R.string.carrier_prompt_title_hindi));

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(ctx,
                android.R.layout.select_dialog_singlechoice);
        for (String key : com.msr.sneakernetcommon.Constants.carrierNameToCode.keySet()) {
            arrayAdapter.add(key);
        }

        final WeakReference<Context> ref = new WeakReference<>(ctx);

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Context ctx = ref.get();
                if (ctx == null) {
                    return;
                }
                String carrierName = arrayAdapter.getItem(which);
                Integer carrierCode = com.msr.sneakernetcommon.Constants.carrierNameToCode.get(carrierName);
                editor.putInt(ctx.getString(com.msr.sneakernetcommon.R.string.carrier_code), carrierCode);
                editor.putString(ctx.getString(R.string.carrier_name), carrierName);
                editor.commit();

                if (carrierCode == 26) {
                    AlertDialog warningDialog = new AlertDialog.Builder(ctx)
                            .setTitle(R.string.warning_jio)
                            .setMessage(R.string.top_up_jio)
                            .setPositiveButton(ctx.getString(R.string.ok_phone), new CancelDialogListener())
                            .show();
                }

                if (callback != null) {
                    callback.onPhoneRetrieved(phone, carrierCode);
                }
            }
        });
        builderSingle.show();
    }

    private static void promptForPhone(Context ctx, final SharedPreferences.Editor editor, final int toBeShared,
                                       final boolean isApk, final PhoneRetrieveListener callback) {
        final EditText txtUrl = new EditText(ctx);
        txtUrl.setSingleLine();

        final WeakReference<Context> ref = new WeakReference<>(ctx);

        String alertMessage = isApk ? ctx.getString(R.string.app_share_success) : ctx.getString(R.string.bultoo_share_alert);//, toBeShared);

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle(ctx.getString(R.string.phone_prompt_title_hindi))
                .setMessage(alertMessage + "\n" + ctx.getString(R.string.phone_prompt_message_hindi))
                .setView(txtUrl)
                .setNegativeButton(ctx.getString(R.string.cancel_phone), new CancelDialogListener())
                .setPositiveButton(ctx.getString(R.string.ok_phone),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Context ctx = ref.get();
                                if (ctx == null) {
                                    return;
                                }
                                String phone = txtUrl.getText().toString().trim();
                                if (phone.isEmpty()) {
                                    Toast.makeText(ctx, ctx.getString(R.string.nonempty_hindi),
                                            Toast.LENGTH_SHORT).show();
                                    // Prompt again!
                                    dialog.dismiss();
                                    promptForPhone(ctx, editor, toBeShared, isApk, callback);
                                    return;
                                }
                                phone = phone.replaceAll("[^0-9]", "");
                                if (phone.length() != 10) {
                                    Toast.makeText(ctx, ctx.getString(R.string.char_length_hindi),
                                            Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    // Prompt again!
                                    promptForPhone(ctx, editor, toBeShared, isApk, callback);
                                    return;
                                }
                                // Save before calling the callback.
                                editor.putString(ctx.getString(phone_input), phone);
                                editor.commit();
                                dialog.dismiss();
                                // Now prompt for the carrier
                                promptForCarrier(ctx, editor, phone, callback);
                            }
                        })
                .show();
    }
    public static void promptForPhoneNumber(Context ctx, boolean isApk, PhoneRetrieveListener callback) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(com.msr.sneakernetcommon.R.string.shared_prefs), Context.MODE_PRIVATE);
        String phone = sharedPref.getString(ctx.getString(phone_input), "");
        int carrierCode = sharedPref.getInt(ctx.getString(com.msr.sneakernetcommon.R.string.carrier_code), -1);

        //int bultooCounter = sharedPref.getInt(ctx.getString(R.string.bultoo_counter), 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        //editor.putInt(ctx.getString(R.string.bultoo_counter), ++bultooCounter%10);


        String alertMessage;
        int toBeShared = 0;
//        if (!isApk) {
//            if (bultooCounter % 10 == 0) {
//                toBeShared = 10;
//                alertMessage = ctx.getString(R.string.bultoo_sent_10_hindi, toBeShared);
//            } else {
//                toBeShared = 10 - (bultooCounter % 10);
//                alertMessage = ctx.getString(R.string.bultoo_share_counter_message_hindi, toBeShared);
//            }
//        } else {
//            alertMessage = ctx.getString(R.string.app_share_success);
//        }
        alertMessage = ctx.getString(R.string.bultoo_share_alert);

        if (phone != "") {
            if (carrierCode != -1) {
                if (callback!=null) {
                    AlertDialog dialog = new AlertDialog.Builder(ctx)
                            .setTitle(ctx.getString(R.string.bultoo_success_hindi))
                            .setMessage(alertMessage)
                            .setPositiveButton(ctx.getString(R.string.ok_phone), new CancelDialogListener())
                            .show();
                    editor.apply();
                    callback.onPhoneRetrieved(phone, carrierCode);
                }
            } else {
                promptForCarrier(ctx, editor, phone, callback);
            }
            return;
        }

        promptForPhone(ctx, editor, toBeShared, isApk, callback);
    }
}
