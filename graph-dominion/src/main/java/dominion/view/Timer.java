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

    // NUEVO: Atributo para controlar el estado manualmente
    private boolean isManuallyPaused = false;

    // NUEVO: Añadir atributo para el listener
    private PauseListener pauseListener;

    // NUEVO: Definir la interfaz interna
    public interface PauseListener {
        void onPause();
        void onResume();
    }

    // NUEVO: Método para establecer el listener
    public void setPauseListener(PauseListener listener) {
        this.pauseListener = listener;
    }

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
        // Botones - CAMBIADO: Botón de pausa visible, inicio oculto
        startClock = new Button("▶");
        startClock.setStyle("-fx-font-size: 14px; -fx-padding: 8px 16px; -fx-background-color: #2ecc71; -fx-text-fill: white;");
        startClock.setOnAction(actionEvent -> {
            // Este botón ahora solo se usa internamente
            if (!isManuallyPaused) {
                gameTimer.start();
                setupTimer();
                updateButtonStates();

                // NUEVO: Notificar reanudación
                if (pauseListener != null) {
                    pauseListener.onResume();
                }
            }
        });

        // Inicialmente no visible
        startClock.setVisible(false);
        startClock.setManaged(false);

        pauseClock = new Button("⏸ Pausar");
        pauseClock.setStyle("-fx-font-size: 14px; -fx-padding: 8px 16px; -fx-background-color: #e74c3c; -fx-text-fill: white;");
        pauseClock.setOnAction(actionEvent -> {
            if (!isManuallyPaused) {
                pauseClock.setDisable(true);
                pauseClock.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: #7f8c8d;");
                isManuallyPaused = true;

                gameTimer.pause();
                if (updateClock != null) {
                    updateClock.stop();
                }

                // NUEVO: Notificar pausa
                if (pauseListener != null) {
                    pauseListener.onPause();
                }
            }
        });

        // Layout de botones - SOLO mostrar el botón de pausa
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(pauseClock);

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

    // NUEVO: Actualizar estados de los botones
    private void updateButtonStates() {
        if (isManuallyPaused) {
            pauseClock.setDisable(true);
            pauseClock.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: #7f8c8d;");
        } else {
            pauseClock.setDisable(false);
            pauseClock.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        }
    }

    // Devuelve solo el panel del timer
    public VBox getTimerPanel() {
        return timerPanel;
    }

    // Métodos para control desde fuera
    public void startTimer() {
        // Iniciar el timer automáticamente
        gameTimer.start();
        setupTimer();
        isManuallyPaused = false;
        updateButtonStates();
    }

    public void pauseTimer() {
        if (!isManuallyPaused) {
            pauseClock.fire();
        }
    }

    // NUEVO: Método para reanudar desde el menú de pausa
    public void resumeFromPauseMenu() {
        isManuallyPaused = false;
        updateButtonStates();

        // Solo reanudar si estaba en marcha
        if (gameTimer != null && !gameTimer.getIsRunning()) {
            gameTimer.start();
        }

        if (updateClock != null) {
            updateClock.play();
        }
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

    // NUEVO: Método para verificar si está pausado
    public boolean isPaused() {
        return isManuallyPaused;
    }
}