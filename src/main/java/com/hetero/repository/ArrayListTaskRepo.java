package com.hetero.repository;

import com.hetero.db.DatabaseManager;
import com.hetero.model.Priority;
import com.hetero.model.Task;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ArrayListTaskRepo stores tasks in an ArrayList.
 
 * An ArrayList stores elements in a contiguous block of memory (like an array)
 * but resizes itself automatically when it gets full. This means:
 *   - Appending to the end is O(1) on average (sometimes O(n) when it resizes)
 *   - Finding by id requires a linear scan — O(n)
 *   - Deleting requires a scan plus shifting all elements after the gap — O(n)
 *
 * The delete cost is slightly worse than LinkedList because of that shifting,
 * which the benchmark is designed to highlight.
 *
 * This class implements TaskRepository (our Strategy interface).
 */
public class ArrayListTaskRepo implements TaskRepository {

    // The main in-memory store — a standard Java ArrayList
    private final ArrayList<Task> taskStore = new ArrayList<>();

    // Reference to the database gateway for write-through persistence
    private final DatabaseManager databaseManager = DatabaseManager.getInstance();

    // When false, database calls are skipped (used by PerformanceTest)
    private boolean isDbEnabled = true;

    // When false, benchmark console output is suppressed
    private boolean isLoggingEnabled = true;

    // Fake id counter used in test mode instead of the database
    private int mockIdCounter = 1;

    /**
     * Turns test mode on or off.
     * In test mode no database calls are made and no output is printed.
     *
     * @param isTestMode true to enable test mode
     */
    public void setTestMode(boolean isTestMode) {
        this.isDbEnabled      = !isTestMode;
        this.isLoggingEnabled = !isTestMode;
    }

    /**
     * Prints a benchmark line to the console.
     *
     * @param operationLabel what was measured
     * @param elapsedNanos   duration in nanoseconds
     */
    private void printBenchmark(String operationLabel, long elapsedNanos) {
        if (isLoggingEnabled) {
            System.out.printf("[Benchmark] ArrayList %s: %,d ns%n", operationLabel, elapsedNanos);
        }
    }

    // Clears the list and refills it from the given list — O(n)
    @Override
    public void loadAll(List<Task> tasks) {
        long startTime = System.nanoTime();
        taskStore.clear();
        taskStore.addAll(tasks);
        printBenchmark("LoadAll (" + tasks.size() + " tasks)", System.nanoTime() - startTime);
    }

    // Appends to the end of the ArrayList — O(1) amortised
    @Override
    public Task add(Task task) {
        if (isDbEnabled) {
            int generatedId = databaseManager.insert(task);
            task.setId(generatedId);
        } else {
            task.setId(mockIdCounter++);
        }

        long startTime = System.nanoTime();
        taskStore.add(task);
        printBenchmark("Insert", System.nanoTime() - startTime);

        return task;
    }

    // Scans for the index, then replaces at that position — O(n) scan + O(1) set
    @Override
    public void update(Task updatedTask) {
        long startTime = System.nanoTime();

        for (int currentIndex = 0; currentIndex < taskStore.size(); currentIndex++) {
            if (taskStore.get(currentIndex).getId() == updatedTask.getId()) {
                taskStore.set(currentIndex, updatedTask);
                break;
            }
        }

        printBenchmark("Update", System.nanoTime() - startTime);

        if (isDbEnabled) {
            databaseManager.update(updatedTask);
        }
    }

    // Scans and removes matching task, then shifts remaining elements — O(n)
    @Override
    public void delete(int id) {
        long startTime = System.nanoTime();
        taskStore.removeIf(task -> task.getId() == id);
        printBenchmark("Delete", System.nanoTime() - startTime);

        if (isDbEnabled) {
            databaseManager.delete(id);
        }
    }

    // Linear scan to find by id — O(n)
    @Override
    public Optional<Task> findById(int id) {
        long startTime = System.nanoTime();
        Optional<Task> result = taskStore.stream()
                .filter(task -> task.getId() == id)
                .findFirst();
        printBenchmark("FindById", System.nanoTime() - startTime);
        return result;
    }

    // Returns an unmodifiable copy of the whole list — O(n)
    @Override
    public List<Task> findAll() {
        long startTime = System.nanoTime();
        List<Task> snapshot = List.copyOf(taskStore);
        printBenchmark("FindAll (" + snapshot.size() + " tasks)", System.nanoTime() - startTime);
        return snapshot;
    }

    // Linear scan filtered by completion flag — O(n)
    @Override
    public List<Task> findByCompleted(boolean completed) {
        long startTime = System.nanoTime();
        List<Task> result = taskStore.stream()
                .filter(task -> task.isCompleted() == completed)
                .collect(Collectors.toList());
        printBenchmark("FindByCompleted", System.nanoTime() - startTime);
        return result;
    }

    // Linear scan filtered by today's date — O(n)
    @Override
    public List<Task> findDueToday() {
        long startTime  = System.nanoTime();
        LocalDate today = LocalDate.now();
        List<Task> result = taskStore.stream()
                .filter(task -> today.equals(task.getDueDate()))
                .collect(Collectors.toList());
        printBenchmark("FindDueToday", System.nanoTime() - startTime);
        return result;
    }

    // Linear scan with case-insensitive category match — O(n)
    @Override
    public List<Task> findByCategory(String category) {
        long startTime = System.nanoTime();
        List<Task> result = taskStore.stream()
                .filter(task -> category.equalsIgnoreCase(task.getCategory()))
                .collect(Collectors.toList());
        printBenchmark("FindByCategory", System.nanoTime() - startTime);
        return result;
    }

    // Linear scan filtered by priority — O(n)
    @Override
    public List<Task> findByPriority(Priority priority) {
        long startTime = System.nanoTime();
        List<Task> result = taskStore.stream()
                .filter(task -> task.getPriority() == priority)
                .collect(Collectors.toList());
        printBenchmark("FindByPriority", System.nanoTime() - startTime);
        return result;
    }

    @Override
    public String getStrategyName() {
        return "ArrayList";
    }
}
