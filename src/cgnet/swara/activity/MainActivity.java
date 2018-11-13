package cgnet.swara.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.cgnet.swara2.BuildConfig;
import org.cgnet.swara2.MainApplication;
import org.cgnet.swara2.MainApplication.TrackerName;
import org.cgnet.swara2.R;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import static com.msr.sneakernetcommon.R.string.phone_input;
import static org.cgnet.swara2.utils.UiUtils.promptForPhoneNumber;

/** This is the first screen of the CGNet Swara App. 
 *  It allows the user to either record a message (which is then sent to a central location) 
 *  or listen to recordings.
 *  @author Krittika D'Silva (krittika.dsilva@gmail.com) 
 */ 
public class MainActivity extends Activity {
	protected static final String TAG = "MainActivity";

	/** Opens an activity that allows a user to record and send a message. */
	private Button mRecordMessage;

	/** Opens an activity that allows a user to listen to recordings. */
	private Button mListenMessages;

	/** Opens an activity that allows the user to attach a photo, record a message, 
	 *  and send both. */
	private Button mIncludeAudio;

	/** The users' phone number. */	
	private String mPhoneNumber;

	/** The users' phone number. */
	private EditText mNumber;

	/** Opens an activity that allows to share the app over bluetooth */
	private Button mShareApp;

	private Button mSelectOperator;

	private SharedPreferences.OnSharedPreferenceChangeListener spChanged;

	private SharedPreferences mSharedPref;

//	/** Cash-in amount */
//	private TextView mCashInMessage;
//
//	/** Cash-in icon */
//	private ImageView mCashIn;

	private int MY_PERMISSIONS_REQUESTS = 0;

	private void addPermission(List<String> permissionsList, String permission) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
				permissionsList.add(permission);
			}
	}

	private void givePermissions() {
		final List<String> permissionsList = new ArrayList<String>();
		addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION);
		addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE);
		}
		addPermission(permissionsList, Manifest.permission.RECORD_AUDIO);
		addPermission(permissionsList, Manifest.permission.READ_PHONE_STATE);

		if (permissionsList.size() > 0) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
						MY_PERMISSIONS_REQUESTS);
			}
			return;
		}
	}
  	 
	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mRecordMessage = (Button) findViewById(R.id.one);
		mListenMessages = (Button) findViewById(R.id.two);
		mIncludeAudio = (Button) findViewById(R.id.photo);
		mNumber = (EditText) findViewById(R.id.phone);
		mNumber.clearFocus();
		mShareApp = (Button) findViewById(R.id.three);
		mSelectOperator = (Button) findViewById(R.id.selectOperator);
//		mCashInMessage = (TextView) findViewById(R.id.amount_to_cash_in_msg);
//		mCashIn = (ImageView) findViewById(R.id.cash_in);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			givePermissions();
		}

 		// Get Google Analytics tracker.
		Tracker t = ((MainApplication) getApplication()).getTracker(TrackerName.APP_TRACKER);
		   
        t.setScreenName("Home Screen");
        
        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());

		mSharedPref = getSharedPreferences(
				getString(com.msr.sneakernetcommon.R.string.shared_prefs), MODE_PRIVATE);

        spChanged = new
            SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                      String key) {

                    if (key.equals(getString(R.string.carrier_name))) {
                        String chooseOperator = sharedPreferences.getString(key, "");
                        if (chooseOperator.equals("")){
                            chooseOperator = getString(R.string.carrier_prompt_title_hindi);
                        }
                        mSelectOperator.setText(chooseOperator);
                    }
                }
            };
		mSharedPref.registerOnSharedPreferenceChangeListener(spChanged);

        String savedText = getPreferences(MODE_PRIVATE).getString("Phone", null);
		String carrier_name = mSharedPref.getString(getString(R.string.carrier_name), "");
		if(savedText != null && !savedText.equals("") && !carrier_name.equals("")) {
			mNumber.setText(savedText);

			SharedPreferences.Editor editor_phone = mSharedPref.edit();
			editor_phone.putString(getString(phone_input), savedText);
			editor_phone.apply();

			mSelectOperator.setText(carrier_name);

		} else {
			showPrompt();
		}

