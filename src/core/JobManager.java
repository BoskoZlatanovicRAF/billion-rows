package core;

import cli.*;
import cli.impl.ScanCommand;
import cli.impl.StatusCommand;
import map.InMemoryMapManager;
import util.MapExporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class JobManager {
    private final ExecutorService executor = Executors.newFixedThreadPool(4); // za SCAN
    private final ExecutorService lightExecutor = Executors.newSingleThreadExecutor(); // EXPORTMAP i MAP
    /** STATUS ce i dalje raditi na niti za obradu komandu */

    private final InMemoryMapManager mapManager;
    private final Map<String, Future<?>> jobFutures = new ConcurrentHashMap<>();
    private final Map<String, String> jobStatus = new ConcurrentHashMap<>();

    private final Map<String, ScanCommand> scanCommandRegistry = new ConcurrentHashMap<>();


    public JobManager(InMemoryMapManager mapManager) {
        this.mapManager = mapManager;
    }

    public void handleScan(ScanCommand command) {
        String jobName = command.getJobName();

        if (jobFutures.containsKey(jobName)) {
            System.out.println("Job with name '" + jobName + "' already exists.");
            return;
        }

        for (ScanCommand existing : scanCommandRegistry.values()) {
            if (existing.getOutputFile().equalsIgnoreCase(command.getOutputFile())) {
                System.out.println("Output file '" + command.getOutputFile() + "' is already in use by job '" + existing.getJobName() + "'.");
                return;
            }
        }

        File[] files = new File("test_data").listFiles((f, name) ->
                name.endsWith(".txt") || name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            System.out.println("No valid files found.");
            return;
        }

        jobStatus.put(jobName, "running");
        scanCommandRegistry.put(jobName, command);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (File file : files) {
            Callable<Void> task = () -> {
                ScanJobProcessor.processFile(file, command, command.getOutputFile());
                return null;
            };
            tasks.add(task);
        }

        Future<?> future = executor.submit(() -> {
            try {
                List<Future<Void>> futures = new ArrayList<>();
                for (Callable<Void> task : tasks) {
                    futures.add(executor.submit(task)); // dodaj task u executor
                }

                for (Future<Void> f : futures) {
                    try {
                        f.get();
                    } catch (ExecutionException e) {
                        System.out.println("Error in SCAN task: ");
                        e.getCause().printStackTrace();
                    }
                }

                jobStatus.put(jobName, "completed");
                System.out.println("Job '" + jobName + "' completed.");
            } catch (InterruptedException e) {
                jobStatus.put(jobName, "interrupted");
                Thread.currentThread().interrupt();
            }
        });

        jobFutures.put(jobName, future);
    }

    public void handleStatus(StatusCommand command) {
        String job = command.getJobName();
        String status = jobStatus.getOrDefault(job, "unknown");
        System.out.println(job + " is " + status);
    }

    public void handleMap() {
        lightExecutor.submit(() -> {
            Map<Character, StationData> snapshot = mapManager.snapshot(); // u sustini snapshot metoda vraca trenutno stanje mape
            if (snapshot.isEmpty()) {
                System.out.println("Map is currently unavailable.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            List<Character> keys = snapshot.keySet().stream()
                    .filter(c -> Character.toString(c).toLowerCase().matches("[a-z]"))
                    .sorted()
                    .collect(Collectors.toList());
            for (int i = 0; i < keys.size(); i += 2) {
                Character k1 = keys.get(i);
                Character k2 = (i + 1 < keys.size()) ? keys.get(i + 1) : null;

                StationData d1 = snapshot.get(k1);
                StationData d2 = (k2 != null) ? snapshot.get(k2) : null;

                sb.append(String.format("%c: %d - %.1f", k1, d1.getCount(), d1.getSum()));
                if (d2 != null)
                    sb.append(" | ").append(String.format("%c: %d - %.1f", k2, d2.getCount(), d2.getSum()));
                sb.append("\n");
            }

            System.out.print(sb);
        });
    }

    public void handleExportMap() {
        lightExecutor.submit(() -> {
            try {
                MapExporter.exportToCsv(mapManager.snapshot(), "map_log.csv");
            } catch (Exception e) {
                System.out.println("Failed to export map: " + e.getMessage());
            }
        });
    }

    public void savePendingJobs() {
        JobSaver.savePendingJobs(jobFutures, scanCommandRegistry);
    }

    public void shutdown(boolean saveJobs) {
        if (saveJobs) {
            savePendingJobs();
        }

        executor.shutdownNow();
        lightExecutor.shutdown();

        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("[JOB-MANAGER]: Forced shutdown of executor, some tasks may not complete.");
                executor.shutdownNow();
            }
            if (!lightExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("[JOB-MANAGER]: Forced shutdown of light executor.");
                lightExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            lightExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("[JOB-MANAGER]: All executors shut down.");
    }

    public void loadPendingJobs() {
        List<ScanCommand> commands = JobSaver.loadPendingScanJobs();
        for (ScanCommand cmd : commands) {
            handleScan(cmd);
        }
    }

    public void onDirectoryChanged() {
        // Resetuj in-memory mapu
        mapManager.reset();

        // Sačuvaj trenutne poslove
        List<ScanCommand> activeScans = new ArrayList<>();
        for (Map.Entry<String, ScanCommand> entry : scanCommandRegistry.entrySet()) {
            activeScans.add(entry.getValue());
        }

        // Otkaži sve trenutne poslove
        for (Map.Entry<String, Future<?>> entry : jobFutures.entrySet()) {
            entry.getValue().cancel(true);
        }

        // Čišćenje postojećih struktura
        jobFutures.clear();
        jobStatus.clear();
        scanCommandRegistry.clear(); // Važno - očisti registar

        // Resetuj izlazne fajlove za SCAN poslove
        for (ScanCommand cmd : activeScans) {
            try {
                // Obriši sadržaj output fajla
                new FileWriter(cmd.getOutputFile(), false).close();
            } catch (IOException e) {
                System.out.println("Failed to reset output file: " + cmd.getOutputFile());
            }

            // Ponovo pokreni SCAN posao
            handleScan(cmd);
        }

        System.out.println("Directory changed, reset in-memory map and restarted " + activeScans.size() + " active SCAN jobs");
    }
}
