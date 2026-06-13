package com.example.myapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.dao.UserDao;
import com.example.myapplication.models.User;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.PasswordUtil;
import com.example.myapplication.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private static final String[] QUESTIONS = {
        "Quel est le nom de votre premier animal de compagnie ?",
        "Quel est le nom de jeune fille de votre mère ?",
        "Quelle était votre école primaire ?",
        "Dans quelle ville êtes-vous né(e) ?",
        "Quel est le prénom de votre meilleur(e) ami(e) d'enfance ?",
        "Quel est le surnom de votre grand-mère ?",
        "Quelle est votre équipe de sport préférée ?"
    };

    private UserDao userDao;
    private SessionManager session;

    private TextInputLayout nameLayout, emailLayout, passwordLayout;
    private TextInputEditText nameInput, emailInput, passwordInput;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        userDao = new UserDao(this);
        session = new SessionManager(this);

        nameLayout     = findViewById(R.id.name_layout);
        emailLayout    = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);
        nameInput      = findViewById(R.id.name_edit_text);
        emailInput     = findViewById(R.id.email_edit_text);
        passwordInput  = findViewById(R.id.password_edit_text);
        btnRegister    = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> attemptRegister());
        findViewById(R.id.tv_sign_in).setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String name     = nameInput.getText()     == null ? "" : nameInput.getText().toString().trim();
        String email    = emailInput.getText()    == null ? "" : emailInput.getText().toString().trim();
        String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Entrez votre nom");
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Entrez un email valide");
            return;
        }
        if (password.length() < 6) {
            passwordLayout.setError("Mot de passe : 6 caractères minimum");
            return;
        }

        btnRegister.setEnabled(false);
        String hash = PasswordUtil.hash(password);

        AppExecutors.io().execute(() -> {
            User existing = userDao.findByEmail(email);
            AppExecutors.main().execute(() -> {
                btnRegister.setEnabled(true);
                if (existing != null) {
                    emailLayout.setError("Email déjà utilisé");
                } else {
                    showSecurityQuestionDialog(name, email, hash);
                }
            });
        });
    }

    private void showSecurityQuestionDialog(String name, String email, String hash) {
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);

        // Spinner questions
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, QUESTIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // EditText réponse
        EditText answerInput = new EditText(this);
        answerInput.setHint("Votre réponse");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(spinner);
        container.addView(answerInput);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Question secrète")
                .setMessage("Choisissez une question pour sécuriser votre compte.")
                .setView(container)
                .setPositiveButton("Confirmer", null)
                .setNegativeButton("Annuler", (d, w) -> {})
                .create();

        dialog.setOnShowListener(d ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String answer = answerInput.getText().toString().trim();
                if (TextUtils.isEmpty(answer)) {
                    answerInput.setError("Entrez une réponse");
                    return;
                }
                String question   = (String) spinner.getSelectedItem();
                String answerHash = PasswordUtil.hash(answer.toLowerCase());
                dialog.dismiss();
                saveUser(name, email, hash, question, answerHash);
            })
        );

        dialog.show();
    }

    private void saveUser(String name, String email, String hash,
                          String question, String answerHash) {
        btnRegister.setEnabled(false);
        AppExecutors.io().execute(() -> {
            User user = new User(name, email, hash);
            user.securityQuestion = question;
            user.securityAnswer   = answerHash;
            long id = userDao.insert(user);
            AppExecutors.main().execute(() -> {
                btnRegister.setEnabled(true);
                if (id > 0) {
                    session.saveSession(id, name, email);
                    goToMain();
                } else {
                    emailLayout.setError("Impossible de créer le compte, réessayez");
                }
            });
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
