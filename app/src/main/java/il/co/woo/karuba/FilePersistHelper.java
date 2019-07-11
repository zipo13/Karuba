package il.co.woo.karuba;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

class FilePersistHelper {
    private static final String TAG = "FilePersistHelper";
    private static final String FILENAME = "karuba_storage1.json";

    static String read(Context context) {
        Log.d(TAG, "read: start reading file from storage");
        try {
            FileInputStream fis = context.openFileInput(FILENAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (FileNotFoundException fileNotFound) {
            return null;
        } catch (IOException ioException) {
            return null;
        }
    }

    static boolean create(Context context, String jsonString){
        Log.d(TAG, "create: start creating/writing file from storage");
        try {
            FileOutputStream fos = context.openFileOutput(FILENAME,Context.MODE_PRIVATE);
            if (jsonString != null) {
                fos.write(jsonString.getBytes());
            }
            fos.close();
            return true;
        } catch (FileNotFoundException fileNotFound) {
            return false;
        } catch (IOException ioException) {
            return false;
        }

    }

    public static boolean isFilePresent(Context context) {
        Log.d(TAG, "isFilePresent: Enter");
        String path = context.getFilesDir().getAbsolutePath() + "/" + FILENAME;
        File file = new File(path);
        return file.exists();
    }
}
