package com.gettingreal.bpos.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.FileNotFoundException;

/**
 * Created by ivanfoong on 31/3/14.
 */
public class ImageHelper {
    public static Bitmap decodeBitmapUri(final Context aContext, final Uri aSelectedImageURI) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(aContext.getContentResolver().openInputStream(aSelectedImageURI), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 300;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(aContext.getContentResolver().openInputStream(aSelectedImageURI), null, o2);

    }

    public static Bitmap squareBitmap(final Bitmap aBitmap, final int aSize) {
        Bitmap newBitmap = null;

        int finalSize = aSize;
        if (finalSize > aBitmap.getWidth()) {
            finalSize = aBitmap.getWidth();
        }
        if (finalSize > aBitmap.getHeight()) {
            finalSize = aBitmap.getHeight();
        }

        if (aBitmap.getWidth() >= aBitmap.getHeight()) {
            newBitmap = Bitmap.createBitmap(
                aBitmap,
                aBitmap.getWidth() / 2 - aBitmap.getHeight() / 2,
                0,
                finalSize,
                finalSize
            );

        } else {
            newBitmap = Bitmap.createBitmap(
                aBitmap,
                0,
                aBitmap.getHeight() / 2 - aBitmap.getWidth() / 2,
                finalSize,
                finalSize
            );
        }

        return newBitmap;
    }
}
