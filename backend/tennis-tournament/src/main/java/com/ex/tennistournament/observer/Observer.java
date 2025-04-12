package com.ex.tennistournament.observer;

/**
 * The Observer interface defines the update method that all concrete observers must implement.
 * This will be called when a subject (observable) changes state.
 */
public interface Observer {
    void update(String message, Object data);
}