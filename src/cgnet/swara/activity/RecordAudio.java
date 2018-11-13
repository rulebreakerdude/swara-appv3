package cgnet.swara.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;

import org.cgnet.swara2.MainApplication;
import org.cgnet.swara2.MainApplication.TrackerName;
import org.cgnet.swara2.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import static com.msr.sneakernetcommon.R.string.phone_input;


/** This screen allows the user to record an audio message.
 *  They can then chose to send the recording off to a central location. 
 *  The user is able to listen to the recording prior to sending the off.  
 *   
 *  @author Krittika D'Silva (krittika.dsilva@gmail.com) */
public class RecordAudio extends Activity implements LocationListener {
	private static final String TAG = "RecordAudio";

	/** CGNet Swara's main directory with audio files. */
	private String mMainDir;

	/** Folder containing all audio files that have yet to be sent. */
	private final String mInnerDir = "/ToBeSent";

	/** Name of the audio file created.	*/
	private String mUniqueAudioRecording;

	/** Plays back the users voice recording. */
	private MediaPlayer mUserAudio; 
   
	/** Starts recording audio.  */
	private ImageButton mStart;

	/** Stops recording audio. */
	private ImageButton mStop;

	/** Plays back the audio that the user recorded. */
	private ImageButton mPlayback;

	/** Discards the audio recording created and returns the user to the
	 *  main menu.  */
	private ImageButton mBack;

	/** Sends audio recording to a central location if there's an Internet 
	 *  connection, if not saves the audio recording in a 
	 *  known folder - to be sent later. */
	private ImageButton mSendAudio;
 
	/** The action code we use in our intent, 
	 *  this way we know we're looking at the response from our own action.  */
	private static final int SELECT_PICTURE = 1;

	/** Saves logs about the user */
	private SaveAudioInfo mUserLogs;

	/** True if the created audio file should be sent, false otherwise. */
	private boolean mFileToBeSent;

	/** Shows the amount of time left - audio recordings must be less 
	 *  than 3 minutes. */
	private Chronometer chronometer;
	
	/** If the user wants to send an image with their audio file, their chosen 
	 *  image is shown as they record audio. */
	private ImageView mUserImage;

	/** The users phone number - inputed on the main screen. */
	private String mPhoneNumber; 
 
	/** Used to show the users chosen image.*/
	private Bitmap bitmap = null;
	    
	/** Audio recorder that records the users' message. */
	private RecMicToMp3 mRecMicToMp3;
	  
	private String provider;
	
	private boolean includePhoto;
	
	private boolean doneRecording;
	 
	private	Tracker t;
	
	private int mCountPlaybacks = 0;
	
	private int mCountChangingImages = 0;

	String mSecondPhonenumber = "";

	private static int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;
	
	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.record_audio); 

		mStart = (ImageButton) findViewById(R.id.start);
		mStop = (ImageButton) findViewById(R.id.stop);
		mPlayback = (ImageButton) findViewById(R.id.playback);
		mSendAudio = (ImageButton) findViewById(R.id.sendAudio); 
		mUserImage = (ImageView) findViewById(R.id.userImage);
		mBack = (ImageButton) findViewById(R.id.backToMain);
		chronometer = (Chronometer) findViewById(R.id.time);
		
		mFileToBeSent = false; 
		includePhoto = false;
		doneRecording = false;
		
		// At first, the only option the user has is to record audio
		mStart.setVisibility(View.VISIBLE); 
		mStop.setVisibility(View.GONE);
		mPlayback.setVisibility(View.GONE);
		mSendAudio.setVisibility(View.GONE); 
		mBack.setVisibility(View.INVISIBLE);
		findViewById(R.id.time).setVisibility(View.INVISIBLE);
 
		Intent intent = getIntent();
		Bundle extras = intent.getExtras(); 
		includePhoto = extras.getBoolean("photo"); 
		mPhoneNumber = extras.getString("phone");

		t = ((MainApplication) getApplication()).getTracker(TrackerName.APP_TRACKER);

		// Create folders for the audio files 
		setupDirectory();
		
		if(includePhoto) { 
			Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(Intent.createChooser(i,
					this.getString(R.string.select_picture)), SELECT_PICTURE);
			t.setScreenName("Record Audio - With an image");
		} else { 
			mUserImage.setVisibility(View.GONE);
			t.setScreenName("Record Audio - Without an image");
		}
		 // check if GPS enabled
        GPSTracker gpsTracker = new GPSTracker(this);
        if (gpsTracker.canGetLocation()) {
        	
        	 String stringLatitude = String.valueOf(gpsTracker.getLatitude());
             String stringLongitude = String.valueOf(gpsTracker.getLongitude());
             String country = gpsTracker.getCountryName(this);
             String city = gpsTracker.getLocality(this);
     
             String data = "Lat: " + stringLatitude + "; Lon: " + stringLongitude + "; " + country + "; " + city;
             Log.i(TAG, "Location data: "+data);

             t.set("" + 1, stringLatitude + " " + stringLongitude);
             t.set("" + 2, country);
             t.set("" + 3, city);  
             
             if(mUserLogs != null) {
            	 mUserLogs.setLocation(data);
             } 
        } else {
        	mUserLogs.setLocation("unk");
        }
		
		// Send a screen view.
		t.send(new HitBuilders.AppViewBuilder().build());

