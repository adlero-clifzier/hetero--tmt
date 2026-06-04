package com.hetero.controller;

import com.hetero.model.Priority;
import com.hetero.model.Task;
import com.hetero.repository.TaskRepository;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * AllTasksController powers the main task management screen.
 *
 * It shows all tasks in a table and lets the user:
 *   - Add a new task using the form panel (opened by "+ New Task")
 *   - Edit an existing task by clicking Edit in that row
 *   - Delete a task by clicking Delete (a confirmation dialog appears first)
 *   - Toggle completion with the checkbox in the first column
 *   - Search tasks by typing in the search bar
 *   - Filter tasks by status or priority using the dropdown
 *
 * All changes go through the active TaskRepository strategy —
 * whichever data structure the user has selected in the topbar.
 */
public class AllTasksController implements Initializable {

    // ── Table and columns ─────────────────────────────────────────────────────

    @FXML private TableView<Task>            table;
    @FXML private TableColumn<Task, Boolean> colDone;      // Checkbox column
    @FXML private TableColumn<Task, String>  colTitle;
    @FXML private TableColumn<Task, String>  colPriority;  // Colour-coded priority label
    @FXML private TableColumn<Task, String>  colCategory;
    @FXML private TableColumn<Task, String>  colDue;
    @FXML private TableColumn<Task, Void>    colActions;   // Edit + Delete buttons

    // ── Form panel fields ─────────────────────────────────────────────────────

    @FXML private TextField          fTitle;
    @FXML private TextField          fCategory;
    @FXML private TextArea           fNotes;
    @FXML private ComboBox<Priority> fPriority;
    @FXML private DatePicker         fDue;
    @FXML private Button             btnSave;
    @FXML private Label              lblFormTitle;
    @FXML private VBox               formPanel;  // The whole form — shown/hidden as needed

    // ── Filter bar ────────────────────────────────────────────────────────────

    @FXML private TextField        tfSearch;
    @FXML private ComboBox<String> cmbFilter;
    @FXML private Label            lblCount;

    // ── State ─────────────────────────────────────────────────────────────────

    // ObservableList automatically refreshes the TableView when it changes
    private final ObservableList<Task> tableData = FXCollections.observableArrayList();

    // Holds the task being edited, or null when we are adding a new one
    private Task currentlyEditingTask = null;

    // ── Initializable ─────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureTableColumns();

        // Fill the priority dropdown with all five enum values
        fPriority.setItems(FXCollections.observableArrayList(Priority.values()));
        fPriority.setValue(Priority.MEDIUM);

        // Fill the filter dropdown
        cmbFilter.setItems(FXCollections.observableArrayList(
            "All", "Pending", "Completed",
            "Critical", "High", "Medium", "Low", "Minimal"));
        cmbFilter.setValue("All");
        cmbFilter.setOnAction(e -> applySearchAndFilter());

