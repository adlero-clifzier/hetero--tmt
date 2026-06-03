package com.hetero.repository;

import com.hetero.model.Priority; // custom-built enum from the model package
import com.hetero.model.Task;     // custom-built domain entity

import java.util.List;     // java.util collection interface
import java.util.Optional; // java.util wrapper for nullable return values

/**
 * Strategy interface for in-memory task data access.
 *
 * <p>This is the centrepiece of the <b>Strategy Pattern</b> used throughout
 * Hetero. By programming to this interface, the rest of the application
 * ({@link com.hetero.controller.MainLayoutController} and all child controllers)
 * remains completely decoupled from whichever concrete backing collection is
 * active at runtime.
 *
 * <p>Three concrete strategies are provided:
 * <ol>
 *   <li>{@link HashMapTaskRepo}  — O(1) average-case lookup / insert / delete.</li>
 *   <li>{@link LinkedListTaskRepo} — O(1) tail insert, O(n) lookup / delete.</li>
 *   <li>{@link ArrayListTaskRepo} — O(1) amortised append, O(n) remove / find.</li>
 * </ol>
 *
 * <p><b>Specification compliance:</b>
 * <ul>
 *   <li><b>Interface (inheritance / polymorphism):</b> All three repository
 *       implementations {@code implements TaskRepository}, enabling runtime
 *       polymorphic dispatch through a single reference type.</li>
 *   <li><b>Imported classes:</b> {@link List}, {@link Optional}, {@link Task},
 *       {@link Priority}.</li>
 *   <li><b>Custom-built classes:</b> {@link Task} and {@link Priority}.</li>
 *   <li><b>Java Collections:</b> Returns and accepts {@link List} throughout.</li>
 *   <li><b>Detailed documentation:</b> Every method carries a full Javadoc
 *       block with {@code @param} and {@code @return} tags.</li>
 * </ul>
 */
public interface TaskRepository {

    /**
     * Replaces the entire in-memory store with the provided list.
     *
     * <p>Called on application startup and whenever the user switches the
     * active data-structure strategy via the topbar {@code ComboBox}.
     * The implementation must clear its existing store before loading.
     *
     * @param tasks the full task list loaded fresh from SQLite; must not be null
     */
    void loadAll(List<Task> tasks);

    /**
     * Inserts a new task into the in-memory collection and persists it to SQLite.
     *
     * <p>The {@code task.id} is populated with the database-generated key as a
     * side-effect of this call, so the caller can use the returned object
     * immediately without a subsequent lookup.
     *
     * @param task the task to add; {@code title} must not be null or blank
     * @return the same task object with its {@code id} now assigned
     */
    Task add(Task task);

    /**
     * Updates an existing task in the in-memory collection and syncs to SQLite.
     *
     * <p>The task is matched by its {@code id}; if no matching entry exists
     * the operation is silently ignored.
     *
     * @param task the task carrying updated field values; matched by {@code id}
     */
    void update(Task task);

    /**
     * Removes a task by its primary key from the in-memory collection and SQLite.
     *
     * @param id the primary key of the task to delete
     */
    void delete(int id);

    /**
     * Looks up a single task by its primary key in the in-memory collection.
     *
     * <p>Using {@link Optional} as the return type makes the absence of a
     * result explicit at the call site and avoids null-pointer risks.
     *
     * @param id the primary key to search for
     * @return an {@link Optional} containing the task, or {@code Optional.empty()}
     */
    Optional<Task> findById(int id);

    /**
     * Returns all tasks currently held in the in-memory collection.
     *
     * @return an unmodifiable snapshot list; never null, but may be empty
     */
    List<Task> findAll();

    /**
     * Filters tasks by completion status from the in-memory collection.
     *
     * @param completed {@code true} to return only completed tasks;
     *                  {@code false} for pending tasks
     * @return list of matching tasks; never null
     */
    List<Task> findByCompleted(boolean completed);

    /**
     * Returns all tasks whose {@code dueDate} equals today's date.
     *
     * <p>Tasks without a due date ({@code null}) are excluded.
     *
     * @return list of tasks due today; never null, but may be empty
     */
    List<Task> findDueToday();

    /**
     * Filters tasks by category label using a case-insensitive comparison.
     *
     * @param category the category string to match
     * @return list of tasks belonging to the specified category; never null
     */
    List<Task> findByCategory(String category);

    /**
     * Filters tasks by exact priority level.
     *
     * @param priority the {@link Priority} enum value to filter by
     * @return list of tasks with the specified priority; never null
     */
    List<Task> findByPriority(Priority priority);

    /**
     * Returns the human-readable name of the backing collection strategy.
     *
     * <p>Displayed in the topbar performance metric label so the user always
     * knows which data structure is active.
     *
     * @return one of {@code "HashMap"}, {@code "LinkedList"}, or {@code "ArrayList"}
     */
    String getStrategyName();
}
