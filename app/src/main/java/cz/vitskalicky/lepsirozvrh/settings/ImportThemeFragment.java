package cz.vitskalicky.lepsirozvrh.settings;

import android.app.Activity;
import android.content.ClipboardManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;

import cz.vitskalicky.lepsirozvrh.R;
import cz.vitskalicky.lepsirozvrh.SharedPrefs;
import cz.vitskalicky.lepsirozvrh.Utils;
import cz.vitskalicky.lepsirozvrh.donations.Donations;
import cz.vitskalicky.lepsirozvrh.theme.Theme;
import cz.vitskalicky.lepsirozvrh.theme.ThemeData;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImportThemeFragment extends Fragment {

    private Donations donations;

    private View root;
    private EditText editTextData;
    private String preloadedString = "";
    private Button buttonPaste;
    private Button buttonOK;
    private Button buttonClear;
    public ImportThemeFragment() {
        // Required empty public constructor
    }

    public void init(Donations donations){
        this.donations = donations;
        updateDonationsStatus();
    }

    public void setString(String preloadedString) {
        this.preloadedString = preloadedString;
        if (editTextData != null) {
            editTextData.setText(preloadedString);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (root == null) {
            root = inflater.inflate(R.layout.fragment_import_theme, container, false);
            editTextData = root.findViewById(R.id.editTextData);
            buttonPaste = root.findViewById(R.id.buttonPaste);
            buttonOK = root.findViewById(R.id.buttonOK);
            buttonClear = root.findViewById(R.id.buttonClear);
        }

        buttonPaste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
            if (clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemCount() > 0) {
                editTextData.setText(clipboard.getPrimaryClip().getItemAt(0).getText());
            }
        });
        buttonOK.setOnClickListener(this::doImport);
        buttonClear.setOnClickListener(v -> editTextData.setText(""));
        updateDonationsStatus();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!preloadedString.isEmpty()){
            editTextData.setText(preloadedString);
            preloadedString = "";
        }
    }

    private void doImport(View v){
        AsyncTask.execute(() -> {
            ThemeData td = null;
            String input = editTextData.getText().toString().trim();
            try {
                td = ThemeData.parseZipped(input);
            } catch (IOException e) {
                //try to parse as json (not zipped)
                try {
                    td = ThemeData.parseJson(input);
                } catch (IOException ex) {
                    //try to parse as base64, but not url-safe
                    input = input.replace('+','-').replace('/','_');
                    try {
                        td = ThemeData.parseZipped(input);
                    } catch (IOException ignored) {
                    }
                }
            }
            final ThemeData ftd = td;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (ftd != null) {
                    Theme.of(getContext()).setThemeData(ftd);
                    SharedPrefs.setIntPreference(getContext(), R.string.PREFS_DETAIL_LEVEL, 3);

                    Activity activity = getActivity();
                    if (activity instanceof Utils.RecreateWithAnimationActivity) {
                        ((Utils.RecreateWithAnimationActivity) activity).recreateWithAnimation();
                    }
                } else {
                    Snackbar s = Snackbar.make(root, R.string.import_invalid, Snackbar.LENGTH_LONG);
                    s.show();
                }
            });
        });
    }

    public void updateDonationsStatus(){
        if (donations != null && root != null){
            buttonOK.setEnabled(donations.isSponsor());
            if (donations.isSponsor()){
                buttonOK.setOnClickListener(this::doImport);
            }else {
                buttonOK.setOnClickListener(v -> donations.showDialog());
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
