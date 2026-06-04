package com.hetero.model;

import java.time.LocalDate;

/**
 * Task represents a single to-do item in the Hetero app.
 *
 * Each Task object stores all the information about one task:
 * its title, notes, priority level, category, due date, and
 * whether or not it has been completed.
 *
 * Every field is private so only the getters and setters
 * can read or change them — this is called encapsulation.
 *
 * This class maps directly to one row in the "tasks" table
 * in our SQLite database.
 */
public class Task {

    // ── Instance variables ────────────────────────────────────────────────────

    // The database gives this a number automatically when the task is saved.
    // Before saving it is 0.
    private int id;

    // A short name that describes the task. Cannot be blank.
    private String title;

    // Optional extra details about the task. Can be null.
    private String notes;

    // How urgent the task is. Uses our custom Priority enum.
    private Priority priority;

    // Groups the task under a label like "Work" or "Personal".
    // Defaults to "General" if the user leaves it blank.
    private String category;

    // The deadline for the task. Can be null if there is no due date.
    // We use LocalDate (from java.time) to avoid dealing with time zones.
    private LocalDate dueDate;

    // True if the user has marked this task as finished, false otherwise.
    private boolean isCompleted;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Full constructor — used when loading a task back from the database.
     * All seven values are already known at this point.
     *
     * @param id          the database primary key
     * @param title       the task title
     * @param notes       optional notes (can be null)
     * @param priority    how urgent this task is
     * @param category    the category label (null becomes "General")
     * @param dueDate     the deadline (can be null)
     * @param isCompleted whether the task is already done
     */
    public Task(int id,
                String title,
                String notes,
                Priority priority,
                String category,
                LocalDate dueDate,
                boolean isCompleted) {

        this.id          = id;
        this.title       = title;
        this.notes       = notes;
        this.priority    = priority;
        this.category    = (category != null && !category.isBlank()) ? category : "General";
        this.dueDate     = dueDate;
        this.isCompleted = isCompleted;
    }

    /**
     * Short constructor — used when the user creates a brand-new task.
     * The id will be assigned by the database later, so we start it at 0.
     * isCompleted starts as false because a new task is not done yet.
     *
     * This constructor just calls the full one with those safe defaults.
     *
     * @param title    the task title
     * @param notes    optional notes (can be null)
     * @param priority how urgent this task is
     * @param category the category label
     * @param dueDate  the deadline (can be null)
     */
    public Task(String title,
                String notes,
                Priority priority,
                String category,
                LocalDate dueDate) {

        this(0, title, notes, priority, category, dueDate, false);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** Returns the database id. Zero means the task has not been saved yet. */
    public int getId() { return id; }

    /** Returns the task title. */
    public String getTitle() { return title; }

    /** Returns the optional notes. May be null. */
    public String getNotes() { return notes; }

    /** Returns the priority level. */
    public Priority getPriority() { return priority; }

    /** Returns the category label. Never null — defaults to "General". */
    public String getCategory() { return category; }

    /** Returns the due date. May be null if no deadline was set. */
    public LocalDate getDueDate() { return dueDate; }

    /** Returns true if the task has been marked as completed. */
    public boolean isCompleted() { return isCompleted; }

    // ── Setters ───────────────────────────────────────────────────────────────

    /** Sets the database id. Only the repository layer should call this. */
    public void setId(int id) { this.id = id; }

    /** Updates the task title. */
    public void setTitle(String title) { this.title = title; }

    /** Updates the notes. Pass null to clear notes. */
    public void setNotes(String notes) { this.notes = notes; }

    /** Updates the priority level. */
    public void setPriority(Priority priority) { this.priority = priority; }

    /** Updates the category. Null or blank values become "General". */
    public void setCategory(String category) {
        this.category = (category != null && !category.isBlank()) ? category : "General";
    }

    /** Sets or clears the due date. Pass null to remove the deadline. */
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    /** Marks the task as completed (true) or reopens it (false). */
    public void setCompleted(boolean completed) { this.isCompleted = completed; }

    // ── toString ──────────────────────────────────────────────────────────────

    /**
     * Returns a readable summary of this task.
     * Useful for printing to the console during testing.
     */
    @Override
    public String toString() {
        return "Task{id=%d, title='%s', priority=%s, category='%s', dueDate=%s, isCompleted=%b}"
                .formatted(id, title, priority, category, dueDate, isCompleted);
    }
}
