package com.aaupush.aaupush.Setting;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.aaupush.aaupush.DBHelper;
import com.aaupush.aaupush.FirstRunAndSetup.CourseSelectionFragment;
import com.aaupush.aaupush.FirstRunAndSetup.FirstRunActivity;
import com.aaupush.aaupush.PushService;
import com.aaupush.aaupush.PushUtils;
import com.aaupush.aaupush.R;

import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsHomeFragment extends Fragment implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener{

    // SharedPreferences
    SharedPreferences preferences;

    // Main View
    View view;

    // Background refresh interval text view
    TextView backgroundIntervalTextView;


    public SettingsHomeFragment() {
        // Required empty public constructor
    }

    public static SettingsHomeFragment newInstance() { return new SettingsHomeFragment(); }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_settings_home, container, false);

        // Init SharedPreferences
        preferences = getContext().getSharedPreferences(PushUtils.SP_KEY_NAME, Context.MODE_PRIVATE);

        // Set OnClickListeners
        view.findViewById(R.id.follow_unfollow_courses_tv).setOnClickListener(this);
        view.findViewById(R.id.start_over_tv).setOnClickListener(this);
        view.findViewById(R.id.refresh_interval_layout).setOnClickListener(this);

        // Init notification switches
        SwitchCompat mainNotificationSwitch = (SwitchCompat)view.findViewById(R.id.notification_switch);
        SwitchCompat announcementNotificationSwitch = (SwitchCompat)view.findViewById(R.id.announcements_notification_switch);
        SwitchCompat materialNotificationSwitch = (SwitchCompat)view.findViewById(R.id.materials_notification_switch);

        // Get values from SharedPreferences
        boolean mainNotificationEnabled = preferences.getBoolean(PushUtils.SP_NOTIFICATION_ENABLED, true);
        boolean announcementNotificationEnabled = preferences.getBoolean(PushUtils.SP_ANNOUNCEMENT_NOTIFICATION_ENABLED, true);
        boolean materialNotificationEnabled = preferences.getBoolean(PushUtils.SP_MATERIAL_NOTIFICATION_ENABLED, true);

        // Set the values to the Switch views
        mainNotificationSwitch.setChecked(mainNotificationEnabled);
        announcementNotificationSwitch.setChecked(announcementNotificationEnabled);
        materialNotificationSwitch.setChecked(materialNotificationEnabled);

        // Enable or disable the switches of material and announcement notification based on
        // main notification switch value
        if (!mainNotificationEnabled) {
            announcementNotificationSwitch.setEnabled(false);
            materialNotificationSwitch.setEnabled(false);
        }

        // Set OnCheckedChangeListeners
        mainNotificationSwitch.setOnCheckedChangeListener(this);
        announcementNotificationSwitch.setOnCheckedChangeListener(this);
        materialNotificationSwitch.setOnCheckedChangeListener(this);

        // Background refresh interval text view
        backgroundIntervalTextView = (TextView)view.findViewById(R.id.background_refresh_value_tv);
        long refreshIntervalValue = preferences.getLong(PushUtils.SP_BACKGROUND_REFRESH_INTERVAL, PushService.SERVICE_REFRESH_10_MIN)/60000;
        backgroundIntervalTextView.setText(String.format(Locale.ENGLISH, "%d Minutes", refreshIntervalValue));

        return view;
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.follow_unfollow_courses_tv:
                // Go to follow course fragment
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.setting_activity, CourseSelectionFragment.newInstance())
                        .addToBackStack(null)
                        .commit();

                break;
            case R.id.start_over_tv:
                // Show a 'are you sure dialog'
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Are You Sure?");
                builder.setMessage("This will not delete already downloaded materials." +
                        " But you'll have to start from the beginning and select your section" +
                        " and courses to follow.");
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.setPositiveButton("Start Over", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Reset every shared preferences value to the default
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean(PushUtils.SP_IS_FIRST_RUN, true);
                        editor.putInt(PushUtils.SP_SELECTED_YEAR, 0);
                        editor.putInt(PushUtils.SP_SELECTED_SECTION, 0);
                        editor.putInt(PushUtils.SP_LAST_ANNOUNCEMENT_RECEIVED_ID, 0);
                        editor.putString(PushUtils.SP_STUDY_FIELD_CODE, "CS");
                        editor.putBoolean(PushUtils.SP_IS_ANNOUNCEMENT_FRAGMENT_RUNNING, false);
                        editor.putBoolean(PushUtils.SP_IS_MATERIAL_FRAGMENT_RUNNING, false);
                        editor.putInt(PushUtils.SP_NOTIFICATION_COUNTER, 1);
                        editor.putInt(PushUtils.SP_LAST_MATERIAL_RECEIVED_ID, 0);
                        editor.putLong(PushUtils.SP_MATERIAL_LAST_CHECKED, 0L);
                        editor.putLong(PushUtils.SP_ANNOUNCEMENT_LAST_CHECKED, 0L);
                        editor.putString(PushUtils.SP_SECTION_CODE, "CSY1S1");
                        editor.putInt(PushUtils.SP_STUDY_FIELD_ID, 0);
                        editor.putBoolean(PushUtils.SP_NOTIFICATION_ENABLED, true);
                        editor.putBoolean(PushUtils.SP_ANNOUNCEMENT_NOTIFICATION_ENABLED, true);
                        editor.putBoolean(PushUtils.SP_MATERIAL_NOTIFICATION_ENABLED, true);
                        editor.apply();

                        // Delete everything from the db
                        DBHelper dbHelper = new DBHelper(getContext());
                        dbHelper.deleteEverything();
                        dbHelper.close();

                        getActivity().finish();
                        startActivity(new Intent(getContext(), FirstRunActivity.class));
                    }
                });
                builder.show();
                break;
            case R.id.refresh_interval_layout:

                // Refresh interval options
                String[] refreshIntervals = {
                        "1 Minute (For debugging only)",
                        "5 Minutes",
                        "10 Minutes (Recommended)",
                        "15 Minutes" };

                AlertDialog.Builder listDialog = new AlertDialog.Builder(getContext());
                listDialog.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_list_item_1,
                        refreshIntervals), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int position) {
                        switch (position) {
                            case 0:
                                preferences.edit().putLong(PushUtils.SP_BACKGROUND_REFRESH_INTERVAL, PushService.SERVICE_REFRESH_1_MIN).apply();
                                break;
                            case 1:
                                preferences.edit().putLong(PushUtils.SP_BACKGROUND_REFRESH_INTERVAL, PushService.SERVICE_REFRESH_5_MIN).apply();
                                break;
                            case 2:
                                preferences.edit().putLong(PushUtils.SP_BACKGROUND_REFRESH_INTERVAL, PushService.SERVICE_REFRESH_10_MIN).apply();
                                break;
                            case 3:
                                preferences.edit().putLong(PushUtils.SP_BACKGROUND_REFRESH_INTERVAL, PushService.SERVICE_REFRESH_15_MIN).apply();
                                break;
                        }

                        // Set the value to the text view
                        long refreshIntervalValue = preferences.getLong(PushUtils.SP_BACKGROUND_REFRESH_INTERVAL, PushService.SERVICE_REFRESH_10_MIN)/60000;
                        backgroundIntervalTextView.setText(String.format(Locale.ENGLISH, "%d Minutes", refreshIntervalValue));
                    }
                });
                listDialog.show();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.notification_switch:
                preferences.edit().putBoolean(PushUtils.SP_NOTIFICATION_ENABLED, b).apply();
                if(!b) {
                    view.findViewById(R.id.announcements_notification_switch).setEnabled(false);
                    view.findViewById(R.id.materials_notification_switch).setEnabled(false);
                } else {
                    view.findViewById(R.id.announcements_notification_switch).setEnabled(true);
                    view.findViewById(R.id.materials_notification_switch).setEnabled(true);
                }
                break;
            case R.id.announcements_notification_switch:
                preferences.edit().putBoolean(PushUtils.SP_ANNOUNCEMENT_NOTIFICATION_ENABLED, b).apply();
                break;
            case R.id.materials_notification_switch:
                preferences.edit().putBoolean(PushUtils.SP_MATERIAL_NOTIFICATION_ENABLED, b).apply();
                break;
        }
    }
}
