package com.hetero.repository;

import com.hetero.model.Priority;
import com.hetero.model.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for in-memory task data access.
 *
 * <p>Each implementation backs the in-memory store with a different Java
 * collection ({@code HashMap}, {@code LinkedList}, {@code ArrayList}) so that
 * algorithmic complexity differences are measurable via benchmark output.
 *
 * <p>All mutating operations perform the collection operation first (timed),
 * then asynchronously sync the change to SQLite.
 */
public interface TaskRepository {

    /**
     * Replaces the entire in-memory store with the provided list.
     * Called on startup or when the active strategy is swapped.
     *
     * @param tasks the full task list loaded from SQLite
     */
    void loadAll(List<Task> tasks);

    /**
     * Inserts a new task into the in-memory collection and persists it to SQLite.
     * The {@code task.id} is populated with the generated database key after insert.
     *
     * @param task the task to add; must have a non-null title
     * @return the saved task with its assigned id
     */
    Task add(Task task);

    /**
     * Updates an existing task in the in-memory collection and syncs to SQLite.
     *
     * @param task the task with updated fields; matched by {@code task.id}
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
     * @param id the primary key to search for
     * @return an {@link Optional} containing the task, or empty if not found
     */
    Optional<Task> findById(int id);

    /**
     * Returns all tasks currently held in the in-memory collection.
     *
     * @return an unmodifiable snapshot of all tasks
     */
    List<Task> findAll();

    /**
     * Filters tasks by completion status from the in-memory collection.
     *
     * @param completed {@code true} to return completed tasks, {@code false} for pending
     * @return list of matching tasks
     */
    List<Task> findByCompleted(boolean completed);

    /**
     * Filters tasks whose due date equals today from the in-memory collection.
     *
     * @return list of tasks due today
     */
    List<Task> findDueToday();

    /**
     * Filters tasks by category label from the in-memory collection.
     *
     * @param category the category string to match (case-insensitive)
     * @return list of matching tasks
     */
    List<Task> findByCategory(String category);

    /**
     * Filters tasks by priority level from the in-memory collection.
     *
     * @param priority the {@link Priority} to filter by
     * @return list of matching tasks
     */
    List<Task> findByPriority(Priority priority);

    /**
     * Returns the human-readable name of the backing collection strategy.
     * Used by the UI to display the active mode label.
     *
     * @return e.g. "HashMap", "LinkedList", "ArrayList"
     */
    String getStrategyName();
}
