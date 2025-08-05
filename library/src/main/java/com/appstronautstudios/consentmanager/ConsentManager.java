package com.appstronautstudios.consentmanager;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class ConsentManager {

    private static ConsentManager instance;

    private ConsentInformation consentInformation;
    private boolean isInitialized = false;
    private boolean isInitializing = false;

    private ConsentManager() {
    }

    public static ConsentManager getInstance() {
        if (instance == null) {
            instance = new ConsentManager();
        }
        return instance;
    }

    public void initialize(Activity activity, boolean debugMode, Runnable onConsentResolved) {
        if (isInitialized || isInitializing) {
            return;
        }

        isInitializing = true;
        consentInformation = UserMessagingPlatform.getConsentInformation(activity);

        ConsentRequestParameters params;
        if (debugMode) {
            // debug EEA consent outside of EU
            ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA) // Force EEA behavior
                    .build();
            params = new ConsentRequestParameters
                    .Builder()
                    .setTagForUnderAgeOfConsent(false)
                    .setConsentDebugSettings(debugSettings)
                    .build();
        } else {
            params = new ConsentRequestParameters
                    .Builder()
                    .setTagForUnderAgeOfConsent(false)
                    .build();
        }

        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    if (consentInformation.isConsentFormAvailable()
                            && consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {

                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                                activity,
                                consentForm -> {
                                    onConsentReady(onConsentResolved);
                                }
                        );

                    } else {
                        // Consent not required or already handled
                        onConsentReady(onConsentResolved);
                    }
                },
                formError -> {
                    onConsentReady(onConsentResolved);
                }
        );
    }

    private void onConsentReady(Runnable onConsentResolved) {
        isInitialized = true;
        isInitializing = false;
        new Handler(Looper.getMainLooper()).post(() -> {
            onConsentResolved.run();
        });
    }

    public boolean canRequestAds() {
        return consentInformation != null &&
                (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED
                        || consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.NOT_REQUIRED);
    }
}