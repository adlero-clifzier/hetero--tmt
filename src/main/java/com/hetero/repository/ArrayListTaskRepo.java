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
 * {@link TaskRepository} implementation backed by an {@link ArrayList}.
 *
 * <p><b>Algorithmic complexity:</b>
 * <ul>
 *   <li>Insert (append) — O(1) amortised: elements are appended to the end of the
 *       backing array. An occasional O(n) resize occurs when capacity is exceeded,
 *       but the amortised cost per insert remains O(1).</li>
 *   <li>Lookup by id  — O(n): requires a full index scan because there is no hash map.</li>
 *   <li>Delete by id  — O(n): scan plus O(n) element shift to close the gap.</li>
 *   <li>Update by id  — O(n): scan to find the index, then O(1) {@code set}.</li>
 * </ul>
 *
 * <p>This contrasts with {@link HashMapTaskRepo} (O(1) lookup) and
 * {@link LinkedListTaskRepo} (O(1) tail insert, no index shift on delete),
 * making the three strategies an informative algorithmic benchmark trio.
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Inheritance / polymorphism / interface:</b>
 *       {@code implements TaskRepository} — enables runtime polymorphic dispatch.</li>
 *   <li><b>Java Collection:</b>
 *       {@link ArrayList} as the primary store; {@link List} for return types.</li>
 *   <li><b>Imported classes:</b>
 *       {@link LocalDate}, {@link ArrayList}, {@link List}, {@link Optional},
 *       {@link Collectors}, {@link DatabaseManager}, {@link Task}, {@link Priority}.</li>
 *   <li><b>Custom-built classes:</b>
 *       {@link Task}, {@link Priority}, {@link DatabaseManager}.</li>
 *   <li><b>Instance variables:</b>
 *       {@code taskStore}, {@code databaseManager}, {@code isDbEnabled},
 *       {@code isLoggingEnabled}, {@code mockIdCounter}.</li>
 *   <li><b>Primitive data:</b>
 *       {@code isDbEnabled} (boolean), {@code isLoggingEnabled} (boolean),
 *       {@code mockIdCounter} (int), timing delta (long), loop index (int).</li>
 *   <li><b>Exception handling:</b>
 *       Delegated entirely to {@link DatabaseManager}; this layer stays free
 *       of checked-exception clutter and focuses on collection logic.</li>
 *   <li><b>Meaningful identifiers:</b>
 *       Variables named {@code taskStore}, {@code operationLabel},
 *       {@code elapsedNanos}, {@code currentIndex} — never single letters.</li>
 * </ul>
 */
public class ArrayListTaskRepo implements TaskRepository {

    // ── Instance variables ────────────────────────────────────────────────────

    /**
     * The primary in-memory data store.
     * {@link ArrayList} chosen to demonstrate index-based access and the
     * O(n) element-shift cost of mid-list removal versus the other strategies.
     */
    private final ArrayList<Task> taskStore = new ArrayList<>();

    /** Reference to the singleton database gateway for write-through persistence. */
    private final DatabaseManager databaseManager = DatabaseManager.getInstance();

    /**
     * When {@code false}, all calls to {@link DatabaseManager} are bypassed.
     * Useful during isolated benchmark testing to measure pure collection speed.
     */
    private boolean isDbEnabled = true;

    /**
     * When {@code false}, benchmark console output is suppressed.
     * Avoids console I/O overhead skewing timings during bulk operations.
     */
    private boolean isLoggingEnabled = true;

    /**
     * Sequential counter used in test mode to assign task ids without a database.
     * Incremented by one for each {@link #add} call made while {@code isDbEnabled == false}.
     */
    private int mockIdCounter = 1;

    // ── Test-mode control ─────────────────────────────────────────────────────

    /**
     * Enables or disables test/benchmark mode for this repository instance.
     *
     * <p>In test mode ({@code isTestMode = true}):
     * <ul>
     *   <li>No database calls are made — pure in-memory speed is measured.</li>
     *   <li>Console benchmark output is suppressed.</li>
     *   <li>Task ids are assigned by an internal counter.</li>
     * </ul>
     *
     * @param isTestMode {@code true} to enable test mode; {@code false} for production
     */
    public void setTestMode(boolean isTestMode) {
        this.isDbEnabled      = !isTestMode;
        this.isLoggingEnabled = !isTestMode;
    }

    // ── Private helper ────────────────────────────────────────────────────────

    /**
     * Conditionally prints a benchmark result line to standard output.
     *
     * <p>Format: {@code [Benchmark] ArrayList <operationLabel>: <elapsedNanos> ns}
     *
     * @param operationLabel human-readable name of the measured operation
     * @param elapsedNanos   elapsed time in nanoseconds
     */
    private void printBenchmark(String operationLabel, long elapsedNanos) {
        if (isLoggingEnabled) {
            System.out.printf("[Benchmark] ArrayList %s: %,d ns%n",
                operationLabel, elapsedNanos);
        }
    }

