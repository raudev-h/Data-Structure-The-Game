package dominion.view;

import com.almasb.fxgl.app.GameController;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import javafx.stage.*;
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
    private GameControler gameControler;
    private Player actualPlayer;
    private GameMap gameMap;
    private Territory territory1;


    @Override
    public void start(Stage stage) {
        //Configurar Conexion con Backend
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

        createUnitNextToTownHall("leñador", "minero.png", 50);
        createUnitNextToTownHall("minero", "minero.png", 50);
        createUnitNextToTownHall("leñador", "Leñador.png", 50);






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

            //agregar TownHall al territorio1
            TownHall townHall1 = new TownHall("1", territory1,100, 5);
            territory1.setTownHall(townHall1);
            //Eliminar TODO
            territory1.getTownHall().getStoredResources().addResource(ResourceType.WOOD, 160);


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

        Button houseButton = createTextButton("🏠", "Crear Casa", "60 Madera");
        Button barracksButton = createTextButton("⚔", "Crear Cuartel", "100 Madera");
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
            createUnitNextToTownHall("minero", "minero.png", 50);
        });

        lumberjackButton.setOnAction(e -> {
            System.out.println("✅ Creando Leñador...");
            townHallPopup.hide();
            createUnitNextToTownHall("leñador", "Leñador.png", 50);
        });

        buttonContainer.getChildren().addAll(
                houseButton, barracksButton, minerButton, lumberjackButton
        );


        panel.getChildren().addAll(title, separator, buttonContainer);

        return panel;
    }

    /**
     * Método genérico para crear cualquier unidad (minero, leñador, etc.)
     */
    private void createUnitNextToTownHall(String unitType, String imageName, double unitSize) {
        try {
            // 1. Obtener posición y tamaño del TownHall
            double townHallX = windowWidth * 0.3 - 85 + 100;
            double townHallY = windowHeight * 0.4 - 85 + 100;
            double townHallSize = 170;

            double spacing = 5; // Separación mínima

            // 2. Buscar posición válida
            Position validPosition = findPositionForUnit(townHallX, townHallY, townHallSize, unitSize, spacing, unitType);

            // 3. Si no hay posición, mostrar error
            if (validPosition == null) {
                System.out.println("❌ No hay espacio disponible para el " + unitType);
                return;
            }

            // 4. Crear la unidad en la posición encontrada
            createUnitAtPosition(unitType, imageName, validPosition.x, validPosition.y, unitSize);

        } catch (Exception e) {
            System.err.println("❌ Error al crear " + unitType + ": " + e.getMessage());
        }
    }

    /**
     * Busca posición para una unidad
     */
    private Position findPositionForUnit(double townHallX, double townHallY, double townHallSize,
                                         double unitSize, double spacing, String unitType) {

        System.out.println("🔍 Buscando posición para " + unitType + "...");

        // Crear lista de todas las posiciones a probar
        List<Position> positionsToTry = new ArrayList<>();

        // Radio cercano al TownHall (primera prioridad)
        generatePositionsAroundPoint(positionsToTry,
                townHallX + townHallSize/2,
                townHallY + townHallSize/2,
                townHallSize/2 + unitSize + spacing,
                16, unitSize);

        // Radio medio (segunda prioridad)
        generatePositionsAroundPoint(positionsToTry,
                townHallX + townHallSize/2,
                townHallY + townHallSize/2,
                townHallSize + unitSize * 3,
                24, unitSize);

        // Buscar posición válida
        for (Position pos : positionsToTry) {
            if (!checkCollisionForUnit(pos.x, pos.y, unitSize, unitSize, unitType) &&
                    pos.x >= 0 && pos.y >= 0 &&
                    pos.x + unitSize <= windowWidth &&
                    pos.y + unitSize <= windowHeight) {

                System.out.println("✅ Posición encontrada para " + unitType +
                        " en: (" + (int)pos.x + ", " + (int)pos.y + ")");
                return pos;
            }
        }

        // Si no hay espacio cercano, buscar cerca de otras unidades del mismo tipo
        System.out.println("⚠️ No hay espacio cerca del TownHall, buscando junto a otros " + unitType + "s...");
        return findPositionNextToOtherUnits(unitType, unitSize, spacing);
    }

    /**
     * Genera posiciones alrededor de un punto
     */
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
     * Verifica colisiones para una unidad (permite que se agrupen unidades del mismo tipo)
     */

    private boolean checkCollisionForUnit(double x, double y, double width, double height, String unitType) {
        Rectangle newBounds = new Rectangle(x, y, width, height);

        // 1. Verificar límites del mapa
        if (x < 0 || y < 0 || x + width > windowWidth || y + height > windowHeight) {
            return true; // Fuera de los límites
        }

        // 2. Verificar TODAS las unidades existentes (sin importar el tipo)
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView && node != buildingGhost) {
                ImageView existing = (ImageView) node;

                // Verificar si es una unidad (tamaño 50x50)
                if (existing.getFitWidth() == 50 && existing.getFitHeight() == 50) {
                    Rectangle existingBounds = new Rectangle(
                            existing.getX(),
                            existing.getY(),
                            existing.getFitWidth(),
                            existing.getFitHeight()
                    );

                    // IMPORTANTE: Verificar colisión con CUALQUIER unidad existente
                    // No solo con unidades del mismo tipo
                    if (newBounds.intersects(existingBounds.getBoundsInLocal())) {
                        System.out.println("⚠️ Colisión detectada con otra unidad en: (" +
                                (int)existing.getX() + ", " + (int)existing.getY() + ")");
                        return true;
                    }
                }
            }
        }

        // 3. Verificar colisión con edificios (100x100 o más grandes)
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView && node != buildingGhost) {
                ImageView existing = (ImageView) node;

                // Verificar si es un edificio (tamaño 100x100 o más)
                if (existing.getFitWidth() >= 100 || existing.getFitHeight() >= 100) {
                    Rectangle existingBounds = new Rectangle(
                            existing.getX(),
                            existing.getY(),
                            existing.getFitWidth(),
                            existing.getFitHeight()
                    );

                    // Añadir margen de seguridad alrededor de edificios
                    Rectangle paddedBounds = new Rectangle(
                            existingBounds.getX() - 10,
                            existingBounds.getY() - 10,
                            existingBounds.getWidth() + 20,
                            existingBounds.getHeight() + 20
                    );

                    if (newBounds.intersects(paddedBounds.getBoundsInLocal())) {
                        System.out.println("⚠️ Colisión detectada con edificio en: (" +
                                (int)existing.getX() + ", " + (int)existing.getY() + ")");
                        return true;
                    }
                }
            }
        }

        return false; // Espacio libre
    }
    /**
     * Crea una unidad en una posición específica
     */
    private void createUnitAtPosition(String unitType, String imageName, double x, double y, double size) {
        try {
            // Cargar imagen de la unidad
            String imagePath = "file:src/main/resources/images/" + imageName;
            Image unitImage = new Image(imagePath);

            // Crear ImageView de la unidad
            ImageView unitView = new ImageView(unitImage);
            unitView.setFitWidth(size);
            unitView.setFitHeight(size);
            unitView.setPreserveRatio(true);
            unitView.setX(x);
            unitView.setY(y);

            // Añadir etiqueta para identificarla
            unitView.setId(unitType + "_" + System.currentTimeMillis());

            // Añadir efectos visuales específicos según el tipo
            DropShadow shadow = new DropShadow();
            if (unitType.equals("minero")) {
                shadow.setColor(Color.rgb(184, 134, 11, 0.6)); // Dorado para mineros
            } else if (unitType.equals("leñador")) {
                shadow.setColor(Color.rgb(34, 139, 34, 0.6)); // Verde para leñadores
            } else {
                shadow.setColor(Color.rgb(0, 0, 0, 0.4));
            }
            shadow.setRadius(8);
            unitView.setEffect(shadow);

            // Animación de aparición
            FadeTransition fade = new FadeTransition(Duration.millis(300), unitView);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);

            ScaleTransition scale = new ScaleTransition(Duration.millis(300), unitView);
            scale.setFromX(0.3);
            scale.setFromY(0.3);
            scale.setToX(1.0);
            scale.setToY(1.0);

            // Añadir al root
            root.getChildren().add(unitView);


            // Reproducir animaciones
            javafx.animation.ParallelTransition parallel =
                    new javafx.animation.ParallelTransition(fade, scale);
            parallel.play();

            System.out.println("✅ " + unitType + " creado en: (" + (int)x + ", " + (int)y + ")");

            // Mostrar mensaje de éxito con emoji específico
            String emoji = unitType.equals("minero") ? "⛏️" : "🪓";

        } catch (Exception e) {
            System.err.println("❌ Error al crear " + unitType + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Capitaliza la primera letra de un string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Determina si un ImageView es de un tipo específico de unidad
     */
    private boolean isUnitType(ImageView imageView, String unitType) {
        // Podemos identificar por tamaño (50x50) o por etiqueta
        if (imageView.getFitWidth() == 50 && imageView.getFitHeight() == 50) {
            // Verificar por nombre de archivo o propiedad
            if (imageView.getId() != null && imageView.getId().startsWith(unitType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Busca posición junto a otras unidades del mismo tipo (MEJORADO)
     */
    private Position findPositionNextToOtherUnits(String unitType, double unitSize, double spacing) {
        List<ImageView> existingUnits = getExistingUnits(unitType);

        if (existingUnits.isEmpty()) {
            System.out.println("📭 No hay " + unitType + "s existentes, buscando espacio libre...");
            return findAnyFreeSpace(unitSize, spacing);
        }

        System.out.println("🔍 Buscando junto a " + existingUnits.size() + " " + unitType + "s existentes...");

        // Probar alrededor de cada unidad existente
        for (ImageView unit : existingUnits) {
            double unitX = unit.getX();
            double unitY = unit.getY();

            // Generar 8 posiciones alrededor (como puntos de una rosa de los vientos)
            Position[] positionsAround = {
                    new Position(unitX + unitSize + spacing, unitY), // Este
                    new Position(unitX - unitSize - spacing, unitY), // Oeste
                    new Position(unitX, unitY - unitSize - spacing), // Norte
                    new Position(unitX, unitY + unitSize + spacing), // Sur
                    new Position(unitX + unitSize + spacing, unitY - unitSize - spacing), // Noreste
                    new Position(unitX - unitSize - spacing, unitY - unitSize - spacing), // Noroeste
                    new Position(unitX + unitSize + spacing, unitY + unitSize + spacing), // Sureste
                    new Position(unitX - unitSize - spacing, unitY + unitSize - spacing)  // Suroeste
            };

            // Verificar cada posición
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

        // Si no hay espacio junto a unidades existentes, buscar cualquier espacio libre
        System.out.println("⚠️ No hay espacio junto a " + unitType + "s existentes, buscando en todo el mapa...");
        return findAnyFreeSpace(unitSize, spacing);
    }

    /**
     * Busca cualquier espacio libre en el mapa (último recurso)
     */
    private Position findAnyFreeSpace(double unitSize, double spacing) {
        System.out.println("🔍 Buscando espacio libre en todo el mapa...");

        // Crear una cuadrícula para buscar espacios
        int gridCols = (int) (windowWidth / (unitSize + spacing));
        int gridRows = (int) (windowHeight / (unitSize + spacing));

        // Primero, buscar cerca del TownHall (radio más grande)
        double townHallCenterX = windowWidth * 0.3 + 15; // Centro del TownHall
        double townHallCenterY = windowHeight * 0.4 + 15;
        double searchRadius = 300; // Radio amplio de búsqueda

        // Buscar en anillos concéntricos alrededor del TownHall
        for (int radius = 1; radius <= 10; radius++) {
            double currentRadius = searchRadius * (radius / 10.0);

            // Buscar en puntos alrededor del círculo
            for (int i = 0; i < 16; i++) {
                double angle = 2 * Math.PI * i / 16;
                double x = townHallCenterX + Math.cos(angle) * currentRadius - unitSize/2;
                double y = townHallCenterY + Math.sin(angle) * currentRadius - unitSize/2;

                // Asegurar que esté dentro de los límites
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

        // Si no se encuentra cerca del TownHall, buscar en cuadrícula por todo el mapa
        System.out.println("🌍 Buscando en cuadrícula por todo el mapa...");

        // Dividir el mapa en celdas y buscar
        double cellSize = unitSize + spacing * 2;
        int cols = (int) (windowWidth / cellSize);
        int rows = (int) (windowHeight / cellSize);

        // Buscar de forma aleatoria pero sistemática
        java.util.Random random = new java.util.Random();

        for (int attempt = 0; attempt < cols * rows * 2; attempt++) {
            int col = random.nextInt(cols);
            int row = random.nextInt(rows);

            double x = col * cellSize + spacing;
            double y = row * cellSize + spacing;

            // Asegurar que no salga de los límites
            if (x + unitSize > windowWidth) continue;
            if (y + unitSize > windowHeight) continue;

            if (!checkCollisionForUnit(x, y, unitSize, unitSize, "unidad")) {
                System.out.println("✅ Espacio encontrado en cuadrícula (" + col + ", " + row + ")");
                return new Position(x, y);
            }
        }

        // Último intento: búsqueda exhaustiva celda por celda
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

        // Si llegamos aquí, el mapa está completamente lleno
        System.out.println("❌ El mapa está completamente lleno");
        return null;
    }

    /**
     * Obtiene todas las unidades existentes de un tipo específico
     */
    private List<ImageView> getExistingUnits(String unitType) {
        List<ImageView> units = new ArrayList<>();

        for (Node node : root.getChildren()) {
            if (node instanceof ImageView && node != buildingGhost) {
                ImageView imageView = (ImageView) node;
                // Asumiendo que las unidades tienen tamaño 50px
                if (imageView.getFitWidth() == 50
                        && imageView.getFitHeight() == 50) {
                    // Filtrar por tipo si es necesario
                    if (imageView.getId() != null && imageView.getId().startsWith(unitType)) {
                        units.add(imageView);
                    } else if (unitType.equals("unidad")) { // Caso genérico
                        units.add(imageView);
                    }
                }
            }
        }

        return units;
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





    private void enterBuildingMode(String buildingType) {
        this.isBuildingMode = true;
        this.currentBuildingType = buildingType;
        boolean construir = true;

        if(currentBuildingType.equalsIgnoreCase("Casa"))
            construir = territory1.getTownHall().canCreateHouse();
        else if(currentBuildingType.equalsIgnoreCase("Cuartel"))
            construir = territory1.getTownHall().canCreateMilitaryBase();

        if(construir){


                try {
                    // Cargar la imagen correspondiente
                    String imagePath = "file:src/main/resources/images/" +
                            buildingType + ".png";
                    Image buildingImage = new Image(imagePath);

                    // Configurar el fantasma (imagen semi-transparente)
                    buildingGhost.setImage(buildingImage);
                    if (buildingType.equalsIgnoreCase("Cuartel")) {
                        width = 170;
                        height = 170;
                    } else {
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

                } catch (Exception ex) {
                    System.err.println("❌ Error al cargar imagen del edificio: " + ex.getMessage());

                }
            }
        else{
                // Crear Stage para el warning
                Stage warningStage = new Stage();
                warningStage.initModality(Modality.APPLICATION_MODAL);
                warningStage.initStyle(StageStyle.TRANSPARENT); // Sin bordes de ventana
                warningStage.setTitle("Materiales insuficientes");

                // Panel principal con el MISMO estilo transparente
                VBox warningPanel = new VBox(15);
                warningPanel.setPadding(new Insets(25, 30, 25, 30));
                warningPanel.setAlignment(Pos.CENTER);
                warningPanel.setStyle(
                        "-fx-background-color: rgba(255, 255, 255, 0.50); " + // MISMA opacidad 50%
                                "-fx-background-radius: 15; " +
                                "-fx-border-color: #dcdde1; " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 15; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.5, 0, 2);"
                );

                // Icono de advertencia (usa tu fuente de iconos)
                Label warningIcon = new Label("⚠");
                warningIcon.setStyle("-fx-font-size: 36px; -fx-padding: 0 0 5 0;");

                // Mensaje con el MISMO estilo
                VBox messageContainer = new VBox(5);
                messageContainer.setAlignment(Pos.CENTER);

                Label titleLabel = new Label("Materiales insuficientes");
                titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                Label detailLabel = new Label("No tienes los recursos necesarios\npara construir este edificio");
                detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #000000; -fx-text-alignment: center;");
                detailLabel.setWrapText(true);

                messageContainer.getChildren().addAll(titleLabel, detailLabel);

                // Botón "Entendido" con el MISMO estilo
                Button okButton = new Button("Entendido");
                okButton.setPrefWidth(150);
                okButton.setPrefHeight(38);
                okButton.setStyle(
                        "-fx-background-color: rgba(255, 255, 255, 0.5); " + // 50% de opacidad
                                "-fx-background-radius: 6; " +
                                "-fx-border-color: #dcdde1; " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 6; " +
                                "-fx-cursor: hand; " +
                                "-fx-text-fill: #2c3e50; " +
                                "-fx-font-size: 12px; " +
                                "-fx-font-weight: bold;"
                );

                // Efecto hover IDÉNTICO
                okButton.setOnMouseEntered(e -> {
                    okButton.setStyle(
                            "-fx-background-color: rgba(236, 240, 241, 0.5); " + // 50% de opacidad
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
                            "-fx-background-color: rgba(255, 255, 255, 0.5); " + // 50% de opacidad
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

                // Añadir al panel
                warningPanel.getChildren().addAll(warningIcon, messageContainer, okButton);

                // Crear StackPane para centrar y agregar fondo transparente
                StackPane rootPane = new StackPane(warningPanel);
                rootPane.setStyle("-fx-background-color: transparent;");
                rootPane.setAlignment(Pos.CENTER);

                // Escena
                Scene warningScene = new Scene(rootPane, 300, 250);
                warningScene.setFill(Color.TRANSPARENT);

                // Posicionar en el centro de la ventana principal
                warningStage.initOwner(root.getScene().getWindow());
                warningStage.setScene(warningScene);
                warningStage.setResizable(false);

                // Mostrar y esperar
                warningStage.showAndWait();

            this.isBuildingMode = false;
            this.currentBuildingType = null;
            buildingGhost.setVisible(false);
            root.setCursor(javafx.scene.Cursor.DEFAULT);
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
        double buildingWidth = width;
        double buildingHeight = height;
        double posX = x - buildingWidth / 2;
        double posY = y - buildingHeight / 2;

        // Verificar colisiones PRIMERO
        if (checkCollision(posX, posY, buildingWidth, buildingHeight)) {
            System.out.println("❌ No se puede construir aquí - Colisión detectada");
            showCollisionFeedback();
            return;
        }

        // Verificar límites del mapa
        if (posX < 0 || posY < 0 ||
                posX + buildingWidth > windowWidth ||
                posY + buildingHeight > windowHeight) {
            System.out.println("❌ No se puede construir fuera del mapa");
            showOutOfBoundsFeedback();
            return;
        }

        // INTENTAR CREAR EL EDIFICIO EN EL BACKEND PRIMERO
        boolean creado = false;

        if(currentBuildingType.equalsIgnoreCase("Casa")){
            creado = territory1.getTownHall().createHouse(); // Esto gastará recursos
        }
        else if(currentBuildingType.equalsIgnoreCase("Cuartel")){
            creado = territory1.getTownHall().createMilitaryBase(); // Esto gastará recursos
        }

        // Si NO se pudo crear (debería ser raro ya que verificamos con canCreate)
        if (!creado) {
            System.out.println("❌ Error: No se pudo crear el edificio en el backend");
            cancelBuildingMode();
            return;
        }

        // Si SÍ se creó exitosamente, ahora mostrar visualmente
        try {
            String imagePath = "file:src/main/resources/images/" +
                    currentBuildingType + ".png";
            Image buildingImage = new Image(imagePath);

            ImageView buildingView = new ImageView(buildingImage);
            buildingView.setFitWidth(buildingWidth);
            buildingView.setFitHeight(buildingHeight);
            buildingView.setPreserveRatio(true);
            buildingView.setX(posX);
            buildingView.setY(posY);

            // Efectos visuales
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.5));
            shadow.setRadius(10);
            shadow.setSpread(0.1);
            buildingView.setEffect(shadow);

            // Animación
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

            // Añadir a la escena
            root.getChildren().add(buildingView);
            placedBuildings.add(buildingView);
            makeBuildingInteractive(buildingView, currentBuildingType);

            System.out.println("✅ " + currentBuildingType + " construido en: (" +
                    (int)posX + ", " + (int)posY + ")");

            cancelBuildingMode();

        } catch (Exception e) {
            System.err.println("❌ Error al colocar edificio visualmente: " + e.getMessage());

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