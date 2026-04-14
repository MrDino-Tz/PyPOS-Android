package com.dtcteam.pypos.api;

import android.os.Handler;
import android.os.Looper;
import com.dtcteam.pypos.model.*;
import com.google.gson.*;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ApiService {
    private static ApiService instance;
    private final SupabaseClient supabase;
    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;

    private ApiService() {
        supabase = SupabaseClient.getInstance();
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    
    public interface StringCallback {
        void onError(String error);
    }

    public void login(String email, String password, Callback<User> callback) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        String url = SupabaseClient.getSUPABASE_URL() + "/auth/v1/token?grant_type=password";
        
        RequestBody requestBody = RequestBody.create(
            supabase.toJson(body),
            SupabaseClient.JSON
        );

        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    android.util.Log.d("PyPOS", "Login response code: " + response.code());
                    android.util.Log.d("PyPOS", "Login response body: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        
                        if (json == null) {
                            mainHandler.post(() -> callback.onError("Invalid response"));
                            return;
                        }
                        
                        JsonObject userJson = json.has("user") ? json.getAsJsonObject("user") : null;
                        String accessToken = json.has("access_token") ? json.get("access_token").getAsString() : null;

                        if (userJson == null || accessToken == null) {
                            android.util.Log.e("PyPOS", "Missing user or access_token in response");
                            mainHandler.post(() -> callback.onError("Invalid login response"));
                            return;
                        }

                        String userId = userJson.has("id") ? userJson.get("id").getAsString() : "";
                        String userEmail = userJson.has("email") ? userJson.get("email").getAsString() : "";

                        supabase.setSession(accessToken, userId, userEmail);

                        User user = new User();
                        user.setId(userId);
                        user.setEmail(userEmail);
                        user.setUsername(userEmail.split("@")[0]);
                        user.setRole("admin");
                        user.setFullName(userEmail);

                        mainHandler.post(() -> callback.onSuccess(user));
                    } else {
                        android.util.Log.e("PyPOS", "Login failed with code: " + response.code() + " body: " + responseBody);
                        final String[] errorMsg = {"Login failed"};
                        try {
                            JsonObject errorJson = gson.fromJson(responseBody, JsonObject.class);
                            errorMsg[0] = errorJson.has("error_description") 
                                ? errorJson.get("error_description").getAsString()
                                : errorJson.has("msg") 
                                    ? errorJson.get("msg").getAsString() 
                                    : errorJson.has("error") 
                                        ? errorJson.get("error").getAsString()
                                        : "Login failed";
                        } catch (Exception e) {}
                        mainHandler.post(() -> callback.onError(errorMsg[0]));
                    }
                } catch (Exception e) {
                    android.util.Log.e("PyPOS", "Login exception: " + e.getMessage());
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("PyPOS", "Login failure: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getCurrentUser(Callback<User> callback) {
        if (supabase.getAccessToken() == null) {
            callback.onError("Not logged in");
            return;
        }

        String url = SupabaseClient.getSUPABASE_URL() + "/auth/v1/user";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .get()
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        User user = new User();
                        user.setId(json.has("id") ? json.get("id").getAsString() : "");
                        user.setEmail(json.has("email") ? json.get("email").getAsString() : "");
                        user.setUsername(json.has("email") ? json.get("email").getAsString().split("@")[0] : "");
                        user.setRole("admin");
                        String fullName = json.has("email") ? json.get("email").getAsString() : "";
                        if (json.has("user_metadata")) {
                            JsonObject meta = json.getAsJsonObject("user_metadata");
                            if (meta != null && meta.has("full_name") && !meta.get("full_name").isJsonNull()) {
                                fullName = meta.get("full_name").getAsString();
                            }
                        }
                        user.setFullName(fullName);
                        mainHandler.post(() -> callback.onSuccess(user));
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to get user"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void logout(Callback<Void> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/auth/v1/logout";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .post(RequestBody.create("", SupabaseClient.JSON))
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                supabase.clearSession();
                mainHandler.post(() -> callback.onSuccess(null));
            }

            @Override
            public void onFailure(Call call, IOException e) {
                supabase.clearSession();
                mainHandler.post(() -> callback.onSuccess(null));
            }
        });
    }

    public void getCategories(Callback<List<Category>> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/categories?select=*&order=name.asc";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .get()
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                        List<Category> categories = new ArrayList<>();
                        for (int i = 0; i < array.size(); i++) {
                            JsonObject json = array.get(i).getAsJsonObject();
                            Category cat = new Category();
                            cat.setId(json.has("id") ? json.get("id").getAsInt() : 0);
                            cat.setName(json.has("name") && !json.get("name").isJsonNull() ? json.get("name").getAsString() : "");
                            if (json.has("description") && !json.get("description").isJsonNull()) {
                                cat.setDescription(json.get("description").getAsString());
                            }
                            categories.add(cat);
                        }
                        mainHandler.post(() -> callback.onSuccess(categories));
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to load categories"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getItems(String search, Integer categoryId, Callback<List<Item>> callback) {
        StringBuilder url = new StringBuilder(SupabaseClient.getSUPABASE_URL() + "/rest/v1/items?select=*,categories(name)");
        
        List<String> filters = new ArrayList<>();
        if (search != null && !search.isEmpty()) {
            filters.add("name.ilike.*" + search + "*");
            filters.add("sku.ilike.*" + search + "*");
        }
        if (categoryId != null && categoryId > 0) {
            filters.add("category_id.eq." + categoryId);
        }
        
        if (!filters.isEmpty()) {
            url.append("&or=(").append(String.join(",", filters)).append(")");
        }
        url.append("&order=name.asc");

        Request request = new Request.Builder()
            .url(url.toString())
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .get()
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                        List<Item> items = new ArrayList<>();
                        for (int i = 0; i < array.size(); i++) {
                            JsonObject json = array.get(i).getAsJsonObject();
                            Item item = new Item();
                            item.setId(json.has("id") ? json.get("id").getAsInt() : 0);
                            item.setName(json.has("name") ? json.get("name").getAsString() : "");
                            item.setSku(json.has("sku") && !json.get("sku").isJsonNull() ? json.get("sku").getAsString() : "");
                            if (json.has("category_id") && !json.get("category_id").isJsonNull()) {
                                item.setCategoryId(json.get("category_id").getAsInt());
                            }
                            item.setUnitPrice(json.has("unit_price") && !json.get("unit_price").isJsonNull() ? json.get("unit_price").getAsDouble() : 0.0);
                            item.setQuantity(json.has("quantity") && !json.get("quantity").isJsonNull() ? json.get("quantity").getAsInt() : 0);
                            item.setMinStockLevel(json.has("min_stock_level") && !json.get("min_stock_level").isJsonNull() ? json.get("min_stock_level").getAsInt() : 0);
                            item.setActive(!json.has("is_active") || (json.get("is_active").isJsonNull() || json.get("is_active").getAsBoolean()));
                            item.setService(json.has("is_service") && !json.get("is_service").isJsonNull() && json.get("is_service").getAsBoolean());
                            
                            if (json.has("categories") && !json.get("categories").isJsonNull()) {
                                JsonObject cat = json.getAsJsonObject("categories");
                                if (cat != null && cat.has("name") && !cat.get("name").isJsonNull()) {
                                    item.setCategoryName(cat.get("name").getAsString());
                                }
                            }
                            
                            items.add(item);
                        }
                        mainHandler.post(() -> callback.onSuccess(items));
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to load items"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getSales(Callback<List<Sale>> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/sales?select=*,sale_items(*,items(name))&order=created_at.desc&limit=100";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .get()
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                        List<Sale> sales = new ArrayList<>();
                        for (int i = 0; i < array.size(); i++) {
                            JsonObject json = array.get(i).getAsJsonObject();
                            Sale sale = new Sale();
                            sale.setId(json.has("id") ? json.get("id").getAsInt() : 0);
                            sale.setTotalAmount(json.has("total_amount") && !json.get("total_amount").isJsonNull() ? json.get("total_amount").getAsDouble() : 0.0);
                            sale.setFinalAmount(json.has("final_amount") && !json.get("final_amount").isJsonNull() ? json.get("final_amount").getAsDouble() : 0.0);
                            sale.setDiscountAmount(json.has("discount_amount") && !json.get("discount_amount").isJsonNull() ? json.get("discount_amount").getAsDouble() : 0.0);
                            sale.setPaymentMethod(json.has("payment_method") && !json.get("payment_method").isJsonNull() ? json.get("payment_method").getAsString() : "cash");
                            sale.setCreatedAt(json.has("created_at") && !json.get("created_at").isJsonNull() ? json.get("created_at").getAsString() : "");
                            
                            List<SaleItem> saleItems = new ArrayList<>();
                            if (json.has("sale_items") && !json.get("sale_items").isJsonNull()) {
                                JsonArray itemsArray = json.getAsJsonArray("sale_items");
                                for (int j = 0; j < itemsArray.size(); j++) {
                                    JsonObject itemJson = itemsArray.get(j).getAsJsonObject();
                                    SaleItem si = new SaleItem();
                                    si.setId(itemJson.has("id") ? itemJson.get("id").getAsInt() : 0);
                                    si.setSaleId(itemJson.has("sale_id") ? itemJson.get("sale_id").getAsInt() : 0);
                                    si.setItemId(itemJson.has("item_id") ? itemJson.get("item_id").getAsInt() : 0);
                                    si.setQuantity(itemJson.has("quantity") && !itemJson.get("quantity").isJsonNull() ? itemJson.get("quantity").getAsInt() : 0);
                                    si.setUnitPrice(itemJson.has("unit_price") && !itemJson.get("unit_price").isJsonNull() ? itemJson.get("unit_price").getAsDouble() : 0.0);
                                    si.setSubtotal(itemJson.has("subtotal") && !itemJson.get("subtotal").isJsonNull() ? itemJson.get("subtotal").getAsDouble() : 0.0);
                                    if (itemJson.has("items") && !itemJson.get("items").isJsonNull()) {
                                        JsonObject itemObj = itemJson.getAsJsonObject("items");
                                        if (itemObj != null && itemObj.has("name") && !itemObj.get("name").isJsonNull()) {
                                            si.setItemName(itemObj.get("name").getAsString());
                                        }
                                    }
                                    saleItems.add(si);
                                }
                            }
                            sale.setSaleItems(saleItems);
                            sales.add(sale);
                        }
                        mainHandler.post(() -> callback.onSuccess(sales));
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to load sales"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void createSale(List<Map<String, Object>> items, String paymentMethod, double discountAmount, String customerName, Callback<Sale> callback) {
        double total = 0;
        for (Map<String, Object> item : items) {
            total += ((Number) item.get("unit_price")).doubleValue() * ((Number) item.get("quantity")).intValue();
        }
        
        JsonObject saleBody = new JsonObject();
        saleBody.addProperty("total_amount", total);
        saleBody.addProperty("final_amount", total - discountAmount);
        saleBody.addProperty("discount_amount", discountAmount);
        saleBody.addProperty("payment_method", paymentMethod);
        if (customerName != null) {
            saleBody.addProperty("customer_name", customerName);
        }

        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/sales";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .addHeader("Prefer", "return=representation")
            .post(RequestBody.create(saleBody.toString(), SupabaseClient.JSON))
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    android.util.Log.d("PyPOS", "Sale created response: " + response.code() + " - " + responseBody);
                    
                    if (response.isSuccessful()) {
                        JsonArray saleArray = gson.fromJson(responseBody, JsonArray.class);
                        JsonObject saleJson = saleArray.get(0).getAsJsonObject();
                        int saleId = saleJson.get("id").getAsInt();
                        
                        createSaleItemsAndDeductStock(saleId, items, () -> {
                            Sale sale = new Sale();
                            sale.setId(saleId);
                            sale.setTotalAmount(saleJson.get("total_amount").getAsDouble());
                            sale.setFinalAmount(saleJson.get("final_amount").getAsDouble());
                            mainHandler.post(() -> callback.onSuccess(sale));
                        }, error -> {
                            mainHandler.post(() -> callback.onError(error));
                        });
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to create sale: " + response.code()));
                    }
                } catch (Exception e) {
                    android.util.Log.e("PyPOS", "Error creating sale", e);
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
    
    private void createSaleItemsAndDeductStock(int saleId, List<Map<String, Object>> items, Runnable onComplete, StringCallback onError) {
        if (items.isEmpty()) {
            onComplete.run();
            return;
        }
        
        final int[] completed = {0};
        final boolean[] hasError = {false};
        
        for (Map<String, Object> itemData : items) {
            int itemId = ((Number) itemData.get("item_id")).intValue();
            int quantity = ((Number) itemData.get("quantity")).intValue();
            double unitPrice = ((Number) itemData.get("unit_price")).doubleValue();
            double subtotal = quantity * unitPrice;
            
            JsonObject saleItemBody = new JsonObject();
            saleItemBody.addProperty("sale_id", saleId);
            saleItemBody.addProperty("item_id", itemId);
            saleItemBody.addProperty("quantity", quantity);
            saleItemBody.addProperty("unit_price", unitPrice);
            saleItemBody.addProperty("subtotal", subtotal);
            
            String saleItemUrl = SupabaseClient.getSUPABASE_URL() + "/rest/v1/sale_items";
            
            Request saleItemRequest = new Request.Builder()
                .url(saleItemUrl)
                .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
                .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
                .post(RequestBody.create(saleItemBody.toString(), SupabaseClient.JSON))
                .build();
            
            client.newCall(saleItemRequest).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        android.util.Log.e("PyPOS", "Failed to create sale item: " + response.code());
                        hasError[0] = true;
                    }
                    
                    checkComplete();
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    android.util.Log.e("PyPOS", "Error creating sale item", e);
                    hasError[0] = true;
                    checkComplete();
                }
                
                private void checkComplete() {
                    completed[0]++;
                    if (completed[0] == items.size()) {
                        if (hasError[0]) {
                            // Continue anyway - sale was created
                        }
                        // Deduct stock for each item
                        deductStock(items, 0, onComplete);
                    }
                }
            });
        }
    }
    
    private void deductStock(List<Map<String, Object>> items, int index, Runnable onComplete) {
        if (index >= items.size()) {
            onComplete.run();
            return;
        }
        
        int itemId = ((Number) items.get(index).get("item_id")).intValue();
        int quantity = ((Number) items.get(index).get("quantity")).intValue();
        
        getItemStock(itemId, currentQty -> {
            int newQty = Math.max(0, currentQty - quantity);
            updateItemStock(itemId, newQty, () -> deductStock(items, index + 1, onComplete));
        });
    }
    
    private void getItemStock(int itemId, onStockLoaded callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/items?id=eq." + itemId + "&select=quantity";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .get()
            .build();
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                if (response.isSuccessful() && body.length() > 2) {
                    JsonArray arr = gson.fromJson(body, JsonArray.class);
                    if (arr.size() > 0) {
                        int qty = arr.get(0).getAsJsonObject().get("quantity").getAsInt();
                        callback.onLoaded(qty);
                        return;
                    }
                }
                callback.onLoaded(0);
            }

            @Override
            public void onFailure(Call call, IOException e) {
                callback.onLoaded(0);
            }
        });
    }
    
    private void updateItemStock(int itemId, int quantity, Runnable onComplete) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/items?id=eq." + itemId;
        
        JsonObject body = new JsonObject();
        body.addProperty("quantity", quantity);
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .patch(RequestBody.create(body.toString(), SupabaseClient.JSON))
            .build();
        
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                onComplete.run();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                onComplete.run();
            }
        });
    }
    
    interface onStockLoaded {
        void onLoaded(int quantity);
    }

    public void getDashboardStats(Callback<DashboardStats> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/items?select=id";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .get()
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        JsonArray itemsArray = gson.fromJson(responseBody, JsonArray.class);
                        int totalItems = itemsArray.size();
                        
                        DashboardStats stats = new DashboardStats();
                        stats.setTotalItems(totalItems);
                        stats.setLowStockItems(0);
                        stats.setTodaySales(0);
                        stats.setTodayTransactions(0);
                        
                        mainHandler.post(() -> callback.onSuccess(stats));
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to load stats"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void updateItem(int id, double newPrice, Callback<Item> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/items?id=eq." + id;
        
        JsonObject body = new JsonObject();
        body.addProperty("unit_price", newPrice);
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .patch(RequestBody.create(body.toString(), SupabaseClient.JSON))
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess(null));
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to update"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getLowStockItems(Callback<List<Item>> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/items?select=*&quantity.lt.min_stock_level&is_service.eq.false&order=quantity.asc";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .get()
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                        List<Item> items = new ArrayList<>();
                        for (int i = 0; i < array.size(); i++) {
                            JsonObject json = array.get(i).getAsJsonObject();
                            Item item = parseItem(json);
                            items.add(item);
                        }
                        mainHandler.post(() -> callback.onSuccess(items));
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to load items"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void createItem(Item item, Callback<Item> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/items";
        
        JsonObject body = new JsonObject();
        body.addProperty("name", item.getName());
        body.addProperty("sku", item.getSku());
        if (item.getCategoryId() != null) body.addProperty("category_id", item.getCategoryId());
        body.addProperty("unit_price", item.getUnitPrice());
        body.addProperty("cost", item.getCost());
        body.addProperty("quantity", item.getQuantity());
        body.addProperty("min_stock_level", item.getMinStockLevel());
        body.addProperty("is_active", item.isActive());
        body.addProperty("is_service", item.isService());

        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .addHeader("Prefer", "return=representation")
            .post(RequestBody.create(body.toString(), SupabaseClient.JSON))
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        mainHandler.post(() -> callback.onSuccess(parseItem(json)));
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to create item"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void updateItem(Item item, Callback<Item> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/items?id=eq." + item.getId();
        
        JsonObject body = new JsonObject();
        body.addProperty("name", item.getName());
        body.addProperty("sku", item.getSku());
        if (item.getCategoryId() != null) body.addProperty("category_id", item.getCategoryId());
        body.addProperty("unit_price", item.getUnitPrice());
        body.addProperty("cost", item.getCost());
        body.addProperty("quantity", item.getQuantity());
        body.addProperty("min_stock_level", item.getMinStockLevel());
        body.addProperty("is_active", item.isActive());
        body.addProperty("is_service", item.isService());

        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .addHeader("Prefer", "return=representation")
            .patch(RequestBody.create(body.toString(), SupabaseClient.JSON))
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                        if (array.size() > 0) {
                            mainHandler.post(() -> callback.onSuccess(parseItem(array.get(0).getAsJsonObject())));
                        } else {
                            mainHandler.post(() -> callback.onError("Item not found"));
                        }
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to update item"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void deleteItem(int itemId, Callback<Void> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/items?id=eq." + itemId;
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .delete()
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to delete item"));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void createCategory(Category category, Callback<Category> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/categories";
        
        JsonObject body = new JsonObject();
        body.addProperty("name", category.getName());
        if (category.getDescription() != null) body.addProperty("description", category.getDescription());

        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .addHeader("Prefer", "return=representation")
            .post(RequestBody.create(body.toString(), SupabaseClient.JSON))
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        Category cat = new Category();
                        cat.setId(json.has("id") ? json.get("id").getAsInt() : 0);
                        cat.setName(json.has("name") ? json.get("name").getAsString() : "");
                        cat.setDescription(json.has("description") ? json.get("description").getAsString() : null);
                        mainHandler.post(() -> callback.onSuccess(cat));
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to create category"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void updateCategory(Category category, Callback<Category> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/categories?id=eq." + category.getId();
        
        JsonObject body = new JsonObject();
        body.addProperty("name", category.getName());
        if (category.getDescription() != null) body.addProperty("description", category.getDescription());

        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .addHeader("Prefer", "return=representation")
            .patch(RequestBody.create(body.toString(), SupabaseClient.JSON))
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                        if (array.size() > 0) {
                            JsonObject json = array.get(0).getAsJsonObject();
                            Category cat = new Category();
                            cat.setId(json.has("id") ? json.get("id").getAsInt() : 0);
                            cat.setName(json.has("name") ? json.get("name").getAsString() : "");
                            cat.setDescription(json.has("description") ? json.get("description").getAsString() : null);
                            mainHandler.post(() -> callback.onSuccess(cat));
                        } else {
                            mainHandler.post(() -> callback.onError("Category not found"));
                        }
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to update category"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void deleteCategory(int categoryId, Callback<Void> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/categories?id=eq." + categoryId;
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .delete()
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to delete category"));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getUsers(Callback<List<User>> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/users?select=*&order=email.asc";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .get()
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                        List<User> users = new ArrayList<>();
                        for (int i = 0; i < array.size(); i++) {
                            JsonObject json = array.get(i).getAsJsonObject();
                            User user = new User();
                            user.setId(json.has("id") ? json.get("id").getAsString() : "");
                            user.setEmail(json.has("email") ? json.get("email").getAsString() : "");
                            users.add(user);
                        }
                        mainHandler.post(() -> callback.onSuccess(users));
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to load users"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private Item parseItem(JsonObject json) {
        Item item = new Item();
        item.setId(json.has("id") ? json.get("id").getAsInt() : 0);
        item.setName(json.has("name") && !json.get("name").isJsonNull() ? json.get("name").getAsString() : "");
        item.setSku(json.has("sku") && !json.get("sku").isJsonNull() ? json.get("sku").getAsString() : "");
        item.setUnitPrice(json.has("unit_price") && !json.get("unit_price").isJsonNull() ? json.get("unit_price").getAsDouble() : 0.0);
        item.setCost(json.has("cost") && !json.get("cost").isJsonNull() ? json.get("cost").getAsDouble() : 0.0);
        item.setQuantity(json.has("quantity") && !json.get("quantity").isJsonNull() ? json.get("quantity").getAsInt() : 0);
        item.setMinStockLevel(json.has("min_stock_level") && !json.get("min_stock_level").isJsonNull() ? json.get("min_stock_level").getAsInt() : 0);
        item.setActive(json.has("is_active") && !json.get("is_active").isJsonNull() ? json.get("is_active").getAsBoolean() : true);
        item.setService(json.has("is_service") && !json.get("is_service").isJsonNull() ? json.get("is_service").getAsBoolean() : false);
        if (json.has("category_id") && !json.get("category_id").isJsonNull()) {
            item.setCategoryId(json.get("category_id").getAsInt());
        }
        if (json.has("categories") && !json.get("categories").isJsonNull()) {
            JsonObject catJson = json.getAsJsonObject("categories");
            item.setCategoryName(catJson.has("name") ? catJson.get("name").getAsString() : "");
        }
        return item;
    }

    public void createStockMovement(StockMovement movement, Callback<Void> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/stock_movements";
        
        JsonObject body = new JsonObject();
        body.addProperty("item_id", movement.getItemId());
        body.addProperty("type", movement.getType());
        body.addProperty("quantity", movement.getQuantity());
        if (movement.getReference() != null) body.addProperty("reference", movement.getReference());
        if (movement.getNotes() != null) body.addProperty("notes", movement.getNotes());

        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .post(RequestBody.create(body.toString(), SupabaseClient.JSON))
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to create movement"));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getStockMovements(Callback<List<StockMovement>> callback) {
        String url = SupabaseClient.getSUPABASE_URL() + "/rest/v1/stock_movements?select=*,items(name)&order=created_at.desc&limit=50";
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", "sb_publishable_8tb4LzD6ZvfIUa04TSQSDA_FsSe7vF5")
            .addHeader("Authorization", "Bearer " + supabase.getAccessToken())
            .get()
            .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        JsonArray array = gson.fromJson(responseBody, JsonArray.class);
                        List<StockMovement> movements = new ArrayList<>();
                        for (int i = 0; i < array.size(); i++) {
                            JsonObject json = array.get(i).getAsJsonObject();
                            StockMovement sm = new StockMovement();
                            sm.setId(json.has("id") ? json.get("id").getAsInt() : 0);
                            sm.setItemId(json.has("item_id") ? json.get("item_id").getAsInt() : 0);
                            sm.setType(json.has("type") && !json.get("type").isJsonNull() ? json.get("type").getAsString() : "");
                            sm.setQuantity(json.has("quantity") ? json.get("quantity").getAsInt() : 0);
                            sm.setReference(json.has("reference") && !json.get("reference").isJsonNull() ? json.get("reference").getAsString() : null);
                            sm.setNotes(json.has("notes") && !json.get("notes").isJsonNull() ? json.get("notes").getAsString() : null);
                            sm.setCreatedAt(json.has("created_at") && !json.get("created_at").isJsonNull() ? json.get("created_at").getAsString() : "");
                            if (json.has("items") && !json.get("items").isJsonNull()) {
                                JsonObject itemJson = json.getAsJsonObject("items");
                                sm.setItemName(itemJson.has("name") ? itemJson.get("name").getAsString() : "");
                            }
                            movements.add(sm);
                        }
                        mainHandler.post(() -> callback.onSuccess(movements));
                    } else {
                        mainHandler.post(() -> callback.onError("Failed to load movements"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
