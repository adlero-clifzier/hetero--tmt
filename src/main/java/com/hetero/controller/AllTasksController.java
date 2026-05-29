package com.hetero.controller;

import com.hetero.model.Priority;
import com.hetero.model.Task;
import com.hetero.repository.TaskRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/** Full CRUD view for all tasks — wired to the active TaskRepository strategy. */
public class AllTasksController implements Initializable {

    @FXML private TableView<Task>              table;
    @FXML private TableColumn<Task,Boolean>    colDone;
    @FXML private TableColumn<Task,String>     colTitle, colPriority, colCategory, colDue;
    @FXML private TableColumn<Task,Void>       colActions;

    // Form fields
    @FXML private TextField     fTitle, fCategory;
    @FXML private TextArea      fNotes;
    @FXML private ComboBox<Priority> fPriority;
    @FXML private DatePicker    fDue;
    @FXML private Button        btnSave;
    @FXML private Label         lblFormTitle;

    // Filter bar
    @FXML private TextField     tfSearch;
    @FXML private ComboBox<String> cmbFilter;

    @FXML private VBox          formPanel;
    @FXML private Label         lblCount;

    private final ObservableList<Task> data = FXCollections.observableArrayList();
    private Task editingTask = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        fPriority.setItems(FXCollections.observableArrayList(Priority.values()));
        fPriority.setValue(Priority.MEDIUM);
        cmbFilter.setItems(FXCollections.observableArrayList(
            "All","Pending","Completed","Critical","High","Medium","Low","Minimal"));
        cmbFilter.setValue("All");
        cmbFilter.setOnAction(e -> applyFilter());
        tfSearch.textProperty().addListener((o,ov,nv) -> applyFilter());
        table.setItems(data);
        table.setEditable(true);
        refresh();
    }

    // ── Columns ───────────────────────────────────────────────────────────────

    private void setupColumns() {
        colDone.setCellValueFactory(cd -> {
            Task t = cd.getValue();
            javafx.beans.property.SimpleBooleanProperty prop =
                new javafx.beans.property.SimpleBooleanProperty(t.isCompleted());
            prop.addListener((o, ov, nv) -> {
                t.setCompleted(nv);
                repo().update(t);
                updateCount();
            });
            return prop;
        });
        colDone.setCellFactory(CheckBoxTableCell.forTableColumn(colDone));

        colTitle.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTitle()));
        colPriority.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPriority().name()));
        colPriority.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = switch (item) {
                    case "CRITICAL" -> "#f87171"; case "HIGH" -> "#fb923c";
                    case "MEDIUM"   -> "#facc15"; case "LOW"  -> "#4ade80";
                    default         -> "#94a3b8";
                };
                setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;");
            }
        });
        colCategory.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCategory()));
        colDue.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getDueDate() != null ? cd.getValue().getDueDate().toString() : "—"));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button("Edit");
            private final Button del  = new Button("Delete");
            { edit.getStyleClass().add("btn-sm-accent");
              del.getStyleClass().add("btn-sm-danger");
              edit.setOnAction(e -> openEdit(getTableView().getItems().get(getIndex())));
              del.setOnAction(e  -> deleteTask(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : new HBox(6, edit, del));
            }
        });
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @FXML private void onNewTask() {
        editingTask = null;
        lblFormTitle.setText("New Task");
        btnSave.setText("Add Task");
        clearForm();
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML private void onSave() {
        String title = fTitle.getText().trim();
        if (title.isEmpty()) { fTitle.setStyle("-fx-border-color:#f87171;"); return; }
        fTitle.setStyle("");

        if (editingTask == null) {
            Task t = new Task(title, fNotes.getText(),
                fPriority.getValue(), fCategory.getText().isEmpty() ? "General" : fCategory.getText(),
                fDue.getValue());
            repo().add(t);
        } else {
            editingTask.setTitle(title);
            editingTask.setNotes(fNotes.getText());
            editingTask.setPriority(fPriority.getValue());
            editingTask.setCategory(fCategory.getText().isEmpty() ? "General" : fCategory.getText());
            editingTask.setDueDate(fDue.getValue());
            repo().update(editingTask);
        }
        onCancel();
        refresh();
    }

    @FXML private void onCancel() {
        formPanel.setVisible(false); formPanel.setManaged(false);
        editingTask = null; clearForm();
    }

    private void openEdit(Task t) {
        editingTask = t;
        lblFormTitle.setText("Edit Task");
        btnSave.setText("Save Changes");
        fTitle.setText(t.getTitle());
        fNotes.setText(t.getNotes());
        fPriority.setValue(t.getPriority());
        fCategory.setText(t.getCategory());
        fDue.setValue(t.getDueDate());
        formPanel.setVisible(true); formPanel.setManaged(true);
    }

    private void deleteTask(Task t) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + t.getTitle() + "\"?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.showAndWait().filter(r -> r == ButtonType.YES).ifPresent(r -> {
            repo().delete(t.getId()); refresh();
        });
    }

    // ── Filter / Refresh ──────────────────────────────────────────────────────

    private void applyFilter() {
        String search = tfSearch.getText().toLowerCase();
        String filter = cmbFilter.getValue();
        List<Task> all = repo().findAll();
        data.setAll(all.stream().filter(t -> {
            boolean matchSearch = search.isEmpty() ||
                t.getTitle().toLowerCase().contains(search) ||
                t.getCategory().toLowerCase().contains(search);
            boolean matchFilter = switch (filter) {
                case "Pending"   -> !t.isCompleted();
                case "Completed" -> t.isCompleted();
                case "Critical"  -> t.getPriority() == Priority.CRITICAL;
                case "High"      -> t.getPriority() == Priority.HIGH;
                case "Medium"    -> t.getPriority() == Priority.MEDIUM;
                case "Low"       -> t.getPriority() == Priority.LOW;
                case "Minimal"   -> t.getPriority() == Priority.MINIMAL;
                default          -> true;
            };
            return matchSearch && matchFilter;
        }).toList());
        updateCount();
    }

    private void refresh() {
        data.setAll(repo().findAll());
        updateCount();
    }

    private void updateCount() {
        long done = data.stream().filter(Task::isCompleted).count();
        lblCount.setText(data.size() + " tasks · " + done + " completed");
    }

    private void clearForm() {
        fTitle.clear(); fNotes.clear(); fCategory.clear();
        fPriority.setValue(Priority.MEDIUM); fDue.setValue(null);
    }

    private TaskRepository repo() { return MainLayoutController.getActiveRepo(); }
}
