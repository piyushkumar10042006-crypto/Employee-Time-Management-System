package com.timemanagement.dao;

import com.timemanagement.exception.DatabaseOperationException;

import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object interface.
 * Demonstrates Java Generics <T, ID>, Collections (List<T>), and Interface Abstraction.
 */
public interface GenericDAO<T, ID> {

    T save(T entity) throws DatabaseOperationException;

    Optional<T> findById(ID id) throws DatabaseOperationException;

    List<T> findAll() throws DatabaseOperationException;

    boolean update(T entity) throws DatabaseOperationException;

    boolean delete(ID id) throws DatabaseOperationException;
}
