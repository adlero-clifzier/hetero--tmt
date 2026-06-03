package com.hetero.repository;

import java.time.LocalDate;       // imported class — date comparison for findDueToday
import java.util.LinkedList;      // java.util collection — backing store
import java.util.List;            // java.util collection interface
import java.util.ListIterator;    // iterator for in-place node replacement
import java.util.Optional;        // nullable return wrapper
import java.util.stream.Collectors; // Stream terminal operation

import com.hetero.db.DatabaseManager; // custom-built singleton gateway
import com.hetero.model.Priority;     // custom-built enum
import com.hetero.model.Task;         // custom-built domain entity

/**
 * {@link TaskRepository} implementation backed by a {@link LinkedList}.
 *
 * <p><b>Algorithmic complexity:</b>
 * <ul>
 *   <li>Insert (tail) — O(1): {@link LinkedList#addLast} updates only two
 *       node pointers regardless of list length.</li>
 *   <li>Lookup by id  — O(n): requires a full sequential scan because nodes
 *       carry no index and cannot be addressed directly.</li>
 *   <li>Delete by id  — O(n): same sequential scan to find the target node.</li>
 *   <li>Update by id  — O(n): scan with {@link ListIterator} for in-place set.</li>
 * </ul>
 *
 * <p>This contrasts with {@link HashMapTaskRepo} (O(1) lookup) and
 * {@link ArrayListTaskRepo} (O(n) with index shift on delete), making the
 * three strategies an informative benchmark trio.
 *
 * <p><b>Specification compliance:</b>
 * <ul>
 *   <li><b>Inheritance / polymorphism / interface:</b> {@code implements TaskRepository}.</li>
 *   <li><b>Java Collection:</b> {@link LinkedList}, {@link List}, {@link ListIterator}.</li>
 *   <li><b>Imported classes:</b> {@link LocalDate}, {@link LinkedList}, {@link ListIterator},
 *       {@link Optional}, {@link DatabaseManager}, {@link Task}, {@link Priority}.</li>
 *   <li><b>Custom-built classes:</b> {@link Task}, {@link Priority}, {@link DatabaseManager}.</li>
 *   <li><b>Instance variables:</b> {@code taskStore}, {@code databaseManager},
 *       {@code dbEnabled}, {@code loggingEnabled}, {@code mockIdCounter}.</li>
 *   <li><b>Primitive data:</b> {@code dbEnabled} (boolean), {@code loggingEnabled} (boolean),
 *       {@code mockIdCounter} (int), timing delta (long).</li>
 * </ul>
 */
public class LinkedListTaskRepo implements TaskRepository {

    // ── Instance variables ────────────────────────────────────────────────────

    /**
     * The primary in-memory data store.
     * LinkedList chosen for O(1) tail-append and to demonstrate pointer-based
     * traversal cost vs. the HashMap and ArrayList strategies.
     */
    private final LinkedList<Task> taskStore = new LinkedList<>();

    /** Reference to the singleton database gateway for write-through persistence. */
    private final DatabaseManager databaseManager = DatabaseManager.getInstance();

    /** When {@code false}, all database calls are bypassed (test/benchmark mode). */
    private boolean dbEnabled = true;

    /** When {@code false}, benchmark console output is suppressed. */
    private boolean loggingEnabled = true;

    /** Sequential id counter used in test mode instead of the database. */
    private int mockIdCounter = 1;

    // ── Test-mode control ─────────────────────────────────────────────────────

