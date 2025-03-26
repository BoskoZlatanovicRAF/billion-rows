import cli.CLIThread;
import cli.Command;
import core.CommandDispatcher;
import core.FileUpdateProcessor;
import core.JobManager;
import core.PeriodicReporter;
import map.InMemoryMapManager;
import monitor.DirectoryMonitor;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    public static void main(String[] args) {
        // Centralna shared in-memory mapa
        InMemoryMapManager mapManager = new InMemoryMapManager();

        // Red komandi između CLI niti i CommandDispatcher-a
        BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();

        CLIThread cliThread = new CLIThread(commandQueue);
        PeriodicReporter periodicReporter = new PeriodicReporter(mapManager);

        // Job manager (obrada SCAN, STATUS, itd.)
        JobManager jobManager = new JobManager(mapManager);

        // Command dispatcher – veza između CLI i logike
        CommandDispatcher dispatcher = new CommandDispatcher(commandQueue, jobManager, cliThread);

        BlockingQueue<File> updateQueue = new LinkedBlockingQueue<>();
        DirectoryMonitor directoryMonitor = new DirectoryMonitor("test_data", updateQueue);
        FileUpdateProcessor updateProcessor = new FileUpdateProcessor(updateQueue, mapManager);

        directoryMonitor.start();
        updateProcessor.start();
        cliThread.start();
        dispatcher.start();
        periodicReporter.start();

        if (args.length > 0 && args[0].equalsIgnoreCase("--load-jobs")) {
            jobManager.loadPendingJobs();
        }

        System.out.println("System initialized.");
    }
}