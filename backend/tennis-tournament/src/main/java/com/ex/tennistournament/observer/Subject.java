package com.ex.tennistournament.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * The Subject interface defines methods for attaching, detaching, and notifying observers.
 * Any class that needs to be observed implements this interface.
 */
public interface Subject {
    void attach(Observer observer);
    void detach(Observer observer);
    void notifyObservers(String message, Object data);
}