package com.ex.tennistournament.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * The Subject interface defines methods for attaching, detaching, and notifying observers.
 * Any class that needs to be observed implements this interface.
 */
public interface Subject {
    /**
     * Attaches an observer to the subject.
     * Observers will be notified of changes in the subject's state.
     *
     * @param observer the observer to attach
     */
    void attach(Observer observer);

    /**
     * Detaches an observer from the subject.
     * The observer will no longer receive updates from the subject.
     *
     * @param observer the observer to detach
     */
    void detach(Observer observer);

    /**
     * Notifies all registered observers of a change in the subject's state.
     * Sends a message and optional data to each observer.
     *
     * @param message a descriptive message about the update
     * @param data    additional data related to the update
     */
    void notifyObservers(String message, Object data);
}