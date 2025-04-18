package core;

import map.InMemoryMapManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileUpdateProcessor extends Thread {
    private final BlockingQueue<File> updateQueue;
    private final InMemoryMapManager mapManager;

    private volatile boolean running = true;

    // Deljeni map za lokove za pristup fajlovima
    private static final ConcurrentHashMap<String, ReentrantReadWriteLock> fileLocks = new ConcurrentHashMap<>();

    public static ReentrantReadWriteLock getFileLock(String filePath) {
        return fileLocks.computeIfAbsent(filePath, k -> new ReentrantReadWriteLock());
    }

    public FileUpdateProcessor(BlockingQueue<File> updateQueue, InMemoryMapManager mapManager) {
        this.updateQueue = updateQueue;
        this.mapManager = mapManager;
        setName("FileUpdateProcessor");
    }

    public void shutdown() {
        running = false;
        this.interrupt();
        System.out.println("[FILE-PROCESSOR-THREAD]: File processor thread stopping...");
    }

    @Override
    public void run() {
        while (running) {
            try {
                File file = updateQueue.take();
                processFile(file);
            } catch (InterruptedException e) {
                if (!running) {
                    System.out.println("[FILE-PROCESSOR-THREAD]: File processor thread stopped.");
                    break;
                }

                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("Failed to process updated file: " + e.getMessage());
            }
        }
        Thread.currentThread().interrupt();
        System.out.println("[FILE-PROCESSOR-THREAD]: FileUpdateProcessor stopped.");
    }

    private void processFile(File file) {
        boolean isCsv = file.getName().endsWith(".csv");

        ReentrantReadWriteLock lock = getFileLock(file.getAbsolutePath());
        lock.writeLock().lock();
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8))) {

                String line;
                boolean skip = isCsv;

                while ((line = reader.readLine()) != null && running) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Processing interrupted");
                    }

                    if (skip) {
                        skip = false;
                        continue;
                    }

                    String[] parts = line.split(";");
                    if (parts.length != 2) continue;

                    String station = parts[0].trim();
                    double temp;

                    try {
                        temp = Double.parseDouble(parts[1].trim());
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    mapManager.update(station, temp);
                }

            } catch (IOException e) {
                System.out.println("Error reading file " + file.getName() + ": " + e.getMessage());
            }
        } catch (InterruptedException e) {
            System.out.println("[UPDATE] Process interrupted for file: " + file.getName());
            Thread.currentThread().interrupt();
        } finally {
            lock.writeLock().unlock();
        }
    }


}