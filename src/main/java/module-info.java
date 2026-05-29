module com.hetero {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.logging;

    opens com.hetero.app        to javafx.graphics, javafx.fxml;
    opens com.hetero.controller to javafx.fxml;
    opens com.hetero.model      to javafx.base, javafx.fxml;
    opens com.hetero.db         to javafx.fxml;
    opens com.hetero.repository to javafx.fxml;
}
