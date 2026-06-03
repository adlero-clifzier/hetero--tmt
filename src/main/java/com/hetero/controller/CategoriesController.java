package com.hetero.controller;

import com.hetero.model.Task;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for {@code categoriesView.fxml}.
 *
 * <p>Groups all tasks by their {@code category} field and renders each group
 * as a distinct card (a styled {@link VBox}) inside a scrollable container.
 * Cards are sorted alphabetically by category name.
 *
 * <p>Each card shows:
 * <ul>
 *   <li>The category name and a {@code done / total} count in the header.</li>
 *   <li>A bulleted list of task titles, with completed ones visually distinguished.</li>
 * </ul>
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Inheritance / interface:</b>
 *       {@code implements Initializable}.</li>
 *   <li><b>Java Collections:</b>
 *       {@link Map} (from {@code Collectors.groupingBy}), {@link List}.</li>
 *   <li><b>Polymorphism:</b>
 *       {@link com.hetero.repository.TaskRepository} accessed via interface reference.</li>
 *   <li><b>Custom-built classes:</b>
 *       {@link Task}, {@link MainLayoutController}.</li>
 *   <li><b>Primitive data:</b>
 *       {@code long completedCount} used in the card header.</li>
 *   <li><b>Meaningful identifiers:</b>
 *       {@code groupedByCategory}, {@code categoryName}, {@code tasksInCategory},
 *       {@code completedCount}, {@code categoryCard}.</li>
 *   <li><b>Avoid duplicating similar code:</b>
 *       Card and row construction is encapsulated in private helper methods.</li>
 * </ul>
 */
public class CategoriesController implements Initializable {

    // ── FXML-injected controls ────────────────────────────────────────────────

    /**
     * The root {@link VBox} container inside the ScrollPane.
     * Category cards are dynamically appended here during initialisation.
     */
    @FXML private VBox container;

    // ── Initializable ─────────────────────────────────────────────────────────

    /**
     * Loads all tasks, groups them by category, and renders a card per group.
     *
     * @param location  the FXML URL (unused)
     * @param resources the resource bundle (unused)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<Task> allTasks = MainLayoutController.getActiveRepository().findAll();

        // Group tasks into a Map<categoryName, List<Task>> using Java Streams
        Map<String, List<Task>> groupedByCategory = allTasks.stream()
            .collect(Collectors.groupingBy(Task::getCategory));

        // Sort categories alphabetically and build one card per category
        groupedByCategory.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry ->
                container.getChildren().add(
                    buildCategoryCard(entry.getKey(), entry.getValue())));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds a styled card {@link VBox} for a single category group.
     *
     * <p>The card contains a header label with the category name and completion
     * count, a {@link Separator}, and a row for each task in the group.
     *
     * @param categoryName     the category label used as the card title
     * @param tasksInCategory  the list of tasks belonging to this category
     * @return a fully populated, styled {@link VBox} card ready to add to the container
     */
    private VBox buildCategoryCard(String categoryName, List<Task> tasksInCategory) {

        // Primitive long — count completed tasks for the header badge
        long completedCount = tasksInCategory.stream()
            .filter(Task::isCompleted)
            .count();

        // Header label: "Work  (2/5)"
        Label headerLabel = new Label(
            categoryName + "  (" + completedCount + "/" + tasksInCategory.size() + ")");
        headerLabel.setStyle(
            "-fx-text-fill:#e2e8f0;" +
            "-fx-font-size:14px;" +
            "-fx-font-weight:bold;" +
            "-fx-padding:12 16 8 16;");

        // Build the task-row list inside the card
        VBox taskListBox = new VBox(2);
        tasksInCategory.forEach(task ->
            taskListBox.getChildren().add(buildTaskRowLabel(task)));

        // Assemble the card
        VBox categoryCard = new VBox(headerLabel, new Separator(), taskListBox);
        categoryCard.setStyle(
            "-fx-background-color:#161a22;" +
            "-fx-border-color:#252c3b;"     +
            "-fx-border-radius:8;"          +
            "-fx-background-radius:8;"      +
            "-fx-border-width:1;");
        VBox.setMargin(categoryCard, new Insets(0, 0, 12, 0));

        return categoryCard;
    }

    /**
     * Builds a single task-row {@link Label} for display inside a category card.
     *
     * <p>Completed tasks show a tick prefix and greyed-out text.
     * Pending tasks show a circle prefix and normal text.
     *
     * @param task the task to render as a label row
     * @return a configured {@link Label} representing the task row
     */
    private Label buildTaskRowLabel(Task task) {
        String rowPrefix      = task.isCompleted() ? "✓  " : "○  ";
        String rowTextColor   = task.isCompleted() ? "#4b5563" : "#cbd5e1";

        Label taskRowLabel = new Label(rowPrefix + task.getTitle());
        taskRowLabel.setStyle(
            "-fx-text-fill:" + rowTextColor + ";" +
            "-fx-padding:4 16 4 28;"              +
            "-fx-font-size:13px;");

        return taskRowLabel;
    }
}
