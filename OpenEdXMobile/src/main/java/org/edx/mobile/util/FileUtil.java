package org.edx.mobile.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by miankhalid on 8/3/15.
 */
public class FileUtil {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * Returns the text of a file as a String object
     *
     * @param context  The current context
     * @param fileName The name of the file to load from assets folder
     * @return The text content of the file
     */
    public static String loadTextFileFromAssets
            (Context context, String fileName) throws IOException {
        InputStream inputStream = context.getAssets().open(fileName);
        try {
            OutputStream outputStream = new ByteArrayOutputStream();
            try {
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                for (int n; (n = inputStream.read(buffer)) >= 0;) {
                    outputStream.write(buffer, 0, n);
                }
                return outputStream.toString();
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }
    }

    /**
     * Deletes a single file and/or recursively deletes files/folders within a folder before
     * deleting it.
     *
     * @param fileOrDirectory The file/folder that needs to be deleted.
     * @param exceptions      Name of the file/folder that needs to be skipped while deletion.
     */
    public static void deleteRecursive(@NonNull File fileOrDirectory,
                                       @Nullable List<String> exceptions) {
        if (exceptions != null && exceptions.contains(fileOrDirectory.getName())) return;

        if (fileOrDirectory.isDirectory()) {
            File[] filesList = fileOrDirectory.listFiles();
            if (filesList != null) {
                for (File child : filesList) {
                    deleteRecursive(child, exceptions);
                }
            }
        }
        fileOrDirectory.delete();
    }
}
