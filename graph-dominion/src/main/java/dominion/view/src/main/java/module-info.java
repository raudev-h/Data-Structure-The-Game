module com.juego.conquista {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    opens com.juego.conquista to javafx.fxml;
    exports com.juego.conquista;
}