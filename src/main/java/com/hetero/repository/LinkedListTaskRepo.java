package com.hetero.repository;

import com.hetero.db.DatabaseManager;
import com.hetero.model.Priority;
import com.hetero.model.Task;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link TaskRepository} backed by a {@link LinkedList}.
 *
 * <p>Provides O(1) head/tail insert and O(n) lookup/delete, demonstrating
 * the trade-off versus the HashMap strategy. Benchmark output is emitted
 * for every operation to make the O(n) scan cost visible.
 */
public class LinkedListTaskRepo implements TaskRepository {

    private final LinkedList<Task> store = new LinkedList<>();
    private final DatabaseManager db = DatabaseManager.getInstance();

    private static void bench(String operation, long nanos) {
        System.out.printf("[Benchmark] LinkedList %s: %,d ns%n", operation, nanos);
    }

    /** {@inheritDoc} O(n) — rebuilds the list from scratch. */
    @Override
    public void loadAll(List<Task> tasks) {
        long t0 = System.nanoTime();
        store.clear();
        store.addAll(tasks);
        bench("LoadAll (" + tasks.size() + " tasks)", System.nanoTime() - t0);
    }

    /** {@inheritDoc} O(1) append to tail, then SQLite sync. */
    @Override
    public Task add(Task task) {
        int generatedId = db.insert(task);
        task.setId(generatedId);

        long t0 = System.nanoTime();
        store.addLast(task);
        bench("Insert", System.nanoTime() - t0);

        return task;
    }

    /** {@inheritDoc} O(n) scan to find and replace, then SQLite sync. */
    @Override
    public void update(Task task) {
        long t0 = System.nanoTime();
        ListIterator<Task> it = store.listIterator();
        while (it.hasNext()) {
            if (it.next().getId() == task.getId()) {
                it.set(task);
                break;
            }
        }
        bench("Update", System.nanoTime() - t0);
        db.update(task);
    }

    /** {@inheritDoc} O(n) scan to find and remove, then SQLite sync. */
    @Override
    public void delete(int id) {
        long t0 = System.nanoTime();
        store.removeIf(t -> t.getId() == id);
        bench("Delete", System.nanoTime() - t0);
        db.delete(id);
    }

    /** {@inheritDoc} O(n) linear scan. */
    @Override
    public Optional<Task> findById(int id) {
        long t0 = System.nanoTime();
        Optional<Task> result = store.stream().filter(t -> t.getId() == id).findFirst();
        bench("FindById", System.nanoTime() - t0);
        return result;
    }

    /** {@inheritDoc} O(n) — returns a copy of the list. */
    @Override
    public List<Task> findAll() {
        long t0 = System.nanoTime();
        List<Task> result = List.copyOf(store);
        bench("FindAll (" + result.size() + " tasks)", System.nanoTime() - t0);
        return result;
    }

    /** {@inheritDoc} O(n) linear scan. */
    @Override
    public List<Task> findByCompleted(boolean completed) {
        long t0 = System.nanoTime();
        List<Task> result = store.stream()
                .filter(t -> t.isCompleted() == completed)
                .collect(Collectors.toList());
        bench("FindByCompleted", System.nanoTime() - t0);
        return result;
    }

    /** {@inheritDoc} O(n) linear scan. */
    @Override
    public List<Task> findDueToday() {
        long t0 = System.nanoTime();
        LocalDate today = LocalDate.now();
        List<Task> result = store.stream()
                .filter(t -> today.equals(t.getDueDate()))
                .collect(Collectors.toList());
        bench("FindDueToday", System.nanoTime() - t0);
        return result;
    }

    /** {@inheritDoc} O(n) linear scan. */
    @Override
    public List<Task> findByCategory(String category) {
        long t0 = System.nanoTime();
        List<Task> result = store.stream()
                .filter(t -> category.equalsIgnoreCase(t.getCategory()))
                .collect(Collectors.toList());
        bench("FindByCategory", System.nanoTime() - t0);
        return result;
    }

    /** {@inheritDoc} O(n) linear scan. */
    @Override
    public List<Task> findByPriority(Priority priority) {
        long t0 = System.nanoTime();
        List<Task> result = store.stream()
                .filter(t -> t.getPriority() == priority)
                .collect(Collectors.toList());
        bench("FindByPriority", System.nanoTime() - t0);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String getStrategyName() { return "LinkedList"; }
}
