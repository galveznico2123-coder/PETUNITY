package com.petunity.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    /**
     * Compresses an image from a Uri and returns a temporary File path.
     */
    public static File compressImage(Context context, Uri imageUri, String fileName) throws IOException {
        if (imageUri == null) {
            throw new IOException("Image URI is null");
        }

        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        if (inputStream == null) {
            throw new IOException("InputStream is null for URI: " + imageUri);
        }

        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(inputStream);
        } finally {
            inputStream.close();
        }

        if (bitmap == null) {
            throw new IOException("Failed to decode bitmap from URI: " + imageUri);
        }

        // Target dimensions (e.g., max 1080p)
        int maxWidth = 1080;
        int maxHeight = 1080;
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        
        float ratio = Math.min((float) maxWidth / originalWidth, (float) maxHeight / originalHeight);
        
        Bitmap resizedBitmap = bitmap;
        if (ratio < 1.0) {
            int width = Math.round(ratio * originalWidth);
            int height = Math.round(ratio * originalHeight);
            resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        File outputDir = context.getCacheDir();
        File outputFile = new File(outputDir, fileName);
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out); // 80% quality
            fos.write(out.toByteArray());
            fos.flush();
        } finally {
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle();
            }
            bitmap.recycle();
        }
        
        return outputFile;
    }
}
