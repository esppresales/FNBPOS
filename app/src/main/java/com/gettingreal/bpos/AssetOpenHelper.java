package com.gettingreal.bpos;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ivanfoong on 25/3/14.
 */
public class AssetOpenHelper {

    private Context mContext;

    public AssetOpenHelper(Context   context) {
        mContext = context;
    }


    public void copyAssets() {
        copyFileOrDir("product_images"); // copy all files in assets folder in my project

    }

    private void copyFileOrDir(String path) {
        AssetManager assetManager = mContext.getAssets();
        String assets[] = null;
        try {
            Log.i("tag", "copyFileOrDir() " + path);
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(path);
            } else {
                String fullPath = mContext.getFilesDir().getAbsolutePath() + "/" + path;
                Log.i("tag", "path=" + fullPath);
                File dir = new File(fullPath);
                if (!dir.exists() && !path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit")) {
                    if (!dir.mkdirs()) {
                        Log.i("tag", "could not create dir " + fullPath);
                    }
                    for (int i = 0; i < assets.length; ++i) {
                        String p;
                        if (path.equals(""))
                            p = "";
                        else
                            p = path + "/";

                        if (!path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                            copyFileOrDir(p + assets[i]);
                    }
                }
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
        }
    }

    private void copyFile(String filename) {
        AssetManager assetManager = mContext.getAssets();

        InputStream in = null;
        OutputStream out = null;
        String newFileName = null;
        try {
            Log.i("tag", "copyFile() " + filename);
            in = assetManager.open(filename);
            newFileName = mContext.getFilesDir().getAbsolutePath() + "/" + filename.substring(0, filename.length() - 4);
            File newFile = new File(newFileName);
            newFile.getParentFile().mkdirs();

            if (!newFile.exists()) {
                out = new FileOutputStream(newFile);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
                out.close();
            }

            in.close();
        } catch (Exception e) {
            Log.e("tag", "Exception in copyFile() of " + newFileName);
            Log.e("tag", "Exception in copyFile() " + e.toString());
        }

    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
