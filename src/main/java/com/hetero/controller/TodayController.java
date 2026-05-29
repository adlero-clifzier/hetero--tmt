package com.hetero.controller;

import com.hetero.model.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.beans.property.SimpleBooleanProperty;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/** Shows tasks due today with inline completion toggle. */
public class TodayController implements Initializable {

    @FXML private Label lblDate, lblCount;
    @FXML private ListView<Task> listView;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")));
        List<Task> today = MainLayoutController.getActiveRepo().findDueToday();
        lblCount.setText(today.size() + " task" + (today.size() == 1 ? "" : "s") + " due today");

        listView.getItems().setAll(today);
        listView.setCellFactory(lv -> new ListCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setOnAction(e -> {
                    Task t = getItem();
                    if (t != null) {
                        t.setCompleted(cb.isSelected());
                        MainLayoutController.getActiveRepo().update(t);
                    }
                });
            }
            @Override protected void updateItem(Task t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) { setGraphic(null); return; }
                cb.setSelected(t.isCompleted());
                cb.setText(t.getTitle());
                cb.setStyle("-fx-text-fill:#e2e8f0;");
                if (t.isCompleted()) cb.setStyle("-fx-text-fill:#4b5563;-fx-strikethrough:true;");
                setGraphic(cb);
                setStyle("-fx-background-color:transparent;-fx-padding:6 12;");
            }
        });
    }
}
