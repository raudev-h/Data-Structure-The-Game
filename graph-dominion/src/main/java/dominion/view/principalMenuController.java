package com.juego.conquista;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.io.IOException;
public class principalMenuController {


    @FXML
    private void playButtonAction(ActionEvent event) {


    }


    @FXML
    private void informationButtonAction(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/juego/conquista/information.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // Esto ajusta la imagen de la pantalla INFO
        ImageView bg = (ImageView) root.lookup("#bgInfo");
        if (bg != null) {
            bg.fitWidthProperty().bind(stage.widthProperty());
            bg.fitHeightProperty().bind(stage.heightProperty());
        }

        stage.getScene().setRoot(root);
    }

    @FXML
    private void volverMenu(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/juego/conquista/principalMenu.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // Esto re-ajusta la imagen de la pantalla PRINCIPAL
        ImageView bg = (ImageView) root.lookup("#backgroundImageView");
        if (bg != null) {
            bg.fitWidthProperty().bind(stage.widthProperty());
            bg.fitHeightProperty().bind(stage.heightProperty());
        }

        stage.getScene().setRoot(root);
    }


    @FXML
    private void exitButtonAction(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }



}