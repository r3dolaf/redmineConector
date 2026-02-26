package redmineconnector.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

import redmineconnector.util.LoggerUtil;

/**
 * Service to handle persistent caching of downloaded images.
 * Stores images in user's home directory under .redmineconnector/cache
 */
public class ImageCacheService {

    private static final String CACHE_DIR = "cache" + File.separator + "images";
    private static ImageCacheService instance;
    private final File cacheFolder;

    private ImageCacheService() {
        cacheFolder = new File(CACHE_DIR);
        if (!cacheFolder.exists()) {
            if (cacheFolder.mkdirs()) {
                LoggerUtil.logInfo("ImageCacheService", "Created cache directory at " + CACHE_DIR);
            } else {
                LoggerUtil.logError("ImageCacheService", "Failed to create cache directory at " + CACHE_DIR);
            }
        }
    }

    public static synchronized ImageCacheService getInstance() {
        if (instance == null) {
            instance = new ImageCacheService();
        }
        return instance;
    }

    public File getCacheDir() {
        return cacheFolder;
    }

    /**
     * Gets a cached file if it exists.
     * 
     * @param filename The original filename from Redmine
     * @return File object if exists in cache, null otherwise
     */
    public File getCachedImage(String filename) {
        if (filename == null || filename.isEmpty())
            return null;

        File f = new File(cacheFolder, sanitize(filename));
        if (f.exists() && f.length() > 0) {
            return f;
        }
        return null;
    }

    /**
     * Saves data to cache.
     * 
     * @param filename Original filename
     * @param data     Image bytes
     * @return The cached File
     */
    public File saveImage(String filename, byte[] data) {
        if (filename == null || data == null || data.length == 0)
            return null;

        File f = new File(cacheFolder, sanitize(filename));
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(data);
            return f;
        } catch (IOException e) {
            LoggerUtil.logError("ImageCacheService", "Failed to save " + filename + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Sanitizes filename to be safe for file system.
     * Just keeps it simple, maybe hash it if filenames are too long or weird?
     * Redmine filenames are usually safeish, but let's be careful.
     */
    private String sanitize(String filename) {
        // Replace chars that might be invalid in Windows/Linux paths
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }

    public void clearCache() {
        File[] files = cacheFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        LoggerUtil.logInfo("ImageCacheService", "Cache cleared.");
    }

    /**
     * Cleans up files older than the specified age.
     * 
     * @param maxAgeMillis Max age in milliseconds
     */
    public void cleanupOldFiles(long maxAgeMillis) {
        File[] files = cacheFolder.listFiles();
        if (files == null)
            return;

        long now = System.currentTimeMillis();
        int deleted = 0;

        for (File f : files) {
            if (now - f.lastModified() > maxAgeMillis) {
                if (f.delete()) {
                    deleted++;
                }
            }
        }

        if (deleted > 0) {
            LoggerUtil.logInfo("ImageCacheService", "Cleaned up " + deleted + " old files from cache.");
        }
    }
}
