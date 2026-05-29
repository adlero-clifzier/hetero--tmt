package com.hetero.controller;

import com.hetero.model.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/** Groups tasks by category, renders each as a collapsible card. */
public class CategoriesController implements Initializable {

    @FXML private VBox container;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        List<Task> all = MainLayoutController.getActiveRepo().findAll();
        Map<String, List<Task>> grouped = all.stream()
            .collect(Collectors.groupingBy(Task::getCategory));

        grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> container.getChildren().add(buildCard(entry.getKey(), entry.getValue())));
    }

    private VBox buildCard(String category, List<Task> tasks) {
        long done = tasks.stream().filter(Task::isCompleted).count();

        Label header = new Label(category + "  (" + done + "/" + tasks.size() + ")");
        header.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:14px;-fx-font-weight:bold;-fx-padding:12 16 8 16;");

        VBox taskList = new VBox(2);
        tasks.forEach(t -> {
            Label lbl = new Label((t.isCompleted() ? "✓  " : "○  ") + t.getTitle());
            lbl.setStyle("-fx-text-fill:" + (t.isCompleted() ? "#4b5563" : "#cbd5e1") +
                ";-fx-padding:4 16 4 28;-fx-font-size:13px;");
            taskList.getChildren().add(lbl);
        });

        VBox card = new VBox(header, new Separator(), taskList);
        card.setStyle("-fx-background-color:#161a22;-fx-border-color:#252c3b;" +
            "-fx-border-radius:8;-fx-background-radius:8;-fx-border-width:1;-fx-margin:0 0 10 0;");
        VBox.setMargin(card, new javafx.geometry.Insets(0, 0, 10, 0));
        return card;
    }
}
