package cz.vitskalicky.lepsirozvrh.activity;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import cz.vitskalicky.lepsirozvrh.R;
import cz.vitskalicky.lepsirozvrh.SharedPrefs;
import cz.vitskalicky.lepsirozvrh.bakaAPI.Login;
import cz.vitskalicky.lepsirozvrh.schoolsDatabase.SchoolsListFragment;
import javassist.bytecode.analysis.Util;

public class LoginActivity extends AppCompatActivity {
    public static final String TAG = LoginActivity.class.getSimpleName();
    /**
     * when you want this activity to log out as soon as it starts, pass this in intent extras.
     */
    public static final String LOGOUT = "logout";

    public static final int REQUEST_PICK_SCHOOL = 64585; //random number

    TextInputLayout tilUsername;
    TextInputLayout tilPassword;
    TextInputLayout tilURL;
    Button bChooseSchool;
    Button bLogin;
    ProgressBar progressBar;
    TextView twMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilUsername = findViewById(R.id.textInputLayoutName);
        tilPassword = findViewById(R.id.textInputLayoutPassword);
        tilURL = findViewById(R.id.textInputLayoutURL);
        bChooseSchool = findViewById(R.id.buttonSchoolList);
        bLogin = findViewById(R.id.buttonLogin);
        progressBar = findViewById(R.id.progressBar);
        twMessage = findViewById(R.id.textViewMessage);

        tilUsername.setErrorEnabled(true);
        tilPassword.setErrorEnabled(true);
        tilURL.setErrorEnabled(true);

        progressBar.setVisibility(View.GONE);
        twMessage.setText("");

        tilUsername.getEditText().setText(SharedPrefs.getString(this, SharedPrefs.USERNAME));
        tilURL.getEditText().setText(SharedPrefs.getString(this, SharedPrefs.URL));

        if (getIntent().getBooleanExtra(LOGOUT, false)){
            Login.logout(this);
        }

        bChooseSchool.setOnClickListener(v -> {
            Intent intent = new Intent(this, SchoolsListActivity.class);
            startActivityForResult(intent, REQUEST_PICK_SCHOOL);
        });

        bLogin.setOnClickListener(v -> {
            bLogin.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            twMessage.setText("");

            /*tilUsername.setError(null);
            tilPassword.setError(null);
            tilURL.setError(null);*/
            tilUsername.setErrorEnabled(false);
            tilPassword.setErrorEnabled(false);
            tilURL.setErrorEnabled(false);

            if (tilURL.getEditText().getText().toString().trim().equals("")){
                tilURL.setError(getText(R.string.enter_url));
                bLogin.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                return;
            }
            if (tilUsername.getEditText().getText().toString().trim().equals("")){
                tilUsername.setError(getText(R.string.enter_username));
                bLogin.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                return;
            }

            Login.login(tilURL.getEditText().getText().toString(), tilUsername.getEditText().getText().toString(), tilPassword.getEditText().getText().toString(), (code, data) -> {
                if (code == Login.SUCCESS){
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
                bLogin.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                if (code == Login.WRONG_USERNAME){
                    tilUsername.setError(getText(R.string.invalid_username));
                }
                if (code == Login.WRONG_PASSWORD){
                    tilPassword.setError(getText(R.string.invalid_password));
                }
                if (code == Login.SERVER_UNREACHABLE){
                    twMessage.setText(R.string.unreachable);
                    tilURL.setError(" ");
                }
                if (code == Login.UNEXPECTER_RESPONSE){
                    tilURL.setError(getText(R.string.unexpected_response));
                }
                if (code == Login.ROZVRH_DISABLED){
                    tilURL.setError(" ");
                    twMessage.setText(R.string.schedule_disabled);
                }

            }, this);
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_SCHOOL && resultCode == SchoolsListActivity.RESULT_OK && data != null){
            String url = data.getStringExtra(SchoolsListActivity.EXTRA_URL);
            if (url != null){
                tilURL.getEditText().setText(url);
            }else {
                Log.e(TAG, "No extra containing url (extra key: " + SchoolsListActivity.EXTRA_URL + ")");
            }
        }
    }
}
