package com.hetero.model;

import java.time.LocalDate;

/**
 * Core domain entity representing a single task in the Hetero system.
 * Maps directly to the 'tasks' table in SQLite.
 */
public class Task {

    private int id;
    private String title;
    private String notes;
    private Priority priority;
    private String category;
    private LocalDate dueDate;
    private boolean isCompleted;

    /** Full constructor used when hydrating from the database. */
    public Task(int id, String title, String notes, Priority priority,
                String category, LocalDate dueDate, boolean isCompleted) {
        this.id          = id;
        this.title       = title;
        this.notes       = notes;
        this.priority    = priority;
        this.category    = category;
        this.dueDate     = dueDate;
        this.isCompleted = isCompleted;
    }

    /** Creation constructor — id is assigned by the database on insert. */
    public Task(String title, String notes, Priority priority,
                String category, LocalDate dueDate) {
        this(0, title, notes, priority, category, dueDate, false);
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    /** @return the database-assigned primary key; 0 if not yet persisted. */
    public int getId()                  { return id; }
    public void setId(int id)           { this.id = id; }

    /** @return the task title; never null. */
    public String getTitle()            { return title; }
    public void setTitle(String title)  { this.title = title; }

    /** @return optional free-text notes attached to this task. */
    public String getNotes()            { return notes; }
    public void setNotes(String notes)  { this.notes = notes; }

    /** @return the {@link Priority} level of this task. */
    public Priority getPriority()                   { return priority; }
    public void setPriority(Priority priority)      { this.priority = priority; }

    /** @return the category label; defaults to "General". */
    public String getCategory()                     { return category; }
    public void setCategory(String category)        { this.category = category; }

    /** @return the optional due date, or {@code null} if none set. */
    public LocalDate getDueDate()                   { return dueDate; }
    public void setDueDate(LocalDate dueDate)       { this.dueDate = dueDate; }

    /** @return {@code true} if the task has been marked complete. */
    public boolean isCompleted()                        { return isCompleted; }
    public void setCompleted(boolean completed)         { isCompleted = completed; }

    @Override
    public String toString() {
        return "Task{id=%d, title='%s', priority=%s, category='%s', due=%s, done=%b}"
                .formatted(id, title, priority, category, dueDate, isCompleted);
    }
}
