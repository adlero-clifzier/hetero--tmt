package com.hetero.repository;

import com.hetero.model.Priority;
import com.hetero.model.Task;
import java.util.List;
import java.util.Optional;

/**
 * TaskRepository is an interface that defines the operations
 * every data structure must be able to perform on tasks.
 *
 * An interface is like a contract — it says "any class that
 * implements me must have all of these methods."
 *
 * We have three classes that implement this interface:
 *   - HashMapTaskRepo   (uses a HashMap)
 *   - LinkedListTaskRepo (uses a LinkedList)
 *   - ArrayListTaskRepo  (uses an ArrayList)
 *
 * This is the Strategy Pattern — we can swap which data
 * structure is active without changing any other code.
 * The rest of the app just talks to this interface.
 *
 * Optional is used as the return type for findById because
 * the task might not exist — it is cleaner than returning null.
 */
public interface TaskRepository {

    /**
     * Fills the in-memory collection with all tasks from the database.
     * Called on startup and whenever the user switches data structure mode.
     *
     * @param tasks the list of tasks loaded from SQLite
     */
    void loadAll(List<Task> tasks);

    /**
     * Adds a new task to the collection and saves it to the database.
     * The task's id is set automatically by the database.
     *
     * @param task the new task to add
     * @return the same task, now with its database id filled in
     */
    Task add(Task task);

    /**
     * Updates an existing task in the collection and saves the changes.
     * The task is matched by its id.
     *
     * @param task the task with updated values
     */
    void update(Task task);

    /**
     * Removes a task from the collection and deletes it from the database.
     *
     * @param id the id of the task to remove
     */
    void delete(int id);

    /**
     * Searches for a single task by its id.
     * Returns an Optional so the caller does not have to deal with null.
     *
     * @param id the id to look for
     * @return Optional containing the task if found, or empty if not found
     */
    Optional<Task> findById(int id);

    /**
     * Returns a snapshot of all tasks currently in the collection.
     *
     * @return a list of all tasks
     */
    List<Task> findAll();

    /**
     * Returns only completed tasks (true) or only pending tasks (false).
     *
     * @param completed pass true for done tasks, false for not-done tasks
     * @return filtered list
     */
    List<Task> findByCompleted(boolean completed);

    /**
     * Returns all tasks whose due date is today.
     * Tasks with no due date are excluded.
     *
     * @return list of tasks due today
     */
    List<Task> findDueToday();

    /**
     * Returns all tasks that belong to the given category.
     * The comparison ignores upper/lower case.
     *
     * @param category the category name to search for
     * @return list of matching tasks
     */
    List<Task> findByCategory(String category);

    /**
     * Returns all tasks that have the given priority level.
     *
     * @param priority the priority to filter by
     * @return list of matching tasks
     */
    List<Task> findByPriority(Priority priority);

    /**
     * Returns the name of the data structure backing this repository.
     * This name is shown in the topbar of the app.
     *
     * @return "HashMap", "LinkedList", or "ArrayList"
     */
    String getStrategyName();
}
