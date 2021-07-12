package org.odk.collect.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;
import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;

import org.odk.collect.android.preferences.keys.MetaKeys;
import org.odk.collect.android.preferences.source.SettingsProvider;
import org.odk.collect.android.projects.ProjectImporter;
import org.odk.collect.shared.Settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import org.odk.collect.android.injection.DaggerUtils;

public class MainActivity extends CollectAbstractActivity {

    View loginLayout;
    View landingLayout;

    SettingsProvider settingsProvider=DaggerUtils.getComponent(Collect.getInstance()).settingsProvider();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        configureLandingLayout();
        configureLoginLayout();

        String odk_server_url = settingsProvider.getGeneralSettings().getString("support_api_token");
        showLogin(odk_server_url == null);
    }

    private void showLogin(boolean state) {
        loginLayout.setVisibility(View.INVISIBLE);
        landingLayout.setVisibility(View.INVISIBLE);
        if (state) {
            loginLayout.setVisibility(View.VISIBLE);
        } else {
            landingLayout.setVisibility(View.VISIBLE);
        }
    }

    private void configureLandingLayout() {

        landingLayout = findViewById(R.id.landingLayout);
        findViewById(R.id.option2).setOnClickListener(v -> startActivity(
                new Intent(this, MainMenuActivity.class)
        ));
        findViewById(R.id.option3).setOnClickListener(v -> {
            DaggerUtils.getComponent(Collect.getInstance()).settingsProvider().clearAll();
            startActivity(new Intent(this, SplashScreenActivity.class));
        });
    }

    private void configureLoginLayout() {
        loginLayout = findViewById(R.id.loginLayout);
        EditText loginUser = findViewById(R.id.loginUser);
        EditText loginPassword = findViewById(R.id.loginPassword);
        TextView loginError = findViewById(R.id.loginError);
        TextView loginButtonText = findViewById(R.id.loginButtonText);
        View loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(v -> {
            HandlerThread ht = new HandlerThread("MyHandlerThread");
            ht.start();
            Handler handler = new Handler(ht.getLooper());
            Runnable runnable = () -> {
                runOnUiThread(() -> {
                    loginUser.setEnabled(false);
                    loginPassword.setEnabled(false);
                    loginError.setText("");
                    loginButtonText.setText("Ingresando");

                });
                String username=loginUser.getText().toString();
                String password=loginPassword.getText().toString();
                String server_url="https://kc.nexion-dev.tk";
                HashMap<String, String> body = new HashMap<>();
                body.put("username",username );
                body.put("password", password);
                try {
                    JSONObject response = performPostCall("https://support.nexion-dev.tk/login", body);
                    String support_api_token = response.getString("authToken");
                    settingsProvider.getGeneralSettings().save("support_api_token", support_api_token);
                    settingsProvider.getGeneralSettings().save("server_url", server_url);
                    settingsProvider.getGeneralSettings().save("username", username);
                    settingsProvider.getGeneralSettings().save("password", password);
                    runOnUiThread(() -> {
                        showLogin(false);
                        loginUser.setText("");
                        loginPassword.setText("");
                        loginError.setText("");
                        loginUser.setEnabled(true);
                        loginPassword.setEnabled(true);
                        loginButtonText.setText("Ingresar");
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        loginError.setText(e.toString());
                        loginUser.setEnabled(true);
                        loginPassword.setEnabled(true);
                        loginButtonText.setText("Ingresar");
                    });
                }
            };
            handler.post(runnable);

        });

    }

    private JSONObject performPostCall(
            String requestURL,
            HashMap<String, String> postDataParams
    ) throws Exception {

        URL url;
        StringBuilder response = new StringBuilder();
        url = new URL(requestURL);


        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(15000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        JSONObject jsonParam = new JSONObject();

        for (Map.Entry<String, String> entry : postDataParams.entrySet()) {
            jsonParam.put(entry.getKey(), entry.getValue());
        }

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.write(jsonParam.toString());

        writer.flush();
        writer.close();
        os.close();
        int responseCode = conn.getResponseCode();

        if (responseCode == HttpsURLConnection.HTTP_OK) {
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        } else {
            throw new Exception("Error en el servidor: " + responseCode);
        }
        return new JSONObject(response.toString());
    }
}
