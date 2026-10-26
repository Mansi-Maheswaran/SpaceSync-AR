package com.truefitar.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

/** Small helpers so every screen and dialog shares one look. */
public final class UiKit {

    public static final int NAVY = 0xFF0B1B3A;
    public static final int CARD = 0xFF16294F;
    public static final int CARD_LIGHT = 0xFF1F3868;
    public static final int TEAL = 0xFF19C3B1;
    public static final int ORANGE = 0xFFFF9F43;
    public static final int GREEN = 0xFF2ECC71;
    public static final int RED = 0xFFE74C3C;
    public static final int TEXT_DIM = 0xFF9FB3D9;

    private UiKit() {}

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable rounded(Context c, int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable roundedStroke(Context c, int fill, int stroke, float radiusDp) {
        GradientDrawable d = rounded(c, fill, radiusDp);
        d.setStroke(dp(c, 1.5f), stroke);
        return d;
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.START);
        return t;
    }

    public static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
