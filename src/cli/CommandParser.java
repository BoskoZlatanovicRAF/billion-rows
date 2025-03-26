package cli;

import cli.impl.*;

import java.util.*;

public class CommandParser {
    public static Optional<Command> parse(String input) {
        try {
            String[] tokens = input.trim().split("\\s+");
            String cmd = tokens[0].toUpperCase();

            Map<String, String> args = new HashMap<>();
            for (int i = 1; i < tokens.length; i++) {
                if (tokens[i].startsWith("-")) {
                    String key = tokens[i];
                    if (i + 1 < tokens.length && (tokens[i + 1].matches("-?\\d+(\\.\\d+)?") || !tokens[i + 1].startsWith("-"))) {
                        args.put(key, tokens[i + 1]);
                        i++;
                    } else {
                        args.put(key, "true"); // flag bez vrednosti
                    }
                }
            }

            switch (cmd) {
                case "SCAN":
                    String minStr = args.getOrDefault("-m", args.get("--min"));
                    String maxStr = args.getOrDefault("-M", args.get("--max"));
                    String letterStr = args.getOrDefault("-l", args.get("--letter"));
                    String output = args.getOrDefault("-o", args.get("--output"));
                    String job = args.getOrDefault("-j", args.get("--job"));

                    if (minStr == null || maxStr == null || letterStr == null || output == null || job == null) {
                        System.out.println("Invalid SCAN command: missing arguments.");
                        return Optional.empty();
                    }

                    double min = Double.parseDouble(minStr);
                    double max = Double.parseDouble(maxStr);
                    char letter = letterStr.charAt(0);

                    return Optional.of(new ScanCommand(min, max, letter, output, job));
                case "STATUS":
                    String jobName = args.getOrDefault("-j", args.get("--job"));
                    if (jobName == null) {
                        System.out.println("Invalid STATUS command: missing job name.");
                        return Optional.empty();
                    }
                    return Optional.of(new StatusCommand(jobName));
                case "MAP":
                    return Optional.of(new MapCommand());
                case "EXPORTMAP":
                    return Optional.of(new ExportMapCommand());
                case "SHUTDOWN":
                    boolean save = args.containsKey("-s") || args.containsKey("--save-jobs");
                    return Optional.of(new ShutdownCommand(save));
                case "START":
                    boolean load = args.containsKey("-l") || args.containsKey("--load-jobs");
                    return Optional.of(new StartCommand(load));
                default:
                    System.out.println("Unknown command: " + cmd);
            }
        } catch (Exception e) {
            System.out.println("Invalid command: " + e.getMessage());
        }
        return Optional.empty();
    }
}