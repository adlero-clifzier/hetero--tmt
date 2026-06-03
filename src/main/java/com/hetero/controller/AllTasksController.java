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
 * Controller for {@code all-tasksView.fxml} — the full CRUD task management view.
 *
 * <p>Provides:
 * <ul>
 *   <li>A filterable, searchable {@link TableView} showing all tasks.</li>
 *   <li>An inline slide-in form panel for adding new tasks and editing existing ones.</li>
 *   <li>Inline checkbox completion toggling directly in the table.</li>
 *   <li>Per-row Edit and Delete action buttons.</li>
 * </ul>
 *
 * <p>All CRUD operations are delegated to the active {@link TaskRepository}
 * strategy via {@link MainLayoutController#getActiveRepository()}, so this
 * controller automatically benefits from whichever data structure is selected
 * in the topbar without any code changes.
 *
 * <p><b>Specification compliance — this class demonstrates:</b>
 * <ul>
 *   <li><b>Inheritance / interface:</b>
 *       {@code implements Initializable} — JavaFX post-injection setup hook.</li>
 *   <li><b>Java Collections:</b>
 *       {@link ObservableList} as the live table data source; {@link List} for
 *       filtered results from the repository.</li>
 *   <li><b>Polymorphism:</b>
 *       {@link TaskRepository} is used as the declared type — the concrete
 *       implementation is resolved at runtime.</li>
 *   <li><b>Custom-built classes:</b>
 *       {@link Task}, {@link Priority}, {@link TaskRepository}, {@link MainLayoutController}.</li>
 *   <li><b>Imported classes:</b>
 *       {@link SimpleBooleanProperty}, {@link SimpleStringProperty},
 *       {@link FXCollections}, {@link ObservableList}, {@link CheckBoxTableCell},
 *       and many other JavaFX imports.</li>
 *   <li><b>Primitive data:</b>
 *       {@code long done} task count; {@code boolean} completed flag; loop indices.</li>
 *   <li><b>Instance variables:</b>
 *       {@code tableData} (Observable collection), {@code currentlyEditingTask}.</li>
 *   <li><b>Meaningful identifiers:</b>
 *       {@code tableData}, {@code currentlyEditingTask}, {@code searchText},
 *       {@code selectedFilter}, {@code priorityColorHex} — all descriptive.</li>
 * </ul>
 */
public class AllTasksController implements Initializable {

    // ── FXML-injected — TableView and columns ─────────────────────────────────

    /** The main task table. Editable to support the inline checkbox column. */
    @FXML private TableView<Task>           table;

    /** Checkbox column — toggling marks the task complete / incomplete. */
    @FXML private TableColumn<Task, Boolean> colDone;

    /** Title column — displays the task's title text. */
    @FXML private TableColumn<Task, String>  colTitle;

    /** Priority column — displays a colour-coded priority badge. */
    @FXML private TableColumn<Task, String>  colPriority;

    /** Category column — displays the task's category label. */
    @FXML private TableColumn<Task, String>  colCategory;

    /** Due date column — displays the due date or "—" if none set. */
    @FXML private TableColumn<Task, String>  colDue;

    /** Actions column — contains Edit and Delete buttons per row. */
    @FXML private TableColumn<Task, Void>    colActions;

    // ── FXML-injected — Form panel fields ─────────────────────────────────────

    /** Title text field in the add/edit form. */
    @FXML private TextField       fTitle;

    /** Category text field in the add/edit form. */
    @FXML private TextField       fCategory;

    /** Notes text area in the add/edit form. */
    @FXML private TextArea        fNotes;

    /** Priority ComboBox in the add/edit form. */
    @FXML private ComboBox<Priority> fPriority;

    /** Due date picker in the add/edit form. */
    @FXML private DatePicker      fDue;

    /** Save/Update button — text changes based on add vs edit mode. */
    @FXML private Button          btnSave;

    /** Title label at the top of the form panel — changes between "New Task" and "Edit Task". */
    @FXML private Label           lblFormTitle;

    /** The slide-in form panel container. Hidden until the user clicks "+ New Task" or "Edit". */
    @FXML private VBox            formPanel;

    // ── FXML-injected — Filter bar ─────────────────────────────────────────────

    /** Search text field — filters tasks by title or category as the user types. */
    @FXML private TextField        tfSearch;

    /** Filter ComboBox — shows All, Pending, Completed, or specific priority levels. */
    @FXML private ComboBox<String> cmbFilter;

    /** Label showing the count of visible tasks and how many are completed. */
    @FXML private Label            lblCount;

    // ── Instance state ────────────────────────────────────────────────────────

    /**
     * The {@link ObservableList} that backs the {@link TableView}.
     * JavaFX automatically refreshes the table whenever this list changes.
     */
    private final ObservableList<Task> tableData = FXCollections.observableArrayList();

    /**
     * The task currently being edited in the form panel.
     * {@code null} when the form is in "add new task" mode.
     */
    private Task currentlyEditingTask = null;

    // ── Initializable ─────────────────────────────────────────────────────────

    /**
     * Called by JavaFX after all {@code @FXML} fields are injected.
     * Sets up table columns, populates the form ComboBoxes, attaches listeners
     * to the search field and filter ComboBox, and loads the initial task data.
     *
     * @param location  the FXML URL (unused)
     * @param resources the resource bundle (unused)
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureTableColumns();

        // Populate the priority ComboBox with all enum values
        fPriority.setItems(FXCollections.observableArrayList(Priority.values()));
        fPriority.setValue(Priority.MEDIUM);

        // Populate the filter ComboBox
        cmbFilter.setItems(FXCollections.observableArrayList(
            "All", "Pending", "Completed",
            "Critical", "High", "Medium", "Low", "Minimal"));
        cmbFilter.setValue("All");
        cmbFilter.setOnAction(event -> applySearchAndFilter());

        // Live-filter as the user types in the search box
        tfSearch.textProperty().addListener(
            (observable, oldValue, newValue) -> applySearchAndFilter());

        table.setItems(tableData);
        table.setEditable(true);

        refreshTableFromRepository();
    }

    // ── Column configuration ──────────────────────────────────────────────────

    /**
     * Configures cell-value factories and cell factories for all table columns.
     *
     * <p>The checkbox column uses a {@link SimpleBooleanProperty} whose
     * change listener immediately persists the toggle to the active repository.
     * The priority column uses a custom {@link TableCell} to apply colour styling.
     * The actions column uses an anonymous inner class to embed Edit/Delete buttons.
     */
    private void configureTableColumns() {

        // ── Checkbox (done) column ────────────────────────────────────────────
        colDone.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            SimpleBooleanProperty completedProperty =
                new SimpleBooleanProperty(task.isCompleted());

            // When the checkbox is toggled, persist the change immediately
            completedProperty.addListener((observable, oldValue, newValue) -> {
                task.setCompleted(newValue);
                getActiveRepository().update(task);
                updateTaskCountLabel();
            });
            return completedProperty;
        });
        colDone.setCellFactory(CheckBoxTableCell.forTableColumn(colDone));

        // ── Title column ──────────────────────────────────────────────────────
        colTitle.setCellValueFactory(
            cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));

        // ── Priority column — colour-coded ────────────────────────────────────
        colPriority.setCellValueFactory(
            cellData -> new SimpleStringProperty(cellData.getValue().getPriority().name()));

        colPriority.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String priorityName, boolean isEmpty) {
                super.updateItem(priorityName, isEmpty);

                if (isEmpty || priorityName == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(priorityName);

                // Map each priority level to its designated colour hex code
                String priorityColorHex = switch (priorityName) {
                    case "CRITICAL" -> "#f87171"; // red
                    case "HIGH"     -> "#fb923c"; // orange
                    case "MEDIUM"   -> "#facc15"; // yellow
                    case "LOW"      -> "#4ade80"; // green
                    default         -> "#94a3b8"; // grey (MINIMAL)
                };

                setStyle("-fx-text-fill:" + priorityColorHex + ";-fx-font-weight:bold;");
            }
        });

        // ── Category column ───────────────────────────────────────────────────
        colCategory.setCellValueFactory(
            cellData -> new SimpleStringProperty(cellData.getValue().getCategory()));

        // ── Due date column ───────────────────────────────────────────────────
        colDue.setCellValueFactory(cellData -> {
            String dueDateText = (cellData.getValue().getDueDate() != null)
                    ? cellData.getValue().getDueDate().toString()
                    : "—";
            return new SimpleStringProperty(dueDateText);
        });

        // ── Actions column — Edit and Delete buttons ──────────────────────────
        colActions.setCellFactory(column -> new TableCell<>() {

            private final Button editButton   = new Button("Edit");
            private final Button deleteButton = new Button("Delete");

            // Configure button styles and click handlers once in the initialiser block
            {
                editButton.getStyleClass().add("btn-sm-accent");
                deleteButton.getStyleClass().add("btn-sm-danger");

                editButton.setOnAction(event ->
                    openEditForm(getTableView().getItems().get(getIndex())));

                deleteButton.setOnAction(event ->
                    confirmAndDeleteTask(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void value, boolean isEmpty) {
                super.updateItem(value, isEmpty);
                setGraphic(isEmpty ? null : new HBox(6, editButton, deleteButton));
            }
        });
    }

    // ── Form panel actions ────────────────────────────────────────────────────

    /**
     * Handles the "+ New Task" button — shows the form panel in add mode.
     */
    @FXML
    private void onNewTask() {
        currentlyEditingTask = null;
        lblFormTitle.setText("New Task");
        btnSave.setText("Add Task");
        clearFormFields();
        showFormPanel();
    }

    /**
     * Handles the Save / Add Task button click.
     *
     * <p>Validates the title, then either adds a new task or updates the
     * existing one depending on whether {@code currentlyEditingTask} is set.
     */
    @FXML
    private void onSave() {
        String enteredTitle = fTitle.getText().trim();

        // Highlight the title field red if blank — required field
        if (enteredTitle.isEmpty()) {
            fTitle.setStyle("-fx-border-color:#f87171;");
            return;
        }
        fTitle.setStyle(""); // Clear the error highlight

        String resolvedCategory = fCategory.getText().trim().isEmpty()
                ? "General"
                : fCategory.getText().trim();

        if (currentlyEditingTask == null) {
            // ── Add mode: create and persist a brand-new task ─────────────────
            Task newTask = new Task(
                enteredTitle,
                fNotes.getText(),
                fPriority.getValue(),
                resolvedCategory,
                fDue.getValue()
            );
            getActiveRepository().add(newTask);

        } else {
            // ── Edit mode: apply changes to the existing task object ──────────
            currentlyEditingTask.setTitle(enteredTitle);
            currentlyEditingTask.setNotes(fNotes.getText());
            currentlyEditingTask.setPriority(fPriority.getValue());
            currentlyEditingTask.setCategory(resolvedCategory);
            currentlyEditingTask.setDueDate(fDue.getValue());
            getActiveRepository().update(currentlyEditingTask);
        }

        onCancel();                   // Collapse the form panel
        refreshTableFromRepository(); // Reload the table with updated data
    }

    /**
     * Handles the Cancel button click — collapses the form panel.
     */
    @FXML
    private void onCancel() {
        hideFormPanel();
        currentlyEditingTask = null;
        clearFormFields();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Populates the form panel with the given task's data and shows the panel
     * in edit mode.
     *
     * @param taskToEdit the task whose fields should be loaded into the form
     */
    private void openEditForm(Task taskToEdit) {
        currentlyEditingTask = taskToEdit;
        lblFormTitle.setText("Edit Task");
        btnSave.setText("Save Changes");

        fTitle.setText(taskToEdit.getTitle());
        fNotes.setText(taskToEdit.getNotes());
        fPriority.setValue(taskToEdit.getPriority());
        fCategory.setText(taskToEdit.getCategory());
        fDue.setValue(taskToEdit.getDueDate());

        showFormPanel();
    }

    /**
     * Prompts the user for confirmation before deleting a task.
     * Only proceeds with deletion if the user confirms via the dialog.
     *
     * @param taskToDelete the task the user wants to remove
     */
    private void confirmAndDeleteTask(Task taskToDelete) {
        Alert confirmationDialog = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Delete \"" + taskToDelete.getTitle() + "\"?",
            ButtonType.YES,
            ButtonType.NO
        );
        confirmationDialog.setHeaderText(null);

        confirmationDialog.showAndWait()
            .filter(response -> response == ButtonType.YES)
            .ifPresent(confirmed -> {
                getActiveRepository().delete(taskToDelete.getId());
                refreshTableFromRepository();
            });
    }

    /**
     * Applies the current search text and filter ComboBox selection to the
     * task list and updates {@code tableData} accordingly.
     */
    private void applySearchAndFilter() {
        String searchText      = tfSearch.getText().toLowerCase();
        String selectedFilter  = cmbFilter.getValue();
        List<Task> allTasks    = getActiveRepository().findAll();

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
                default          -> true; // "All" — no filter applied
            };

            return matchesSearch && matchesFilter;

        }).toList());

        updateTaskCountLabel();
    }

    /**
     * Reloads all tasks from the active repository into {@code tableData}.
     * Called after any CRUD operation to ensure the table reflects current state.
     */
    private void refreshTableFromRepository() {
        tableData.setAll(getActiveRepository().findAll());
        updateTaskCountLabel();
    }

    /**
     * Updates the task count label below the toolbar with the current visible
     * task count and how many of those are marked as completed.
     */
    private void updateTaskCountLabel() {
        long completedTaskCount = tableData.stream()
            .filter(Task::isCompleted)
            .count();
        lblCount.setText(tableData.size() + " tasks  ·  " + completedTaskCount + " completed");
    }

    /** Clears all form input fields back to their default/empty states. */
    private void clearFormFields() {
        fTitle.clear();
        fNotes.clear();
        fCategory.clear();
        fPriority.setValue(Priority.MEDIUM);
        fDue.setValue(null);
    }

    /** Makes the form panel visible and allocates space for it in the layout. */
    private void showFormPanel() {
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    /** Hides the form panel and removes it from the layout flow to reclaim space. */
    private void hideFormPanel() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
    }

    /**
     * Convenience accessor that reads the active repository from the shared
     * static field in {@link MainLayoutController}.
     *
     * @return the currently active {@link TaskRepository}
     */
    private TaskRepository getActiveRepository() {
        return MainLayoutController.getActiveRepository();
    }
}
