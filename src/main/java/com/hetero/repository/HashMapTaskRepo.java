package com.hetero.repository;

import java.time.LocalDate;       // imported class — date comparison for findDueToday
import java.util.HashMap;         // java.util collection — backing store
import java.util.List;            // java.util collection interface
import java.util.Map;             // java.util collection interface
import java.util.Optional;        // nullable return wrapper
import java.util.stream.Collectors; // Stream terminal operation

import com.hetero.db.DatabaseManager; // custom-built singleton gateway
import com.hetero.model.Priority;     // custom-built enum
import com.hetero.model.Task;         // custom-built domain entity

/**
 * {@link TaskRepository} implementation backed by a {@link HashMap} keyed on task id.
 *
 * <p><b>Algorithmic complexity:</b>
 * <ul>
 *   <li>Insert   — O(1) average (hash table bucket placement).</li>
 *   <li>Lookup   — O(1) average (direct key hash).</li>
 *   <li>Delete   — O(1) average (direct key removal).</li>
 *   <li>Scan ops — O(n) (values() iteration required for filtering).</li>
 * </ul>
 *
 * <p>Every mutating operation records execution time with {@link System#nanoTime()}
 * before and after the collection call, printing a benchmark line to stdout.
 * The SQLite write-through sync happens <em>after</em> the timed block so it
 * never inflates the in-memory measurement.
 *
 * <p><b>Specification compliance:</b>
 * <ul>
 *   <li><b>Inheritance / polymorphism / interface:</b> {@code implements TaskRepository}.</li>
 *   <li><b>Java Collection:</b> {@link HashMap}, {@link List} used throughout.</li>
 *   <li><b>Imported classes:</b> {@link LocalDate}, {@link HashMap}, {@link Optional},
 *       {@link DatabaseManager}, {@link Task}, {@link Priority}.</li>
 *   <li><b>Custom-built classes:</b> {@link Task}, {@link Priority}, {@link DatabaseManager}.</li>
 *   <li><b>Instance variables:</b> {@code store}, {@code db}, {@code dbEnabled},
 *       {@code loggingEnabled}, {@code mockIdCounter}.</li>
 *   <li><b>Primitive data:</b> {@code dbEnabled} (boolean), {@code loggingEnabled} (boolean),
 *       {@code mockIdCounter} (int), timing delta (long).</li>
 *   <li><b>Exception handling:</b> Delegated to {@link DatabaseManager}; strategy
 *       layer itself stays free of checked-exception clutter.</li>
 * </ul>
 */
public class HashMapTaskRepo implements TaskRepository {

    // ── Instance variables ────────────────────────────────────────────────────

    /**
     * The primary in-memory data store.
     * Key = task id (Integer), Value = Task object.
     * HashMap chosen for O(1) average-case insert / lookup / delete.
     */
    private final Map<Integer, Task> taskStore = new HashMap<>();

    /** Reference to the singleton database gateway used for write-through persistence. */
    private final DatabaseManager databaseManager = DatabaseManager.getInstance();

    /**
     * When {@code false}, all database calls are bypassed.
     * Useful in test/benchmark mode to isolate pure collection performance.
     */
    private boolean dbEnabled = true;

    /**
     * When {@code false}, benchmark output is suppressed.
     * Useful when running bulk operations where console I/O would distort timings.
     */
    private boolean loggingEnabled = true;

    /**
     * Sequential id counter used only when {@code dbEnabled == false}
     * (test/benchmark mode) to assign unique ids without a real database.
     */
    private int mockIdCounter = 1;

    // ── Test-mode control ─────────────────────────────────────────────────────

