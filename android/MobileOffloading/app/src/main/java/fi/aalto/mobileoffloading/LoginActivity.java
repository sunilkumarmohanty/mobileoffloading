package fi.aalto.mobileoffloading;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.io.IOException;
import java.net.HttpURLConnection;

import fi.aalto.mobileoffloading.api.RemoteOcrClient;
import fi.aalto.mobileoffloading.api.RemoteOcrService;
import fi.aalto.mobileoffloading.models.FacebookLoginData;
import fi.aalto.mobileoffloading.models.LoginData;
import retrofit2.Call;
import retrofit2.Response;

/**
 * A login screen that offers login via email/password and facebook.
 */
public class LoginActivity extends AppCompatActivity{

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mLoginView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private LoginButton fbLoginButton;
    CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mLoginView = (EditText) findViewById(R.id.user);
        mPasswordView = (EditText) findViewById(R.id.password);
//        if(AccessToken.getCurrentAccessToken() != null)
//        {
//            OpenMainActivity();
//        }
//        else
//        {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String token = sharedPref.getString("TOKEN", "");
            if(!token.equals(""))
            {
                OpenMainActivity();
            }
//        }
        callbackManager = CallbackManager.Factory.create();
        fbLoginButton = (LoginButton) findViewById(R.id.fblogin_button);
        fbLoginButton.setReadPermissions("email");
        // Facebook Callback registration
        fbLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                Log.d("Access Token : " , loginResult.getAccessToken().getToken());
                //Log.d("User ID : ",loginResult.getAccessToken().getUserId());
                showProgress(true);
                mAuthTask = new UserLoginTask("", "",loginResult.getAccessToken().getToken());
                mAuthTask.execute((Void) null);

            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });
        Log.d("Login : ","Initialized");
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.user || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    /***
     * Opens the Main Activity
     */
    private void OpenMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        LoginActivity.this.startActivity(intent);
        LoginActivity.this.finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mLoginView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mLoginView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mLoginView.setError(getString(R.string.error_field_required));
            focusView = mLoginView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.

            showProgress(true);
            mAuthTask = new UserLoginTask(email, password,"");
            mAuthTask.execute((Void) null);
        }
    }

    /***
     * Checks for password validity at clietn side
     * @param password - password
     * @return isValid
     */
    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private final String mFacebookToken;
        UserLoginTask(String email, String password, String facebookToken) {
            mEmail = email;
            mPassword = password;
            mFacebookToken = facebookToken;
        }

        /***
         * Logs in if already loggedin else makes API call
         * @param params
         * @return
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String token="";
                if(mFacebookToken.length()!= 0) {
                    try {
                        FacebookLoginData fbloginData = new FacebookLoginData(mFacebookToken);
                        RemoteOcrService remoteAppsService =
                                new RemoteOcrClient(getApplicationContext()).getRemoteOcrService();
                        Call<FacebookLoginData> loginCall = remoteAppsService.facebooklogin(fbloginData);
                        Response<FacebookLoginData> response = loginCall.execute();
                        Log.d("CODE", response.code() + "");

                        if (response.code() != HttpURLConnection.HTTP_OK) {
                            LoginManager.getInstance().logOut();
                            return false;
                        }
                        if (response.body() != null) {
                            Log.d("Response", response.body().toString());
                            token = response.body().token;
                            SharedPreferences.Editor sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                            sharedPref.putString("TOKEN", token).apply();
                            sharedPref.commit();
                            Log.d("token", token);
                        }
                    }catch(Exception ex)
                    {
                        LoginManager.getInstance().logOut();
                    }
                }
                else
                {
                    LoginData loginData = new LoginData(mEmail, mPassword);
                    RemoteOcrService remoteAppsService =
                            new RemoteOcrClient(getApplicationContext()).getRemoteOcrService();
                    Call<LoginData> loginCall = remoteAppsService.login(loginData);
                    Response<LoginData> response = loginCall.execute();
                    Log.d("CODE", response.code() + "");

                    if (response.code() != HttpURLConnection.HTTP_OK) {
                        return false;
                    }
                    if (response.body() != null) {
                        Log.d("Response", response.body().toString());
                        token = response.body().token;
                        SharedPreferences.Editor sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                        sharedPref.putString("TOKEN", token).apply();
                        sharedPref.commit();
                        Log.d("token", token);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            return true;

        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                OpenMainActivity();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}