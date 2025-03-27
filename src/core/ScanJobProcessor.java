package core;


import cli.impl.ScanCommand;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ScanJobProcessor {
    private static final ConcurrentHashMap<String, ReentrantReadWriteLock> outputLocks = new ConcurrentHashMap<>();

    public static void processFile(File file, ScanCommand command, String outputFile) {
        if (!file.exists()) {
            System.out.println("Skipping missing file: " + file.getName());
            return;
        }

        ReentrantReadWriteLock fileLock = FileUpdateProcessor.getFileLock(file.getAbsolutePath());
        fileLock.readLock().lock();
        try {
            boolean isCsv = file.getName().endsWith(".csv");

            // Uzmi lock za izlazni fajl na poctku obrade ulaznog fajla
            ReentrantReadWriteLock outputLock = outputLocks.computeIfAbsent(outputFile, key -> new ReentrantReadWriteLock());
            outputLock.writeLock().lock();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                 BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {

                String line;
                boolean skipFirst = isCsv;
                int matchCount = 0;

                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println("[SCAN-" + command.getJobName() + "] Thread interrupted, stopping gracefully.");
                        break;
                    }

                    if (skipFirst) {
                        skipFirst = false;
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

                    if (Character.toLowerCase(station.charAt(0)) != Character.toLowerCase(command.getLetter()))
                        continue;

                    if (temp >= command.getMinTemp() && temp <= command.getMaxTemp()) {
                        // Direktno upisi u fajl umesto stringBuilder u memoriji da kesira
                        writer.write(line);
                        writer.newLine();
                        matchCount++;
                    }
                }

                System.out.println("[SCAN-" + command.getJobName() + "] Wrote " + matchCount +
                        " matching records from " + file.getName() + " to " + outputFile);

            } catch (IOException e) {
                System.out.println("Failed to process file " + file.getName() + ": " + e.getMessage());
            } finally {
                // Oslobodi lock za izlazni fajl
                outputLock.writeLock().unlock();
            }

            System.out.println("[SCAN-" + command.getJobName() + "] Processing file: " + file.getName() + " completed.");

        } finally {
            fileLock.readLock().unlock();
        }
    }
}
