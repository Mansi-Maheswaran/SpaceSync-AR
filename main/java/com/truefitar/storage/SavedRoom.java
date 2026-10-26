package com.truefitar.storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * TrueFit AR - one saved room layout: the measured gap plus every piece of
 * furniture that was placed in it. Positions are stored relative to the gap
 * (not to the camera), so the layout can be redrawn as a 2D plan any time.
 */
public class SavedRoom {

    public static class Item {
        public String type;        // FurnitureType name, e.g. "SOFA"
        public float lengthM;
        public float heightM;
        public float widthM;
        public float along;        // centre, metres from point A along the gap
        public float lateral;      // centre, metres sideways from the gap line
        public float relAngleDeg;  // item rotation relative to the gap
        public boolean fits;       // size ok AND sitting inside the gap

        JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("type", type);
            o.put("l", lengthM);
            o.put("h", heightM);
            o.put("w", widthM);
            o.put("along", along);
            o.put("lat", lateral);
            o.put("rot", relAngleDeg);
            o.put("fits", fits);
            return o;
        }

        static Item fromJson(JSONObject o) {
            Item i = new Item();
            i.type = o.optString("type", "OTHER");
            i.lengthM = (float) o.optDouble("l", 1);
            i.heightM = (float) o.optDouble("h", 1);
            i.widthM = (float) o.optDouble("w", 1);
            i.along = (float) o.optDouble("along", 0);
            i.lateral = (float) o.optDouble("lat", 0);
            i.relAngleDeg = (float) o.optDouble("rot", 0);
            i.fits = o.optBoolean("fits", false);
            return i;
        }
    }

    public String id;
    public String name;
    public long timestamp;
    public float gapMeters;
    public final List<Item> items = new ArrayList<>();

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("time", timestamp);
        o.put("gap", gapMeters);
        JSONArray arr = new JSONArray();
        for (Item i : items) arr.put(i.toJson());
        o.put("items", arr);
        return o;
    }

    static SavedRoom fromJson(JSONObject o) {
        SavedRoom r = new SavedRoom();
        r.id = o.optString("id");
        r.name = o.optString("name", "Room");
        r.timestamp = o.optLong("time");
        r.gapMeters = (float) o.optDouble("gap", 0);
        JSONArray arr = o.optJSONArray("items");
        if (arr != null) {
            for (int k = 0; k < arr.length(); k++) {
                JSONObject io = arr.optJSONObject(k);
                if (io != null) r.items.add(Item.fromJson(io));
            }
        }
        return r;
    }
}
