package org.edx.mobile.http.cache;

import android.content.Context;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.edx.mobile.logger.Logger;
import org.edx.mobile.util.IOUtils;
import org.edx.mobile.util.Sha1Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

@Singleton
public class CacheManager {

    private File cacheFolder;
    protected final Logger logger = new Logger(getClass().getName());

    @Inject
    public CacheManager(Context context) {
        if (context == null) {
            logger.warn("Context must not be NULL");
        }
        cacheFolder = new File(context.getFilesDir(), "http-cache");
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }
    }

    public boolean has(String url) {
        String hash = Sha1Util.SHA1(url);
        File file = new File(cacheFolder, hash);
        return file.exists();
    }

    public void put(String url, String response) throws IOException {
        String hash = Sha1Util.SHA1(url);
        File file = new File(cacheFolder, hash);
        FileOutputStream out = new FileOutputStream(file);
        out.write(response.getBytes());
        out.close();
        logger.debug("Cache.put = " + hash);
    }

    public String get(String url) throws IOException {
        String hash = Sha1Util.SHA1(url);
        File file = new File(cacheFolder, hash);

        if (!file.exists()) {
            logger.debug("Cache.get failed, not cached");
            // not in cache
            return null;
        }

        FileInputStream in = new FileInputStream(file);
        String cache = IOUtils.toString(in, Charset.defaultCharset());
        in.close();
        logger.debug("Cache.get = " + hash);
        return cache;
    }
}
