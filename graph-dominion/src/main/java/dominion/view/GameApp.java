package dominion.view;

import dominion.core.GameControler;
import dominion.core.GameMap;
import dominion.model.buildings.TownHall;
import dominion.model.players.Player;
import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameApp extends Application {

    private Pane root;
    private double windowWidth;
    private double windowHeight;
    private Popup townHallPopup;
    private boolean isBuildingMode = false;
    private boolean wasDragSelect = false;


    // ==================== SELECCIÓN TIPO WINDOWS (MARQUEE) ====================
    private Rectangle selectionRect;
    private boolean isSelecting = false;
    private double selectStartX;
    private double selectStartY;
    private final List<ImageView> selectedUnitViews = new ArrayList<>();


    // ==================== CARGA DE IMÁGENES (classpath primero, file: como fallback) ====================
// ==================== Image loader (classpath only, simple) ====================
    private Image loadImage(String imageName) {
        // Normaliza nombre
        String name = imageName == null ? "" : imageName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("imageName is empty");
        }
        if (!name.contains(".")) {
            name = name + ".png";
        }

        // Variantes comunes: con/sin espacio antes de ( y con/sin espacios
        String[] candidates = new String[]{
                name,
                name.replace(" (", "("),
                name.replace("(", " ("),
                name.replace(" ", ""),
        };

        for (String c : candidates) {
            var url = getClass().getResource("/images/" + c);
            if (url != null) {
                return new Image(url.toExternalForm());
            }
        }

        throw new IllegalStateException(
                "No se encontró la imagen en /images/: " + imageName +
                        " (verifica que exista en src/main/resources/images y que 'resources' sea Resources Root)"
        );
    }

    private ImageView buildingGhost;
    private String currentBuildingType = "";
    private List<ImageView> placedBuildings = new ArrayList<>();
    private int width = 100;
    private int height = 100;
    private GameControler gameControler;
    private Player actualPlayer;
    private GameMap gameMap;
    private Territory territory1;
    private Timer gameTimer;
    private StackPane pauseOverlay;
    private boolean isGamePaused = false;
    private Popup barracksPopup;    // Para el menú del cuartel
    private List<ImageView> createdKnights = new ArrayList<>(); // Para rastrear caballeros creados


    @Override
    public void start(Stage stage) {
        // Configurar Conexion con Backend
        gameControler = new GameControler();
        actualPlayer = gameControler.createPlayer("Player1", dominion.core.Color.BLUE);
        gameMap = gameControler.createGameMap();
        territory1 = new Territory();

        // 1. Obtener tamaño de pantalla
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        windowWidth = Math.min(screen.getWidth() * 0.9, 1600);
        windowHeight = Math.min(screen.getHeight() * 0.9, 900);

        // 2. Crear contenedor principal
        root = new Pane();
        root.setPrefSize(windowWidth, windowHeight);

        // 3. Añadir mapa como Background
        setMapBackground(root, windowWidth, windowHeight);

        // 4. Configurar el sistema de pausa (ANTES de otros elementos)
        setupPauseSystem();

        // 5. Añadir TownHall INTERACTIVO
        addInteractiveTownHall();

        // 6. Inicializar el ImageView fantasma
        buildingGhost = new ImageView();
        buildingGhost.setVisible(false);
        buildingGhost.setMouseTransparent(true);
        root.getChildren().add(buildingGhost);

        // 7. Configurar ventana
        Scene scene = new Scene(root, windowWidth, windowHeight);
        // ==================== INPUT (SELECCIÓN + MOVER UNIDADES) ====================

        setupUnitSelectionAndMovement(scene);
        setupBuildingListeners(scene);

        // 8. Añadir árboles
        addOrganicForest();

        // NUEVO: 8.1 Añadir minas distribuidas
        addMinesToMap();

        // 9. Crear unidades
        createUnitNextToTownHall("leñador", "leñador.pgn", 50);
        createUnitNextToTownHall("minero", "minero.png", 50);
        createUnitNextToTownHall("leñador", "leñador.png", 50);

        // 10. AÑADIR PANEL SUPERIOR CON TIMER INTEGRADO
        Pane topPanel = createTopPanel();
        root.getChildren().add(topPanel);

        // 11. Configurar el stage
        stage.setTitle("Dominion");
        stage.setScene(scene);
        centerStage(stage, windowWidth, windowHeight);
        stage.show();

        // 12. Configurar el listener del timer para manejar pausa
        if (gameTimer != null) {
            gameTimer.setPauseListener(new Timer.PauseListener() {
                @Override
                public void onPause() {
                    showPauseMenu();
                }

                @Override
                public void onResume() {
                }
            });
        }

        // 13. POSICIONAR EL PANEL AUTOMÁTICAMENTE después de que todo esté renderizado
        Platform.runLater(() -> {
            positionTopPanel();
            updateResourceDisplay();
            // Iniciar el timer automáticamente
            if (gameTimer != null) {
                gameTimer.startTimer();
            }
        });

        Canvas canvas = new Canvas(800, 600);
        Pane root = new Pane(canvas);
        stage.setScene(scene);
        stage.show();
    }
    // ==================== SISTEMA DE PAUSA ====================

    /**
     * Configura el sistema de pausa (solo inicializa variables)
     */
    private void setupPauseSystem() {
        // Solo inicializa las variables, el overlay se crea dinámicamente
        isGamePaused = false;
        // No crear el overlay aquí, se creará dinámicamente cuando se necesite
    }

    /**
     * Crea el menú de pausa compacto con estilo TownHall (50% opacidad)
     */
    private VBox createPauseMenu() {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(25, 30, 25, 30));
        panel.setMaxWidth(Region.USE_PREF_SIZE);
        panel.setMaxHeight(Region.USE_PREF_SIZE);

        // MISMO estilo EXACTO que el TownHall pero con 50% opacidad
        panel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.50); " + // 50% opacidad igual que TownHall
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0.5, 0, 3);"
        );

        // Título
        Label title = new Label("Juego en Pausa");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Separador elegante
        Region separator = new Region();
        separator.setPrefHeight(2);
        separator.setPrefWidth(180);
        separator.setStyle("-fx-background-color: linear-gradient(to right, transparent, #d4af37, transparent);");

        // Contenedor de botones
        VBox buttonContainer = new VBox(10);
        buttonContainer.setAlignment(Pos.CENTER);

        // Botón Reanudar
        Button resumeButton = createPauseButton("▶ Reanudar");
        resumeButton.setOnAction(e -> {
            hidePauseMenu();
            if (gameTimer != null) {
                // NUEVO: Usar el método de reanudación del timer
                gameTimer.resumeFromPauseMenu();
            }
        });

        // Botón Salir al Menú
        Button exitButton = createPauseButton("🚪 Salir al Menú");
        exitButton.setOnAction(e -> {
            System.out.println("Saliendo al menú principal...");
            Stage stage = (Stage) root.getScene().getWindow();
            stage.close();
        });

        buttonContainer.getChildren().addAll(resumeButton, exitButton);
        panel.getChildren().addAll(title, separator, buttonContainer);

        return panel;
    }

    /**
     * Crea un botón para el menú de pausa con el MISMO estilo que TownHall (50% opacidad)
     */
    private Button createPauseButton(String text) {
        HBox buttonContent = new HBox(8);
        buttonContent.setAlignment(Pos.CENTER);
        buttonContent.setPadding(new Insets(8, 20, 8, 20));

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        buttonContent.getChildren().add(textLabel);

        Button button = new Button();
        button.setGraphic(buttonContent);
        button.setPrefWidth(200);
        button.setPrefHeight(45);

        // ESTILO BASE con 50% opacidad igual que TownHall
        String baseStyle =
                "-fx-background-color: rgba(255, 255, 255, 0.50); " + // 50% opacidad
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #2c3e50;";

        // Determinar color del borde según el botón (haciéndolo final)
        final String borderColor = text.contains("Salir") ? "#e74c3c" : "#2ecc71";

        // Aplicar el color de borde específico
        button.setStyle(baseStyle +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 2;");

        // Determinar color de sombra (también final)
        final String shadowColor = text.contains("Salir") ?
                "rgba(231, 76, 60, 0.4)" : "rgba(46, 204, 113, 0.4)";

        // EFECTO HOVER IDÉNTICO a los botones del TownHall
        button.setOnMouseEntered(e -> {
            String hoverStyle =
                    "-fx-background-color: rgba(236, 240, 241, 0.50); " + // 50% opacidad en hover
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: " + borderColor + ";" +
                            "-fx-border-width: 2.5; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, " + shadowColor + ", 8, 0.5, 0, 2);";

            button.setStyle(hoverStyle);
            button.setScaleX(1.02);
            button.setScaleY(1.02);
        });

        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle +
                    "-fx-border-color: " + borderColor + ";" +
                    "-fx-border-width: 2;");
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        // Efecto al presionar
        button.setOnMousePressed(e -> {
            button.setStyle(baseStyle +
                    "-fx-border-color: " + borderColor + ";" +
                    "-fx-border-width: 3; " +
                    "-fx-background-color: rgba(220, 220, 220, 0.50);"); // 50% opacidad
        });

        button.setOnMouseReleased(e -> {
            button.setStyle(baseStyle +
                    "-fx-border-color: " + borderColor + ";" +
                    "-fx-border-width: 2;");
        });

        return button;
    }

    /**
     * Muestra el menú de pausa con efecto de anochecer
     */
    private void showPauseMenu() {
        if (isGamePaused || pauseOverlay != null) return;

        isGamePaused = true;

        if (gameTimer != null) {
            gameTimer.pauseTimer(); // Esto desactivará el botón automáticamente
        }

        // Crear overlay oscuro que cubra TODA la pantalla
        pauseOverlay = new StackPane();

        // Usar fondo negro con 85% opacidad para efecto anochecer
        pauseOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");

        // IMPORTANTE: Asegurar que cubra toda el área visible
        pauseOverlay.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        pauseOverlay.setPrefSize(root.getWidth(), root.getHeight());
        pauseOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Vincular tamaño al root para que se ajuste automáticamente
        pauseOverlay.prefWidthProperty().bind(root.widthProperty());
        pauseOverlay.prefHeightProperty().bind(root.heightProperty());

        pauseOverlay.setOpacity(0); // Comienza transparente para la animación

        // Crear panel de pausa
        VBox pauseMenu = createPauseMenu();
        pauseMenu.setOpacity(0); // Comienza transparente para la animación
        pauseMenu.setScaleX(0.8);
        pauseMenu.setScaleY(0.8);

        pauseOverlay.getChildren().add(pauseMenu);
        StackPane.setAlignment(pauseMenu, Pos.CENTER);

        // Asegurar que el overlay esté al frente de TODO
        root.getChildren().add(pauseOverlay);
        pauseOverlay.toFront();

        // Forzar layout para asegurar que cubre toda el área
        pauseOverlay.layout();

        // Deshabilitar interacción con el juego
        disableGameInteractions(true);

        // Animación suave de entrada
        FadeTransition overlayFade = new FadeTransition(Duration.millis(500), pauseOverlay);
        overlayFade.setToValue(1.0);

        FadeTransition menuFade = new FadeTransition(Duration.millis(400), pauseMenu);
        menuFade.setToValue(1.0);
        menuFade.setDelay(Duration.millis(100));

        ScaleTransition menuScale = new ScaleTransition(Duration.millis(400), pauseMenu);
        menuScale.setToX(1.0);
        menuScale.setToY(1.0);
        menuScale.setDelay(Duration.millis(100));
        menuScale.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        javafx.animation.ParallelTransition parallel = new javafx.animation.ParallelTransition(
                overlayFade, menuFade, menuScale
        );
        parallel.play();

        System.out.println("⏸ Juego en pausa - Mostrando menú de pausa");
        System.out.println("📏 Tamaño overlay: " + root.getWidth() + "x" + root.getHeight());
    }

    /**
     * Oculta el menú de pausa con animación suave
     */
    private void hidePauseMenu() {
        if (!isGamePaused || pauseOverlay == null) return;

        // Obtener el menú para animarlo
        VBox pauseMenu = null;
        for (Node node : pauseOverlay.getChildren()) {
            if (node instanceof VBox) {
                pauseMenu = (VBox) node;
                break;
            }
        }

        // Animación suave de salida
        if (pauseMenu != null) {
            FadeTransition menuFade = new FadeTransition(Duration.millis(300), pauseMenu);
            menuFade.setToValue(0);

            ScaleTransition menuScale = new ScaleTransition(Duration.millis(300), pauseMenu);
            menuScale.setToX(0.8);
            menuScale.setToY(0.8);

            FadeTransition overlayFade = new FadeTransition(Duration.millis(400), pauseOverlay);
            overlayFade.setToValue(0);
            overlayFade.setDelay(Duration.millis(100));

            overlayFade.setOnFinished(e -> {
                root.getChildren().remove(pauseOverlay);
                pauseOverlay = null;
                isGamePaused = false;
                disableGameInteractions(false);

                // NUEVO: No necesitamos reanudar aquí porque ya se hizo con el botón
                System.out.println("▶ Juego reanudado");
            });

            javafx.animation.ParallelTransition parallel = new javafx.animation.ParallelTransition(
                    menuFade, menuScale, overlayFade
            );
            parallel.play();
        } else {
            // Si no hay menú, simplemente remover
            root.getChildren().remove(pauseOverlay);
            pauseOverlay = null;
            isGamePaused = false;
            disableGameInteractions(false);
            System.out.println("▶ Juego reanudado");
        }
    }

    /**
     * Habilita/deshabilita la interacción con elementos del juego
     */
    private void disableGameInteractions(boolean disable) {
        if (disable) {
            // Deshabilitar TODOS los elementos del root excepto el overlay de pausa
            for (int i = 0; i < root.getChildren().size(); i++) {
                Node node = root.getChildren().get(i);
                if (node != pauseOverlay && node != buildingGhost) {
                    node.setMouseTransparent(true);
                    node.setFocusTraversable(false);
                }
            }

            // Asegurar que el overlay de pausa sea interactivo
            if (pauseOverlay != null) {
                pauseOverlay.setMouseTransparent(false);
                pauseOverlay.setFocusTraversable(true);
            }

            // Deshabilitar modo construcción si está activo
            if (isBuildingMode) {
                cancelBuildingMode();
            }

            // Deshabilitar eventos del mouse en la escena
            if (root.getScene() != null) {
                root.getScene().setOnMouseMoved(null);
                root.getScene().setOnMouseClicked(null);
                root.getScene().setOnMousePressed(null);
                root.setCursor(javafx.scene.Cursor.DEFAULT);
            }

            // Asegurar que el overlay esté al frente
            if (pauseOverlay != null) {
                pauseOverlay.toFront();
            }
        } else {
            // Rehabilitar todos los elementos
            for (Node node : root.getChildren()) {
                node.setMouseTransparent(false);
                node.setFocusTraversable(true);
            }

            // Rehabilitar eventos del mouse
            if (root.getScene() != null) {
                setupBuildingListeners(root.getScene());
            }
        }
    }

    // ==================== PANEL SUPERIOR CON TIMER INTEGRADO ====================

    /**
     * Crea un panel superior con recursos y timer integrado
     */
    private Pane createTopPanel() {
        // Panel principal horizontal
        HBox topPanel = new HBox(15);
        topPanel.setPadding(new Insets(10, 20, 10, 20));
        topPanel.setAlignment(Pos.CENTER);

        // MISMO estilo que el TownHall
        topPanel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.50); " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0.5, 0, 2);"
        );

        // ========== MADERA ==========
        HBox woodSection = createResourceSection("\uD83C\uDFE0", "Madera",
                territory1 != null && territory1.getTownHall() != null ?
                        String.valueOf(territory1.getTownHall().getStoredResources().getAmount(ResourceType.WOOD)) : "0");

        // ========== ORO ==========
        HBox goldSection = createResourceSection("💰", "Oro",
                territory1 != null && territory1.getTownHall() != null ?
                        String.valueOf(territory1.getTownHall().getStoredResources().getAmount(ResourceType.GOLD)) : "0");

        // ========== TIMER INTEGRADO ==========
        gameTimer = new Timer();
        VBox timerPanel = gameTimer.getTimerPanel();

        // APLICAR ESTILOS DEL TOWNHALL AL TIMER
        applyTownHallStyleToTimer(timerPanel);

        // Añadir elementos en orden: Madera - Oro - Timer
        topPanel.getChildren().addAll(woodSection, goldSection, timerPanel);

        // Forzar que el panel se ajuste a su contenido
        topPanel.setMaxWidth(Region.USE_PREF_SIZE);
        topPanel.setMinWidth(Region.USE_PREF_SIZE);

        // Contenedor para posicionar
        StackPane container = new StackPane(topPanel);

        return container;
    }

    /**
     * Aplica los estilos del TownHall al panel del timer
     */
    private void applyTownHallStyleToTimer(VBox timerPanel) {
        // Cambiar el estilo oscuro por el estilo del TownHall
        timerPanel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.50); " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0.5, 0, 2); " +
                        "-fx-padding: 10 15;"
        );

        // Buscar y modificar los elementos del timer
        for (Node node : timerPanel.getChildren()) {
            if (node instanceof Label) {
                Label label = (Label) node;
                if (label.getText().matches("\\d{2}:\\d{2}:\\d{2}")) {
                    label.setStyle(
                            "-fx-font-size: 20px; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-text-fill: #2c3e50;"
                    );
                }
            } else if (node instanceof HBox) {
                HBox buttonBox = (HBox) node;
                // Modificar los botones del timer
                for (Node buttonNode : buttonBox.getChildren()) {
                    if (buttonNode instanceof Button) {
                        Button button = (Button) buttonNode;
                        applyTownHallStyleToButton(button);

                        // Asegurar que los botones funcionen incluso cuando el juego está en pausa
                        button.setMouseTransparent(false);
                    }
                }
            }
        }
    }

    /**
     * Aplica el estilo del TownHall a un botón
     */
    private void applyTownHallStyleToButton(Button button) {
        String originalText = button.getText();

        // Estilo base del TownHall para botones
        button.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.5); " +
                        "-fx-background-radius: 6; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 6; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 6 12;"
        );

        // Determinar color según el tipo de botón
        // ELIMINADO: La condición para "Iniciar" o "▶"
        if (originalText.contains("Pausar") || originalText.contains("⏸")) {
            button.setStyle(button.getStyle() +
                    "-fx-background-color: rgba(231, 76, 60, 0.7); " + // Rojo
                    "-fx-border-color: #c0392b;"
            );
        } else if (originalText.contains("🔄")) {
            button.setStyle(button.getStyle() +
                    "-fx-background-color: rgba(52, 152, 219, 0.7); " + // Azul
                    "-fx-border-color: #2980b9;"
            );
        }

        // Efecto hover
        button.setOnMouseEntered(e -> {
            String currentStyle = button.getStyle();
            // ELIMINADO: La condición para "Iniciar" o "▶"
            if (originalText.contains("Pausar") || originalText.contains("⏸")) {
                button.setStyle(currentStyle +
                        "-fx-effect: dropshadow(gaussian, rgba(231, 76, 60, 0.5), 5, 0.5, 0, 1);"
                );
            } else if (originalText.contains("🔄")) {
                button.setStyle(currentStyle +
                        "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.5), 5, 0.5, 0, 1);"
                );
            }
        });

        button.setOnMouseExited(e -> {
            String currentStyle = button.getStyle();
            // Remover el efecto de sombra
            button.setStyle(currentStyle.replace(
                    "-fx-effect: dropshadow(gaussian, rgba(.*), 5, 0.5, 0, 1);",
                    "-fx-effect: null;"
            ));
        });
    }

    /**
     * Crea una sección de recurso compacta
     */
    private HBox createResourceSection(String icon, String resourceName, String amount) {
        HBox section = new HBox(8);
        section.setAlignment(Pos.CENTER);
        section.setPadding(new Insets(0, 10, 0, 0));

        // Icono
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");

        // Contenedor vertical
        VBox textContainer = new VBox(1);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        // Nombre del recurso
        Label nameLabel = new Label(resourceName);
        nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");

        // Cantidad
        Label amountLabel = new Label(amount);
        amountLabel.setId(resourceName.toLowerCase() + "_amount");
        amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        textContainer.getChildren().addAll(nameLabel, amountLabel);
        section.getChildren().addAll(iconLabel, textContainer);

        return section;
    }

    /**
     * Actualiza los recursos en el panel superior
     */
    private void updateResourceDisplay() {
        if (territory1 != null && territory1.getTownHall() != null) {
            int wood = territory1.getTownHall().getStoredResources().getAmount(ResourceType.WOOD);
            int gold = territory1.getTownHall().getStoredResources().getAmount(ResourceType.GOLD);

            Label woodLabel = (Label) root.lookup("#madera_amount");
            Label goldLabel = (Label) root.lookup("#oro_amount");

            if (woodLabel != null) {
                woodLabel.setText(String.valueOf(wood));
            }
            if (goldLabel != null) {
                goldLabel.setText(String.valueOf(gold));
            }
        }
    }

    /**
     * Método para posicionar el panel superior automáticamente
     */
    private void positionTopPanel() {
        for (Node node : root.getChildren()) {
            if (node instanceof StackPane) {
                StackPane stackPane = (StackPane) node;
                if (!stackPane.getChildren().isEmpty()) {
                    Node child = stackPane.getChildren().get(0);
                    if (child instanceof HBox) {
                        HBox topPanel = (HBox) child;

                        // Forzar cálculo de dimensiones
                        topPanel.applyCss();
                        topPanel.layout();

                        double panelWidth = topPanel.getWidth();
                        double panelHeight = topPanel.getHeight();

                        // Posicionar en el centro superior
                        node.setLayoutX((windowWidth - panelWidth) / 2);
                        node.setLayoutY(15);

                        // Asegurar que esté al frente (pero detrás del overlay de pausa)
                        node.toFront();

                        // Asegurar que el overlay de pausa esté siempre más al frente
                        if (pauseOverlay != null) {
                            pauseOverlay.toFront();
                        }

                        System.out.println("📍 Panel superior posicionado: " + panelWidth + "x" + panelHeight);
                        break;
                    }
                }
            }
        }
    }

    // ==================== TOWNHALL Y CONSTRUCCIÓN ====================

    private void addInteractiveTownHall() {
        try {
            Image townHallImage = loadImage("TownHall1.png");
            ImageView townHallView = new ImageView(townHallImage);

            double townHallSize = 170;
            townHallView.setFitWidth(townHallSize);
            townHallView.setFitHeight(townHallSize);
            townHallView.setPreserveRatio(true);

            double townHallX = windowWidth * 0.3 - townHallSize / 2;
            double townHallY = windowHeight * 0.4 - townHallSize / 2;
            townHallView.setX(townHallX + 100);
            townHallView.setY(townHallY + 100);

            placedBuildings.add(townHallView);

            TownHall townHall1 = new TownHall("1", territory1, 100, 5);
            territory1.setTownHall(townHall1);
            territory1.getTownHall().getStoredResources().addResource(ResourceType.WOOD, 600);
            territory1.getTownHall().getStoredResources().addResource(ResourceType.GOLD, 500);

            DropShadow glow = new DropShadow();
            glow.setColor(Color.rgb(255, 215, 0, 0.7));
            glow.setRadius(15);
            townHallView.setEffect(glow);

            townHallView.setOnMouseClicked(event -> {
                System.out.println("🏰 TownHall clickeado - Abriendo menú...");
                showTownHallMenu(townHallX + townHallSize / 3, townHallY);
            });

            townHallView.setOnMouseEntered(e -> {
                townHallView.setCursor(javafx.scene.Cursor.HAND);
                townHallView.setScaleX(1.1);
                townHallView.setScaleY(1.1);
            });

            townHallView.setOnMouseExited(e -> {
                townHallView.setCursor(javafx.scene.Cursor.DEFAULT);
                townHallView.setScaleX(1.0);
                townHallView.setScaleY(1.0);
            });

            root.getChildren().addAll(townHallView);
            System.out.println("✅ TownHall interactivo añadido");

        } catch (Exception e) {
            System.err.println("❌ Error al cargar TownHall: " + e.getMessage());
            addPlaceholderTownHall();
        }
    }

    private void showTownHallMenu(double centerX, double centerY) {
        if (townHallPopup != null) {
            townHallPopup.hide();
        }

        townHallPopup = new Popup();
        townHallPopup.setAutoFix(true);
        townHallPopup.setAutoHide(true);
        townHallPopup.setHideOnEscape(true);

        VBox mainPanel = createCenteredPanel();
        StackPane container = new StackPane(mainPanel);

        double panelWidth = 100;
        double panelHeight = 200;
        double panelX = (windowWidth - panelWidth) / 2;
        double panelY = (windowHeight - panelHeight) / 2;

        townHallPopup.getContent().add(container);
        townHallPopup.show(root.getScene().getWindow(), panelX, panelY);

        animateCenterEntrance(mainPanel);
    }

    private VBox createCenteredPanel() {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20, 20, 20, 20));
        panel.setPrefSize(250, 320);

        panel.setBackground(new Background(new BackgroundFill(
                Color.rgb(255, 255, 255, 0.50),
                new CornerRadii(12),
                Insets.EMPTY
        )));

        panel.setBorder(new Border(new BorderStroke(
                Color.rgb(212, 175, 55, 0.8),
                BorderStrokeStyle.SOLID,
                new CornerRadii(12),
                new BorderWidths(2)
        )));

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(15);
        shadow.setSpread(0.1);
        panel.setEffect(shadow);

        Label title = new Label("TownHall");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        title.setPadding(new Insets(0, 0, 10, 0));

        Region separator = new Region();
        separator.setPrefHeight(2);
        separator.setStyle("-fx-background-color: #d4af37; -fx-background-radius: 1;");

        VBox buttonContainer = new VBox(8);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(10, 0, 0, 0));

        Button houseButton = createTextButton("🏠", "Crear Casa", "60 Madera");
        Button barracksButton = createTextButton("⚔", "Crear Cuartel", "100 Madera");
        Button minerButton = createTextButton("⛏", "Crear Minero", "75 Oro, 25 Madera");
        Button lumberjackButton = createTextButton("", "Crear Leñador", "50 Oro, 50 Madera");

        houseButton.setOnAction(e -> {
            System.out.println("✅ Creando Casa...");
            townHallPopup.hide();
            enterBuildingMode("casa");
            showConstructionAnimation("Casa");
        });

        barracksButton.setOnAction(e -> {
            System.out.println("✅ Creando Cuartel...");
            townHallPopup.hide();
            enterBuildingMode("Cuartel");
            showConstructionAnimation("Cuartel");
        });

        minerButton.setOnAction(e -> {
            System.out.println("✅ Creando Minero...");
            townHallPopup.hide();
            createUnitNextToTownHall("minero", "minero.png", 50);
        });

        lumberjackButton.setOnAction(e -> {
            System.out.println("✅ Creando Leñador...");
            townHallPopup.hide();
            createUnitNextToTownHall("leñador", "Leñador.png", 50);
        });

        buttonContainer.getChildren().addAll(houseButton, barracksButton, minerButton, lumberjackButton);
        panel.getChildren().addAll(title, separator, buttonContainer);

        return panel;
    }

    // ==================== CONSTRUCCIÓN DE EDIFICIOS ====================

    private void enterBuildingMode(String buildingType) {
        this.isBuildingMode = true;
        this.currentBuildingType = buildingType;
        boolean construir = true;

        if (currentBuildingType.equalsIgnoreCase("Casa"))
            construir = territory1.getTownHall().canCreateHouse();
        else if (currentBuildingType.equalsIgnoreCase("Cuartel"))
            construir = territory1.getTownHall().canCreateMilitaryBase();

        if (construir) {
            try {
                Image buildingImage = loadImage(buildingType + ".png");

                buildingGhost.setImage(buildingImage);
                if (buildingType.equalsIgnoreCase("Cuartel")) {
                    width = 170;
                    height = 170;
                } else {
                    width = 100;
                    height = 100;
                }

                buildingGhost.setFitWidth(width);
                buildingGhost.setFitHeight(height);
                buildingGhost.setPreserveRatio(true);
                buildingGhost.setOpacity(0.6);
                buildingGhost.setVisible(true);

                root.setCursor(javafx.scene.Cursor.CROSSHAIR);

                System.out.println("✅ Modo construcción activado para: " + buildingType);

                root.getScene().setOnKeyPressed(event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                        cancelBuildingMode();
                    }
                });

            } catch (Exception ex) {
                System.err.println("❌ Error al cargar imagen del edificio: " + ex.getMessage());
            }
        } else {
            showMaterialWarning();
            this.isBuildingMode = false;
            this.currentBuildingType = null;
            buildingGhost.setVisible(false);
            root.setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }

    private void showMaterialWarning() {
        Stage warningStage = new Stage();
        warningStage.initModality(Modality.APPLICATION_MODAL);
        warningStage.initStyle(StageStyle.TRANSPARENT);
        warningStage.setTitle("Materiales insuficientes");

        VBox warningPanel = new VBox(15);
        warningPanel.setPadding(new Insets(25, 30, 25, 30));
        warningPanel.setAlignment(Pos.CENTER);
        warningPanel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.50); " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.5, 0, 2);"
        );

        Label warningIcon = new Label("⚠");
        warningIcon.setStyle("-fx-font-size: 36px; -fx-padding: 0 0 5 0;");

        VBox messageContainer = new VBox(5);
        messageContainer.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Materiales insuficientes");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label detailLabel = new Label("No tienes los recursos necesarios\npara construir este edificio");
        detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #000000; -fx-text-alignment: center;");
        detailLabel.setWrapText(true);

        messageContainer.getChildren().addAll(titleLabel, detailLabel);

        Button okButton = new Button("Entendido");
        okButton.setPrefWidth(150);
        okButton.setPrefHeight(38);
        okButton.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.5); " +
                        "-fx-background-radius: 6; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 6; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold;"
        );

        okButton.setOnMouseEntered(e -> {
            okButton.setStyle(
                    "-fx-background-color: rgba(236, 240, 241, 0.5); " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-color: #3498db; " +
                            "-fx-border-width: 1.5; " +
                            "-fx-border-radius: 6; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: #2c3e50; " +
                            "-fx-font-size: 12px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.3), 5, 0.5, 0, 1);"
            );
        });

        okButton.setOnMouseExited(e -> {
            okButton.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.5); " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-color: #dcdde1; " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 6; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: #2c3e50; " +
                            "-fx-font-size: 12px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-effect: null;"
            );
        });

        okButton.setOnAction(e -> {
            warningStage.close();
            cancelBuildingMode();
        });

        warningPanel.getChildren().addAll(warningIcon, messageContainer, okButton);

        StackPane rootPane = new StackPane(warningPanel);
        rootPane.setStyle("-fx-background-color: transparent;");
        rootPane.setAlignment(Pos.CENTER);

        Scene warningScene = new Scene(rootPane, 300, 250);
        warningScene.setFill(Color.TRANSPARENT);

        warningStage.initOwner(root.getScene().getWindow());
        warningStage.setScene(warningScene);
        warningStage.setResizable(false);
        warningStage.showAndWait();
    }

    private void cancelBuildingMode() {
        isBuildingMode = false;
        currentBuildingType = "";
        buildingGhost.setVisible(false);
        root.setCursor(javafx.scene.Cursor.DEFAULT);
        System.out.println("❌ Modo construcción cancelado");
    }

    private void placeBuilding(double x, double y) {
        if (!isBuildingMode) return;

        double buildingWidth = width;
        double buildingHeight = height;
        double posX = x - buildingWidth / 2;
        double posY = y - buildingHeight / 2;

        // Verificar colisión con márgenes reducidos
        if (checkCollisionWithReducedMargin(posX, posY, buildingWidth, buildingHeight, 3)) {
            System.out.println("❌ No se puede construir aquí - Colisión detectada");
            showCollisionFeedback();
            return;
        }

        if (posX < 0 || posY < 0 ||
                posX + buildingWidth > windowWidth ||
                posY + buildingHeight > windowHeight) {
            System.out.println("❌ No se puede construir fuera del mapa");
            showOutOfBoundsFeedback();
            return;
        }

        boolean creado = false;

        if (currentBuildingType.equalsIgnoreCase("Casa")) {
            creado = territory1.getTownHall().createHouse();
        } else if (currentBuildingType.equalsIgnoreCase("Cuartel")) {
            creado = territory1.getTownHall().createMilitaryBase();
        }

        if (!creado) {
            System.out.println("❌ Error: No se pudo crear el edificio en el backend");
            cancelBuildingMode();
            return;
        }

        // ACTUALIZAR RECURSOS DESPUÉS DE CONSTRUIR
        updateResourceDisplay();

        try {
            Image buildingImage = loadImage(currentBuildingType + ".png");

            ImageView buildingView = new ImageView(buildingImage);
            buildingView.setUserData("obstacle");
            buildingView.setFitWidth(buildingWidth);
            buildingView.setFitHeight(buildingHeight);
            buildingView.setPreserveRatio(true);
            buildingView.setX(posX);
            buildingView.setY(posY);

            // Marcar como cuartel si es el caso
            if (currentBuildingType.equalsIgnoreCase("Cuartel")) {
                buildingView.setId("Cuartel_" + System.currentTimeMillis());
                System.out.println("⚔️ Cuartel creado y marcado con ID: " + buildingView.getId());
            }

            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.5));
            shadow.setRadius(10);
            shadow.setSpread(0.1);
            buildingView.setEffect(shadow);

            FadeTransition fade = new FadeTransition(Duration.millis(500), buildingView);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);

            ScaleTransition scale = new ScaleTransition(Duration.millis(500), buildingView);
            scale.setFromX(0.5);
            scale.setFromY(0.5);
            scale.setToX(1.0);
            scale.setToY(1.0);

            javafx.animation.ParallelTransition parallel =
                    new javafx.animation.ParallelTransition(fade, scale);
            parallel.play();

            root.getChildren().add(buildingView);
            placedBuildings.add(buildingView);
            makeBuildingInteractive(buildingView, currentBuildingType);

            System.out.println("✅ " + currentBuildingType + " construido en: (" + (int) posX + ", " + (int) posY + ")");
            cancelBuildingMode();

        } catch (Exception e) {
            System.err.println("❌ Error al colocar edificio visualmente: " + e.getMessage());
            cancelBuildingMode();
        }
    }


    // ==================== SELECCIÓN Y MOVIMIENTO DE UNIDADES ====================

    private void setupUnitSelectionAndMovement(Scene scene) {

        if (selectionRect == null) {
            selectionRect = new Rectangle();
            selectionRect.setVisible(false);
            selectionRect.setManaged(false);
            selectionRect.setMouseTransparent(true);
            selectionRect.setFill(Color.color(0.2, 0.6, 1.0, 0.18));
            selectionRect.setStroke(Color.color(0.2, 0.6, 1.0, 0.9));
            selectionRect.getStrokeDashArray().addAll(8.0, 6.0);
            root.getChildren().add(selectionRect);
        }

        scene.setOnMousePressed(e -> {
            if (isGamePaused || isBuildingMode) return;
            if (!e.isPrimaryButtonDown()) return;

            isSelecting = true;
            selectStartX = e.getX();
            selectStartY = e.getY();

            // NO limpies aquí: si es click para mover, perderías la selección
            selectionRect.setX(selectStartX);
            selectionRect.setY(selectStartY);
            selectionRect.setWidth(0);
            selectionRect.setHeight(0);
            selectionRect.setVisible(true);
            selectionRect.toFront();
        });

        scene.setOnMouseDragged(e -> {
            if (isGamePaused || isBuildingMode) return;
            if (!isSelecting) return;

            double x = e.getX();
            double y = e.getY();

            double minX = Math.min(selectStartX, x);
            double minY = Math.min(selectStartY, y);
            double w = Math.abs(x - selectStartX);
            double h = Math.abs(y - selectStartY);

            selectionRect.setX(minX);
            selectionRect.setY(minY);
            selectionRect.setWidth(w);
            selectionRect.setHeight(h);
        });

        scene.setOnMouseReleased(e -> {
            if (isGamePaused || isBuildingMode) return;
            if (!isSelecting) return;

            double w = selectionRect.getWidth();
            double h = selectionRect.getHeight();

            selectionRect.setVisible(false);
            isSelecting = false;

            // Drag real -> selección por rectángulo
            if (w > 6 && h > 6) {
                if (!e.isShiftDown()) clearSelectedUnitViews();
                selectUnitsInsideSelectionRect(true); // ya limpiamos arriba si hacía falta
                return;
            }

            // Click normal -> seleccionar unidad o mover selección
            handleUnitClickOrMove(e.getX(), e.getY(), e.isShiftDown());
        });
    }
    private List<Node> getObstacleNodes() {
        List<Node> obs = new ArrayList<>();

        // Revisa root
        for (Node n : root.getChildren()) {
            if (n instanceof ImageView iv && isObstacle(iv)) obs.add(iv);
        }

        return obs;
    }
    private boolean isObstacle(ImageView iv) {
        // Recomendado: marca tus obstáculos con userData="obstacle" al crearlos
        Object ud = iv.getUserData();
        if (ud instanceof String s && s.equalsIgnoreCase("obstacle")) return true;

        // Fallback por id (ajusta según tus ids reales)
        String id = iv.getId();
        if (id == null) return false;
        id = id.toLowerCase();

        return id.contains("arbol")
                || id.contains("mina")
                || id.contains("townhall")
                || id.contains("casa")
                || id.contains("cuartel")
                || id.contains("building")
                || id.contains("tree");
    }
    private double[] adjustTargetToAvoidObstacles(double tx, double ty, double unitW, double unitH) {

        List<Node> obstacles = getObstacleNodes();

        Bounds unitBounds = new BoundingBox(tx - unitW / 2, ty - unitH / 2, unitW, unitH);

        for (int tries = 0; tries < 40; tries++) {
            Bounds hitB = null;

            for (Node ob : obstacles) {
                Bounds obB = ob.getBoundsInParent(); // ✅ parent coords
                if (obB.intersects(unitBounds)) {
                    hitB = obB;
                    break;
                }
            }

            if (hitB == null) break;

            double left  = unitBounds.getMaxX() - hitB.getMinX();
            double right = hitB.getMaxX() - unitBounds.getMinX();
            double up    = unitBounds.getMaxY() - hitB.getMinY();
            double down  = hitB.getMaxY() - unitBounds.getMinY();

            double min = Math.min(Math.min(left, right), Math.min(up, down));
            double pad = 2;

            if (min == left)  tx -= (left + pad);
            else if (min == right) tx += (right + pad);
            else if (min == up)    ty -= (up + pad);
            else                   ty += (down + pad);

            unitBounds = new BoundingBox(tx - unitW / 2, ty - unitH / 2, unitW, unitH);
        }

        return new double[]{tx, ty};
    }
    private void handleUnitClickOrMove(double x, double y, boolean shiftDown) {
        ImageView clicked = getUnitViewAt(x, y);

        if (clicked != null) {
            if (!shiftDown && selectedUnitViews.contains(clicked)) {
                removeFromSelection(clicked);   // 👈 lo creamos abajo si no existe
                return;
            }

            // Si NO hay shift y no estaba seleccionada -> seleccionar solo esa
            if (!shiftDown) clearSelectedUnitViews();

            // Con shift: toggle (si está, se quita; si no, se agrega)
            if (shiftDown && selectedUnitViews.contains(clicked)) {
                removeFromSelection(clicked);
            } else {
                addToSelection(clicked);
            }
            return;
        }

        // Click en suelo: mover lo seleccionado
        moveSelectedUnitsTo(x, y);
        clearSelectedUnitViews();
    }
    
    private void selectUnitsInsideSelectionRect(boolean shiftIgnored) {

        // Bounds del rectángulo en coordenadas de SCENE
        Bounds selScene = selectionRect.localToScene(selectionRect.getBoundsInLocal());

        for (ImageView iv : getAllWorkerUnitViews()) {

            // Bounds de la unidad en coordenadas de SCENE
            Bounds unitScene = iv.localToScene(iv.getBoundsInLocal());

            // Windows-style “dentro del cuadro”: usa CONTAINS, no intersects
            if (containsFully(selScene, unitScene)) {
                addToSelection(iv);
            }
        }
    }

        private List<ImageView> getAllWorkerUnitViews() {
            List<ImageView> units = new ArrayList<>();
            for (var node : root.getChildren()) {
                if (node instanceof ImageView iv && isWorkerUnit(iv)) {
                    units.add(iv);
                }
            }
            return units;
        }

    private boolean containsFully(Bounds outer, Bounds inner) {
        return outer.contains(inner.getMinX(), inner.getMinY())
                && outer.contains(inner.getMaxX(), inner.getMaxY());
    }


    private void moveSelectedUnitsTo(double destX, double destY) {

        // Agrupar workers por tipo
        List<ImageView> miners = new ArrayList<>();
        List<ImageView> woodcutters = new ArrayList<>();

        for (ImageView iv : selectedUnitViews) {
            if (!isWorkerUnit(iv)) continue;

            Object ud = iv.getUserData();
            String type = (ud instanceof String s) ? s.toLowerCase() : "";

            if (type.contains("minero")) miners.add(iv);
            else woodcutters.add(iv); // leñador/lenador
        }

        double spacing = 30;  // distancia dentro del grupo
        double padding = 20;  // distancia ENTRE grupos (anti-superposición)

        // Calcula “bloques” (ancho/alto) de cada grupo
        double[] minerSize = formationSize(miners.size(), spacing);
        double[] woodSize  = formationSize(woodcutters.size(), spacing);

        // Si solo hay un grupo, lo pones centrado en dest
        if (!miners.isEmpty() && woodcutters.isEmpty()) {
            moveGroupInCompactFormation(miners, destX, destY, spacing);
            clearSelectedUnitViews();
            return;
        }
        if (miners.isEmpty() && !woodcutters.isEmpty()) {
            moveGroupInCompactFormation(woodcutters, destX, destY, spacing);
            clearSelectedUnitViews();
            return;
        }

        // Si hay ambos: los ponemos lado a lado
        double totalWidth = minerSize[0] + padding + woodSize[0];

        // Centro de cada bloque
        double minerCenterX = destX - totalWidth / 2.0 + minerSize[0] / 2.0;
        double woodCenterX  = minerCenterX + minerSize[0] / 2.0 + padding + woodSize[0] / 2.0;

        double minerCenterY = destY;
        double woodCenterY  = destY;

        moveGroupInCompactFormation(miners, minerCenterX, minerCenterY, spacing);
        moveGroupInCompactFormation(woodcutters, woodCenterX, woodCenterY, spacing);

        // Quitar selección después de ordenar movimiento
        clearSelectedUnitViews();
    }
    // Devuelve {ancho, alto} aproximados del bloque de formación
    private double[] formationSize(int n, double spacing) {
        if (n <= 0) return new double[]{0, 0};

        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);

        double width = (cols - 1) * spacing;
        double height = (rows - 1) * spacing;

        // si n=1 => width/height 0, igual sirve
        return new double[]{width, height};
    }


    private void moveGroupInCompactFormation(List<ImageView> units, double cx, double cy, double spacing) {
        int n = units.size();
        if (n == 0) return;

        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);

        double startX = cx - (cols - 1) * spacing / 2.0;
        double startY = cy - (rows - 1) * spacing / 2.0;

        for (int i = 0; i < n; i++) {
            int col = i % cols;
            int row = i / cols;

            double tx = startX + col * spacing;
            double ty = startY + row * spacing;

            animateMove(units.get(i), tx, ty);
        }
    }


    private void moveGroupInCompactFormation(List<ImageView> units, double cx, double cy) {

        int n = units.size();
        if (n == 0) return;

        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);

        double spacing = 30; //
        double startX = cx - (cols - 1) * spacing / 2;
        double startY = cy - (rows - 1) * spacing / 2;

        for (int i = 0; i < n; i++) {
            int col = i % cols;
            int row = i / cols;

            double tx = startX + col * spacing;
            double ty = startY + row * spacing;

            ImageView u = units.get(i);
            double w = u.getBoundsInLocal().getWidth();
            double h = u.getBoundsInLocal().getHeight();
            double[] fixed = adjustTargetToAvoidObstacles(tx, ty, w, h);
            animateMove(u, fixed[0], fixed[1]);
        }
    }


    private void animateMove(ImageView unit, double targetX, double targetY) {
        double startX = unit.getX() + unit.getTranslateX();
        double startY = unit.getY() + unit.getTranslateY();

        double dx = targetX - startX;
        double dy = targetY - startY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        double speed = 160.0;
        double seconds = Math.max(0.15, dist / speed);

        TranslateTransition tt = new TranslateTransition(Duration.seconds(seconds), unit);
        tt.setByX(dx);
        tt.setByY(dy);

        tt.setOnFinished(ev -> {
            unit.setX(targetX);
            unit.setY(targetY);
            unit.setTranslateX(0);
            unit.setTranslateY(0);
        });

        tt.play();
    }
    private void pushApartTargets(List<double[]> targets) {
        double minDist = 30; // ajusta 24-40
        double minDistSq = minDist * minDist;

        for (int iter = 0; iter < 12; iter++) {
            boolean changed = false;

            for (int i = 0; i < targets.size(); i++) {
                for (int j = i + 1; j < targets.size(); j++) {
                    double[] a = targets.get(i);
                    double[] b = targets.get(j);

                    double dx = b[0] - a[0];
                    double dy = b[1] - a[1];
                    double d2 = dx * dx + dy * dy;

                    if (d2 < 0.0001) {
                        // si están en el mismo punto, separa un poquito
                        b[0] += 1;
                        b[1] += 1;
                        changed = true;
                        continue;
                    }

                    if (d2 < minDistSq) {
                        double d = Math.sqrt(d2);
                        double push = (minDist - d) / 2.0;

                        double nx = dx / d;
                        double ny = dy / d;

                        a[0] -= nx * push;
                        a[1] -= ny * push;
                        b[0] += nx * push;
                        b[1] += ny * push;

                        changed = true;
                    }
                }
            }

            if (!changed) break;
        }
    }

    private ImageView getUnitViewAt(double x, double y) {
        for (int i = root.getChildren().size() - 1; i >= 0; i--) {
            if (!(root.getChildren().get(i) instanceof ImageView iv)) continue;

            if (!isWorkerUnit(iv)) continue;

            Bounds b = iv.getBoundsInParent();
            if (b.contains(x, y)) {
                return iv;
            }
        }
        return null;
    }

    private boolean isWorkerUnit(ImageView iv) {
        Object ud = iv.getUserData();
        if (ud instanceof String s) {
            return s.equals("minero") || s.equals("leñador");
        }
        // Fallback: por id
        String id = iv.getId();
        return id != null && (id.startsWith("minero_") || id.startsWith("leñador_"));
    }

    private void addToSelection(ImageView unit) {
        if (selectedUnitViews.contains(unit)) return;

        selectedUnitViews.add(unit);
        applySelectionStyle(unit, true);
    }

    private void removeFromSelection(ImageView iv) {
        selectedUnitViews.remove(iv);
        applySelectionStyle(iv, false); //
    }


    private void clearSelectedUnitViews() {
        for (ImageView u : selectedUnitViews) {
            applySelectionStyle(u, false);
        }
        selectedUnitViews.clear();
    }

    private void applySelectionStyle(ImageView unit, boolean selected) {
        if (selected) {
            DropShadow glow = new DropShadow();
            glow.setRadius(25);
            glow.setSpread(0.25);
            glow.setColor(Color.color(1.0, 0.92, 0.2, 0.95));
            unit.setEffect(glow);
            unit.setScaleX(1.08);
            unit.setScaleY(1.08);
        } else {
            Object base = unit.getProperties().get("baseEffect");
            if (base instanceof Effect effect) {
                unit.setEffect(effect);
            }
            unit.setScaleX(1.0);
            unit.setScaleY(1.0);
        }
    }


    // ==================== UNIDADES ====================

    private void createUnitNextToTownHall(String unitType, String imageName, double unitSize) {
        try {
            double townHallX = windowWidth * 0.3 - 85 + 100;
            double townHallY = windowHeight * 0.4 - 85 + 100;
            double townHallSize = 170;
            double spacing = 5;

            Position validPosition = findPositionForUnit(townHallX, townHallY, townHallSize, unitSize, spacing, unitType);

            if (validPosition == null) {
                System.out.println("❌ No hay espacio disponible para el " + unitType);
                return;
            }

            createUnitAtPosition(unitType, imageName, validPosition.x, validPosition.y, unitSize);

        } catch (Exception e) {
            System.err.println("❌ Error al crear " + unitType + ": " + e.getMessage());
        }
    }

    // ==================== ÁRBOLES ====================

    /**
     * Añade árboles de forma orgánica pero bien distribuida
     */
    private void addOrganicForest() {
        try {
            Image treeImage = loadImage("Arbol.png");
            double treeSize = 65;

            System.out.println("🌲 Creando bosques en esquinas...");
            createForestCluster(treeImage, treeSize, 70, 70, 6);
            createForestCluster(treeImage, treeSize, windowWidth - 170, 70, 6);
            createForestCluster(treeImage, treeSize, 70, windowHeight - 170, 6);
            createForestCluster(treeImage, treeSize, windowWidth - 170, windowHeight - 170, 6);

            System.out.println("🌳 Creando línea de árboles superior...");
            createWavyTreeLine(treeImage, treeSize, 30, 0, windowWidth - 40, 45, 20, 12);

            System.out.println("🌳 Creando línea de árboles inferior...");
            createWavyTreeLine(treeImage, treeSize, 40, windowHeight - 65, windowWidth - 40, windowHeight - 65, 15, 12);

            System.out.println("🌿 Creando grupos laterales...");
            createForestCluster(treeImage, treeSize, 60, windowHeight/2 - 50, 20);
            createForestCluster(treeImage, treeSize, windowWidth - 60, windowHeight/2 - 50, 4);

            System.out.println("✅ Bosque orgánico creado con éxito!");

        } catch (Exception e) {
            System.err.println("❌ Error al crear bosque: " + e.getMessage());
            createOrganicPlaceholderForest();
        }
    }

    /**
     * Crea un grupo denso de árboles
     */
    private void createForestCluster(Image treeImage, double baseSize, double centerX, double centerY, int treeCount) {
        for (int i = 0; i < treeCount; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double radius = 30 + Math.random() * 25;

            double x = centerX + Math.cos(angle) * radius;
            double y = centerY + Math.sin(angle) * radius;
            double size = baseSize * (0.85 + Math.random() * 0.3);

            double townHallX = windowWidth * 0.3 + 100;
            double townHallY = windowHeight * 0.4 + 100;
            double distanceToTownHall = Math.sqrt(Math.pow(x - townHallX, 2) + Math.pow(y - townHallY, 2));

            if (distanceToTownHall < 160) {
                angle = Math.atan2(y - townHallY, x - townHallX);
                x = townHallX + Math.cos(angle) * 170;
                y = townHallY + Math.sin(angle) * 170;
            }

            x = Math.max(25, Math.min(x, windowWidth - size - 25));
            y = Math.max(25, Math.min(y, windowHeight - size - 25));

            createTree(treeImage, size, x, y, "Bosque_" + (int)centerX + "_" + (int)centerY + "_" + i);
        }
    }

    /**
     * Crea una línea de árboles ondulada con buen espaciado
     */
    private void createWavyTreeLine(Image treeImage, double baseSize,
                                    double startX, double startY,
                                    double endX, double endY,
                                    int treeCount, double waveHeight) {
        double step = (endX - startX) / (treeCount - 1);

        for (int i = 0; i < treeCount; i++) {
            double x = startX + i * step;
            double wave = Math.sin(i * 0.6) * waveHeight;
            double y = startY + wave;
            double sizeVariation = 0.8 + Math.random() * 0.4;
            double size = baseSize * sizeVariation;

            x += (Math.random() - 0.5) * 15;

            double townHallX = windowWidth * 0.3 + 100;
            double townHallY = windowHeight * 0.4 + 100;
            double distance = Math.sqrt(Math.pow(x - townHallX, 2) + Math.pow(y - townHallY, 2));

            if (distance > 150) {
                createTree(treeImage, size, x, y, "Linea_" + i);
            }
        }
    }

    /**
     * Crea un árbol individual
     */
    private void createTree(Image treeImage, double size, double x, double y, String treeId) {
        ImageView treeView = new ImageView(treeImage);

        treeView.setFitWidth(size);
        treeView.setFitHeight(size);
        treeView.setPreserveRatio(true);
        treeView.setX(x);
        treeView.setY(y);
        treeView.setId("Arbol_" + treeId);

        treeView.setRotate((Math.random() - 0.5) * 8);

        DropShadow treeShadow = new DropShadow();
        treeShadow.setColor(Color.rgb(0, 0, 0, 0.4));
        treeShadow.setRadius(4);
        treeShadow.setOffsetY(2);
        treeView.setEffect(treeShadow);

        makeTreeInteractive(treeView, "Árbol " + treeId.replace("_", " "));
        root.getChildren().add(treeView);
    }

    /**
     * Hace un árbol interactivo
     */
    private void makeTreeInteractive(ImageView treeView, String treeName) {
        treeView.setOnMouseClicked(event -> {
            System.out.println("🌳 " + treeName + " clickeado");

            FadeTransition flash = new FadeTransition(Duration.millis(150), treeView);
            flash.setFromValue(1.0);
            flash.setToValue(0.7);
            flash.setAutoReverse(true);
            flash.setCycleCount(2);
            flash.play();
        });

        treeView.setOnMouseEntered(e -> {
            treeView.setCursor(javafx.scene.Cursor.HAND);
            treeView.setScaleX(1.05);
            treeView.setScaleY(1.05);

            DropShadow highlight = new DropShadow();
            highlight.setColor(Color.rgb(255, 220, 100, 0.6));
            highlight.setRadius(8);
            treeView.setEffect(highlight);
        });

        treeView.setOnMouseExited(e -> {
            treeView.setCursor(javafx.scene.Cursor.DEFAULT);
            treeView.setScaleX(1.0);
            treeView.setScaleY(1.0);

            DropShadow normalShadow = new DropShadow();
            normalShadow.setColor(Color.rgb(0, 0, 0, 0.4));
            normalShadow.setRadius(4);
            normalShadow.setOffsetY(2);
            treeView.setEffect(normalShadow);
        });
    }

    /**
     * Versión placeholder si no carga la imagen
     */
    private void createOrganicPlaceholderForest() {
        System.out.println("🌿 Creando bosque placeholder...");
        double baseSize = 55;

        createSimpleTreeCluster(70, 70, 6);
        createSimpleTreeCluster(windowWidth - 170, 70, 6);
        createSimpleTreeCluster(70, windowHeight - 170, 6);
        createSimpleTreeCluster(windowWidth - 170, windowHeight - 170, 6);

        for (int i = 0; i < 10; i++) {
            double x = 45 + i * 75;
            double wave = Math.sin(i * 0.6) * 10;
            createSimpleTree(x, 45 + wave, baseSize);
        }

        for (int i = 0; i < 10; i++) {
            double x = 45 + i * 75;
            double wave = Math.sin(i * 0.5 + 2) * 10;
            createSimpleTree(x, windowHeight - 75 + wave, baseSize);
        }

        createSimpleTreeCluster(60, windowHeight/2 - 50, 4);
        createSimpleTreeCluster(windowWidth - 60, windowHeight/2 - 50, 4);

        System.out.println("✅ Bosque placeholder creado");
    }

    /**
     * Crea un grupo de árboles placeholder
     */
    private void createSimpleTreeCluster(double centerX, double centerY, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double radius = 20 + Math.random() * 20;
            double x = centerX + Math.cos(angle) * radius;
            double y = centerY + Math.sin(angle) * radius;
            createSimpleTree(x, y, 45 + Math.random() * 25);
        }
    }

    /**
     * Crea un árbol placeholder simple
     */
    private void createSimpleTree(double x, double y, double size) {
        javafx.scene.shape.Circle canopy = new javafx.scene.shape.Circle(size/2);
        canopy.setCenterX(x + size/2);
        canopy.setCenterY(y + size/2);

        int greenValue = 80 + (int)(Math.random() * 40);
        canopy.setFill(Color.rgb(0, greenValue, 0));
        canopy.setStroke(Color.rgb(0, greenValue - 15, 0));
        canopy.setStrokeWidth(1.5);

        javafx.scene.shape.Rectangle trunk = new javafx.scene.shape.Rectangle(
                x + size/2 - size/7, y + size - size/3, size/3.5, size/2.5
        );
        trunk.setFill(Color.rgb(101, 67, 33));

        Pane tree = new Pane(canopy, trunk);

        tree.setOnMouseClicked(e -> System.out.println("🌲 Árbol clickeado"));
        tree.setOnMouseEntered(e -> {
            tree.setCursor(javafx.scene.Cursor.HAND);
            tree.setScaleX(1.05);
            tree.setScaleY(1.05);
        });
        tree.setOnMouseExited(e -> {
            tree.setScaleX(1.0);
            tree.setScaleY(1.0);
        });

        root.getChildren().add(tree);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Button createTextButton(String icon, String text, String cost) {
        HBox buttonContent = new HBox(10);
        buttonContent.setAlignment(Pos.CENTER_LEFT);
        buttonContent.setPadding(new Insets(5, 15, 5, 15));

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px; -fx-padding: 0 10 0 0;");

        VBox textContainer = new VBox(2);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label costLabel = new Label(cost);
        costLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        textContainer.getChildren().addAll(textLabel, costLabel);
        buttonContent.getChildren().addAll(iconLabel, textContainer);

        Button button = new Button();
        button.setGraphic(buttonContent);
        button.setPrefWidth(260);
        button.setPrefHeight(55);
        button.setAlignment(Pos.CENTER_LEFT);

        button.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.5); " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #2c3e50;"
        );

        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: rgba(236, 240, 241, 0.5); " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: #3498db; " +
                            "-fx-border-width: 1.5; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.3), 5, 0.5, 0, 1);"
            );
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.5); " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: #dcdde1; " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: null;"
            );
        });

        return button;
    }

    private void animateCenterEntrance(VBox panel) {
        panel.setScaleX(0.9);
        panel.setScaleY(0.9);
        panel.setOpacity(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(400), panel);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(400), panel);
        fade.setToValue(1.0);
        fade.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        javafx.animation.ParallelTransition parallel = new javafx.animation.ParallelTransition(scale, fade);
        parallel.play();
    }

    private void showConstructionAnimation(String buildingType) {
        System.out.println("🔨 Iniciando construcción de: " + buildingType);
        System.out.println("⏳ Tiempo estimado: 10 segundos");
    }

    private void addPlaceholderTownHall() {
        Rectangle placeholder = new Rectangle(100, 100, Color.rgb(139, 69, 19, 0.8));
        placeholder.setX(windowWidth * 0.3 - 50);
        placeholder.setY(windowHeight * 0.4 - 50);
        placeholder.setStroke(Color.GOLD);
        placeholder.setStrokeWidth(2);

        placeholder.setOnMouseClicked(e -> showTownHallMenu(windowWidth * 0.3, windowHeight * 0.4));
        root.getChildren().add(placeholder);
    }

    void setupBuildingListeners(Scene scene) {

        scene.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {
            if (!isBuildingMode || !buildingGhost.isVisible()) return;

            double x = event.getX() - buildingGhost.getFitWidth() / 2;
            double y = event.getY() - buildingGhost.getFitHeight() / 2;

            buildingGhost.setX(x);
            buildingGhost.setY(y);

            if (checkCollisionWithReducedMargin(x, y,
                    buildingGhost.getFitWidth(), buildingGhost.getFitHeight(), 3)) {
                javafx.scene.effect.ColorAdjust redTint = new javafx.scene.effect.ColorAdjust();
                redTint.setHue(1.0);
                buildingGhost.setEffect(redTint);
            } else if (x < 0 || y < 0 ||
                    x + buildingGhost.getFitWidth() > windowWidth ||
                    y + buildingGhost.getFitHeight() > windowHeight) {
                javafx.scene.effect.ColorAdjust redTint = new javafx.scene.effect.ColorAdjust();
                redTint.setHue(1.0);
                buildingGhost.setEffect(redTint);
            } else {
                buildingGhost.setEffect(null);
            }

            event.consume(); // 👈 importante: en build mode, el mouse es del build mode
        });

        scene.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> {
            if (!isBuildingMode) return;

            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                placeBuilding(event.getX(), event.getY());
                event.consume();
            }
        });

        scene.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (!isBuildingMode) return;

            if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                cancelBuildingMode();
                event.consume();
            }
        });
    }
    private void setMapBackground(Pane pane, double width, double height) {
        try {
            BackgroundImage background = new BackgroundImage(
                    loadImage("map_background(4)"),
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(
                            BackgroundSize.AUTO, BackgroundSize.AUTO,
                            false, false,
                            true, true
                    )
            );
            pane.setBackground(new Background(background));
        } catch (Exception e) {
            pane.setStyle("-fx-background-color: linear-gradient(to bottom, #1a472a, #2a5c2a);");
        }
    }

    /**
     * Verifica colisión con márgenes reducidos
     */
    private boolean checkCollisionWithReducedMargin(double x, double y, double width, double height, double margin) {
        Rectangle newBuildingBounds = new Rectangle(x + margin, y + margin, width - margin * 2, height - margin * 2);

        for (ImageView building : placedBuildings) {
            Rectangle existingBounds = new Rectangle(
                    building.getX() + margin,
                    building.getY() + margin,
                    building.getFitWidth() - margin * 2,
                    building.getFitHeight() - margin * 2
            );

            if (newBuildingBounds.intersects(existingBounds.getBoundsInLocal())) {
                return true;
            }
        }

        for (Node node : root.getChildren()) {
            if (node instanceof ImageView && node != buildingGhost) {
                ImageView existingBuilding = (ImageView) node;
                if (!existingBuilding.equals(buildingGhost)) {
                    Rectangle existingBounds = new Rectangle(
                            existingBuilding.getX() + margin,
                            existingBuilding.getY() + margin,
                            existingBuilding.getFitWidth() - margin * 2,
                            existingBuilding.getFitHeight() - margin * 2
                    );

                    if (newBuildingBounds.intersects(existingBounds.getBoundsInLocal())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean checkCollision(double x, double y, double width, double height) {
        // Usar margen reducido por defecto
        return checkCollisionWithReducedMargin(x, y, width, height, 5);
    }

    private void showCollisionFeedback() {
        if (!isBuildingMode) return;

        buildingGhost.setEffect(new javafx.scene.effect.ColorAdjust());
        javafx.scene.effect.ColorAdjust redTint = new javafx.scene.effect.ColorAdjust();
        redTint.setHue(1.0);
        buildingGhost.setEffect(redTint);

        TranslateTransition shakeX = new TranslateTransition(Duration.millis(50), buildingGhost);
        shakeX.setFromX(-10);
        shakeX.setToX(10);
        shakeX.setCycleCount(6);
        shakeX.setAutoReverse(true);

        TranslateTransition shakeY = new TranslateTransition(Duration.millis(50), buildingGhost);
        shakeY.setFromY(-5);
        shakeY.setToY(5);
        shakeY.setCycleCount(6);
        shakeY.setAutoReverse(true);

        javafx.animation.ParallelTransition shake = new javafx.animation.ParallelTransition(shakeX, shakeY);

        shake.setOnFinished(e -> {
            buildingGhost.setEffect(null);
            buildingGhost.setTranslateX(0);
            buildingGhost.setTranslateY(0);
        });

        shake.play();
    }

    private void showOutOfBoundsFeedback() {
        if (!isBuildingMode) return;

        javafx.scene.effect.ColorAdjust blueTint = new javafx.scene.effect.ColorAdjust();
        blueTint.setHue(-0.7);
        buildingGhost.setEffect(blueTint);

        FadeTransition pulse = new FadeTransition(Duration.millis(300), buildingGhost);
        pulse.setFromValue(0.4);
        pulse.setToValue(0.8);
        pulse.setCycleCount(4);
        pulse.setAutoReverse(true);

        pulse.setOnFinished(e -> {
            buildingGhost.setEffect(null);
            buildingGhost.setOpacity(0.6);
        });

        pulse.play();
    }

    private void makeBuildingInteractive(ImageView buildingView, String buildingType) {
        buildingView.setOnMouseClicked(e -> {
            System.out.println("🏠 " + buildingType + " clickeado");

            // Si es un cuartel, mostrar su menú especial
            if (buildingType.equalsIgnoreCase("Cuartel")) {
                System.out.println("⚔️ Cuartel clickeado - Abriendo menú de unidades...");
                showBarracksMenu(buildingView);
            }
        });

        buildingView.setOnMouseEntered(e -> {
            buildingView.setCursor(javafx.scene.Cursor.HAND);
            buildingView.setScaleX(1.05);
            buildingView.setScaleY(1.05);

            // Efecto especial para cuarteles
            if (buildingType.equalsIgnoreCase("Cuartel")) {
                DropShadow glow = new DropShadow();
                glow.setColor(Color.rgb(220, 20, 60, 0.7)); // Rojo carmesí para cuartel
                glow.setRadius(15);
                buildingView.setEffect(glow);
            }
        });

        buildingView.setOnMouseExited(e -> {
            buildingView.setCursor(javafx.scene.Cursor.DEFAULT);
            buildingView.setScaleX(1.0);
            buildingView.setScaleY(1.0);

            // Restaurar efecto normal para cuarteles
            if (buildingType.equalsIgnoreCase("Cuartel")) {
                DropShadow shadow = new DropShadow();
                shadow.setColor(Color.rgb(0, 0, 0, 0.5));
                shadow.setRadius(10);
                shadow.setSpread(0.1);
                buildingView.setEffect(shadow);
            }
        });
    }

    private void centerStage(Stage stage, double width, double height) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setX((screen.getWidth() - width) / 2);
        stage.setY((screen.getHeight() - height) / 2);
    }

    // ==================== CLASES AUXILIARES ====================

    private class Position {
        double x;
        double y;

        Position(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private Position findPositionForUnit(double townHallX, double townHallY, double townHallSize,
                                         double unitSize, double spacing, String unitType) {
        System.out.println("🔍 Buscando posición para " + unitType + "...");

        List<Position> positionsToTry = new ArrayList<>();

        generatePositionsAroundPoint(positionsToTry,
                townHallX + townHallSize/2,
                townHallY + townHallSize/2,
                townHallSize/2 + unitSize + spacing,
                16, unitSize);

        generatePositionsAroundPoint(positionsToTry,
                townHallX + townHallSize/2,
                townHallY + townHallSize/2,
                townHallSize + unitSize * 3,
                24, unitSize);

        for (Position pos : positionsToTry) {
            if (!checkCollisionForUnitReduced(pos.x, pos.y, unitSize, unitSize, unitType, 2) &&
                    pos.x >= 0 && pos.y >= 0 &&
                    pos.x + unitSize <= windowWidth &&
                    pos.y + unitSize <= windowHeight) {

                System.out.println("✅ Posición encontrada para " + unitType +
                        " en: (" + (int)pos.x + ", " + (int)pos.y + ")");
                return pos;
            }
        }

        System.out.println("⚠️ No hay espacio cerca del TownHall, buscando junto a otros " + unitType + "s...");
        return findPositionNextToOtherUnits(unitType, unitSize, spacing);
    }

    private void generatePositionsAroundPoint(List<Position> positions,
                                              double centerX, double centerY,
                                              double radius, int numPoints, double unitSize) {
        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double x = centerX + Math.cos(angle) * radius - unitSize/2;
            double y = centerY + Math.sin(angle) * radius - unitSize/2;
            positions.add(new Position(x, y));
        }
    }

    /**
     * Verifica colisión para unidades con margen reducido
     */
    private boolean checkCollisionForUnitReduced(double x, double y, double width, double height,
                                                 String unitType, double margin) {
        Rectangle newBounds = new Rectangle(x + margin, y + margin, width - margin * 2, height - margin * 2);

        if (x < 0 || y < 0 || x + width > windowWidth || y + height > windowHeight) {
            return true;
        }

        for (Node node : root.getChildren()) {
            if (node instanceof ImageView && node != buildingGhost) {
                ImageView existing = (ImageView) node;

                if (existing.getFitWidth() == 50 && existing.getFitHeight() == 50) {
                    Rectangle existingBounds = new Rectangle(
                            existing.getX() + margin,
                            existing.getY() + margin,
                            existing.getFitWidth() - margin * 2,
                            existing.getFitHeight() - margin * 2
                    );

                    if (newBounds.intersects(existingBounds.getBoundsInLocal())) {
                        return true;
                    }
                }
            }
        }

        for (Node node : root.getChildren()) {
            if (node instanceof ImageView && node != buildingGhost) {
                ImageView existing = (ImageView) node;

                if (existing.getFitWidth() >= 100 || existing.getFitHeight() >= 100) {
                    Rectangle existingBounds = new Rectangle(
                            existing.getX() + margin,
                            existing.getY() + margin,
                            existing.getFitWidth() - margin * 2,
                            existing.getFitHeight() - margin * 2
                    );

                    if (newBounds.intersects(existingBounds.getBoundsInLocal())) {
                        return true;
                    }
                }
            }
        }

        // Verificar colisión con árboles con margen reducido
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView) {
                ImageView imageView = (ImageView) node;

                if (imageView.getId() != null && imageView.getId().startsWith("Arbol_")) {
                    Rectangle treeBounds = new Rectangle(
                            imageView.getX() + margin,
                            imageView.getY() + margin,
                            imageView.getFitWidth() - margin * 2,
                            imageView.getFitHeight() - margin * 2
                    );

                    if (newBounds.intersects(treeBounds.getBoundsInLocal())) {
                        return true;
                    }
                }
            }
        }

        // Verificar colisión con minas con margen reducido
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView) {
                ImageView imageView = (ImageView) node;

                if (imageView.getId() != null && imageView.getId().startsWith("Mina_")) {
                    Rectangle mineBounds = new Rectangle(
                            imageView.getX() + margin,
                            imageView.getY() + margin,
                            imageView.getFitWidth() - margin * 2,
                            imageView.getFitHeight() - margin * 2
                    );

                    if (newBounds.intersects(mineBounds.getBoundsInLocal())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean checkCollisionForUnit(double x, double y, double width, double height, String unitType) {
        // Usar margen reducido de 2px
        return checkCollisionForUnitReduced(x, y, width, height, unitType, 2);
    }

    private void createUnitAtPosition(String unitType, String imageName, double x, double y, double size) {
        try {
            Image unitImage = loadImage(imageName);

            ImageView unitView = new ImageView(unitImage);
            unitView.setFitWidth(size);
            unitView.setFitHeight(size);
            unitView.setPreserveRatio(true);
            unitView.setX(x);
            unitView.setY(y);

            unitView.setId(unitType + "_" + System.currentTimeMillis());
            unitView.setUserData(unitType);


            DropShadow shadow = new DropShadow();
            if (unitType.equals("minero")) {
                shadow.setColor(Color.rgb(184, 134, 11, 0.6));
            } else if (unitType.equals("leñador")) {
                shadow.setColor(Color.rgb(34, 139, 34, 0.6));
            } else {
                shadow.setColor(Color.rgb(0, 0, 0, 0.4));
            }
            shadow.setRadius(8);
            unitView.setEffect(shadow);
            unitView.getProperties().put("baseEffect", shadow);

            FadeTransition fade = new FadeTransition(Duration.millis(300), unitView);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);

            ScaleTransition scale = new ScaleTransition(Duration.millis(300), unitView);
            scale.setFromX(0.3);
            scale.setFromY(0.3);
            scale.setToX(1.0);
            scale.setToY(1.0);

            root.getChildren().add(unitView);

            javafx.animation.ParallelTransition parallel =
                    new javafx.animation.ParallelTransition(fade, scale);
            parallel.play();

            System.out.println("✅ " + unitType + " creado en: (" + (int)x + ", " + (int)y + ")");

        } catch (Exception e) {
            System.err.println("❌ Error al crear " + unitType + ": " + e.getMessage());
            throw e;
        }
    }

    private List<ImageView> getExistingUnits(String unitType) {
        List<ImageView> units = new ArrayList<>();

        for (Node node : root.getChildren()) {
            if (node instanceof ImageView && node != buildingGhost) {
                ImageView imageView = (ImageView) node;
                if (imageView.getFitWidth() == 50 && imageView.getFitHeight() == 50) {
                    if (imageView.getId() != null && imageView.getId().startsWith(unitType)) {
                        units.add(imageView);
                    } else if (unitType.equals("unidad")) {
                        units.add(imageView);
                    }
                }
            }
        }

        return units;
    }

    private Position findPositionNextToOtherUnits(String unitType, double unitSize, double spacing) {
        List<ImageView> existingUnits = getExistingUnits(unitType);

        if (existingUnits.isEmpty()) {
            System.out.println("📭 No hay " + unitType + "s existentes, buscando espacio libre...");
            return findAnyFreeSpace(unitSize, spacing);
        }

        System.out.println("🔍 Buscando junto a " + existingUnits.size() + " " + unitType + "s existentes...");

        for (ImageView unit : existingUnits) {
            double unitX = unit.getX();
            double unitY = unit.getY();

            Position[] positionsAround = {
                    new Position(unitX + unitSize + spacing, unitY),
                    new Position(unitX - unitSize - spacing, unitY),
                    new Position(unitX, unitY - unitSize - spacing),
                    new Position(unitX, unitY + unitSize + spacing),
                    new Position(unitX + unitSize + spacing, unitY - unitSize - spacing),
                    new Position(unitX - unitSize - spacing, unitY - unitSize - spacing),
                    new Position(unitX + unitSize + spacing, unitY + unitSize + spacing),
                    new Position(unitX - unitSize - spacing, unitY + unitSize - spacing)
            };

            for (Position pos : positionsAround) {
                if (!checkCollisionForUnit(pos.x, pos.y, unitSize, unitSize, unitType) &&
                        pos.x >= 0 && pos.y >= 0 &&
                        pos.x + unitSize <= windowWidth &&
                        pos.y + unitSize <= windowHeight) {

                    System.out.println("✅ Encontrada posición junto a otro " + unitType +
                            " en: (" + (int)pos.x + ", " + (int)pos.y + ")");
                    return pos;
                }
            }
        }

        System.out.println("⚠️ No hay espacio junto a " + unitType + "s existentes, buscando en todo el mapa...");
        return findAnyFreeSpace(unitSize, spacing);
    }

    private Position findAnyFreeSpace(double unitSize, double spacing) {
        System.out.println("🔍 Buscando espacio libre en todo el mapa...");

        int gridCols = (int) (windowWidth / (unitSize + spacing));
        int gridRows = (int) (windowHeight / (unitSize + spacing));

        double townHallCenterX = windowWidth * 0.3 + 15;
        double townHallCenterY = windowHeight * 0.4 + 15;
        double searchRadius = 300;

        for (int radius = 1; radius <= 10; radius++) {
            double currentRadius = searchRadius * (radius / 10.0);

            for (int i = 0; i < 16; i++) {
                double angle = 2 * Math.PI * i / 16;
                double x = townHallCenterX + Math.cos(angle) * currentRadius - unitSize/2;
                double y = townHallCenterY + Math.sin(angle) * currentRadius - unitSize/2;

                x = Math.max(0, Math.min(x, windowWidth - unitSize));
                y = Math.max(0, Math.min(y, windowHeight - unitSize));

                if (!checkCollisionForUnit(x, y, unitSize, unitSize, "unidad") &&
                        x >= 0 && y >= 0 &&
                        x + unitSize <= windowWidth &&
                        y + unitSize <= windowHeight) {

                    System.out.println("✅ Espacio encontrado en radio " + (int)currentRadius +
                            "px del TownHall");
                    return new Position(x, y);
                }
            }
        }

        System.out.println("🌍 Buscando en cuadrícula por todo el mapa...");

        double cellSize = unitSize + spacing * 2;
        int cols = (int) (windowWidth / cellSize);
        int rows = (int) (windowHeight / cellSize);

        java.util.Random random = new java.util.Random();

        for (int attempt = 0; attempt < cols * rows * 2; attempt++) {
            int col = random.nextInt(cols);
            int row = random.nextInt(rows);

            double x = col * cellSize + spacing;
            double y = row * cellSize + spacing;

            if (x + unitSize > windowWidth) continue;
            if (y + unitSize > windowHeight) continue;

            if (!checkCollisionForUnit(x, y, unitSize, unitSize, "unidad")) {
                System.out.println("✅ Espacio encontrado en cuadrícula (" + col + ", " + row + ")");
                return new Position(x, y);
            }
        }

        System.out.println("⏳ Búsqueda exhaustiva...");

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                double x = col * cellSize + spacing;
                double y = row * cellSize + spacing;

                if (x + unitSize > windowWidth || y + unitSize > windowHeight) {
                    continue;
                }

                if (!checkCollisionForUnit(x, y, unitSize, unitSize, "unidad")) {
                    System.out.println("✅ Espacio encontrado en (" + col + ", " + row + ") después de búsqueda exhaustiva");
                    return new Position(x, y);
                }
            }
        }

        System.out.println("❌ El mapa está completamente lleno");
        return null;
    }

    /**
     * Crea minas distribuidas aleatoriamente por el mapa
     */
    private void addMinesToMap() {
        try {
            Image mineImage = loadImage("Mina.png");
            double mineSize = 45; // Tamaño de la mina

            System.out.println("⛏ Creando 5 minas en el mapa...");

            int minesCreated = 0;
            int maxAttempts = 500; // Para evitar bucles infinitos

            // Intentar crear 15 minas
            while (minesCreated < 5 && maxAttempts > 0) {
                double x = getRandomPosition(windowWidth, mineSize);
                double y = getRandomPosition(windowHeight, mineSize);

                // Verificar que no colisione con nada (con margen reducido)
                if (!checkMineCollisionReduced(x, y, mineSize, 3)) {
                    createMine(mineImage, mineSize, x, y, minesCreated);
                    minesCreated++;
                }

                maxAttempts--;
            }

            if (minesCreated < 15) {
                System.out.println("⚠️ Solo se pudieron crear " + minesCreated + " minas (espacio insuficiente)");
            } else {
                System.out.println("✅ 15 minas creadas exitosamente!");
            }

        } catch (Exception e) {
            System.err.println("❌ Error al crear minas: " + e.getMessage());
            createPlaceholderMines();
        }
    }

    /**
     * Genera una posición aleatoria dentro de los límites del mapa
     */
    private double getRandomPosition(double maxSize, double objectSize) {
        double margin = 20; // Margen mínimo del borde
        return margin + Math.random() * (maxSize - objectSize - margin * 2);
    }

    /**
     * Verifica colisiones para una mina con margen reducido
     */
    private boolean checkMineCollisionReduced(double x, double y, double size, double margin) {
        // Crear el área de la mina con margen reducido
        Rectangle mineBounds = new Rectangle(x + margin, y + margin, size - margin * 2, size - margin * 2);

        // Verificar colisión con edificios existentes
        for (ImageView building : placedBuildings) {
            Rectangle buildingBounds = new Rectangle(
                    building.getX() + margin,
                    building.getY() + margin,
                    building.getFitWidth() - margin * 2,
                    building.getFitHeight() - margin * 2
            );

            if (mineBounds.intersects(buildingBounds.getBoundsInLocal())) {
                return true;
            }
        }

        // Verificar colisión con TownHall
        double townHallX = windowWidth * 0.3 + 100;
        double townHallY = windowHeight * 0.4 + 100;
        double townHallSize = 170;

        Rectangle townHallBounds = new Rectangle(
                townHallX + margin,
                townHallY + margin,
                townHallSize - margin * 2,
                townHallSize - margin * 2
        );

        if (mineBounds.intersects(townHallBounds.getBoundsInLocal())) {
            return true;
        }

        // Verificar colisión con árboles
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView) {
                ImageView imageView = (ImageView) node;

                // Si es un árbol
                if (imageView.getId() != null && imageView.getId().startsWith("Arbol_")) {
                    Rectangle treeBounds = new Rectangle(
                            imageView.getX() + margin,
                            imageView.getY() + margin,
                            imageView.getFitWidth() - margin * 2,
                            imageView.getFitHeight() - margin * 2
                    );

                    if (mineBounds.intersects(treeBounds.getBoundsInLocal())) {
                        return true;
                    }
                }
            }
        }

        // Verificar colisión con unidades
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView) {
                ImageView imageView = (ImageView) node;

                // Si es una unidad (tamaño 50x50)
                if (imageView.getFitWidth() == 50 && imageView.getFitHeight() == 50) {
                    if (imageView.getId() != null &&
                            (imageView.getId().startsWith("minero_") ||
                                    imageView.getId().startsWith("leñador_") ||
                                    imageView.getId().startsWith("caballero_"))) {

                        Rectangle unitBounds = new Rectangle(
                                imageView.getX() + margin,
                                imageView.getY() + margin,
                                imageView.getFitWidth() - margin * 2,
                                imageView.getFitHeight() - margin * 2
                        );

                        if (mineBounds.intersects(unitBounds.getBoundsInLocal())) {
                            return true;
                        }
                    }
                }
            }
        }

        // Verificar colisión con otras minas
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView) {
                ImageView imageView = (ImageView) node;

                // Si es una mina
                if (imageView.getId() != null && imageView.getId().startsWith("Mina_")) {
                    Rectangle otherMineBounds = new Rectangle(
                            imageView.getX() + margin,
                            imageView.getY() + margin,
                            imageView.getFitWidth() - margin * 2,
                            imageView.getFitHeight() - margin * 2
                    );

                    if (mineBounds.intersects(otherMineBounds.getBoundsInLocal())) {
                        return true;
                    }
                }
            }
        }

        // Verificar que no esté demasiado cerca de los bordes
        if (x < 20 || y < 20 ||
                x + size > windowWidth - 20 ||
                y + size > windowHeight - 20) {
            return true;
        }

        return false;
    }

    private boolean checkMineCollision(double x, double y, double size) {
        // Usar margen reducido de 3px
        return checkMineCollisionReduced(x, y, size, 3);
    }

    /**
     * Crea una mina individual en una posición específica
     */
    private void createMine(Image mineImage, double size, double x, double y, int mineNumber) {
        ImageView mineView = new ImageView(mineImage);

        mineView.setFitWidth(size);
        mineView.setFitHeight(size);
        mineView.setPreserveRatio(true);
        mineView.setX(x);
        mineView.setY(y);
        mineView.setId("Mina_" + mineNumber);

        // Rotación aleatoria ligera
        mineView.setRotate((Math.random() - 0.5) * 20);

        // Efecto de sombra
        DropShadow mineShadow = new DropShadow();
        mineShadow.setColor(Color.rgb(139, 69, 19, 0.6)); // Color marrón para minas
        mineShadow.setRadius(8);
        mineShadow.setOffsetY(3);
        mineView.setEffect(mineShadow);

        // Hacer la mina interactiva
        makeMineInteractive(mineView, "Mina " + (mineNumber + 1));

        // Animación de aparición
        FadeTransition fade = new FadeTransition(Duration.millis(500), mineView);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(500), mineView);
        scale.setFromX(0.3);
        scale.setFromY(0.3);
        scale.setToX(1.0);
        scale.setToY(1.0);

        // Añadir a la escena
        root.getChildren().add(mineView);

        // Ejecutar animaciones
        javafx.animation.ParallelTransition parallel =
                new javafx.animation.ParallelTransition(fade, scale);
        parallel.play();

        System.out.println("⛏ Mina " + (mineNumber + 1) + " creada en: (" +
                (int)x + ", " + (int)y + ")");
    }

    /**
     * Hace una mina interactiva (clickeable)
     */
    private void makeMineInteractive(ImageView mineView, String mineName) {
        mineView.setOnMouseClicked(event -> {
            System.out.println("⛏ " + mineName + " clickeada");

            // Efecto visual al hacer clic
            FadeTransition flash = new FadeTransition(Duration.millis(150), mineView);
            flash.setFromValue(1.0);
            flash.setToValue(0.7);
            flash.setAutoReverse(true);
            flash.setCycleCount(2);
            flash.play();

            // Efecto de sacudida
            TranslateTransition shake = new TranslateTransition(Duration.millis(50), mineView);
            shake.setFromX(-3);
            shake.setToX(3);
            shake.setCycleCount(4);
            shake.setAutoReverse(true);
            shake.play();
        });

        mineView.setOnMouseEntered(e -> {
            mineView.setCursor(javafx.scene.Cursor.HAND);
            mineView.setScaleX(1.05);
            mineView.setScaleY(1.05);

            // Efecto de resaltado
            DropShadow highlight = new DropShadow();
            highlight.setColor(Color.rgb(255, 215, 0, 0.7)); // Color dorado para resaltar
            highlight.setRadius(12);
            mineView.setEffect(highlight);
        });

        mineView.setOnMouseExited(e -> {
            mineView.setCursor(javafx.scene.Cursor.DEFAULT);
            mineView.setScaleX(1.0);
            mineView.setScaleY(1.0);

            // Restaurar sombra normal
            DropShadow normalShadow = new DropShadow();
            normalShadow.setColor(Color.rgb(139, 69, 19, 0.6));
            normalShadow.setRadius(8);
            normalShadow.setOffsetY(3);
            mineView.setEffect(normalShadow);
        });
    }

    /**
     * Crea minas placeholder si no se carga la imagen
     */
    private void createPlaceholderMines() {
        System.out.println("⛏ Creando minas placeholder...");

        double mineSize = 60;
        int minesCreated = 0;

        // Zonas predefinidas para colocar minas (evitando el centro)
        double[][] zones = {
                {windowWidth * 0.2, windowHeight * 0.2},  // Esquina superior izquierda
                {windowWidth * 0.8, windowHeight * 0.2},  // Esquina superior derecha
                {windowWidth * 0.2, windowHeight * 0.8},  // Esquina inferior izquierda
                {windowWidth * 0.8, windowHeight * 0.8},  // Esquina inferior derecha
                {windowWidth * 0.5, windowHeight * 0.15}, // Centro superior
                {windowWidth * 0.15, windowHeight * 0.5}, // Centro izquierdo
                {windowWidth * 0.85, windowHeight * 0.5}, // Centro derecho
                {windowWidth * 0.5, windowHeight * 0.85}  // Centro inferior
        };

        for (double[] zone : zones) {
            if (minesCreated >= 15) break;

            // Crear 2 minas en cada zona
            for (int i = 0; i < 2 && minesCreated < 15; i++) {
                double x = zone[0] + (Math.random() - 0.5) * 100;
                double y = zone[1] + (Math.random() - 0.5) * 100;

                // Asegurar que esté dentro de los límites
                x = Math.max(30, Math.min(x, windowWidth - mineSize - 30));
                y = Math.max(30, Math.min(y, windowHeight - mineSize - 30));

                // Verificar colisión simple
                boolean hasCollision = false;
                for (Node node : root.getChildren()) {
                    if (node instanceof ImageView) {
                        ImageView existing = (ImageView) node;
                        if (Math.abs(existing.getX() - x) < mineSize &&
                                Math.abs(existing.getY() - y) < mineSize) {
                            hasCollision = true;
                            break;
                        }
                    }
                }

                if (!hasCollision) {
                    createPlaceholderMine(x, y, mineSize, minesCreated);
                    minesCreated++;
                }
            }
        }

        System.out.println("✅ " + minesCreated + " minas placeholder creadas");
    }

    /**
     * Crea una mina placeholder simple
     */
    private void createPlaceholderMine(double x, double y, double size, int mineNumber) {
        // Círculo para la mina
        javafx.scene.shape.Circle mineCircle = new javafx.scene.shape.Circle(size/2);
        mineCircle.setCenterX(x + size/2);
        mineCircle.setCenterY(y + size/2);
        mineCircle.setFill(Color.rgb(101, 67, 33)); // Color marrón

        // Detalle interior
        javafx.scene.shape.Circle detail = new javafx.scene.shape.Circle(size/4);
        detail.setCenterX(x + size/2);
        detail.setCenterY(y + size/2);
        detail.setFill(Color.rgb(66, 44, 22));

        // Símbolo de pico
        javafx.scene.text.Text pickaxe = new javafx.scene.text.Text("⛏");
        pickaxe.setX(x + size/2 - 8);
        pickaxe.setY(y + size/2 + 8);
        pickaxe.setStyle("-fx-font-size: 16px; -fx-fill: gold;");

        Pane mine = new Pane(mineCircle, detail, pickaxe);
        mine.setId("MinaPlaceholder_" + mineNumber);

        // Hacer interactiva
        mine.setOnMouseClicked(e -> System.out.println("⛏ Mina " + (mineNumber + 1) + " clickeada"));
        mine.setOnMouseEntered(e -> {
            mine.setCursor(javafx.scene.Cursor.HAND);
            mine.setScaleX(1.05);
            mine.setScaleY(1.05);
        });
        mine.setOnMouseExited(e -> {
            mine.setScaleX(1.0);
            mine.setScaleY(1.0);
        });

        root.getChildren().add(mine);
    }

    // ==================== SISTEMA DE CUARTEL Y CABALLEROS ====================

    /**
     * Muestra el menú del Cuartel
     */
    private void showBarracksMenu(ImageView barracksView) {
        if (barracksPopup != null) {
            barracksPopup.hide();
        }

        barracksPopup = new Popup();
        barracksPopup.setAutoFix(true);
        barracksPopup.setAutoHide(true);
        barracksPopup.setHideOnEscape(true);

        VBox mainPanel = createBarracksPanel();
        StackPane container = new StackPane(mainPanel);

        double panelWidth = 280;
        double panelHeight = 220;

        // Posicionar cerca del cuartel, no en el centro
        double barracksX = barracksView.getX() + barracksView.getFitWidth()/2;
        double barracksY = barracksView.getY();

        double panelX = Math.max(20, Math.min(barracksX - panelWidth/2, windowWidth - panelWidth - 20));
        double panelY = Math.max(20, Math.min(barracksY - panelHeight - 10, windowHeight - panelHeight - 20));

        barracksPopup.getContent().add(container);
        barracksPopup.show(root.getScene().getWindow(), panelX, panelY);

        animateBarracksEntrance(mainPanel);
    }

    /**
     * Crea el panel del Cuartel con el MISMO estilo que el TownHall
     */
    private VBox createBarracksPanel() {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20, 20, 20, 20));
        panel.setPrefSize(280, 220);

        // MISMO estilo EXACTO que el TownHall (50% opacidad)
        panel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.50); " + // 50% opacidad
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0.5, 0, 3);"
        );

        // Título con icono de cuartel
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER);

        Label swordIcon = new Label("⚔");
        swordIcon.setStyle("-fx-font-size: 20px;");

        Label title = new Label("Cuartel");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        titleBox.getChildren().addAll(swordIcon, title);

        // Separador elegante (mismo que TownHall pero color diferente)
        Region separator = new Region();
        separator.setPrefHeight(2);
        separator.setPrefWidth(200);
        separator.setStyle("-fx-background-color: linear-gradient(to right, transparent, #c0392b, transparent);"); // Rojo para cuartel

        // Contenedor de botones
        VBox buttonContainer = new VBox(8);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(15, 0, 0, 0));

        // Botón para crear caballero
        Button knightButton = createBarracksButton("♞", "Crear Caballero", "50 Oro");

        knightButton.setOnAction(e -> {
            System.out.println("♞ Creando Caballero...");
            barracksPopup.hide();
            createKnightUnit(barracksPopup);
        });

        buttonContainer.getChildren().addAll(knightButton);

        panel.getChildren().addAll(titleBox, separator, buttonContainer);

        return panel;
    }

    /**
     * Crea un botón para el menú del Cuartel con el MISMO estilo que el TownHall
     */
    private Button createBarracksButton(String icon, String text, String cost) {
        HBox buttonContent = new HBox(10);
        buttonContent.setAlignment(Pos.CENTER_LEFT);
        buttonContent.setPadding(new Insets(8, 15, 8, 15));

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 22px; -fx-padding: 0 10 0 0;");

        VBox textContainer = new VBox(2);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label costLabel = new Label(cost);
        costLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        textContainer.getChildren().addAll(textLabel, costLabel);
        buttonContent.getChildren().addAll(iconLabel, textContainer);

        Button button = new Button();
        button.setGraphic(buttonContent);
        button.setPrefWidth(240);
        button.setPrefHeight(55);
        button.setAlignment(Pos.CENTER_LEFT);

        // MISMO estilo EXACTO que los botones del TownHall (50% opacidad)
        String baseStyle =
                "-fx-background-color: rgba(255, 255, 255, 0.50); " + // 50% opacidad
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #2c3e50;";

        // Color específico para botones de cuartel (rojo/marrón)
        String borderColor = "#c0392b"; // Rojo oscuro para cuartel

        // Aplicar el color de borde específico
        button.setStyle(baseStyle +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 2;");

        // EFECTO HOVER IDÉNTICO a los botones del TownHall
        button.setOnMouseEntered(e -> {
            String hoverStyle =
                    "-fx-background-color: rgba(236, 240, 241, 0.50); " + // 50% opacidad en hover
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: " + borderColor + ";" +
                            "-fx-border-width: 2.5; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(192, 57, 43, 0.4), 8, 0.5, 0, 2);";

            button.setStyle(hoverStyle);
            button.setScaleX(1.02);
            button.setScaleY(1.02);
        });

        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle +
                    "-fx-border-color: " + borderColor + ";" +
                    "-fx-border-width: 2;");
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        // Efecto al presionar
        button.setOnMousePressed(e -> {
            button.setStyle(baseStyle +
                    "-fx-border-color: " + borderColor + ";" +
                    "-fx-border-width: 3; " +
                    "-fx-background-color: rgba(220, 220, 220, 0.50);"); // 50% opacidad
        });

        button.setOnMouseReleased(e -> {
            button.setStyle(baseStyle +
                    "-fx-border-color: " + borderColor + ";" +
                    "-fx-border-width: 2;");
        });

        return button;
    }

    /**
     * Animación de entrada para el menú del cuartel
     */
    private void animateBarracksEntrance(VBox panel) {
        panel.setScaleX(0.9);
        panel.setScaleY(0.9);
        panel.setOpacity(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(400), panel);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(400), panel);
        fade.setToValue(1.0);
        fade.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        javafx.animation.ParallelTransition parallel = new javafx.animation.ParallelTransition(scale, fade);
        parallel.play();
    }

    /**
     * Crea una unidad de caballero (versión optimizada)
     */
    private void createKnightUnit(Popup barracksPopup) {
        try {
            // Verificar recursos usando el método spend existente
            if (territory1 != null && territory1.getTownHall() != null) {
                // Crear mapa de costos
                Map<ResourceType, Integer> knightCost = new HashMap<>();
                knightCost.put(ResourceType.GOLD, 50);

                // Verificar si puede pagar
                if (territory1.getTownHall().getStoredResources().canAfford(knightCost)) {
                    // Restar recursos usando el método spend existente
                    territory1.getTownHall().getStoredResources().spend(knightCost);
                    System.out.println("✅ Recursos descontados exitosamente");

                    // Actualizar display de recursos
                    updateResourceDisplay();

                    // Encontrar el cuartel más cercano
                    ImageView nearestBarracks = findNearestBarracks();
                    if (nearestBarracks != null) {
                        // Crear caballero cerca del cuartel
                        if (createKnightNextToBarracks(nearestBarracks)) {
                            System.out.println("♞ Caballero creado exitosamente!");
                        } else {
                            System.out.println("⚠️ No se pudo crear el caballero cerca del cuartel");
                            // Devolver los recursos si no se pudo crear
                            territory1.getTownHall().getStoredResources().addResource(ResourceType.GOLD, 50);
                            updateResourceDisplay();
                        }
                    } else {
                        System.out.println("⚠️ No se encontró un cuartel para crear el caballero");
                        // Devolver los recursos
                        territory1.getTownHall().getStoredResources().addResource(ResourceType.GOLD, 50);
                        updateResourceDisplay();
                    }
                } else {
                    System.out.println("❌ Recursos insuficientes para crear caballero");
                    showInsufficientResourcesForKnight();
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error al crear caballero: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Encuentra el cuartel más cercano
     */
    private ImageView findNearestBarracks() {
        ImageView nearestBarracks = null;
        double minDistance = Double.MAX_VALUE;

        // Buscar entre todos los nodos del root
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView) {
                ImageView imageView = (ImageView) node;

                // Verificar si es un cuartel por ID
                if (imageView.getId() != null && imageView.getId().startsWith("Cuartel_")) {
                    // Calcular distancia desde el centro de la pantalla
                    double centerX = windowWidth / 2;
                    double centerY = windowHeight / 2;
                    double buildingCenterX = imageView.getX() + imageView.getFitWidth() / 2;
                    double buildingCenterY = imageView.getY() + imageView.getFitHeight() / 2;

                    double distance = Math.sqrt(
                            Math.pow(buildingCenterX - centerX, 2) +
                                    Math.pow(buildingCenterY - centerY, 2)
                    );

                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestBarracks = imageView;
                    }
                }
            }
        }

        if (nearestBarracks != null) {
            System.out.println("📍 Cuartel encontrado en: (" +
                    (int)nearestBarracks.getX() + ", " +
                    (int)nearestBarracks.getY() + ")");
        } else {
            System.out.println("⚠️ No se encontró ningún cuartel");
        }

        return nearestBarracks;
    }

    /**
     * Crea un caballero cerca de un cuartel específico (versión optimizada)
     */
    private boolean createKnightNextToBarracks(ImageView barracksView) {
        try {
            if (barracksView == null) {
                System.out.println("❌ No hay cuartel para crear unidades");
                return false;
            }

            double barracksX = barracksView.getX();
            double barracksY = barracksView.getY();
            double barracksWidth = barracksView.getFitWidth();
            double barracksHeight = barracksView.getFitHeight();
            double knightSize = 50;

            System.out.println("🔍 Buscando posición para caballero cerca del cuartel...");

            // Intentar posiciones en una formación compacta
            Position validPosition = findPositionForKnightCompact(barracksX, barracksY,
                    barracksWidth, barracksHeight,
                    knightSize);

            if (validPosition != null) {
                createKnightAtPosition("caballero", "caballero.png", validPosition.x, validPosition.y, knightSize);
                return true;
            } else {
                System.out.println("❌ No se pudo encontrar espacio para el caballero");
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Error al crear caballero: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Busca posición compacta para caballero (uno al lado del otro)
     */
    private Position findPositionForKnightCompact(double barracksX, double barracksY,
                                                  double barracksWidth, double barracksHeight,
                                                  double knightSize) {
        System.out.println("🔍 Buscando posición compacta para caballero...");

        // Calcular centro del cuartel
        double centerX = barracksX + barracksWidth / 2;
        double centerY = barracksY + barracksHeight / 2;

        // Distancia desde el cuartel para crear caballeros
        double distanceFromBarracks = barracksWidth / 2 + knightSize + 5; // Solo 5px de separación

        // Direcciones para posicionar caballeros (formación compacta)
        double[][] directions = {
                {1, 0},   // Derecha
                {-1, 0},  // Izquierda
                {0, 1},   // Abajo
                {0, -1},  // Arriba
                {1, 1},   // Diagonal inferior derecha
                {-1, 1},  // Diagonal inferior izquierda
                {1, -1},  // Diagonal superior derecha
                {-1, -1}  // Diagonal superior izquierda
        };

        // Primero intentar cerca de otros caballeros existentes
        if (!createdKnights.isEmpty()) {
            ImageView lastKnight = createdKnights.get(createdKnights.size() - 1);
            double lastX = lastKnight.getX();
            double lastY = lastKnight.getY();

            // Intentar posiciones alrededor del último caballero creado
            for (double[] dir : directions) {
                double x = lastX + dir[0] * (knightSize + 3); // Solo 3px de separación entre caballeros
                double y = lastY + dir[1] * (knightSize + 3);

                // Verificar que esté dentro de los límites
                if (x >= 0 && y >= 0 && x + knightSize <= windowWidth && y + knightSize <= windowHeight) {
                    // Verificar colisión con margen mínimo
                    if (!checkCollisionForKnightReduced(x, y, knightSize, knightSize, 2)) {
                        System.out.println("✅ Posición encontrada junto a otro caballero");
                        return new Position(x, y);
                    }
                }
            }
        }

        // Si no hay otros caballeros o no hay espacio, intentar alrededor del cuartel
        for (double[] dir : directions) {
            double x = centerX + dir[0] * distanceFromBarracks - knightSize / 2;
            double y = centerY + dir[1] * distanceFromBarracks - knightSize / 2;

            // Ajustar a límites
            x = Math.max(10, Math.min(x, windowWidth - knightSize - 10));
            y = Math.max(10, Math.min(y, windowHeight - knightSize - 10));

            if (!checkCollisionForKnightReduced(x, y, knightSize, knightSize, 2)) {
                System.out.println("✅ Posición encontrada alrededor del cuartel");
                return new Position(x, y);
            }
        }

        // Si no hay espacio inmediato, buscar un poco más lejos
        for (int ring = 1; ring <= 3; ring++) {
            double currentDistance = distanceFromBarracks + knightSize * ring;

            for (int i = 0; i < 8; i++) {
                double angle = 2 * Math.PI * i / 8;
                double x = centerX + Math.cos(angle) * currentDistance - knightSize / 2;
                double y = centerY + Math.sin(angle) * currentDistance - knightSize / 2;

                x = Math.max(10, Math.min(x, windowWidth - knightSize - 10));
                y = Math.max(10, Math.min(y, windowHeight - knightSize - 10));

                if (!checkCollisionForKnightReduced(x, y, knightSize, knightSize, 1)) {
                    System.out.println("✅ Posición encontrada en anillo " + ring);
                    return new Position(x, y);
                }
            }
        }

        return null;
    }

    /**
     * Verifica colisiones para caballeros con margen reducido
     */
    private boolean checkCollisionForKnightReduced(double x, double y, double width, double height, double margin) {
        Rectangle newBounds = new Rectangle(x + margin, y + margin, width - margin * 2, height - margin * 2);

        // Verificar límites de ventana
        if (x < 0 || y < 0 || x + width > windowWidth || y + height > windowHeight) {
            return true;
        }

        // Verificar colisión con edificios (margen mínimo)
        for (ImageView building : placedBuildings) {
            Rectangle buildingBounds = new Rectangle(
                    building.getX() + margin,
                    building.getY() + margin,
                    building.getFitWidth() - margin * 2,
                    building.getFitHeight() - margin * 2
            );

            if (newBounds.intersects(buildingBounds.getBoundsInLocal())) {
                return true;
            }
        }

        // Verificar colisión con otras unidades (margen mínimo)
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView) {
                ImageView existing = (ImageView) node;

                // Si es una unidad
                if (existing.getFitWidth() == 50 && existing.getFitHeight() == 50) {
                    Rectangle unitBounds = new Rectangle(
                            existing.getX() + margin,
                            existing.getY() + margin,
                            existing.getFitWidth() - margin * 2,
                            existing.getFitHeight() - margin * 2
                    );

                    if (newBounds.intersects(unitBounds.getBoundsInLocal())) {
                        return true;
                    }
                }
            }
        }

        // Verificar colisión con árboles (margen mínimo)
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView) {
                ImageView imageView = (ImageView) node;

                if (imageView.getId() != null && imageView.getId().startsWith("Arbol_")) {
                    Rectangle treeBounds = new Rectangle(
                            imageView.getX() + margin,
                            imageView.getY() + margin,
                            imageView.getFitWidth() - margin * 2,
                            imageView.getFitHeight() - margin * 2
                    );

                    if (newBounds.intersects(treeBounds.getBoundsInLocal())) {
                        return true;
                    }
                }
            }
        }

        // Verificar colisión con minas (margen mínimo)
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView) {
                ImageView imageView = (ImageView) node;

                if (imageView.getId() != null && imageView.getId().startsWith("Mina_")) {
                    Rectangle mineBounds = new Rectangle(
                            imageView.getX() + margin,
                            imageView.getY() + margin,
                            imageView.getFitWidth() - margin * 2,
                            imageView.getFitHeight() - margin * 2
                    );

                    if (newBounds.intersects(mineBounds.getBoundsInLocal())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Crea un caballero en una posición específica
     */
    private void createKnightAtPosition(String unitType, String imageName, double x, double y, double size) {
        try {
            Image unitImage = loadImage(imageName);

            ImageView unitView = new ImageView(unitImage);
            unitView.setFitWidth(size);
            unitView.setFitHeight(size);
            unitView.setPreserveRatio(true);
            unitView.setX(x);
            unitView.setY(y);

            unitView.setId(unitType + "_" + System.currentTimeMillis());
            unitView.setUserData(unitType);


            // Guardar referencia al caballero creado
            createdKnights.add(unitView);

            // Efecto especial para caballero
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(184, 134, 11, 0.8)); // Dorado para caballero
            shadow.setRadius(10);
            shadow.setSpread(0.2);
            unitView.setEffect(shadow);
            unitView.getProperties().put("baseEffect", shadow);

            // Animación de aparición
            FadeTransition fade = new FadeTransition(Duration.millis(500), unitView);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);

            ScaleTransition scale = new ScaleTransition(Duration.millis(500), unitView);
            scale.setFromX(0.3);
            scale.setFromY(0.3);
            scale.setToX(1.0);
            scale.setToY(1.0);

            root.getChildren().add(unitView);

            javafx.animation.ParallelTransition parallel =
                    new javafx.animation.ParallelTransition(fade, scale);
            parallel.play();

            System.out.println("✅ " + unitType + " creado en: (" + (int)x + ", " + (int)y + ")");

            // Hacer el caballero interactivo
            makeKnightInteractive(unitView, unitType);

        } catch (Exception e) {
            System.err.println("❌ Error al crear " + unitType + ": " + e.getMessage());

            // Crear placeholder si falla la imagen
            createKnightPlaceholder(x, y, size, unitType);
        }
    }

    /**
     * Hace un caballero interactivo
     */
    private void makeKnightInteractive(ImageView knightView, String knightType) {
        knightView.setOnMouseClicked(event -> {
            System.out.println("♞ " + knightType + " clickeado - ¡Listo para la batalla!");

            // Efecto especial al hacer clic
            FadeTransition flash = new FadeTransition(Duration.millis(100), knightView);
            flash.setFromValue(1.0);
            flash.setToValue(0.8);
            flash.setAutoReverse(true);
            flash.setCycleCount(4);
            flash.play();
        });

        knightView.setOnMouseEntered(e -> {
            knightView.setCursor(javafx.scene.Cursor.HAND);
            knightView.setScaleX(1.1);
            knightView.setScaleY(1.1);

            // Efecto de resaltado para caballero
            DropShadow highlight = new DropShadow();
            highlight.setColor(Color.rgb(255, 215, 0, 0.9));
            highlight.setRadius(15);
            knightView.setEffect(highlight);
        });

        knightView.setOnMouseExited(e -> {
            knightView.setCursor(javafx.scene.Cursor.DEFAULT);
            knightView.setScaleX(1.0);
            knightView.setScaleY(1.0);

            // Restaurar efecto normal
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(184, 134, 11, 0.8));
            shadow.setRadius(10);
            shadow.setSpread(0.2);
            knightView.setEffect(shadow);
        });
    }

    /**
     * Crea un placeholder para caballero si no se carga la imagen
     */
    private void createKnightPlaceholder(double x, double y, double size, String unitType) {
        // Círculo para la armadura
        javafx.scene.shape.Circle armor = new javafx.scene.shape.Circle(size/2);
        armor.setCenterX(x + size/2);
        armor.setCenterY(y + size/2);
        armor.setFill(Color.rgb(70, 70, 70)); // Gris acero

        // Detalle del escudo
        javafx.scene.shape.Circle shield = new javafx.scene.shape.Circle(size/3);
        shield.setCenterX(x + size/2);
        shield.setCenterY(y + size/2);
        shield.setFill(Color.rgb(30, 30, 30));

        // Cruz en el escudo
        javafx.scene.shape.Line crossVertical = new javafx.scene.shape.Line(
                x + size/2, y + size/2 - size/4,
                x + size/2, y + size/2 + size/4
        );
        crossVertical.setStroke(Color.SILVER);
        crossVertical.setStrokeWidth(2);

        javafx.scene.shape.Line crossHorizontal = new javafx.scene.shape.Line(
                x + size/2 - size/4, y + size/2,
                x + size/2 + size/4, y + size/2
        );
        crossHorizontal.setStroke(Color.SILVER);
        crossHorizontal.setStrokeWidth(2);

        Pane knight = new Pane(armor, shield, crossVertical, crossHorizontal);
        knight.setId(unitType + "_placeholder_" + System.currentTimeMillis());

        // Guardar referencia al placeholder
        ImageView placeholderView = new ImageView();
        placeholderView.setId(knight.getId());
        createdKnights.add(placeholderView);

        // Hacer interactivo
        knight.setOnMouseClicked(e -> System.out.println("♞ Caballero placeholder clickeado"));
        knight.setOnMouseEntered(e -> {
            knight.setCursor(javafx.scene.Cursor.HAND);
            knight.setScaleX(1.05);
            knight.setScaleY(1.05);
        });
        knight.setOnMouseExited(e -> {
            knight.setScaleX(1.0);
            knight.setScaleY(1.0);
        });

        root.getChildren().add(knight);
    }

    /**
     * Muestra advertencia de recursos insuficientes para crear caballero
     */
    private void showInsufficientResourcesForKnight() {
        Stage warningStage = new Stage();
        warningStage.initModality(Modality.APPLICATION_MODAL);
        warningStage.initStyle(StageStyle.TRANSPARENT);
        warningStage.setTitle("Recursos insuficientes");

        VBox warningPanel = new VBox(15);
        warningPanel.setPadding(new Insets(25, 30, 25, 30));
        warningPanel.setAlignment(Pos.CENTER);
        warningPanel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.50); " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #c0392b; " + // Rojo para cuartel
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.5, 0, 2);"
        );

        Label warningIcon = new Label("⚔");
        warningIcon.setStyle("-fx-font-size: 36px; -fx-padding: 0 0 5 0;");

        VBox messageContainer = new VBox(5);
        messageContainer.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Recursos insuficientes");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #c0392b;");

        Label detailLabel = new Label("Necesitas 50 Oro \npara crear un Caballero");
        detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #000000; -fx-text-alignment: center;");
        detailLabel.setWrapText(true);

        messageContainer.getChildren().addAll(titleLabel, detailLabel);

        Button okButton = new Button("Entendido");
        okButton.setPrefWidth(150);
        okButton.setPrefHeight(38);
        okButton.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.5); " +
                        "-fx-background-radius: 6; " +
                        "-fx-border-color: #c0392b; " + // Rojo para cuartel
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 6; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold;"
        );

        okButton.setOnMouseEntered(e -> {
            okButton.setStyle(
                    "-fx-background-color: rgba(236, 240, 241, 0.5); " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-color: #e74c3c; " +
                            "-fx-border-width: 2.5; " +
                            "-fx-border-radius: 6; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: #2c3e50; " +
                            "-fx-font-size: 12px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-effect: dropshadow(gaussian, rgba(231, 76, 60, 0.3), 5, 0.5, 0, 1);"
            );
        });

        okButton.setOnMouseExited(e -> {
            okButton.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.5); " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-color: #c0392b; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 6; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: #2c3e50; " +
                            "-fx-font-size: 12px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-effect: null;"
            );
        });

        okButton.setOnAction(e -> warningStage.close());

        warningPanel.getChildren().addAll(warningIcon, messageContainer, okButton);

        StackPane rootPane = new StackPane(warningPanel);
        rootPane.setStyle("-fx-background-color: transparent;");
        rootPane.setAlignment(Pos.CENTER);

        Scene warningScene = new Scene(rootPane, 300, 250);
        warningScene.setFill(Color.TRANSPARENT);

        warningStage.initOwner(root.getScene().getWindow());
        warningStage.setScene(warningScene);
        warningStage.setResizable(false);
        warningStage.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}