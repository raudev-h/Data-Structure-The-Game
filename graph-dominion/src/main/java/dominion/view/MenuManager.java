package dominion.view;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class MenuManager {

    private Stage primaryStage;
    private GameApp gameApp;
    private StackPane menuRoot;
    private MediaPlayer videoPlayer; // Para el video de fondo

    public MenuManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setGameApp(GameApp gameApp) {
        this.gameApp = gameApp;
        // Asegurar que GameApp tenga referencia a este manager
        if (gameApp != null) {
            gameApp.setMenuManager(this);
            System.out.println("✅ Conexión bidireccional establecida");
        }
    }

    public void showMainMenu() {
        try {
            // Obtener la escena actual si existe
            Scene currentScene = primaryStage.getScene();
            Parent currentRoot = (currentScene != null) ? currentScene.getRoot() : null;

            // Cargar el FXML del menú
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/menu.fxml"));
            Parent newRoot = loader.load();

            // Obtener el controlador
            menuController controller = loader.getController();
            controller.setMenuManager(this);

            // Detener cualquier video anterior
            stopBackgroundVideo();

            // Cargar VIDEO de fondo en lugar de imagen
            try {
                // Buscar el ImageView en el FXML para reemplazarlo
                ImageView bgImageView = (ImageView) newRoot.lookup("#backgroundImageView");

                if (bgImageView != null) {
                    // Crear contenedor para el video
                    StackPane videoContainer = new StackPane();
                    videoContainer.setStyle("-fx-background-color: black;");

                    // Cargar y configurar el video
                    MediaView mediaView = loadBackgroundVideo();
                    if (mediaView != null) {
                        // Reemplazar el ImageView con el MediaView
                        StackPane parent = (StackPane) bgImageView.getParent();
                        int index = parent.getChildren().indexOf(bgImageView);
                        parent.getChildren().set(index, videoContainer);
                        videoContainer.getChildren().add(mediaView);

                        // Ajustar tamaño del video
                        mediaView.fitWidthProperty().bind(primaryStage.widthProperty());
                        mediaView.fitHeightProperty().bind(primaryStage.heightProperty());
                        mediaView.setPreserveRatio(false);

                        System.out.println("✅ Video de fondo cargado: menu.mp4");
                    } else {
                        // Si no se encuentra el video, usar imagen de respaldo
                        loadFallbackImage(newRoot);
                    }
                } else {
                    System.err.println("⚠️ No se encontró #backgroundImageView en el FXML");
                    loadFallbackImage(newRoot);
                }
            } catch (Exception e) {
                System.err.println("Error al cargar video de fondo: " + e.getMessage());
                loadFallbackImage(newRoot);
            }

            // Guardar referencia
            menuRoot = (StackPane) newRoot;

            // Manejar la transición
            if (currentScene != null && currentRoot != null) {
                // Transición suave desde la pantalla actual
                performSmoothTransition(currentRoot, newRoot);
            } else {
                // Primera vez, crear nueva escena
                Scene scene = new Scene(newRoot);
                primaryStage.setScene(scene);
                primaryStage.setFullScreen(true);
                primaryStage.setFullScreenExitHint("");
                primaryStage.show();
            }

        } catch (IOException e) {
            System.err.println("Error al cargar el menú FXML: " + e.getMessage());
            showFallbackMenu();
        }
    }

    /**
     * Carga el video de fondo del menú
     */
    private MediaView loadBackgroundVideo() {
        try {
            // Intentar varias ubicaciones posibles para el video
            String[] possiblePaths = {
                    "/videos/menu.mp4",
                    "/video/menu.mp4",
                    "/media/menu.mp4",
                    "/images/menu.mp4", // En la misma carpeta que la imagen
                    "src/main/resources/videos/menu.mp4",
                    "src/main/resources/video/menu.mp4",
                    "src/main/resources/media/menu.mp4",
                    "src/main/resources/images/menu.mp4",
                    "resources/videos/menu.mp4",
                    "resources/video/menu.mp4",
                    "resources/media/menu.mp4",
                    "resources/images/menu.mp4",
                    "videos/menu.mp4",
                    "video/menu.mp4",
                    "media/menu.mp4",
                    "images/menu.mp4"
            };

            Media media = null;

            // Primero intentar desde classpath (recursos)
            for (String path : possiblePaths) {
                if (path.startsWith("/")) {
                    URL videoUrl = getClass().getResource(path);
                    if (videoUrl != null) {
                        System.out.println("✅ Encontrado video en classpath: " + path);
                        media = new Media(videoUrl.toExternalForm());
                        break;
                    }
                }
            }

            // Si no se encontró en classpath, buscar en filesystem
            if (media == null) {
                for (String path : possiblePaths) {
                    if (!path.startsWith("/")) {
                        File videoFile = new File(path);
                        if (videoFile.exists()) {
                            System.out.println("✅ Encontrado video en filesystem: " + videoFile.getAbsolutePath());
                            media = new Media(videoFile.toURI().toString());
                            break;
                        }
                    }
                }
            }

            if (media != null) {
                videoPlayer = new MediaPlayer(media);

                // Configurar el video
                videoPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Repetir indefinidamente
                videoPlayer.setMute(true); // Silenciar si no quieres audio
                videoPlayer.setVolume(0.0); // Volumen 0

                MediaView mediaView = new MediaView(videoPlayer);
                mediaView.setSmooth(true);

                // Reproducir el video
                videoPlayer.play();

                return mediaView;
            } else {
                System.err.println("❌ No se encontró el archivo menu.mp4 en ninguna ubicación");
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error al cargar video: " + e.getMessage());
            return null;
        }
    }

    /**
     * Método de respaldo si no se encuentra el video
     */
    private void loadFallbackImage(Parent root) {
        try {
            URL imageUrl = getClass().getResource("/images/fondo.png");
            if (imageUrl != null) {
                Image backgroundImage = new Image(imageUrl.toString());
                ImageView bg = (ImageView) root.lookup("#backgroundImageView");
                if (bg != null) {
                    bg.setImage(backgroundImage);
                    bg.fitWidthProperty().bind(primaryStage.widthProperty());
                    bg.fitHeightProperty().bind(primaryStage.heightProperty());
                    bg.setPreserveRatio(false);
                    System.out.println("⚠️  Usando imagen de respaldo: fondo.png");
                }
            } else {
                System.err.println("⚠️ No se encontró: /images/fondo.png");
                applyDefaultBackground(root);
            }
        } catch (Exception e) {
            System.err.println("Error al cargar imagen de respaldo: " + e.getMessage());
            applyDefaultBackground(root);
        }
    }

    private void applyDefaultBackground(Parent root) {
        if (root instanceof Region) {
            ((Region) root).setStyle("-fx-background-color: linear-gradient(to bottom, #1a472a, #2a5c2a);");
        }
    }

    /**
     * Detiene el video de fondo cuando no se necesita
     */
    private void stopBackgroundVideo() {
        if (videoPlayer != null) {
            videoPlayer.stop();
            videoPlayer.dispose();
            videoPlayer = null;
            System.out.println("⏹️  Video de fondo detenido");
        }
    }

    /**
     * Menú de respaldo si falla la carga del FXML
     */
    private void showFallbackMenu() {
        StackPane root = new StackPane();

        // Intentar cargar video de fondo
        MediaView videoView = loadBackgroundVideo();
        if (videoView != null) {
            videoView.fitWidthProperty().bind(primaryStage.widthProperty());
            videoView.fitHeightProperty().bind(primaryStage.heightProperty());
            videoView.setPreserveRatio(false);
            root.getChildren().add(videoView);
        } else {
            // Usar imagen de respaldo si no hay video
            try {
                Image backgroundImage = new Image(getClass().getResourceAsStream("/images/fondo.png"));
                BackgroundImage bgImg = new BackgroundImage(
                        backgroundImage,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
                );
                root.setBackground(new Background(bgImg));
            } catch (Exception e) {
                root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a472a, #2a5c2a);");
            }
        }

        // Crear menú manualmente
        VBox menuContainer = new VBox(30);
        menuContainer.setAlignment(Pos.CENTER);
        menuContainer.setMaxWidth(400);
        menuContainer.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.85);" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 40;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0.5, 0, 5);"
        );

        Label title = new Label("DOMINION");
        title.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Button playButton = createMenuButton("JUGAR");
        Button infoButton = createMenuButton("INFORMACIÓN");
        Button exitButton = createMenuButton("SALIR");

        playButton.setOnAction(e -> startGame());
        infoButton.setOnAction(e -> showInformation());
        exitButton.setOnAction(e -> {
            stopBackgroundVideo();
            primaryStage.close();
        });

        menuContainer.getChildren().addAll(title, playButton, infoButton, exitButton);
        root.getChildren().add(menuContainer);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitHint("");

        primaryStage.show();
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(250);
        button.setPrefHeight(60);
        button.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9);" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0.5, 0, 2);" +
                        "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() +
                    "-fx-background-color: linear-gradient(to bottom, #2980b9, #1c5a80);" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0.5, 0, 3);"
            );
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-font-size: 20px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-color: linear-gradient(to bottom, #3498db, #2980b9);" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 10;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0.5, 0, 2);" +
                            "-fx-cursor: hand;"
            );
        });

        return button;
    }

    public void startGame() {
        // Detener el video del menú
        stopBackgroundVideo();

        if (gameApp == null) {
            System.err.println("GameApp no está configurada");
            gameApp = new GameApp();
        }

        // Asegurar que GameApp tenga referencia a este manager
        gameApp.setMenuManager(this);

        // Animación de transición
        if (menuRoot != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), menuRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                // **LIMPIAR EL GAMEAPP ANTERIOR ANTES DE INICIAR UNO NUEVO**
                if (gameApp != null) {
                    try {
                        // Detener cualquier animación o timer activo
                        gameApp.cleanupBeforeRestart();
                    } catch (Exception ex) {
                        System.err.println("Error limpiando GameApp anterior: " + ex.getMessage());
                    }
                }

                // **REINICIAR EL GAMEAPP COMPLETAMENTE**
                gameApp = new GameApp();
                gameApp.setMenuManager(this);

                // Iniciar el juego - el stage YA debería estar en pantalla completa
                gameApp.start(primaryStage);

                // Solo asegurar que el stage esté visible
                Platform.runLater(() -> {
                    if (!primaryStage.isShowing()) {
                        primaryStage.show();
                    }
                });
            });

            fadeOut.play();
        } else {
            // **LIMPIAR EL GAMEAPP ANTERIOR ANTES DE INICIAR UNO NUEVO**
            if (gameApp != null) {
                try {
                    gameApp.cleanupBeforeRestart();
                } catch (Exception ex) {
                    System.err.println("Error limpiando GameApp anterior: " + ex.getMessage());
                }
            }

            // **REINICIAR EL GAMEAPP COMPLETAMENTE**
            gameApp = new GameApp();
            gameApp.setMenuManager(this);
            gameApp.start(primaryStage);

            Platform.runLater(() -> {
                primaryStage.setFullScreen(true);
                primaryStage.setFullScreenExitHint("");
            });
        }
    }

    public void showInformation() {
        try {
            // Obtener la escena y root actual
            Scene currentScene = primaryStage.getScene();
            if (currentScene == null) {
                currentScene = new Scene(new StackPane());
                primaryStage.setScene(currentScene);
            }
            Parent currentRoot = currentScene.getRoot();

            // Cargar el nuevo contenido
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/information.fxml"));
            Parent newRoot = loader.load();

            // Obtener el controlador
            menuController controller = loader.getController();
            controller.setMenuManager(this);

            // Intentar cargar imagen de fondo
            try {
                URL imageUrl = getClass().getResource("/images/informationPhoto.png");
                if (imageUrl != null) {
                    Image backgroundImage = new Image(imageUrl.toString());
                    ImageView bg = (ImageView) newRoot.lookup("#bgInfo");
                    if (bg != null) {
                        bg.setImage(backgroundImage);
                        bg.fitWidthProperty().bind(primaryStage.widthProperty());
                        bg.fitHeightProperty().bind(primaryStage.heightProperty());
                        bg.setPreserveRatio(false);
                    }
                } else {
                    System.err.println("⚠️ No se encontró: /images/informationPhoto.png");
                    if (newRoot instanceof Region) {
                        ((Region) newRoot).setStyle("-fx-background-color: linear-gradient(to bottom, #1a472a, #2a5c2a);");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al cargar imagen de información: " + e.getMessage());
                if (newRoot instanceof Region) {
                    ((Region) newRoot).setStyle("-fx-background-color: linear-gradient(to bottom, #1a472a, #2a5c2a);");
                }
            }

            // Transición suave
            performSmoothTransition(currentRoot, newRoot);

        } catch (IOException e) {
            System.err.println("Error al cargar información: " + e.getMessage());
            showInformationFallback();
        }
    }

    private void performSmoothTransition(Parent fromRoot, Parent toRoot) {
        Scene scene = primaryStage.getScene();

        // Si no hay root anterior
        if (fromRoot == null) {
            scene.setRoot(toRoot);
            primaryStage.setFullScreen(true);
            return;
        }

        // Configurar opacidad inicial del nuevo root
        toRoot.setOpacity(0.0);

        // Configurar tamaño si es Region
        if (toRoot instanceof Region) {
            Region region = (Region) toRoot;
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }

        // Primero fade out del root actual
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), fromRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            // Cambiar al nuevo root
            scene.setRoot(toRoot);

            // Animar fade in del nuevo root
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), toRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            fadeIn.setOnFinished(e2 -> {
                primaryStage.setFullScreen(true);
                System.out.println("✅ Transición completada - Menú principal visible");
            });

            fadeIn.play();
        });

        fadeOut.play();
    }

    private void showInformationFallback() {
        StackPane newRoot = new StackPane();

        // Fondo
        try {
            URL imageUrl = getClass().getResource("/images/informationPhoto.png");
            if (imageUrl != null) {
                Image backgroundImage = new Image(imageUrl.toString());
                BackgroundImage bgImg = new BackgroundImage(
                        backgroundImage,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
                );
                newRoot.setBackground(new Background(bgImg));
            } else {
                throw new Exception("Imagen no encontrada");
            }
        } catch (Exception e) {
            newRoot.setStyle("-fx-background-color: linear-gradient(to bottom, #1a472a, #2a5c2a);");
        }

        // Contenedor de información
        VBox infoContainer = new VBox(20);
        infoContainer.setAlignment(Pos.CENTER);
        infoContainer.setMaxWidth(600);
        infoContainer.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.9);" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 40;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0.5, 0, 5);"
        );

        Label title = new Label("INFORMACIÓN DEL JUEGO");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER_LEFT);

        String[] infoLines = {
                "DOMINION - Estrategia en Tiempo Real",
                "",
                "🎮 CONTROLES:",
                "- Click izquierdo: Seleccionar unidades",
                "- Drag: Seleccionar múltiples unidades",
                "- Click derecho en recurso: Enviar trabajadores",
                "- Click en edificios: Abrir menús",
                "",
                "🏗️ CONSTRUCCIÓN:",
                "- TownHall: Centro de operaciones",
                "- Casas: Aumentan población máxima",
                "- Cuarteles: Entrenan caballeros",
                "",
                "⚔️ CONQUISTA:",
                "- Entrena caballeros en los cuarteles",
                "- Conquista territorios en el mapa",
                "- Gana recursos de territorios conquistados",
                "",
                "⛏️ RECURSOS:",
                "- Oro: Para construir y entrenar",
                "- Madera: Para construir edificios",
                "- Mineros y Leñadores recolectan recursos"
        };

        for (String line : infoLines) {
            Label label = new Label(line);
            if (line.isEmpty()) {
                label.setPrefHeight(10);
            } else if (line.startsWith("🎮") || line.startsWith("🏗️") || line.startsWith("⚔️") || line.startsWith("⛏️")) {
                label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3498db;");
            } else if (line.startsWith("-")) {
                label.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-padding: 0 0 0 20;");
            } else if (line.equals("DOMINION - Estrategia en Tiempo Real")) {
                label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            } else {
                label.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");
            }
            content.getChildren().add(label);
        }

        Button backButton = createMenuButton("VOLVER AL MENÚ");
        backButton.setPrefWidth(200);
        backButton.setOnAction(e -> showMainMenu());

        infoContainer.getChildren().addAll(title, content, backButton);
        newRoot.getChildren().add(infoContainer);

        // Configurar tamaño
        newRoot.prefWidthProperty().bind(primaryStage.widthProperty());
        newRoot.prefHeightProperty().bind(primaryStage.heightProperty());

        // Obtener root actual
        Scene scene = primaryStage.getScene();
        if (scene != null && scene.getRoot() != null) {
            performSmoothTransition(scene.getRoot(), newRoot);
        } else {
            primaryStage.setScene(new Scene(newRoot));
            primaryStage.setFullScreen(true);
            primaryStage.show();
        }
    }

    /**
     * Vuelve al menú principal desde el juego
     */
    public void returnToMenu() {
        System.out.println("🔄 Volviendo al menú principal...");
        showMainMenu();
    }
}