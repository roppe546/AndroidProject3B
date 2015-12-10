package com.example.robin.androidproject3b;


import android.app.Activity;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }

    public static class PrefsFragment extends PreferenceFragment {
        private EditTextPreference filename;
        private EditTextPreference ipaddress;
        private EditTextPreference portnumber;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.pref_general);

            // Get preferences
            filename = (EditTextPreference) findPreference("filename");
            ipaddress = (EditTextPreference) findPreference("ipaddress");
            portnumber = (EditTextPreference) findPreference("portnumber");
        }
    }
}
