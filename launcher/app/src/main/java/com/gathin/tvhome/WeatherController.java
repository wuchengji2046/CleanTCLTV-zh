package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** User-selected city forecast, cached across boots, fetched off the UI thread only while home is visible. */
final class WeatherController {
    private static final long REFRESH = 30 * 60 * 1000L;
    private static final long RETRY = 5 * 60 * 1000L;
    private final String urlText, cityName;
    private final boolean configured;
    private final SharedPreferences prefs;
    private final TextView text, updated;
    private final WeatherIconView icon;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Runnable tick = this::refresh;
    private boolean active, fetching, closed, failed;

    WeatherController(Context context, TextView text, TextView updated, WeatherIconView icon) {
        SharedPreferences city = context.getSharedPreferences("weather_city", Context.MODE_PRIVATE);
        configured = city.contains("latitude") && city.contains("longitude");
        cityName = city.getString("name", "");
        urlText = "https://api.open-meteo.com/v1/forecast?latitude=" + city.getString("latitude", "0")
            + "&longitude=" + city.getString("longitude", "0")
            + "&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1";
        this.prefs = context.getSharedPreferences("weather_cache_" + city.getString("id", "unset"), Context.MODE_PRIVATE);
        this.text = text;
        this.updated = updated;
        this.icon = icon;
    }
    void start() { if (closed) return; active = true; render(); if (configured) refresh(); }
    void stop() { active = false; main.removeCallbacks(tick); }
    void close() { closed = true; stop(); worker.shutdownNow(); main.removeCallbacksAndMessages(null); }

    private void refresh() {
        main.removeCallbacks(tick);
        if (!active || closed || fetching) return;
        long age = System.currentTimeMillis() - prefs.getLong("fetched_at", 0);
        if (!failed && age >= 0 && age < REFRESH) {
            render(); main.postDelayed(tick, REFRESH - age); return;
        }
        fetching = true;
        worker.execute(() -> {
            boolean ok = false;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(urlText).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setRequestProperty("Accept", "application/json");
                if (connection.getResponseCode() != 200) throw new java.io.IOException("Weather HTTP error");
                String raw;
                try (InputStream in = connection.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096]; int n;
                    while ((n = in.read(buffer)) != -1) {
                        if (bytes.size() + n > 128 * 1024) throw new java.io.IOException("Weather response too large");
                        bytes.write(buffer, 0, n);
                    }
                    raw = bytes.toString("UTF-8");
                }
                parse(raw); // Do not replace a useful cache with an invalid response.
                prefs.edit().putString("json", raw).putLong("fetched_at", System.currentTimeMillis()).apply();
                ok = true;
            } catch (Exception ignored) {
                // Offline/errors never block navigation or discard the previous forecast.
            } finally { if (connection != null) connection.disconnect(); }
            final boolean success = ok;
            main.post(() -> {
                fetching = false;
                if (closed) return;
                failed = !success;
                if (active) { render(); main.postDelayed(tick, success ? REFRESH : RETRY); }
            });
        });
    }
    private static JSONObject parse(String raw) throws Exception {
        JSONObject result = new JSONObject(raw);
        JSONObject current = result.getJSONObject("current");
        JSONObject daily = result.getJSONObject("daily");
        double temp = current.getDouble("temperature_2m");
        double high = daily.getJSONArray("temperature_2m_max").getDouble(0);
        double low = daily.getJSONArray("temperature_2m_min").getDouble(0);
        if (!Double.isFinite(temp) || !Double.isFinite(high) || !Double.isFinite(low)) throw new IllegalArgumentException("Missing temperature");
        current.getInt("weather_code"); current.getInt("is_day");
        current.getString("time"); daily.getJSONArray("time").getString(0);
        return result;
    }
    private void render() {
        if (!configured) {
            icon.setWeather(-1, false);
            text.setText("选择天气城市"); updated.setText("遥控器选中此处，按确定设置"); return;
        }
        String raw = prefs.getString("json", "");
        try {
            JSONObject value = parse(raw);
            JSONObject current = value.getJSONObject("current");
            JSONObject daily = value.getJSONObject("daily");
            int code = current.getInt("weather_code");
            icon.setWeather(code, current.getInt("is_day") == 1);
            text.setText(String.format(Locale.SIMPLIFIED_CHINESE, "%s  ·  %s  %d°C    ↑%d°  ↓%d°",
                    cityName,
                    description(code), Math.round(current.getDouble("temperature_2m")),
                    Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0)),
                    Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0))));
            long fetched = prefs.getLong("fetched_at", 0);
            long age = System.currentTimeMillis() - fetched;
            SimpleDateFormat time = new SimpleDateFormat(age > 24 * 60 * 60 * 1000L ? "MM-dd HH:mm" : "HH:mm", Locale.CHINA);
            // Update timestamp uses the TV local timezone.
            String state = failed ? "更新失败 · " : age < 0 || age > REFRESH + RETRY ? "缓存 · " : "";
            updated.setText(state + time.format(new java.util.Date(fetched)) + " 更新 · Open-Meteo");
        } catch (Exception ignored) {
            icon.setWeather(-1, false);
            text.setText(cityName + (failed ? "  ·  天气暂时无法更新" : "  ·  天气加载中"));
            updated.setText(failed ? "联网后自动重试 · Open-Meteo" : "Open-Meteo");
        }
    }
    private static String description(int code) {
        if (code == 0) return "晴";
        if (code == 1) return "晴间多云";
        if (code == 2) return "多云";
        if (code == 3) return "阴";
        if (code == 45 || code == 48) return "雾";
        if (code >= 71 && code <= 77 || code == 85 || code == 86) return "雪";
        if (code >= 95) return "雷雨";
        if (code >= 51 && code <= 57) return "毛毛雨";
        if (code >= 61 && code <= 67 || code >= 80 && code <= 82) return "雨";
        return "天气";
    }
}