        // Live-filter as the user types
        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> applySearchAndFilter());

        table.setItems(tableData);
        table.setEditable(true);

        refreshTable();
    }

    // ── Column setup ──────────────────────────────────────────────────────────

    /**
     * Sets up how each column gets its data and how each cell looks.
     * The checkbox column saves changes immediately when toggled.
     * The priority column changes text colour based on urgency.
     * The actions column has Edit and Delete buttons per row.
     */
    private void configureTableColumns() {

        // Checkbox column — toggles isCompleted and saves right away
        colDone.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            SimpleBooleanProperty completedProp = new SimpleBooleanProperty(task.isCompleted());
            completedProp.addListener((obs, oldVal, newVal) -> {
                task.setCompleted(newVal);
                getRepo().update(task);
                updateCountLabel();
            });
            return completedProp;
        });
        colDone.setCellFactory(CheckBoxTableCell.forTableColumn(colDone));

        // Title column
        colTitle.setCellValueFactory(
            cd -> new SimpleStringProperty(cd.getValue().getTitle()));

        // Priority column — each level gets a different colour
        colPriority.setCellValueFactory(
            cd -> new SimpleStringProperty(cd.getValue().getPriority().name()));

        colPriority.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String priorityName, boolean isEmpty) {
                super.updateItem(priorityName, isEmpty);
                if (isEmpty || priorityName == null) { setText(null); setStyle(""); return; }
                setText(priorityName);
                String colour = switch (priorityName) {
                    case "CRITICAL" -> "#f87171";
                    case "HIGH"     -> "#fb923c";
                    case "MEDIUM"   -> "#facc15";
                    case "LOW"      -> "#4ade80";
                    default         -> "#94a3b8";
                };
                setStyle("-fx-text-fill:" + colour + ";-fx-font-weight:bold;");
            }
        });

        // Category column
        colCategory.setCellValueFactory(
            cd -> new SimpleStringProperty(cd.getValue().getCategory()));

        // Due date column — shows "—" when no date is set
        colDue.setCellValueFactory(cd -> {
            String text = cd.getValue().getDueDate() != null
                    ? cd.getValue().getDueDate().toString()
                    : "—";
            return new SimpleStringProperty(text);
        });

        // Actions column — each row gets Edit and Delete buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");

            {
                editBtn.getStyleClass().add("btn-sm-accent");
                deleteBtn.getStyleClass().add("btn-sm-danger");
                editBtn.setOnAction(e ->
                    openEditForm(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e ->
                    confirmAndDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void v, boolean isEmpty) {
                super.updateItem(v, isEmpty);
                setGraphic(isEmpty ? null : new HBox(6, editBtn, deleteBtn));
            }
        });
    }

    // ── CRUD handlers ─────────────────────────────────────────────────────────

    /** Opens the form panel in "add new task" mode. */
    @FXML
    private void onNewTask() {
        currentlyEditingTask = null;
        lblFormTitle.setText("New Task");
        btnSave.setText("Add Task");
        clearForm();
        showFormPanel();
    }

    /**
     * Called when the Save button is clicked.
     * Either adds a new task or updates the existing one depending on the mode.
     */
    @FXML
    private void onSave() {
        String enteredTitle = fTitle.getText().trim();

        // Title is required — highlight the field red if it is empty
        if (enteredTitle.isEmpty()) {
            fTitle.setStyle("-fx-border-color:#f87171;");
            return;
        }
        fTitle.setStyle("");

        // Use "General" if the category field is left blank
        String resolvedCategory = fCategory.getText().trim().isEmpty()
                ? "General"
                : fCategory.getText().trim();

        // Null-safe notes to avoid NullPointerException in the database layer
        String notes = (fNotes.getText() != null) ? fNotes.getText() : "";

        if (currentlyEditingTask == null) {
            // Add mode — create a brand-new Task object and save it
            Task newTask = new Task(
                enteredTitle,
                notes,
                fPriority.getValue(),
                resolvedCategory,
                fDue.getValue()
            );
            getRepo().add(newTask);
        } else {
            // Edit mode — update the existing task's fields
            currentlyEditingTask.setTitle(enteredTitle);
            currentlyEditingTask.setNotes(notes);
            currentlyEditingTask.setPriority(fPriority.getValue());
            currentlyEditingTask.setCategory(resolvedCategory);
            currentlyEditingTask.setDueDate(fDue.getValue());
            getRepo().update(currentlyEditingTask);
        }

        onCancel();    // Close the form
        refreshTable();
    }

    /** Hides the form panel and resets its state. */
    @FXML
    private void onCancel() {
        hideFormPanel();
        currentlyEditingTask = null;
        clearForm();
    }

    /** Populates the form panel with the given task's data for editing. */
    private void openEditForm(Task task) {
        currentlyEditingTask = task;
        lblFormTitle.setText("Edit Task");
        btnSave.setText("Save Changes");
        fTitle.setText(task.getTitle());
        fNotes.setText(task.getNotes() != null ? task.getNotes() : "");
        fPriority.setValue(task.getPriority());
        fCategory.setText(task.getCategory());
        fDue.setValue(task.getDueDate());
        showFormPanel();
    }

    /** Shows a confirmation dialog before deleting a task. */
    private void confirmAndDelete(Task task) {
        Alert dialog = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Delete \"" + task.getTitle() + "\"?",
            ButtonType.YES,
            ButtonType.NO
        );
        dialog.setHeaderText(null);
        dialog.showAndWait()
            .filter(r -> r == ButtonType.YES)
            .ifPresent(r -> {
                getRepo().delete(task.getId());
                refreshTable();
            });
    }

    // ── Filter and refresh ────────────────────────────────────────────────────

    /**
     * Filters the table using the current search text and selected filter option.
     * Called every time the search box changes or the filter dropdown changes.
     */
    private void applySearchAndFilter() {
        String searchText     = tfSearch.getText().toLowerCase();
        String selectedFilter = cmbFilter.getValue();
        List<Task> allTasks   = getRepo().findAll();

        tableData.setAll(allTasks.stream().filter(task -> {

            boolean matchesSearch = searchText.isEmpty()
                || task.getTitle().toLowerCase().contains(searchText)
                || task.getCategory().toLowerCase().contains(searchText);

            boolean matchesFilter = switch (selectedFilter) {
                case "Pending"   -> !task.isCompleted();
                case "Completed" ->  task.isCompleted();
                case "Critical"  -> task.getPriority() == Priority.CRITICAL;
                case "High"      -> task.getPriority() == Priority.HIGH;
                case "Medium"    -> task.getPriority() == Priority.MEDIUM;
                case "Low"       -> task.getPriority() == Priority.LOW;
                case "Minimal"   -> task.getPriority() == Priority.MINIMAL;
                default          -> true;
            };

            return matchesSearch && matchesFilter;
        }).toList());

        updateCountLabel();
    }

    /** Reloads all tasks from the repository into the table. */
    private void refreshTable() {
        tableData.setAll(getRepo().findAll());
        updateCountLabel();
    }

    /** Updates the "X tasks · Y completed" label below the toolbar. */
    private void updateCountLabel() {
        long completed = tableData.stream().filter(Task::isCompleted).count();
        lblCount.setText(tableData.size() + " tasks  ·  " + completed + " completed");
    }

    /** Clears all fields in the form panel back to their defaults. */
    private void clearForm() {
        fTitle.clear();
        fNotes.clear();
        fCategory.clear();
        fPriority.setValue(Priority.MEDIUM);
        fDue.setValue(null);
    }

    private void showFormPanel() { formPanel.setVisible(true);  formPanel.setManaged(true);  }
    private void hideFormPanel() { formPanel.setVisible(false); formPanel.setManaged(false); }

    /** Shortcut to get the active repository from the main controller. */
    private TaskRepository getRepo() {
        return MainLayoutController.getActiveRepository();
    }
}
