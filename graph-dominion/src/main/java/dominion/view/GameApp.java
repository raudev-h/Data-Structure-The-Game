package dominion.view;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class GameApp extends Application {

    @Override
    public void start(Stage stage) {
        // 1. Obtener tamaño de pantalla
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double windowWidth = Math.min(screen.getWidth() * 0.9, 1600);
        double windowHeight = Math.min(screen.getHeight() * 0.9, 900);

        // 2. Crear contenedor principal
        Pane root = new Pane();
        root.setPrefSize(windowWidth, windowHeight);

        // 3. Añadir mapa como Background
        setMapBackground(root, windowWidth, windowHeight);

        // 4. Añadir timer
        Timer timer = new Timer();
        Pane timerPanel = timer.getTimerPanel();
        positionInCorner(timerPanel, windowWidth- 100, windowHeight);
        root.getChildren().add(timerPanel);

        // 5. Configurar ventana
        Scene scene = new Scene(root, windowWidth, windowHeight);
        stage.setTitle("Dominion");
        stage.setScene(scene);
        centerStage(stage, windowWidth, windowHeight);
        stage.show();

        // 6. Iniciar timer
        timer.startTimer();
    }

    private void setMapBackground(Pane pane, double width, double height) {
        try {
            BackgroundImage background = new BackgroundImage(
                    new Image("file:src/main/resources/images/map_background.png"),
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(
                            BackgroundSize.AUTO, BackgroundSize.AUTO,  // Tamaño automático
                            false, false,  // No contener
                            true, true     // Cubrir completamente
                    )
            );

            pane.setBackground(new Background(background));

        } catch (Exception e) {
            // Fallback
            pane.setStyle("-fx-background-color: linear-gradient(to bottom, #1a472a, #2a5c2a);");
        }
    }

    private void positionInCorner(Pane element, double screenWidth, double screenHeight) {
        element.setLayoutX(screenWidth - element.getPrefWidth() - 20);
        element.setLayoutY(20);
    }

    private void centerStage(Stage stage, double width, double height) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setX((screen.getWidth() - width) / 2);
        stage.setY((screen.getHeight() - height) / 2);
    }

    public static void main(String[] args) {
        launch(args);
    }
}