package core;

import map.InMemoryMapManager;
import util.MapExporter;

public class PeriodicReporter extends Thread {
    private final InMemoryMapManager mapManager;
    private volatile boolean running = true;

    public PeriodicReporter(InMemoryMapManager mapManager) {
        this.mapManager = mapManager;
        setName("PeriodicReporter");
    }

    public void shutdown() {
        running = false;
        this.interrupt(); // prekini spavanje ako čeka
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(60_000); // 1 minut

                synchronized (MapExporter.getExportLock()) {
                    MapExporter.exportToCsv(mapManager.snapshot(), "map_log.csv");
                }

            } catch (InterruptedException e) {
                if (!running) break; // očekivano gašenje
                System.out.println("PeriodicReporter interrupted unexpectedly.");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("Error in periodic reporter: " + e.getMessage());
            }
        }

        System.out.println("PeriodicReporter stopped.");
    }
}
