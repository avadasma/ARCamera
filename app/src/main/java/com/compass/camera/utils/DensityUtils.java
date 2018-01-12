package com.compass.camera.utils;

import android.content.Context;
import android.util.TypedValue;

/**
 * Created by Simon on 2017/7/15.
 */

public class DensityUtils {

    /**
     * dp switch px
     */
    public static int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, context.getResources().getDisplayMetrics());
    }


}
