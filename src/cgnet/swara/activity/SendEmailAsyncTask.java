package cgnet.swara.activity;

import java.io.File;

import android.content.SharedPreferences;
import android.util.Log;
import java.util.Scanner;
import android.os.AsyncTask;
import android.content.Context;

import org.cgnet.swara2.R;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import javax.mail.MessagingException;
import java.io.FileNotFoundException;
import javax.mail.AuthenticationFailedException;


/** This class performs background operations for the CGNet Swara app. 
 *  When there's an Internet connection, audio files are sent 
 *  via email with the relevant information included.  
 *  @author Krittika D'Silva
 */
class SendEmailAsyncTask extends AsyncTask <Void, Void, Boolean> {
	private static final String TAG = "SendEmailAsyncTask"; 
	
	/** Email sent with the audio recording. */
	private Mail mMail;
	
	/** CGNet Swara's main directory with audio files. */
	private String mMainDir;
	
	/** Folder containing all audio files that have yet to be sent. */
	private String mInnerDir; 
	
	/** Name of the audio file created.	*/
	private String mUniqueAudioRecording;
	
	/** Email address the email is sent from. */
	private final String mFromAdddress = "rulebreakerdude@gmail.com";//EmailLogin.email;
	
	/** Password for the SMTP server so that the email can be sent.  */
	private final String mFromPassword = "";//EmailLogin.password;
	
	/** Email address that the message is sent to. */
	private final String mToAddress = "cgnetswaratest@gmail.com";//"cgnetswaratest@gmail.com";
	  
	/** True if the email has been sent, false otherwise. */
	private boolean mEmailSent = false;
	
	/** Name of the text file. */ 
	private String mTextFile;

	private String mTextFileName= "";
	
	/** Name of the audio file. */
	private String mAudioFile;

	private Context mContext;
	
	/** Given a context, the path to a main direction as a string, the path to the 
	 *  inner folder as a string, and the name of the text file to be attached 
	 *  an async email is sent to a central location containing the attachment.  
	 * */
    public SendEmailAsyncTask(Context context, String outerDir, String innerDir, String fileName) {
    	Log.e(TAG, "5. Trying to send: " + fileName);
    	mContext = context;
    	mTextFile = outerDir + innerDir + fileName;
		mTextFileName = fileName;
    	
    	FileInputStream fstream;
    	String firstLine = "";
		try {
			 fstream = new FileInputStream(mTextFile);
	    	 Scanner br = new Scanner(new InputStreamReader(fstream)); 
	    	 while (br.hasNext()) {
	    		 firstLine = br.nextLine(); 
	    	 }
	    	 
		} catch (FileNotFoundException e) { 
			e.printStackTrace();
		}

		String[] parts = firstLine.split(",");
		 
		mAudioFile = parts[0] + ".mp3"; 
		String photo = parts[1];
		String phoneNumber = parts[2]; 
		String time = parts[3];
		String length;
		if(parts.length > 4) {
			length = parts[4];
		} else { 
			length = "0";
		}
		String location;
		if(parts.length > 5) {
			location = parts[5];
		} else { 
			location = "unk";
		}
    	mMail = new Mail(mFromAdddress, mFromPassword);  
    	Log.e(TAG, "mMail: " + mMail);
    	mMainDir = outerDir;
    	mInnerDir = innerDir;
        mUniqueAudioRecording = fileName;
    			 
    	String[] toArr = {mToAddress}; // multiple email addresses can be added here 
        mMail.setTo(toArr);
        mMail.setFrom(mFromAdddress);
        mMail.setSubject(getSubject(phoneNumber, time, length, location));
         
        String body = getBody(phoneNumber, time, length, location); 
        		
        mMail.setBody(body); 
        
        Log.e(TAG, "6. Location of file: " + mMainDir + mInnerDir + mUniqueAudioRecording);
        try { 
			if(mAudioFile != null) {
				mMail.addAttachment(mAudioFile);
				Log.i(TAG, "Audio attached to email");
			}
			if(photo != null) { 
				mMail.addAttachment(photo);
			}
		} catch (Exception e) { 
			Log.e(TAG, "Problem including an attachment " +  e.toString());
		}
    }
     
