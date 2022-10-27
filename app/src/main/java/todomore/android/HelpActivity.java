package todomore.android;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

/*
 * Displays information about the application
 * @author Ian Darwin, Rejminet Group Inc.
 */
public class HelpActivity extends Activity {
	String TAG = "AboutActivity";
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		
		WebView aboutView = (WebView)findViewById(R.id.help_content);
		try {
			InputStreamReader is = new InputStreamReader(this.getResources().openRawResource(R.raw.help));
			BufferedReader reader = new BufferedReader(is);
			StringBuilder htmlBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
			    htmlBuilder.append(line).append(' ');
			}
			
			final PackageInfo packageInfo =
					getPackageManager().getPackageInfo(getClass().getPackage().getName(), 0);
			final String versionName = packageInfo.versionName;
			final String html = htmlBuilder.toString();
			aboutView.loadData(
				String.format(html, versionName),
				"text/html", "utf-8");
		} catch (Exception e) {
			Log.wtf(TAG, "Could not get version info??", e);
			Toast.makeText(this, "Sorry, unable to display About information", Toast.LENGTH_LONG).show();
		}
		
	}
	
	public void done(@SuppressWarnings("unused") View v) {
		finish();
	}
}
