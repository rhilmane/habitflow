package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.dao.UserDao;
import com.example.myapplication.models.User;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.PasswordUtil;
import com.example.myapplication.util.SessionManager;
import com.example.myapplication.util.SupabaseClient;
import com.example.myapplication.util.SyncManager;

import org.json.JSONArray;
import org.json.JSONException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private UserDao userDao;
    private SessionManager session;

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userDao = new UserDao(this);
        session = new SessionManager(this);

        emailLayout = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);
        emailInput = findViewById(R.id.email_edit_text);
        passwordInput = findViewById(R.id.password_edit_text);
        btnLogin = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> attemptLogin());

        findViewById(R.id.tv_sign_up).setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        findViewById(R.id.tv_forgot_password)
                .setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));

    }

    private void attemptLogin() {
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
        String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Enter your password");
            return;
        }

        btnLogin.setEnabled(false);
        String hash = PasswordUtil.hash(password);

        AppExecutors.io().execute(() -> {
            User user = userDao.login(email, hash);

            // Fallback: ila ma lqach mahallyan, jreb Supabase
            if (user == null && SyncManager.isNetworkAvailable(this)) {
                user = fetchFromSupabase(email, hash);
                if (user != null) {
                    userDao.insert(user); // khzen mahallyan
                }
            }

            final User finalUser = user;
            AppExecutors.main().execute(() -> {
                btnLogin.setEnabled(true);
                if (finalUser != null) {
                    session.saveSession(finalUser.id, finalUser.name, finalUser.email);
                    goToMain();
                } else {
                    passwordLayout.setError("Invalid email or password");
                }
            });
        });
    }

    private User fetchFromSupabase(String email, String hash) {
        String json = SupabaseClient.fetch("users",
                "email=eq." + email + "&password_hash=eq." + hash + "&limit=1");
        if (json == null) return null;
        try {
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) return null;
            org.json.JSONObject obj = arr.getJSONObject(0);
            User u = new User();
            u.id             = obj.getLong("id");
            u.name           = obj.optString("name", "");
            u.email          = obj.optString("email", "");
            u.passwordHash   = obj.optString("password_hash", "");
            u.avatarUrl      = obj.optString("avatar_url", null);
            u.createdAt      = obj.optLong("created_at", System.currentTimeMillis());
            u.securityQuestion = obj.optString("security_question", null);
            u.securityAnswer   = obj.optString("security_answer", null);
            return u;
        } catch (JSONException e) {
            return null;
        }
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
