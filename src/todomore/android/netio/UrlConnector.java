package todomore.android.netio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import android.util.Log;

/**
 * Send a request to a URL and read its response data from a URLConnection
 */
public class UrlConnector {
	
    private final static String TAG = UrlConnector.class.getSimpleName();
    
    public static String converse(URL url, String postBody, Map<String,String> headers) {
    	try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			boolean isPost = postBody != null;
			Log.d(TAG, String.format("converse: %s %s", url, isPost?"POST":"GET"));
			if (isPost) {
				conn.setRequestMethod("POST");
				conn.setDoInput(true);
			}
			// conn.setDoOutput(true);
			// conn.setAllowUserInteraction(true);
			
			if (headers != null) {
				for (String s : headers.keySet()) {
					conn.setRequestProperty(s, headers.get(s));
				}
			}
			
			conn.connect();

			if (isPost) {
				PrintWriter out = new PrintWriter(conn.getOutputStream());
				out.println(postBody);
				out.close();			// Important!
			}

			StringBuilder sb = new StringBuilder();
			BufferedReader in = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			String line;

			while ((line = in.readLine()) != null) {
			    sb.append(line);
			}

			in.close();
			return sb.toString();

		} catch (IOException e) {
			throw new RuntimeException("converse() failure: " + e, e);
		}
    }
}
