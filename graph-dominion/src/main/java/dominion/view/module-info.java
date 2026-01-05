module dominion {
    requires javafx.controls;
    requires javafx.fxml;

    opens dominion.view to javafx.fxml;
    exports dominion.view;
} 