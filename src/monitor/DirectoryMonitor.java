package monitor;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class DirectoryMonitor extends Thread {
    private final File directory;
    private final BlockingQueue<File> updateQueue;
    private final Map<String, Long> fileTimestamps = new HashMap<>();

    private volatile boolean running = true;

    public DirectoryMonitor(String path, BlockingQueue<File> updateQueue) {
        this.directory = new File(path);
        this.updateQueue = updateQueue;
        setName("DirectoryMonitor");
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        System.out.println("Monitoring directory: " + directory.getAbsolutePath());

        while (running) {
            try {
                File[] files = directory.listFiles((dir, name) ->
                        name.endsWith(".txt") || name.endsWith(".csv"));

                if (files != null) {
                    for (File file : files) {
                        long lastModified = file.lastModified();
                        String name = file.getName();

                        if (!fileTimestamps.containsKey(name) || fileTimestamps.get(name) != lastModified) {
                            fileTimestamps.put(name, lastModified);
                            System.out.println("Detected update: " + name);
                            updateQueue.put(file);
                        }
                    }
                }

                Thread.sleep(5000);

            } catch (InterruptedException e) {
                if (!running) break;
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("Directory monitor error: " + e.getMessage());
            }
        }

        System.out.println("DirectoryMonitor stopped.");
    }
}
