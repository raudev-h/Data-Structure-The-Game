package com.juego.conquista;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView; // IMPORTANTE: Asegúrate de tener esta línea
import javafx.stage.Stage;

public class principalMenu extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Cargamos el archivo FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/juego/conquista/principalMenu.fxml"));
        Parent root = loader.load();

        // 2. Buscamos la imagen de fondo para que se estire con la ventana
        // Usamos lookup para encontrar el ID que pusimos en el FXML (#backgroundImageView)
        ImageView bg = (ImageView) root.lookup("#backgroundImageView");
        if (bg != null) {
            bg.fitWidthProperty().bind(stage.widthProperty());
            bg.fitHeightProperty().bind(stage.heightProperty());
        }

        // 3. Configuramos la escena
        Scene scene = new Scene(root);
        stage.setScene(scene);

        // 4. Ponemos pantalla completa y mostramos
        stage.setFullScreen(true);
        stage.setFullScreenExitHint(""); // Quita el mensaje de "Presione ESC para salir"
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}