    /**
     * Enables or disables test/benchmark mode.
     *
     * @param isTestMode {@code true} disables persistence and logging
     */
    public void setTestMode(boolean isTestMode) {
        this.dbEnabled      = !isTestMode;
        this.loggingEnabled = !isTestMode;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Emits a formatted benchmark result to stdout.
     *
     * @param operationLabel description of the measured operation
     * @param elapsedNanos   elapsed nanoseconds measured by {@link System#nanoTime()}
     */
    private void printBenchmark(String operationLabel, long elapsedNanos) {
        if (loggingEnabled) {
            System.out.printf("[Benchmark] LinkedList %s: %,d ns%n", operationLabel, elapsedNanos);
        }
    }

    // ── TaskRepository implementation ─────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Clears the list and appends all tasks in iteration order.
     * Complexity: O(n).
     *
     * @param tasks full task list from SQLite; must not be null
     */
    @Override
    public void loadAll(List<Task> tasks) {
        long startTime = System.nanoTime();

        taskStore.clear();
        taskStore.addAll(tasks); // bulk-add preserves the SQLite ordering

        printBenchmark("LoadAll (" + tasks.size() + " tasks)", System.nanoTime() - startTime);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Appends the new task to the tail of the linked list in O(1).
     * Database insert runs first so the auto-generated id is available.
     *
     * @param task the new task; title must not be null
     * @return the task with its id assigned
     */
    @Override
    public Task add(Task task) {
        if (dbEnabled) {
            // Persist first to get the database-assigned primary key
            int generatedId = databaseManager.insert(task);
            task.setId(generatedId);
        } else {
            // Test mode: assign sequential mock id without hitting the database
            task.setId(mockIdCounter++);
        }

        // Time only the in-memory linked-list operation (tail append)
        long startTime = System.nanoTime();
        taskStore.addLast(task);
        printBenchmark("Insert", System.nanoTime() - startTime);

        return task;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Scans the list with a {@link ListIterator} for in-place node replacement.
     * Complexity: O(n) — no random access available in a linked list.
     *
     * @param task task with updated fields; matched by id
     */
    @Override
    public void update(Task task) {
        long startTime = System.nanoTime();

        // Use a ListIterator to allow in-place replacement without removing/re-inserting
        ListIterator<Task> iterator = taskStore.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next().getId() == task.getId()) {
                iterator.set(task); // O(1) node pointer swap at the current position
                break;
            }
        }

        printBenchmark("Update", System.nanoTime() - startTime);

        if (dbEnabled) {
            databaseManager.update(task);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@link LinkedList#removeIf} for a single-pass O(n) scan and removal.
     *
     * @param id the primary key of the task to delete
     */
    @Override
    public void delete(int id) {
        long startTime = System.nanoTime();

        // removeIf traverses once and removes matching node in O(1) pointer update
        taskStore.removeIf(task -> task.getId() == id);

        printBenchmark("Delete", System.nanoTime() - startTime);

        if (dbEnabled) {
            databaseManager.delete(id);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sequential scan — O(n). No direct addressing is possible in a linked list.
     *
     * @param id the primary key to look up
     * @return an {@link Optional} containing the task, or empty if not found
     */
    @Override
    public Optional<Task> findById(int id) {
        long startTime = System.nanoTime();
        Optional<Task> result = taskStore.stream()
                .filter(task -> task.getId() == id)
                .findFirst();
        printBenchmark("FindById", System.nanoTime() - startTime);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns an unmodifiable copy of the list — O(n).
     *
     * @return unmodifiable snapshot of all tasks
     */
    @Override
    public List<Task> findAll() {
        long startTime = System.nanoTime();
        List<Task> result = List.copyOf(taskStore);
        printBenchmark("FindAll (" + result.size() + " tasks)", System.nanoTime() - startTime);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Linear scan — O(n).
     *
     * @param completed {@code true} for completed tasks, {@code false} for pending
     * @return filtered list
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
     * <p>Linear scan filtered by comparing {@code dueDate} with today — O(n).
     *
     * @return list of tasks due today
     */
    @Override
    public List<Task> findDueToday() {
        long startTime = System.nanoTime();
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
     * <p>Linear scan with case-insensitive category match — O(n).
     *
     * @param category the category label to filter by
     * @return filtered list
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
     * <p>Linear scan with exact priority enum comparison — O(n).
     *
     * @param priority the priority level to filter by
     * @return filtered list
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
     * @return the string {@code "LinkedList"}
     */
    @Override
    public String getStrategyName() {
        return "LinkedList";
    }
}
