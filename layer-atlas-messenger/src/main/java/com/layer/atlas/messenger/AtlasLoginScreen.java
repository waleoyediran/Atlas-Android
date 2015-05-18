package com.layer.atlas.messenger;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerAuthenticationListener;

/**
 * @author Oleg Orlov
 * @since 24 Apr 2015
 */
public class AtlasLoginScreen extends Activity {
    private static final String TAG = AtlasLoginScreen.class.getSimpleName();
    private static final boolean debug = true;
    
    private volatile boolean inProgress = false;
    private EditText loginText;
    private EditText passwText;
    private View goBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.atlas_screen_login);
        
        final App101 app = (App101) getApplication();
        final LayerClient layerClient = app.getLayerClient();
        
        loginText = (EditText) findViewById(R.id.atlas_screen_login_username);
        passwText = (EditText) findViewById(R.id.atlas_screen_login_password);
        goBtn = findViewById(R.id.atlas_screen_login_go_btn);
        
        goBtn.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                final String login = loginText.getText().toString().trim();
                final String passw = passwText.getText().toString().trim();
                if (login.length() == 0 || passw.length() == 0) return;
                    
                inProgress = true;
                
                layerClient.registerAuthenticationListener(new LayerAuthenticationListener() {
                    public void onAuthenticationChallenge(final LayerClient client, final String nonce) {
                        if (debug) Log.w(TAG, "onAuthenticationChallenge() nonce: " + nonce);
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    final String[] eitAndAuth = app.requestToken(login, passw, nonce);
                                    String eit = eitAndAuth[0];
                                    String authToken = eitAndAuth[1];
                                    
                                    if (eit == null || eit.trim().length() == 0 || authToken == null || authToken.trim().length() == 0) {
                                        inProgress = false;
                                        v.post(new Runnable() {
                                            public void run() {
                                                if (eitAndAuth[2] != null) {
                                                    Toast.makeText(v.getContext(), eitAndAuth[2], Toast.LENGTH_LONG)
                                                        .show();
                                                }
                                                updateValues();
                                            }
                                        });
                                        return;
                                    }
                                    
                                    app.authToken = authToken;
                                    app.login = login;
                                    app.savePrefs();
                                    
                                    // 3. Submit identity token to Layer for validation
                                    layerClient.answerAuthenticationChallenge(eit);
                                    
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        
                    }
                    
                    public void onAuthenticated(LayerClient client, String userId) {
                        if (debug) Log.w(TAG, "onAuthenticated() userID: " + userId);
                        inProgress = false;
                        updateValues();
                        setResult(RESULT_OK);
                        finish();
                    }
                    
                    public void onDeauthenticated(LayerClient client) {
                        if (debug) Log.e(TAG, "onDeauthenticated() ");
                    }
                    
                    public void onAuthenticationError(LayerClient client, LayerException exception) {
                        Log.e(TAG, "onAuthenticationError() ", exception);
                    }
                    
                });
                
                layerClient.authenticate();
                
                updateValues();
            }
        });
        loginText.requestFocus();
    }
    
    private void updateValues() {
        goBtn.setEnabled(!inProgress);
    }
}
