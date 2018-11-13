package cgnet.swara.activity;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.cgnet.swara2.R;

import java.io.FileWriter;
import java.io.IOException;

/** 
 * 
 *  @author Krittika D'Silva (krittika.dsilva@gmail.com)
 * */
public class SaveAudioInfo {
	private static final String TAG = "SaveAudioInfo";
	/** CGNet Swara's main directory with audio files. */
	private String mMainDir;

	/** Folder containing all audio files that have yet to be sent. */
	private final String mInnerDir = "/ToBeSent";

	/** Path to an image chosen by the user. */
	private String mPhotoFile = null;

	/** Path to a user's audio file */
	private String mAudioPath;

	/** The user's phone number. */
	private String mPhoneNumber = "";
 
	private String duration = "";
	
	private String time = "";

	private String location = "";

	private Context mContext = null;
 
	
	/** Given a path to a main direction, the unique audio file name and the 
	 *  users phone number, initializes an object. */
	public SaveAudioInfo(String mainDir, String audioRecordingPath, Context context) {
		mMainDir = mainDir;
		mAudioPath = audioRecordingPath;
		mContext = context;
		
		// This folder will be queried when there's Internet - files that 
		// need to be sent should be stored in here 
		File dirInner = new File(mMainDir + "/Logs");
		if(!dirInner.exists() || !dirInner.isDirectory()) {
			dirInner.mkdir();
		} 
	} 

	public void setPhoneNumber(String phone) { 
		mPhoneNumber = phone;   
	}
	
	/** Sets the path to an image chosen by the user. */
	public void setPhotoPath(String path) { 
		mPhotoFile = path;
	}
	 
 

	/** Saves information about the user in a comma separated file. */
	public void writeToFile() {   
		Log.e(TAG, "Writing file");
		
		String content = mMainDir + mInnerDir + mAudioPath + "," + mPhotoFile + "," + mPhoneNumber; 
		content += "," + time + "," + duration + "," + location;
		Log.e(TAG, "Saving a text file: " + content);
		File root = new File(mMainDir + "/Logs");
		try { 
			if (!root.exists()) {
				root.mkdirs();
			}
			File gpxfile = new File(root, mAudioPath + ".txt");
			FileWriter writer = new FileWriter(gpxfile);
			writer.append(content);
			writer.flush();
			writer.close();
            Log.i(TAG, mAudioPath.substring(1,mAudioPath.length()-1));
			SharedPreferences sharedPref = mContext.getSharedPreferences(
					Integer.toString(R.string.email_preference_file_key),Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putBoolean(mAudioPath.substring(1,mAudioPath.length()-1), false);
			editor.apply();

		} catch (IOException e) { 
			Log.e(TAG,"!!!! " + e.toString()); 
		} catch(Exception e) { 
			Log.e(TAG, "!!!! " + e.toString()); 
		}
	}

	public void setAudioDateTime(String timeAudio) {
		Log.e(TAG, "Setting date & time: " + timeAudio);
		time = timeAudio; 
	}

	public void setAudioLength(long durationAudio) {
		Log.e(TAG, "Setting duration: " + durationAudio);
		duration = Long.toString(durationAudio); 
	} 	
	
	public String getDuration() {
		return duration; 
	}

	public void setLocation(String data) {
		 location = data;
	}
}
