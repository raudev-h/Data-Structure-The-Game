package dominion.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class principalMenu extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/juego/conquista/principalMenu.fxml"));
        Parent root = loader.load();


        ImageView bg = (ImageView) root.lookup("#backgroundImageView");
        if (bg != null) {
            bg.fitWidthProperty().bind(stage.widthProperty());
            bg.fitHeightProperty().bind(stage.heightProperty());
        }


        Scene scene = new Scene(root);
        stage.setScene(scene);


        stage.setFullScreen(true);
        stage.setFullScreenExitHint(""); // Quita el mensaje de "Presione ESC para salir"
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}