package dominion.view;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Configurar el stage para pantalla completa sin decoraciones
            primaryStage.initStyle(StageStyle.UNDECORATED); // Elimina la barra de título
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint(""); // Sin mensaje de salida
            primaryStage.setFullScreenExitKeyCombination(null); // Desactiva tecla de salida

            // 1. Crear MenuManager primero
            MenuManager menuManager = new MenuManager(primaryStage);

            // 2. Crear GameApp
            GameApp gameApp = new GameApp();

            // 3. ESTABLECER LA CONEXIÓN BIDIRECCIONAL
            gameApp.setMenuManager(menuManager);
            menuManager.setGameApp(gameApp);

            // 4. Guardar referencia en el stage
            primaryStage.setUserData(menuManager);

            // 5. Mostrar menú
            menuManager.showMainMenu();

        } catch (Exception e) {
            System.err.println("Error al iniciar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}