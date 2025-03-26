package core;

import cli.CLIThread;
import cli.Command;
import cli.impl.*;
import monitor.DirectoryMonitor;

import java.util.concurrent.BlockingQueue;

public class CommandDispatcher extends Thread {
    private final BlockingQueue<Command> commandQueue;
    private final JobManager jobManager;
    private final CLIThread cliThread;
    private PeriodicReporter periodicReporter;
    private DirectoryMonitor directoryMonitor;
    private FileUpdateProcessor fileUpdateProcessor;

    private volatile boolean running = true;
    private volatile boolean systemStarted = false;

    private static final Command POISON_PILL = new PoisonPillCommand();

    public CommandDispatcher(BlockingQueue<Command> commandQueue, JobManager jobManager, CLIThread cliThread, PeriodicReporter periodicReporter) {
        this.commandQueue = commandQueue;
        this.jobManager = jobManager;
        this.cliThread = cliThread;
        this.periodicReporter = periodicReporter;
    }

    public void setComponents(DirectoryMonitor directoryMonitor, FileUpdateProcessor fileUpdateProcessor, PeriodicReporter periodicReporter) {
        this.directoryMonitor = directoryMonitor;
        this.fileUpdateProcessor = fileUpdateProcessor;
        this.periodicReporter = periodicReporter;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Command command = commandQueue.take();

                if (command instanceof ScanCommand) {
                    jobManager.handleScan((ScanCommand) command);

                } else if (command instanceof StatusCommand) {
                    jobManager.handleStatus((StatusCommand) command);

                } else if (command instanceof MapCommand) {
                    jobManager.handleMap();

                } else if (command instanceof ExportMapCommand) {
                    jobManager.handleExportMap();

                } else if (command instanceof ShutdownCommand) {
                    ShutdownCommand shutdownCommand = (ShutdownCommand) command;
                    shutdownSystem(shutdownCommand.shouldSaveJobs());

                } else if (command instanceof StartCommand) {
                    StartCommand startCommand = (StartCommand) command;
                    startSystem(startCommand.shouldLoadJobs());

                } else if (command == POISON_PILL) {
                    break;
                }

            } catch (InterruptedException e) {
                System.out.println("Command dispatcher interrupted.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.out.println("Error while processing command: " + e.getMessage());
            }
        }

        System.out.println("Command dispatcher stopped.");
    }


    private void startSystem(boolean loadJobs) {
        if (systemStarted) {
            System.out.println("System is already running.");
            return;
        }

        System.out.println("Starting system...");

        if (loadJobs) {
            jobManager.loadPendingJobs();
        }

        // Eksplicitno naglasi pokretanje svake komponente
        System.out.println("Starting directory monitor...");
        directoryMonitor.start();

        System.out.println("Starting file update processor...");
        fileUpdateProcessor.start();

        System.out.println("Starting periodic reporter...");
        periodicReporter.start();



        systemStarted = true;
        System.out.println("System started successfully.");
    }


    private void shutdownSystem(boolean saveJobs) {
        if (!systemStarted) {
            System.out.println("System is not running.");
            return;
        }

        System.out.println("[CLI-THREAD]: Shutting down the system...");

        if (saveJobs) {
            jobManager.savePendingJobs();
        }

        // Prekini sve niti
        directoryMonitor.shutdown();
        fileUpdateProcessor.shutdown();
        periodicReporter.shutdown();

        // Zaustavi JobManager i njegov executor service
        jobManager.shutdown(saveJobs);


        systemStarted = false;
        // Dodaj poison pill u red komandi
        try {
            commandQueue.put(POISON_PILL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Prekini CLI nit
        cliThread.shutdown();

        // Završi trenutnu nit
        this.running = false;

        System.out.println("System shutdown completed.");
    }

}