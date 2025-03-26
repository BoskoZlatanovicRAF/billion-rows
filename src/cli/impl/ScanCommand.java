package cli.impl;

import cli.Command;

public class ScanCommand implements Command {
    private final double minTemp;
    private final double maxTemp;
    private final char letter;
    private final String outputFile;
    private final String jobName;

    public ScanCommand(double minTemp, double maxTemp, char letter, String outputFile, String jobName) {
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.letter = letter;
        this.outputFile = outputFile;
        this.jobName = jobName;
    }

    public double getMinTemp() { return minTemp; }
    public double getMaxTemp() { return maxTemp; }
    public char getLetter() { return letter; }
    public String getOutputFile() { return outputFile; }

    @Override
    public String getJobName() {
        return jobName;
    }
}