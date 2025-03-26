package monitor;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class DirectoryMonitor extends Thread {
    private final File directory;
    private final BlockingQueue<File> updateQueue;
    private final Map<String, Long> fileTimestamps = new HashMap<>();
    private final Set<String> currentFiles = new HashSet<>();

    private volatile boolean running = true;

    public DirectoryMonitor(String path, BlockingQueue<File> updateQueue) {
        this.directory = new File(path);
        this.updateQueue = updateQueue;
        setName("DirectoryMonitor");
    }

    public void shutdown() {
        running = false;
        this.interrupt();
        System.out.println("[MONITOR-THREAD]: Monitor thread stopping...");
    }

    @Override
    public void run() {
        System.out.println("Monitoring directory: " + directory.getAbsolutePath());

        while (running) {
            try {
                File[] files = directory.listFiles((dir, name) ->
                        name.endsWith(".txt") || name.endsWith(".csv"));

                // Čisti set trenutnih fajlova za ovu iteraciju
                currentFiles.clear();

                if (files != null) {
                    for (File file : files) {
                        String name = file.getName();
                        long lastModified = file.lastModified();

                        // Dodaj u set trenutnih fajlova
                        currentFiles.add(name);

                        // Detektuj nove ili promenjene fajlove
                        if (!fileTimestamps.containsKey(name) || fileTimestamps.get(name) != lastModified) {
                            fileTimestamps.put(name, lastModified);
                            System.out.println("Detected update: " + name);
                            updateQueue.put(file);
                        }
                    }

                    // Proveri ima li uklonjenih fajlova
                    Set<String> removed = new HashSet<>(fileTimestamps.keySet());
                    removed.removeAll(currentFiles);

                    // Ukloni iz fileTimestamps one koji više ne postoje
                    for (String removedFile : removed) {
                        System.out.println("File removed: " + removedFile);
                        fileTimestamps.remove(removedFile);
                    }
                }

                Thread.sleep(5000);

            } catch (InterruptedException e) {
                if (!running) {
                    System.out.println("[MONITOR-THREAD]: Monitor thread stopped. (sleep interrupted)");
                    break;
                }
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("Directory monitor error: " + e.getMessage());
            }
        }

        System.out.println("DirectoryMonitor stopped.");
    }
}