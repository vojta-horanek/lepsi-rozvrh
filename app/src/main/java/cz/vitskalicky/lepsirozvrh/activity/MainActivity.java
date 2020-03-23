package cz.vitskalicky.lepsirozvrh.activity;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.jaredrummler.cyanea.app.CyaneaAppCompatActivity;
import com.jaredrummler.cyanea.utils.ColorUtils;

import cz.vitskalicky.lepsirozvrh.AppSingleton;
import cz.vitskalicky.lepsirozvrh.BuildConfig;
import cz.vitskalicky.lepsirozvrh.DisplayInfo;
import cz.vitskalicky.lepsirozvrh.R;
import cz.vitskalicky.lepsirozvrh.SharedPrefs;
import cz.vitskalicky.lepsirozvrh.bakaAPI.Login;
import cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.RozvrhAPI;
import cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.RozvrhCache;
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification;
import cz.vitskalicky.lepsirozvrh.settings.SettingsActivity;
import cz.vitskalicky.lepsirozvrh.theme.Themator;
import cz.vitskalicky.lepsirozvrh.view.RozvrhTableFragment;
import cz.vitskalicky.lepsirozvrh.whatsnew.WhatsNewFragment;

public class MainActivity extends CyaneaAppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG_TIMER = TAG + "-timer";

    public static final String EXTRA_JUMP_TO_TODAY = MainActivity.class.getCanonicalName() + ".JUMP_TO_TODAY";

    Context context = this;

    Toolbar bottomAppBar;

    RozvrhTableFragment rtFragment;

    ImageButton ibSettings;
    ImageButton ibPrev;
    ImageButton ibCurrent;
    ImageButton ibPermanent;
    ImageButton ibNext;
    ImageButton ibRefresh;
    ProgressBar progressBar;

    RozvrhAPI rozvrhAPI;

    TextView infoLine;
    DisplayInfo displayInfo;

    int week = 0;

    boolean showedNotiInfo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkLogin();

        rozvrhAPI = AppSingleton.getInstance(context).getRozvrhAPI();

        bottomAppBar = findViewById(R.id.toolbar);
        setSupportActionBar(bottomAppBar);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(false);
        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowCustomEnabled(false);

        displayInfo = new DisplayInfo();
        infoLine = findViewById(R.id.infoLine);
        displayInfo.addOnMessageChangeListener((oldMessage, newMessage) -> {
            setInfoText(newMessage);
            if (displayInfo.getErrorMessage() != null){
                TooltipCompat.setTooltipText(ibRefresh, displayInfo.getErrorMessage());
            }else {
                TooltipCompat.setTooltipText(ibRefresh, getText(R.string.refresh));
            }
        });

        rtFragment = (RozvrhTableFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        rtFragment.init(rozvrhAPI, displayInfo);

        ibSettings = findViewById(R.id.settings);
        ibPrev = findViewById(R.id.prev);
        ibCurrent = findViewById(R.id.curent);
        ibPermanent = findViewById(R.id.permanent);
        ibNext = findViewById(R.id.next);
        ibRefresh = findViewById(R.id.refresh);
        progressBar = findViewById(R.id.progressBar);

        rtFragment.createViews();

        ibSettings.setOnClickListener(view -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        TooltipCompat.setTooltipText(ibSettings, getText(R.string.settings));
        TooltipCompat.setTooltipText(ibPrev, getText(R.string.prev_week));
        TooltipCompat.setTooltipText(ibCurrent, getText(R.string.current_week));
        TooltipCompat.setTooltipText(ibPermanent, getText(R.string.permanent_schedule));
        TooltipCompat.setTooltipText(ibNext, getText(R.string.next_week));
        TooltipCompat.setTooltipText(ibRefresh, getText(R.string.refresh));
        ibPrev.setOnClickListener(v -> {
            week--;
            rtFragment.displayWeek(week, false);
            showHideButtons();
        });
        ibNext.setOnClickListener(v -> {
            week++;
            rtFragment.displayWeek(week, false);
            showHideButtons();
        });
        ibCurrent.setOnClickListener(v -> {
            week = 0;
            rtFragment.displayWeek(week, true);
            showHideButtons();
        });
        ibPermanent.setOnClickListener(v -> {
            week = Integer.MAX_VALUE;
            rtFragment.displayWeek(week, false);
            showHideButtons();
        });
        ibRefresh.setOnClickListener(v -> {
            rtFragment.refresh();
            //rtFragment.displayWeek(week);
        });
        displayInfo.addOnLoadingStateChangeListener((oldState, newState) -> {
            if (newState == DisplayInfo.LOADED){
                ibRefresh.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                ibRefresh.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_refresh_black_24));
            } else if (newState == DisplayInfo.ERROR) {
                ibRefresh.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                ibRefresh.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_refresh_problem_black_24dp));
            } else if (newState == DisplayInfo.LOADING){
                ibRefresh.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }
        });
