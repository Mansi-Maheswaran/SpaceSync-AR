package com.truefitar.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.truefitar.fitcheck.FurnitureType;
import com.truefitar.storage.SavedRoom;

/**
 * Draws a saved room as a simple 2D floor plan, seen from above:
 * the measured gap is the horizontal bar (A on the left, B on the right)
 * and every placed piece of furniture is a rectangle at its saved position
 * and angle. Green outline = fits, red outline = did not fit.
 */
public class RoomPlanView extends View {

    private final SavedRoom room;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public RoomPlanView(Context c, SavedRoom room) {
        super(c);
        this.room = room;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, (int) (w * 0.75f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(UiKit.CARD);
        canvas.drawRoundRect(0, 0, w, h, UiKit.dp(getContext(), 14), UiKit.dp(getContext(), 14), paint);

        float margin = UiKit.dp(getContext(), 28);
        float gap = Math.max(room.gapMeters, 0.5f);

        // Make sure items that stick out past the ends still fit on screen.
        float minAlong = 0f;
        float maxAlong = gap;
        for (SavedRoom.Item it : room.items) {
            float half = Math.max(it.lengthM, it.widthM) / 2f;
            minAlong = Math.min(minAlong, it.along - half);
            maxAlong = Math.max(maxAlong, it.along + half);
        }
        float span = maxAlong - minAlong;
        float scale = (w - 2 * margin) / span;
        float originX = margin - minAlong * scale;
        float cy = h / 2f;

        // Gap bar and end markers.
        paint.setColor(UiKit.ORANGE);
        paint.setStrokeWidth(UiKit.dp(getContext(), 3));
        canvas.drawLine(originX, cy, originX + gap * scale, cy, paint);
        float r = UiKit.dp(getContext(), 6);
        canvas.drawCircle(originX, cy, r, paint);
        canvas.drawCircle(originX + gap * scale, cy, r, paint);

        paint.setTextSize(UiKit.dp(getContext(), 13));
        paint.setColor(UiKit.TEXT_DIM);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("A", originX, cy + UiKit.dp(getContext(), 22), paint);
        canvas.drawText("B", originX + gap * scale, cy + UiKit.dp(getContext(), 22), paint);
        paint.setColor(UiKit.ORANGE);
        canvas.drawText(String.format("%.2f m", room.gapMeters),
                originX + gap * scale / 2f, UiKit.dp(getContext(), 20), paint);

        for (SavedRoom.Item it : room.items) {
            FurnitureType type = FurnitureType.fromName(it.type);
            float px = originX + it.along * scale;
            float py = cy + it.lateral * scale;
            float hw = it.lengthM * scale / 2f;
            float hh = it.widthM * scale / 2f;

            canvas.save();
            canvas.translate(px, py);
            canvas.rotate(it.relAngleDeg);
            rect.set(-hw, -hh, hw, hh);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(UiKit.withAlpha(colorFor(type), 200));
            canvas.drawRoundRect(rect, 6, 6, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(UiKit.dp(getContext(), 2.5f));
            paint.setColor(it.fits ? UiKit.GREEN : UiKit.RED);
            canvas.drawRoundRect(rect, 6, 6, paint);
            canvas.restore();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFFFFFF);
            paint.setTextSize(UiKit.dp(getContext(), 12));
            canvas.drawText(type.getLabel(), px, py + UiKit.dp(getContext(), 4), paint);
        }
    }

    private static int colorFor(FurnitureType t) {
        switch (t) {
            case SOFA: return 0xFF965F41;
            case CHAIR: return 0xFF46966A;
            case CUPBOARD: return 0xFFAF8255;
            default: return 0xFF6496DC;
        }
    }
}
