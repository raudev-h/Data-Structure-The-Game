package dominion.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.Stage;

public class menuController {

    private MenuManager menuManager;

    public void setMenuManager(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @FXML
    private void playButtonAction(ActionEvent event) {
        if (menuManager != null) {
            menuManager.startGame();
        } else {
            System.out.println("Botón JUGAR presionado (sin MenuManager)");
        }
    }

    @FXML
    private void informationButtonAction(ActionEvent event) {
        if (menuManager != null) {
            menuManager.showInformation();
        } else {
            System.out.println("Botón INFORMACIÓN presionado (sin MenuManager)");
        }
    }

    @FXML
    private void volverMenu(ActionEvent event) {
        if (menuManager != null) {
            menuManager.showMainMenu();
        } else {
            System.out.println("Botón VOLVER presionado (sin MenuManager)");
        }
    }

    @FXML
    private void exitButtonAction(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}