	/** Tries to send off an email with the relevant information and 
	 *  attachments. If there is no Internet connection, 
	 *  the email will not be sent and false will be returned.  
     * */
    @Override
    protected Boolean doInBackground(Void... params) { 
    	  
    	Log.e(TAG, "mMail: " + mMail);
        try { 
        	Log.e(TAG, "about to send the file");
        	if(mMail==null){Log.i(TAG, "Mail is empty");}
        	if (mMail != null && mMail.send()) {
        		mEmailSent = true; 

    	    	Log.e(TAG, "email sent");
    	    	Log.e(TAG, "deleting: " + mTextFile);
        		// For now, the audio file created is NOT deleted. 
        		// It can be, if the files end up taking too much storage space.
        		//File audio = new File(mAudioFile);
        		//audio.delete();
        		File file = new File(mTextFile);
        		file.delete();

				Log.i(TAG, mTextFileName.substring(1,mTextFileName.length()-4));
				SharedPreferences sharedPref = mContext.getSharedPreferences(
						Integer.toString(R.string.email_preference_file_key),Context.MODE_PRIVATE
				);
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putBoolean(mTextFileName.substring(0,mTextFileName.length()-4), true);
				editor.apply();

        		return true;
        	} else { 
        		mEmailSent = false;
        		Log.e(TAG, "Email not sent"); 
        		return false;
        	} 
        } catch (AuthenticationFailedException e) {
            Log.e(TAG, "Bad account details: " + e); 
            return false;
        } catch (MessagingException e) {
            Log.e(TAG, " " + e); 
            return false;
        } catch (Exception e) {
        	Log.e(TAG, "" + e); 
            return false;
        }
    } 
    
    /** Returns true if the email has been sent, false otherwise. */
    public boolean emailSent() { 
    	return mEmailSent;
    }
    
    /** Given a phone number, as a string, a time and a length of the audio file, 
     *  returns a string that will serve as the subject of the email - includes 
     *  the relevant information about the audio file that will be parsed later by the
     *  server. */    
    private String getSubject(String phoneNumber, String time, String length, String location) {
    	String subject = "Swara-Main|app|" + length + "|DRAFT|" + phoneNumber + "|" + location + "|" + time + "|PUBLIC";
    	 
		return subject;
	}
    
    /** Given a phone number, as a string, a time and a length of the audio file, 
     *  returns a string that will serve as the body of the email - includes 
     *  the relevant information about the audio file that will be parsed later by the
     *  server. */
    private String getBody(String phoneNumber, String time, String length, String location) { 
    	String body;
    	body =  "******************************************************************************\n" + 
    			"SERVER/सर्वर                        : Swara-Main\n" +
    			"******************************************************************************\n" +
    			"POST ID/पोस्ट क्र                       : unk" + "\n" +
    			"******************************************************************************\n" +
    			"CALLER/नंबर                         : " + phoneNumber + "\n" +
    			"******************************************************************************\n" +
    			"TIME STAMP/समय                  : " + time + "\n" +
    			"******************************************************************************\n" +
    			"NAME OF CALLER/फ़ोन करने वाले का नाम     :\n" + 
    			"******************************************************************************\n" +
    			"CALL LOCATION/कॉल कहाँ से आई        :\n" + 
    			"******************************************************************************\n" +
    			"TEL CIRC/ टेलिकॉम सर्किल                : "+ location + "\n" +
    			"******************************************************************************\n" +
    			"LNGTH/अवधी                              : " + length + "\n" +
    			"******************************************************************************\n" +
    			"STATUS/स्थिति                                           : DRAFT\n" + 
    			"******************************************************************************\n" +
    			"TEXT SUMMARY/   सन्देश                  :";
    	
    	return body;
    } 
}
