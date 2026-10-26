package com.truefitar.storage;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TrueFit AR - saves rooms on the phone (SharedPreferences, JSON text).
 * No database library and no internet needed.
 */
public final class RoomStore {

    private static final String PREFS = "truefit_rooms";
    private static final String KEY = "rooms";

    private RoomStore() {}

    public static List<SavedRoom> loadAll(Context ctx) {
        List<SavedRoom> out = new ArrayList<>();
        String raw = prefs(ctx).getString(KEY, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                out.add(SavedRoom.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            // Corrupt data: start with an empty list rather than crash.
        }
        return out;
    }

    public static void save(Context ctx, SavedRoom room) {
        if (room.id == null || room.id.isEmpty()) room.id = UUID.randomUUID().toString();
        List<SavedRoom> all = loadAll(ctx);
        all.add(0, room); // newest first
        write(ctx, all);
    }

    public static void delete(Context ctx, String id) {
        List<SavedRoom> all = loadAll(ctx);
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(id)) {
                all.remove(i);
                break;
            }
        }
        write(ctx, all);
    }

    private static void write(Context ctx, List<SavedRoom> rooms) {
        JSONArray arr = new JSONArray();
        try {
            for (SavedRoom r : rooms) arr.put(r.toJson());
        } catch (JSONException e) {
            return;
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
