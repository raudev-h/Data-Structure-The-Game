/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package dominion.view.informacion;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Usuario
 */
public class InformationController implements Initializable {

    @FXML
    private Button backToMenu;
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    
    @FXML
    public void backToMenuAction(){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dominion/view/menu/interfaz.fxml"));
            Scene newScene = new Scene(loader.load());
            
            Stage newStage = new Stage();
            newStage.setTitle("Menu");
            newStage.setScene(newScene);
            newStage.show();
            
            
            Stage menuStage = (Stage) backToMenu.getScene().getWindow();
            menuStage.close();
            
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
}