/*
        ibSettings.setOnLongClickListener(v -> {
            SharedPrefs.setString(context, SharedPrefs.DISABLE_WTF_ROZVRH_UP_TO_DATE, Utils.dateToString(LocalDate.now()));
            return true;
        });*/

        Themator themator = new Themator(this);
        infoLine.setBackgroundColor(themator.getInfolineColor());
        infoLine.setTextColor(themator.getInfolineTextColor());
        infoLine.setTextSize(themator.getInfolineTextSize());

        rtFragment.createViews();
        if (savedInstanceState == null) {
            week = 0;
        }else {
            week = savedInstanceState.getInt(STATE_WEEK, 0);
            showedNotiInfo = savedInstanceState.getBoolean(STATE_SHOWE_NOTI_INFO, false);
        }

        int lastInterestingFeatureVersion = 12;
        String lastInterestingFeatureMessage = getString(R.string.interestig_widget);

        if (!SharedPrefs.contains(this, SharedPrefs.LAST_VERSION_SEEN) || (SharedPrefs.getInt(this, SharedPrefs.LAST_VERSION_SEEN) < lastInterestingFeatureVersion)){
            Snackbar snackbar = Snackbar.make(infoLine, lastInterestingFeatureMessage, Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.whats_new,view1 -> {
                WhatsNewFragment whatsNewFragment = new WhatsNewFragment();
                whatsNewFragment.show(getSupportFragmentManager(), "dialog");
            });
            snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            snackbar.setDuration(5000);

            snackbar.show();

            SharedPrefs.setInt(this, SharedPrefs.LAST_VERSION_SEEN, BuildConfig.VERSION_CODE);
        }


        showHideButtons();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ColorUtils.darker(themator.getRozvrhBgHeaderColor()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkLogin();

        if (!SharedPrefs.containsPreference(context, R.string.PREFS_SHOW_INFO_LINE) || SharedPrefs.getBooleanPreference(context, R.string.PREFS_SHOW_INFO_LINE)){
            infoLine.setVisibility(View.VISIBLE);
        }else {
            infoLine.setVisibility(View.GONE);
        }

        Intent intent = getIntent();
        boolean jumpToToday = intent.getBooleanExtra(EXTRA_JUMP_TO_TODAY,false);
        if (jumpToToday){
            rtFragment.displayWeek(0, true);
            intent.removeExtra(EXTRA_JUMP_TO_TODAY);
        }else {
            rtFragment.displayWeek(week, true);
        }
        boolean fromNotification = intent.getBooleanExtra(PermanentNotification.EXTRA_NOTIFICATION, false);
        intent.removeExtra(PermanentNotification.EXTRA_NOTIFICATION);
        if (fromNotification && !showedNotiInfo){
            PermanentNotification.showInfoDialog(context, false);
            showedNotiInfo = true;
        }
    }

    /**
     * shows/hides buttons accordingly to current state. My english is bad, but you got the point.
     */
    private void showHideButtons(){
        if (week == 0){
            ibPermanent.setVisibility(View.VISIBLE);
            ibCurrent.setVisibility(View.GONE);
        }else{
            ibPermanent.setVisibility(View.GONE);
            ibCurrent.setVisibility(View.VISIBLE);
        }
        if (week == Integer.MAX_VALUE){
            ibNext.setVisibility(View.GONE);
            ibPrev.setVisibility(View.GONE);
        } else {
            ibPrev.setVisibility(View.VISIBLE);
            ibNext.setVisibility(View.VISIBLE);
        }
    }

    private void setInfoText(String text){
        infoLine.setText(text);
    }

    public void checkLogin(){
        if (Login.checkLogin(this) != null){
            if (rozvrhAPI != null){
                rozvrhAPI.clearMemory();
            }
            finish();
        }
    }

    private static final String STATE_WEEK = "week";
    private static final String STATE_SHOWE_NOTI_INFO = "showed-noti-info";

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_WEEK, week);
        outState.putBoolean(STATE_SHOWE_NOTI_INFO, showedNotiInfo);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RozvrhCache.clearOldCache(context);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
