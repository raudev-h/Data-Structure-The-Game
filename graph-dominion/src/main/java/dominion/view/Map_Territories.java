package dominion.view;

import dominion.core.AttackResult;
import dominion.core.GameControler;
import dominion.core.GameMap;
import dominion.model.players.Player;
import dominion.model.territories.Territory;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.Set;

public class Map_Territories extends Pane {
    private GameApp gameApp; // Referencia a la pantalla principal
    private GameControler gameControler;
    private GameMap gameMap;
    private Player principalPlayer;

    // Tamaños de ventana (se ajustarán automáticamente)
    private double windowWidth;
    private double windowHeight;

    // Imágenes
    private ImageView backgroundMap;
    private ImageView currentTerritory;
    private ImageView enemyTerritory1;
    private ImageView enemyTerritory2;

    // Referencia al territorio conquistado
    private ImageView conqueredTerritory = null;
    private int conqueredTerritoryNumber = -1;

    // Panel de información que se muestra al pasar el mouse
    private VBox defenseInfoPanel;

    // Referencia al territorio actual sobre el que está el mouse
    private ImageView currentHoveredTerritory;
    private int currentHoveredTerritoryNumber;

    // Panel de confirmación actual
    private Pane currentConfirmationPanel;

    // Overlay para victoria
    private StackPane victoryOverlay;
    private boolean isVictoryShowing = false;


    public Map_Territories(GameApp gameApp, double width, double height) {
        this.gameApp = gameApp;
        this.windowWidth = width;
        this.windowHeight = height;
        this.gameMap = gameApp.getGameMap();
        this.principalPlayer = gameApp.getActualPlayer();
        this.gameControler = gameApp.getGameControler();

        // Cargar territorios ya conquistados
        loadConqueredTerritories();

        // Configurar el tamaño
        setPrefSize(windowWidth, windowHeight);

        // Hacer que ocupe toda el área
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Inicializar
        initialize();
    }

    private void initialize() {
        // Añadir la imagen de fondo del mapa
        setupMapBackground();

        // Añadir mapa de territorios (capa superior)
        setupTerritoriesMap();

        // Añadir territorios interactivos
        setupTerritories();

        // Añadir panel de información
        setupInfoPanel();

        // Añadir botón de volver
        setupBackButton();
    }

