package cz.vitskalicky.lepsirozvrh.settings;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import cz.vitskalicky.lepsirozvrh.BuildConfig;
import cz.vitskalicky.lepsirozvrh.MainApplication;
import cz.vitskalicky.lepsirozvrh.R;
import cz.vitskalicky.lepsirozvrh.SharedPrefs;
import cz.vitskalicky.lepsirozvrh.Utils;
import cz.vitskalicky.lepsirozvrh.activity.LicencesActivity;
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification;
import cz.vitskalicky.lepsirozvrh.whatsnew.WhatsNewFragment;
import io.sentry.Sentry;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    private Utils.Listener logoutListener = () -> {
    };

    private Utils.Listener shownThemeSettingsListener = () -> {};

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findPreference(getString(R.string.PREFS_LOGOUT)).setOnPreferenceClickListener(preference -> {
            logoutListener.method();
            return true;
        });

        findPreference(getString(R.string.PREFS_APP_THEME_SCREEN)).setOnPreferenceClickListener(preference -> {
            shownThemeSettingsListener.method();
            return true;
        });

        SwitchPreferenceCompat sendCrashReportsPreference = findPreference(getString(R.string.PREFS_SEND_CRASH_REPORTS));
        //Crash reports are allowed on official release builds only. (see build.gradle)
        if (BuildConfig.ALLOW_SENTRY){
            sendCrashReportsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue instanceof Boolean && getActivity() != null) {
                    boolean value = (boolean) newValue;
                    if (value) {
                        ((MainApplication) getActivity().getApplication()).enableSentry();
                    } else {
                        ((MainApplication) getActivity().getApplication()).diableSentry();
                    }
                }
                return true;
            });
            sendCrashReportsPreference.setVisible(true);
        }else {
            sendCrashReportsPreference.setVisible(false);
        }

        findPreference(getString(R.string.PREFS_OSS_LICENCES)).setOnPreferenceClickListener(preference -> {
            Intent i = new Intent(getContext(), LicencesActivity.class);
            startActivity(i);
            return true;
        });

        findPreference(getString(R.string.PREFS_SEND_FEEDBACK)).setOnPreferenceClickListener(preference -> {
            AlertDialog ad = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.include_schedule)
                    .setMessage(R.string.include_schedule_desc)
                    .setNegativeButton(R.string.no, (dialog, which) -> Utils.sendFeedback(false, null, getContext(), getView()))
                    .setPositiveButton(R.string.yes, (dialog, which) -> Utils.sendFeedback(true,null, getContext(), getView()))
                    .setOnCancelListener(dialog -> Utils.sendFeedback(false,null, getContext(), getView()))
                    .create();
            ad.show();
            return true;
        });

        Preference userInfo = findPreference(getString(R.string.PREFS_USER));
        userInfo.setTitle(SharedPrefs.getString(getContext(), SharedPrefs.NAME));
        String type = SharedPrefs.getString(getContext(), SharedPrefs.TYPE);
        switch (type) {
            case "Z":
                userInfo.setSummary(R.string.student);
                break;
            case "R":
                userInfo.setSummary(R.string.parent);
                break;
            case "U":
                userInfo.setSummary(R.string.teacher);
                break;
        }

        ListPreference switchToNextWeek = findPreference(getString(R.string.PREFS_SWITCH_TO_NEXT_WEEK));
        switchToNextWeek.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());

        SwitchPreferenceCompat notificationPreference = findPreference(getString(R.string.PREFS_NOTIFICATION));
        notificationPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((Boolean) newValue) {
                PermanentNotification.showInfoDialog(getContext(), false);
                ((MainApplication) getContext().getApplicationContext()).enableNotification();
            } else {
                ((MainApplication) getContext().getApplicationContext()).disableNotification();
            }
            return true;
        });

        Preference appVersionPreference = findPreference(getString(R.string.PREFS_APP_VERSION));
        String versionText = BuildConfig.FLAVOR + "-" + BuildConfig.BUILD_TYPE + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.GitHash + ")";
        appVersionPreference.setSummary(versionText);
        appVersionPreference.setOnPreferenceClickListener(preference -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(versionText, versionText);
            clipboard.setPrimaryClip(clip);
            Snackbar.make(getView(), R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show();
            return true;
        });

        findPreference(getString(R.string.PREFS_CHANGELOG)).setOnPreferenceClickListener(preference -> {
            WhatsNewFragment whatsNewFragment = new WhatsNewFragment();
            whatsNewFragment.show(getActivity().getSupportFragmentManager(), "dialog");
            return true;
        });
    }

    public void setLogoutListener(Utils.Listener listener) {
        this.logoutListener = listener;
    }
    public void setShownThemeSettingsListener(Utils.Listener listener){this.shownThemeSettingsListener = listener; }

}
