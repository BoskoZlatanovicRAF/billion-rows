package core;

import cli.CLIThread;
import cli.Command;
import cli.impl.*;

import java.util.concurrent.BlockingQueue;

public class CommandDispatcher extends Thread {
    private final BlockingQueue<Command> commandQueue;
    private final JobManager jobManager;
    private final CLIThread cliThread;

    private volatile boolean running = true;

    public CommandDispatcher(BlockingQueue<Command> commandQueue, JobManager jobManager, CLIThread cliThread) {
        this.commandQueue = commandQueue;
        this.jobManager = jobManager;
        this.cliThread = cliThread;
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
                    System.out.println("Shutdown command received. Terminating system...");

                    jobManager.shutdown(shutdownCommand.shouldSaveJobs());
                    cliThread.shutdown(); // CLIThread will break the loop
                    this.running = false;

                } else if (command instanceof StartCommand) {
                    StartCommand startCommand = (StartCommand) command;
                    if (startCommand.shouldLoadJobs()) {
                        jobManager.loadPendingJobs();
                    }
                }

            } catch (InterruptedException e) {
                System.out.println("Command dispatcher interrupted.");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("Error while processing command: " + e.getMessage());
            }
        }

        System.out.println("Command dispatcher stopped.");
    }
}