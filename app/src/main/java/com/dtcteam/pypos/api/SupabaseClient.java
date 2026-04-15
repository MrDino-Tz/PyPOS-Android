package com.dtcteam.pypos.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SupabaseClient {
    private static final String SUPABASE_URL = "https://dbocluzncuhhlrkeggez.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5";
    
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private static SupabaseClient instance;
    private final OkHttpClient client;
    private final Gson gson;
    private String accessToken;
    private String userId;
    private String userEmail;
    private String userRole;
    
    private SupabaseClient() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
        gson = new Gson();
    }
    
    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }
    
    public void setSession(String token, String id, String email) {
        this.accessToken = token;
        this.userId = id;
        this.userEmail = email;
    }
    
    public void setSession(String token, String id, String email, String role) {
        this.accessToken = token;
        this.userId = id;
        this.userEmail = email;
        this.userRole = role;
    }
    
    public void clearSession() {
        this.accessToken = null;
        this.userId = null;
        this.userEmail = null;
        this.userRole = null;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public String getUserRole() {
        return userRole;
    }
    
    public boolean isAdmin() {
        return "admin".equals(userRole);
    }
    
    private Request.Builder buildRequest() {
        Request.Builder builder = new Request.Builder()
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Content-Type", "application/json");
        if (accessToken != null) {
            builder.addHeader("Authorization", "Bearer " + accessToken);
        }
        return builder;
    }
    
    public Response execute(Request request) throws IOException {
        return client.newCall(request).execute();
    }
    
    public <T> T fromJson(JsonObject json, Class<T> cls) {
        return gson.fromJson(json, cls);
    }
    
    public <T> List<T> fromJsonArray(JsonArray array, Class<T> cls) {
        return gson.fromJson(array, TypeToken.getParameterized(List.class, cls).getType());
    }
    
    public JsonObject toJsonObject(Object obj) {
        return gson.toJsonTree(obj).getAsJsonObject();
    }
    
    public String toJson(Object obj) {
        return gson.toJson(obj);
    }
    
    public static String getSUPABASE_URL() {
        return SUPABASE_URL;
    }
}
