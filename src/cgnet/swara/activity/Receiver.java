package cgnet.swara.activity;

import java.io.File;

import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;
import android.content.Intent;
import android.os.Environment;
import android.content.Context; 
import android.net.ConnectivityManager; 
import android.content.BroadcastReceiver;

import org.cgnet.swara2.R;

/** A BroadcastReciever responds to broadcast messages from the system, 
 *  this class specifically responds to changes in network connectivity.  
 *  @author Krittika D'Silva
 *  @author Alok Sharma:Since the Connectivity change is deprecated from Nougat, migrating to an async timed event to induce CUSTOM_INTENT
 * */
public class Receiver extends BroadcastReceiver {
	private static final String TAG = "Receiver";
	  
	/** CGNet Swara's main directory with audio files. */
	private String mMainDir;
	
	/** Folder containing all audio files that have yet to be sent. */
	private final String mInnerDir = "/Logs";
	  
	/** Called when there's a change in connectivity. Iterates through files 
	 * that need to be sent and sends each one if there's Internet. */ 
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("truth","5");
    	mMainDir = Environment.getExternalStorageDirectory().getAbsolutePath();
		mMainDir += "/Android/data/com.MSRi.ivr.cgnetswara";
        Log.e("truth","6");

		
    	ConnectivityManager cm = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        if (cm == null)
            return; 
        if (cm.getActiveNetworkInfo() != null || intent.getAction().equals("com.android.CUSTOM_INTENT")) {

        	 
        	File dir = new File(mMainDir + mInnerDir); // Contains files to be sent
        	File[] directoryListing = dir.listFiles();
         
        	if (directoryListing != null) {
        		for (File child : directoryListing) { 
        	    	String fileName = child.getName();

					SharedPreferences sharedPref = context.getSharedPreferences(
							Integer.toString(R.string.email_preference_file_key),Context.MODE_PRIVATE
					);
					boolean sentEmail;
                    if (sharedPref.getBoolean(fileName.substring(0, fileName.length() - 4), false))
                        sentEmail = true;
                    else sentEmail = false;
                    Log.e(TAG, fileName.substring(0,fileName.length()-4)+" with value "+sentEmail);
					if (!sentEmail) {
						SendEmailAsyncTask task = new SendEmailAsyncTask(context,
								mMainDir, mInnerDir, "/" + fileName);
						task.execute();
					}
        	    }
        	}       	
        }   
    }  
}