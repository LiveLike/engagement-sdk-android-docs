package com.mixpanel.android.mpmetrics;

import android.content.Context;
import android.content.SharedPreferences;
import com.mixpanel.android.mpmetrics.MPConfig;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class MixpanelExtension {
    public static MixpanelAPI getUniqueInstance(Context context, String token, String programId) {
        if (null == token || null == context) {
            return null;
        }
        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();

            if (null == sReferrerPrefs) {
                sReferrerPrefs = sPrefsLoader.loadPreferences(context, MPConfig.REFERRER_PREFS_NAME, null);
            }

            Map<Context, MixpanelAPI> instances = sInstanceMap.get(programId);
            if (null == instances) {
                instances = new HashMap<Context, MixpanelAPI>();
                sInstanceMap.put(programId, instances);
            }

            MixpanelAPI instance = instances.get(appContext);
            if (null == instance && ConfigurationChecker.checkBasicConfiguration(appContext)) {
                instance = new MixpanelAPI(appContext, sReferrerPrefs, token, false);
                instances.put(appContext, instance);
            }
            try {
                // https://github.com/mixpanel/mixpanel-android/issues/253#issuecomment-133559080
                Field field = MixpanelAPI.class.getDeclaredField("mTrackingDebug");
                field.setAccessible(true);
                field.set(instance, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return instance;
        }
    }


    private static final Map<String, Map<Context, MixpanelAPI>> sInstanceMap = new HashMap<String, Map<Context, MixpanelAPI>>();
    private static final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
    private static Future<SharedPreferences> sReferrerPrefs;
}
