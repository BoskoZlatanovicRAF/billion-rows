package cli.impl;

import cli.Command;

public class StatusCommand implements Command {
    private final String jobName;

    public StatusCommand(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public String getJobName() {
        return jobName;
    }
}
