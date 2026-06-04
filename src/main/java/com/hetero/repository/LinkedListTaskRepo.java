package com.hetero.repository;

import com.hetero.db.DatabaseManager;
import com.hetero.model.Priority;
import com.hetero.model.Task;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * LinkedListTaskRepo stores tasks in a LinkedList.
 *
 * A LinkedList is made of nodes where each node holds a task and a
 * pointer to the next node. This means:
 *   - Adding to the end (tail) is O(1) — we just update one pointer
 *   - Finding or deleting by id is O(n) — we must scan every node
 *     because there is no direct address like in a HashMap
 *
 * This makes LinkedList noticeably slower than HashMap for lookups,
 * which is exactly the trade-off the benchmark is designed to show.
 *
 * For updates we use a ListIterator, which allows us to replace a
 * node in-place without removing and re-inserting it.
 *
 * This class implements TaskRepository (our Strategy interface).
 */
public class LinkedListTaskRepo implements TaskRepository {

    // The main in-memory store — a standard Java LinkedList
    private final LinkedList<Task> taskStore = new LinkedList<>();

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
            System.out.printf("[Benchmark] LinkedList %s: %,d ns%n", operationLabel, elapsedNanos);
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

    // Appends the task to the end of the list — O(1)
    @Override
    public Task add(Task task) {
        if (isDbEnabled) {
            int generatedId = databaseManager.insert(task);
            task.setId(generatedId);
        } else {
            task.setId(mockIdCounter++);
        }

        long startTime = System.nanoTime();
        taskStore.addLast(task);
        printBenchmark("Insert", System.nanoTime() - startTime);

        return task;
    }

    // Scans with a ListIterator to find and replace the node in-place — O(n)
    @Override
    public void update(Task task) {
        long startTime = System.nanoTime();

        ListIterator<Task> iterator = taskStore.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next().getId() == task.getId()) {
                iterator.set(task); // Replace the current node without re-inserting
                break;
            }
        }

        printBenchmark("Update", System.nanoTime() - startTime);

        if (isDbEnabled) {
            databaseManager.update(task);
        }
    }

    // Scans the list and removes the matching node — O(n)
    @Override
    public void delete(int id) {
        long startTime = System.nanoTime();
        taskStore.removeIf(task -> task.getId() == id);
        printBenchmark("Delete", System.nanoTime() - startTime);

        if (isDbEnabled) {
            databaseManager.delete(id);
        }
    }

    // Sequential scan to find by id — O(n)
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
        List<Task> result = List.copyOf(taskStore);
        printBenchmark("FindAll (" + result.size() + " tasks)", System.nanoTime() - startTime);
        return result;
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
        return "LinkedList";
    }
}
