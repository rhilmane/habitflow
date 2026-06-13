package com.example.myapplication.activities;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.dao.UserDao;
import com.example.myapplication.models.User;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.PasswordUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ForgotPasswordActivity extends AppCompatActivity {

    private UserDao userDao;

    private TextInputLayout emailLayout;
    private TextInputEditText emailInput;
    private MaterialButton btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        userDao = new UserDao(this);

        emailLayout = findViewById(R.id.email_layout);
        emailInput  = findViewById(R.id.email_edit_text);
        btnSend     = findViewById(R.id.btn_send_reset);

        btnSend.setOnClickListener(v -> attemptReset());
        findViewById(R.id.btn_back_to_login).setOnClickListener(v -> finish());
    }

    private void attemptReset() {
        emailLayout.setError(null);
        String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Entrez un email valide");
            return;
        }

        btnSend.setEnabled(false);
        AppExecutors.io().execute(() -> {
            User user = userDao.findByEmail(email);
            AppExecutors.main().execute(() -> {
                btnSend.setEnabled(true);
                if (user == null) {
                    emailLayout.setError("Aucun compte associé à cet email");
                } else if (user.securityQuestion == null || user.securityAnswer == null) {
                    // Ancien compte sans question secrète → reset direct
                    showNewPasswordDialog(user);
                } else {
                    showSecurityQuestionDialog(user);
                }
            });
        });
    }

    /** Étape 2 : poser la question secrète et vérifier la réponse. */
    private void showSecurityQuestionDialog(User user) {
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);

        TextView tvQuestion = new TextView(this);
        tvQuestion.setText(user.securityQuestion);
        tvQuestion.setPadding(pad, pad / 2, pad, pad / 2);

        EditText answerInput = new EditText(this);
        answerInput.setHint("Votre réponse");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, 0, pad, 0);
        container.addView(tvQuestion);
        container.addView(answerInput);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Question secrète")
                .setView(container)
                .setPositiveButton("Vérifier", null)
                .setNegativeButton("Annuler", null)
                .create();

        dialog.setOnShowListener(d ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String answer = answerInput.getText().toString().trim().toLowerCase();
                if (TextUtils.isEmpty(answer)) {
                    answerInput.setError("Entrez votre réponse");
                    return;
                }
                String answerHash = PasswordUtil.hash(answer);
                if (answerHash.equals(user.securityAnswer)) {
                    dialog.dismiss();
                    showNewPasswordDialog(user);
                } else {
                    answerInput.setError("Réponse incorrecte");
                    answerInput.setText("");
                }
            })
        );

        dialog.show();
    }

    /** Étape 3 : saisir et confirmer le nouveau mot de passe. */
    private void showNewPasswordDialog(User user) {
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);

        EditText pass1 = new EditText(this);
        pass1.setHint("Nouveau mot de passe");
        pass1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        EditText pass2 = new EditText(this);
        pass2.setHint("Confirmer le mot de passe");
        pass2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(pass1);
        container.addView(pass2);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Nouveau mot de passe")
                .setMessage("Définissez un nouveau mot de passe pour " + user.email)
                .setView(container)
                .setPositiveButton("Enregistrer", null)
                .setNegativeButton("Annuler", null)
                .create();

        dialog.setOnShowListener(d ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String p1 = pass1.getText().toString();
                String p2 = pass2.getText().toString();
                if (p1.length() < 6) {
                    pass1.setError("6 caractères minimum");
                    return;
                }
                if (!p1.equals(p2)) {
                    pass2.setError("Les mots de passe ne correspondent pas");
                    return;
                }
                resetPassword(user, p1, dialog);
            })
        );

        dialog.show();
    }

    private void resetPassword(User user, String newPassword, AlertDialog dialog) {
        String hash = PasswordUtil.hash(newPassword);
        AppExecutors.io().execute(() -> {
            user.passwordHash = hash;
            userDao.update(user);
            AppExecutors.main().execute(() -> {
                dialog.dismiss();
                android.widget.Toast.makeText(this,
                        "Mot de passe mis à jour. Veuillez vous reconnecter.",
                        android.widget.Toast.LENGTH_LONG).show();
                finish();
            });
        });
    }
}