    private void setupMapBackground() {
        try {
            // Cargar la imagen específica del mapa de fondo
            Image mapBackgroundImage = new Image("file:src/main/resources/images/map_background (4).png");
            ImageView mapBackground = new ImageView(mapBackgroundImage);

            mapBackground.setPreserveRatio(true);
            mapBackground.setSmooth(true);

            // Ajustar tamaño para cubrir todo el área
            mapBackground.fitWidthProperty().bind(widthProperty());
            mapBackground.fitHeightProperty().bind(heightProperty());
            mapBackground.setPreserveRatio(false);

            // Posicionar
            mapBackground.setX(0);
            mapBackground.setY(0);

            // Listener para reajustar cuando cambie el tamaño
            widthProperty().addListener((obs, oldVal, newVal) -> {
                if (mapBackground.isPreserveRatio()) {
                    adjustBackgroundPosition(mapBackground);
                }
            });

            heightProperty().addListener((obs, oldVal, newVal) -> {
                if (mapBackground.isPreserveRatio()) {
                    adjustBackgroundPosition(mapBackground);
                }
            });

            // Añadir al principio (fondo)
            getChildren().add(0, mapBackground);

            // Inicializar posición después de cargar la imagen
            mapBackground.imageProperty().addListener((obs, oldImg, newImg) -> {
                if (newImg != null) {
                    adjustBackgroundPosition(mapBackground);
                }
            });

            // Si la imagen ya está cargada, ajustar posición
            if (mapBackgroundImage.getProgress() == 1.0) {
                adjustBackgroundPosition(mapBackground);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al cargar el fondo del mapa: " + e.getMessage());
            javafx.scene.shape.Rectangle fallbackBackground = new javafx.scene.shape.Rectangle();
            fallbackBackground.widthProperty().bind(widthProperty());
            fallbackBackground.heightProperty().bind(heightProperty());
            fallbackBackground.setFill(Color.rgb(40, 45, 70));

            getChildren().add(0, fallbackBackground);
        }
    }


    private void adjustBackgroundPosition(ImageView mapBackground) {
        if (mapBackground.getImage() == null) return;

        double imageWidth = mapBackground.getImage().getWidth();
        double imageHeight = mapBackground.getImage().getHeight();
        double aspectRatio = imageWidth / imageHeight;
        double containerWidth = getWidth();
        double containerHeight = getHeight();
        double containerAspectRatio = containerWidth / containerHeight;

        if (mapBackground.isPreserveRatio()) {
            if (aspectRatio > containerAspectRatio) {
                mapBackground.setFitWidth(containerWidth);
                mapBackground.setFitHeight(containerWidth / aspectRatio);
                mapBackground.setX(0);
                mapBackground.setY((containerHeight - (containerWidth / aspectRatio)) / 2);
            } else {
                mapBackground.setFitHeight(containerHeight);
                mapBackground.setFitWidth(containerHeight * aspectRatio);
                mapBackground.setX((containerWidth - (containerHeight * aspectRatio)) / 2);
                mapBackground.setY(0);
            }
        } else {
            mapBackground.setFitWidth(containerWidth);
            mapBackground.setFitHeight(containerHeight);
            mapBackground.setX(0);
            mapBackground.setY(0);
        }
    }

    private void setupTerritoriesMap() {
        try {
            Image mapImage = new Image("file:src/main/resources/images/mapaTerritorios.png");
            backgroundMap = new ImageView(mapImage);

            backgroundMap.setPreserveRatio(true);
            backgroundMap.setSmooth(true);
            backgroundMap.fitWidthProperty().bind(widthProperty());
            backgroundMap.fitHeightProperty().bind(heightProperty());
            backgroundMap.setX(0);
            backgroundMap.setY(0);
            backgroundMap.setOpacity(0.85);

            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.7));
            shadow.setRadius(30);
            shadow.setSpread(0.1);
            backgroundMap.setEffect(shadow);

            getChildren().add(1, backgroundMap);

            widthProperty().addListener((obs, oldVal, newVal) -> {
                adjustTerritoryPositions();
            });

            heightProperty().addListener((obs, oldVal, newVal) -> {
                adjustTerritoryPositions();
            });

        } catch (Exception e) {
            System.err.println("❌ Error al cargar el mapa de territorios: " + e.getMessage());
        }
    }

    private void setupTerritories() {
        try {
            // ========== TERRITORIO ACTUAL ==========
            Image currentImage = new Image("file:src/main/resources/images/territorioActual.png");
            currentTerritory = new ImageView(currentImage);

            double territorySize = windowWidth * 0.1;
            currentTerritory.setFitWidth(territorySize);
            currentTerritory.setFitHeight(territorySize);
            currentTerritory.setPreserveRatio(true);

            DropShadow glow = new DropShadow();
            glow.setColor(Color.rgb(0, 255, 0, 0.8));
            glow.setRadius(20);
            glow.setSpread(0.3);
            currentTerritory.setEffect(glow);

            Label currentLabel = createTerritoryLabel("Tu Territorio");

            // ========== TERRITORIOS ENEMIGOS ==========
            Image enemyImage = new Image("file:src/main/resources/images/territorioEnemigo.png");

            // Enemigo 1
            enemyTerritory1 = new ImageView(enemyImage);
            enemyTerritory1.setFitWidth(territorySize * 0.9);
            enemyTerritory1.setFitHeight(territorySize * 0.9);
            enemyTerritory1.setPreserveRatio(true);

            // Enemigo 2
            enemyTerritory2 = new ImageView(enemyImage);
            enemyTerritory2.setFitWidth(territorySize * 1.0);
            enemyTerritory2.setFitHeight(territorySize * 1.0);
            enemyTerritory2.setPreserveRatio(true);

            DropShadow enemyGlow = new DropShadow();
            enemyGlow.setColor(Color.rgb(255, 0, 0, 0.8));
            enemyGlow.setRadius(15);
            enemyGlow.setSpread(0.2);
            enemyTerritory1.setEffect(enemyGlow);
            enemyTerritory2.setEffect(enemyGlow);

            Label enemyLabel1 = createTerritoryLabel("Nivel 1");
            Label enemyLabel2 = createTerritoryLabel("Nivel Final");

            // VERIFICAR si ya están conquistados ANTES de hacerlos interactivos
            Set<Integer> conquered = gameApp.getConqueredTerritories();

            if (!conquered.contains(1)) {
                makeTerritoryInteractive(enemyTerritory1, "Nivel 1", 1);
            } else {
                markTerritoryAsConquered(enemyTerritory1, 1);
            }

            if (!conquered.contains(2)) {
                makeTerritoryInteractive(enemyTerritory2, "Nivel Final", 2);
            } else {
                markTerritoryAsConquered(enemyTerritory2, 2);
            }

            getChildren().addAll(
                    currentTerritory, currentLabel,
                    enemyTerritory1, enemyLabel1,
                    enemyTerritory2, enemyLabel2
            );

            adjustTerritoryPositions();

        } catch (Exception e) {
            System.err.println("❌ Error al cargar territorios: " + e.getMessage());
            createPlaceholderTerritories();
        }
    }

    private void adjustTerritoryPositions() {
        double currentWidth = getWidth();
        double currentHeight = getHeight();

        double territorySize = windowWidth * 0.08;

        double startX = currentWidth * 0.15;
        double startY = currentHeight * 0.2;
        double spacingX = currentWidth * 0.2;
        double spacingY = currentHeight * 0.15;

        // Posición 1: Tu territorio
        if (currentTerritory != null) {
            currentTerritory.setFitWidth(territorySize);
            currentTerritory.setFitHeight(territorySize);
            currentTerritory.setX(startX);
            currentTerritory.setY(startY);

            if (getChildren().indexOf(currentTerritory) + 1 < getChildren().size()) {
                Label label = (Label) getChildren().get(getChildren().indexOf(currentTerritory) + 1);
                label.setLayoutX(currentTerritory.getX() + currentTerritory.getFitWidth() / 2 - 40);
                label.setLayoutY(currentTerritory.getY() + currentTerritory.getFitHeight() + 10);
            }
        }

        // Posición 2: Enemigo 1
        if (enemyTerritory1 != null) {
            enemyTerritory1.setFitWidth(territorySize * 0.9);
            enemyTerritory1.setFitHeight(territorySize * 0.9);
            enemyTerritory1.setX(startX + spacingX);
            enemyTerritory1.setY(startY + spacingY);

            if (getChildren().indexOf(enemyTerritory1) + 1 < getChildren().size()) {
                Label label = (Label) getChildren().get(getChildren().indexOf(enemyTerritory1) + 1);
                label.setLayoutX(enemyTerritory1.getX() + enemyTerritory1.getFitWidth() / 2 - 50);
                label.setLayoutY(enemyTerritory1.getY() + enemyTerritory1.getFitHeight() + 10);
            }
        }

        // Posición 3: Enemigo 2
        if (enemyTerritory2 != null) {
            enemyTerritory2.setFitWidth(territorySize);
            enemyTerritory2.setFitHeight(territorySize);
            enemyTerritory2.setX(startX + (spacingX * 2));
            enemyTerritory2.setY(startY + (spacingY * 2));

            if (getChildren().indexOf(enemyTerritory2) + 1 < getChildren().size()) {
                Label label = (Label) getChildren().get(getChildren().indexOf(enemyTerritory2) + 1);
                label.setLayoutX(enemyTerritory2.getX() + enemyTerritory2.getFitWidth() / 2 - 55);
                label.setLayoutY(enemyTerritory2.getY() + enemyTerritory2.getFitHeight() + 10);
            }
        }
    }

    private Label createTerritoryLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-color: rgba(0, 0, 0, 0.7); " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 5 12;"
        );
        return label;
    }

    private void makeTerritoryInteractive(ImageView territory, String name, int territoryNumber) {
        // VERIFICAR primero si ya está conquistado
        if (gameApp.isTerritoryConquered(territoryNumber)) {
            System.out.println("⚠️ Territorio " + territoryNumber + " ya está conquistado, no hacer interactivo");
            return;
        }

        territory.setOnMouseClicked(e -> {
            if (gameApp.isTerritoryConquered(territoryNumber)) {
                System.out.println("⚠️ Este territorio ya fue conquistado");
                return;
            }

            Territory actual = gameApp.getGameMap().getTerritories().get(territoryNumber);
            boolean puedeAtacar = gameMap.playerCanAttack(principalPlayer, actual);

            if (puedeAtacar) {
                System.out.println("⚔ Atacando " + name + "...");
                AttackResult attackResult = gameControler.handleAttack(principalPlayer, actual);
                System.out.println("Attack Result: " + attackResult + " ---------");

                if (attackResult.equals(AttackResult.VICTORY)) {
                    // ¡Victoria! Verificar si es el territorio final (territorio 2)
                    if (territoryNumber == 2) { // Terriotrio final/jefe
                        // Mostrar pantalla de victoria final especial
                        showFinalVictoryScreen(territoryNumber, name);
                    } else {
                        // Mostrar pantalla de victoria normal
                        showVictoryScreen(territory, territoryNumber, name);
                    }

                    // Cambiar el territorio a verde (propio)
                    conquerTerritory(territory, territoryNumber);

                    // CALCULAR Y ELIMINAR CABALLEROS MUERTOS DEL FRONTEND
                    int deadKnights = principalPlayer.calculateDeadKnights(actual);
                    if (deadKnights > 0) {
                        System.out.println("💀 " + deadKnights + " caballeros murieron en batalla");

                        // Eliminar caballeros del frontend
                        gameApp.removeKnightsFromFrontend(deadKnights);

                        // Mostrar mensaje de bajas
                        showCasualtyMessage(deadKnights);
                    }

                } else if (attackResult.equals(AttackResult.DEFEAT)) {
                    System.out.println("💀 DERROTA TOTAL - Mostrando pantalla de derrota");

                    // Mostrar pantalla de derrota
                    showDefeatScreen();

                    // Eliminar TODOS los caballeros del frontend
                    int totalKnights = principalPlayer.getKnights().size();
                    if (totalKnights > 0) {
                        System.out.println("💀 Todos los " + totalKnights + " caballeros murieron");
                        gameApp.removeKnightsFromFrontend(totalKnights);
                    }

                    // Deshabilitar todos los territorios enemigos (no se puede seguir atacando)
                    disableAllEnemyTerritories();
                }

                FadeTransition flash = new FadeTransition(Duration.millis(150), territory);
                flash.setFromValue(1.0);
                flash.setToValue(0.6);
                flash.setAutoReverse(true);
                flash.setCycleCount(2);
                flash.play();

                if (attackResult.equals(AttackResult.VICTORY) || attackResult.equals(AttackResult.DEFEAT)) {
                    // Sincronizar caballeros después de la batalla
                    Platform.runLater(() -> {
                        // Pequeño retraso para asegurar que el backend se actualizó
                        Timeline syncDelay = new Timeline(
                                new KeyFrame(Duration.millis(500), ex -> gameApp.syncKnightsAfterBattle())
                        );
                        syncDelay.play();
                    });
                }
            } else {
                showNotAdjacentAlert();

                javafx.animation.Timeline errorFlash = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(Duration.millis(0),
                                ev -> territory.setEffect(createRedGlowEffect())),
                        new javafx.animation.KeyFrame(Duration.millis(100),
                                ev -> territory.setEffect(createNormalGlowEffect())),
                        new javafx.animation.KeyFrame(Duration.millis(200),
                                ev -> territory.setEffect(createRedGlowEffect())),
                        new javafx.animation.KeyFrame(Duration.millis(300),
                                ev -> territory.setEffect(createNormalGlowEffect()))
                );
                errorFlash.setCycleCount(2);
                errorFlash.play();
            }
        });

        territory.setOnMouseEntered(e -> {
            territory.setCursor(javafx.scene.Cursor.HAND);
            territory.setScaleX(1.15);
            territory.setScaleY(1.15);

            DropShadow highlight = new DropShadow();
            highlight.setColor(Color.rgb(255, 255, 100, 0.9));
            highlight.setRadius(25);
            territory.setEffect(highlight);

            showSimpleDefenseInfo(territory, territoryNumber);
        });

        territory.setOnMouseExited(e -> {
            territory.setCursor(javafx.scene.Cursor.DEFAULT);
            territory.setScaleX(1.0);
            territory.setScaleY(1.0);

            // Si es un territorio conquistado, mantenerlo verde
            if (conqueredTerritory != null && territory == conqueredTerritory) {
                DropShadow greenGlow = new DropShadow();
                greenGlow.setColor(Color.rgb(0, 255, 0, 0.8));
                greenGlow.setRadius(20);
                greenGlow.setSpread(0.3);
                territory.setEffect(greenGlow);
            } else {
                // Si no, mantener el rojo de enemigo
                DropShadow enemyGlow = new DropShadow();
                enemyGlow.setColor(Color.rgb(255, 0, 0, 0.8));
                enemyGlow.setRadius(15);
                territory.setEffect(enemyGlow);
            }

            hideSimpleDefenseInfo();
        });
    }

    /**
     * Muestra mensaje de bajas después de una batalla
     */
    private void showCasualtyMessage(int deadKnights) {
        Platform.runLater(() -> {
            // Crear mensaje flotante
            Label casualtyLabel = new Label("💀 " + deadKnights + " caballeros caídos");
            casualtyLabel.setStyle(
                    "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: #e74c3c; " +
                            "-fx-background-color: rgba(0, 0, 0, 0.7); " +
                            "-fx-background-radius: 10; " +
                            "-fx-padding: 8 15;"
            );

            // Posicionar en el centro inferior
            double x = (getWidth() - 150) / 2;
            double y = getHeight() * 0.7;
            casualtyLabel.setLayoutX(x);
            casualtyLabel.setLayoutY(y);

            getChildren().add(casualtyLabel);

            // Animación de aparición y desaparición
            casualtyLabel.setOpacity(0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), casualtyLabel);
            fadeIn.setToValue(1.0);

            TranslateTransition moveUp = new TranslateTransition(Duration.seconds(2), casualtyLabel);
            moveUp.setByY(-50);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), casualtyLabel);
            fadeOut.setToValue(0);
            fadeOut.setDelay(Duration.seconds(1.5));

            fadeOut.setOnFinished(e -> getChildren().remove(casualtyLabel));

            ParallelTransition animation = new ParallelTransition(fadeIn, moveUp);
            SequentialTransition sequence = new SequentialTransition(animation, fadeOut);
            sequence.play();
        });
    }

    /**
     * Muestra la pantalla de victoria con overlay oscuro
     */
    private void showVictoryScreen(ImageView conqueredTerritory, int territoryNumber, String territoryName) {
        if (isVictoryShowing) return;
        isVictoryShowing = true;

        // Crear overlay oscuro (85% opacidad)
        victoryOverlay = new StackPane();
        victoryOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");
        victoryOverlay.setPrefSize(getWidth(), getHeight());
        victoryOverlay.setMouseTransparent(false); // Permitir interacción con el botón

        // Panel de victoria
        VBox victoryPanel = createVictoryPanel(territoryName);
        victoryPanel.setOpacity(0);
        victoryPanel.setScaleX(0.9);
        victoryPanel.setScaleY(0.9);

        victoryOverlay.getChildren().add(victoryPanel);
        StackPane.setAlignment(victoryPanel, Pos.CENTER);

        // Añadir overlay a la escena
        getChildren().add(victoryOverlay);
        victoryOverlay.toFront();

        // Animación de entrada
        FadeTransition overlayFade = new FadeTransition(Duration.millis(500), victoryOverlay);
        overlayFade.setFromValue(0);
        overlayFade.setToValue(1);

        FadeTransition panelFade = new FadeTransition(Duration.millis(400), victoryPanel);
        panelFade.setFromValue(0);
        panelFade.setToValue(1);
        panelFade.setDelay(Duration.millis(100));

        ScaleTransition panelScale = new ScaleTransition(Duration.millis(400), victoryPanel);
        panelScale.setFromX(0.9);
        panelScale.setFromY(0.9);
        panelScale.setToX(1.0);
        panelScale.setToY(1.0);
        panelScale.setDelay(Duration.millis(100));
        panelScale.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        ParallelTransition entrance = new ParallelTransition(overlayFade, panelFade, panelScale);
        entrance.play();

        System.out.println("🎉 Mostrando pantalla de victoria para territorio: " + territoryName);
    }

    /**
     * Crea el panel de victoria con la imagen y botón
     */
    private VBox createVictoryPanel(String territoryName) {
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(30, 40, 30, 40));
        panel.setMaxWidth(400);
        panel.setMaxHeight(500);

        // MISMO estilo EXACTO que el TownHall (50% opacidad)
        panel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.50); " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #2ecc71; " + // Verde para victoria
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0.5, 0, 3);"
        );

        try {
            // Cargar imagen de victoria
            Image victoryImage = new Image("file:src/main/resources/images/Victory.png");
            ImageView victoryImageView = new ImageView(victoryImage);
            victoryImageView.setPreserveRatio(true);
            victoryImageView.setFitWidth(250);

            // Título
            Label titleLabel = new Label("¡VICTORIA!");
            titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

            // Separador elegante
            Region separator = new Region();
            separator.setPrefHeight(2);
            separator.setPrefWidth(200);
            separator.setStyle("-fx-background-color: linear-gradient(to right, transparent, #27ae60, transparent);");

            // Botón para cerrar el mapa
            Button closeMapButton = createVictoryButton("CONTINUAR");
            closeMapButton.setOnAction(e -> {
                hideVictoryScreen();

            });

            panel.getChildren().addAll(
                    victoryImageView,
                    titleLabel,
                    separator,
                    closeMapButton
            );

        } catch (Exception ex) {
            System.err.println("❌ Error al cargar imagen de victoria: " + ex.getMessage());

            // Placeholder si no se carga la imagen
            Label victoryText = new Label("¡VICTORIA!\nHas conquistado " + territoryName + "!");
            victoryText.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-text-alignment: center;");
            victoryText.setWrapText(true);

            Button closeMapButton = createVictoryButton("CONTINUAR");
            closeMapButton.setOnAction(e -> {
                hideVictoryScreen();
                closeMap();
            });

            panel.getChildren().addAll(victoryText, closeMapButton);
        }

        return panel;
    }

    /**
     * Crea botón para pantalla de victoria con el MISMO estilo que GameApp
     */
    private Button createVictoryButton(String text) {
        HBox buttonContent = new HBox(8);
        buttonContent.setAlignment(Pos.CENTER);
        buttonContent.setPadding(new Insets(10, 25, 10, 25));

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        buttonContent.getChildren().add(textLabel);

        Button button = new Button();
        button.setGraphic(buttonContent);
        button.setPrefWidth(200);
        button.setPrefHeight(50);

        // ESTILO BASE con 50% opacidad igual que GameApp
        String baseStyle =
                "-fx-background-color: rgba(255, 255, 255, 0.50); " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #2ecc71; " + // Verde para victoria
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #2c3e50;";

        button.setStyle(baseStyle);

        // EFECTO HOVER
        button.setOnMouseEntered(e -> {
            String hoverStyle =
                    "-fx-background-color: rgba(236, 240, 241, 0.50); " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: #27ae60; " + // Verde más oscuro
                            "-fx-border-width: 2.5; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(46, 204, 113, 0.4), 8, 0.5, 0, 2);";

            button.setStyle(hoverStyle);
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });

        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle);
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        // Efecto al presionar
        button.setOnMousePressed(e -> {
            button.setStyle(
                    "-fx-background-color: rgba(220, 220, 220, 0.50); " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: #229954; " + // Verde más oscuro aún
                            "-fx-border-width: 3; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: #2c3e50;"
            );
        });

        button.setOnMouseReleased(e -> {
            button.setStyle(baseStyle);
        });

        return button;
    }

    /**
     * Oculta la pantalla de victoria con animación
     */
    private void hideVictoryScreen() {
        if (victoryOverlay == null) return;

        // Obtener el panel de victoria
        VBox victoryPanel = null;
        for (Node node : victoryOverlay.getChildren()) {
            if (node instanceof VBox) {
                victoryPanel = (VBox) node;
                break;
            }
        }

        // Animación de salida
        if (victoryPanel != null) {
            FadeTransition panelFade = new FadeTransition(Duration.millis(300), victoryPanel);
            panelFade.setToValue(0);

            ScaleTransition panelScale = new ScaleTransition(Duration.millis(300), victoryPanel);
            panelScale.setToX(0.9);
            panelScale.setToY(0.9);

            FadeTransition overlayFade = new FadeTransition(Duration.millis(400), victoryOverlay);
            overlayFade.setToValue(0);
            overlayFade.setDelay(Duration.millis(100));

            overlayFade.setOnFinished(e -> {
                getChildren().remove(victoryOverlay);
                victoryOverlay = null;
                isVictoryShowing = false;
            });

            ParallelTransition exit = new ParallelTransition(panelFade, panelScale, overlayFade);
            exit.play();
        } else {
            getChildren().remove(victoryOverlay);
            victoryOverlay = null;
            isVictoryShowing = false;
        }
    }

    /**
     * Muestra la pantalla de derrota con overlay oscuro
     */
    private void showDefeatScreen() {
        if (isVictoryShowing) return;
        isVictoryShowing = true;

        // Crear overlay oscuro que cubra TODA la pantalla
        victoryOverlay = new StackPane();

        // IMPORTANTE: Usar fondo negro con 85% opacidad para efecto anochecer
        victoryOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");

        // CRÍTICO: Asegurar que cubra toda el área visible
        victoryOverlay.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        victoryOverlay.setPrefSize(getWidth(), getHeight());
        victoryOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Vincular tamaño al contenedor principal para que se ajuste automáticamente
        victoryOverlay.prefWidthProperty().bind(widthProperty());
        victoryOverlay.prefHeightProperty().bind(heightProperty());

        // IMPORTANTE: Permitir interacción con el botón pero NO bloquear completamente
        victoryOverlay.setMouseTransparent(false);

        // Panel de derrota
        VBox defeatPanel = createDefeatPanel();
        defeatPanel.setOpacity(0);
        defeatPanel.setScaleX(0.9);
        defeatPanel.setScaleY(0.9);

        victoryOverlay.getChildren().add(defeatPanel);
        StackPane.setAlignment(defeatPanel, Pos.CENTER);

        // Añadir overlay a la escena
        getChildren().add(victoryOverlay);
        victoryOverlay.toFront();

        // Forzar layout para asegurar que cubre toda el área
        victoryOverlay.layout();

        // Animación de entrada
        victoryOverlay.setOpacity(0); // Comienza transparente

        FadeTransition overlayFade = new FadeTransition(Duration.millis(500), victoryOverlay);
        overlayFade.setToValue(1.0);

        FadeTransition panelFade = new FadeTransition(Duration.millis(400), defeatPanel);
        panelFade.setFromValue(0);
        panelFade.setToValue(1);
        panelFade.setDelay(Duration.millis(100));

        ScaleTransition panelScale = new ScaleTransition(Duration.millis(400), defeatPanel);
        panelScale.setFromX(0.9);
        panelScale.setFromY(0.9);
        panelScale.setToX(1.0);
        panelScale.setToY(1.0);
        panelScale.setDelay(Duration.millis(100));
        panelScale.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        ParallelTransition entrance = new ParallelTransition(overlayFade, panelFade, panelScale);
        entrance.play();

        System.out.println("💀 Mostrando pantalla de derrota");
        System.out.println("📏 Tamaño overlay: " + getWidth() + "x" + getHeight());
    }

    /**
     * Crea el panel de derrota con la imagen y botón para salir al menú
     */
    private VBox createDefeatPanel() {
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(30, 40, 30, 40));
        panel.setMaxWidth(400);
        panel.setMaxHeight(500);

        // MISMO estilo EXACTO pero con rojo para derrota
        panel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.50); " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #e74c3c; " + // Rojo para derrota
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0.5, 0, 3);"
        );

        try {
            // Cargar imagen de derrota
            Image defeatImage = new Image("file:src/main/resources/images/Derrota.png");
            ImageView defeatImageView = new ImageView(defeatImage);
            defeatImageView.setPreserveRatio(true);
            defeatImageView.setFitWidth(250);

            // Título
            Label titleLabel = new Label("¡DERROTA!");
            titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

            // Mensaje de derrota
            Label messageLabel = new Label("Tu ejército ha sido derrotado\n¡Inténtalo de nuevo!");
            messageLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-text-alignment: center;");
            messageLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            // Separador elegante (rojo)
            Region separator = new Region();
            separator.setPrefHeight(2);
            separator.setPrefWidth(200);
            separator.setStyle("-fx-background-color: linear-gradient(to right, transparent, #e74c3c, transparent);");

            // Botón para SALIR AL MENÚ (cierra toda la aplicación)
            Button exitToMenuButton = createDefeatButton("SALIR AL MENÚ");
            exitToMenuButton.setOnAction(e -> {
                System.out.println("🚪 Saliendo al menú principal...");
                hideDefeatScreen();

                // Cerrar la ventana principal de GameApp
                if (gameApp != null) {
                    Platform.runLater(() -> {
                        // Asegurar que GameApp tenga un método para regresar al menú
                        if (gameApp.getMenuManager() != null) {
                            gameApp.getMenuManager().returnToMenu();
                        } else {
                            // Si no hay MenuManager, cerrar directamente
                            Stage mainStage = (Stage) gameApp.getSceneContainer().getScene().getWindow();
                            if (mainStage != null) {
                                mainStage.close();
                            }
                        }
                    });
                }
            });

            panel.getChildren().addAll(
                    defeatImageView,
                    titleLabel,
                    messageLabel,
                    separator,
                    exitToMenuButton
            );

        } catch (Exception e) {
            System.err.println("❌ Error al cargar imagen de derrota: " + e.getMessage());

            // Placeholder si no se carga la imagen
            Label defeatText = new Label("¡DERROTA!\nTu ejército ha sido derrotado\n¡Inténtalo de nuevo!");
            defeatText.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-text-alignment: center;");
            defeatText.setWrapText(true);
            defeatText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            Button exitToMenuButton = createDefeatButton("SALIR AL MENÚ");
            exitToMenuButton.setOnAction(ex -> {
                hideDefeatScreen();
                // Cerrar la ventana principal
                Stage mainStage = (Stage) gameApp.getSceneContainer().getScene().getWindow();
                if (mainStage != null) {
                    mainStage.close();
                }
            });

            panel.getChildren().addAll(defeatText, exitToMenuButton);
        }

        return panel;
    }

    /**
     * Crea botón para pantalla de derrota
     */
    private Button createDefeatButton(String text) {
        HBox buttonContent = new HBox(8);
        buttonContent.setAlignment(Pos.CENTER);
        buttonContent.setPadding(new Insets(10, 25, 10, 25));

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        buttonContent.getChildren().add(textLabel);

        Button button = new Button();
        button.setGraphic(buttonContent);
        button.setPrefWidth(200);
        button.setPrefHeight(50);

        // ESTILO BASE con 50% opacidad igual que GameApp
        String baseStyle =
                "-fx-background-color: rgba(255, 255, 255, 0.50); " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #e74c3c; " + // Rojo para derrota
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #2c3e50;";

        button.setStyle(baseStyle);

        // EFECTO HOVER
        button.setOnMouseEntered(e -> {
            String hoverStyle =
                    "-fx-background-color: rgba(236, 240, 241, 0.50); " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: #c0392b; " + // Rojo más oscuro
                            "-fx-border-width: 2.5; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(192, 57, 43, 0.4), 8, 0.5, 0, 2);";

            button.setStyle(hoverStyle);
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });

        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle);
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        // Efecto al presionar
        button.setOnMousePressed(e -> {
            button.setStyle(
                    "-fx-background-color: rgba(220, 220, 220, 0.50); " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: #a93226; " + // Rojo aún más oscuro
                            "-fx-border-width: 3; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: #2c3e50;"
            );
        });

        button.setOnMouseReleased(e -> {
            button.setStyle(baseStyle);
        });

        return button;
    }

    /**
     * Oculta la pantalla de derrota (similar a hideVictoryScreen)
     */
    private void hideDefeatScreen() {
        if (victoryOverlay == null) return;

        // Obtener el panel de derrota
        VBox defeatPanel = null;
        for (Node node : victoryOverlay.getChildren()) {
            if (node instanceof VBox) {
                defeatPanel = (VBox) node;
                break;
            }
        }

        // Animación de salida
        if (defeatPanel != null) {
            FadeTransition panelFade = new FadeTransition(Duration.millis(300), defeatPanel);
            panelFade.setToValue(0);

            ScaleTransition panelScale = new ScaleTransition(Duration.millis(300), defeatPanel);
            panelScale.setToX(0.9);
            panelScale.setToY(0.9);

            FadeTransition overlayFade = new FadeTransition(Duration.millis(400), victoryOverlay);
            overlayFade.setToValue(0);
            overlayFade.setDelay(Duration.millis(100));

            overlayFade.setOnFinished(e -> {
                getChildren().remove(victoryOverlay);
                victoryOverlay = null;
                isVictoryShowing = false;
            });

            ParallelTransition exit = new ParallelTransition(panelFade, panelScale, overlayFade);
            exit.play();
        } else {
            getChildren().remove(victoryOverlay);
            victoryOverlay = null;
            isVictoryShowing = false;
        }
    }

    /**
     * Deshabilita todos los territorios enemigos después de una derrota
     */
    private void disableAllEnemyTerritories() {
        // Deshabilitar territorio 1
        if (enemyTerritory1 != null) {
            enemyTerritory1.setOnMouseClicked(null);
            enemyTerritory1.setOnMouseEntered(null);
            enemyTerritory1.setOnMouseExited(null);
            enemyTerritory1.setCursor(javafx.scene.Cursor.DEFAULT);
            enemyTerritory1.setOpacity(0.5); // Hacerlo semi-transparente
        }

        // Deshabilitar territorio 2
        if (enemyTerritory2 != null) {
            enemyTerritory2.setOnMouseClicked(null);
            enemyTerritory2.setOnMouseEntered(null);
            enemyTerritory2.setOnMouseExited(null);
            enemyTerritory2.setCursor(javafx.scene.Cursor.DEFAULT);
            enemyTerritory2.setOpacity(0.5);
        }

        System.out.println("🚫 Todos los territorios enemigos deshabilitados");
    }
    /**
     * Conquista un territorio (cambia su apariencia)
     */
    /**
     * Conquista un territorio (cambia su apariencia y guarda estado)
     */
    private void conquerTerritory(ImageView territory, int territoryNumber) {
        try {
            // Guardar referencia al territorio conquistado
            conqueredTerritory = territory;
            conqueredTerritoryNumber = territoryNumber;

            // Guardar en GameApp para persistencia
            gameApp.addConqueredTerritory(territoryNumber);

            // Cambiar la imagen a territorio propio
            Image conqueredImage = new Image("file:src/main/resources/images/territorioActual.png");
            territory.setImage(conqueredImage);

            // Cambiar el efecto a verde
            DropShadow greenGlow = new DropShadow();
            greenGlow.setColor(Color.rgb(0, 255, 0, 0.8));
            greenGlow.setRadius(20);
            greenGlow.setSpread(0.3);
            territory.setEffect(greenGlow);

            // REMOVER interactividad
            removeTerritoryInteractivity(territory);

            // Actualizar el texto de la etiqueta
            updateTerritoryLabel(territory, "Conquistado");

            System.out.println("✅ Territorio " + territoryNumber + " conquistado y guardado!");

        } catch (Exception e) {
            System.err.println("❌ Error al conquistar territorio: " + e.getMessage());
        }
    }

    private DropShadow createNormalGlowEffect() {
        DropShadow normalGlow = new DropShadow();
        normalGlow.setColor(Color.rgb(255, 0, 0, 0.8));
        normalGlow.setRadius(15);
        normalGlow.setSpread(0.2);
        return normalGlow;
    }

    private DropShadow createRedGlowEffect() {
        DropShadow redGlow = new DropShadow();
        redGlow.setColor(Color.rgb(255, 0, 0, 0.9));
        redGlow.setRadius(20);
        redGlow.setSpread(0.3);
        return redGlow;
    }

    private int calcularFuerzaTerritorio(int numeroTerritorio) {
        if (numeroTerritorio >= 0 && numeroTerritorio < gameMap.getTerritories().size()) {
            Territory territory = gameMap.getTerritories().get(numeroTerritorio);
            if (territory != null && territory.getPlayerOwner() != null) {
                return territory.getPlayerOwner().calculateTotalDefence();
            }
        }
        return 0;
    }

    private void setupInfoPanel() {
        VBox infoPanel = new VBox(10);
        infoPanel.setAlignment(Pos.TOP_CENTER);
        infoPanel.setPadding(new Insets(20, 25, 20, 25));
        infoPanel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.95); " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 15, 0.5, 0, 3);"
        );

        Label title = new Label("🗺 MAPA DE CONQUISTA");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label instructions = new Label(
                "⚔ Haz clic en un territorio enemigo vecino para atacarlo\n" +
                        " Tu territorio está marcado en verde\n" +
                        " Territorios enemigos en rojo\n"
        );
        instructions.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-text-alignment: center;");
        instructions.setWrapText(true);

        infoPanel.getChildren().addAll(title, instructions);

        infoPanel.layoutXProperty().bind(widthProperty().divide(2).subtract(infoPanel.widthProperty().divide(2)));
        infoPanel.setLayoutY(20);

        getChildren().add(infoPanel);
    }

    private void setupBackButton() {
        Button backButton = new Button("← VOLVER AL TERRITORIO");
        backButton.setPrefWidth(220);
        backButton.setPrefHeight(50);

        backButton.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.95); " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #2c3e50; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold;"
        );

        backButton.setOnMouseEntered(e -> {
            backButton.setStyle(
                    "-fx-background-color: rgba(236, 240, 241, 0.95); " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: #34495e; " +
                            "-fx-border-width: 2.5; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: #34495e; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-effect: dropshadow(gaussian, rgba(52, 73, 94, 0.4), 10, 0.5, 0, 3);"
            );
            backButton.setScaleX(1.05);
            backButton.setScaleY(1.05);
        });

        backButton.setOnMouseExited(e -> {
            backButton.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.95); " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: #2c3e50; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: #2c3e50; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-effect: null;"
            );
            backButton.setScaleX(1.0);
            backButton.setScaleY(1.0);
        });

        backButton.setOnAction(e -> {
            System.out.println("🔙 Volviendo al territorio principal...");
            closeMap();
        });

        backButton.setLayoutX(20);
        backButton.layoutYProperty().bind(heightProperty().subtract(70));

        getChildren().add(backButton);
    }

    private void showSimpleDefenseInfo(ImageView territory, int territoryNumber) {
        if (defenseInfoPanel != null) {
            getChildren().remove(defenseInfoPanel);
        }

        int defense = calcularFuerzaTerritorio(territoryNumber);

        defenseInfoPanel = new VBox(3);
        defenseInfoPanel.setAlignment(Pos.CENTER);
        defenseInfoPanel.setPadding(new Insets(6, 10, 6, 10));

        defenseInfoPanel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.85); " +
                        "-fx-background-radius: 6; " +
                        "-fx-border-color: #e74c3c; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 6; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0.5, 0, 1);"
        );

        Label defenseLabel = new Label("Defensa: " + defense);
        defenseLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        defenseInfoPanel.getChildren().add(defenseLabel);

        double posX = territory.getX() + territory.getFitWidth() / 2 - 35;
        double posY = territory.getY() - 30;

        if (posY < 10) {
            posY = territory.getY() + territory.getFitHeight() + 5;
        }

        defenseInfoPanel.setLayoutX(posX);
        defenseInfoPanel.setLayoutY(posY);

        getChildren().add(defenseInfoPanel);

        defenseInfoPanel.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), defenseInfoPanel);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void hideSimpleDefenseInfo() {
        if (defenseInfoPanel != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), defenseInfoPanel);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                getChildren().remove(defenseInfoPanel);
                defenseInfoPanel = null;
            });
            fadeOut.play();
        }
    }

    private void showNotAdjacentAlert() {
        javafx.application.Platform.runLater(() -> {
            Stage alertStage = new Stage();
            alertStage.initModality(Modality.APPLICATION_MODAL);
            alertStage.initStyle(StageStyle.TRANSPARENT);
            alertStage.setTitle("Territorio no adyacente");

            VBox alertPanel = new VBox(15);
            alertPanel.setPadding(new Insets(25, 30, 25, 30));
            alertPanel.setAlignment(Pos.CENTER);

            alertPanel.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.50); " +
                            "-fx-background-radius: 15; " +
                            "-fx-border-color: #e74c3c; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 15; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.5, 0, 2);"
            );

            Label warningIcon = new Label("🚫");
            warningIcon.setStyle("-fx-font-size: 36px; -fx-padding: 0 0 5 0;");

            VBox messageContainer = new VBox(5);
            messageContainer.setAlignment(Pos.CENTER);

            Label titleLabel = new Label("¡TERRITORIO NO ADYACENTE!");
            titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

            Label detailLabel = new Label(
                    "Solo puedes atacar territorios que sean vecinos directos de los tuyos.\n\n" +
                            "Debes conquistar territorio por territorio, avanzando desde tus fronteras."
            );
            detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #000000; -fx-text-alignment: center;");
            detailLabel.setWrapText(true);

            messageContainer.getChildren().addAll(titleLabel, detailLabel);

            Button okButton = new Button("Entendido");
            okButton.setPrefWidth(150);
            okButton.setPrefHeight(38);
            okButton.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.5); " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-color: #e74c3c; " +
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
                                "-fx-border-color: #c0392b; " +
                                "-fx-border-width: 2.5; " +
                                "-fx-border-radius: 6; " +
                                "-fx-cursor: hand; " +
                                "-fx-text-fill: #2c3e50; " +
                                "-fx-font-size: 12px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-effect: dropshadow(gaussian, rgba(192, 57, 43, 0.3), 5, 0.5, 0, 1);"
                );
            });

            okButton.setOnMouseExited(e -> {
                okButton.setStyle(
                        "-fx-background-color: rgba(255, 255, 255, 0.5); " +
                                "-fx-background-radius: 6; " +
                                "-fx-border-color: #e74c3c; " +
                                "-fx-border-width: 2; " +
                                "-fx-border-radius: 6; " +
                                "-fx-cursor: hand; " +
                                "-fx-text-fill: #2c3e50; " +
                                "-fx-font-size: 12px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-effect: null;"
                );
            });

            okButton.setOnAction(e -> alertStage.close());

            alertPanel.getChildren().addAll(warningIcon, messageContainer, okButton);

            StackPane rootPane = new StackPane(alertPanel);
            rootPane.setStyle("-fx-background-color: transparent;");
            rootPane.setAlignment(Pos.CENTER);

            Scene alertScene = new Scene(rootPane, 350, 250);
            alertScene.setFill(Color.TRANSPARENT);

            alertStage.initOwner(this.getScene().getWindow());
            alertStage.setScene(alertScene);
            alertStage.setResizable(false);
            alertStage.showAndWait();
        });
    }

    private void createPlaceholderTerritories() {
        double territorySize = getWidth() * 0.1;

        javafx.scene.shape.Circle myTerritory = new javafx.scene.shape.Circle(territorySize / 2);
        myTerritory.setFill(Color.rgb(0, 255, 0, 0.7));
        myTerritory.setCenterX(getWidth() * 0.1);
        myTerritory.setCenterY(getHeight() * 0.15);

        javafx.scene.shape.Circle enemy1 = new javafx.scene.shape.Circle(territorySize / 2 * 0.9);
        enemy1.setFill(Color.rgb(255, 0, 0, 0.7));
        enemy1.setCenterX(getWidth() * 0.8);
        enemy1.setCenterY(getHeight() * 0.2);

        javafx.scene.shape.Circle enemy2 = new javafx.scene.shape.Circle(territorySize / 2);
        enemy2.setFill(Color.rgb(255, 0, 0, 0.7));
        enemy2.setCenterX(getWidth() * 0.75);
        enemy2.setCenterY(getHeight() * 0.5);

        getChildren().addAll(myTerritory, enemy1, enemy2);
    }

    public void showMap() {
        setOpacity(0);
        setScaleX(0.95);
        setScaleY(0.95);

        toFront();

        FadeTransition fade = new FadeTransition(Duration.millis(500), this);
        fade.setToValue(1.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(500), this);
        scale.setToX(1.0);
        scale.setToY(1.0);

        ParallelTransition entrance = new ParallelTransition(fade, scale);
        entrance.play();

        System.out.println("🗺️ Mapa de territorios abierto (superpuesto)");
    }

    public void closeMap() {
        FadeTransition fade = new FadeTransition(Duration.millis(300), this);
        fade.setToValue(0);

        fade.setOnFinished(e -> {
            if (gameApp != null && gameApp.getSceneContainer() != null) {
                gameApp.getSceneContainer().getChildren().remove(this);
            }
            if (gameApp != null) {
                gameApp.onMapClosed();
            }
        });

        fade.play();
    }

    public void setTerritoryPosition(String territoryType, double percentX, double percentY) {
        switch (territoryType.toLowerCase()) {
            case "current":
                if (currentTerritory != null) {
                    currentTerritory.setX(getWidth() * percentX);
                    currentTerritory.setY(getHeight() * percentY);
                }
                break;
            case "enemy1":
                if (enemyTerritory1 != null) {
                    enemyTerritory1.setX(getWidth() * percentX);
                    enemyTerritory1.setY(getHeight() * percentY);
                }
                break;
            case "enemy2":
                if (enemyTerritory2 != null) {
                    enemyTerritory2.setX(getWidth() * percentX);
                    enemyTerritory2.setY(getHeight() * percentY);
                }
                break;
        }
    }

    /**
     * Carga los territorios ya conquistados desde GameApp
     */
    private void loadConqueredTerritories() {
        Set<Integer> conquered = gameApp.getConqueredTerritories();
        System.out.println("📂 Cargando territorios conquistados: " + conquered);

        for (int territoryNumber : conquered) {
            // Buscar y marcar el territorio como conquistado
            ImageView territory = findTerritoryByNumber(territoryNumber);
            if (territory != null) {
                markTerritoryAsConquered(territory, territoryNumber);
            }
        }
    }

    /**
     * Busca un territorio por su número
     */
    private ImageView findTerritoryByNumber(int territoryNumber) {
        switch (territoryNumber) {
            case 0: // Tu territorio base
                return currentTerritory;
            case 1:
                return enemyTerritory1;
            case 2:
                return enemyTerritory2;
            default:
                return null;
        }
    }

    /**
     * Marca un territorio como conquistado (sin interacción)
     */
    private void markTerritoryAsConquered(ImageView territory, int territoryNumber) {
        try {
            if (territory == null) return;

            // Cambiar la imagen a territorio propio
            Image conqueredImage = new Image("file:src/main/resources/images/territorioActual.png");
            territory.setImage(conqueredImage);

            // Cambiar el efecto a verde
            DropShadow greenGlow = new DropShadow();
            greenGlow.setColor(Color.rgb(0, 255, 0, 0.8));
            greenGlow.setRadius(20);
            greenGlow.setSpread(0.3);
            territory.setEffect(greenGlow);

            // REMOVER interactividad
            removeTerritoryInteractivity(territory);

            // Actualizar el texto de la etiqueta
            updateTerritoryLabel(territory, "Conquistado");

            System.out.println("🔄 Territorio " + territoryNumber + " cargado como conquistado");

        } catch (Exception e) {
            System.err.println("❌ Error al marcar territorio como conquistado: " + e.getMessage());
        }
    }

    /**
     * Remueve toda interactividad de un territorio
     */
    private void removeTerritoryInteractivity(ImageView territory) {
        // Remover todos los event handlers
        territory.setOnMouseClicked(null);
        territory.setOnMouseEntered(null);
        territory.setOnMouseExited(null);

        // Deshabilitar cursor
        territory.setCursor(javafx.scene.Cursor.DEFAULT);

        // Hacer no interactivo
        territory.setDisable(true);
        territory.setMouseTransparent(false); // Permitir que pase el mouse (sin interacción)

        // Remover cualquier efecto de hover que pueda quedar
        territory.setScaleX(1.0);
        territory.setScaleY(1.0);
    }

    /**
     * Actualiza la etiqueta de un territorio
     */
    private void updateTerritoryLabel(ImageView territory, String newText) {
        int labelIndex = getChildren().indexOf(territory) + 1;
        if (labelIndex < getChildren().size()) {
            Node node = getChildren().get(labelIndex);
            if (node instanceof Label) {
                Label label = (Label) node;
                label.setText(newText);
                // Cambiar estilo a verde
                label.setStyle(
                        "-fx-font-size: 14px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-color: rgba(39, 174, 96, 0.7); " + // Verde
                                "-fx-background-radius: 8; " +
                                "-fx-padding: 5 12;"
                );
            }
        }
    }

    /**
     * Muestra la pantalla de victoria del jefe final (territorio 2)
     */
    private void showFinalVictoryScreen(int territoryNumber, String territoryName) {
        if (isVictoryShowing) return;
        isVictoryShowing = true;

        // Crear overlay que cubra TODA la pantalla
        victoryOverlay = new StackPane();

        // CRÍTICO: Fondo negro con 85% opacidad
        victoryOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");

        // IMPORTANTE: Asegurar que cubra toda el área visible
        victoryOverlay.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        victoryOverlay.setPrefSize(getWidth(), getHeight());
        victoryOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // VINCULAR tamaño al contenedor principal para que se ajuste automáticamente
        victoryOverlay.prefWidthProperty().bind(widthProperty());
        victoryOverlay.prefHeightProperty().bind(heightProperty());

        // Permitir interacción con el botón
        victoryOverlay.setMouseTransparent(false);

        // Panel de victoria final
        VBox finalVictoryPanel = createFinalVictoryPanel(territoryName);
        finalVictoryPanel.setOpacity(0);
        finalVictoryPanel.setScaleX(0.9);
        finalVictoryPanel.setScaleY(0.9);

        victoryOverlay.getChildren().add(finalVictoryPanel);
        StackPane.setAlignment(finalVictoryPanel, Pos.CENTER);

        // Añadir overlay a la escena
        getChildren().add(victoryOverlay);
        victoryOverlay.toFront();

        // Forzar layout para asegurar que cubre toda el área
        victoryOverlay.layout();

        // Animación de entrada
        victoryOverlay.setOpacity(0); // Comienza transparente

        FadeTransition overlayFade = new FadeTransition(Duration.millis(500), victoryOverlay);
        overlayFade.setToValue(1.0);

        FadeTransition panelFade = new FadeTransition(Duration.millis(400), finalVictoryPanel);
        panelFade.setFromValue(0);
        panelFade.setToValue(1);
        panelFade.setDelay(Duration.millis(100));

        ScaleTransition panelScale = new ScaleTransition(Duration.millis(400), finalVictoryPanel);
        panelScale.setFromX(0.9);
        panelScale.setFromY(0.9);
        panelScale.setToX(1.0);
        panelScale.setToY(1.0);
        panelScale.setDelay(Duration.millis(100));
        panelScale.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        ParallelTransition entrance = new ParallelTransition(overlayFade, panelFade, panelScale);
        entrance.play();

        System.out.println("🎉🎉🎉 ¡VICTORIA FINAL! Territorio jefe conquistado: " + territoryName);
        System.out.println("📏 Tamaño overlay: " + getWidth() + "x" + getHeight());
    }

    /**
     * Crea el panel de victoria final con color amarillo predominante
     */
    private VBox createFinalVictoryPanel(String territoryName) {
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(30, 40, 30, 40));
        panel.setMaxWidth(450);
        panel.setMaxHeight(550);

        // Estilo con color amarillo dorado predominante
        panel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.50); " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: linear-gradient(to bottom, #FFD700, #FF8C00); " + // Gradiente amarillo/dorado
                        "-fx-border-width: 3; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(218, 165, 32, 0.4), 20, 0.5, 0, 5);" // Sombra dorada
        );

        try {
            // Cargar imagen de victoria (misma imagen pero con contexto diferente)
            Image victoryImage = new Image("file:src/main/resources/images/VictoriaFinal.png");
            ImageView victoryImageView = new ImageView(victoryImage);
            victoryImageView.setPreserveRatio(true);
            victoryImageView.setFitWidth(280);

            // Título con estilo dorado
            Label titleLabel = new Label("¡VICTORIA ABSOLUTA!");
            titleLabel.setStyle(
                    "-fx-font-size: 32px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: linear-gradient(to bottom, #FFD700, #B8860B); " + // Texto con gradiente dorado
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0.5, 1, 1);"
            );

            // Mensaje de victoria final
            Label messageLabel = new Label("¡Has conquistado el territorio final!");
            messageLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #8B4513; -fx-font-weight: bold;");

            // Subtítulo específico para el territorio jefe
            Label territoryLabel = new Label(territoryName);
            territoryLabel.setStyle(
                    "-fx-font-size: 22px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: #DAA520; " +
                            "-fx-font-style: italic;"
            );

            // Separador elegante (dorado)
            Region separator = new Region();
            separator.setPrefHeight(3);
            separator.setPrefWidth(250);
            separator.setStyle(
                    "-fx-background-color: linear-gradient(to right, transparent, #FFD700, #FFA500, #FFD700, transparent);"
            );

            // Botón para ir al menú principal
            Button menuButton = createFinalVictoryButton("IR AL MENÚ PRINCIPAL");
            menuButton.setOnAction(e -> {
                hideVictoryScreen();
                goToMainMenu();
            });

            panel.getChildren().addAll(
                    victoryImageView,
                    titleLabel,
                    messageLabel,
                    territoryLabel,
                    separator,
                    menuButton
            );

        } catch (Exception ex) {
            System.err.println("❌ Error al cargar imagen de victoria final: " + ex.getMessage());

            // Placeholder si no se carga la imagen
            VBox textContent = new VBox(15);
            textContent.setAlignment(Pos.CENTER);

            Label victoryText = new Label("¡VICTORIA ABSOLUTA!");
            victoryText.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #FFD700;");

            Label messageText = new Label("¡Has conquistado el territorio final:\n" + territoryName + "!");
            messageText.setStyle("-fx-font-size: 20px; -fx-text-fill: #8B4513; -fx-font-weight: bold; -fx-text-alignment: center;");
            messageText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            Label congratsText = new Label("¡Felicidades! Has completado el juego.\nTu reino está completamente conquistado.");
            congratsText.setStyle("-fx-font-size: 16px; -fx-text-fill: #654321; -fx-text-alignment: center;");
            congratsText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            textContent.getChildren().addAll(victoryText, messageText, congratsText);

            Button menuButton = createFinalVictoryButton("IR AL MENÚ PRINCIPAL");
            menuButton.setOnAction(e -> {
                hideVictoryScreen();
                goToMainMenu();
            });

            panel.getChildren().addAll(textContent, menuButton);
        }

        return panel;
    }

    /**
     * Crea botón para pantalla de victoria final con estilo dorado
     */
    private Button createFinalVictoryButton(String text) {
        HBox buttonContent = new HBox(8);
        buttonContent.setAlignment(Pos.CENTER);
        buttonContent.setPadding(new Insets(12, 30, 12, 30));

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #8B4513;");

        // Ícono de corona opcional
        Label crownIcon = new Label("👑");
        crownIcon.setStyle("-fx-font-size: 20px;");

        buttonContent.getChildren().addAll(crownIcon, textLabel);

        Button button = new Button();
        button.setGraphic(buttonContent);
        button.setPrefWidth(300);
        button.setPrefHeight(60);

        // ESTILO BASE con gradiente dorado
        String baseStyle =
                "-fx-background-color: linear-gradient(to bottom, #FFD700, #FFA500); " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #8B4513; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 10; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #8B4513;";

        button.setStyle(baseStyle);

        // EFECTO HOVER - más brillante
        button.setOnMouseEntered(e -> {
            String hoverStyle =
                    "-fx-background-color: linear-gradient(to bottom, #FFEC8B, #FFB90F); " +
                            "-fx-background-radius: 10; " +
                            "-fx-border-color: #654321; " +
                            "-fx-border-width: 3; " +
                            "-fx-border-radius: 10; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(gaussian, rgba(218, 165, 32, 0.6), 15, 0.5, 0, 3);";

            button.setStyle(hoverStyle);
            button.setScaleX(1.08);
            button.setScaleY(1.08);
        });

        button.setOnMouseExited(e -> {
            button.setStyle(baseStyle);
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        // Efecto al presionar
        button.setOnMousePressed(e -> {
            button.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #FFA500, #CD853F); " +
                            "-fx-background-radius: 10; " +
                            "-fx-border-color: #654321; " +
                            "-fx-border-width: 4; " +
                            "-fx-border-radius: 10; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: #8B4513;"
            );
        });

        button.setOnMouseReleased(e -> {
            button.setStyle(baseStyle);
        });

        return button;
    }

    private void goToMainMenu() {
        System.out.println("🚪 Navegando al menú principal...");

        // Cerrar la ventana principal de GameApp
        if (gameApp != null) {
            Platform.runLater(() -> {
                // Primero ocultar la pantalla de victoria
                hideVictoryScreen();

                // Usar el MenuManager para regresar al menú
                if (gameApp.getMenuManager() != null) {
                    gameApp.getMenuManager().returnToMenu();
                } else {
                    // Si no hay MenuManager, cerrar directamente
                    Stage mainStage = (Stage) gameApp.getSceneContainer().getScene().getWindow();
                    if (mainStage != null) {
                        mainStage.close();
                    }
                }
            });
        }
    }
}