//		displayTopUpAmount();
		  
		mRecordMessage.setEnabled(true);
		mIncludeAudio.setEnabled(true);


		mRecordMessage.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View arg) {
				mRecordMessage.setEnabled(false);
				recordInput(false);
			}  
		}); 

		mIncludeAudio.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View arg) { 
				mIncludeAudio.setEnabled(false);
				recordInput(true);
			}  
		}); 

		mListenMessages.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg) {
				loadRecordings();
			}  
		});

		mShareApp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				shareApplication();
			}
		});

		mSelectOperator.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				SharedPreferences.Editor editor_phone = mSharedPref.edit();
				editor_phone.putInt(getString(com.msr.sneakernetcommon.R.string.carrier_code), -1);
                editor_phone.putString(getString(R.string.carrier_name), "");
				editor_phone.apply();

				promptForPhoneNumber(MainActivity.this, false, null);

			}
		});

//		mCashIn.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				TODO: Request the server to give the user an eligible top up amount.
//			}
//		});


		// Creates a folder for the app's recordings that have to be uploaded
		String path = Environment.getExternalStorageDirectory().getAbsolutePath();
		path += "/Android/data/com.MSRi.ivr.cgnetswara";
		File dir = new File(path); 
		if (!dir.exists()|| !dir.isDirectory()) {
			dir.mkdirs();
		}
 
		// Creates a folder for the app's recordings that have been downloaded
		String path_audio = Environment.getExternalStorageDirectory().getAbsolutePath();
		path_audio += "/CGNet_Swara";
		File dir_audio = new File(path_audio); 
		if (!dir_audio.exists()|| !dir_audio.isDirectory()) {
			dir_audio.mkdirs();
		}
		
		if(mNumber != null && mNumber.getText().toString() != null &&  mNumber.getText().toString().length() == 10) {
			mRecordMessage.setEnabled(true);
			mIncludeAudio.setEnabled(true);
			mListenMessages.setEnabled(true);
		} else { 
			mRecordMessage.setEnabled(false);
			mIncludeAudio.setEnabled(false);
			mListenMessages.setEnabled(false);
		}

		// Saves the users phone number
		mNumber.addTextChangedListener(new TextWatcher(){
			public void afterTextChanged(Editable s) {
				mPhoneNumber = s.toString(); 
				SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
				editor.putString("Phone", mPhoneNumber); 
				editor.apply();

				SharedPreferences.Editor editor_phone = mSharedPref.edit();
				editor_phone.putString(getString(phone_input), mPhoneNumber);
				editor_phone.apply();

				Log.e(TAG, ""+mPhoneNumber.length());
				if(mPhoneNumber.length() != 10) {
					mRecordMessage.setEnabled(false);
					mIncludeAudio.setEnabled(false);
					mListenMessages.setEnabled(false);
				} else {
					mRecordMessage.setEnabled(true);
					mIncludeAudio.setEnabled(true);
					mListenMessages.setEnabled(true);
				}
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){}
		}); 
		
		Intent intent = new Intent();   
		intent.setAction("com.android.CUSTOM_INTENT");
		sendBroadcast(intent);
	}



	/** Displays an alert dialog prompting
	 *  the user to input their phone number. */
	private void showPrompt() {
 		// get prompts.xml view
		LayoutInflater li = LayoutInflater.from(this);
		View promptsView = li.inflate(R.layout.phone_prompt, null);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		// set prompts.xml to alertdialog builder
		alertDialogBuilder.setView(promptsView);

		final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);

		// set dialog message
		alertDialogBuilder.setCancelable(false).setPositiveButton(this.getString(R.string.ok_phone),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) { 
				mPhoneNumber = userInput.getText().toString();
				SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
				editor.putString("Phone", mPhoneNumber);
				editor.apply();

				SharedPreferences sharedPref = getSharedPreferences(
						getString(com.msr.sneakernetcommon.R.string.shared_prefs), MODE_PRIVATE);
				SharedPreferences.Editor editor_phone = sharedPref.edit();
				editor_phone.putString(getString(phone_input), mPhoneNumber);
				editor_phone.apply();

				promptForPhoneNumber(MainActivity.this, false, null);

				mNumber.setText(mPhoneNumber);
				if(mPhoneNumber == null || mPhoneNumber.equals("") || mPhoneNumber.equals(" ")) { 
					showPrompt();
				}
			}
		})
		.setNegativeButton(this.getString(R.string.cancel_phone),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				mPhoneNumber = userInput.getText().toString();
				mNumber.setText(mPhoneNumber);
				dialog.cancel();
				onResume();
			}
		});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create(); 
		alertDialog.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		// show it
		alertDialog.show();
	} 

	/** Called when the activity is paused; begins playing the audio recording
	 *  for the user. */
	@Override
	public void onResume() {
		super.onResume();   
//		displayTopUpAmount();
		mSharedPref.registerOnSharedPreferenceChangeListener(spChanged);

		if(mNumber != null && mNumber.getText().toString() != null &&  mNumber.getText().toString().length() == 10) {
			mRecordMessage.setEnabled(true);
			mIncludeAudio.setEnabled(true);
		} else { 
			mRecordMessage.setEnabled(false);
			mIncludeAudio.setEnabled(false); 
		}	

		mNumber.clearFocus();
		mNumber.setSelected(false);
	}

	/** Called when the activity is paused; releases resources back to the 
	 *  system and stops audio recordings that may be playing. */
	@Override
	protected void onPause() { 
		super.onPause();
		mSharedPref.unregisterOnSharedPreferenceChangeListener(spChanged);
		 
	}

	/** Opens a new activity to allow the user to record audio content. */
	private void recordInput(final boolean includePhoto) {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE); 
		String restoredText = prefs.getString("Phone", null);
		

		InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(mNumber.getWindowToken(), 0);

		if (restoredText != null) { 
			Intent intent = new Intent(MainActivity.this, RecordAudio.class);
			intent.putExtra("photo", includePhoto); 
			intent.putExtra("phone", restoredText); 
			startActivity(intent);
		} else { 
			// this shouldn't happen
			showPrompt(); 
		}
	}

