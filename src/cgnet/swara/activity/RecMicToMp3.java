package cgnet.swara.activity;

import java.io.File;
import android.os.Handler;
import java.io.IOException;
import java.io.FileOutputStream;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.io.FileNotFoundException;
import cgnet.swara.activity.lame.SimpleLame;
import java.security.InvalidParameterException;

public class RecMicToMp3 {

	static {
		System.loadLibrary("mp3lame");
	}
 
	private String mFilePath;

 	private int mSampleRate;
 	private boolean mIsRecording = false;

 	private Handler mHandler;

 	public static final int MSG_REC_STARTED = 0;
 	public static final int MSG_REC_STOPPED = 1;

 	public static final int MSG_ERROR_GET_MIN_BUFFERSIZE = 2;

 	public static final int MSG_ERROR_CREATE_FILE = 3;

 	public static final int MSG_ERROR_REC_START = 4;

 	public static final int MSG_ERROR_AUDIO_RECORD = 5;

 	public static final int MSG_ERROR_AUDIO_ENCODE = 6;
 	public static final int MSG_ERROR_WRITE_FILE = 7;

 	public static final int MSG_ERROR_CLOSE_FILE = 8;

 
	public RecMicToMp3(String filePath, int sampleRate) {
		if (sampleRate <= 0) {
			throw new InvalidParameterException(
					"Invalid sample rate specified.");
		}
		this.mFilePath = filePath;
		this.mSampleRate = sampleRate;
	}

	 
	public void start() { 
		if (mIsRecording) {
			return;
		} 
		new Thread() {
			@Override
			public void run() {
				android.os.Process
						.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
			
				final int minBufferSize = AudioRecord.getMinBufferSize(
						mSampleRate, AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT);
			
				if (minBufferSize < 0) {
					if (mHandler != null) {
						mHandler.sendEmptyMessage(MSG_ERROR_GET_MIN_BUFFERSIZE);
					}
					return;
				}
				 
				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, mSampleRate,
						AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT, minBufferSize * 2);
 
				short[] buffer = new short[mSampleRate * (16 / 8) * 1 * 5]; // SampleRate[Hz] * 16bit * Mono * 5sec
				byte[] mp3buffer = new byte[(int) (7200 + buffer.length * 2 * 1.25)];

				FileOutputStream output = null;
				try {
					output = new FileOutputStream(new File(mFilePath));
				} catch (FileNotFoundException e) {
 
					if (mHandler != null) {
						mHandler.sendEmptyMessage(MSG_ERROR_CREATE_FILE);
					}
					return;
				}
 				SimpleLame.init(mSampleRate, 1, mSampleRate, 32);

				mIsRecording = true;  
				try {
					try {
						audioRecord.startRecording(); 
					} catch (IllegalStateException e) { 
						if (mHandler != null) {
							mHandler.sendEmptyMessage(MSG_ERROR_REC_START);
						}
						return;
					}

					try { 
						if (mHandler != null) {
							mHandler.sendEmptyMessage(MSG_REC_STARTED);
						}

						int readSize = 0;
						while (mIsRecording) {
							readSize = audioRecord.read(buffer, 0, minBufferSize);
							if (readSize < 0) { 
								if (mHandler != null) {
									mHandler.sendEmptyMessage(MSG_ERROR_AUDIO_RECORD);
								}
								break;
							} 
							else if (readSize == 0) {
								;
							} 
							else {
								int encResult = SimpleLame.encode(buffer,
										buffer, readSize, mp3buffer);
								if (encResult < 0) { 
									if (mHandler != null) {
										mHandler.sendEmptyMessage(MSG_ERROR_AUDIO_ENCODE);
									}
									break;
								}
								if (encResult != 0) {
									try {
										output.write(mp3buffer, 0, encResult);
									} catch (IOException e) { 
										if (mHandler != null) {
											mHandler.sendEmptyMessage(MSG_ERROR_WRITE_FILE);
										}
										break;
									}
								}
							}
						}

						int flushResult = SimpleLame.flush(mp3buffer);
						if (flushResult < 0) { 
							if (mHandler != null) {
								mHandler.sendEmptyMessage(MSG_ERROR_AUDIO_ENCODE);
							}
						}
						if (flushResult != 0) {
							try {
								output.write(mp3buffer, 0, flushResult);
							} catch (IOException e) { 
								if (mHandler != null) {
									mHandler.sendEmptyMessage(MSG_ERROR_WRITE_FILE);
								}
							}
						}

						try {
							output.close();
						} catch (IOException e) { 
							if (mHandler != null) {
								mHandler.sendEmptyMessage(MSG_ERROR_CLOSE_FILE);
							}
						}
					} finally {
						audioRecord.stop();  
						audioRecord.release();
					}
				} finally {
					SimpleLame.close();
					mIsRecording = false; 
				}

				if (mHandler != null) {
					mHandler.sendEmptyMessage(MSG_REC_STOPPED);
				}
			}
		}.start();
	}

 	public void stop() {
		mIsRecording = false;
	}

 	public boolean isRecording() {
		return mIsRecording;
	}
  
	public void setHandle(Handler handler) {
		this.mHandler = handler;
	}
} 