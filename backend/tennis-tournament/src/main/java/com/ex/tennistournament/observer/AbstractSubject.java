package com.ex.tennistournament.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class implementing the Subject interface in the Observer pattern.
 *  * Manages observer registration and notification.
 */
public abstract class AbstractSubject implements Subject {
    private final List<Observer> observers = new ArrayList<>();

    @Override
    public void attach(Observer observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String message, Object data) {
        for (Observer observer : observers) {
            observer.update(message, data);
        }
    }
}