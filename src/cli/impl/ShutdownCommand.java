package cli.impl;

import cli.Command;

public class ShutdownCommand implements Command {
    private final boolean saveJobs;

    public ShutdownCommand(boolean saveJobs) {
        this.saveJobs = saveJobs;
    }

    public boolean shouldSaveJobs() {
        return saveJobs;
    }

    @Override
    public String getJobName() {
        return null; // not tied to job
    }
}
