package com.itsmarts.SmartRouteTruckApp;

import static android.speech.tts.TextToSpeech.LANG_AVAILABLE;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class VoiceAssistant {

    private static final String TAG = VoiceAssistant.class.getName();
    private final TextToSpeech textToSpeech;
    private String utteranceId;
    private int messageId;

    public VoiceAssistant(Context context) {
        textToSpeech = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.ERROR) {
                Log.d(TAG, ("ERROR: Initialization of Android's TextToSpeech failed."));
            }
        });
    }

    public boolean isLanguageAvailable(Locale locale) {
        return textToSpeech.isLanguageAvailable(locale) == LANG_AVAILABLE;
    }

    public boolean setLanguage(Locale locale) {
        boolean isLanguageSet = textToSpeech.setLanguage(locale) == LANG_AVAILABLE;
        return isLanguageSet;
    }

    public void speak(String speechMessage) {
        Log.d(TAG, "Voice message: " + speechMessage);

        // No engine specific params used for this example.
        Bundle engineParams = null;
        utteranceId = TAG + messageId++;

        // QUEUE_FLUSH interrupts already speaking messages.
        int error = textToSpeech.speak(speechMessage, TextToSpeech.QUEUE_FLUSH, engineParams, utteranceId);
        if (error != -1) {
            Log.e(TAG, "Error when speaking using Android's TextToSpeech: " + error);
        }
    }
}
