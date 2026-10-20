package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.widget.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.*;

/** Explicit city selection: no GPS or Google services required. */
public class WeatherCityActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private LinearLayout results;
    private TextView status;
    private Button search;
    private boolean closed;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(48, 24, 48, 24);
        page.setBackgroundColor(Color.rgb(15, 24, 38));
        TextView title = new TextView(this);
        title.setText("天气城市 · 输入城市名后搜索");
        title.setTextSize(24); title.setTextColor(Color.WHITE); page.addView(title);
        EditText query = new EditText(this);
        query.setSingleLine(true); query.setHint("例如：长春、上海、Chengdu");
        query.setTextColor(Color.WHITE); query.setHintTextColor(Color.LTGRAY);
        page.addView(query);
        search = new Button(this); search.setText("搜索城市"); page.addView(search);
        status = new TextView(this); status.setTextColor(Color.LTGRAY);
        status.setText("选择结果后自动保存。城市查询：Open-Meteo / GeoNames"); page.addView(status);
        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(results); page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        Button clear = new Button(this); clear.setText("清除城市，暂不显示天气");
        clear.setOnClickListener(v -> { getSharedPreferences("weather_city", MODE_PRIVATE).edit().clear().apply(); finish(); });
        page.addView(clear); setContentView(page);
        search.setOnClickListener(v -> {
            String name = query.getText().toString().trim();
            if (name.length() < 2 || name.length() > 100) { status.setText("请输入 2–100 个字符的城市名称"); return; }
            search.setEnabled(false); results.removeAllViews(); status.setText("搜索中…");
            worker.execute(() -> {
                JSONArray cities = null;
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL("https://geocoding-api.open-meteo.com/v1/search?name="
                            + URLEncoder.encode(name, "UTF-8") + "&count=10&language=zh&format=json").openConnection();
                    connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
                    if (connection.getResponseCode() != 200) throw new java.io.IOException("HTTP error");
                    try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[4096]; int n;
                        while ((n = in.read(buffer)) != -1) {
                            if (out.size() + n > 131072) throw new java.io.IOException("Response too large");
                            out.write(buffer, 0, n);
                        }
                        cities = new JSONObject(out.toString("UTF-8")).optJSONArray("results");
                        if (cities == null) cities = new JSONArray();
                    }
                } catch (Exception ignored) { }
                finally { if (connection != null) connection.disconnect(); }
                final JSONArray matches = cities;
                main.post(() -> {
                    if (closed) return;
                    search.setEnabled(true);
                    status.setText(matches == null ? "搜索失败，请检查网络后重试" : matches.length() == 0
                            ? "未找到城市，可尝试拼音或英文名称" : "请选择城市，注意省份和国家 · Open-Meteo / GeoNames");
                    if (matches == null) return;
                    for (int i = 0; i < matches.length(); i++) {
                        JSONObject city = matches.optJSONObject(i);
                        if (city == null || !city.has("latitude") || !city.has("longitude")) continue;
                        double lat = city.optDouble("latitude"), lon = city.optDouble("longitude");
                        if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) continue;
                        String label = city.optString("name", "") + " · " + city.optString("admin1", "") + " · " + city.optString("country", "");
                        Button pick = new Button(this); pick.setText(label); results.addView(pick);
                        pick.setOnClickListener(w -> {
                            getSharedPreferences("weather_city", MODE_PRIVATE).edit()
                                .putString("name", city.optString("name", "城市"))
                                .putString("latitude", Double.toString(lat)).putString("longitude", Double.toString(lon))
                                .putString("id", city.optString("id", lat + "," + lon)).apply();
                            finish();
                        });
                    }
                    if (results.getChildCount() > 0) results.getChildAt(0).requestFocus();
                });
            });
        });
    }
    @Override public void onDestroy() {
        closed = true; worker.shutdownNow(); main.removeCallbacksAndMessages(null); super.onDestroy();
    }
}
