package com.rr.CPing.Activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.rr.CPing.Adapters.PlatformAdapter;
import com.rr.CPing.Model.AtCoderUserDetails;
import com.rr.CPing.Model.CodeChefUserDetails;
import com.rr.CPing.Model.CodeForcesUserDetails;
import com.rr.CPing.Model.LeetCodeUserDetails;
import com.rr.CPing.Model.PlatformListItem;
import com.rr.CPing.R;
import com.rr.CPing.SharedPref.SharedPrefConfig;
import com.rr.CPing.Utils.NetworkChangeListener;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    NetworkChangeListener networkChangeListener = new NetworkChangeListener();

    // For making loading tasks in background
    boolean saveButtonClicked = false;
    int stillLoadingCount = 0;

    private LinearLayout platFormTitleLinearLayout, themeTitleLinearLayout;
    private ListView platformsListView;
    private ImageView platFormTitleDropDown, themeTitleDropDown;

    private RadioGroup themeRadioGroup;

    private PlatformAdapter platformAdapter;
    private Button settingsSaveButton;
    private EditText appUsernameEditText;
    private ProgressBar settingsProgressBar;
    private ArrayList<PlatformListItem> platformNamesList;
    private AlertDialog dialog;

    private RequestQueue requestQueue;

    @SuppressLint({"ResourceType", "NonConstantResourceId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAppTheme();
        setContentView(R.layout.activity_settings);

        if (SharedPrefConfig.readIsFirstTime(this)) {
            loadFirstTimeData();
            SharedPrefConfig.writePlatformsCount(this, 0);
        } else {
            loadData();
        }

        requestQueue = Volley.newRequestQueue(this);

        Toolbar dashBoardToolbar = findViewById(R.id.settings_page_toolbar);
        setSupportActionBar(dashBoardToolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Settings");
        dashBoardToolbar.setTitleTextColor(getResources().getColor(R.color.fontColor));

        dashBoardToolbar.setNavigationIcon(R.drawable.ic_back_button);
        dashBoardToolbar.setNavigationOnClickListener(v -> {
            settingsSaveButton.setVisibility(View.GONE);
            settingsProgressBar.setVisibility(View.VISIBLE);
            if (appUsernameEditText.getText().toString().isEmpty()) {
                Toast.makeText(this, "How should I call you?", Toast.LENGTH_SHORT).show();
            } else if (SharedPrefConfig.readPlatformsCount(this) == 0) {
                Toast.makeText(this, "No Platform is selected!", Toast.LENGTH_SHORT).show();
            } else {
                if (stillLoadingCount <= 0) {
                    startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                    finish();
                } else
                    Toast.makeText(this, "Settings are not yet saved!", Toast.LENGTH_SHORT).show();
            }
            settingsSaveButton.setVisibility(View.VISIBLE);
            settingsProgressBar.setVisibility(View.GONE);
        });

        findViewByIds();

        switch (SharedPrefConfig.readAppTheme(this)) {
            case -1:
                // System Default
                themeRadioGroup.check(R.id.system_default_theme_radio_button);
                break;
            case 0:
                // Light Theme
                themeRadioGroup.check(R.id.light_theme_radio_button);
                break;
            case 1:
                // Dark Theme
                themeRadioGroup.check(R.id.dark_theme_radio_button);
                break;
        }

        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.system_default_theme_radio_button:
                    // System Default
                    SharedPrefConfig.writeAppTheme(SettingsActivity.this, -1);
                    break;
                case R.id.light_theme_radio_button:
                    // Light Theme
                    SharedPrefConfig.writeAppTheme(SettingsActivity.this, 0);
                    break;
                case R.id.dark_theme_radio_button:
                    // Dark Theme
                    SharedPrefConfig.writeAppTheme(SettingsActivity.this, 1);
                    break;
            }
//            setAppTheme();
        });

        settingsProgressBar.setVisibility(View.GONE);
        settingsSaveButton.setVisibility(View.VISIBLE);

        platFormTitleLinearLayout.setOnClickListener(v -> {
            platFormTitleLinearLayout.setClickable(false);
            int angle;
            if (platformsListView.getVisibility() == View.GONE) {
                angle = 180;
                platformsListView.setVisibility(View.VISIBLE);
                if (themeRadioGroup.getVisibility() == View.VISIBLE) {
                    themeRadioGroup.setVisibility(View.GONE);
                    themeTitleDropDown.animate().setDuration(100).rotationBy(-180);
                }
            } else {
                angle = -180;
                platformsListView.setVisibility(View.GONE);
            }
            platFormTitleDropDown.animate().setDuration(100).rotationBy(angle).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    platFormTitleLinearLayout.setClickable(true);
                }
            });
        });

        themeTitleLinearLayout.setOnClickListener(v -> {
            themeTitleLinearLayout.setClickable(false);
            int angle;
            if (themeRadioGroup.getVisibility() == View.GONE) {
                angle = 180;
                themeRadioGroup.setVisibility(View.VISIBLE);
                if (platformsListView.getVisibility() == View.VISIBLE) {
                    platformsListView.setVisibility(View.GONE);
                    platFormTitleDropDown.animate().setDuration(100).rotationBy(-180);
                }
            } else {
                angle = -180;
                themeRadioGroup.setVisibility(View.GONE);
            }
            themeTitleDropDown.animate().setDuration(100).rotationBy(angle).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    themeTitleLinearLayout.setClickable(true);
                }
            });
        });

        settingsSaveButton.setOnClickListener(v -> {
            if (appUsernameEditText.getText().toString().isEmpty()) {
                Toast.makeText(SettingsActivity.this, "How should I call you?", Toast.LENGTH_SHORT).show();
            } else if (SharedPrefConfig.readPlatformsCount(SettingsActivity.this) == 0) {
                Toast.makeText(SettingsActivity.this, "No Platform is selected!", Toast.LENGTH_SHORT).show();
            } else {
                saveButtonClicked = true;

                settingsSaveButton.setVisibility(View.GONE);
                settingsProgressBar.setVisibility(View.VISIBLE);

                if (SharedPrefConfig.readIsFirstTime(SettingsActivity.this)) {
                    SharedPrefConfig.writeIsFirstTime(SettingsActivity.this, false);
                }
                if (stillLoadingCount <= 0) {
                    startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                    finish();
                }
            }
        });

        appUsernameEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) appUsernameEditText.clearFocus();
            return false;
        });

        appUsernameEditText.setText(SharedPrefConfig.readAppUserName(this));

        appUsernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                SharedPrefConfig.writeAppUserName(SettingsActivity.this, s.toString());
            }
        });

        platformAdapter = new PlatformAdapter(this, platformNamesList);
        platformsListView.setAdapter(platformAdapter);

        platformsListView.setOnItemClickListener((parent, view, position, id) -> {
            if (platformNamesList.get(position).isUserNameAllowed()) {
                createPopupDialog(position);
            } else {
                platformAdapter.setSelectedIndex(position, "", false);
            }
        });


    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (view instanceof EditText) {
                Rect outRect = new Rect();
                view.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    view.clearFocus();
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void finish() {
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        super.finish();
    }

    @Override
    public void onBackPressed() {
        settingsSaveButton.setVisibility(View.GONE);
        settingsProgressBar.setVisibility(View.VISIBLE);
        if (appUsernameEditText.getText().toString().isEmpty()) {
            Toast.makeText(this, "How should I call you?", Toast.LENGTH_SHORT).show();
        } else if (SharedPrefConfig.readPlatformsCount(this) == 0) {
            Toast.makeText(this, "No Platform is selected!", Toast.LENGTH_SHORT).show();
        } else {
            if (stillLoadingCount <= 0) {
                startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                finish();
            } else Toast.makeText(this, "Settings are not yet saved!", Toast.LENGTH_SHORT).show();
        }
        settingsSaveButton.setVisibility(View.VISIBLE);
        settingsProgressBar.setVisibility(View.GONE);
        super.onBackPressed();
    }

    @Override
    protected void onStart() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeListener, filter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(networkChangeListener);
        super.onStop();
    }

    private void setAppTheme() {
        switch (SharedPrefConfig.readAppTheme(this)) {
            case -1:
                // System Default
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case 0:
                // Light Theme
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                // Dark Theme
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }

    private void createPopupDialog(int position) {
        String platformName = getPlatformName(platformNamesList.get(position).getPlatformName());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.platforms_list_dialog, null);

        ImageView platformDialogImage = view.findViewById(R.id.platform_list_dialog_image);
        AutoCompleteTextView platformDialogUserName = view.findViewById(R.id.platform_list_dialog_user_name);
        Button platformDialogSaveButton = view.findViewById(R.id.platform_list_dialog_save_button);
        Button platformDialogRemoveButton = view.findViewById(R.id.platform_list_dialog_remove_button);
        ProgressBar platformDialogProgressBar = view.findViewById(R.id.platform_list_dialog_progress_bar);

        boolean update;
        if (!platformNamesList.get(position).getUserName().isEmpty()) {
            platformDialogUserName.setText(platformNamesList.get(position).getUserName());
            update = true;
        } else update = false;

        platformDialogUserName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) platformDialogUserName.clearFocus();
            return false;
        });

        platformDialogImage.setImageResource(platformNamesList.get(position).getLogo2X());

        if (platformNamesList.get(position).isEnabled()) {
            platformDialogRemoveButton.setVisibility(View.VISIBLE);
        } else {
            platformDialogRemoveButton.setVisibility(View.GONE);
        }

        platformDialogRemoveButton.setOnClickListener(v -> {
            platformAdapter.setSelectedIndex(position, "", false);
            dialog.cancel();
        });

        platformDialogSaveButton.setOnClickListener(v -> {
            if (platformDialogUserName.getText().toString().isEmpty()) {
                platformAdapter.setSelectedIndex(position, "", false);
                Snackbar.make(v, "Invalid User Name!", Snackbar.LENGTH_SHORT).show();
            } else {
                platformDialogSaveButton.setVisibility(View.GONE);
                platformDialogRemoveButton.setVisibility(View.GONE);
                platformDialogProgressBar.setVisibility(View.VISIBLE);

                checkValidUsername(platformDialogProgressBar, platformDialogSaveButton, v,
                        platformName, platformDialogUserName.getText().toString().trim(),
                        position, update);
            }
        });
        builder.setView(view);
        dialog = builder.create();
        dialog.getWindow().setWindowAnimations(R.style.PopupDialogAnimation);
        dialog.show();
    }

    private String getPlatformName(String platformName) {
        switch (platformName) {
            case "AtCoder":
                return "atcoder";
            case "CodeChef":
                return "codechef";
            case "CodeForces":
                return "codeforces";
            case "LeetCode":
                return "leetcode";
        }
        return null;
    }

    private void checkValidUsername(ProgressBar platformDialogProgressBar,
                                    Button platformDialogSaveButton, View v, String platform,
                                    String username, int position, boolean update) {
        String url = "https://cping-api.herokuapp.com/api/" + platform + "/" + username;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            try {
                if (response.getString("status").equals("Success")) {
                    platformAdapter.setSelectedIndex(position, username, update);
                    dialog.dismiss();
                    switch (platform) {
                        case "atcoder":
                            getAC(username);
                            break;
                        case "codechef":
                            getCC(username);
                            break;
                        case "codeforces":
                            getCF(username);
                            break;
                        case "leetcode":
                            getLC(username);
                            break;
                    }
                } else {
                    Snackbar.make(v, "Invalid User Name!", Snackbar.LENGTH_SHORT).show();
                    platformDialogProgressBar.setVisibility(View.GONE);
                    platformDialogSaveButton.setVisibility(View.VISIBLE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Snackbar.make(v, "Some error occurred! Retry again...", Snackbar.LENGTH_SHORT).show();
                platformDialogProgressBar.setVisibility(View.GONE);
                platformDialogSaveButton.setVisibility(View.VISIBLE);
            }
        }, error -> {
            Snackbar.make(v, "Some error occurred! Retry again...", Snackbar.LENGTH_SHORT).show();
            platformDialogProgressBar.setVisibility(View.GONE);
            platformDialogSaveButton.setVisibility(View.VISIBLE);
        });
        requestQueue.add(jsonObjectRequest);
    }

    private void loadData() {
        platformNamesList = SharedPrefConfig.readPlatformsSelected(this);
    }

    private void loadFirstTimeData() {
        platformNamesList = new ArrayList<>();
        platformNamesList.add(new PlatformListItem("AtCoder", "", false, true, R.drawable.ic_at_coder_logo, R.drawable.ic_at_coder_logo));
        platformNamesList.add(new PlatformListItem("CodeChef", "", false, true, R.drawable.ic_codechef_logo, R.drawable.ic_codechef_logo_2x));
        platformNamesList.add(new PlatformListItem("CodeForces", "", false, true, R.drawable.ic_codeforces_logo, R.drawable.ic_codeforces_logo_2x));
        platformNamesList.add(new PlatformListItem("HackerEarth", "", false, false, R.drawable.ic_hackerearth_logo, -1));
        platformNamesList.add(new PlatformListItem("HackerRank", "", false, false, R.drawable.ic_hackerrank_logo, -1));
        platformNamesList.add(new PlatformListItem("Kick Start", "", false, false, R.drawable.ic_kickstart_logo, -1));
        platformNamesList.add(new PlatformListItem("LeetCode", "", false, true, R.drawable.ic_leetcode_logo, R.drawable.ic_leetcode_logo_2x));
        platformNamesList.add(new PlatformListItem("TopCoder", "", false, false, R.drawable.ic_topcoder_logo, -1));

        saveData();
    }

    private void saveData() {
        SharedPrefConfig.writePlatformsSelected(this, platformNamesList);
    }

    private void getAC(String user_name) {
        stillLoadingCount++;

        String platform_name = "atcoder";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                "https://cping-api.herokuapp.com/api/" + platform_name + "/" + user_name, null, response -> {
            try {
                ArrayList<Integer> recentRatingsArrayList = new ArrayList<>();
                JSONArray jsonArray = response.getJSONArray("contest_ratings");
                int n = jsonArray.length();
                for (int i = 0; i < n; ++i) {
                    recentRatingsArrayList.add(jsonArray.getInt(i));
                }
                AtCoderUserDetails item = new AtCoderUserDetails(
                        user_name,
                        response.getInt("rating"),
                        response.getInt("highest"),
                        response.getInt("rank"),
                        response.getString("level"),
                        recentRatingsArrayList);

                SharedPrefConfig.writeInAtCoderPref(getApplicationContext(), item);

                stillLoadingCount--;
                if (stillLoadingCount <= 0 && saveButtonClicked) {
                    startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                    finish();
                }
            } catch (JSONException e) {
                Log.d(TAG, "getAC: " + e.getMessage());
                e.printStackTrace();
                stillLoadingCount--;
                getAC(user_name);
            }
        }, error -> {
            Log.d(TAG, "onErrorResponse: " + error.getMessage());
            stillLoadingCount--;
            getAC(user_name);
        });
        if (stillLoadingCount <= 0 && saveButtonClicked) {
            startActivity(new Intent(SettingsActivity.this, MainActivity.class));
            finish();
        }
        requestQueue.add(jsonObjectRequest);
    }

    private void getCC(String user_name) {
        stillLoadingCount++;

        String platform_name = "codechef";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                "https://cping-api.herokuapp.com/api/" + platform_name + "/" + user_name, null, response -> {
            try {
                ArrayList<Integer> recentRatingsArrayList = new ArrayList<>();
                JSONArray jsonArray = response.getJSONArray("contest_ratings");
                int n = jsonArray.length();
                for (int i = 0; i < n; ++i) {
                    recentRatingsArrayList.add(jsonArray.getInt(i));
                }
                CodeChefUserDetails item = new CodeChefUserDetails(
                        user_name,
                        response.getInt("rating"),
                        response.getInt("highest_rating"),
                        response.getString("stars"),
                        recentRatingsArrayList);

                SharedPrefConfig.writeInCodeChefPref(getApplicationContext(), item);

                stillLoadingCount--;
                if (stillLoadingCount <= 0 && saveButtonClicked) {
                    startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                    finish();
                }
            } catch (JSONException e) {
                Log.d(TAG, "onResponse: " + e.getMessage());
                e.printStackTrace();
                stillLoadingCount--;
                getCC(user_name);
            }
        }, error -> {
            Log.d(TAG, "onErrorResponse: " + error.getMessage());
            stillLoadingCount--;
            getCC(user_name);
        });
        if (stillLoadingCount <= 0 && saveButtonClicked) {
            startActivity(new Intent(SettingsActivity.this, MainActivity.class));
            finish();
        }
        requestQueue.add(jsonObjectRequest);
    }

    private void getCF(String user_name) {
        stillLoadingCount++;

        String platform_name = "codeforces";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                "https://cping-api.herokuapp.com/api/" + platform_name + "/" + user_name, null, response -> {
            try {
                ArrayList<Integer> recentRatingsArrayList = new ArrayList<>();
                JSONArray jsonArray = response.getJSONArray("contests");
                int n = jsonArray.length();
                for (int i = 0; i < n; i++) {
                    recentRatingsArrayList.add(jsonArray.getInt(i));
                }
                Collections.reverse(recentRatingsArrayList);
                CodeForcesUserDetails item = new CodeForcesUserDetails(user_name,
                        response.getInt(
                                "rating"),
                        response.getInt("max rating"),
                        response.getString("rank"),
                        response.getString("max rank"),
                        recentRatingsArrayList);

                SharedPrefConfig.writeInCodeForcesPref(getApplicationContext(), item);
                stillLoadingCount--;
                if (stillLoadingCount <= 0 && saveButtonClicked) {
                    startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                    finish();
                }
            } catch (JSONException e) {
                Log.d(TAG, "getCF: " + e.getMessage());
                e.printStackTrace();
                stillLoadingCount--;
                getCF(user_name);
            }
        }, error -> {
            stillLoadingCount--;
            Log.d(TAG, "onErrorResponse: " + error.getMessage());
            getCF(user_name);
        });
        if (stillLoadingCount <= 0 && saveButtonClicked) {
            startActivity(new Intent(SettingsActivity.this, MainActivity.class));
            finish();
        }
        requestQueue.add(jsonObjectRequest);
    }

    private void getLC(String user_name) {
        stillLoadingCount++;

        String platform_name = "leetcode";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                "https://cping-api.herokuapp.com/api/" + platform_name + "/" + user_name, null, response -> {
            try {
                LeetCodeUserDetails item = new LeetCodeUserDetails(user_name,
                        response.getString("total_problems_solved"),
                        response.getString("acceptance_rate"),
                        response.getString("easy_questions_solved"),
                        response.getString("total_easy_questions"),
                        response.getString("medium_questions_solved"),
                        response.getString("total_medium_questions"),
                        response.getString("hard_questions_solved"),
                        response.getString("total_hard_questions"));

                SharedPrefConfig.writeInLeetCodePref(getApplicationContext(), item);

                stillLoadingCount--;
                if (stillLoadingCount <= 0 && saveButtonClicked) {
                    startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                    finish();
                }
            } catch (JSONException e) {
                Log.d(TAG, "getLC: " + e.getMessage());
                e.printStackTrace();
                stillLoadingCount--;
                getLC(user_name);
            }
        }, error -> {
            stillLoadingCount--;
            Log.d(TAG, "onErrorResponse: " + error.getMessage());
            getLC(user_name);
        });
        if (stillLoadingCount <= 0 && saveButtonClicked) {
            startActivity(new Intent(SettingsActivity.this, MainActivity.class));
            finish();
        }
        requestQueue.add(jsonObjectRequest);
    }

    private void findViewByIds() {
        themeTitleDropDown = findViewById(R.id.theme_title_drop_down_image);
        themeTitleLinearLayout = findViewById(R.id.theme_title_linear_layout);
        themeRadioGroup = findViewById(R.id.theme_radio_group);
        settingsSaveButton = findViewById(R.id.settings_save_button);
        platformsListView = findViewById(R.id.settings_platforms_list_view);
        appUsernameEditText = findViewById(R.id.editTextUserName);
        settingsProgressBar = findViewById(R.id.settings_page_progress_bar);
        platFormTitleLinearLayout = findViewById(R.id.platform_title_linear_layout);
        platFormTitleDropDown = findViewById(R.id.platform_title_drop_down_image);
    }
}