//        if (ContextCompat.checkSelfPermission(RecordAudio.this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//            mStart.setEnabled(false);
//        }
 
		  
		chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() { 
            @Override
            public void onChronometerTick(Chronometer chronometer) { 
                if(chronometer.getText().toString().equalsIgnoreCase("2:59") || 
                   chronometer.getText().toString().equalsIgnoreCase("02:59")) { 
                  mStop.performClick();
                }
            }
        });
		
		mStart.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View arg) {
				if (ContextCompat.checkSelfPermission(RecordAudio.this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
					chronometer.setVisibility(View.VISIBLE);
					mStart.setVisibility(View.GONE);
					mStop.setVisibility(View.VISIBLE);
					chronometer.setBase(SystemClock.elapsedRealtime());
					chronometer.start();

					startRecording();
				}
			}  
		}); 

		mStop.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View arg) {  
				chronometer.stop();  
				chronometer.setVisibility(View.INVISIBLE);
				mStop.setVisibility(View.GONE); 
				mPlayback.setVisibility(View.VISIBLE);
				mSendAudio.setVisibility(View.VISIBLE); 
				mBack.setVisibility(View.VISIBLE); 
				stopRecording(); 
			}  
		});

		mPlayback.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View arg) {
				if(mUserAudio != null && mUserAudio.isPlaying()) {
					mUserAudio.pause();
					mPlayback.setImageResource(R.drawable.play_icon);
					chronometer.stop(); 
				} else {
					mCountPlaybacks++; 
					chronometer.setVisibility(View.VISIBLE); 
					chronometer.setBase(SystemClock.elapsedRealtime());
					chronometer.start();
					
					
					mStart.setVisibility(View.GONE);
					mStop.setVisibility(View.GONE); 
					mPlayback.setVisibility(View.VISIBLE); 
					mSendAudio.setVisibility(View.VISIBLE); 
					startPlaying();
				} 
			}
		});

		mSendAudio.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View arg) {  
				sendData(); 
				Toast.makeText(RecordAudio.this, RecordAudio.this.getString(R.string.file_sent),  
						Toast.LENGTH_LONG).show();
				Intent intent = new Intent(RecordAudio.this, MainActivity.class);
				startActivity(intent);
				finish();
			}
		}); 

		mBack.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View arg) { 
				goBackHome();		  
				if(bitmap != null) { 
					Log.e(TAG, "Recycling bitmap.");
					bitmap.recycle();
					bitmap = null;
				}
			}
		});
		
		mUserImage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg) { 
				if(doneRecording) {  
					mCountChangingImages++;
					Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					startActivityForResult(Intent.createChooser(i,
							RecordAudio.this.getString(R.string.select_picture)), SELECT_PICTURE);
				}
			} 
		});
		
		if(!includePhoto) { 
			showPrompt(); 
		} 		
		
		
	}
	 
	
	/** Displays an alert dialog prompting 
	 *  the user to input their phone number. */
	private void showPrompt() {
 		// get prompts.xml view
		LayoutInflater li = LayoutInflater.from(this);
		View promptsView = li.inflate(R.layout.phone_selection, null);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		
		// set prompts.xml to alertdialog builder
		alertDialogBuilder.setView(promptsView);

		final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
//		TextView tv = (TextView) promptsView.findViewById(R.id.textView1);
//		tv.setText(this.getString(R.string.interviee_phone_number));

        final RadioGroup radioSelection = promptsView.findViewById(R.id.radioGroupForPhone);

		radioSelection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == R.id.own_recording){
                    String savedText = getSharedPreferences(
                            getString(com.msr.sneakernetcommon.R.string.shared_prefs), Context.MODE_PRIVATE)
                            .getString(getString(phone_input), "");;
					mSecondPhonenumber = savedText;
				} else {
					mSecondPhonenumber = "";
					userInput.requestFocus();
				}
				userInput.setText(mSecondPhonenumber);
			}

		});

		// set dialog message
		alertDialogBuilder.setCancelable(false).setPositiveButton(this.getString(R.string.ok_phone),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) { 
				mSecondPhonenumber = userInput.getText().toString(); 
				if(userInput.getText().toString().length() == 10) {
					mSecondPhonenumber = userInput.getText().toString();
                    TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
					String device_id = "";
                    try {
						device_id = telephonyManager.getDeviceId();
					}
					catch (SecurityException e){
						ActivityCompat.requestPermissions(RecordAudio.this, new String[]{android.Manifest.permission.READ_PHONE_STATE},
								MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
						device_id = telephonyManager.getDeviceId();
					}
					mUserLogs.setPhoneNumber("Reporter: " + mPhoneNumber + " IMEI: " + device_id + " Interviewee: " + mSecondPhonenumber);
					((TextView) findViewById(R.id.their_number)).setText(RecordAudio.this.getString(R.string.interviee_show_numbernumber) + " " + mSecondPhonenumber);
					
				} else { 
					showPrompt();
				}
			}
		})
		.setNegativeButton(this.getString(R.string.cancel_phone),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) { 
				
				Intent intent = new Intent(RecordAudio.this, MainActivity.class);
				startActivity(intent);
				finish();
				InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                im.hideSoftInputFromWindow(userInput.getWindowToken(), 0);
                dialog.cancel();
			}
		});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create(); 
		alertDialog.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		// show it
		alertDialog.show();
	} 

	private void updatePhoneDetails(){

    }
	 
	/**  */
	private void goBackHome() { 
		if(!mUserLogs.getDuration().equals(null) && !mUserLogs.getDuration().equals("")) {
			if(includePhoto) {
				 t.send(new HitBuilders.TimingBuilder()
		         .setCategory("Length of recording") 
		         .setVariable("Audio recording not sent, button clicked to return home")
		         .setLabel("Photo included") 
		         .setValue(Long.parseLong(mUserLogs.getDuration()) * 1000) 
		         .build());
			} else { 
				 t.send(new HitBuilders.TimingBuilder()
		         .setCategory("Length of recording") 
		         .setVariable("Audio recording not sent, button clicked to return home")
		         .setLabel("Photo not included") 	  
		         .setValue(Long.parseLong(mUserLogs.getDuration()) * 1000) 						 
		         .build()); 
			} 
		}
		if(includePhoto) {
			 t.send(new HitBuilders.EventBuilder()
	         .setCategory("Number of times image changed") 
	         .setAction("Audio recording not sent, button clicked to return home") 
	         .setValue(mCountChangingImages)
	         .build());
		}
		
		
		
		if(includePhoto) {
			 t.send(new HitBuilders.EventBuilder()
	         .setCategory("Number of playbacks") 
	         .setAction("Audio recording not sent, button clicked to return home")
	         .setLabel("Photo included") 
	         .setValue(mCountPlaybacks)
	         .build());
		} else { 
			 t.send(new HitBuilders.EventBuilder()
	         .setCategory("Number of playbacks") 
	         .setAction("Audio recording not sent, button clicked to return home")
	         .setLabel("Photo not included") 	  
	         .setValue(mCountPlaybacks) 						 
	         .build()); 
		} 
		
		if(bitmap != null) {
			Log.e(TAG, "Recycling bitmap.");
			bitmap.recycle();
			bitmap = null;
		}
		File file = new File(mMainDir + mInnerDir + mUniqueAudioRecording);
		if(file.exists()) {
			Log.e(TAG, "mBack.onClick - Deleting file: " + mMainDir + mInnerDir + mUniqueAudioRecording);
			file.delete();
		}
		
		Toast.makeText(RecordAudio.this, RecordAudio.this.getString(R.string.your_message_discarded),  
				Toast.LENGTH_LONG).show();
		
		Intent intent = new Intent(RecordAudio.this, MainActivity.class);
		startActivity(intent);
		finish();
	}


	/**  */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data); 
		if (resultCode == RESULT_OK) { 
			if (requestCode == SELECT_PICTURE) { 
				Uri selectedImageUri = data.getData();
				String selectedImagePath = getPath(selectedImageUri);

				bitmap = BitmapFactory.decodeFile(selectedImagePath);
				if(bitmap != null) {
					while(bitmap.getHeight() > 2000 || bitmap.getWidth() > 2000) {  
						bitmap = halfSize(bitmap);
					}
					
					Log.e(TAG, "Bitmap height: " + bitmap.getHeight() + " width: " + bitmap.getWidth());
					mUserImage.setImageBitmap(bitmap);  
					mUserLogs.setPhotoPath(selectedImagePath); 
				}
				showPrompt();
			}
		} else if(bitmap != null) {
			Log.e(TAG, resultCode + " " + requestCode);
			 
		} else { 
			goBackHome();
		}
	}
	
	/**   */ 
	private Bitmap halfSize(Bitmap input) { 
		int height = input.getHeight();
		int width = input.getWidth();  
		return Bitmap.createScaledBitmap(input,  width/2, height/2, false);
	}

	/**  */
	public String getPath(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	/** Sets up file structure for audio recordings. **/
	private void setupDirectory() {
		// This folder should have been created in MainActivity
		// This is just in case it wasn't.
		mMainDir = Environment.getExternalStorageDirectory().getAbsolutePath();
		mMainDir += "/Android/data/com.MSRi.ivr.cgnetswara";
		File dir = new File(mMainDir);
		if(!dir.exists() || !dir.isDirectory()) {
			dir.mkdir();
		} 		

		// This folder will be queried when there's Internet - files that 
		// need to be sent should be stored in here 
		File dirInner = new File(mMainDir + mInnerDir);
		if(!dirInner.exists() || !dirInner.isDirectory()) {
			dirInner.mkdir();
		} 

		Calendar c = Calendar.getInstance();
		
		// Name of audio file is the data and then time the audio was created. 
		String date = c.get(Calendar.YEAR) + "_"+ c.get(Calendar.MONTH)
				+ "_" + c.get(Calendar.DAY_OF_MONTH);
		String time = c.get(Calendar.HOUR_OF_DAY) + "_" 
				+ c.get(Calendar.MINUTE) + "_" + c.get(Calendar.SECOND);


		// Different format for the name and date - used for the body of the email.
		String dateAudio = c.get(Calendar.YEAR) + "-"+ c.get(Calendar.MONTH)
				+ "-" + c.get(Calendar.DAY_OF_MONTH);

		String timeAudio = c.get(Calendar.HOUR_OF_DAY) + ":" 
				+ c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);

		
		mUniqueAudioRecording = "/" + date + "__" + time;
		
		mUserLogs = new SaveAudioInfo(mMainDir, mUniqueAudioRecording, this);
		mUserLogs.setAudioDateTime(dateAudio + " " + timeAudio);

		mUniqueAudioRecording += ".mp3";  
	}

	/** Releases resources back to the system.  */
	private void stopPlayingAudio(MediaPlayer mp) {
		if(mp != null) {
			mp.stop();
			mp.reset();   
			mp = null;	
		}
	}

	/** Releases resources back to the system.  */
	private void stopRecording() { 
		doneRecording = true;
		if(mRecMicToMp3 != null) {
			mRecMicToMp3.stop();
		}
		if(includePhoto) { 
			TextView textLimit = (TextView) findViewById(R.id.limit);
			textLimit.setText(this.getString(R.string.tap_image_to_choose_another));
		} 
		
		
		MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
		
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(mMainDir + mInnerDir + mUniqueAudioRecording);
			metaRetriever.setDataSource(inputStream.getFD()); 
			inputStream.close();
		 	
			Long durationms = Long.parseLong(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
			long duration = durationms / 1000;
		      
			mUserLogs.setAudioLength(duration);
		    
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			long duration = 0;
		     
			
			mUserLogs.setAudioLength(duration);
		    
		} 
	}

	/** Called when the activity is paused; releases resources back to the 
	 *  system and stops audio recordings that may be playing. */
	@Override
	public void onPause() {
		super.onPause(); 
		
		// If the user pauses the app when they're recording a message 
		// we're going to treat it like they stopped recording before 
		// pausing the app
		if(mStop.getVisibility() == View.VISIBLE) {  
			stopRecording(); 
		}  
		stopPlayingAudio(mUserAudio); 
	}

	/** Called when the activity is paused; begins playing the audio recording
	 *  for the user. */
	@Override
	public void onResume() {
		super.onResume(); 
 
		if(mRecMicToMp3 != null) {
			mRecMicToMp3.stop();
		} 
	}

	/** Creates an audio recording using the phone mic as the audio source. */
	private void startRecording() {   
		mRecMicToMp3 = new RecMicToMp3(mMainDir + mInnerDir + mUniqueAudioRecording, 8000); 
		mRecMicToMp3.start();
	}	

	/** Plays the generated audio recording. */
	private void startPlaying() {
		mUserAudio = new MediaPlayer();
		 
		try {
			// Saved in the main folder 
			mUserAudio.setDataSource(mMainDir + mInnerDir + mUniqueAudioRecording);
			mUserAudio.prepare();
			mUserAudio.start();
  			 
			mPlayback.setImageResource(R.drawable.stop_icon);
			
			mUserAudio.setOnCompletionListener(new OnCompletionListener() {
	            public void onCompletion(MediaPlayer mp) { 
	            	mPlayback.setImageResource(R.drawable.play_icon);
	            	chronometer.stop(); 
	            }
	        }); 
			
		} catch (IOException e) {
			Log.e(TAG, "StartPlaying() : prepare() failed");
		} catch (Exception e) { 
			Log.e(TAG, e.toString());
		}
	}
 

	/** Sends the audio file to a central location. */
	private void sendData() { 
		Log.e(TAG, "Send data is being called");
		
		if(!mUserLogs.getDuration().equals(null) && !mUserLogs.getDuration().equals("")) {
			if(includePhoto) {
				 t.send(new HitBuilders.TimingBuilder()
		         .setCategory("Length of recording") 
		         .setVariable("Audio recording sent")
		         .setLabel("Photo included") 
		         .setValue(Long.parseLong(mUserLogs.getDuration()) * 1000) 
		         .build());
			} else { 
				 t.send(new HitBuilders.TimingBuilder()
		         .setCategory("Length of recording") 
		         .setVariable("Audio recording sent")
		         .setLabel("Photo not included") 	  
		         .setValue(Long.parseLong(mUserLogs.getDuration()) * 1000) 						 
		         .build()); 
			}
		}
		   
		if(includePhoto) {
			 t.send(new HitBuilders.EventBuilder()
	         .setCategory("Number of playbacks") 
	         .setAction("Audio recording sent")
	         .setLabel("Photo included") 
	         .setValue(mCountPlaybacks)
	         .build());
		} else { 
			 t.send(new HitBuilders.EventBuilder()
	         .setCategory("Number of playbacks") 
	         .setAction("Audio recording sent")
	         .setLabel("Photo not included") 	  
	         .setValue(mCountPlaybacks) 						 
	         .build()); 
		}
		
		if(includePhoto) {
			 t.send(new HitBuilders.EventBuilder()
	         .setCategory("Number of times image changed") 
	         .setAction("Audio recording sent") 
	         .setValue(mCountChangingImages)
	         .build());
		}
		
		 
		mFileToBeSent = true; 
		mUserLogs.writeToFile();
		 
		Intent intent = new Intent();  
		GoogleAnalytics.getInstance(this.getBaseContext()).dispatchLocalHits();
		intent.setAction("com.android.CUSTOM_INTENT");
		sendBroadcast(intent);  
	} 
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {   
		if(keyCode == KeyEvent.KEYCODE_BACK && mStop.getVisibility() == View.VISIBLE) { 
			mStop.performClick();
		}  
			
		
	    //Handle the back button
	    if(keyCode == KeyEvent.KEYCODE_BACK && mBack.getVisibility() == View.VISIBLE) {
	        //Ask the user if they want to quit
	        new AlertDialog.Builder(this) 
	        .setMessage(this.getString(R.string.discard_message))
	        .setPositiveButton(this.getString(R.string.yes_message), new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	            	if(!mUserLogs.getDuration().equals(null) && !mUserLogs.getDuration().equals("")) {
		            	if(includePhoto) {
		            		t.send(new HitBuilders.TimingBuilder()
		       	         	.setCategory("Length of recording") 
		       	         	.setVariable("Audio recording not sent, soft key clicked to return home")
		       	         	.setLabel("Photo included") 
		       	         	.setValue(Long.parseLong(mUserLogs.getDuration()) * 1000) 
		       	         	.build());
			       		} else { 
			       			t.send(new HitBuilders.TimingBuilder()
			       	        .setCategory("Length of recording") 
			       	        .setVariable("Audio recording not sent, soft key clicked to return home")
			       	        .setLabel("Photo not included") 	  
	            	         .setValue(Long.parseLong(mUserLogs.getDuration()) * 1000) 						 
			       	        .build()); 
			       		}
	            	}
		        		
		        		
	        		if(includePhoto) {
		       			 t.send(new HitBuilders.EventBuilder()
		       	         .setCategory("Number of playbacks") 
		       	         .setAction("Audio recording not sent, soft key clicked to return home")
		       	         .setLabel("Photo included") 
		       	         .setValue(mCountPlaybacks)
		       	         .build());
		       		} else { 
		       			 t.send(new HitBuilders.EventBuilder()
		       	         .setCategory("Number of playbacks") 
		       	         .setAction("Audio recording not sent, soft key clicked to return home")
		       	         .setLabel("Photo not included") 	  
		       	         .setValue(mCountPlaybacks) 						 
		       	         .build()); 
		       		} 
	            	RecordAudio.this.finish();
	                
	            } 
	        })
	        .setNegativeButton(this.getString(R.string.no_message), null)
	        .show(); 
	        return true;
	    }
	    else {
	        return super.onKeyDown(keyCode, event);
	    }

	}

	/** Called when the activity is destroyed, deletes the 
	 *  audio file if it shouldn't be sent. */
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(!mFileToBeSent) { 
			File file = new File(mMainDir + mInnerDir + mUniqueAudioRecording);
			if(file.exists()) {
				Log.e(TAG, "onDestroy, deleting file: " + mMainDir + mInnerDir + mUniqueAudioRecording);
				file.delete();
			}
			if(bitmap != null) { 
				Log.e(TAG, "Recycling bitmap.");
				bitmap.recycle();
				bitmap = null;
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		int lat = (int) (location.getLatitude());
	    int lng = (int) (location.getLongitude());
	    Log.e(TAG, "lat: " + lat + " lng: " + lng); 
	}

	@Override
	public void onProviderDisabled(String provider) { 
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) { 
	} 
}
