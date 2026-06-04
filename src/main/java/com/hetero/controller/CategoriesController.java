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
 * CategoriesController groups tasks by category and shows each
 * group as a card on the Categories screen.
 *
 * For example if you have tasks in "Work" and "Personal",
 * you will see two cards — one for each category.
 *
 * Each card shows the category name, how many tasks are done
 * out of the total, and a row for each task in that category.
 *
 * Cards are sorted alphabetically so the order is predictable.
 *
 * We use a Map from the Java Collections framework to group
 * tasks by category — Map.Entry gives us the category name
 * (key) and its list of tasks (value).
 *
 * BUG FIX: Previously used hardcoded dark-mode colours which
 * made the text invisible in light mode. Now uses CSS style
 * classes that adapt to both themes.
 */
public class CategoriesController implements Initializable {

    // The VBox inside the ScrollPane where we add the category cards
    @FXML private VBox container;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<Task> allTasks = MainLayoutController.getActiveRepository().findAll();

        // Group tasks by category name into a Map<String, List<Task>>
        Map<String, List<Task>> groupedByCategory = allTasks.stream()
            .collect(Collectors.groupingBy(Task::getCategory));

        // Sort alphabetically and build one card for each category
        groupedByCategory.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry ->
                container.getChildren().add(
                    buildCategoryCard(entry.getKey(), entry.getValue())));
    }

    /**
     * Builds a styled card for one category group.
     * The card has a header with the category name and completion count,
     * a divider, and a row for each task.
     *
     * Uses the CSS "card" style class so colours work in both themes.
     *
     * @param categoryName    the name of this category (e.g. "Work")
     * @param tasksInCategory the list of tasks belonging to this category
     * @return a VBox styled as a card, ready to add to the container
     */
    private VBox buildCategoryCard(String categoryName, List<Task> tasksInCategory) {

        // Count completed tasks — result is a primitive long
        long completedCount = tasksInCategory.stream()
            .filter(Task::isCompleted)
            .count();

        // Header shows: "Work  (2/5)"
        Label headerLabel = new Label(
            categoryName + "  (" + completedCount + "/" + tasksInCategory.size() + ")");
        headerLabel.getStyleClass().add("form-title");
        headerLabel.setStyle("-fx-padding:12 16 8 16;");

        // Build the list of task rows inside the card
        VBox taskListBox = new VBox(2);
        tasksInCategory.forEach(task ->
            taskListBox.getChildren().add(buildTaskRow(task)));

        // Assemble the card and apply the "card" CSS class for theme compatibility
        VBox card = new VBox(headerLabel, new Separator(), taskListBox);
        card.getStyleClass().add("card");
        VBox.setMargin(card, new Insets(0, 0, 12, 0));

        return card;
    }

    /**
     * Builds one row label for a task inside a category card.
     * Completed tasks get a tick (✓) and the "stat-label" style (muted colour).
     * Pending tasks get a circle (○) and normal text.
     *
     * Using CSS classes instead of hardcoded hex colours means the
     * text remains readable in both dark and light themes.
     *
     * @param task the task to display as a row
     * @return a Label configured for this task
     */
    private Label buildTaskRow(Task task) {
        String prefix = task.isCompleted() ? "✓  " : "○  ";

        Label rowLabel = new Label(prefix + task.getTitle());
        rowLabel.setStyle("-fx-padding:4 16 4 28;-fx-font-size:13px;");

        // stat-label is a muted style defined in both CSS theme files
        if (task.isCompleted()) {
            rowLabel.getStyleClass().add("stat-label");
        }

        return rowLabel;
    }
}
