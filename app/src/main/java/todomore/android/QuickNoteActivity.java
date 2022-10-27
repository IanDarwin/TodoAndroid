package todomore.android;

import java.util.List;

import com.darwinsys.todo.model.Task;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Toast;

public class QuickNoteActivity extends Activity {

	final static String TAG = QuickNoteActivity.class.getSimpleName();
	
	TaskDao mDao;

	/** This is a non-interactive (usually) Activity */
	public void onCreate(Bundle b) {
		
		Log.d(TAG, "onCreate");
		
		// Get the Intent, make sure it looks OK
		final Intent i = getIntent();
		
		// Either the intent has text in it, or we have to listen explicitly.
		String note = i.getStringExtra(Intent.EXTRA_TEXT);
		if (note == null || note.length() == 0) {
			error("No text");
			displaySpeechRecognizer();
			finish();
		}
		Task t = new Task(note);
		mDao = ((TodoMoreApplication) getApplication()).getTaskDao();
		long id = mDao.insert(t);
		if (id < 1) {
			error("Insert failed");
		} else {
			Log.d(TAG, "Inserted, id = " + id);
		}
		finish(); // Or startActivity(ListActivity)?
	}

	private static final int REQUEST_CODE_RECOGNIZE_SPEECH = 0;

	// Start a Speech Recognizer activity by implicit intent
	private void displaySpeechRecognizer() {
		Log.d(TAG, "QuickNoteActivity.displaySpeechRecognizer()");
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		startActivityForResult(intent, REQUEST_CODE_RECOGNIZE_SPEECH);
	}

	// Called back when any Activity started for a result returns the result.
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "QuickNoteActivity.onActivityResult()");
		switch (requestCode) {
		case REQUEST_CODE_RECOGNIZE_SPEECH:
			if (resultCode == RESULT_OK) {
				List<String> results = data.getStringArrayListExtra(
						RecognizerIntent.EXTRA_RESULTS);
				String text = results.get(0);
				mDao.insert(new Task(text));
			}
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void error(String mesg) {
		Log.e(TAG, mesg);
		// XXX use TTS api here since this Activity invoked by voice
		Toast.makeText(this, mesg, Toast.LENGTH_LONG).show();
	}
}
