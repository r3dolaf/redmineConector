package redmineconnector.util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RollingFileLogger {
    private static final String LOG_FILE = "redmine_connector.log";
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private static Thread writerThread;

    public static void init() {
        if (writerThread != null)
            return;

        writerThread = new Thread(() -> {
            while (true) {
                try {
                    String msg = queue.take();
                    writeInternal(msg);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    redmineconnector.util.LoggerUtil.logError("RollingFileLogger",
                            "Failed to write log message", e);
                }
            }
        });
        writerThread.setDaemon(true);
        writerThread.start();
    }

    public static void log(String msg) {
        if (writerThread == null)
            init();
        queue.offer(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " " + msg);
    }

    private static void writeInternal(String msg) {
        File file = new File(LOG_FILE);
        boolean append = true;

        if (file.exists() && file.length() > MAX_SIZE) {
            rotateLogs(file);
            append = false;
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(file, append))) {
            pw.println(msg);
        } catch (Exception ignored) {
        }
    }

    private static void rotateLogs(File current) {
        File backup = new File(LOG_FILE + ".1.bak");
        if (backup.exists())
            backup.delete();
        current.renameTo(backup);
    }
}
