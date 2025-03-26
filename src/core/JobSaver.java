package core;

import cli.impl.ScanCommand;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Future;

public class JobSaver {
    private static final String SAVE_FILE = "saved_jobs.txt";

    public static void savePendingJobs(
            Map<String, Future<?>> jobFutures,
            Map<String, ScanCommand> scanCommandMap) {

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(SAVE_FILE))) {
            for (Map.Entry<String, Future<?>> entry : jobFutures.entrySet()) {
                String jobName = entry.getKey();
                Future<?> future = entry.getValue();

                if (!future.isDone()) {
                    ScanCommand cmd = scanCommandMap.get(jobName);
                    if (cmd != null) {
                        writer.write(String.format(Locale.US, "%s;%f;%f;%c;%s",
                                cmd.getJobName(),
                                cmd.getMinTemp(),
                                cmd.getMaxTemp(),
                                cmd.getLetter(),
                                cmd.getOutputFile()));
                        writer.newLine();
                    }
                }
            }

            System.out.println("Pending jobs saved.");
        } catch (IOException e) {
            System.out.println("Failed to save jobs: " + e.getMessage());
        }
    }

    public static List<ScanCommand> loadPendingScanJobs() {
        List<ScanCommand> list = new ArrayList<>();

        Path path = Paths.get(SAVE_FILE);
        if (!Files.exists(path)) return list;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split(";");
                    if (parts.length != 5) continue;

                    String jobName = parts[0];
                    double min = Double.parseDouble(parts[1]);
                    double max = Double.parseDouble(parts[2]);
                    char letter = parts[3].charAt(0);
                    String outputFile = parts[4];

                    ScanCommand cmd = new ScanCommand(min, max, letter, outputFile, jobName);
                    list.add(cmd);
                } catch (Exception e) {
                    System.out.println("Invalid job entry: " + line);
                }
            }

            System.out.println("Loaded " + list.size() + " pending jobs.");
        } catch (IOException e) {
            System.out.println("Failed to load saved jobs: " + e.getMessage());
        }

        return list;
    }
}
