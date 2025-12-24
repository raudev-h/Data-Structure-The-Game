/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package dominion.view.menu;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 *
 * @author Usuario
 */
public class PrimeraInterfaz extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("interfaz.fxml"));

        primaryStage.setTitle("Menu");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

    }

    public static void main(String[] args){
        launch(args);

    }


}

