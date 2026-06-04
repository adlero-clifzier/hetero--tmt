package com.hetero.repository;

import com.hetero.db.DatabaseManager;
import com.hetero.model.Priority;
import com.hetero.model.Task;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * HashMapTaskRepo stores tasks in a HashMap where the key is the task id.
 *
 * A HashMap lets us find, insert, and delete a task by id in O(1) average time
 * because it uses a hash function to jump directly to the right bucket
 * instead of scanning through every element.
 *
 * Every operation is timed with System.nanoTime() so we can benchmark
 * how fast the HashMap is compared to the other two data structures.
 *
 * After each in-memory operation the change is also written to SQLite
 * (write-through persistence) — but the timing only measures the
 * collection operation, not the database call.
 *
 * This class implements TaskRepository, which is our Strategy interface.
 * That means the rest of the app can swap this out for LinkedList or
 * ArrayList without changing any other code.
 */
public class HashMapTaskRepo implements TaskRepository {

    // The main in-memory store. Key = task id, Value = Task object.
    private final Map<Integer, Task> taskStore = new HashMap<>();

    // Reference to the database gateway for write-through persistence
    private final DatabaseManager databaseManager = DatabaseManager.getInstance();

    // When false, database calls are skipped (used by PerformanceTest)
    private boolean isDbEnabled = true;

    // When false, benchmark output to the console is suppressed
    private boolean isLoggingEnabled = true;

    // Used in test mode to assign fake ids instead of using the database
    private int mockIdCounter = 1;

    /**
     * Turns test mode on or off.
     * In test mode no database calls are made and no console output is printed.
     * This lets PerformanceTest measure pure collection speed.
     *
     * @param isTestMode true to enable test mode, false for normal use
     */
    public void setTestMode(boolean isTestMode) {
        this.isDbEnabled      = !isTestMode;
        this.isLoggingEnabled = !isTestMode;
    }

    /**
     * Prints a benchmark result to the console.
     * Format: [Benchmark] HashMap <operation>: <time> ns
     *
     * @param operationLabel name of what was measured
     * @param elapsedNanos   how long it took in nanoseconds
     */
    private void printBenchmark(String operationLabel, long elapsedNanos) {
        if (isLoggingEnabled) {
            System.out.printf("[Benchmark] HashMap %s: %,d ns%n", operationLabel, elapsedNanos);
        }
    }

    // Clears the map and refills it from the given list — O(n)
    @Override
    public void loadAll(List<Task> tasks) {
        long startTime = System.nanoTime();
        taskStore.clear();
        tasks.forEach(task -> taskStore.put(task.getId(), task));
        printBenchmark("LoadAll (" + tasks.size() + " tasks)", System.nanoTime() - startTime);
    }

    // Inserts one task into the map — O(1) average
    @Override
    public Task add(Task task) {
        if (isDbEnabled) {
            int generatedId = databaseManager.insert(task);
            task.setId(generatedId);
        } else {
            task.setId(mockIdCounter++);
        }

        long startTime = System.nanoTime();
        taskStore.put(task.getId(), task);
        printBenchmark("Insert", System.nanoTime() - startTime);

        return task;
    }

    // Replaces the existing entry in the map — O(1) average
    @Override
    public void update(Task task) {
        long startTime = System.nanoTime();
        taskStore.put(task.getId(), task);
        printBenchmark("Update", System.nanoTime() - startTime);

        if (isDbEnabled) {
            databaseManager.update(task);
        }
    }

    // Removes the entry by key — O(1) average
    @Override
    public void delete(int id) {
        long startTime = System.nanoTime();
        taskStore.remove(id);
        printBenchmark("Delete", System.nanoTime() - startTime);

        if (isDbEnabled) {
            databaseManager.delete(id);
        }
    }

    // Direct key lookup — O(1) average
    @Override
    public Optional<Task> findById(int id) {
        long startTime = System.nanoTime();
        Optional<Task> result = Optional.ofNullable(taskStore.get(id));
        printBenchmark("FindById", System.nanoTime() - startTime);
        return result;
    }

    // Returns a snapshot of all values — O(n)
    @Override
    public List<Task> findAll() {
        long startTime = System.nanoTime();
        List<Task> result = List.copyOf(taskStore.values());
        printBenchmark("FindAll (" + result.size() + " tasks)", System.nanoTime() - startTime);
        return result;
    }

    // Scans all values and keeps only those with matching completion flag — O(n)
    @Override
    public List<Task> findByCompleted(boolean completed) {
        long startTime = System.nanoTime();
        List<Task> result = taskStore.values().stream()
                .filter(task -> task.isCompleted() == completed)
                .collect(Collectors.toList());
        printBenchmark("FindByCompleted", System.nanoTime() - startTime);
        return result;
    }

    // Scans all values and keeps only those due today — O(n)
    @Override
    public List<Task> findDueToday() {
        long startTime  = System.nanoTime();
        LocalDate today = LocalDate.now();
        List<Task> result = taskStore.values().stream()
                .filter(task -> today.equals(task.getDueDate()))
                .collect(Collectors.toList());
        printBenchmark("FindDueToday", System.nanoTime() - startTime);
        return result;
    }

    // Scans all values with case-insensitive category match — O(n)
    @Override
    public List<Task> findByCategory(String category) {
        long startTime = System.nanoTime();
        List<Task> result = taskStore.values().stream()
                .filter(task -> category.equalsIgnoreCase(task.getCategory()))
                .collect(Collectors.toList());
        printBenchmark("FindByCategory", System.nanoTime() - startTime);
        return result;
    }

    // Scans all values for matching priority — O(n)
    @Override
    public List<Task> findByPriority(Priority priority) {
        long startTime = System.nanoTime();
        List<Task> result = taskStore.values().stream()
                .filter(task -> task.getPriority() == priority)
                .collect(Collectors.toList());
        printBenchmark("FindByPriority", System.nanoTime() - startTime);
        return result;
    }

    @Override
    public String getStrategyName() {
        return "HashMap";
    }
}
