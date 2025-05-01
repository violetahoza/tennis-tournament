package com.ex.tennistournament.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class implementing the Subject interface in the Observer pattern.
 * Manages observer registration and notification.
 */
public abstract class AbstractSubject implements Subject {
    /**
     * List of observers registered to this subject.
     */
    private final List<Observer> observers = new ArrayList<>();

    /**
     * Attaches an observer to the subject.
     * Ensures the observer is not already registered.
     *
     * @param observer the observer to attach
     */
    @Override
    public void attach(Observer observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Detaches an observer from the subject.
     * Removes the observer if it is registered.
     *
     * @param observer the observer to detach
     */
    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }

    /**
     * Notifies all registered observers with a message and optional data.
     * Calls the `update` method on each observer.
     *
     * @param message the message to send to observers
     * @param data    additional data to send to observers
     */
    @Override
    public void notifyObservers(String message, Object data) {
        for (Observer observer : observers) {
            observer.update(message, data);
        }
    }
}