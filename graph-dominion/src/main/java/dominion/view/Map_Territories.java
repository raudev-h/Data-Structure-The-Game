package dominion.view;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class Map_Territories extends Pane {
    private GameApp gameApp; //// Referencia a la pantalla principal
    public int forceTerritory1 = 100;
    public int forceTerritory2 = 200;


    // Tamaños de ventana (se ajustarán automáticamente)
    private double windowWidth;
    private double windowHeight;

    // Imágenes
    private ImageView backgroundMap;
    private ImageView currentTerritory;
    private ImageView enemyTerritory1;
    private ImageView enemyTerritory2;

    // Panel de confirmación actual
    private Pane currentConfirmationPanel;

    public Map_Territories(GameApp gameApp, double width, double height) {
        this.gameApp = gameApp;
        this.windowWidth = width;
        this.windowHeight = height;

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

            // **OPCIÓN 2: Usar StackPane para centrado automático**
            mapBackground.setPreserveRatio(true);
            mapBackground.setSmooth(true);

            // Ajustar tamaño para cubrir todo el área (puede recortar bordes)
            mapBackground.fitWidthProperty().bind(widthProperty());
            mapBackground.fitHeightProperty().bind(heightProperty());

            // **FORZAR que se escale para cubrir todo (puede distorsionar)**
            mapBackground.setPreserveRatio(false); // Esto permitirá que cubra todo exactamente

            // **O mantener proporción pero centrar:**
            // mapBackground.setPreserveRatio(true);

            // Calcular posición para centrar
            mapBackground.setX(0);
            mapBackground.setY(0);

            // **Listener para reajustar cuando cambie el tamaño**
            widthProperty().addListener((obs, oldVal, newVal) -> {
                if (mapBackground.isPreserveRatio()) {
                    // Si mantiene proporción, centrar horizontalmente
                    double imageWidth = mapBackground.getImage().getWidth();
                    double imageHeight = mapBackground.getImage().getHeight();
                    double aspectRatio = imageWidth / imageHeight;

                    double newHeight = newVal.doubleValue() / aspectRatio;
                    if (newHeight < getHeight()) {
                        // Si la altura calculada es menor que la disponible
                        mapBackground.setFitWidth(newVal.doubleValue());
                        mapBackground.setFitHeight(newHeight);
                        mapBackground.setX(0);
                        mapBackground.setY((getHeight() - newHeight) / 2);
                    } else {
                        // Si necesita ajustar por altura
                        mapBackground.setFitHeight(getHeight());
                        mapBackground.setFitWidth(getHeight() * aspectRatio);
                        mapBackground.setX((newVal.doubleValue() - (getHeight() * aspectRatio)) / 2);
                        mapBackground.setY(0);
                    }
                }
            });

            heightProperty().addListener((obs, oldVal, newVal) -> {
                if (mapBackground.isPreserveRatio()) {
                    // Si mantiene proporción, centrar verticalmente
                    double imageWidth = mapBackground.getImage().getWidth();
                    double imageHeight = mapBackground.getImage().getHeight();
                    double aspectRatio = imageWidth / imageHeight;

                    double newWidth = newVal.doubleValue() * aspectRatio;
                    if (newWidth < getWidth()) {
                        // Si el ancho calculado es menor que el disponible
                        mapBackground.setFitHeight(newVal.doubleValue());
                        mapBackground.setFitWidth(newWidth);
                        mapBackground.setX((getWidth() - newWidth) / 2);
                        mapBackground.setY(0);
                    } else {
                        // Si necesita ajustar por ancho
                        mapBackground.setFitWidth(getWidth());
                        mapBackground.setFitHeight(getWidth() / aspectRatio);
                        mapBackground.setX(0);
                        mapBackground.setY((newVal.doubleValue() - (getWidth() / aspectRatio)) / 2);
                    }
                }
            });

            // Añadir al principio (fondo)
            getChildren().add(0, mapBackground);

            // **Inicializar posición después de cargar la imagen**
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
            // Si falla, usar fondo sólido apropiado para un mapa
            javafx.scene.shape.Rectangle fallbackBackground = new javafx.scene.shape.Rectangle();
            fallbackBackground.widthProperty().bind(widthProperty());
            fallbackBackground.heightProperty().bind(heightProperty());
            fallbackBackground.setFill(Color.rgb(40, 45, 70)); // Azul oscuro apropiado para mapa

            getChildren().add(0, fallbackBackground);
        }
    }

    // **Nuevo método auxiliar para ajustar posición del fondo**
    private void adjustBackgroundPosition(ImageView mapBackground) {
        if (mapBackground.getImage() == null) return;

        double imageWidth = mapBackground.getImage().getWidth();
        double imageHeight = mapBackground.getImage().getHeight();
        double aspectRatio = imageWidth / imageHeight;
        double containerWidth = getWidth();
        double containerHeight = getHeight();
        double containerAspectRatio = containerWidth / containerHeight;

        if (mapBackground.isPreserveRatio()) {
            // Mantener proporción de la imagen
            if (aspectRatio > containerAspectRatio) {
                // La imagen es más ancha que el contenedor - ajustar por ancho
                mapBackground.setFitWidth(containerWidth);
                mapBackground.setFitHeight(containerWidth / aspectRatio);
                mapBackground.setX(0);
                mapBackground.setY((containerHeight - (containerWidth / aspectRatio)) / 2);
            } else {
                // La imagen es más alta que el contenedor - ajustar por altura
                mapBackground.setFitHeight(containerHeight);
                mapBackground.setFitWidth(containerHeight * aspectRatio);
                mapBackground.setX((containerWidth - (containerHeight * aspectRatio)) / 2);
                mapBackground.setY(0);
            }
        } else {
            // Distorsionar para cubrir todo exactamente
            mapBackground.setFitWidth(containerWidth);
            mapBackground.setFitHeight(containerHeight);
            mapBackground.setX(0);
            mapBackground.setY(0);
        }
    }

    private void setupTerritoriesMap() {
        try {
            // Cargar imagen del mapa de territorios (superpuesto sobre el fondo)
            Image mapImage = new Image("file:src/main/resources/images/mapaTerritorios.png");
            backgroundMap = new ImageView(mapImage);

            // Hacer que la imagen OCUPE TODA LA PANTALLA y mantenga proporción
            backgroundMap.setPreserveRatio(true);
            backgroundMap.setSmooth(true);

            // Ajustar tamaño para cubrir toda el área disponible
            backgroundMap.fitWidthProperty().bind(widthProperty());
            backgroundMap.fitHeightProperty().bind(heightProperty());

            // Centrar la imagen
            backgroundMap.setX(0);
            backgroundMap.setY(0);

            // Hacer el mapa de territorios semi-transparente para ver el fondo
            backgroundMap.setOpacity(0.85);

            // Efecto de sombra
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.7));
            shadow.setRadius(30);
            shadow.setSpread(0.1);
            backgroundMap.setEffect(shadow);

            // Añadir en posición 1 (después del fondo)
            getChildren().add(1, backgroundMap);

            // Listener para ajustar cuando cambie el tamaño
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

            // Tamaño proporcional al ancho de la pantalla
            double territorySize = windowWidth * 0.1; // 10% del ancho
            currentTerritory.setFitWidth(territorySize);
            currentTerritory.setFitHeight(territorySize);
            currentTerritory.setPreserveRatio(true);

            // Efecto especial para territorio actual
            DropShadow glow = new DropShadow();
            glow.setColor(Color.rgb(0, 255, 0, 0.8)); // Verde para territorio propio
            glow.setRadius(20);
            glow.setSpread(0.3);
            currentTerritory.setEffect(glow);

            // Etiqueta
            Label currentLabel = createTerritoryLabel("Tu Territorio");

            // Animación de pulso
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.millis(0), e -> {
                        currentTerritory.setScaleX(1.0);
                        currentTerritory.setScaleY(1.0);
                    }),
                    new KeyFrame(Duration.millis(1000), e -> {
                        currentTerritory.setScaleX(1.08);
                        currentTerritory.setScaleY(1.08);
                    })
            );
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();

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


            // Efecto rojo para enemigos
            DropShadow enemyGlow = new DropShadow();
            enemyGlow.setColor(Color.rgb(255, 0, 0, 0.8)); // Rojo para enemigos
            enemyGlow.setRadius(15);
            enemyGlow.setSpread(0.2);
            enemyTerritory1.setEffect(enemyGlow);
            enemyTerritory2.setEffect(enemyGlow);

            // Etiquetas para enemigos
            Label enemyLabel1 = createTerritoryLabel("Nivel 1");
            Label enemyLabel2 = createTerritoryLabel("Nivel Final");

            // Hacer territorios enemigos interactivos
            makeTerritoryInteractive(enemyTerritory1, "Nivel 1", 1);
            makeTerritoryInteractive(enemyTerritory2, "Nivel Final", 2);

            // Añadir todo al pane
            getChildren().addAll(
                    currentTerritory, currentLabel,
                    enemyTerritory1, enemyLabel1,
                    enemyTerritory2, enemyLabel2
            );

            // Posicionar territorios
            adjustTerritoryPositions();

        } catch (Exception e) {
            System.err.println("❌ Error al cargar territorios: " + e.getMessage());
            createPlaceholderTerritories();
        }
    }

    /**
     * Ajusta las posiciones de los territorios en diagonal con separación uniforme
     */
    private void adjustTerritoryPositions() {
        double currentWidth = getWidth();
        double currentHeight = getHeight();

        // Tamaño de los territorios (todos iguales para uniformidad)
        double territorySize = windowWidth * 0.08; // 8% del ancho original

        // Configuración de la diagonal
        double startX = currentWidth * 0.15;      // Inicio en 15% del ancho
        double startY = currentHeight * 0.2;      // Inicio en 20% del alto
        double spacingX = currentWidth * 0.2;     // Espaciado horizontal 20%
        double spacingY = currentHeight * 0.15;   // Espaciado vertical 15%

        // Posición 1: Tu territorio (esquina superior izquierda de la diagonal)
        if (currentTerritory != null) {
            currentTerritory.setFitWidth(territorySize);
            currentTerritory.setFitHeight(territorySize);
            currentTerritory.setX(startX);
            currentTerritory.setY(startY);

            // Ajustar etiqueta
            if (getChildren().indexOf(currentTerritory) + 1 < getChildren().size()) {
                Label label = (Label) getChildren().get(getChildren().indexOf(currentTerritory) + 1);
                label.setLayoutX(currentTerritory.getX() + currentTerritory.getFitWidth()/2 - 40);
                label.setLayoutY(currentTerritory.getY() + currentTerritory.getFitHeight() + 10);
            }
        }

        // Posición 2: Enemigo 1 (segundo en la diagonal)
        if (enemyTerritory1 != null) {
            enemyTerritory1.setFitWidth(territorySize * 0.9);
            enemyTerritory1.setFitHeight(territorySize * 0.9);
            enemyTerritory1.setX(startX + spacingX);
            enemyTerritory1.setY(startY + spacingY);

            if (getChildren().indexOf(enemyTerritory1) + 1 < getChildren().size()) {
                Label label = (Label) getChildren().get(getChildren().indexOf(enemyTerritory1) + 1);
                label.setLayoutX(enemyTerritory1.getX() + enemyTerritory1.getFitWidth()/2 - 50);
                label.setLayoutY(enemyTerritory1.getY() + enemyTerritory1.getFitHeight() + 10);
            }
        }

        // Posición 3: Enemigo 2 (tercero en la diagonal)
        if (enemyTerritory2 != null) {
            enemyTerritory2.setFitWidth(territorySize);
            enemyTerritory2.setFitHeight(territorySize);
            enemyTerritory2.setX(startX + (spacingX * 2));
            enemyTerritory2.setY(startY + (spacingY * 2));

            if (getChildren().indexOf(enemyTerritory2) + 1 < getChildren().size()) {
                Label label = (Label) getChildren().get(getChildren().indexOf(enemyTerritory2) + 1);
                label.setLayoutX(enemyTerritory2.getX() + enemyTerritory2.getFitWidth()/2 - 55);
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
        territory.setOnMouseClicked(e -> {
            System.out.println("⚔ Atacando " + name + "...");
            showConquestConfirmation(name, territoryNumber);

            // Efecto visual al hacer clic
            FadeTransition flash = new FadeTransition(Duration.millis(150), territory);
            flash.setFromValue(1.0);
            flash.setToValue(0.6);
            flash.setAutoReverse(true);
            flash.setCycleCount(2);
            flash.play();
        });

        territory.setOnMouseEntered(e -> {
            territory.setCursor(javafx.scene.Cursor.HAND);
            territory.setScaleX(1.15);
            territory.setScaleY(1.15);

            // Resaltar
            DropShadow highlight = new DropShadow();
            highlight.setColor(Color.rgb(255, 255, 100, 0.9));
            highlight.setRadius(25);
            territory.setEffect(highlight);
        });

        territory.setOnMouseExited(e -> {
            territory.setCursor(javafx.scene.Cursor.DEFAULT);
            territory.setScaleX(1.0);
            territory.setScaleY(1.0);

            // Restaurar efecto rojo
            DropShadow enemyGlow = new DropShadow();
            enemyGlow.setColor(Color.rgb(255, 0, 0, 0.8));
            enemyGlow.setRadius(15);
            territory.setEffect(enemyGlow);
        });
    }

    private void showConquestConfirmation(String territoryName, int territoryNumber) {
        // Remover panel anterior si existe
        if (currentConfirmationPanel != null) {
            getChildren().remove(currentConfirmationPanel);
        }

        // Crear panel de confirmación
        VBox confirmationPanel = new VBox(15);
        confirmationPanel.setAlignment(Pos.CENTER);
        confirmationPanel.setPadding(new Insets(25, 30, 25, 30));
        confirmationPanel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.95); " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #e74c3c; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0.5, 0, 5);"
        );

        Label title = new Label("⚔ CONQUISTAR " + territoryName.toUpperCase() + " ⚔");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        // Información del territorio
        Label infoLabel = new Label(
                "Fuerza estimada del enemigo: " + (calcularFuerzaTerritorio(territoryNumber)) + "\n" +
                        "¿Deseas atacar este territorio?"
        );
        infoLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-text-alignment: center;");
        infoLabel.setWrapText(true);

        // Botones
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button attackButton = createMapButton("¡ATACAR!", "#e74c3c");
        attackButton.setPrefWidth(150);
        attackButton.setPrefHeight(45);
        attackButton.setOnAction(e -> {
            System.out.println("⚔¡Ataque confirmado a " + territoryName + "!");
            startConquestBattle(territoryNumber);
            getChildren().remove(confirmationPanel);
            currentConfirmationPanel = null;
        });

        Button cancelButton = createMapButton("CANCELAR", "#7f8c8d");
        cancelButton.setPrefWidth(150);
        cancelButton.setPrefHeight(45);
        cancelButton.setOnAction(e -> {
            getChildren().remove(confirmationPanel);
            currentConfirmationPanel = null;
        });

        buttonBox.getChildren().addAll(attackButton, cancelButton);

        confirmationPanel.getChildren().addAll(title, infoLabel, buttonBox);

        // Posicionar en el centro
        confirmationPanel.setLayoutX((getWidth() - 400) / 2);
        confirmationPanel.setLayoutY((getHeight() - 250) / 2);

        getChildren().add(confirmationPanel);
        currentConfirmationPanel = confirmationPanel;

        // Animación de entrada
        confirmationPanel.setScaleX(0.8);
        confirmationPanel.setScaleY(0.8);
        confirmationPanel.setOpacity(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), confirmationPanel);
        scale.setToX(1.0);
        scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(300), confirmationPanel);
        fade.setToValue(1.0);

        ParallelTransition entrance = new ParallelTransition(scale, fade);
        entrance.play();
    }

    private int calcularFuerzaTerritorio(int numeroTerritorio){
        int fuerza = 0;
        if(numeroTerritorio == 1){
            return forceTerritory1;
        }
        else{
            return forceTerritory2;
        }
    }

    private void startConquestBattle(int territoryNumber) {
        System.out.println("⚔ Iniciando batalla por territorio #" + territoryNumber);

        // Mostrar mensaje de batalla
        showBattleMessage(territoryNumber);
    }

    private void showBattleMessage(int territoryNumber) {
        VBox messagePanel = new VBox(15);
        messagePanel.setAlignment(Pos.CENTER);
        messagePanel.setPadding(new Insets(25, 30, 25, 30));
        messagePanel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.95); " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #3498db; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0.5, 0, 5);"
        );

        Label title = new Label("⚔ ¡BATALLA EN CURSO! ⚔");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #3498db;");

        Label message = new Label(
                "Tu ejército se dirige al territorio #" + territoryNumber + "\n\n" +
                        "La batalla comenzará en breve...\n" +
                        "Puedes seguir construyendo mientras tanto.\n\n" +
                        "¡Buena suerte!"
        );
        message.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-text-alignment: center;");
        message.setWrapText(true);

        Button okButton = createMapButton("CONTINUAR", "#3498db");
        okButton.setPrefWidth(150);
        okButton.setPrefHeight(45);
        okButton.setOnAction(e -> {
            getChildren().remove(messagePanel);
            // Opcional: cerrar el mapa después de iniciar batalla
            // closeMap();
        });

        messagePanel.getChildren().addAll(title, message, okButton);

        // Posicionar
        messagePanel.setLayoutX((getWidth() - 400) / 2);
        messagePanel.setLayoutY(100); // Parte superior

        getChildren().add(messagePanel);

        // Animación
        messagePanel.setScaleX(0.8);
        messagePanel.setScaleY(0.8);
        messagePanel.setOpacity(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), messagePanel);
        scale.setToX(1.0);
        scale.setToY(1.0);

        FadeTransition fade = new FadeTransition(Duration.millis(300), messagePanel);
        fade.setToValue(1.0);

        ParallelTransition entrance = new ParallelTransition(scale, fade);
        entrance.play();
    }

    private Button createMapButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: " + darkenColor(color) + "; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold;"
        );

        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: " + darkenColor(color) + "; " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: " + darkenColor(darkenColor(color)) + "; " +
                            "-fx-border-width: 2.5; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-effect: dropshadow(gaussian, " + color + ", 10, 0.5, 0, 3);"
            );
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: " + color + "; " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: " + darkenColor(color) + "; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-effect: null;"
            );
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        return button;
    }

    private String darkenColor(String color) {
        if (color.equals("#e74c3c")) return "#c0392b";
        if (color.equals("#3498db")) return "#2980b9";
        if (color.equals("#7f8c8d")) return "#616a6b";
        return color;
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
                "⚔ Haz clic en un territorio enemigo para atacarlo\n" +
                        " Tu territorio está marcado en verde\n" +
                        " Territorios enemigos en rojo\n"
        );
        instructions.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-text-alignment: center;");
        instructions.setWrapText(true);

        infoPanel.getChildren().addAll(title, instructions);

        // Posicionar en parte superior central
        infoPanel.layoutXProperty().bind(widthProperty().divide(2).subtract(infoPanel.widthProperty().divide(2)));
        infoPanel.setLayoutY(20);

        getChildren().add(infoPanel);
    }

    private void setupBackButton() {
        Button backButton = new Button("← VOLVER AL TERRITORIO");
        backButton.setPrefWidth(220);
        backButton.setPrefHeight(50);

        // MISMO ESTILO que el juego principal
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

        // Posicionar en esquina inferior izquierda
        backButton.setLayoutX(20);
        backButton.layoutYProperty().bind(heightProperty().subtract(70));

        getChildren().add(backButton);
    }

    private void createPlaceholderBackground() {
        // Placeholder oscuro con patrón
        javafx.scene.shape.Rectangle placeholder = new javafx.scene.shape.Rectangle();
        placeholder.widthProperty().bind(widthProperty());
        placeholder.heightProperty().bind(heightProperty());
        placeholder.setFill(Color.rgb(40, 45, 70)); // Azul oscuro para mapa

        getChildren().add(placeholder);

        // Texto
        Label placeholderLabel = new Label("MAPA DE CONQUISTA");
        placeholderLabel.setStyle(
                "-fx-font-size: 36px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: rgba(255, 255, 255, 0.3);"
        );
        placeholderLabel.layoutXProperty().bind(widthProperty().divide(2).subtract(180));
        placeholderLabel.layoutYProperty().bind(heightProperty().divide(2).subtract(20));

        getChildren().add(placeholderLabel);
    }

    private void createPlaceholderTerritories() {
        // Placeholders circulares proporcionales
        double territorySize = getWidth() * 0.1;

        javafx.scene.shape.Circle myTerritory = new javafx.scene.shape.Circle(territorySize/2);
        myTerritory.setFill(Color.rgb(0, 255, 0, 0.7));
        myTerritory.setCenterX(getWidth() * 0.1);
        myTerritory.setCenterY(getHeight() * 0.15);

        javafx.scene.shape.Circle enemy1 = new javafx.scene.shape.Circle(territorySize/2 * 0.9);
        enemy1.setFill(Color.rgb(255, 0, 0, 0.7));
        enemy1.setCenterX(getWidth() * 0.8);
        enemy1.setCenterY(getHeight() * 0.2);

        javafx.scene.shape.Circle enemy2 = new javafx.scene.shape.Circle(territorySize/2);
        enemy2.setFill(Color.rgb(255, 0, 0, 0.7));
        enemy2.setCenterX(getWidth() * 0.75);
        enemy2.setCenterY(getHeight() * 0.5);

        javafx.scene.shape.Circle enemy3 = new javafx.scene.shape.Circle(territorySize/2 * 0.8);
        enemy3.setFill(Color.rgb(255, 0, 0, 0.7));
        enemy3.setCenterX(getWidth() * 0.78);
        enemy3.setCenterY(getHeight() * 0.8);

        getChildren().addAll(myTerritory, enemy1, enemy2, enemy3);
    }

    public void showMap() {
        // Animación de entrada
        setOpacity(0);
        setScaleX(0.95);
        setScaleY(0.95);

        // Asegurar que está al frente
        toFront();

        // Animación
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
        // Animación de salida
        FadeTransition fade = new FadeTransition(Duration.millis(300), this);
        fade.setToValue(0);

        fade.setOnFinished(e -> {
            // Remover de la pantalla
            if (gameApp != null && gameApp.getSceneContainer() != null) {
                gameApp.getSceneContainer().getChildren().remove(this);
            }
            // Notificar al GameApp
            if (gameApp != null) {
                gameApp.onMapClosed();
            }
        });

        fade.play();
    }

    // Método para ajustar posiciones (puedes llamarlo desde fuera)
    public void setTerritoryPosition(String territoryType, double percentX, double percentY) {
        // Ahora usa porcentajes en lugar de coordenadas absolutas
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
}