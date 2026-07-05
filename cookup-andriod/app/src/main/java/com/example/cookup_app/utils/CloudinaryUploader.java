package com.example.cookup_app.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.example.cookup_app.BuildConfig;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Uploads images directly from the device to Cloudinary using an UNSIGNED
 * upload preset, so no backend/API secret is needed on the client.
 *
 * Setup required (free, no credit card):
 * 1. Create a free account at https://cloudinary.com
 * 2. Copy your "Cloud name" from the Dashboard.
 * 3. Settings > Upload > Upload presets > Add upload preset, set
 *    Signing Mode = "Unsigned", give it a name.
 * 4. Put both values into the project's .env file as:
 *    CLOUDINARY_CLOUD_NAME=your_cloud_name
 *    CLOUDINARY_UPLOAD_PRESET=your_preset_name
 */
public class CloudinaryUploader {

    public interface UploadCallback {
        void onSuccess(String secureUrl);
        void onFailure(Exception e);
    }

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * Uploads a local image (file:// or content:// Uri) to Cloudinary.
     * The callback is always invoked on the main thread.
     */
    public static void uploadImage(Context context, Uri localUri, UploadCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        String cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME;
        String uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET;

        if (cloudName == null || cloudName.isEmpty() || cloudName.equals("YOUR_CLOUD_NAME")
                || uploadPreset == null || uploadPreset.isEmpty() || uploadPreset.equals("YOUR_UNSIGNED_PRESET_NAME")) {
            mainHandler.post(() -> callback.onFailure(
                    new IllegalStateException("Chưa cấu hình Cloudinary trong file .env (CLOUDINARY_CLOUD_NAME / CLOUDINARY_UPLOAD_PRESET).")));
            return;
        }

        byte[] imageBytes;
        try {
            java.io.InputStream inputStream = context.getContentResolver().openInputStream(localUri);
            if (inputStream == null) {
                mainHandler.post(() -> callback.onFailure(new IOException("Không đọc được file ảnh đã chọn.")));
                return;
            }
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            inputStream.close();
            imageBytes = buffer.toByteArray();
        } catch (Exception e) {
            mainHandler.post(() -> callback.onFailure(e));
            return;
        }

        if (imageBytes.length == 0) {
            mainHandler.post(() -> callback.onFailure(new IOException("File ảnh rỗng hoặc không tồn tại.")));
            return;
        }

        String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";

        RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "upload.jpg", fileBody)
                .addFormDataPart("upload_preset", uploadPreset)
                .build();

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) {
                try (Response r = response) {
                    String bodyStr = r.body() != null ? r.body().string() : "";
                    if (!r.isSuccessful()) {
                        // Full error is logged here because Toast truncates long text.
                        // View via Android Studio > Logcat, filter tag "CloudinaryUploader".
                        android.util.Log.e("CloudinaryUploader", "Upload failed (" + r.code() + "): " + bodyStr);
                        mainHandler.post(() -> callback.onFailure(
                                new IOException("Cloudinary lỗi (" + r.code() + "): " + bodyStr)));
                        return;
                    }
                    JSONObject json = new JSONObject(bodyStr);
                    String secureUrl = json.optString("secure_url", null);
                    if (secureUrl == null || secureUrl.isEmpty()) {
                        mainHandler.post(() -> callback.onFailure(new IOException("Phản hồi Cloudinary không có secure_url.")));
                        return;
                    }
                    mainHandler.post(() -> callback.onSuccess(secureUrl));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }
}