//	/** Displays the cash-in amount by fetching
//	 * the approximate amount from shared preferences. */
//	private void displayTopUpAmount() {
//		SharedPreferences sharedPref = getSharedPreferences(getString(R.string.top_up_preference_file_key),MODE_PRIVATE);
//		float cashInAmount = sharedPref.getFloat(getString(R.string.cash_in_amount),0);
//		mCashInMessage.setText(getString(R.string.cash_in_message,cashInAmount));
//	}

	/** Opens a new activity to allow the user to view and listen to 
	 *  recordings. */
	private void loadRecordings() {
		Intent intent = new Intent(this, org.cgnet.swara2.activity.HomeActivity.class);
		startActivity(intent); 
	}

	/** Starts a new Intent that allows to select a device
	 * and share the entire app over bluetooth.*/
	private void shareApplication() {
		ApplicationInfo app = getApplicationContext().getApplicationInfo();
		String filePath = app.sourceDir;

		Intent intent = new Intent(Intent.ACTION_SEND);

		// MIME of .apk is "application/vnd.android.package-archive".
		// but Bluetooth does not accept this. Let's use "*/*" instead.
		intent.setType("*/*");

		// Append file and send Intent
		File originalApk = new File(filePath);

		Log.e(TAG, ""+getExternalCacheDir());
		//Make new directory in new location
		File tempFile = new File(getExternalCacheDir() + "/ExtractedApk");
		//If directory doesn't exists create new
		if (!tempFile.isDirectory())
			if (!tempFile.mkdirs())
				return;
		//Get application's name and convert to lowercase
		tempFile = new File(tempFile.getPath() + "/" + getString(app.labelRes).replace(" ","").toLowerCase() + ".apk");

		//Copy file to new location
		InputStream in;
		OutputStream out;
		try {
			in = new FileInputStream(originalApk);
			out = new FileOutputStream(tempFile);
		} catch (FileNotFoundException e){
			Log.e(TAG,"PLease try again later");
			return;
		}

		byte[] buf = new byte[1024];
		int len;
		try {
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (IOException e){

		}

		Uri shareUri;
		if (Build.VERSION.SDK_INT >= 24) {
			shareUri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", tempFile);
		}
		else {
			shareUri = Uri.fromFile(tempFile);
		}
		intent.setClassName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity");
		intent.putExtra(Intent.EXTRA_STREAM, shareUri);
		startActivity(intent);
	}
}
