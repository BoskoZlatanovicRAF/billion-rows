package cli;

import cli.impl.ShutdownCommand;
import cli.impl.StartCommand;

import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

public class CLIThread extends Thread {

    private volatile boolean running = true;
    private final BlockingQueue<Command> commandQueue;

    public CLIThread(BlockingQueue<Command> commandQueue) {
        this.commandQueue = commandQueue;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("CLI ready. Enter command:");

        while (running) {
            String line = scanner.nextLine();
            Optional<Command> command = CommandParser.parse(line);
            command.ifPresent(cmd -> {
                if (!(cmd instanceof StartCommand || cmd instanceof ShutdownCommand)) {
                    try {
                        commandQueue.put(cmd);
                    } catch (InterruptedException e) {
                        System.out.println("Aborted waiting for command line.");
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
    }

    public void shutdown() {
        running = false;
    }
}