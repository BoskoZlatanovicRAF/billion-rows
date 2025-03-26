package monitor;

import core.JobManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class DirectoryMonitor extends Thread {
    private final File directory;
    private final BlockingQueue<File> updateQueue;
    private final Map<String, Long> fileTimestamps = new HashMap<>();
    private final Set<String> currentFiles = new HashSet<>();
    private final JobManager jobManager;

    private volatile boolean running = true;

    public DirectoryMonitor(String path, BlockingQueue<File> updateQueue, JobManager jobManager) {
        this.directory = new File(path);
        this.updateQueue = updateQueue;
        this.jobManager = jobManager;
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
                detectChanges();
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



    public void detectChanges() {
        File[] files = directory.listFiles((f, name) -> name.endsWith(".txt") || name.endsWith(".csv"));
        Set<String> currentFiles = new HashSet<>();
        boolean directoryChanged = false;

        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                long lastModified = file.lastModified();
                currentFiles.add(name);

                if (!fileTimestamps.containsKey(name) || fileTimestamps.get(name) != lastModified) {
                    fileTimestamps.put(name, lastModified);
                    System.out.println("Detected update: " + name);
                    try {
                        updateQueue.put(file);
                        directoryChanged = true;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("Interrupted while adding file to update queue: " + name);
                        return; // Prekini izvršavanje ako je nit prekinuta
                    }
                }
            }

            // Proveri ima li obrisanih fajlova
            Set<String> removed = new HashSet<>(fileTimestamps.keySet());
            removed.removeAll(currentFiles);

            for (String removedFile : removed) {
                System.out.println("File removed: " + removedFile);
                fileTimestamps.remove(removedFile);
                directoryChanged = true;
            }

            // Ako je bilo promena, obavesti JobManager
            if (directoryChanged) {
                jobManager.onDirectoryChanged();
            }
        }
    }
}