    // ── TaskRepository implementation ─────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Clears the existing {@link ArrayList} and refills it from the provided list.
     * Complexity: O(n) where n = number of tasks.
     *
     * @param tasks the full task list loaded from SQLite; must not be null
     */
    @Override
    public void loadAll(List<Task> tasks) {
        long startTime = System.nanoTime();

        taskStore.clear();
        taskStore.addAll(tasks); // Bulk-add preserves the provided ordering

        printBenchmark("LoadAll (" + tasks.size() + " tasks)", System.nanoTime() - startTime);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Appends the new task to the end of the ArrayList in O(1) amortised time.
     * The database insert runs first so the auto-generated id is available
     * before the element is added to the list.
     *
     * @param task the new task to add; {@code title} must not be null
     * @return the same task with its {@code id} now assigned
     */
    @Override
    public Task add(Task task) {
        if (isDbEnabled) {
            // Persist to SQLite first to obtain the authoritative auto-incremented id
            int generatedId = databaseManager.insert(task);
            task.setId(generatedId);
        } else {
            // Test mode: assign a sequential mock id without touching the database
            task.setId(mockIdCounter++);
        }

        // Time only the in-memory ArrayList operation
        long startTime = System.nanoTime();
        taskStore.add(task);
        printBenchmark("Insert", System.nanoTime() - startTime);

        return task;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Scans the array by index to find the matching task, then calls
     * {@link ArrayList#set} for an O(1) replacement at that position.
     * Overall complexity: O(n) for the scan.
     *
     * @param updatedTask the task with updated fields; matched by {@code getId()}
     */
    @Override
    public void update(Task updatedTask) {
        long startTime = System.nanoTime();

        // Linear scan to locate the index of the task with the matching id
        for (int currentIndex = 0; currentIndex < taskStore.size(); currentIndex++) {
            if (taskStore.get(currentIndex).getId() == updatedTask.getId()) {
                taskStore.set(currentIndex, updatedTask); // O(1) index-based replacement
                break;
            }
        }

        printBenchmark("Update", System.nanoTime() - startTime);

        // Sync the change to SQLite after the timed in-memory block
        if (isDbEnabled) {
            databaseManager.update(updatedTask);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@link ArrayList#removeIf} which performs an O(n) scan and compacts
     * the array by shifting remaining elements left — demonstrating the O(n) delete
     * cost characteristic of contiguous-memory structures.
     *
     * @param taskId the primary key of the task to delete
     */
    @Override
    public void delete(int taskId) {
        long startTime = System.nanoTime();

        // removeIf traverses once and handles the O(n) element shift internally
        taskStore.removeIf(task -> task.getId() == taskId);

        printBenchmark("Delete", System.nanoTime() - startTime);

        if (isDbEnabled) {
            databaseManager.delete(taskId);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Linear scan to find the first element whose id matches — O(n).
     * Uses Java Stream API with {@link Optional} to avoid null-return patterns.
     *
     * @param taskId the primary key to look up
     * @return an {@link Optional} containing the task, or empty if not found
     */
    @Override
    public Optional<Task> findById(int taskId) {
        long startTime = System.nanoTime();
        Optional<Task> result = taskStore.stream()
                .filter(task -> task.getId() == taskId)
                .findFirst();
        printBenchmark("FindById", System.nanoTime() - startTime);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns an unmodifiable snapshot of the entire list — O(n) copy.
     * Callers cannot mutate the returned list; modifications must go through
     * the repository methods to maintain write-through consistency.
     *
     * @return unmodifiable list of all tasks currently in the store
     */
    @Override
    public List<Task> findAll() {
        long startTime = System.nanoTime();
        List<Task> snapshot = List.copyOf(taskStore);
        printBenchmark("FindAll (" + snapshot.size() + " tasks)", System.nanoTime() - startTime);
        return snapshot;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Linear scan filtered by the completion flag — O(n).
     *
     * @param completed {@code true} for completed tasks; {@code false} for pending
     * @return filtered list of matching tasks; never null
     */
    @Override
    public List<Task> findByCompleted(boolean completed) {
        long startTime = System.nanoTime();
        List<Task> result = taskStore.stream()
                .filter(task -> task.isCompleted() == completed)
                .collect(Collectors.toList());
        printBenchmark("FindByCompleted", System.nanoTime() - startTime);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Linear scan comparing each task's {@code dueDate} against today — O(n).
     * Tasks with a null {@code dueDate} are excluded by the {@link LocalDate#equals} check.
     *
     * @return list of tasks whose due date equals today's date
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Linear scan with case-insensitive category matching — O(n).
     *
     * @param category the category label to match
     * @return filtered list of tasks in that category; never null
     */
    @Override
    public List<Task> findByCategory(String category) {
        long startTime = System.nanoTime();
        List<Task> result = taskStore.stream()
                .filter(task -> category.equalsIgnoreCase(task.getCategory()))
                .collect(Collectors.toList());
        printBenchmark("FindByCategory", System.nanoTime() - startTime);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Linear scan filtered by exact {@link Priority} enum comparison — O(n).
     *
     * @param priority the priority level to match
     * @return filtered list of tasks at that priority; never null
     */
    @Override
    public List<Task> findByPriority(Priority priority) {
        long startTime = System.nanoTime();
        List<Task> result = taskStore.stream()
                .filter(task -> task.getPriority() == priority)
                .collect(Collectors.toList());
        printBenchmark("FindByPriority", System.nanoTime() - startTime);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @return the string {@code "ArrayList"}
     */
    @Override
    public String getStrategyName() {
        return "ArrayList";
    }
}
