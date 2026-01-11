/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package dominion.view.menu;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class InterfazController implements Initializable {

    @FXML
    private Button playButton;

    @FXML
    private Button exitButton;

    @FXML
    private Button informationButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Inicialización si la necesitas
    }

    @FXML
    private void playButtonAction() {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dominion/view/mapa/mapa.fxml"));
            Scene nuevaScene = new Scene(loader.load());

            // 2) Crear un nuevo Stage y mostrarlo
            Stage nuevaStage = new Stage();
            nuevaStage.setTitle("Juego de Conquista - Nivel 1");
            nuevaStage.setScene(nuevaScene);
            nuevaStage.show();

            // 3) Cerrar el Stage actual (el menú)
            Stage menuStage = (Stage) playButton.getScene().getWindow();
            menuStage.close();

        } catch (Exception e) {
            e.printStackTrace(); // útil para depuración si algo falla
        }
    }
    
    
    @FXML
    private void informationButtonAction(){
        try{

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dominion/view/informacion/Information.fxml"));
        Scene newScene = new Scene(loader.load());
        
        Stage newStage = new Stage();
        newStage.setTitle("Informacion");
        newStage.setScene(newScene);
        newStage.show();
        
        
        Stage menuStage = (Stage) informationButton.getScene().getWindow();
        menuStage.close();
        
        
        
        
        } catch (Exception e) {
            e.printStackTrace(); // útil para depuración si algo falla
        }

        
        
        
    }
    
  
    
    @FXML
    private void exitButtonAction() {
        Stage stage = (Stage) exitButton.getScene().getWindow();
        stage.close();
    }
    
}



