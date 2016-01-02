package todomore.android;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


/**
 * A preferences screen that offers login via username/password and other settings
 */
public class LoginCredentialsActivity extends Activity {

	private SharedPreferences prefs;

    // UI references.
    private EditText mUsernameTF;
    private EditText mPasswordTF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Set up the login form.
        mUsernameTF = (EditText) findViewById(R.id.username);
        mUsernameTF.setText(prefs.getString("KEY_USERNAME", null));

        mPasswordTF = (EditText) findViewById(R.id.password);
        mPasswordTF.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    validateCreds();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                validateCreds();
            }
        });
    }

    /**
     * Attempts to validate the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void validateCreds() {

        // Reset errors.
        mUsernameTF.setError(null);
        mPasswordTF.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameTF.getText().toString();
        String password = mPasswordTF.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid username .
        if (TextUtils.isEmpty(username)) {
            mUsernameTF.setError(getString(R.string.error_field_required));
            focusView = mUsernameTF;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mUsernameTF.setError(getString(R.string.error_invalid_user));
            focusView = mUsernameTF;
            cancel = true;
        }
        
        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            mPasswordTF.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordTF;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
        	// email is syntactically valid so save it
        	prefs.edit().putString("KEY_USERNAME", username).commit();
        	// Do NOT save the password as there's no good way to do so securely
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
        	AppSingleton app = AppSingleton.getInstance();
        	app.setUserName(username);
        	app.setPassword(password);
        	finish();
        }
    }

    private boolean isUsernameValid(String name) {
        return name.matches("[\\w.]{2,}");
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }
}



