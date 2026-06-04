// This file tells the Java module system which packages exist
// and which ones are allowed to be used by JavaFX.
module com.hetero {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.logging;

    // "opens" means JavaFX is allowed to read these packages at runtime
    opens com.hetero.app        to javafx.graphics, javafx.fxml;
    opens com.hetero.controller to javafx.fxml;
    opens com.hetero.model      to javafx.base, javafx.fxml;
    opens com.hetero.db         to javafx.fxml;
    opens com.hetero.repository to javafx.fxml;

    // The performance test package also needs to be visible
    opens com.hetero.test       to javafx.fxml;
}
