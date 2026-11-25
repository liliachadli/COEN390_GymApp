package com.example.smartgymtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

// The user's templates. Saved in Firebase (users/{uid}/templates) and kept on the phone too,
// so they show right away and still work without internet.
public class TemplateStore {

    public static List<Template> templates = new ArrayList<>();
    private static String loadedFor = null;   // user id the list belongs to
    private static final WorkoutRepository repo = new WorkoutRepository();

    // Shows the phone copy right away, then the Firebase copy when it arrives
    public static void load(Context context, Runnable onChange) {
        String uid = userId();
        if (uid == null) {
            return;
        }
        if (!uid.equals(loadedFor)) {
            loadedFor = uid;
            List<Template> cached = readCache(context, uid);
            templates = cached != null ? cached : Template.defaults();
            onChange.run();
        }

        Context app = context.getApplicationContext();
        repo.getTemplates(new WorkoutRepository.getTemplatesCallback() {
            @Override
            public void onSuccess(List<Template> fromServer) {
                if (!uid.equals(loadedFor)) {
                    return;   // logged out meanwhile
                }
                if (fromServer.isEmpty()) {
                    // Nothing online yet (new account, or saved offline): upload what we show
                    if (readCache(app, uid) == null) {
                        writeCache(app, uid);
                    }
                    for (Template template : templates) {
                        repo.saveTemplate(template);
                    }
                    return;
                }
                templates = fromServer;
                writeCache(app, uid);
                onChange.run();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("TemplateStore", "Could not load templates, using the phone copy", e);
            }
        });
    }

    public static Template find(String id) {
        for (Template template : templates) {
            if (template.id.equals(id)) {
                return template;
            }
        }
        return null;
    }

    // New or edited template
    public static void save(Context context, Template template) {
        boolean replaced = false;
        for (int i = 0; i < templates.size(); i++) {
            if (templates.get(i).id.equals(template.id)) {
                templates.set(i, template);
                replaced = true;
            }
        }
        if (!replaced) {
            templates.add(template);
        }
        writeCache(context, userId());
        repo.saveTemplate(template);
    }

    public static void delete(Context context, String id) {
        for (int i = templates.size() - 1; i >= 0; i--) {
            if (templates.get(i).id.equals(id)) {
                templates.remove(i);
            }
        }
        writeCache(context, userId());
        repo.deleteTemplate(id);
    }

    // Called on logout so the next user doesn't see these
    public static void clear() {
        templates = new ArrayList<>();
        loadedFor = null;
    }

    // ---------- phone copy (JSON in SharedPreferences, one per user) ----------

    private static void writeCache(Context context, String uid) {
        if (uid == null) {
            return;
        }
        try {
            JSONArray array = new JSONArray();
            for (Template t : templates) {
                JSONObject o = new JSONObject();
                o.put("id", t.id);
                o.put("name", t.name);
                o.put("color", t.color);
                o.put("createdAt", t.createdAt);
                JSONArray exercises = new JSONArray();
                for (int i = 0; i < t.exercises.size(); i++) {
                    exercises.put(new JSONArray().put(t.exercises.get(i)).put(t.equipment.get(i)).put(t.sets.get(i)));
                }
                o.put("exercises", exercises);
                array.put(o);
            }
            prefs(context).edit().putString(uid, array.toString()).apply();
        } catch (Exception e) {
            Log.e("TemplateStore", "Could not save templates on the phone", e);
        }
    }

    // null = never saved on this phone for this user
    private static List<Template> readCache(Context context, String uid) {
        String text = prefs(context).getString(uid, null);
        if (text == null) {
            return null;
        }
        try {
            List<Template> list = new ArrayList<>();
            JSONArray array = new JSONArray(text);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Template t = new Template();
                t.id = o.getString("id");
                t.name = o.getString("name");
                t.color = o.optString("color", "lime");
                t.createdAt = o.optLong("createdAt", 0);
                JSONArray exercises = o.getJSONArray("exercises");
                for (int e = 0; e < exercises.length(); e++) {
                    JSONArray row = exercises.getJSONArray(e);
                    t.exercises.add(row.getString(0));
                    t.equipment.add(row.getString(1));
                    t.sets.add(row.getInt(2));
                }
                list.add(t);
            }
            return list;
        } catch (Exception e) {
            Log.e("TemplateStore", "Broken phone copy, ignoring it", e);
            return null;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("templates", Context.MODE_PRIVATE);
    }

    private static String userId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }
}
