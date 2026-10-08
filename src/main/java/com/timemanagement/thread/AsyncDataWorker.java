package com.timemanagement.thread;

import javax.swing.SwingWorker;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reusable asynchronous SwingWorker executing database and calculation tasks
 * on a background worker thread so the Swing UI never freezes.
 * Demonstrates Multithreading in GUI applications.
 */
public class AsyncDataWorker<T> extends SwingWorker<T, Void> {

    private final Supplier<T> task;
    private final Consumer<T> onSuccess;
    private final Consumer<Exception> onFailure;

    public AsyncDataWorker(Supplier<T> task, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        this.task = task;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    @Override
    protected T doInBackground() throws Exception {
        // Runs on a background worker thread
        return task.get();
    }

    @Override
    protected void done() {
        // Runs on the Swing Event Dispatch Thread (EDT)
        try {
            T result = get();
            if (onSuccess != null) {
                onSuccess.accept(result);
            }
        } catch (Exception ex) {
            if (onFailure != null) {
                onFailure.accept(ex);
            }
        }
    }
}
