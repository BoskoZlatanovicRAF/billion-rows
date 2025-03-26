package cli.impl;

import cli.Command;

public class StartCommand implements Command {
    private final boolean loadJobs;

    public StartCommand(boolean loadJobs) {
        this.loadJobs = loadJobs;
    }

    public boolean shouldLoadJobs() {
        return loadJobs;
    }

    @Override
    public String getJobName() {
        return null; // not tied to job
    }
}
