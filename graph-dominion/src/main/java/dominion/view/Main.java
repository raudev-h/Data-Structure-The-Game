package dominion.view;

import dominion.view.GameApp;
import dominion.view.MenuManager;
import javafx.application.Application;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
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