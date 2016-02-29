package com.mutualmobile.cardstack.sample;

import android.content.Context;

import com.mutualmobile.cardstack.CardStackLayout;
import com.tramsun.libs.prefcompat.Pref;

public class Prefs {

    public static final String SHOW_INIT_ANIMATION = "showInitAnimation";
    public static final String PARALLAX_ENABLED = "parallaxEnabled";
    public static final String PARALLAX_SCALE = "parallaxScale";
    public static final String CARD_GAP = "cardGap";
    public static final String CARD_GAP_BOTTOM = "cardGapBottom";

    public static boolean isShowInitAnimationEnabled() {
        return Pref.getBoolean(SHOW_INIT_ANIMATION, CardStackLayout.SHOW_INIT_ANIMATION_DEFAULT);
    }

    public static boolean isParallaxEnabled() {
        return Pref.getBoolean(PARALLAX_ENABLED, CardStackLayout.PARALLAX_ENABLED_DEFAULT);
    }

    public static int getParallaxScale(Context context) {
        return Pref.getInt(PARALLAX_SCALE, context.getResources().getInteger(com.mutualmobile.cardstack.R.integer.parallax_scale_default));
    }


}
