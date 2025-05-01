package com.ex.tennistournament.observer;

/**
 * The Observer interface defines the update method that all concrete observers must implement.
 * This will be called when a subject (observable) changes state.
 */
public interface Observer {
    /**
     * Called when the subject's state changes.
     * Implementing classes should define how they handle the update.
     *
     * @param message a descriptive message about the update
     * @param data    additional data related to the update
     */
    void update(String message, Object data);
}