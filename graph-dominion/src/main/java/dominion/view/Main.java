package dominion.view;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.net.URL;

public class Main extends Application {

    private MediaPlayer backgroundMusic;
    private double currentVolume = 0.2; // Volumen inicial (30%)

    @Override
    public void start(Stage primaryStage) {
        try {
            // Configurar el stage para pantalla completa sin decoraciones
            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint("");
            primaryStage.setFullScreenExitKeyCombination(null);

            // INICIALIZAR MÚSICA DE FONDO CON FADE-OUT
            initializeBackgroundMusic();

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

    /**
     * Método para inicializar la música de fondo con capacidad de fade-out
     */
    private void initializeBackgroundMusic() {
        try {
            // Obtener la URL del archivo de música desde resources
            URL musicUrl = getClass().getResource("/music/background_sound.mpeg");

            if (musicUrl == null) {
                // Si no está en resources, buscar en filesystem
                String[] possiblePaths = {
                        "src/main/resources/music/background_sound.mpeg",
                        "resources/music/background_sound.mpeg",
                        "music/background_sound.mpeg"
                };

                for (String path : possiblePaths) {
                    java.io.File file = new java.io.File(path);
                    if (file.exists()) {
                        musicUrl = file.toURI().toURL();
                        break;
                    }
                }
            }

            if (musicUrl != null) {
                // Crear Media y MediaPlayer
                Media media = new Media(musicUrl.toExternalForm());
                backgroundMusic = new MediaPlayer(media);

                // Configurar propiedades iniciales
                backgroundMusic.setCycleCount(MediaPlayer.INDEFINITE); // Repetir indefinidamente
                backgroundMusic.setVolume(currentVolume); // Volumen inicial

                // Configurar el comportamiento al terminar cada ciclo (para fade-out antes de repetir)
                backgroundMusic.setOnEndOfMedia(() -> {
                    // No hacemos nada aquí porque usaremos setOnRepeat para el fade-out
                });

                // Configurar el comportamiento al repetir (cuando termina y vuelve a empezar)
                backgroundMusic.setOnRepeat(() -> {
                    // Aquí podríamos agregar un fade-out/in si queremos
                });

                // Iniciar la música
                backgroundMusic.play();
                System.out.println("✅ Música de fondo iniciada con volumen: " + currentVolume);

            } else {
                System.err.println("❌ No se encontró el archivo de música");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al cargar la música: " + e.getMessage());
        }
    }


    /**
     * Método para hacer fade-out y luego detener completamente
     * @param durationSeconds Duración del fade-out en segundos
     */
    public void fadeOutAndStop(double durationSeconds) {
        if (backgroundMusic != null && backgroundMusic.getStatus() == MediaPlayer.Status.PLAYING) {
            Timeline fadeOutTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(backgroundMusic.volumeProperty(), backgroundMusic.getVolume())),
                    new KeyFrame(Duration.seconds(durationSeconds), new KeyValue(backgroundMusic.volumeProperty(), 0.0))
            );

            fadeOutTimeline.setOnFinished(event -> {
                backgroundMusic.stop();
                System.out.println("🔇 Música desvanecida y detenida");
            });

            fadeOutTimeline.play();
        }
    }

    @Override
    public void stop() {
        // Hacer fade-out antes de cerrar la aplicación
        if (backgroundMusic != null) {
            fadeOutAndStop(5.0); // 5 segundos de fade-out antes de cerrar
            try {
                Thread.sleep(5000); // Esperar un poco para que complete el fade-out
            } catch (InterruptedException e) {
                // Ignorar
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}