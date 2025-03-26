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

        // Inicijalizacija komponenti sistema (bez startovanja)
        CLIThread cliThread = new CLIThread(commandQueue);
        PeriodicReporter periodicReporter = new PeriodicReporter(mapManager);
        JobManager jobManager = new JobManager(mapManager);
        CommandDispatcher dispatcher = new CommandDispatcher(commandQueue, jobManager, cliThread, periodicReporter);

        BlockingQueue<File> updateQueue = new LinkedBlockingQueue<>();
        DirectoryMonitor directoryMonitor = new DirectoryMonitor("test_data", updateQueue, jobManager);
        FileUpdateProcessor updateProcessor = new FileUpdateProcessor(updateQueue, mapManager);

        // Dodaj komponente dispatcheru da može upravljati njihovim životnim ciklusom
        dispatcher.setComponents(directoryMonitor, updateProcessor, periodicReporter);

        // Samo pokrećemo CLI nit i dispatcher da bi korisnik mogao da unosi komande
        cliThread.start();
        dispatcher.start();

        System.out.println("System initialized. Enter START to begin processing.");
    }
}