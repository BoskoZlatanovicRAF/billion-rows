package cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

import java.util.concurrent.BlockingQueue;

public class CLIThread extends Thread {

    private volatile boolean running = true;
    private final BlockingQueue<Command> commandQueue;
    private BufferedReader reader;

    public CLIThread(BlockingQueue<Command> commandQueue) {
        this.commandQueue = commandQueue;
    }

    @Override
    public void run() {
        reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("CLI ready. Enter command:");

        while (running) {
            try {
                if (!reader.ready()) {
                    Thread.sleep(100); // mali delay da CPU ne poludi
                    continue;
                }

                String line = reader.readLine();
                if (line == null) continue;

                Optional<Command> command = CommandParser.parse(line);
                command.ifPresent(cmd -> {
                    try {
                        commandQueue.put(cmd);
                    } catch (InterruptedException e) {
                        System.out.println("Aborted waiting for command line.");
                        Thread.currentThread().interrupt();
                    }
                });

            } catch (IOException e) {
                if (!running) break;
                System.out.println("CLI error: " + e.getMessage());
            } catch (InterruptedException e) {
                if (!running) break;
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("[CLI-THREAD]: CLI thread stopped.");
    }

    public void shutdown() {
        running = false;
        System.out.println("[CLI-THREAD]: CLI thread stopping...");
        try {
            if (reader != null) reader.close(); // ovo ce "probuditi" readLine()
        } catch (IOException e) {
            System.out.println("Error closing reader: " + e.getMessage());
        }
        this.interrupt(); // prekid ako je usput spavao
    }
}
