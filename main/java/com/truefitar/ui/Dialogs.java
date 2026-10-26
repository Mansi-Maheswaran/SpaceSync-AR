package com.truefitar.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.truefitar.fitcheck.FurnitureType;

/** The three pop-ups of the Fit Check flow: pick furniture, enter size, name the room. */
public final class Dialogs {

    private Dialogs() {}

    public interface OnTypePicked { void onPicked(FurnitureType type); }

    public interface OnDimensions { void onDimensions(float lengthM, float heightM, float widthM); }

    public interface OnText { void onText(String text); }

    private static Dialog baseDialog(Activity a, LinearLayout content) {
        Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(content);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setLayout(
                    (int) (a.getResources().getDisplayMetrics().widthPixels * 0.9f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return d;
    }

    private static LinearLayout panel(Activity a) {
        LinearLayout p = new LinearLayout(a);
        p.setOrientation(LinearLayout.VERTICAL);
        int pad = UiKit.dp(a, 20);
        p.setPadding(pad, pad, pad, pad);
        p.setBackground(UiKit.roundedStroke(a, UiKit.NAVY, UiKit.CARD_LIGHT, 22));
        return p;
    }

    private static TextView button(Activity a, String label, boolean primary) {
        TextView b = UiKit.text(a, label, 16, primary ? UiKit.NAVY : Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, UiKit.dp(a, 14), 0, UiKit.dp(a, 14));
        b.setBackground(primary
                ? UiKit.rounded(a, UiKit.TEAL, 14)
                : UiKit.roundedStroke(a, UiKit.CARD, UiKit.CARD_LIGHT, 14));
        return b;
    }

    // ------------------------------------------------------------------ 1. furniture list

    public static void pickFurniture(Activity a, OnTypePicked cb) {
        LinearLayout p = panel(a);
        p.addView(UiKit.text(a, "Choose furniture", 22, Color.WHITE, true));
        TextView sub = UiKit.text(a, "What are you thinking of buying?", 14, UiKit.TEXT_DIM, false);
        sub.setPadding(0, UiKit.dp(a, 2), 0, UiKit.dp(a, 12));
        p.addView(sub);

        final Dialog d = baseDialog(a, p);

        for (FurnitureType t : FurnitureType.values()) {
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(UiKit.dp(a, 16), UiKit.dp(a, 14), UiKit.dp(a, 16), UiKit.dp(a, 14));
            row.setBackground(UiKit.rounded(a, UiKit.CARD, 16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = UiKit.dp(a, 10);
            row.setLayoutParams(lp);

            TextView icon = UiKit.text(a, t.getIcon(), 28, Color.WHITE, false);
            icon.setPadding(0, 0, UiKit.dp(a, 14), 0);
            row.addView(icon);

            LinearLayout col = new LinearLayout(a);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(UiKit.text(a, t.getLabel(), 18, Color.WHITE, true));
            col.addView(UiKit.text(a, "Typical " + t.getDefaultLengthCm() + " x "
                    + t.getDefaultWidthCm() + " cm", 13, UiKit.TEXT_DIM, false));
            row.addView(col);

            row.setOnClickListener(v -> {
                d.dismiss();
                cb.onPicked(t);
            });
            p.addView(row);
        }

        TextView cancel = button(a, "Cancel", false);
        cancel.setOnClickListener(v -> d.dismiss());
        p.addView(cancel);
        d.show();
    }

    // ------------------------------------------------------------------ 2. dimensions

    public static void enterDimensions(Activity a, FurnitureType type, OnDimensions cb) {
        LinearLayout p = panel(a);
        p.addView(UiKit.text(a, type.getIcon() + "  " + type.getLabel() + " size", 22, Color.WHITE, true));
        TextView sub = UiKit.text(a,
                "Type the size from the shop page (centimetres)", 14, UiKit.TEXT_DIM, false);
        sub.setPadding(0, UiKit.dp(a, 2), 0, UiKit.dp(a, 12));
        p.addView(sub);

        final Dialog d = baseDialog(a, p);

        final EditText length = field(a, "Length (cm)", type.getDefaultLengthCm());
        final EditText width = field(a, "Width (cm)", type.getDefaultWidthCm());
        final EditText height = field(a, "Height (cm)", type.getDefaultHeightCm());
        p.addView(length);
        p.addView(width);
        p.addView(height);

        LinearLayout buttons = new LinearLayout(a);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, UiKit.dp(a, 8), 0, 0);

        TextView back = button(a, "Back", false);
        TextView place = button(a, "Place", true);
        LinearLayout.LayoutParams lpL = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpL.rightMargin = UiKit.dp(a, 6);
        LinearLayout.LayoutParams lpR = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpR.leftMargin = UiKit.dp(a, 6);
        buttons.addView(back, lpL);
        buttons.addView(place, lpR);
        p.addView(buttons);

        back.setOnClickListener(v -> {
            d.dismiss();
            pickFurniture(a, t -> enterDimensions(a, t, cb));
        });
        place.setOnClickListener(v -> {
            try {
                float l = Float.parseFloat(length.getText().toString()) / 100f;
                float w = Float.parseFloat(width.getText().toString()) / 100f;
                float h = Float.parseFloat(height.getText().toString()) / 100f;
                if (l <= 0.05f || w <= 0.05f || h <= 0.05f) {
                    Toast.makeText(a, "Each size must be at least 5 cm", Toast.LENGTH_SHORT).show();
                    return;
                }
                d.dismiss();
                cb.onDimensions(l, h, w);
            } catch (NumberFormatException e) {
                Toast.makeText(a, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });
        d.show();
    }

    private static EditText field(Activity a, String hint, int defaultCm) {
        EditText e = new EditText(a);
        e.setHint(hint);
        e.setText(String.valueOf(defaultCm));
        e.setSelectAllOnFocus(true);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(UiKit.TEXT_DIM);
        e.setTextSize(18);
        e.setPadding(UiKit.dp(a, 16), UiKit.dp(a, 12), UiKit.dp(a, 16), UiKit.dp(a, 12));
        e.setBackground(UiKit.rounded(a, UiKit.CARD, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = UiKit.dp(a, 10);
        e.setLayoutParams(lp);
        return e;
    }

    // ------------------------------------------------------------------ 3. room name

    public static void askText(Activity a, String title, String subtitle, String defaultText,
                               String okLabel, OnText cb) {
        LinearLayout p = panel(a);
        p.addView(UiKit.text(a, title, 22, Color.WHITE, true));
        TextView sub = UiKit.text(a, subtitle, 14, UiKit.TEXT_DIM, false);
        sub.setPadding(0, UiKit.dp(a, 2), 0, UiKit.dp(a, 12));
        p.addView(sub);

        final Dialog d = baseDialog(a, p);

        final EditText input = new EditText(a);
        input.setText(defaultText);
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setTextSize(18);
        input.setPadding(UiKit.dp(a, 16), UiKit.dp(a, 12), UiKit.dp(a, 16), UiKit.dp(a, 12));
        input.setBackground(UiKit.rounded(a, UiKit.CARD, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = UiKit.dp(a, 12);
        p.addView(input, lp);

        LinearLayout buttons = new LinearLayout(a);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView cancel = button(a, "Cancel", false);
        TextView ok = button(a, okLabel, true);
        LinearLayout.LayoutParams lpL = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpL.rightMargin = UiKit.dp(a, 6);
        LinearLayout.LayoutParams lpR = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpR.leftMargin = UiKit.dp(a, 6);
        buttons.addView(cancel, lpL);
        buttons.addView(ok, lpR);
        p.addView(buttons);

        cancel.setOnClickListener((View v) -> d.dismiss());
        ok.setOnClickListener(v -> {
            String s = input.getText().toString().trim();
            if (s.isEmpty()) s = defaultText;
            d.dismiss();
            cb.onText(s);
        });
        d.show();
    }
}
