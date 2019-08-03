package `in`.avimarine.seawatercurrentmeasure

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 31/07/2019.
 */
class MySettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }
}