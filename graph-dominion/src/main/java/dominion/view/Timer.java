package dominion.view;

import dominion.core.GameTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class Timer {
    // Atributos
    private GameTimer gameTimer;
    private Label timeLabel;
    private Timeline updateClock;
    private Button startClock;
    private Button pauseClock;
    private VBox timerPanel;

    // Constructor
    public Timer() {
        initializeTimer();
        createTimerPanel();
    }

    private void initializeTimer() {
        gameTimer = new GameTimer();
        timeLabel = new Label("00:00:00");
        timeLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
    }

    private void createTimerPanel() {
        // Botones
        startClock = new Button("▶ Iniciar");
        startClock.setStyle("-fx-font-size: 14px; -fx-padding: 8px 16px; -fx-background-color: #2ecc71; -fx-text-fill: white;");
        startClock.setOnAction(actionEvent -> {
            gameTimer.start();
            setupTimer();
            startClock.setDisable(true);
            pauseClock.setDisable(false);
            startClock.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: #7f8c8d;");
            pauseClock.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        });

        pauseClock = new Button("⏸ Pausar");
        pauseClock.setStyle("-fx-font-size: 14px; -fx-padding: 8px 16px; -fx-background-color: #e74c3c; -fx-text-fill: white;");
        pauseClock.setOnAction(actionEvent -> {
            gameTimer.pause();
            if (updateClock != null) {
                updateClock.stop();
            }
            startClock.setDisable(false);
            pauseClock.setDisable(true);
            startClock.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
            pauseClock.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: #7f8c8d;");
        });
        pauseClock.setDisable(true);
        pauseClock.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: #7f8c8d;");

        // Layout de botones
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(startClock, pauseClock);

        // Panel principal del timer
        timerPanel = new VBox(10);
        timerPanel.setAlignment(Pos.CENTER);
        timerPanel.getChildren().addAll(timeLabel, buttonBox);

        // Estilo del panel (fondo semitransparente)
        timerPanel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.7); " +
                        "-fx-padding: 15px; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #3498db; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 10;"
        );
    }

    private void setupTimer() {
        updateClock = new Timeline(
                new KeyFrame(Duration.seconds(1),
                        event -> updateTimeDisplay())
        );
        updateClock.setCycleCount(Timeline.INDEFINITE);
        updateClock.play();
    }

    private void updateTimeDisplay() {
        timeLabel.setText(gameTimer.getTime());
    }

    // Devuelve solo el panel del timer
    public VBox getTimerPanel() {
        return timerPanel;
    }

    // Métodos para control desde fuera si los necesitas
    public void startTimer() {
        startClock.fire();
    }

    public void pauseTimer() {
        pauseClock.fire();
    }

    public String getCurrentTime() {
        return gameTimer.getTime();
    }

    public long getElapsedSeconds() {
        return gameTimer.getElapsedSeconds();
    }

    public void adjustSize(double scaleFactor) {
        if (scaleFactor < 0.8) {
            // Pantalla pequeña
            timerPanel.setStyle(
                    "-fx-background-color: rgba(0, 0, 0, 0.7); " +
                            "-fx-padding: 6px 10px; " +
                            "-fx-font-size: 12px; " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-color: #3498db; " +
                            "-fx-border-width: 1px; " +
                            "-fx-border-radius: 6;"
            );
        } else if (scaleFactor > 1.2) {

            timerPanel.setStyle(
                    "-fx-background-color: rgba(0, 0, 0, 0.7); " +
                            "-fx-padding: 15px 20px; " +
                            "-fx-font-size: 20px; " +
                            "-fx-background-radius: 12; " +
                            "-fx-border-color: #3498db; " +
                            "-fx-border-width: 2px; " +
                            "-fx-border-radius: 12;"
            );
        }
    }
}