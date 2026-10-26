package com.truefitar.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.examples.java.helloar.R;
import com.truefitar.fitcheck.FurnitureType;
import com.truefitar.storage.RoomStore;
import com.truefitar.storage.SavedRoom;

import java.util.Date;
import java.util.List;

/** Lists saved rooms; tapping one shows its 2D floor plan and the fit result of each item. */
public class SavedRoomsActivity extends AppCompatActivity {

    private LinearLayout list;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_rooms);
        list = findViewById(R.id.roomList);
        empty = findViewById(R.id.emptyText);
        findViewById(R.id.backButton).setOnClickListener((View v) -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        list.removeAllViews();
        List<SavedRoom> rooms = RoomStore.loadAll(this);
        empty.setVisibility(rooms.isEmpty() ? View.VISIBLE : View.GONE);

        for (SavedRoom room : rooms) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            int pad = UiKit.dp(this, 18);
            card.setPadding(pad, pad, pad, pad);
            card.setBackground(UiKit.roundedStroke(this, UiKit.CARD, UiKit.CARD_LIGHT, 18));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = UiKit.dp(this, 12);
            card.setLayoutParams(lp);

            card.addView(UiKit.text(this, room.name, 20, Color.WHITE, true));

            String when = DateFormat.format("dd MMM yyyy, hh:mm a", new Date(room.timestamp)).toString();
            int ok = 0;
            for (SavedRoom.Item it : room.items) if (it.fits) ok++;
            String summary = String.format("Gap %.2f m  |  %d item%s  |  %d fit",
                    room.gapMeters, room.items.size(), room.items.size() == 1 ? "" : "s", ok);
            card.addView(UiKit.text(this, summary, 14, UiKit.TEAL, false));
            card.addView(UiKit.text(this, when, 12, UiKit.TEXT_DIM, false));

            card.setOnClickListener((View v) -> showRoom(room));
            list.addView(card);
        }
    }

    private void showRoom(SavedRoom room) {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        int pad = UiKit.dp(this, 18);
        p.setPadding(pad, pad, pad, pad);
        p.setBackground(UiKit.roundedStroke(this, UiKit.NAVY, UiKit.CARD_LIGHT, 22));

        p.addView(UiKit.text(this, room.name, 22, Color.WHITE, true));
        TextView hint = UiKit.text(this, "Top-down plan. Green = fits, red = does not fit.",
                13, UiKit.TEXT_DIM, false);
        hint.setPadding(0, UiKit.dp(this, 2), 0, UiKit.dp(this, 10));
        p.addView(hint);

        p.addView(new RoomPlanView(this, room));

        for (SavedRoom.Item it : room.items) {
            FurnitureType t = FurnitureType.fromName(it.type);
            TextView line = UiKit.text(this,
                    String.format("%s %s  %.0f x %.0f x %.0f cm  -  %s",
                            t.getIcon(), t.getLabel(),
                            it.lengthM * 100, it.widthM * 100, it.heightM * 100,
                            it.fits ? "fits" : "does not fit"),
                    14, it.fits ? UiKit.GREEN : UiKit.RED, false);
            line.setPadding(0, UiKit.dp(this, 8), 0, 0);
            p.addView(line);
        }
        if (room.items.isEmpty()) {
            TextView none = UiKit.text(this, "No furniture was placed in this room.",
                    14, UiKit.TEXT_DIM, false);
            none.setPadding(0, UiKit.dp(this, 8), 0, 0);
            p.addView(none);
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, UiKit.dp(this, 16), 0, 0);
        TextView delete = button("Delete", false);
        TextView close = button("Close", true);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        l1.rightMargin = UiKit.dp(this, 6);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        l2.leftMargin = UiKit.dp(this, 6);
        row.addView(delete, l1);
        row.addView(close, l2);
        p.addView(row);

        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(p);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        close.setOnClickListener((View v) -> d.dismiss());
        delete.setOnClickListener((View v) -> {
            RoomStore.delete(this, room.id);
            d.dismiss();
            refresh();
        });
        d.show();
    }

    private TextView button(String label, boolean primary) {
        TextView b = UiKit.text(this, label, 16, primary ? UiKit.NAVY : Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, UiKit.dp(this, 14), 0, UiKit.dp(this, 14));
        b.setBackground(primary
                ? UiKit.rounded(this, UiKit.TEAL, 14)
                : UiKit.roundedStroke(this, UiKit.CARD, UiKit.CARD_LIGHT, 14));
        return b;
    }
}