    /**
     * Enables or disables test mode.
     *
     * <p>In test mode ({@code true}):
     * <ul>
     *   <li>No database calls are made — pure in-memory speed is measured.</li>
     *   <li>Console benchmark output is suppressed.</li>
     *   <li>Task ids are assigned by an internal counter instead of SQLite.</li>
     * </ul>
     *
     * @param isTestMode {@code true} to enable test mode, {@code false} for production
     */
    public void setTestMode(boolean isTestMode) {
        this.dbEnabled     = !isTestMode;
        this.loggingEnabled = !isTestMode;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Prints a benchmark result line to standard output.
     *
     * <p>Format: {@code [Benchmark] HashMap <operation>: <nanos> ns}
     *
     * @param operationLabel human-readable name of the measured operation
     * @param elapsedNanos   elapsed time in nanoseconds
     */
    private void printBenchmark(String operationLabel, long elapsedNanos) {
        if (loggingEnabled) {
            System.out.printf("[Benchmark] HashMap %s: %,d ns%n", operationLabel, elapsedNanos);
        }
    }

    // ── TaskRepository implementation ─────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Clears the existing {@link HashMap} and re-populates it from the
     * provided list. Each task is keyed by its {@code id}.
     * Complexity: O(n) where n = number of tasks.
     *
     * @param tasks the full task list loaded from SQLite; must not be null
     */
    @Override
    public void loadAll(List<Task> tasks) {
        long startTime = System.nanoTime();

        taskStore.clear();
        // Iterate the list and index every task by its id for O(1) future lookups
        tasks.forEach(task -> taskStore.put(task.getId(), task));

        printBenchmark("LoadAll (" + tasks.size() + " tasks)", System.nanoTime() - startTime);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Inserts into the {@link HashMap} in O(1) average time.
     * If persistence is enabled, the database insert runs first so that
     * the auto-generated id is available before the map entry is created.
     *
     * @param task the new task; title must not be null
     * @return the task with its id now assigned
     */
    @Override
    public Task add(Task task) {
        if (dbEnabled) {
            // Persist first — the database assigns the authoritative primary key
            int generatedId = databaseManager.insert(task);
            task.setId(generatedId);
        } else {
            // Test mode: use the internal counter to avoid database dependency
            task.setId(mockIdCounter++);
        }

        // Time only the in-memory HashMap operation
        long startTime = System.nanoTime();
        taskStore.put(task.getId(), task);
        printBenchmark("Insert", System.nanoTime() - startTime);

        return task;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Replaces the existing map entry in O(1) average time, then syncs
     * the updated task to SQLite.
     *
     * @param task the task with updated fields; matched by {@code task.getId()}
     */
    @Override
    public void update(Task task) {
        // Time the in-memory operation first
        long startTime = System.nanoTime();
        taskStore.put(task.getId(), task);
        printBenchmark("Update", System.nanoTime() - startTime);

        // Persist the change after the timed block
        if (dbEnabled) {
            databaseManager.update(task);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Removes the entry by key in O(1) average time, then deletes from SQLite.
     *
     * @param id the primary key of the task to remove
     */
    @Override
    public void delete(int id) {
        // Time the in-memory operation first
        long startTime = System.nanoTime();
        taskStore.remove(id);
        printBenchmark("Delete", System.nanoTime() - startTime);

        // Sync the deletion to the database after the timed block
        if (dbEnabled) {
            databaseManager.delete(id);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Direct key lookup — O(1) average. Returns an {@link Optional} so the
     * caller does not need to handle {@code null} explicitly.
     *
     * @param id the primary key to look up
     * @return an {@link Optional} containing the task, or empty if not found
     */
    @Override
    public Optional<Task> findById(int id) {
        long startTime = System.nanoTime();
        Optional<Task> result = Optional.ofNullable(taskStore.get(id));
        printBenchmark("FindById", System.nanoTime() - startTime);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates an unmodifiable snapshot of all values — O(n).
     *
     * @return unmodifiable list of all tasks currently in the store
     */
    @Override
    public List<Task> findAll() {
        long startTime = System.nanoTime();
        List<Task> result = List.copyOf(taskStore.values());
        printBenchmark("FindAll (" + result.size() + " tasks)", System.nanoTime() - startTime);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Linear scan of the map's values — O(n).
     *
     * @param completed {@code true} for completed tasks, {@code false} for pending
     * @return filtered list of matching tasks
     */
    @Override
    public List<Task> findByCompleted(boolean completed) {
        long startTime = System.nanoTime();
        List<Task> result = taskStore.values().stream()
                .filter(task -> task.isCompleted() == completed)
                .collect(Collectors.toList());
        printBenchmark("FindByCompleted", System.nanoTime() - startTime);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Linear scan filtered by comparing each task's {@code dueDate}
     * against {@link LocalDate#now()} — O(n).
     *
     * @return list of tasks whose due date equals today
     */
    @Override
    public List<Task> findDueToday() {
        long startTime = System.nanoTime();
        LocalDate today = LocalDate.now();
        List<Task> result = taskStore.values().stream()
                .filter(task -> today.equals(task.getDueDate()))
                .collect(Collectors.toList());
        printBenchmark("FindDueToday", System.nanoTime() - startTime);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Linear scan with case-insensitive category matching — O(n).
     *
     * @param category the category label to match
     * @return filtered list of tasks in that category
     */
    @Override
    public List<Task> findByCategory(String category) {
        long startTime = System.nanoTime();
        List<Task> result = taskStore.values().stream()
                .filter(task -> category.equalsIgnoreCase(task.getCategory()))
                .collect(Collectors.toList());
        printBenchmark("FindByCategory", System.nanoTime() - startTime);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Linear scan filtered by exact priority enum comparison — O(n).
     *
     * @param priority the {@link Priority} level to match
     * @return filtered list of tasks at the specified priority
     */
    @Override
    public List<Task> findByPriority(Priority priority) {
        long startTime = System.nanoTime();
        List<Task> result = taskStore.values().stream()
                .filter(task -> task.getPriority() == priority)
                .collect(Collectors.toList());
        printBenchmark("FindByPriority", System.nanoTime() - startTime);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @return the string {@code "HashMap"}
     */
    @Override
    public String getStrategyName() {
        return "HashMap";
    }
}
