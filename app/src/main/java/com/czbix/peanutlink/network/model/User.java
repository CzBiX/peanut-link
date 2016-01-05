package com.czbix.peanutlink.network.model;

import android.util.JsonWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;

public class User {
    public final String uuid;
    public final String ucode;
    public final String session;

    public User(String uuid, String ucode, String session) {
        this.uuid = uuid;
        this.ucode = ucode;
        this.session = session;
    }

    public static User fromJson(String str) {
        final JSONObject json;
        try {
            json = new JSONObject(str);

            return new User(json.getString("uuid"), json.getString("ucode"), json.getString("session"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String toJson() {
        final StringWriter sw = new StringWriter(256);
        final JsonWriter writer = new JsonWriter(sw);

        try {
            writer.beginObject()
                    .name("uuid").value(uuid)
                    .name("ucode").value(ucode)
                    .name("session").value(session)
                    .endObject()
                    .close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sw.toString();
    }
}
