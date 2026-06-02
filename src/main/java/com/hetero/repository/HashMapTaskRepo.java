package com.hetero.repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hetero.db.DatabaseManager;
import com.hetero.model.Priority;
import com.hetero.model.Task;

/**
 * {@link TaskRepository} backed by a {@link HashMap} keyed on task id.
 *
 * <p>Provides O(1) average-case lookup, insert, and delete.
 * Every mutating operation is wrapped with {@code System.nanoTime()} to emit
 * benchmark output, then a write-through sync is dispatched to SQLite.
 */
public class HashMapTaskRepo implements TaskRepository {

    /** The primary in-memory store: id → Task. */
    private final Map<Integer, Task> store = new HashMap<>();
    private final DatabaseManager db = DatabaseManager.getInstance();

    // --- BENCHMARK CONTROL TOGGLES ---
    private boolean dbEnabled = true;
    private boolean loggingEnabled = true;
    private int mockIdCounter = 1;

    /**
     * Toggles persistence and internal console logging.
     * Set to true during bulk benchmarks to isolate pure in-memory execution speeds.
     */
    public void setTestMode(boolean isTestMode) {
        this.dbEnabled = !isTestMode;
        this.loggingEnabled = !isTestMode;
    }

    // ── Benchmark helper ──────────────────────────────────────────────────────

    /**
     * Prints a formatted benchmark line to stdout.
     *
     * @param operation human-readable operation label
     * @param nanos     elapsed nanoseconds
     */
    private void bench(String operation, long nanos) {
        if (loggingEnabled) {
            System.out.printf("[Benchmark] HashMap %s: %,d ns%n", operation, nanos);
        }
    }

    // ── TaskRepository impl ───────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     * Clears the existing store and populates it from the provided list.
     * O(n) where n = list size.
     */
    @Override
    public void loadAll(List<Task> tasks) {
        long t0 = System.nanoTime();
        store.clear();
        tasks.forEach(task -> store.put(task.getId(), task));
        bench("LoadAll (" + tasks.size() + " tasks)", System.nanoTime() - t0);
    }

    /**
     * {@inheritDoc}
     * Inserts into the HashMap in O(1) average, then syncs to SQLite.
     */
    @Override
    public Task add(Task task) {
        if (dbEnabled) {
            // Write-through: get DB-assigned id first
            int generatedId = db.insert(task);
            task.setId(generatedId);
        } else {
            // Generates sequentially safe IDs for search/update validation in RAM
            task.setId(mockIdCounter++);
        }

        long t0 = System.nanoTime();
        store.put(task.getId(), task);
        bench("Insert", System.nanoTime() - t0);

        return task;
    }

    /**
     * {@inheritDoc}
     * Replaces the entry in O(1) average, then syncs to SQLite.
     */
    @Override
    public void update(Task task) {
        long t0 = System.nanoTime();
        store.put(task.getId(), task);
        bench("Update", System.nanoTime() - t0);

        if (dbEnabled) {
            db.update(task);
        }
    }

    /**
     * {@inheritDoc}
     * Removes the entry in O(1) average, then syncs to SQLite.
     */
    @Override
    public void delete(int id) {
        long t0 = System.nanoTime();
        store.remove(id);
        bench("Delete", System.nanoTime() - t0);

        if (dbEnabled) {
            db.delete(id);
        }
    }

    /**
     * {@inheritDoc}
     * Direct key lookup — O(1) average.
     */
    @Override
    public Optional<Task> findById(int id) {
        long t0 = System.nanoTime();
        Optional<Task> result = Optional.ofNullable(store.get(id));
        bench("FindById", System.nanoTime() - t0);
        return result;
    }

    /**
     * {@inheritDoc}
     * Returns a snapshot of all values — O(n).
     */
    @Override
    public List<Task> findAll() {
        long t0 = System.nanoTime();
        List<Task> result = List.copyOf(store.values());
        bench("FindAll (" + result.size() + " tasks)", System.nanoTime() - t0);
        return result;
    }

    /**
     * {@inheritDoc}
     * Linear scan of values — O(n).
     */
    @Override
    public List<Task> findByCompleted(boolean completed) {
        long t0 = System.nanoTime();
        List<Task> result = store.values().stream()
                .filter(t -> t.isCompleted() == completed)
                .collect(Collectors.toList());
        bench("FindByCompleted", System.nanoTime() - t0);
        return result;
    }

    /**
     * {@inheritDoc}
     * Linear scan filtered by today's date — O(n).
     */
    @Override
    public List<Task> findDueToday() {
        long t0 = System.nanoTime();
        LocalDate today = LocalDate.now();
        List<Task> result = store.values().stream()
                .filter(t -> today.equals(t.getDueDate()))
                .collect(Collectors.toList());
        bench("FindDueToday", System.nanoTime() - t0);
        return result;
    }

    /**
     * {@inheritDoc}
     * Linear scan filtered by category — O(n).
     */
    @Override
    public List<Task> findByCategory(String category) {
        long t0 = System.nanoTime();
        List<Task> result = store.values().stream()
                .filter(t -> category.equalsIgnoreCase(t.getCategory()))
                .collect(Collectors.toList());
        bench("FindByCategory", System.nanoTime() - t0);
        return result;
    }

    /**
     * {@inheritDoc}
     * Linear scan filtered by priority — O(n).
     */
    @Override
    public List<Task> findByPriority(Priority priority) {
        long t0 = System.nanoTime();
        List<Task> result = store.values().stream()
                .filter(t -> t.getPriority() == priority)
                .collect(Collectors.toList());
        bench("FindByPriority", System.nanoTime() - t0);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String getStrategyName() { return "HashMap"; }
}