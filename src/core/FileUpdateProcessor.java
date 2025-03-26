package core;

import map.InMemoryMapManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

public class FileUpdateProcessor extends Thread {
    private final BlockingQueue<File> updateQueue;
    private final InMemoryMapManager mapManager;

    private volatile boolean running = true;

    public FileUpdateProcessor(BlockingQueue<File> updateQueue, InMemoryMapManager mapManager) {
        this.updateQueue = updateQueue;
        this.mapManager = mapManager;
        setName("FileUpdateProcessor");
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        while (running) {
            try {
                File file = updateQueue.take();
                processFile(file);
            } catch (InterruptedException e) {
                if (!running) break;
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("Failed to process updated file: " + e.getMessage());
            }
        }

        System.out.println("FileUpdateProcessor stopped.");
    }

    private void processFile(File file) {
        boolean isCsv = file.getName().endsWith(".csv");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            boolean skip = isCsv;

            while ((line = reader.readLine()) != null) {
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
    }


}