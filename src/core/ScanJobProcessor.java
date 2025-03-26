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


        boolean isCsv = file.getName().endsWith(".csv");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            boolean skipFirst = isCsv;

            StringBuilder matchingLines = new StringBuilder();

            while ((line = reader.readLine()) != null) {
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
                    matchingLines.append(line).append(System.lineSeparator());
                }
            }

            if (matchingLines.length() > 0) {
                ReentrantReadWriteLock lock = outputLocks.computeIfAbsent(outputFile, key -> new ReentrantReadWriteLock());
                lock.writeLock().lock();
                try {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
                        writer.write(matchingLines.toString());
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }


        } catch (IOException e) {
            System.out.println("Failed to process file " + file.getName() + ": " + e.getMessage());
        }
    }
}
