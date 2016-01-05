package com.czbix.peanutlink;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.czbix.peanutlink.network.model.User;

public class PrefHelper {
    private static final String KEY_USER = "user";

    private final SharedPreferences mPreferences;

    public static PrefHelper getInstance(Context context) {
        return new PrefHelper(context);
    }

    private PrefHelper(Context context) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public User getUser() {
        final String string = mPreferences.getString(KEY_USER, null);
        if (string == null) {
            return null;
        }

        return User.fromJson(string);
    }

    public void setUser(User user) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        if (user == null) {
            editor.remove(KEY_USER);
        } else {
            editor.putString(KEY_USER, user.toJson());
        }

        editor.apply();
    }
}
