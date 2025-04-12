package com.ex.tennistournament.builder;

/**
 * The Builder interface defines methods that all concrete builders must implement.
 * It's used for constructing complex objects step by step.
 *
 * @param <T> The type of object being built
 */
public interface Builder<T> {
    T build();
}
