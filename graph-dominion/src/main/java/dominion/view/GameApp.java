package dominion.view;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class GameApp extends Application {

    private Pane root;
    private double windowWidth;
    private double windowHeight;
    private Popup townHallPopup;
    private boolean isBuildingMode = false;
    private ImageView buildingGhost; // Imagen fantasma para mostrar en el mouse
    private String currentBuildingType = ""; // Tipo de edificio actual
    private List<ImageView> placedBuildings = new ArrayList<>(); // Lista de edificios colocados
    private int width = 100;
    private int height = 100;


    @Override
    public void start(Stage stage) {
        // 1. Obtener tamaño de pantalla
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        windowWidth = Math.min(screen.getWidth() * 0.9, 1600);
        windowHeight = Math.min(screen.getHeight() * 0.9, 900);

        // 2. Crear contenedor principal
        root = new Pane();
        root.setPrefSize(windowWidth, windowHeight);

        // 3. Añadir mapa como Background
        setMapBackground(root, windowWidth, windowHeight);

        // 4. Añadir TownHall INTERACTIVO
        addInteractiveTownHall();

        // 5. Añadir timer
        Timer timer = new Timer();
        Pane timerPanel = timer.getTimerPanel();
        positionInCorner(timerPanel, windowWidth - 100, windowHeight);
        root.getChildren().add(timerPanel);

        // Inicializar el ImageView fantasma (invisible inicialmente)
        buildingGhost = new ImageView();
        buildingGhost.setVisible(false);
        buildingGhost.setMouseTransparent(true); // No captura eventos del mouse
        root.getChildren().add(buildingGhost);

        // 6. Configurar ventana
        Scene scene = new Scene(root, windowWidth, windowHeight);

        // Configurar listeners para el modo construcción
        setupBuildingListeners(scene);

        stage.setTitle("Dominion");
        stage.setScene(scene);
        centerStage(stage, windowWidth, windowHeight);
        stage.show();



        // 7. Iniciar timer
        timer.startTimer();
    }




    private void addInteractiveTownHall() {
        try {
            // Cargar imagen del TownHall
            Image townHallImage = new Image("file:src/main/resources/images/TownHall1.png");
            ImageView townHallView = new ImageView(townHallImage);

            // Configurar tamaño y posición
            double townHallSize = 170;
            townHallView.setFitWidth(townHallSize);
            townHallView.setFitHeight(townHallSize);
            townHallView.setPreserveRatio(true);

            double townHallX = windowWidth * 0.3 - townHallSize/2;
            double townHallY = windowHeight * 0.4 - townHallSize/2;
            townHallView.setX(townHallX+100);
            townHallView.setY(townHallY+100);

            placedBuildings.add(townHallView);


            // Efectos visuales
            DropShadow glow = new DropShadow();
            glow.setColor(Color.rgb(255, 215, 0, 0.7)); // Dorado
            glow.setRadius(15);
            townHallView.setEffect(glow);

            // **EVENTO DE CLIC - Abrir panel de opciones**
            townHallView.setOnMouseClicked(event -> {
                System.out.println("🏰 TownHall clickeado - Abriendo menú...");
                showTownHallMenu(townHallX + townHallSize/3, townHallY);
            });

            // Cursor de mano para indicar interactividad
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
            System.out.println("✅ TownHall interactivo añadido en: (" + townHallX + ", " + townHallY + ")");

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

        // Posición: CENTRADO en la pantalla
        double panelWidth = 100;  // Ancho mayor para incluir textos
        double panelHeight = 200; // Alto mayor para botones con texto
        double panelX = (windowWidth - panelWidth) / 2;
        double panelY = (windowHeight - panelHeight) / 2;

        townHallPopup.getContent().add(container);
        townHallPopup.show(root.getScene().getWindow(), panelX, panelY);

        // Animación de aparición desde el centro
        animateCenterEntrance(mainPanel);
    }


    private VBox createCenteredPanel() {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20, 20, 20, 20));
        panel.setPrefSize(250, 320);

        // Fondo blanco semitransparente (menos transparente)
        panel.setBackground(new Background(new BackgroundFill(
                Color.rgb(255, 255, 255, 0.50),  // 50% de opacidad - MENOS TRANSPARENTE
                new CornerRadii(12),
                Insets.EMPTY
        )));

        // Borde sutil dorado
        panel.setBorder(new Border(new BorderStroke(
                Color.rgb(212, 175, 55, 0.8),  // Dorado
                BorderStrokeStyle.SOLID,
                new CornerRadii(12),
                new BorderWidths(2)
        )));

        // Sombra para efecto de elevación
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(15);
        shadow.setSpread(0.1);
        panel.setEffect(shadow);

        // TÍTULO del panel
        Label title = new Label("TownHall");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; " +
                "-fx-text-fill: #2c3e50; -fx-font-family: 'Arial';");
        title.setPadding(new Insets(0, 0, 10, 0));

        // Separador
        Region separator = new Region();
        separator.setPrefHeight(2);
        separator.setStyle("-fx-background-color: #d4af37; " +
                "-fx-background-radius: 1;");

        // Botones con icono y texto
        VBox buttonContainer = new VBox(8);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(10, 0, 0, 0));

        Button houseButton = createTextButton("🏠", "Crear Casa", "100 Madera, 50 Oro");
        Button barracksButton = createTextButton("⚔", "Crear Cuartel", "200 Madera, 150 Oro");
        Button minerButton = createTextButton("⛏", "Crear Minero", "75 Oro, 25 Madera");
        Button lumberjackButton = createTextButton("", "Crear Leñador", "50 Oro, 50 Madera");

        // Acciones de los botones
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
            createMinerNextToTownHall();
        });

        lumberjackButton.setOnAction(e -> {
            System.out.println("✅ Creando Leñador...");
            townHallPopup.hide();
            showConstructionAnimation("Leñador");
        });

        buttonContainer.getChildren().addAll(
                houseButton, barracksButton, minerButton, lumberjackButton
        );


        panel.getChildren().addAll(title, separator, buttonContainer);

        return panel;
    }

    /**
     * Crea un minero automáticamente cerca del TownHall con búsqueda exhaustiva
     */
    private void createMinerNextToTownHall() {
        try {
            // 1. Obtener posición y tamaño del TownHall
            // Si tienes guardada la referencia del TownHall, úsala:
            // double townHallX = townHallView.getX();
            // double townHallY = townHallView.getY();
            // double townHallSize = townHallView.getFitWidth();

            // O usa las coordenadas calculadas (ajusta según tu implementación):
            double townHallX = windowWidth * 0.3 - 85 + 100;
            double townHallY = windowHeight * 0.4 - 85 + 100;
            double townHallSize = 170;

            double minerSize = 50;
            double spacing = 5; // Separación mínima (puede ser 0 para pegado)

            // 2. Lista de posiciones a probar (en orden de preferencia)
            List<Position> positionsToTry = new ArrayList<>();

            // Primera ronda: Posiciones inmediatamente alrededor del TownHall
            // Derecha, izquierda, arriba, abajo
            positionsToTry.add(new Position(townHallX + townHallSize + spacing,
                    townHallY + (townHallSize - minerSize) / 2));
            positionsToTry.add(new Position(townHallX - minerSize - spacing,
                    townHallY + (townHallSize - minerSize) / 2));
            positionsToTry.add(new Position(townHallX + (townHallSize - minerSize) / 2,
                    townHallY - minerSize - spacing));
            positionsToTry.add(new Position(townHallX + (townHallSize - minerSize) / 2,
                    townHallY + townHallSize + spacing));

            // Segunda ronda: Diagonales
            positionsToTry.add(new Position(townHallX - minerSize - spacing,
                    townHallY - minerSize - spacing));
            positionsToTry.add(new Position(townHallX + townHallSize + spacing,
                    townHallY - minerSize - spacing));
            positionsToTry.add(new Position(townHallX - minerSize - spacing,
                    townHallY + townHallSize + spacing));
            positionsToTry.add(new Position(townHallX + townHallSize + spacing,
                    townHallY + townHallSize + spacing));

            // Tercera ronda: Más lejos del TownHall (radio expandido)
            double extendedSpacing = 40;
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI / 4 * i;
                double offsetX = Math.cos(angle) * (townHallSize/2 + minerSize/2 + extendedSpacing);
                double offsetY = Math.sin(angle) * (townHallSize/2 + minerSize/2 + extendedSpacing);
                positionsToTry.add(new Position(
                        townHallX + townHallSize/2 - minerSize/2 + offsetX,
                        townHallY + townHallSize/2 - minerSize/2 + offsetY
                ));
            }

            // 3. Buscar posición válida
            Position validPosition = null;

            for (Position pos : positionsToTry) {
                if (!checkCollision(pos.x, pos.y, minerSize, minerSize) &&
                        pos.x >= 0 && pos.y >= 0 &&
                        pos.x + minerSize <= windowWidth &&
                        pos.y + minerSize <= windowHeight) {

                    validPosition = pos;
                    break;
                }
            }

            // 4. Si no hay posición libre alrededor del TownHall, buscar junto a otros mineros
            if (validPosition == null) {
                System.out.println("⚠️ No hay espacio alrededor del TownHall, buscando junto a otros mineros...");
                validPosition = findPositionNextToOtherMiner(minerSize, spacing);
            }

            // 5. Si aún no hay posición, mostrar error
            if (validPosition == null) {
                System.out.println("❌ No hay espacio disponible en ninguna parte");
                return;
            }

            // 6. Crear el minero en la posición encontrada
            createMinerAtPosition(validPosition.x, validPosition.y, minerSize);

        } catch (Exception e) {
            System.err.println("❌ Error al crear minero: " + e.getMessage());
        }
    }

    /**
     * Busca una posición junto a otros mineros existentes
     */
    private Position findPositionNextToOtherMiner(double minerSize, double spacing) {
        // Obtener todos los mineros existentes (puedes rastrearlos en una lista)
        List<ImageView> existingMiners = getExistingMiners();

        if (existingMiners.isEmpty()) {
            // Si no hay mineros, usar una posición por defecto
            return new Position(50, 50); // Esquina superior izquierda
        }

        // Probar alrededor de cada minero existente
        for (ImageView miner : existingMiners) {
            double minerX = miner.getX();
            double minerY = miner.getY();

            // Posiciones alrededor del minero actual
            Position[] positionsAround = {
                    new Position(minerX + minerSize + spacing, minerY), // Derecha
                    new Position(minerX - minerSize - spacing, minerY), // Izquierda
                    new Position(minerX, minerY - minerSize - spacing), // Arriba
                    new Position(minerX, minerY + minerSize + spacing), // Abajo
                    new Position(minerX + minerSize + spacing, minerY - minerSize - spacing), // Diagonal superior derecha
                    new Position(minerX - minerSize - spacing, minerY - minerSize - spacing), // Diagonal superior izquierda
                    new Position(minerX + minerSize + spacing, minerY + minerSize + spacing), // Diagonal inferior derecha
                    new Position(minerX - minerSize - spacing, minerY + minerSize + spacing)  // Diagonal inferior izquierda
            };

            // Verificar cada posición
            for (Position pos : positionsAround) {
                if (!checkCollision(pos.x, pos.y, minerSize, minerSize) &&
                        pos.x >= 0 && pos.y >= 0 &&
                        pos.x + minerSize <= windowWidth &&
                        pos.y + minerSize <= windowHeight) {

                    System.out.println("✅ Encontrada posición junto a otro minero");
                    return pos;
                }
            }
        }

        // Si no hay espacio junto a mineros existentes, buscar cualquier espacio libre
        return findAnyFreeSpace(minerSize, spacing);
    }

    /**
     * Busca cualquier espacio libre en el mapa (último recurso)
     */
    private Position findAnyFreeSpace(double minerSize, double spacing) {
        // Buscar en una cuadrícula por todo el mapa
        int gridSize = 20;
        double cellSize = minerSize + spacing;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                double x = col * cellSize;
                double y = row * cellSize;

                if (!checkCollision(x, y, minerSize, minerSize) &&
                        x + minerSize <= windowWidth &&
                        y + minerSize <= windowHeight) {

                    System.out.println("✅ Encontrado espacio libre en cuadrícula: (" + (int)x + ", " + (int)y + ")");
                    return new Position(x, y);
                }
            }
        }

        return null; // No hay espacio libre en absoluto
    }

    /**
     * Obtiene todos los mineros existentes
     */
    private List<ImageView> getExistingMiners() {
        List<ImageView> miners = new ArrayList<>();

        // Buscar todos los ImageView que sean mineros
        // (Podrías usar una etiqueta o propiedad para identificarlos)
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView && node != buildingGhost) {
                ImageView imageView = (ImageView) node;
                // Asumiendo que los mineros tienen tamaño 80px
                if (imageView.getFitWidth() == 80 && imageView.getFitHeight() == 80) {
                    miners.add(imageView);
                }
            }
        }

        return miners;
    }

    /**
     * Crea el minero en una posición específica
     */
    private void createMinerAtPosition(double x, double y, double size) {
        try {
            // Cargar imagen del minero
            String imagePath = "file:src/main/resources/images/minero.png";
            Image minerImage = new Image(imagePath);

            // Crear ImageView del minero
            ImageView minerView = new ImageView(minerImage);
            minerView.setFitWidth(size);
            minerView.setFitHeight(size);
            minerView.setPreserveRatio(true);
            minerView.setX(x);
            minerView.setY(y);

            // Añadir etiqueta para identificarlo como minero
            minerView.setId("miner_" + System.currentTimeMillis());

            // Añadir efectos visuales
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.4));
            shadow.setRadius(8);
            minerView.setEffect(shadow);

            // Animación de aparición
            FadeTransition fade = new FadeTransition(Duration.millis(300), minerView);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);

            ScaleTransition scale = new ScaleTransition(Duration.millis(300), minerView);
            scale.setFromX(0.3);
            scale.setFromY(0.3);
            scale.setToX(1.0);
            scale.setToY(1.0);

            // Añadir al root
            root.getChildren().add(minerView);

            // Hacerlo interactivo
            //makeMinerInteractive(minerView);

            // Reproducir animaciones
            javafx.animation.ParallelTransition parallel =
                    new javafx.animation.ParallelTransition(fade, scale);
            parallel.play();

            System.out.println("✅ Minero creado en: (" + (int)x + ", " + (int)y + ")");

        } catch (Exception e) {
            System.err.println("❌ Error al crear minero: " + e.getMessage());
            throw e;
        }
    }

    private void makeMinerInteractive(ImageView minerView) {
        // 1. Hacer clicable
        minerView.setOnMouseClicked(e -> {
            System.out.println("⛏️ Minero seleccionado");
            e.consume(); // Evitar que el evento se propague

        });


    }

    /**
     * Clase auxiliar para representar posiciones
     */
    private class Position {
        double x;
        double y;

        Position(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Método mejorado de checkCollision que ignora a otros mineros al buscar espacio
     */
    private boolean checkCollisionForMiner(double x, double y, double width, double height) {
        Rectangle newBounds = new Rectangle(x, y, width, height);

        for (Node node : root.getChildren()) {
            if (node instanceof ImageView && node != buildingGhost) {
                ImageView existing = (ImageView) node;

                // Ignorar otros mineros (tamaño 80x80)
                if (existing.getFitWidth() == 80 && existing.getFitHeight() == 80) {
                    continue; // Saltar mineros, permitir que se agrupen
                }

                Rectangle existingBounds = new Rectangle(
                        existing.getX(),
                        existing.getY(),
                        existing.getFitWidth(),
                        existing.getFitHeight()
                );

                if (newBounds.intersects(existingBounds.getBoundsInLocal())) {
                    return true;
                }
            }
        }

        return false;
    }



    private void enterBuildingMode(String buildingType) {
        this.isBuildingMode = true;
        this.currentBuildingType = buildingType;


        try {
            // Cargar la imagen correspondiente
            String imagePath = "file:src/main/resources/images/" +
                    buildingType + ".png";
            Image buildingImage = new Image(imagePath);

            // Configurar el fantasma (imagen semi-transparente)
            buildingGhost.setImage(buildingImage);
            if(buildingType.equalsIgnoreCase("Cuartel")) {
                width = 170;
                height = 170;
            }
            else{
                width = 100;
                height = 100;

            }

            buildingGhost.setFitWidth(width); // Tamaño ajustable
            buildingGhost.setFitHeight(height);
            buildingGhost.setPreserveRatio(true);
            buildingGhost.setOpacity(0.6); // 60% de opacidad para ver dónde se coloca
            buildingGhost.setVisible(true);

            // Cambiar cursor para indicar modo construcción
            root.setCursor(javafx.scene.Cursor.CROSSHAIR);

            System.out.println("✅ Modo construcción activado para: " + buildingType);
            System.out.println("💡 Haz clic en cualquier lugar del mapa para colocar el edificio");
            System.out.println("⎋ Presiona ESC para cancelar");

            // Agregar listener para cancelar con ESC
            root.getScene().setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    cancelBuildingMode();
                }
            });

        } catch (Exception e) {
            System.err.println("❌ Error al cargar imagen del edificio: " + e.getMessage());
            cancelBuildingMode();
        }
    }

    private void cancelBuildingMode() {
        isBuildingMode = false;
        currentBuildingType = "";
        buildingGhost.setVisible(false);
        root.setCursor(javafx.scene.Cursor.DEFAULT);
        System.out.println("❌ Modo construcción cancelado");
    }

    /**
     * Coloca el edificio en la posición especificada
     */
    private void placeBuilding(double x, double y) {
        if (!isBuildingMode) return;

        // Calcular posición centrada
        double buildingWidth = width; // Mismo tamaño que el fantasma
        double buildingHeight = height;
        double posX = x - buildingWidth / 2;
        double posY = y - buildingHeight / 2;

        // Verificar colisiones
        if (checkCollision(posX, posY, buildingWidth, buildingHeight)) {
            System.out.println("❌ No se puede construir aquí - Colisión detectada");
            showCollisionFeedback();
            return; // No colocar el edificio
        }

        // También verificar que esté dentro de los límites del mapa
        if (posX < 0 || posY < 0 ||
                posX + buildingWidth > windowWidth ||
                posY + buildingHeight > windowHeight) {
            System.out.println("❌ No se puede construir fuera del mapa");
            showOutOfBoundsFeedback();
            return;
        }

        try {
            // Cargar la imagen del edificio
            String imagePath = "file:src/main/resources/images/" +
                    currentBuildingType + ".png";
            Image buildingImage = new Image(imagePath);

            // Crear ImageView para el edificio real
            ImageView buildingView = new ImageView(buildingImage);
            buildingView.setFitWidth(buildingWidth);
            buildingView.setFitHeight(buildingHeight);
            buildingView.setPreserveRatio(true);

            // Posicionar el edificio
            buildingView.setX(posX);
            buildingView.setY(posY);

            // Añadir efectos visuales
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.5));
            shadow.setRadius(10);
            shadow.setSpread(0.1);
            buildingView.setEffect(shadow);

            // Animación de aparición
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

            // Añadir al root
            root.getChildren().add(buildingView);

            // Añadir a la lista de edificios colocados
            placedBuildings.add(buildingView);

            // Hacer el edificio interactivo
            makeBuildingInteractive(buildingView, currentBuildingType);

            System.out.println("✅ " + currentBuildingType + " construido en: (" +
                    (int)posX + ", " + (int)posY + ")");

            // Salir del modo construcción
            cancelBuildingMode();

        } catch (Exception e) {
            System.err.println("❌ Error al colocar edificio: " + e.getMessage());
            cancelBuildingMode();
        }
    }

    private void showCollisionFeedback() {
        if (!isBuildingMode) return;

        // 1. Cambiar a color rojo con opacidad
        buildingGhost.setEffect(new javafx.scene.effect.ColorAdjust());
        javafx.scene.effect.ColorAdjust redTint = new javafx.scene.effect.ColorAdjust();
        redTint.setHue(1.0); // Cambia a tono rojizo
        buildingGhost.setEffect(redTint);

        // 2. Animación de sacudida
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

        // Combinar animaciones X e Y
        javafx.animation.ParallelTransition shake =
                new javafx.animation.ParallelTransition(shakeX, shakeY);

        shake.setOnFinished(e -> {
            // Restaurar color normal después de la animación
            buildingGhost.setEffect(null);
            buildingGhost.setTranslateX(0);
            buildingGhost.setTranslateY(0);
        });

        shake.play();

    }

    private void showOutOfBoundsFeedback() {
        if (!isBuildingMode) return;

        // Efecto similar pero con diferente mensaje
        javafx.scene.effect.ColorAdjust blueTint = new javafx.scene.effect.ColorAdjust();
        blueTint.setHue(-0.7); // Cambia a tono azulado
        buildingGhost.setEffect(blueTint);

        // Animación de pulso
        FadeTransition pulse = new FadeTransition(Duration.millis(300), buildingGhost);
        pulse.setFromValue(0.4);
        pulse.setToValue(0.8);
        pulse.setCycleCount(4);
        pulse.setAutoReverse(true);

        pulse.setOnFinished(e -> {
            buildingGhost.setEffect(null);
            buildingGhost.setOpacity(0.6); // Volver a la opacidad original
        });

        pulse.play();

    }

    /**
     * Hace el edificio interactivo (clickeable, etc.)
     */
    private void makeBuildingInteractive(ImageView buildingView, String buildingType) {
        buildingView.setOnMouseClicked(e -> {
            System.out.println("🏠 " + buildingType + " clickeado");
            // Aquí podrías añadir más funcionalidades
            // como mostrar información, menú de acciones, etc.
        });

        buildingView.setOnMouseEntered(e -> {
            buildingView.setCursor(javafx.scene.Cursor.HAND);
            buildingView.setScaleX(1.05);
            buildingView.setScaleY(1.05);
        });

        buildingView.setOnMouseExited(e -> {
            buildingView.setCursor(javafx.scene.Cursor.DEFAULT);
            buildingView.setScaleX(1.0);
            buildingView.setScaleY(1.0);
        });
    }


    private Button createTextButton(String icon, String text, String cost) {
        HBox buttonContent = new HBox(10);
        buttonContent.setAlignment(Pos.CENTER_LEFT);
        buttonContent.setPadding(new Insets(5, 15, 5, 15));

        // Icono
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px; -fx-padding: 0 10 0 0;");

        // Contenedor para texto y costo (alineados verticalmente)
        VBox textContainer = new VBox(2);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        // Texto principal
        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Costo (más pequeño)
        Label costLabel = new Label(cost);
        costLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        textContainer.getChildren().addAll(textLabel, costLabel);
        buttonContent.getChildren().addAll(iconLabel, textContainer);

        Button button = new Button();
        button.setGraphic(buttonContent);
        button.setPrefWidth(260);  // Ancho fijo para todos los botones
        button.setPrefHeight(55);
        button.setAlignment(Pos.CENTER_LEFT);

        // Misma opacidad del panel (50%)
        Color buttonBaseColor = Color.rgb(255, 255, 255, 0.50); // 50% de opacidad
        Color buttonHoverColor = Color.rgb(236, 240, 241, 0.50); // 50% de opacidad

        // Estilo del botón con 50% de opacidad
        button.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.5); " + // Equivalente a 50% opacidad
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #2c3e50;"
        );

        // Efecto hover con 50% de opacidad
        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: rgba(236, 240, 241, 0.5); " + // 50% de opacidad
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
                    "-fx-background-color: rgba(255, 255, 255, 0.5); " + // 50% de opacidad
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
        // Inicialmente invisible y ligeramente escalado
        panel.setScaleX(0.9);
        panel.setScaleY(0.9);
        panel.setOpacity(0);

        // Animación combinada
        ScaleTransition scale = new ScaleTransition(Duration.millis(400), panel);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(400), panel);
        fade.setToValue(1.0);
        fade.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        // Animaciones en paralelo
        javafx.animation.ParallelTransition parallel = new javafx.animation.ParallelTransition(
                scale, fade
        );

        parallel.play();
    }


    private Node createSeparator() {
        Rectangle separator = new Rectangle(250, 1);
        separator.setFill(Color.rgb(52, 152, 219, 0.5));
        return separator;
    }

    private void animatePanelEntrance(VBox panel) {
        // Animación de escala (crece desde el centro)
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), panel);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1.0);
        scale.setToY(1.0);

        // Animación de fade (aparece gradualmente)
        FadeTransition fade = new FadeTransition(Duration.millis(300), panel);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        // Ejecutar animaciones en paralelo
        scale.play();
        fade.play();
    }

    private void showConstructionAnimation(String buildingType) {
        System.out.println("🔨 Iniciando construcción de: " + buildingType);
        System.out.println("⏳ Tiempo estimado: 10 segundos");

        // Aquí podrías añadir efectos visuales en el futuro
        // Por ahora solo mensaje en consola
    }

    private void addPlaceholderTownHall() {
        // Código para marcador de posición si el TownHall no se carga
        Rectangle placeholder = new Rectangle(100, 100, Color.rgb(139, 69, 19, 0.8));
        placeholder.setX(windowWidth * 0.3 - 50);
        placeholder.setY(windowHeight * 0.4 - 50);
        placeholder.setStroke(Color.GOLD);
        placeholder.setStrokeWidth(2);

        placeholder.setOnMouseClicked(e -> showTownHallMenu(windowWidth * 0.3, windowHeight * 0.4));

        root.getChildren().add(placeholder);
    }


    private void setupBuildingListeners(Scene scene) {
        // Mover la imagen fantasma con el mouse
        scene.setOnMouseMoved(event -> {
            if (isBuildingMode && buildingGhost.isVisible()) {
                // Centrar la imagen en el cursor
                double x = event.getX() - buildingGhost.getFitWidth() / 2;
                double y = event.getY() - buildingGhost.getFitHeight() / 2;

                buildingGhost.setX(x);
                buildingGhost.setY(y);

                // Verificar colisión en tiempo real y cambiar color
                if (checkCollision(x, y, buildingGhost.getFitWidth(), buildingGhost.getFitHeight())) {
                    // Rojo si hay colisión
                    javafx.scene.effect.ColorAdjust redTint = new javafx.scene.effect.ColorAdjust();
                    redTint.setHue(1.0);
                    buildingGhost.setEffect(redTint);
                } else if (x < 0 || y < 0 ||
                        x + buildingGhost.getFitWidth() > windowWidth ||
                        y + buildingGhost.getFitHeight() > windowHeight) {
                    // Azul si está fuera de límites
                    javafx.scene.effect.ColorAdjust blueTint = new javafx.scene.effect.ColorAdjust();
                    blueTint.setHue(-0.7);
                    buildingGhost.setEffect(blueTint);
                } else {
                    // Normal si es válido
                    buildingGhost.setEffect(null);
                }
            }
        });

        // Colocar el edificio al hacer click
        scene.setOnMouseClicked(event -> {
            if (isBuildingMode) {
                placeBuilding(event.getX(), event.getY());
            }
        });

        // Cancelar con click derecho
        scene.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown() && isBuildingMode) {
                cancelBuildingMode();
            }
        });
    }

    private void setMapBackground(Pane pane, double width, double height) {
        try {
            BackgroundImage background = new BackgroundImage(
                    new Image("file:src/main/resources/images/map_background.png"),
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

    // Método para verificar colisiones
    private boolean checkCollision(double x, double y, double width, double height) {
        Rectangle newBuildingBounds = new Rectangle(x, y, width, height);

        for (ImageView building : placedBuildings) {
            Rectangle existingBounds = new Rectangle(
                    building.getX(),
                    building.getY(),
                    building.getFitWidth(),
                    building.getFitHeight()
            );

            // Verificar si los rectángulos se intersecan
            if (newBuildingBounds.intersects(existingBounds.getBoundsInLocal())) {
                return true; // Hay colisión
            }
        }

        // También verificar colisión con el TownHall
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView && node != buildingGhost) {
                ImageView existingBuilding = (ImageView) node;
                // Verificar si es un edificio (no el fantasma)
                if (!existingBuilding.equals(buildingGhost)) {
                    Rectangle existingBounds = new Rectangle(
                            existingBuilding.getX(),
                            existingBuilding.getY(),
                            existingBuilding.getFitWidth(),
                            existingBuilding.getFitHeight()
                    );

                    if (newBuildingBounds.intersects(existingBounds.getBoundsInLocal())) {
                        return true;
                    }
                }
            }
        }

        return false; // No hay colisiones
    }

    private void positionInCorner(Pane element, double screenWidth, double screenHeight) {
        element.setLayoutX(screenWidth - element.getPrefWidth() - 20);
        element.setLayoutY(20);
    }

    private void centerStage(Stage stage, double width, double height) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setX((screen.getWidth() - width) / 2);
        stage.setY((screen.getHeight() - height) / 2);
    }

    public static void main(String[] args) {
        launch(args);
    }
}