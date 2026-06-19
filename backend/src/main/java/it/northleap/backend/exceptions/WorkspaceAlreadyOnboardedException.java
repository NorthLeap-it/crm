package it.northleap.backend.exceptions;

public class WorkspaceAlreadyOnboardedException extends RuntimeException {
    public WorkspaceAlreadyOnboardedException() {
        super("Workspace already configured");
    }
}
