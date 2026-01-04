package dominion.view;

import dominion.model.buildings.ConstructionOrder;
import dominion.model.buildings.MilitaryBase;
import dominion.model.buildings.UnitCreationOrder;
import dominion.model.units.Knight;
import javafx.animation.*;
import dominion.core.GameControler;
import dominion.core.GameMap;
import dominion.model.buildings.TownHall;
import dominion.model.players.Player;
import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;
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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.*;
import javafx.util.Duration;

import java.util.*;

public class GameApp extends Application {

    private Pane root;
    private double windowWidth;
    private double windowHeight;
    private Popup townHallPopup;
    private boolean isBuildingMode = false;
    private boolean wasDragSelect = false;

    // Añade estos campos a la clase para almacenar referencias
    private javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> savedMousePressed;
    private javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> savedMouseDragged;
    private javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> savedMouseReleased;


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
    private final List<ImageView> placedBuildings = new ArrayList<>();
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
    private final List<ImageView> createdKnights = new ArrayList<>(); // Para rastrear caballeros creados
    // ==================== VARIABLES PARA CONSTRUCCIÓN ====================
    private final Map<String, ImageView> constructionVisuals = new HashMap<>();
    private final Map<String, String> buildingTypesUnderConstruction = new HashMap<>();
    private Timeline constructionUpdateTimeline;
    private final Map<String, Position> buildingPositions = new HashMap<>();

    private BarraProgresoAnimadaManager barraProgresoManager = new BarraProgresoAnimadaManager();


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

        startConstructionUpdateLoop();

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
        createUnitNextToTownHall("leñador", "minero.png", 50);
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

    // En el método start(), después de inicializar todo:
    private void startConstructionUpdateLoop() {
        constructionUpdateTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateConstructions())
        );
        constructionUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        constructionUpdateTimeline.play();
        System.out.println("▶️ Iniciando ciclo de actualización de construcciones");
    }
    private void stopConstructionUpdateLoop() {
        if (constructionUpdateTimeline != null) {
            constructionUpdateTimeline.stop();
            System.out.println("⏹️ Deteniendo ciclo de actualización de construcciones");
        }
    }
    private void restartConstructionUpdateLoopIfNeeded() {
        if (constructionUpdateTimeline == null || constructionUpdateTimeline.getStatus() != Animation.Status.RUNNING) {
            startConstructionUpdateLoop();
        }
    }


    private void updateConstructions() {
        // Procesar la cola de construcción del backend
        if (territory1 != null && territory1.getTownHall() != null) {
            territory1.getTownHall().processConstructionQueue();

            // Procesar entrenamientos de unidades
            processUnitTrainingQueue();

            // Sincronizar con el backend
            syncConstructionsWithBackend();
        }
    }

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

    private void removeFromSelection(ImageView iv) {
        selectedUnitViews.remove(iv);
        applySelectionStyle(iv, false); //
    }
    private void sendSelectedWoodcuttersToTree(ImageView tree) {
        // Filtrar solo los leñadores seleccionados
        List<ImageView> woodcutters = new ArrayList<>();

        for (ImageView unit : selectedUnitViews) {
            if (isWoodcutter(unit)) {
                woodcutters.add(unit);
            }
        }

        if (woodcutters.isEmpty()) {
            System.out.println("⚠️ No hay leñadores seleccionados");
            return;
        }

        // Detener cualquier animación de movimiento actual
        for (ImageView woodcutter : woodcutters) {
            stopWoodcuttingIfActive(woodcutter);

            // Detener cualquier animación de movimiento en curso
            woodcutter.getTransforms().clear();
            woodcutter.setTranslateX(0);
            woodcutter.setTranslateY(0);
        }

        // Obtener posición alrededor del árbol
        double treeX = tree.getX();
        double treeY = tree.getY();
        double treeWidth = tree.getFitWidth();
        double treeHeight = tree.getFitHeight();

        // Calcular formación alrededor del árbol
        moveWoodcuttersToTree(woodcutters, treeX, treeY, treeWidth, treeHeight);

        // Iniciar tala para cada leñador
        for (ImageView woodcutter : woodcutters) {
            startWoodcutting(woodcutter, tree);
        }

        // NO LIMPIAR la selección aquí - mantenlos seleccionados
        // clearSelectedUnitViews(); // <-- COMENTA O ELIMINA ESTA LÍNEA
    }

    private boolean isWoodcutter(ImageView unit) {
        Object userData = unit.getUserData();
        String id = unit.getId();
        return (userData instanceof String && ((String) userData).equals("leñador")) ||
                (id != null && id.startsWith("leñador_"));
    }

    private void moveWoodcuttersToTree(List<ImageView> woodcutters, double treeX, double treeY,
                                       double treeWidth, double treeHeight) {
        int count = woodcutters.size();
        double treeCenterX = treeX + treeWidth / 2;
        double treeCenterY = treeY + treeHeight / 2;
        double radius = Math.max(treeWidth, treeHeight) / 2 + 60; // Distancia alrededor del árbol
        double unitSize = 50;

        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double targetX = treeCenterX + Math.cos(angle) * radius - unitSize / 2;
            double targetY = treeCenterY + Math.sin(angle) * radius - unitSize / 2;

            // Asegurar que está dentro de los límites
            targetX = Math.max(0, Math.min(targetX, windowWidth - unitSize));
            targetY = Math.max(0, Math.min(targetY, windowHeight - unitSize));

            animateMove(woodcutters.get(i), targetX, targetY);
        }
    }
    /**
     * Encuentra una mina en las coordenadas dadas (x, y)
     */
    private ImageView getMineAt(double x, double y) {
        System.out.println("🔍 Buscando mina en coordenadas: (" + x + ", " + y + ")");

        // Primero, buscar minas con ID explícito
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView imageView) {
                String id = imageView.getId();

                // Verificar si tiene ID de mina
                if (id != null && id.startsWith("Mina_")) {
                    Bounds bounds = imageView.getBoundsInParent();
                    System.out.println("   - Revisando mina: " + id + " - Bounds: " +
                            bounds.getMinX() + "," + bounds.getMinY() + " -> " +
                            bounds.getMaxX() + "," + bounds.getMaxY());

                    if (bounds.contains(x, y)) {
                        System.out.println("✅ Mina encontrada: " + id);
                        return imageView;
                    }
                }
            }
        }

        // Si no encontró por ID, buscar por UserData
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView imageView) {
                Object userData = imageView.getUserData();
                if (userData instanceof String && ((String) userData).equals("mina")) {
                    Bounds bounds = imageView.getBoundsInParent();
                    if (bounds.contains(x, y)) {
                        System.out.println("✅ Mina encontrada por UserData");
                        return imageView;
                    }
                }
            }
        }

        // Si no encontró, buscar por tamaño aproximado (45px)
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView imageView) {
                double width = imageView.getFitWidth();
                double height = imageView.getFitHeight();

                // Verificar si tiene el tamaño de una mina
                if (Math.abs(width - 45) < 5 && Math.abs(height - 45) < 5) {
                    Bounds bounds = imageView.getBoundsInParent();
                    if (bounds.contains(x, y)) {
                        System.out.println("✅ Mina encontrada por tamaño");
                        return imageView;
                    }
                }
            }
        }

        System.out.println("❌ No se encontró mina en estas coordenadas");
        return null;
    }

    private void handleUnitClickOrMove(double x, double y, boolean shiftDown) {
        System.out.println("🖱️ Click en coordenadas: (" + x + ", " + y + ") - Shift: " + shiftDown);
        System.out.println("📊 Unidades seleccionadas actualmente: " + selectedUnitViews.size());

        // Listar las unidades seleccionadas para depuración
        for (ImageView unit : selectedUnitViews) {
            System.out.println("   - " + unit.getId() + " - Tipo: " + unit.getUserData());
        }

        // Primero verificar si se hizo click en una mina
        ImageView clickedMine = getMineAt(x, y);

        // Verificar si hay mineros seleccionados y se clickeó una mina
        if (clickedMine != null) {
            System.out.println("🎯 Mina clickeada: " + clickedMine.getId());

            // Verificar si hay mineros seleccionados
            boolean hasMiner = false;
            List<ImageView> miners = new ArrayList<>();

            for (ImageView unit : selectedUnitViews) {
                if (isMiner(unit)) {
                    hasMiner = true;
                    miners.add(unit);
                    System.out.println("   ✅ Minero seleccionado: " + unit.getId());
                }
            }

            if (hasMiner) {
                System.out.println("🎯 Enviando " + miners.size() + " mineros a la mina...");
                sendSelectedMinersToMine(clickedMine);
                return;
            } else {
                System.out.println("🎯 Click en mina pero sin mineros seleccionados");
                // No retornar aquí, continuar con la lógica normal
            }
        }

        // Verificar si se hizo click en un árbol
        ImageView clickedTree = getTreeAt(x, y);

        // Verificar si hay leñadores seleccionados y se clickeó un árbol
        if (clickedTree != null) {
            boolean hasWoodcutter = false;
            for (ImageView unit : selectedUnitViews) {
                if (isWoodcutter(unit)) {
                    hasWoodcutter = true;
                    break;
                }
            }

            if (hasWoodcutter) {
                System.out.println("🎯 Click en árbol con leñadores seleccionados - Enviando a talar...");
                sendSelectedWoodcuttersToTree(clickedTree);
                return;
            } else {
                System.out.println("🎯 Click en árbol pero sin leñadores seleccionados");
            }
        }

        // Si no se clickeó en mina o árbol, continuar con la lógica normal de selección/movimiento
        ImageView clicked = getUnitViewAt(x, y);

        if (clicked != null) {
            System.out.println("🎯 Click en unidad: " + clicked.getId() + " - UserData: " + clicked.getUserData());

            if (!shiftDown && selectedUnitViews.contains(clicked)) {
                removeFromSelection(clicked);
                return;
            }

            if (!shiftDown) clearSelectedUnitViews();

            if (shiftDown && selectedUnitViews.contains(clicked)) {
                removeFromSelection(clicked);
            } else {
                addToSelection(clicked);
            }
            return;
        }

        // Click en suelo: mover lo seleccionado
        if (!selectedUnitViews.isEmpty()) {
            System.out.println("🎯 Moviendo " + selectedUnitViews.size() + " unidades a (" + (int)x + ", " + (int)y + ")");
            moveSelectedUnitsTo(x, y);
        }
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


    // Método para obtener un árbol en las coordenadas dadas
    private ImageView getTreeAt(double x, double y) {
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView imageView) {
                if (imageView.getId() != null && imageView.getId().startsWith("Arbol_")) {
                    Bounds bounds = imageView.getBoundsInParent();
                    if (bounds.contains(x, y)) {
                        return imageView;
                    }
                }
            }
        }
        return null;
    }
    private class MiningTask {
        ImageView miner;
        ImageView mine;
        Timeline collectionTimeline;
        Timeline mineLifeTimeline;
        int goldCollected = 0;
        boolean isActive = true;

        MiningTask(ImageView miner, ImageView mine) {
            this.miner = miner;
            this.mine = mine;
        }
    }
    private void sendSelectedMinersToMine(ImageView mine) {
        // Filtrar solo los mineros seleccionados
        List<ImageView> miners = new ArrayList<>();

        for (ImageView unit : selectedUnitViews) {
            if (isMiner(unit)) {
                miners.add(unit);
            }
        }

        if (miners.isEmpty()) {
            System.out.println("⚠️ No hay mineros seleccionados");
            return;
        }

        // Detener cualquier animación de movimiento actual
        for (ImageView miner : miners) {
            stopMiningIfActive(miner);

            // Detener cualquier animación de movimiento en curso
            miner.getTransforms().clear();
            miner.setTranslateX(0);
            miner.setTranslateY(0);
        }

        // Obtener posición alrededor de la mina
        double mineX = mine.getX();
        double mineY = mine.getY();
        double mineWidth = mine.getFitWidth();
        double mineHeight = mine.getFitHeight();

        // Calcular formación alrededor de la mina
        moveMinersToMine(miners, mineX, mineY, mineWidth, mineHeight);

        // Iniciar minería para cada minero
        for (ImageView miner : miners) {
            startMining(miner, mine);
        }
    }

    private boolean isMiner(ImageView unit) {
        Object userData = unit.getUserData();
        String id = unit.getId();
        return (userData instanceof String && ((String) userData).equals("minero")) ||
                (id != null && id.startsWith("minero_"));
    }

    private void moveMinersToMine(List<ImageView> miners, double mineX, double mineY,
                                  double mineWidth, double mineHeight) {
        int count = miners.size();
        double mineCenterX = mineX + mineWidth / 2;
        double mineCenterY = mineY + mineHeight / 2;
        double radius = Math.max(mineWidth, mineHeight) / 2 + 60; // Distancia alrededor de la mina
        double unitSize = 50;

        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double targetX = mineCenterX + Math.cos(angle) * radius - unitSize / 2;
            double targetY = mineCenterY + Math.sin(angle) * radius - unitSize / 2;

            // Asegurar que está dentro de los límites
            targetX = Math.max(0, Math.min(targetX, windowWidth - unitSize));
            targetY = Math.max(0, Math.min(targetY, windowHeight - unitSize));

            animateMove(miners.get(i), targetX, targetY);
        }
    }

    private void startMining(ImageView miner, ImageView mine) {
        // Verificar si ya hay una tarea activa para este minero
        if (activeMiningTasks.containsKey(miner)) {
            MiningTask existingTask = activeMiningTasks.get(miner);
            existingTask.isActive = false;
            if (existingTask.collectionTimeline != null) {
                existingTask.collectionTimeline.stop();
            }
            if (existingTask.mineLifeTimeline != null) {
                existingTask.mineLifeTimeline.stop();
            }
        }

        // Crear nueva tarea
        MiningTask task = new MiningTask(miner, mine);

        // Timeline para recolectar oro cada 10 segundos (10 ciclos = 100 segundos)
        task.collectionTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> collectGoldFromMine(task))
        );
        task.collectionTimeline.setCycleCount(10); // 10 ciclos = 100 segundos

        // Timeline para agotar la mina después de 100 segundos
        task.mineLifeTimeline = new Timeline(
                new KeyFrame(Duration.seconds(100), e -> depleteMine(task))
        );
        task.mineLifeTimeline.setCycleCount(1); // Solo una vez

        // Configurar lo que pasa cuando termina la recolección
        task.collectionTimeline.setOnFinished(e -> {
            System.out.println("✅ Minero completó la explotación de la mina " + mine.getId());
            task.isActive = false;
        });

        // Configurar lo que pasa cuando se agota la mina
        task.mineLifeTimeline.setOnFinished(e -> {
            System.out.println("⛏ Tiempo de vida de la mina " + mine.getId() + " terminado");
        });

        // Iniciar las timelines
        task.collectionTimeline.play();
        task.mineLifeTimeline.play();

        // Guardar la tarea
        activeMiningTasks.put(miner, task);

        System.out.println("⛏ Minero comenzó a extraer oro de la mina " + mine.getId() +
                " - 10 ciclos de 10 segundos = 100 segundos total");
    }

    private void collectGoldFromMine(MiningTask task) {
        if (!task.isActive) return;

        // Verificar que la mina aún exista
        if (!root.getChildren().contains(task.mine)) {
            task.isActive = false;
            return;
        }

        // Agregar oro al TownHall
        if (territory1 != null && territory1.getTownHall() != null) {
            territory1.getTownHall().getStoredResources().addResource(ResourceType.GOLD, 50);
            task.goldCollected += 50;

            // Actualizar display
            Platform.runLater(() -> updateResourceDisplay());

            // Mostrar efecto visual
            showGoldCollectionEffect(task.miner, task.mine);

            System.out.println("💰 +50 Oro recolectado de la mina " + task.mine.getId() +
                    " (Ciclo: " + (task.goldCollected / 50) + "/10, Total: " + task.goldCollected + ")");

            // Si ya se recolectó todo el oro, detener
            if (task.goldCollected >= 500) { // 10 ciclos * 50 = 500
                task.isActive = false;
            }
        }
    }

    private void showGoldCollectionEffect(ImageView miner, ImageView mine) {
        // Crear texto flotante
        Label goldLabel = new Label("+50 Oro");
        goldLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #FFD700;");

        // Posicionar entre el minero y la mina
        double minerX = miner.getX() + miner.getFitWidth() / 2;
        double minerY = miner.getY() + miner.getFitHeight() / 2;
        double mineX = mine.getX() + mine.getFitWidth() / 2;
        double mineY = mine.getY() + mine.getFitHeight() / 2;

        double labelX = (minerX + mineX) / 2 - 25;
        double labelY = (minerY + mineY) / 2;

        goldLabel.setLayoutX(labelX);
        goldLabel.setLayoutY(labelY);

        root.getChildren().add(goldLabel);

        // Animación del texto
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5), goldLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(1.5), goldLabel);
        moveUp.setByY(-30);

        ParallelTransition parallel = new ParallelTransition(fadeOut, moveUp);
        parallel.setOnFinished(e -> root.getChildren().remove(goldLabel));
        parallel.play();

        // Efecto de partículas de oro
        createGoldParticles(mine);
    }

    private void createGoldParticles(ImageView mine) {
        double mineX = mine.getX() + mine.getFitWidth() / 2;
        double mineY = mine.getY() + mine.getFitHeight() / 2;

        for (int i = 0; i < 5; i++) {
            Circle particle = new Circle(2, Color.rgb(255, 215, 0, 0.8));
            particle.setCenterX(mineX);
            particle.setCenterY(mineY);

            root.getChildren().add(particle);

            // Animación aleatoria
            double angle = Math.random() * 2 * Math.PI;
            double distance = 20 + Math.random() * 30;

            TranslateTransition move = new TranslateTransition(Duration.seconds(1), particle);
            move.setByX(Math.cos(angle) * distance);
            move.setByY(Math.sin(angle) * distance);

            FadeTransition fade = new FadeTransition(Duration.seconds(1), particle);
            fade.setFromValue(0.8);
            fade.setToValue(0);

            ParallelTransition particleAnim = new ParallelTransition(move, fade);
            particleAnim.setOnFinished(e -> root.getChildren().remove(particle));
            particleAnim.play();
        }
    }

    private void depleteMine(MiningTask task) {
        if (task.mine != null && root.getChildren().contains(task.mine)) {
            // Detener todas las tareas relacionadas con esta mina
            stopAllTasksForMine(task.mine);

            // Detener las timelines de esta tarea
            if (task.collectionTimeline != null) {
                task.collectionTimeline.stop();
            }
            if (task.mineLifeTimeline != null) {
                task.mineLifeTimeline.stop();
            }

            // Marcar como inactiva
            task.isActive = false;

            // Animación de desaparición de la mina
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), task.mine);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                root.getChildren().remove(task.mine);
                System.out.println("⛏ Mina " + task.mine.getId() + " agotada y removida después de 100 segundos");
            });

            fadeOut.play();

            // Remover la tarea del mapa
            activeMiningTasks.remove(task.miner);
        }
    }

    private void stopAllTasksForMine(ImageView mine) {
        List<ImageView> toRemove = new ArrayList<>();

        for (Map.Entry<ImageView, MiningTask> entry : activeMiningTasks.entrySet()) {
            if (entry.getValue().mine == mine) {
                entry.getValue().isActive = false;
                if (entry.getValue().collectionTimeline != null) {
                    entry.getValue().collectionTimeline.stop();
                }
                toRemove.add(entry.getKey());
            }
        }

        for (ImageView miner : toRemove) {
            activeMiningTasks.remove(miner);
        }
    }


    private final Map<ImageView, MiningTask> activeMiningTasks = new HashMap<>();

    private final Map<ImageView, WoodcuttingTask> activeWoodcuttingTasks = new HashMap<>();

    private class WoodcuttingTask {
        ImageView woodcutter;
        ImageView tree;
        Timeline collectionTimeline;
        Timeline treeLifeTimeline;
        int woodCollected = 0;
        boolean isActive = true;

        WoodcuttingTask(ImageView woodcutter, ImageView tree) {
            this.woodcutter = woodcutter;
            this.tree = tree;
        }
    }

    private void startWoodcutting(ImageView woodcutter, ImageView tree) {
        // Verificar si ya hay una tarea activa para este leñador
        if (activeWoodcuttingTasks.containsKey(woodcutter)) {
            WoodcuttingTask existingTask = activeWoodcuttingTasks.get(woodcutter);
            existingTask.isActive = false;
            if (existingTask.collectionTimeline != null) {
                existingTask.collectionTimeline.stop();
            }
            if (existingTask.treeLifeTimeline != null) {
                existingTask.treeLifeTimeline.stop();
            }
        }

        // Crear nueva tarea
        WoodcuttingTask task = new WoodcuttingTask(woodcutter, tree);

        // Timeline para recolectar madera cada 10 segundos (5 ciclos = 50 segundos)
        task.collectionTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> collectWoodFromTree(task))
        );
        task.collectionTimeline.setCycleCount(5); // 5 ciclos = 50 segundos

        // Timeline para eliminar el árbol después de 50 segundos
        task.treeLifeTimeline = new Timeline(
                new KeyFrame(Duration.seconds(50), e -> removeTree(task))
        );
        task.treeLifeTimeline.setCycleCount(1); // Solo una vez

        // Configurar lo que pasa cuando termina la recolección
        task.collectionTimeline.setOnFinished(e -> {
            System.out.println("✅ Leñador completó la tala del árbol " + tree.getId());
            task.isActive = false;

        });

        // Configurar lo que pasa cuando se elimina el árbol
        task.treeLifeTimeline.setOnFinished(e -> {
            // Esto ya está manejado en removeTree, pero lo dejamos por si acaso
            System.out.println("🌳 Tiempo de vida del árbol " + tree.getId() + " terminado");
        });

        // Iniciar las timelines
        task.collectionTimeline.play();
        task.treeLifeTimeline.play();

        // Guardar la tarea
        activeWoodcuttingTasks.put(woodcutter, task);

        System.out.println("🪓 Leñador comenzó a talar árbol " + tree.getId() +
                " - 5 ciclos de 10 segundos = 50 segundos total");
    }

    private void collectWoodFromTree(WoodcuttingTask task) {
        if (!task.isActive) return;

        // Agregar madera al TownHall
        if (territory1 != null && territory1.getTownHall() != null) {
            territory1.getTownHall().getStoredResources().addResource(ResourceType.WOOD, 25);

            task.woodCollected += 25;

            // Actualizar display
            Platform.runLater(() -> updateResourceDisplay());

            System.out.println("🪵 +25 Madera recolectada del árbol " + task.tree.getId() +
                    " (Ciclo: " + (task.woodCollected / 25) + "/5, Total: " + task.woodCollected + ")");
        }
    }

    private void showWoodCollectionEffect(ImageView woodcutter, ImageView tree) {
        // Crear texto flotante
        Label woodLabel = new Label("+25 Madera");
        woodLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #228B22;");

        // Posicionar entre el leñador y el árbol
        double woodcutterX = woodcutter.getX() + woodcutter.getFitWidth() / 2;
        double woodcutterY = woodcutter.getY() + woodcutter.getFitHeight() / 2;
        double treeX = tree.getX() + tree.getFitWidth() / 2;
        double treeY = tree.getY() + tree.getFitHeight() / 2;

        double labelX = (woodcutterX + treeX) / 2 - 30;
        double labelY = (woodcutterY + treeY) / 2;

        woodLabel.setLayoutX(labelX);
        woodLabel.setLayoutY(labelY);

        root.getChildren().add(woodLabel);

        // Animación del texto
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5), woodLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(1.5), woodLabel);
        moveUp.setByY(-30);

        ParallelTransition parallel = new ParallelTransition(fadeOut, moveUp);
        parallel.setOnFinished(e -> root.getChildren().remove(woodLabel));
        parallel.play();

        // Efecto de partículas de madera
        createWoodParticles(tree);
    }

    private void createWoodParticles(ImageView tree) {
        double treeX = tree.getX() + tree.getFitWidth() / 2;
        double treeY = tree.getY() + tree.getFitHeight() / 2;

        for (int i = 0; i < 5; i++) {
            Circle particle = new Circle(2, Color.rgb(139, 69, 19, 0.8));
            particle.setCenterX(treeX);
            particle.setCenterY(treeY);

            root.getChildren().add(particle);

            // Animación aleatoria
            double angle = Math.random() * 2 * Math.PI;
            double distance = 20 + Math.random() * 30;

            TranslateTransition move = new TranslateTransition(Duration.seconds(1), particle);
            move.setByX(Math.cos(angle) * distance);
            move.setByY(Math.sin(angle) * distance);

            FadeTransition fade = new FadeTransition(Duration.seconds(1), particle);
            fade.setFromValue(0.8);
            fade.setToValue(0);

            ParallelTransition particleAnim = new ParallelTransition(move, fade);
            particleAnim.setOnFinished(e -> root.getChildren().remove(particle));
            particleAnim.play();
        }
    }

    private void removeTree(WoodcuttingTask task) {
        if (task.tree != null && root.getChildren().contains(task.tree)) {
            // Detener todas las tareas relacionadas con este árbol
            stopAllTasksForTree(task.tree);

            // Detener las timelines de esta tarea
            if (task.collectionTimeline != null) {
                task.collectionTimeline.stop();
            }
            if (task.treeLifeTimeline != null) {
                task.treeLifeTimeline.stop();
            }

            // Marcar como inactiva
            task.isActive = false;

            // Animación de desaparición simple del árbol
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), task.tree);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                root.getChildren().remove(task.tree);
                System.out.println("🌳 Árbol " + task.tree.getId() + " talado y removido después de 50 segundos");
            });

            fadeOut.play();

            // Remover la tarea del mapa
            activeWoodcuttingTasks.remove(task.woodcutter);
        }
    }

    private void stopAllTasksForTree(ImageView tree) {
        List<ImageView> toRemove = new ArrayList<>();

        for (Map.Entry<ImageView, WoodcuttingTask> entry : activeWoodcuttingTasks.entrySet()) {
            if (entry.getValue().tree == tree) {
                entry.getValue().isActive = false;
                if (entry.getValue().collectionTimeline != null) {
                    entry.getValue().collectionTimeline.stop();
                }
                toRemove.add(entry.getKey());
            }
        }

        for (ImageView woodcutter : toRemove) {
            activeWoodcuttingTasks.remove(woodcutter);
        }
    }

    private void createTreeStump(double x, double y, double size) {
        // Crear un tocón (opcional, visualmente)
        Circle stump = new Circle(size / 4, Color.rgb(101, 67, 33));
        stump.setCenterX(x + size / 2);
        stump.setCenterY(y + size / 2);

        root.getChildren().add(stump);

        // El tocón puede ser clickeable para eliminarlo más tarde
        stump.setOnMouseClicked(e -> {
            root.getChildren().remove(stump);
            System.out.println("🪵 Tocón removido");
        });
    }

    private void animateMove(ImageView unit, double targetX, double targetY) {
        // IMPORTANTE: Detener cualquier tarea activa antes de mover
        if (isMiner(unit)) {
            stopMiningIfActive(unit);
        } else if (isWoodcutter(unit)) {
            stopWoodcuttingIfActive(unit);
        }

        // Detener cualquier animación de movimiento en curso
        if (unit.getProperties().containsKey("currentAnimation")) {
            TranslateTransition oldAnimation = (TranslateTransition) unit.getProperties().get("currentAnimation");
            if (oldAnimation != null) {
                oldAnimation.stop();
            }
        }

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

        // Guardar referencia a la animación actual
        unit.getProperties().put("currentAnimation", tt);

        tt.setOnFinished(ev -> {
            unit.setX(targetX);
            unit.setY(targetY);
            unit.setTranslateX(0);
            unit.setTranslateY(0);
            unit.getProperties().remove("currentAnimation");

            System.out.println("✅ " + (isMiner(unit) ? "Minero" : "Leñador") +
                    " movido a nueva posición: (" + (int)targetX + ", " + (int)targetY + ")");
        });

        tt.play();
    }

    private void stopWoodcuttingIfActive(ImageView woodcutter) {
        if (activeWoodcuttingTasks.containsKey(woodcutter)) {
            WoodcuttingTask task = activeWoodcuttingTasks.get(woodcutter);
            if (task != null && task.isActive) {
                System.out.println("🪓 CANCELANDO tala para leñador: " + woodcutter.getId());

                task.isActive = false;

                if (task.collectionTimeline != null) {
                    task.collectionTimeline.stop();
                }
                if (task.treeLifeTimeline != null) {
                    task.treeLifeTimeline.stop();
                }

                activeWoodcuttingTasks.remove(woodcutter);

                // Mostrar efecto de cancelación
                showWoodcuttingCancelledEffect(woodcutter);
            }
        }
    }

    private void showWoodcuttingCancelledEffect(ImageView woodcutter) {
        Label cancelLabel = new Label("Tala Cancelada");
        cancelLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #FF6347;");

        double woodcutterX = woodcutter.getX() + woodcutter.getFitWidth() / 2;
        double woodcutterY = woodcutter.getY();

        cancelLabel.setLayoutX(woodcutterX - 40);
        cancelLabel.setLayoutY(woodcutterY - 20);

        root.getChildren().add(cancelLabel);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5), cancelLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(1.5), cancelLabel);
        moveUp.setByY(-30);

        ParallelTransition parallel = new ParallelTransition(fadeOut, moveUp);
        parallel.setOnFinished(e -> root.getChildren().remove(cancelLabel));
        parallel.play();
    }



    private void moveWoodcutterAfterFinishing(ImageView woodcutter, ImageView tree) {
        // Mover el leñador a una posición aleatoria cerca del árbol
        double treeX = tree.getX() + tree.getFitWidth() / 2;
        double treeY = tree.getY() + tree.getFitHeight() / 2;

        double angle = Math.random() * 2 * Math.PI;
        double distance = 100 + Math.random() * 100;

        double targetX = treeX + Math.cos(angle) * distance;
        double targetY = treeY + Math.sin(angle) * distance;

        // Asegurar que esté dentro de los límites
        targetX = Math.max(0, Math.min(targetX, windowWidth - 50));
        targetY = Math.max(0, Math.min(targetY, windowHeight - 50));

        animateMove(woodcutter, targetX, targetY);
    }

    private void stopMiningIfActive(ImageView miner) {
        if (activeMiningTasks.containsKey(miner)) {
            MiningTask task = activeMiningTasks.get(miner);
            if (task != null && task.isActive) {
                System.out.println("⛏ CANCELANDO minería para minero: " + miner.getId());

                task.isActive = false;

                // Detener timelines
                if (task.collectionTimeline != null) {
                    task.collectionTimeline.stop();
                    System.out.println("   Timeline de colección detenida");
                }
                if (task.mineLifeTimeline != null) {
                    task.mineLifeTimeline.stop();
                    System.out.println("   Timeline de vida de mina detenida");
                }

                // Eliminar de la lista
                activeMiningTasks.remove(miner);

                // Mostrar mensaje de cancelación
                showMiningCancelledEffect(miner);
            }
        }
    }

    private void showMiningCancelledEffect(ImageView miner) {
        // Crear texto de cancelación
        Label cancelLabel = new Label("Minería Cancelada");
        cancelLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #FF6347;");

        double minerX = miner.getX() + miner.getFitWidth() / 2;
        double minerY = miner.getY();

        cancelLabel.setLayoutX(minerX - 40);
        cancelLabel.setLayoutY(minerY - 20);

        root.getChildren().add(cancelLabel);

        // Animación
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.5), cancelLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        TranslateTransition moveUp = new TranslateTransition(Duration.seconds(1.5), cancelLabel);
        moveUp.setByY(-30);

        ParallelTransition parallel = new ParallelTransition(fadeOut, moveUp);
        parallel.setOnFinished(e -> root.getChildren().remove(cancelLabel));
        parallel.play();

        // Efecto de partículas rojas
        createCancellationParticles(miner);
    }

    private void createCancellationParticles(ImageView miner) {
        double minerX = miner.getX() + miner.getFitWidth() / 2;
        double minerY = miner.getY() + miner.getFitHeight() / 2;

        for (int i = 0; i < 5; i++) {
            Circle particle = new Circle(2, Color.rgb(255, 99, 71, 0.8)); // Rojo tomate
            particle.setCenterX(minerX);
            particle.setCenterY(minerY);

            root.getChildren().add(particle);

            // Animación aleatoria
            double angle = Math.random() * 2 * Math.PI;
            double distance = 15 + Math.random() * 25;

            TranslateTransition move = new TranslateTransition(Duration.seconds(1), particle);
            move.setByX(Math.cos(angle) * distance);
            move.setByY(Math.sin(angle) * distance);

            FadeTransition fade = new FadeTransition(Duration.seconds(1), particle);
            fade.setFromValue(0.8);
            fade.setToValue(0);

            ParallelTransition particleAnim = new ParallelTransition(move, fade);
            particleAnim.setOnFinished(e -> root.getChildren().remove(particle));
            particleAnim.play();
        }
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

    private void addToSelection(ImageView unit) {
        if (selectedUnitViews.contains(unit)) return;

        selectedUnitViews.add(unit);
        applySelectionStyle(unit, true);
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

    private boolean containsFully(Bounds outer, Bounds inner) {
        return outer.contains(inner.getMinX(), inner.getMinY())
                && outer.contains(inner.getMaxX(), inner.getMaxY());
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

    // Nuevo método para procesar cola de entrenamientos
    private void processUnitTrainingQueue() {
        if (territory1 != null && territory1.getTownHall() != null) {
            // Buscar MilitaryBase en el territorio
            MilitaryBase militaryBase = null;
            for (Object building : territory1.getTownHall().getOwnedBuildings()) {
                if (building instanceof MilitaryBase) {
                    militaryBase = (MilitaryBase) building;
                    break;
                }
            }

            if (militaryBase != null) {
                // Procesar cola de entrenamiento del backend
                militaryBase.processTrainingQueue();

                // Sincronizar caballeros completados
                syncCompletedKnights(militaryBase);
            }
        }
    }

    private void syncCompletedKnights(MilitaryBase militaryBase) {
        if (militaryBase == null) return;

        // Obtener caballeros del backend
        List<Knight> backendKnights = militaryBase.getKnights();
        System.out.println("Hay "+ backendKnights.size() + " caballeros--------------------");

        // Verificar si hay caballeros nuevos en el backend
        for (Knight knight : backendKnights) {
            String unitId = knight.getId();

            // Verificar si ya existe en el frontend
            if (!isKnightInFrontend(unitId) && unitTrainingMap.containsKey(unitId)) {
                // ¡Nuevo caballero completado!
                System.out.println("🎉 ¡Caballero completado en backend! ID: " + unitId);

                // Obtener información del entrenamiento
                UnitTrainingInfo trainingInfo = unitTrainingMap.get(unitId);

                // Crear caballero en el frontend
                if (trainingInfo != null && trainingInfo.barracksView != null) {
                    createCompletedKnight(unitId, trainingInfo.barracksView);
                } else {
                    // Si no tenemos referencia al cuartel, usar uno por defecto
                    ImageView barracks = findNearestBarracks();
                    if (barracks != null) {
                        createCompletedKnight(unitId, barracks);
                    }
                }

                // Limpiar referencia
                unitTrainingMap.remove(unitId);

                // Eliminar indicador de entrenamiento
                removeTrainingIndicator(unitId);
            }
        }
    }

    private boolean isKnightInFrontend(String unitId) {
        for (ImageView knightView : createdKnights) {
            if (knightView.getId() != null && knightView.getId().contains(unitId)) {
                return true;
            }
        }
        return false;
    }

    private void createCompletedKnight(String unitId, ImageView barracksView) {
        try {
            double barracksX = barracksView.getX();
            double barracksY = barracksView.getY();
            double barracksWidth = barracksView.getFitWidth();
            double barracksHeight = barracksView.getFitHeight();
            double knightSize = 50;

            // Buscar posición cerca del cuartel
            Position position = findPositionForKnightCompact(barracksX, barracksY,
                    barracksWidth, barracksHeight, knightSize);

            if (position != null) {
                // Cargar imagen del caballero
                String imagePath = "file:src/main/resources/images/caballero.png";
                Image knightImage = new Image(imagePath);

                ImageView knightView = new ImageView(knightImage);
                knightView.setFitWidth(knightSize);
                knightView.setFitHeight(knightSize);
                knightView.setPreserveRatio(true);
                knightView.setX(position.x);
                knightView.setY(position.y);

                // Usar el ID real del backend
                knightView.setId("knight_" + unitId);

                // Efecto especial para caballero
                DropShadow shadow = new DropShadow();
                shadow.setColor(Color.rgb(184, 134, 11, 0.8));
                shadow.setRadius(10);
                shadow.setSpread(0.2);
                knightView.setEffect(shadow);

                // Animación de aparición
                FadeTransition fade = new FadeTransition(Duration.millis(500), knightView);
                fade.setFromValue(0.0);
                fade.setToValue(1.0);

                ScaleTransition scale = new ScaleTransition(Duration.millis(500), knightView);
                scale.setFromX(0.3);
                scale.setFromY(0.3);
                scale.setToX(1.0);
                scale.setToY(1.0);

                // Efecto de brillo al completarse
                DropShadow glow = new DropShadow();
                glow.setColor(Color.rgb(255, 215, 0, 0.8));
                glow.setRadius(20);

                Timeline glowTimeline = new Timeline(
                        new KeyFrame(Duration.millis(0), e -> knightView.setEffect(glow)),
                        new KeyFrame(Duration.millis(1000), e -> knightView.setEffect(shadow))
                );

                ParallelTransition parallel = new ParallelTransition(fade, scale);
                parallel.setOnFinished(e -> {
                    glowTimeline.play();
                    System.out.println("✨ Caballero creado exitosamente! ID: " + unitId);
                });

                // Añadir a la escena y lista
                root.getChildren().add(knightView);
                createdKnights.add(knightView);

                // Hacer interactivo
                makeKnightInteractive(knightView, "Caballero");

                parallel.play();

            } else {
                System.out.println("⚠️ No se pudo encontrar posición para el caballero completado");
            }

        } catch (Exception e) {
            System.err.println("❌ Error al crear caballero completado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void removeTrainingIndicator(String unitId) {
        // Eliminar vista de entrenamiento
        Node trainingView = root.lookup("#training_" + unitId);
        if (trainingView != null) {
            root.getChildren().remove(trainingView);
        }

        // Eliminar barra de progreso
        Node progressBar = root.lookup("#training_progress_" + unitId);
        if (progressBar != null) {
            root.getChildren().remove(progressBar);
        }

        System.out.println("🧹 Indicadores de entrenamiento eliminados para: " + unitId);
    }

    // Método para convertir tipo de construcción a nombre de imagen
    private String getBuildingTypeForImage(String buildingType) {
        if (buildingType == null) return "casa";

        String typeUpper = buildingType.toUpperCase();

        if (typeUpper.contains("MILITARY_BASE") || typeUpper.contains("BARRACKS") ||
                typeUpper.contains("CUARTEL")) {
            return "Cuartel";
        } else if (typeUpper.contains("HOUSE") || typeUpper.contains("CASA")) {
            return "casa";
        }
        return "casa"; // Por defecto
    }

    private void syncConstructionsWithBackend() {
        if (territory1 == null || territory1.getTownHall() == null) return;

        // Obtener construcciones en progreso del backend
        Deque<ConstructionOrder> constructionQueue = territory1.getTownHall().getConstructionQueue();

        System.out.println("🔄 Sincronizando construcciones...");
        System.out.println("📋 Construcciones en cola backend: " + constructionQueue.size());

        // PASO 1: BUSCAR Y COMPLETAR CONSTRUCCIONES TERMINADAS INMEDIATAMENTE
        for (ConstructionOrder order : constructionQueue) {
            if (order.getRemainingTime() <= 0) {
                String buildingId = order.getBuildingId();
                String buildingType = order.getType().toString();
                String displayBuildingType = getBuildingTypeForImage(buildingType);

                System.out.println("🔥 ¡TIEMPO TERMINADO! Completando ahora mismo: " + buildingId);

                // FORZAR completar la construcción
                forceCompleteConstruction(buildingId, displayBuildingType);

                // Salir después de completar la primera (solo una puede estar terminada)
                break;
            }
        }

        // PASO 2: Actualizar construcción activa (si hay)
        if (!constructionQueue.isEmpty()) {
            ConstructionOrder activeConstruction = constructionQueue.peekFirst();

            // Solo si la construcción activa aún tiene tiempo
            if (activeConstruction != null && activeConstruction.getRemainingTime() > 0) {
                System.out.println("🎯 Construcción activa: " + activeConstruction.getType() +
                        " ID: " + activeConstruction.getBuildingId() +
                        " Tiempo restante: " + activeConstruction.getRemainingTime());

                updateActiveConstruction(activeConstruction);
            }
        }

        // PASO 3: Verificar construcciones huérfanas (terminadas pero aún en visualización)
        checkOrphanedConstructions();
    }

    private void forceCompleteConstruction(String buildingId, String buildingType) {
        System.out.println("⚡⚡⚡ FORZANDO COMPLETACIÓN DE CONSTRUCCIÓN ⚡⚡⚡");
        System.out.println("ID: " + buildingId);
        System.out.println("Tipo: " + buildingType);

        // Opción 1: Usar completeConstructionNow si existe la vista
        ImageView constructionView = constructionVisuals.get(buildingId);
        if (constructionView != null) {
            System.out.println("✅ Usando vista existente");
            completeConstructionNow(buildingId, buildingType);
            return;
        }

        // Opción 2: Crear desde cero en posición por defecto
        System.out.println("⚠️ No hay vista, creando desde cero");

        // Buscar posición guardada
        double x, y;
        if (buildingPositions.containsKey(buildingId)) {
            Position pos = buildingPositions.get(buildingId);
            x = pos.x;
            y = pos.y;
            System.out.println("📍 Usando posición guardada: " + x + ", " + y);
        } else {
            // Posición por defecto cerca del TownHall
            x = windowWidth * 0.5;
            y = windowHeight * 0.5;
            System.out.println("📍 Usando posición por defecto: " + x + ", " + y);
        }

        // Tamaño según el tipo
        double width = getBuildingWidth(buildingType);
        double height = getBuildingHeight(buildingType);

        // Crear la casa INMEDIATAMENTE sin animaciones
        createHouseNow(x, y, width, height);

        // Limpiar referencias
        constructionVisuals.remove(buildingId);
        buildingTypesUnderConstruction.remove(buildingId);
        buildingPositions.remove(buildingId);
        barraProgresoManager.eliminarBarraProgreso(buildingId);

        System.out.println("✅ ¡CASA CREADA POR FUERZA!");
    }

    private void createHouseNow(double x, double y, double width, double height) {
        try {
            System.out.println("🏠 CREANDO CASA AHORA en " + x + ", " + y);

            // Intenta cargar la imagen de la casa
            Image houseImage = new Image("file:src/main/resources/images/casa.png");
            ImageView houseView = new ImageView(houseImage);

            houseView.setFitWidth(width);
            houseView.setFitHeight(height);
            houseView.setPreserveRatio(true);
            houseView.setX(x);
            houseView.setY(y);

            // ID único
            houseView.setId("Casa_" + System.currentTimeMillis());

            // Efecto de sombra simple
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.5));
            shadow.setRadius(10);
            houseView.setEffect(shadow);

            // Añadir a la escena INMEDIATAMENTE
            root.getChildren().add(houseView);

            // Añadir a la lista de edificios
            placedBuildings.add(houseView);

            // Hacerla interactiva
            makeBuildingInteractive(houseView, "Casa");

            System.out.println("✅ ¡CASA CREADA EXITOSAMENTE!");

        } catch (Exception e) {
            System.err.println("❌ Error creando casa: " + e.getMessage());

            // Crear un placeholder SI FALLA
            Rectangle placeholder = new Rectangle(width, height, Color.BROWN);
            placeholder.setX(x);
            placeholder.setY(y);
            placeholder.setStroke(Color.YELLOW);
            placeholder.setStrokeWidth(2);

            Label label = new Label("CASA");
            label.setLayoutX(x + width/2 - 20);
            label.setLayoutY(y + height/2 - 10);
            label.setTextFill(Color.WHITE);

            Pane housePane = new Pane(placeholder, label);
            root.getChildren().add(housePane);

            System.out.println("⚠️ Creado placeholder para casa");
        }
    }

    private void checkOrphanedConstructions() {
        // Verificar si hay construcciones visuales que ya no están en el backend
        if (territory1 == null || territory1.getTownHall() == null) return;

        Deque<ConstructionOrder> constructionQueue = territory1.getTownHall().getConstructionQueue();

        List<String> toRemove = new ArrayList<>();

        for (String buildingId : constructionVisuals.keySet()) {
            boolean foundInBackend = false;

            for (ConstructionOrder order : constructionQueue) {
                if (order.getBuildingId().equals(buildingId)) {
                    foundInBackend = true;
                    break;
                }
            }

            // Si no está en el backend pero SÍ está en visualización, FORZAR completar
            if (!foundInBackend) {
                System.out.println("👻 Construcción huérfana encontrada: " + buildingId);
                String buildingType = buildingTypesUnderConstruction.get(buildingId);

                if (buildingType != null) {
                    System.out.println("⚡ Forzando completar construcción huérfana: " + buildingType);
                    forceCompleteConstruction(buildingId, buildingType);
                } else {
                    toRemove.add(buildingId);
                }
            }
        }

        // Limpiar
        for (String buildingId : toRemove) {
            ImageView view = constructionVisuals.remove(buildingId);
            if (view != null) root.getChildren().remove(view);
            buildingTypesUnderConstruction.remove(buildingId);
            buildingPositions.remove(buildingId);
        }
    }

    private void updateWaitingConstructions(Deque<ConstructionOrder> constructionQueue,
                                            ConstructionOrder activeConstruction) {
        if (activeConstruction == null) return;

        String activeId = activeConstruction.getBuildingId();

        // Procesar todas las construcciones en cola excepto la activa
        for (ConstructionOrder order : constructionQueue) {
            String buildingId = order.getBuildingId();

            if (!buildingId.equals(activeId)) { // Solo las que están en espera
                // Si ya existe una visualización, asegurarse de que NO tenga barra de progreso
                if (constructionVisuals.containsKey(buildingId)) {
                    // Eliminar cualquier barra de progreso existente
                    barraProgresoManager.eliminarBarraProgreso(buildingId);

                    // Mostrar como "en espera" (menos opaco)
                    ImageView constructionView = constructionVisuals.get(buildingId);
                    if (constructionView != null) {
                        constructionView.setOpacity(0.3); // Muy transparente para indicar espera

                        // Añadir texto "En espera" si no existe
                        addWaitingText(constructionView, buildingId);
                    }
                } else {
                    // Si no existe visualización, crear una indicando que está en espera
                    String displayBuildingType = getBuildingTypeForImage(order.getType().toString());
                    double x = getConstructionPositionX(buildingId);
                    double y = getConstructionPositionY(buildingId);
                    double width = getBuildingWidth(displayBuildingType);
                    double height = getBuildingHeight(displayBuildingType);

                    showWaitingConstruction(x, y, width, height, displayBuildingType, buildingId);
                }
            }
        }
    }

    private void checkAndFixActiveConstructions(Deque<ConstructionOrder> constructionQueue) {
        if (constructionQueue.isEmpty()) return;

        ConstructionOrder activeOrder = constructionQueue.peekFirst();
        if (activeOrder == null) return;

        String activeId = activeOrder.getBuildingId();

        // Verificar si la construcción activa tiene barra de progreso
        if (constructionVisuals.containsKey(activeId)) {
            ImageView constructionView = constructionVisuals.get(activeId);

            // Verificar si tiene barra de progreso
            double progresoActual = barraProgresoManager.obtenerProgresoActual(activeId);
            boolean tieneBarra = progresoActual > 0;

            // Si no tiene barra de progreso pero debería tenerla
            if (!tieneBarra) {
                System.out.println("🔧 Reparando construcción activa sin barra: " + activeId);

                // Crear barra de progreso
                String buildingType = buildingTypesUnderConstruction.get(activeId);
                if (buildingType != null) {
                    int totalTime = getTotalBuildTimeForType(buildingType);
                    int remainingTime = activeOrder.getRemainingTime();
                    int tiempoTranscurrido = totalTime - remainingTime;

                    barraProgresoManager.crearBarraProgresoAnimada(
                            activeId, constructionView, totalTime
                    );

                    if (tiempoTranscurrido > 0) {
                        barraProgresoManager.iniciarAnimacionDesde(activeId, tiempoTranscurrido);
                    } else {
                        barraProgresoManager.iniciarAnimacion(activeId);
                    }

                    constructionView.setOpacity(0.7);
                    removeWaitingText(activeId);
                }
            }
        }
    }

    private void showWaitingConstruction(double x, double y, double width, double height,
                                         String buildingType, String constructionId) {
        try {
            Image constructionImage = new Image("file:src/main/resources/images/Construccion.png");
            ImageView constructionView = new ImageView(constructionImage);

            constructionView.setFitWidth(width);
            constructionView.setFitHeight(height);
            constructionView.setPreserveRatio(true);
            constructionView.setX(x);
            constructionView.setY(y);
            constructionView.setOpacity(0.3); // Muy transparente para indicar espera
            constructionView.setId("waiting_" + constructionId);

            // NO crear barra de progreso
            // Solo mostrar un indicador visual de "en espera"
            addWaitingText(constructionView, constructionId);

            // Guardar referencia
            constructionVisuals.put(constructionId, constructionView);
            buildingTypesUnderConstruction.put(constructionId, buildingType);

            // Añadir a la escena
            root.getChildren().add(constructionView);

            System.out.println("⏳ Mostrando construcción EN ESPERA: " + buildingType +
                    " - ID: " + constructionId);

        } catch (Exception e) {
            System.err.println("❌ Error al mostrar construcción en espera: " + e.getMessage());
        }
    }

    private void addWaitingText(ImageView constructionView, String constructionId) {
        // Buscar si ya existe un texto de espera
        for (Node node : root.getChildren()) {
            if (node instanceof Label label && label.getId() != null &&
                    label.getId().equals("waiting_label_" + constructionId)) {
                return; // Ya existe
            }
        }

        // Crear texto "En espera"
        Label waitingLabel = new Label("EN ESPERA");
        waitingLabel.setId("waiting_label_" + constructionId);
        waitingLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #ff9900;");
        waitingLabel.setLayoutX(constructionView.getX() + constructionView.getFitWidth()/2 - 25);
        waitingLabel.setLayoutY(constructionView.getY() + constructionView.getFitHeight() + 5);

        root.getChildren().add(waitingLabel);
    }

    private void updateActiveConstruction(ConstructionOrder activeOrder) {
        String buildingId = activeOrder.getBuildingId();
        String backendBuildingType = activeOrder.getType().toString();
        String displayBuildingType = getBuildingTypeForImage(backendBuildingType);
        int remainingTime = activeOrder.getRemainingTime();
        int totalTime = getTotalBuildTimeForType(displayBuildingType);

        // VERIFICAR SI YA SE TERMINÓ
        if (remainingTime <= 0) {
            System.out.println("⏰ ¡CONSTRUCCIÓN TERMINADA EN updateActiveConstruction!");
            forceCompleteConstruction(buildingId, displayBuildingType);
            return; // NO CONTINUAR
        }

        System.out.println("🎯 Actualizando construcción ACTIVA: " + displayBuildingType +
                " ID: " + buildingId +
                " Tiempo restante: " + remainingTime + "s/" + totalTime + "s");

        // Si no tenemos una visualización para esta construcción, crearla
        if (!constructionVisuals.containsKey(buildingId)) {
            // Obtener posición
            double x = getConstructionPositionX(buildingId);
            double y = getConstructionPositionY(buildingId);
            double width = getBuildingWidth(displayBuildingType);
            double height = getBuildingHeight(displayBuildingType);

            // Mostrar construcción en progreso
            showConstructionInProgress(x, y, width, height, displayBuildingType, buildingId, totalTime);
        } else {
            // Ya existe visualización, actualizarla
            ImageView constructionView = constructionVisuals.get(buildingId);

            // Cambiar de "en espera" a "activa"
            constructionView.setOpacity(0.7); // Más opaco para construcción activa
            removeWaitingText(buildingId);

            // Verificar si ya tiene barra de progreso
            double progresoActual = barraProgresoManager.obtenerProgresoActual(buildingId);
            boolean tieneBarra = progresoActual > 0 ||
                    barraProgresoManager.obtenerProgresoActual(buildingId) >= 0; // Si es >= 0, existe

            // Crear barra de progreso si no existe o si el progreso es 0
            if (!tieneBarra || progresoActual <= 0) {
                barraProgresoManager.crearBarraProgresoAnimada(
                        buildingId, constructionView, totalTime
                );

                // Iniciar animación desde el progreso actual
                int tiempoTranscurrido = totalTime - remainingTime;
                if (tiempoTranscurrido > 0) {
                    barraProgresoManager.iniciarAnimacionDesde(buildingId, tiempoTranscurrido);
                } else {
                    barraProgresoManager.iniciarAnimacion(buildingId);
                }
            }
        }

        // Actualizar progreso
        ImageView constructionView = constructionVisuals.get(buildingId);
        if (constructionView != null) {
            double progress = calculateConstructionProgress(activeOrder, totalTime);
            updateConstructionProgress(constructionView, progress);

            // Actualizar la barra de progreso animada
            barraProgresoManager.actualizarProgreso(buildingId, progress, totalTime - remainingTime);

            // SI EL TIEMPO ES 0 O MENOS, COMPLETAR INMEDIATAMENTE
            if (remainingTime <= 0) {
                System.out.println("⏰ ¡Tiempo completado! Reemplazando construcción: " + buildingId);
                completeConstructionNow(buildingId, displayBuildingType);
            }
        }
    }


    private boolean hasConstructionTimeElapsed(String buildingId) {
        // Aquí necesitas llevar registro del tiempo de inicio
        // Por simplicidad, asumamos 50 segundos para Cuartel, 30 para Casa
        String buildingType = buildingTypesUnderConstruction.get(buildingId);
        if (buildingType == null) return false;

        int expectedTime = getTotalBuildTimeForType(buildingType);

        // Llevar registro del tiempo de inicio (en lugar de esto, usa un mapa)
        Map<String, Long> startTimes = new HashMap<>();

        if (!startTimes.containsKey(buildingId)) {
            startTimes.put(buildingId, System.currentTimeMillis());
            return false;
        }

        long startTime = startTimes.get(buildingId);
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;

        return elapsedSeconds >= expectedTime;
    }

    // Método para obtener tiempo total de construcción según el tipo
    private int getTotalBuildTimeForType(String buildingType) {
        if (buildingType == null) return 30; // Tiempo por defecto

        String typeLower = buildingType.toLowerCase();

        if (typeLower.contains("cuartel") || typeLower.contains("military") ||
                typeLower.contains("barracks")) {
            return 50; // 50 segundos para Cuartel
        } else if (typeLower.contains("casa") || typeLower.contains("house")) {
            return 30; // 30 segundos para Casa
        } else if (typeLower.contains("mina") || typeLower.contains("mine")) {
            return 20; // 20 segundos para Mina
        }

        return 30; // Tiempo por defecto
    }

    // Método para obtener nombre amigable del tipo de edificio
    private String getFriendlyBuildingName(String buildingType) {
        if (buildingType == null) return "Edificio";

        String typeUpper = buildingType.toUpperCase();

        if (typeUpper.contains("MILITARY_BASE") || typeUpper.contains("BARRACKS") ||
                typeUpper.contains("CUARTEL") || typeUpper.contains("MILITARY")) {
            return "Cuartel";
        } else if (typeUpper.contains("HOUSE") || typeUpper.contains("CASA")) {
            return "Casa";
        } else if (typeUpper.contains("MINE") || typeUpper.contains("MINA")) {
            return "Mina";
        }

        return buildingType;
    }


    private void completeConstructionNow(String buildingId, String buildingType) {
        System.out.println("🏗️ Intentando completar construcción: " + buildingId + " - " + buildingType);

        // Verificar si tenemos la visualización
        ImageView constructionView = constructionVisuals.get(buildingId);

        if (constructionView != null) {
            System.out.println("✅ Encontrada vista de construcción para: " + buildingId);
            System.out.println("📍 Posición actual: (" + constructionView.getX() + ", " + constructionView.getY() + ")");
            System.out.println("📏 Tamaño: " + constructionView.getFitWidth() + "x" + constructionView.getFitHeight());

            // Detener y eliminar la barra de progreso animada
            barraProgresoManager.eliminarBarraProgreso(buildingId);

            // Eliminar texto "EN ESPERA" si existe
            removeWaitingText(buildingId);

            double x = constructionView.getX();
            double y = constructionView.getY();
            double width = constructionView.getFitWidth();
            double height = constructionView.getFitHeight();

            // Crear el edificio final ANTES de remover la construcción
            createFinalBuilding(x, y, width, height, buildingType);

            // Luego remover la visualización de construcción
            root.getChildren().remove(constructionView);

            // Limpiar referencias
            constructionVisuals.remove(buildingId);
            buildingTypesUnderConstruction.remove(buildingId);
            buildingPositions.remove(buildingId);

            // Actualizar recursos si es necesario
            updateResourceDisplay();

            System.out.println("✅ Construcción completada y edificio creado: " + buildingType);

        } else {
            System.out.println("⚠️ No se encontró vista de construcción para: " + buildingId);
            System.out.println("🔄 Buscando construcción en cola backend...");

            // Intentar obtener la construcción desde el backend
            if (territory1 != null && territory1.getTownHall() != null) {
                Deque<ConstructionOrder> queue = territory1.getTownHall().getConstructionQueue();
                for (ConstructionOrder order : queue) {
                    if (order.getBuildingId().equals(buildingId)) {
                        System.out.println("✅ Construcción encontrada en backend: " + order.getType());

                        // Obtener posición guardada o usar una por defecto
                        double x, y;
                        if (buildingPositions.containsKey(buildingId)) {
                            Position pos = buildingPositions.get(buildingId);
                            x = pos.x;
                            y = pos.y;
                        } else {
                            x = windowWidth * 0.6;
                            y = windowHeight * 0.4;
                        }

                        // Crear el edificio final
                        createFinalBuilding(x, y, getBuildingWidth(buildingType),
                                getBuildingHeight(buildingType), buildingType);

                        // Limpiar referencias
                        constructionVisuals.remove(buildingId);
                        buildingTypesUnderConstruction.remove(buildingId);
                        buildingPositions.remove(buildingId);

                        System.out.println("✅ Edificio creado desde backend: " + buildingType);
                        break;
                    }
                }
            }
        }
    }

    // Método para verificar construcciones huérfanas (completadas pero sin visualización)
    private void checkForOrphanedConstructions() {
        if (territory1 == null || territory1.getTownHall() == null) return;

        // Obtener construcciones del backend
        List<String> ownedBuildings = new ArrayList<>();
        // Aquí necesitarías un método para obtener edificios ya construidos
        // Por ahora, solo revisa las construcciones en progreso

        Deque<ConstructionOrder> constructionQueue = territory1.getTownHall().getConstructionQueue();

        // Buscar construcciones que tienen visual pero no están en la cola
        List<String> toRemove = new ArrayList<>();
        for (String buildingId : constructionVisuals.keySet()) {
            boolean foundInQueue = false;
            for (ConstructionOrder order : constructionQueue) {
                if (order.getBuildingId().equals(buildingId)) {
                    foundInQueue = true;
                    break;
                }
            }

            if (!foundInQueue) {
                System.out.println("⚠️ Construcción huérfana encontrada: " + buildingId);
                // Intentar completarla
                String buildingType = buildingTypesUnderConstruction.get(buildingId);
                if (buildingType != null) {
                    System.out.println("🔄 Completando construcción huérfana: " + buildingType);
                    completeConstructionNow(buildingId, buildingType);
                } else {
                    toRemove.add(buildingId);
                }
            }
        }

        // Limpiar referencias
        for (String buildingId : toRemove) {
            ImageView view = constructionVisuals.remove(buildingId);
            if (view != null) {
                root.getChildren().remove(view);
            }
            buildingTypesUnderConstruction.remove(buildingId);
            buildingPositions.remove(buildingId);
        }
    }

    private void eliminarBarraProgreso(String buildingId, ImageView constructionView) {
        // Buscar y eliminar la barra de progreso asociada
        Node barraProgreso = null;

        for (Node node : root.getChildren()) {
            if (node instanceof Pane progressPane) {
                if (progressPane.getId() != null &&
                        progressPane.getId().equals("progress_" + constructionView.hashCode())) {
                    barraProgreso = node;
                    break;
                }
            }
        }

        if (barraProgreso != null) {
            root.getChildren().remove(barraProgreso);
            System.out.println("🗑️ Barra de progreso eliminada para: " + buildingId);
        }
    }
    private double calculateConstructionProgress(ConstructionOrder order, int totalTime) {
        if (order == null || totalTime <= 0) return 0.0;

        try {
            int remainingTime = order.getRemainingTime();
            int elapsedTime = totalTime - remainingTime;
            double progress = Math.min(1.0, Math.max(0.0, (double) elapsedTime / totalTime));

            return progress;
        } catch (Exception e) {
            System.err.println("❌ Error calculando progreso: " + e.getMessage());
            return 0.0;
        }
    }

    // Método alternativo si no se puede obtener el tiempo total
    private double calculateFallbackProgress(ConstructionOrder order) {
        try {
            String buildingType = order.getType().toString();
            int totalTime = getTotalBuildTime(buildingType);
            int remainingTime = order.getRemainingTime();

            if (totalTime <= 0) return 0.0;

            int elapsedTime = totalTime - remainingTime;
            return Math.min(1.0, Math.max(0.0, (double) elapsedTime / totalTime));
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    // Método auxiliar para obtener tiempo restante de la orden
    private int getRemainingTimeFromOrder(String order) {
        try {
            if (order.getClass().getMethod("getRemainingTime") != null) {
                return (int) order.getClass().getMethod("getRemainingTime").invoke(order);
            } else if (order.getClass().getMethod("getTimeLeft") != null) {
                return (int) order.getClass().getMethod("getTimeLeft").invoke(order);
            }
        } catch (Exception e) {
            System.err.println("❌ Error al obtener tiempo restante: " + e.getMessage());
        }
        return 0;
    }

    // Método para limpiar construcciones huérfanas
    private void cleanupOrphanedConstructions(Deque<ConstructionOrder> constructionQueue) {
        List<String> toRemove = new ArrayList<>();

        for (String constructionId : constructionVisuals.keySet()) {
            boolean existsInBackend = false;
            for (ConstructionOrder order : constructionQueue) {
                if (order.getBuildingId().equals(constructionId)) {
                    existsInBackend = true;
                    break;
                }
            }

            if (!existsInBackend) {
                toRemove.add(constructionId);
            }
        }

        for (String constructionId : toRemove) {
            ImageView constructionView = constructionVisuals.remove(constructionId);
            if (constructionView != null) {
                root.getChildren().remove(constructionView);
            }
            buildingTypesUnderConstruction.remove(constructionId);
            System.out.println("🧹 Eliminada construcción huérfana: " + constructionId);
        }
    }

    // Método para obtener la posición X de la construcción
    private double getConstructionPositionX(String buildingId) {
        // Usar el campo de clase buildingPositions
        if (buildingPositions.containsKey(buildingId)) {
            return buildingPositions.get(buildingId).x;
        }

        // Si no se encuentra, buscar en las construcciones visuales
        ImageView constructionView = constructionVisuals.get(buildingId);
        if (constructionView != null) {
            return constructionView.getX();
        }

        // Valor por defecto
        return windowWidth * 0.5;
    }

    // Método para obtener la posición Y de la construcción
    private double getConstructionPositionY(String buildingId) {
        // Usar el campo de clase buildingPositions
        if (buildingPositions.containsKey(buildingId)) {
            return buildingPositions.get(buildingId).y;
        }

        // Si no se encuentra, buscar en las construcciones visuales
        ImageView constructionView = constructionVisuals.get(buildingId);
        if (constructionView != null) {
            return constructionView.getY();
        }

        // Valor por defecto
        return windowHeight * 0.5;
    }

    // Método para obtener el ancho del edificio según su tipo
    private double getBuildingWidth(String buildingType) {
        if (buildingType == null) return 100;

        String typeLower = buildingType.toLowerCase();

        if (typeLower.contains("cuartel") || typeLower.contains("military_base") ||
                typeLower.contains("military")) {
            return 170;
        }
        return 100; // Casa u otros edificios
    }

    // Método para obtener la altura del edificio según su tipo
    private double getBuildingHeight(String buildingType) {
        if (buildingType == null) return 100;

        String typeLower = buildingType.toLowerCase();

        if (typeLower.contains("cuartel") || typeLower.contains("military_base") ||
                typeLower.contains("military")) {
            return 170;
        }
        return 100; // Casa u otros edificios
    }



    // Método auxiliar para obtener tiempo total de construcción (debe coincidir con el backend)
    private int getTotalBuildTime(String buildingType) {
        if (buildingType.equalsIgnoreCase("HOUSE")) {
            return 30; // 30 segundos para casa
        } else if (buildingType.equalsIgnoreCase("MILITARY_BASE")) {
            return 50; // 50 segundos para cuartel
        }
        return 30; // Por defecto
    }

    private void updateConstructionProgress(ImageView constructionView, double progress) {
        // Cambiar la opacidad según el progreso (más opaco a medida que avanza)
        double minOpacity = 0.3;
        double maxOpacity = 0.7;
        double currentOpacity = minOpacity + (progress * (maxOpacity - minOpacity));
        constructionView.setOpacity(currentOpacity);

    }

    // Método para añadir edificio completado a la lista del backend
    private void addCompletedBuildingToBackendList(String buildingType) {
        if (territory1 == null || territory1.getTownHall() == null) return;

        System.out.println("🏗️ Añadiendo edificio completado al backend: " + buildingType);

        // Aquí tu backend ya debería haber añadido el edificio a la lista de ownedBuildings
        // Solo necesitamos verificar
        int buildingCount = territory1.getTownHall().getOwnedBuildings().size();
        System.out.println("📊 Total de edificios en backend: " + buildingCount);
    }

    private void checkCompletedConstructions() {
        if (territory1 == null || territory1.getTownHall() == null) return;

        // Obtener construcciones en progreso del backend
        Deque<ConstructionOrder> constructionQueue = territory1.getTownHall().getConstructionQueue();

        System.out.println("✅ Verificando construcciones completadas...");
        System.out.println("📋 Construcciones en cola: " + constructionQueue.size());

        List<ConstructionOrder> ordenesCompletadas = new ArrayList<>();

        // Identificar órdenes completadas
        for (ConstructionOrder order : constructionQueue) {
            String buildingId = order.getBuildingId();
            int tiempoRestante = order.getRemainingTime();

            // Si el tiempo restante es 0 o menos, está completado
            if (tiempoRestante <= 0) {
                System.out.println("🎯 Construcción completada: " + buildingId);

                // Completar visualmente
                ImageView constructionView = constructionVisuals.get(buildingId);
                if (constructionView != null) {
                    String tipoEdificio = buildingTypesUnderConstruction.get(buildingId);
                    if (tipoEdificio != null) {
                        // Completar visualmente
                        completeConstructionNow(buildingId, tipoEdificio);
                    }
                }

                // Añadir a la lista de completadas
                ordenesCompletadas.add(order);
            }
        }

        // IMPORTANTE: Si hay órdenes completadas, sincronizar solo una vez
        if (!ordenesCompletadas.isEmpty()) {
            System.out.println("🏗️ " + ordenesCompletadas.size() + " construcción(es) completada(s)");

            // NO llamar a syncConstructionsWithBackend() aquí - ya estamos en un ciclo
            // En su lugar, actualizar recursos
            updateResourceDisplay();
        }
    }

    // Modifica el método isConstructionCompleteInBackend para usar la lógica real
    private boolean isConstructionCompleteInBackend(String buildingType) {
        // Este método ya no se usa directamente
        // La lógica está en syncConstructionsWithBackend
        return false;
    }

    private void showConstructionInProgress(double x, double y, double width, double height,
                                            String buildingType, String constructionId, int totalTime) {
        try {
            Image constructionImage = new Image("file:src/main/resources/images/Construccion.png");
            ImageView constructionView = new ImageView(constructionImage);

            constructionView.setFitWidth(width);
            constructionView.setFitHeight(height);
            constructionView.setPreserveRatio(true);
            constructionView.setX(x);
            constructionView.setY(y);
            constructionView.setOpacity(0.7); // Más opaco para construcción activa
            constructionView.setId("construction_" + constructionId);

            // Eliminar cualquier texto "En espera"
            removeWaitingText(constructionId);

            // Crear barra de progreso ANIMADA solo si es construcción activa
            barraProgresoManager.crearBarraProgresoAnimada(
                    constructionId, constructionView, totalTime
            );

            // Iniciar animación desde el progreso actual
            int tiempoTranscurrido = totalTime - getRemainingTimeFromOrder(constructionId);
            if (tiempoTranscurrido > 0) {
                barraProgresoManager.iniciarAnimacionDesde(constructionId, tiempoTranscurrido);
            } else {
                barraProgresoManager.iniciarAnimacion(constructionId);
            }

            // Guardar referencia
            constructionVisuals.put(constructionId, constructionView);
            buildingTypesUnderConstruction.put(constructionId, buildingType);

            // Añadir a la escena
            root.getChildren().add(constructionView);

            System.out.println("🚧 Mostrando construcción ACTIVA: " + buildingType +
                    " - Tiempo: " + totalTime + "s - ID: " + constructionId);

        } catch (Exception e) {
            System.err.println("❌ Error al mostrar construcción en progreso: " + e.getMessage());
        }
    }

    private void removeWaitingText(String constructionId) {
        Node waitingText = root.lookup("#waiting_label_" + constructionId);
        if (waitingText != null) {
            root.getChildren().remove(waitingText);
        }
    }

    // Método para reemplazar construcción con edificio final
    private void replaceConstructionWithBuilding(ImageView constructionView, String buildingType, String constructionId) {
        try {
            double x = constructionView.getX();
            double y = constructionView.getY();
            double width = constructionView.getFitWidth();
            double height = constructionView.getFitHeight();

            // Detener animación de pulsación
            constructionView.setOpacity(1.0);
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), constructionView);
            fadeOut.setToValue(0);

            fadeOut.setOnFinished(e -> {
                // Remover la construcción
                root.getChildren().remove(constructionView);

                // Crear el edificio final
                createFinalBuilding(x, y, width, height, buildingType);

                System.out.println("✅ Construcción completada: " + buildingType);
            });

            fadeOut.play();

        } catch (Exception e) {
            System.err.println("❌ Error al reemplazar construcción: " + e.getMessage());
        }
    }

    private void createFinalBuilding(double x, double y, double width, double height, String buildingType) {
        try {
            String imagePath = "file:src/main/resources/images/" + buildingType + ".png";
            System.out.println("🖼️ Cargando imagen: " + imagePath);

            Image buildingImage = new Image(imagePath);

            ImageView buildingView = new ImageView(buildingImage);
            buildingView.setFitWidth(width);
            buildingView.setFitHeight(height);
            buildingView.setPreserveRatio(true);
            buildingView.setX(x);
            buildingView.setY(y);
            buildingView.setOpacity(0); // Comienza transparente

            // Marcar como cuartel si es el caso
            if (buildingType.equalsIgnoreCase("Cuartel")) {
                buildingView.setId("Cuartel_" + System.currentTimeMillis());
            }

            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.5));
            shadow.setRadius(10);
            shadow.setSpread(0.1);
            buildingView.setEffect(shadow);

            // Animación de aparición
            FadeTransition fade = new FadeTransition(Duration.millis(800), buildingView);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);

            ScaleTransition scale = new ScaleTransition(Duration.millis(800), buildingView);
            scale.setFromX(0.8);
            scale.setFromY(0.8);
            scale.setToX(1.0);
            scale.setToY(1.0);

            // Efecto de brillo al completarse
            DropShadow glow = new DropShadow();
            glow.setColor(Color.rgb(255, 215, 0, 0.8));
            glow.setRadius(20);

            Timeline glowTimeline = new Timeline(
                    new KeyFrame(Duration.millis(0), e -> buildingView.setEffect(glow)),
                    new KeyFrame(Duration.millis(1000), e -> buildingView.setEffect(shadow))
            );

            javafx.animation.ParallelTransition parallel =
                    new javafx.animation.ParallelTransition(fade, scale);
            parallel.setOnFinished(e -> {
                glowTimeline.play();
                System.out.println("✨ Edificio final creado: " + buildingType);
            });

            root.getChildren().add(buildingView);
            placedBuildings.add(buildingView);
            makeBuildingInteractive(buildingView, buildingType);

            parallel.play();

        } catch (Exception e) {
            System.err.println("❌ Error al crear edificio final: " + e.getMessage());
            e.printStackTrace();
            // Crear placeholder si falla
            createPlaceholderBuilding(x, y, width, height, buildingType);
        }
    }

    private void createPlaceholderBuilding(double x, double y, double width, double height, String buildingType) {
        Rectangle placeholder = new Rectangle(width, height);
        placeholder.setX(x);
        placeholder.setY(y);

        if (buildingType.equalsIgnoreCase("Cuartel")) {
            placeholder.setFill(Color.rgb(139, 0, 0, 0.8)); // Rojo oscuro para cuartel
        } else {
            placeholder.setFill(Color.rgb(139, 69, 19, 0.8)); // Marrón para casa
        }

        placeholder.setStroke(Color.GOLD);
        placeholder.setStrokeWidth(2);

        Label label = new Label(buildingType);
        label.setLayoutX(x + width/2 - 30);
        label.setLayoutY(y + height/2 - 10);
        label.setTextFill(Color.WHITE);
        label.setFont(javafx.scene.text.Font.font("Arial", 12));

        Pane placeholderPane = new Pane(placeholder, label);
        root.getChildren().add(placeholderPane);
        System.out.println("⚠️ Placeholder creado para: " + buildingType);
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

        // Depurar estado antes de pausar
        System.out.println("\n⏸️ INICIANDO PAUSA - ESTADO ACTUAL:");
        debugSelectionState();

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
     * Método para depurar el estado de selección actual
     */
    private void debugSelectionState() {
        System.out.println("\n=== DEBUG ESTADO DE SELECCIÓN ===");
        System.out.println("Unidades seleccionadas: " + selectedUnitViews.size());

        for (int i = 0; i < selectedUnitViews.size(); i++) {
            ImageView unit = selectedUnitViews.get(i);
            System.out.println("  " + (i+1) + ". ID: " + unit.getId() +
                    " | Tipo: " + unit.getUserData() +
                    " | MouseTransparent: " + unit.isMouseTransparent());
        }
        System.out.println("================================\n");
    }

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

                // Reanudar construcciones
                if (constructionUpdateTimeline != null) {
                    constructionUpdateTimeline.play();
                }

                // REANUDAR TAREAS DE TALA
                for (WoodcuttingTask task : activeWoodcuttingTasks.values()) {
                    if (task.isActive) {
                        if (task.collectionTimeline != null &&
                                task.collectionTimeline.getStatus() == Animation.Status.PAUSED) {
                            task.collectionTimeline.play();
                        }
                        if (task.treeLifeTimeline != null &&
                                task.treeLifeTimeline.getStatus() == Animation.Status.PAUSED) {
                            task.treeLifeTimeline.play();
                        }
                    }
                }

                // REANUDAR TAREAS DE MINERÍA (NUEVO)
                for (MiningTask task : activeMiningTasks.values()) {
                    if (task.isActive) {
                        if (task.collectionTimeline != null &&
                                task.collectionTimeline.getStatus() == Animation.Status.PAUSED) {
                            task.collectionTimeline.play();
                        }
                        if (task.mineLifeTimeline != null &&
                                task.mineLifeTimeline.getStatus() == Animation.Status.PAUSED) {
                            task.mineLifeTimeline.play();
                        }
                    }
                }

                System.out.println("▶ Juego reanudado");
            });

            ParallelTransition parallel = new ParallelTransition(menuFade, menuScale, overlayFade);
            parallel.play();
        } else {
            // Si no hay menú, simplemente remover
            root.getChildren().remove(pauseOverlay);
            pauseOverlay = null;
            isGamePaused = false;
            disableGameInteractions(false);

            // Reanudar construcciones
            if (constructionUpdateTimeline != null) {
                constructionUpdateTimeline.play();
            }

            // Reanudar tareas de tala
            for (WoodcuttingTask task : activeWoodcuttingTasks.values()) {
                if (task.isActive) {
                    if (task.collectionTimeline != null &&
                            task.collectionTimeline.getStatus() == Animation.Status.PAUSED) {
                        task.collectionTimeline.play();
                    }
                    if (task.treeLifeTimeline != null &&
                            task.treeLifeTimeline.getStatus() == Animation.Status.PAUSED) {
                        task.treeLifeTimeline.play();
                    }
                }
            }

            // Reanudar tareas de minería
            for (MiningTask task : activeMiningTasks.values()) {
                if (task.isActive) {
                    if (task.collectionTimeline != null &&
                            task.collectionTimeline.getStatus() == Animation.Status.PAUSED) {
                        task.collectionTimeline.play();
                    }
                    if (task.mineLifeTimeline != null &&
                            task.mineLifeTimeline.getStatus() == Animation.Status.PAUSED) {
                        task.mineLifeTimeline.play();
                    }
                }
            }

            System.out.println("▶ Juego reanudado");
        }
    }

    /**
     * Habilita/deshabilita la interacción con elementos del juego
     */
    private void disableGameInteractions(boolean disable) {
        if (disable) {
            // PAUSAR TODAS LAS TAREAS DE TALA
            for (WoodcuttingTask task : activeWoodcuttingTasks.values()) {
                if (task.collectionTimeline != null) {
                    task.collectionTimeline.pause();
                }
                if (task.treeLifeTimeline != null) {
                    task.treeLifeTimeline.pause();
                }
            }

            // PAUSAR TODAS LAS TAREAS DE MINERÍA
            for (MiningTask task : activeMiningTasks.values()) {
                if (task.collectionTimeline != null) {
                    task.collectionTimeline.pause();
                }
                if (task.mineLifeTimeline != null) {
                    task.mineLifeTimeline.pause();
                }
            }

            // GUARDAR los handlers actuales ANTES de deshabilitarlos
            Scene scene = root.getScene();
            if (scene != null) {
                savedMousePressed = scene.getOnMousePressed();
                savedMouseDragged = scene.getOnMouseDragged();
                savedMouseReleased = scene.getOnMouseReleased();
            }

            // IMPORTANTE: Deshabilitar solo elementos específicos, NO TODOS
            // NO deshabilitar las unidades seleccionadas
            for (int i = 0; i < root.getChildren().size(); i++) {
                Node node = root.getChildren().get(i);

                // NO deshabilitar estos elementos:
                // - Overlay de pausa (debe ser interactivo)
                // - Fantasma de construcción
                // - Unidades seleccionadas (para mantener la selección)
                // - Rectángulo de selección

                boolean shouldDisable = true;

                if (node == pauseOverlay) {
                    shouldDisable = false; // El overlay debe ser interactivo
                } else if (node == buildingGhost) {
                    shouldDisable = false;
                } else if (node == selectionRect) {
                    shouldDisable = false;
                } else if (node instanceof ImageView imageView) {
                    // Verificar si es una unidad seleccionada
                    if (selectedUnitViews.contains(imageView)) {
                        shouldDisable = false; // Mantener las unidades seleccionadas interactivas
                    }
                }

                if (shouldDisable) {
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
            if (scene != null) {
                scene.setOnMouseMoved(null);
                scene.setOnMouseClicked(null);
                scene.setOnMousePressed(null);
                scene.setOnMouseDragged(null);
                scene.setOnMouseReleased(null);
                root.setCursor(javafx.scene.Cursor.DEFAULT);
            }

            // Asegurar que el overlay esté al frente
            if (pauseOverlay != null) {
                pauseOverlay.toFront();
            }
        } else {
            // REANUDAR TODAS LAS TAREAS DE TALA AL SALIR DE PAUSA
            for (WoodcuttingTask task : activeWoodcuttingTasks.values()) {
                if (task.isActive) {
                    if (task.collectionTimeline != null &&
                            task.collectionTimeline.getStatus() == Animation.Status.PAUSED) {
                        task.collectionTimeline.play();
                    }
                    if (task.treeLifeTimeline != null &&
                            task.treeLifeTimeline.getStatus() == Animation.Status.PAUSED) {
                        task.treeLifeTimeline.play();
                    }
                }
            }

            // REANUDAR TODAS LAS TAREAS DE MINERÍA
            for (MiningTask task : activeMiningTasks.values()) {
                if (task.isActive) {
                    if (task.collectionTimeline != null &&
                            task.collectionTimeline.getStatus() == Animation.Status.PAUSED) {
                        task.collectionTimeline.play();
                    }
                    if (task.mineLifeTimeline != null &&
                            task.mineLifeTimeline.getStatus() == Animation.Status.PAUSED) {
                        task.mineLifeTimeline.play();
                    }
                }
            }

            // IMPORTANTE: Rehabilitar todos los elementos EXCEPTO aquellos que deben permanecer deshabilitados
            for (Node node : root.getChildren()) {
                // Solo rehabilitar elementos que fueron deshabilitados anteriormente
                // Las unidades seleccionadas ya eran interactivas, no necesitan cambios
                if (node != pauseOverlay && node != buildingGhost && node != selectionRect) {
                    if (!selectedUnitViews.contains(node)) { // Solo si no es una unidad seleccionada
                        node.setMouseTransparent(false);
                        node.setFocusTraversable(true);
                    }
                }
            }

            // Rehabilitar eventos del mouse usando los handlers guardados
            Scene scene = root.getScene();
            if (scene != null) {
                // Restaurar los handlers de selección de unidades
                if (savedMousePressed != null) {
                    scene.setOnMousePressed(savedMousePressed);
                } else {
                    // Si no hay handlers guardados, reconfigurar desde cero
                    setupUnitSelectionAndMovement(scene);
                }

                if (savedMouseDragged != null) {
                    scene.setOnMouseDragged(savedMouseDragged);
                }

                if (savedMouseReleased != null) {
                    scene.setOnMouseReleased(savedMouseReleased);
                }

                // Reconfigurar listeners de construcción
                setupBuildingListeners(scene);

                // Asegurar que los listeners de unidad se restauraron
                System.out.println("🔄 Listeners de unidades restaurados después de pausa");

                // Limpiar referencias guardadas
                savedMousePressed = null;
                savedMouseDragged = null;
                savedMouseReleased = null;
            }

            // Re-aplicar los estilos de selección a las unidades que estaban seleccionadas
            for (ImageView unit : selectedUnitViews) {
                applySelectionStyle(unit, true);
            }
        }
    }

    private void cleanupAllTasks() {
        // Limpiar tareas de tala
        for (WoodcuttingTask task : activeWoodcuttingTasks.values()) {
            if (task.collectionTimeline != null) {
                task.collectionTimeline.stop();
            }
            if (task.treeLifeTimeline != null) {
                task.treeLifeTimeline.stop();
            }
        }
        activeWoodcuttingTasks.clear();

        // Limpiar tareas de minería
        for (MiningTask task : activeMiningTasks.values()) {
            if (task.collectionTimeline != null) {
                task.collectionTimeline.stop();
            }
            if (task.mineLifeTimeline != null) {
                task.mineLifeTimeline.stop();
            }
        }
        activeMiningTasks.clear();

        System.out.println("🧹 Todas las tareas de recolección limpiadas");
    }

    @Override
    public void stop() throws Exception {
        // Limpiar todas las tareas antes de cerrar
        cleanupAllTasks();

        // Detener el ciclo de actualización de construcciones
        stopConstructionUpdateLoop();

        super.stop();
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


        return new StackPane(topPanel);
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
            if (node instanceof Label label) {
                if (label.getText().matches("\\d{2}:\\d{2}:\\d{2}")) {
                    label.setStyle(
                            "-fx-font-size: 20px; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-text-fill: #2c3e50;"
                    );
                }
            } else if (node instanceof HBox buttonBox) {
                // Modificar los botones del timer
                for (Node buttonNode : buttonBox.getChildren()) {
                    if (buttonNode instanceof Button button) {
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
     *  posicionar el panel superior automáticamente
     */
    private void positionTopPanel() {
        for (Node node : root.getChildren()) {
            if (node instanceof StackPane stackPane) {
                if (!stackPane.getChildren().isEmpty()) {
                    Node child = stackPane.getChildren().getFirst();
                    if (child instanceof HBox topPanel) {

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
            territory1.getTownHall().getStoredResources().addResource(ResourceType.GOLD, 1000);

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

        // Mostrar información de población
        int numHouses = territory1 != null && territory1.getTownHall() != null ?
                territory1.getTownHall().getHouses().size() : 0;
        int mineros = countUnitsOfType("minero");
        int leñadores = countUnitsOfType("leñador");

        System.out.println("Hay "+ leñadores + " cantidad de leñadores");

        Label populationInfo = new Label(
                "Casas: " + numHouses + " | Mineros: " + mineros + " | Leñadores: " + leñadores
        );
        populationInfo.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        populationInfo.setPadding(new Insets(0, 0, 5, 0));

        Region separator = new Region();
        separator.setPrefHeight(2);
        separator.setStyle("-fx-background-color: #d4af37; -fx-background-radius: 1;");

        VBox buttonContainer = new VBox(8);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(10, 0, 0, 0));

        // Obtener límites actuales
        int maxMineros = getMaxUnitsForHouses(numHouses);
        int maxLeñadores = getMaxUnitsForHouses(numHouses);

        Button houseButton = createTextButton("🏠", "Crear Casa", "60 Madera");
        Button barracksButton = createTextButton("⚔", "Crear Cuartel", "100 Madera");

        // Añadir información de límite a los botones de unidades
        String minerButtonText = "Crear Minero (" + mineros + "/" +
                (maxMineros == Integer.MAX_VALUE ? "∞" : maxMineros) + ")";
        String lumberButtonText = "Crear Leñador (" + leñadores + "/" +
                (maxLeñadores == Integer.MAX_VALUE ? "∞" : maxLeñadores) + ")";

        Button minerButton = createTextButton("", minerButtonText, "75 Oro");
        Button lumberjackButton = createTextButton("", lumberButtonText, "50 Oro");

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
        panel.getChildren().addAll(title, populationInfo, separator, buttonContainer);

        return panel;
    }

    // Método auxiliar para obtener límite máximo basado en casas
    private int getMaxUnitsForHouses(int numHouses) {
        switch (numHouses) {
            case 0:
                return 3;
            case 1:
                return 6;
            case 2:
                return 8;
            default:
                return Integer.MAX_VALUE;
        }
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

        // Verificar colisión
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
        String buildingTypeForBackend = "";

        if(currentBuildingType.equalsIgnoreCase("Casa")){
            creado = territory1.getTownHall().createHouse();
        } else if(currentBuildingType.equalsIgnoreCase("Cuartel")){
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
            String imagePath = "file:src/main/resources/images/" + currentBuildingType + ".png";
            Image buildingImage = new Image(imagePath);

            ImageView buildingView = new ImageView(buildingImage);
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

            System.out.println("✅ " + currentBuildingType + " construido en: (" + (int)posX + ", " + (int)posY + ")");
            cancelBuildingMode();

        } catch (Exception e) {
            System.err.println("❌ Error al colocar edificio visualmente: " + e.getMessage());
            cancelBuildingMode();
        }
    }

    // ==================== UNIDADES ====================

    private void createUnitNextToTownHall(String unitType, String imageName, double unitSize) {
        try {
            // Verificar recursos antes de crear la unidad
            if (territory1 != null && territory1.getTownHall() != null) {
                // VERIFICAR LÍMITE DE POBLACIÓN
                if (!canCreateMoreUnits(unitType)) {
                    System.out.println("❌ Límite de población alcanzado para " + unitType);
                    showPopulationLimitWarning(unitType);
                    return;
                }

                Map<ResourceType, Integer> unitCost = new HashMap<>();

                // Definir costos según el tipo de unidad
                if (unitType.equals("minero")) {
                    unitCost.put(ResourceType.GOLD, 75);
                } else if (unitType.equals("leñador")) {
                    unitCost.put(ResourceType.GOLD, 50);
                }

                // Verificar si puede pagar
                if (territory1.getTownHall().getStoredResources().canAfford(unitCost)) {
                    // Restar recursos usando el método spend existente
                    territory1.getTownHall().getStoredResources().spend(unitCost);
                    System.out.println("✅ Recursos descontados para crear " + unitType);

                    // Actualizar display de recursos
                    updateResourceDisplay();

                    // Proceder a crear la unidad visualmente
                    double townHallX = windowWidth * 0.3 - 85 + 100;
                    double townHallY = windowHeight * 0.4 - 85 + 100;
                    double townHallSize = 170;
                    double spacing = 5;

                    Position validPosition = findPositionForUnit(townHallX, townHallY, townHallSize, unitSize, spacing, unitType);

                    if (validPosition == null) {
                        System.out.println("❌ No hay espacio disponible para el " + unitType);

                        // Devolver recursos si no hay espacio
                        territory1.getTownHall().getStoredResources().addResource(ResourceType.GOLD,
                                unitCost.getOrDefault(ResourceType.GOLD, 0));
                        territory1.getTownHall().getStoredResources().addResource(ResourceType.WOOD,
                                unitCost.getOrDefault(ResourceType.WOOD, 0));
                        updateResourceDisplay();

                        return;
                    }

                    createUnitAtPosition(unitType, imageName, validPosition.x, validPosition.y, unitSize);

                    // Añadir la unidad al backend
                    territory1.getTownHall().createUnit(unitType);

                } else {
                    System.out.println("❌ Recursos insuficientes para crear " + unitType);
                    showInsufficientResourcesForUnit(unitType, unitCost);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error al crear " + unitType + ": " + e.getMessage());

            // En caso de error, devolver los recursos
            if (territory1 != null && territory1.getTownHall() != null) {
                Map<ResourceType, Integer> unitCost = new HashMap<>();
                if (unitType.equals("minero")) {
                    territory1.getTownHall().getStoredResources().addResource(ResourceType.GOLD, 75);
                } else if (unitType.equals("leñador")) {
                    territory1.getTownHall().getStoredResources().addResource(ResourceType.GOLD, 50);
                }
                updateResourceDisplay();
            }
        }
    }

    // Método para verificar límites de población
    private boolean canCreateMoreUnits(String unitType) {
        if (territory1 == null || territory1.getTownHall() == null) {
            return false;
        }

        // Obtener número de casas
        int numHouses = territory1.getTownHall().getHouses().size();

        // Límites basados en casas:
        // - 0 casas: máximo 3 mineros y 3 leñadores
        // - 1 casa: máximo 6 de cada uno
        // - 2 casas: máximo 8 de cada uno
        // - 3+ casas: ilimitado

        int maxUnits;
        switch (numHouses) {
            case 0:
                maxUnits = 3;
                break;
            case 1:
                maxUnits = 6;
                break;
            case 2:
                maxUnits = 8;
                break;
            default:
                maxUnits = Integer.MAX_VALUE; // Sin límite para 3+ casas
        }

        // Contar unidades existentes del tipo especificado
        int currentUnits = countUnitsOfType(unitType);

        System.out.println("📊 Verificación población: " + unitType +
                " - Casas: " + numHouses +
                " - Actuales: " + currentUnits +
                " - Máximo: " + maxUnits);

        return currentUnits < maxUnits;
    }

    // Método para contar unidades existentes de un tipo específico
    private int countUnitsOfType(String unitType) {
        int count = 0;

        // Primero contar en el backend (si existe el método)
        if(unitType.equalsIgnoreCase("minero")) {  // ← minúsculas
            return territory1.getTownHall().getMiners().size();
        }
        else if (unitType.equalsIgnoreCase("leñador")){  // ← minúsculas y revisa la codificación
            return territory1.getTownHall().getWoodCutters().size();
        }

        return count;
    }

    // Método para mostrar advertencia de límite de población
    private void showPopulationLimitWarning(String unitType) {
        Stage warningStage = new Stage();
        warningStage.initModality(Modality.APPLICATION_MODAL);
        warningStage.initStyle(StageStyle.TRANSPARENT);
        warningStage.setTitle("Límite de población alcanzado");

        VBox warningPanel = new VBox(15);
        warningPanel.setPadding(new Insets(25, 30, 25, 30));
        warningPanel.setAlignment(Pos.CENTER);
        warningPanel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.50); " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-color: #e74c3c; " + // Rojo para error
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.5, 0, 2);"
        );

        Label warningIcon = new Label("🚫");
        warningIcon.setStyle("-fx-font-size: 36px; -fx-padding: 0 0 5 0;");

        VBox messageContainer = new VBox(5);
        messageContainer.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Límite de población alcanzado");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        // Obtener información actual de población
        int numHouses = territory1 != null && territory1.getTownHall() != null ?
                territory1.getTownHall().getHouses().size() : 0;
        int currentUnits = countUnitsOfType(unitType);

        String limitMessage = getPopulationLimitMessage(numHouses, unitType);

        Label detailLabel = new Label(
                "Ya has alcanzado el límite de " + currentUnits + " " + unitType + "s\n\n" +
                        "Casas construidas: " + numHouses + "\n" +
                        limitMessage + "\n\n" +
                        "¡Construye más casas para aumentar tu población!"
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

        okButton.setOnAction(e -> warningStage.close());

        Button buildHouseButton = new Button("Construir Casa");
        buildHouseButton.setPrefWidth(150);
        buildHouseButton.setPrefHeight(38);
        buildHouseButton.setStyle(
                "-fx-background-color: rgba(46, 204, 113, 0.7); " +
                        "-fx-background-radius: 6; " +
                        "-fx-border-color: #27ae60; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 6; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold;"
        );

        buildHouseButton.setOnMouseEntered(e -> {
            buildHouseButton.setStyle(
                    "-fx-background-color: rgba(39, 174, 96, 0.8); " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-color: #229954; " +
                            "-fx-border-width: 2.5; " +
                            "-fx-border-radius: 6; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 12px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-effect: dropshadow(gaussian, rgba(39, 174, 96, 0.3), 5, 0.5, 0, 1);"
            );
        });

        buildHouseButton.setOnMouseExited(e -> {
            buildHouseButton.setStyle(
                    "-fx-background-color: rgba(46, 204, 113, 0.7); " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-color: #27ae60; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 6; " +
                            "-fx-cursor: hand; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 12px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-effect: null;"
            );
        });

        buildHouseButton.setOnAction(e -> {
            warningStage.close();
            // Abrir menú del TownHall para construir casa
            showTownHallMenu(windowWidth * 0.3, windowHeight * 0.4);
        });

        HBox buttonBox = new HBox(10, okButton, buildHouseButton);
        buttonBox.setAlignment(Pos.CENTER);

        warningPanel.getChildren().addAll(warningIcon, messageContainer, buttonBox);

        StackPane rootPane = new StackPane(warningPanel);
        rootPane.setStyle("-fx-background-color: transparent;");
        rootPane.setAlignment(Pos.CENTER);

        Scene warningScene = new Scene(rootPane, 350, 300);
        warningScene.setFill(Color.TRANSPARENT);

        warningStage.initOwner(root.getScene().getWindow());
        warningStage.setScene(warningScene);
        warningStage.setResizable(false);
        warningStage.showAndWait();
    }

    // Método auxiliar para obtener mensaje de límite
    private String getPopulationLimitMessage(int numHouses, String unitType) {
        switch (numHouses) {
            case 0:
                return "Límite actual: 3 " + unitType + "s (sin casas)";
            case 1:
                return "Límite actual: 6 " + unitType + "s (1 casa)";
            case 2:
                return "Límite actual: 8 " + unitType + "s (2 casas)";
            default:
                return "Límite actual: Ilimitado (" + numHouses + " casas)";
        }
    }

    // Método para mostrar advertencia de recursos insuficientes
    private void showInsufficientResourcesForUnit(String unitType, Map<ResourceType, Integer> cost) {
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
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0.5, 0, 2);"
        );

        Label warningIcon = new Label("⚠");
        warningIcon.setStyle("-fx-font-size: 36px; -fx-padding: 0 0 5 0;");

        VBox messageContainer = new VBox(5);
        messageContainer.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Recursos insuficientes");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Construir mensaje específico según el costo
        String costMessage = "Necesitas ";
        if (cost.containsKey(ResourceType.GOLD)) {
            costMessage += cost.get(ResourceType.GOLD) + " Oro";
        }
        if (cost.containsKey(ResourceType.GOLD) && cost.containsKey(ResourceType.WOOD)) {
            costMessage += " y ";
        }
        if (cost.containsKey(ResourceType.WOOD)) {
            costMessage += cost.get(ResourceType.WOOD) + " Madera";
        }
        costMessage += "\npara crear un " + unitType;

        Label detailLabel = new Label(costMessage);
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
            if (node instanceof ImageView existingBuilding && node != buildingGhost) {
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
            if (node instanceof ImageView existing && node != buildingGhost) {

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
            if (node instanceof ImageView existing && node != buildingGhost) {

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
            if (node instanceof ImageView imageView) {

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
            if (node instanceof ImageView imageView) {

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
            if (node instanceof ImageView imageView && node != buildingGhost) {
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
            if (node instanceof ImageView imageView) {

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
            if (node instanceof ImageView imageView) {

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
            if (node instanceof ImageView imageView) {

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
        return x < 20 || y < 20 ||
                x + size > windowWidth - 20 ||
                y + size > windowHeight - 20;
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

        // IMPORTANTE: Usar el mismo número que se pasa como parámetro
        mineView.setId("Mina_" + mineNumber);

        // Rotación aleatoria ligera
        mineView.setRotate((Math.random() - 0.5) * 20);

        // Efecto de sombra
        DropShadow mineShadow = new DropShadow();
        mineShadow.setColor(Color.rgb(139, 69, 19, 0.6));
        mineShadow.setRadius(8);
        mineShadow.setOffsetY(3);
        mineView.setEffect(mineShadow);

        // Hacer la mina interactiva
        makeMineInteractive(mineView, "Mina " + (mineNumber + 1));

        // Añadir UserData para identificación adicional
        mineView.setUserData("mina");

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
                (int)x + ", " + (int)y + ") con ID: Mina_" + mineNumber);
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
                    if (node instanceof ImageView existing) {
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
     * Versión compatible con el código existente (1 parámetro)
     */
    private void createKnightUnit(Popup barracksPopup) {
        // Usar null como barracksView específico (buscará el más cercano)
        createKnightUnit(barracksPopup, null);
    }

    private void createKnightUnit(Popup barracksPopup, ImageView specificBarracksView) {
        try {
            ImageView barracksToUse = specificBarracksView;

            // Si no se proporciona un cuartel específico, buscar el más cercano
            if (barracksToUse == null) {
                barracksToUse = findNearestBarracks();
                if (barracksToUse == null) {
                    System.out.println("❌ No se encontró ningún cuartel");
                    return;
                }
            }

            System.out.println("♞ Creando caballero en cuartel: " + barracksToUse.getId());

            // Obtener el MilitaryBase del backend
            MilitaryBase militaryBase = getMilitaryBaseForBarracks(barracksToUse);
            if (militaryBase == null) {
                System.out.println("❌ No se encontró MilitaryBase en el backend");
                return;
            }

            // VERIFICACIÓN SIMPLE: ¿Ya hay entrenamiento en curso?
            System.out.println("🔍 Verificando si ya hay entrenamiento en esta base...");

            boolean trainingInProgress = false;

            try {
                // Intento 1: Verificar la cola de entrenamiento directamente
                if (militaryBase.getTrainingQueue() != null) {
                    trainingInProgress = !militaryBase.getTrainingQueue().isEmpty();
                    System.out.println("   Cola de entrenamiento: " +
                            militaryBase.getTrainingQueue().size() + " unidades");
                }

                // Intento 2: Si no funciona el método directo, usar reflexión
                if (!trainingInProgress) {
                    try {
                        java.lang.reflect.Method getQueueMethod = militaryBase.getClass()
                                .getMethod("getTrainingQueue");
                        Object queue = getQueueMethod.invoke(militaryBase);

                        if (queue instanceof java.util.Collection) {
                            trainingInProgress = !((java.util.Collection<?>) queue).isEmpty();
                            System.out.println("   (via reflexión) Cola: " +
                                    ((java.util.Collection<?>) queue).size());
                        }
                    } catch (Exception e) {
                        // Método no disponible
                    }
                }

            } catch (Exception e) {
                System.err.println("⚠️ Error al verificar entrenamiento: " + e.getMessage());
                // Continuar de todos modos
            }

            // ¡BLOQUEO AQUÍ! Si ya hay entrenamiento, no permitir crear otro
            if (trainingInProgress) {
                System.out.println("❌ ¡Ya hay un entrenamiento en curso en este cuartel!");
                System.out.println("   No se pueden entrenar múltiples unidades a la vez.");
                showTrainingInProgressAlert();

                return; // ¡SALIR DEL MÉTODO! No crear caballero
            }

            // Si NO hay entrenamiento en curso, proceder a crear
            boolean knightCreated = militaryBase.createKnight();

            if (knightCreated) {
                System.out.println("✅ Orden de entrenamiento creada");

                // Obtener orden y continuar con el resto...
                UnitCreationOrder latestOrder = getLatestTrainingOrder(militaryBase);
                if (latestOrder != null) {
                    String unitId = latestOrder.getUnitId();
                    saveUnitTrainingInfo(unitId, "caballero", barracksToUse);
                    showKnightTrainingInProgress(barracksToUse, unitId);
                    restartConstructionUpdateLoopIfNeeded();
                    updateResourceDisplay();

                    System.out.println("♞ Entrenamiento iniciado - ID: " + unitId);
                }
            } else {
                System.out.println("❌ No se pudo crear caballero (recursos insuficientes)");
                showInsufficientResourcesForKnight();
            }

        } catch (Exception e) {
            System.err.println("❌ Error al crear caballero: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Muestra alerta de entrenamiento en curso con estilo del juego
     */
    private void showTrainingInProgressAlert() {
        Platform.runLater(() -> {
            // Crear popup con estilo del TownHall
            Popup alertPopup = new Popup();
            alertPopup.setAutoHide(true);
            alertPopup.setHideOnEscape(true);

            // Panel principal con MISMO estilo que TownHall (50% opacidad)
            VBox alertPanel = new VBox(15);
            alertPanel.setAlignment(Pos.CENTER);
            alertPanel.setPadding(new Insets(25, 30, 25, 30));
            alertPanel.setPrefSize(320, 200);

            // ESTILO EXACTO del TownHall
            alertPanel.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.50); " + // 50% opacidad
                            "-fx-background-radius: 15; " +
                            "-fx-border-color: #dcdde1; " +
                            "-fx-border-width: 1; " +
                            "-fx-border-radius: 15; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 15, 0.5, 0, 3);"
            );

            // Icono de advertencia
            Label warningIcon = new Label("⚔");
            warningIcon.setStyle("-fx-font-size: 36px; -fx-padding: 0 0 5 0;");

            // Contenedor del mensaje
            VBox messageContainer = new VBox(8);
            messageContainer.setAlignment(Pos.CENTER);

            // Título
            Label titleLabel = new Label("Entrenamiento en curso");
            titleLabel.setStyle(
                    "-fx-font-size: 18px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: #2c3e50;"
            );

            // Mensaje principal
            Label messageLabel = new Label("Ya hay un caballero en entrenamiento\n" +
                    "No puedes entrenar múltiples unidades\n" +
                    "a la vez en el mismo cuartel");
            messageLabel.setStyle(
                    "-fx-font-size: 13px; " +
                            "-fx-text-fill: #34495e; " +
                            "-fx-text-alignment: center;"
            );
            messageLabel.setWrapText(true);
            messageLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            // Separador elegante
            Region separator = new Region();
            separator.setPrefHeight(2);
            separator.setPrefWidth(180);
            separator.setStyle("-fx-background-color: linear-gradient(to right, transparent, #c0392b, transparent);");

            // Botón Aceptar
            Button okButton = createAlertButton("Entendido");
            okButton.setOnAction(e -> alertPopup.hide());

            // Añadir elementos al panel
            messageContainer.getChildren().addAll(titleLabel, messageLabel);
            alertPanel.getChildren().addAll(warningIcon, messageContainer, separator, okButton);

            // Contenedor final
            StackPane container = new StackPane(alertPanel);

            // Configurar posición (centro de la pantalla)
            alertPopup.getContent().add(container);

            // Mostrar centrado
            double x = (root.getScene().getWindow().getWidth() - alertPanel.getPrefWidth()) / 2;
            double y = (root.getScene().getWindow().getHeight() - alertPanel.getPrefHeight()) / 2;

            alertPopup.show(root.getScene().getWindow(), x, y);

            // Animación de entrada
            alertPanel.setScaleX(0.9);
            alertPanel.setScaleY(0.9);
            alertPanel.setOpacity(0);

            ScaleTransition scale = new ScaleTransition(Duration.millis(300), alertPanel);
            scale.setToX(1.0);
            scale.setToY(1.0);

            FadeTransition fade = new FadeTransition(Duration.millis(300), alertPanel);
            fade.setToValue(1.0);

            ParallelTransition entrance = new ParallelTransition(scale, fade);
            entrance.play();
        });
    }

    /**
     * Crea botón para alerta con estilo del TownHall
     */
    private Button createAlertButton(String text) {
        HBox buttonContent = new HBox(8);
        buttonContent.setAlignment(Pos.CENTER);
        buttonContent.setPadding(new Insets(8, 20, 8, 20));

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        buttonContent.getChildren().add(textLabel);

        Button button = new Button();
        button.setGraphic(buttonContent);
        button.setPrefWidth(180);
        button.setPrefHeight(45);

        // ESTILO BASE con 50% opacidad igual que TownHall
        String baseStyle =
                "-fx-background-color: rgba(255, 255, 255, 0.50); " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-color: #dcdde1; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-cursor: hand; " +
                        "-fx-text-fill: #2c3e50;";

        // Color rojo para alerta
        String borderColor = "#c0392b";

        // Aplicar estilo
        button.setStyle(baseStyle +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 2;");

        // EFECTO HOVER
        button.setOnMouseEntered(e -> {
            String hoverStyle =
                    "-fx-background-color: rgba(236, 240, 241, 0.50); " +
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
                    "-fx-background-color: rgba(220, 220, 220, 0.50);");
        });

        button.setOnMouseReleased(e -> {
            button.setStyle(baseStyle +
                    "-fx-border-color: " + borderColor + ";" +
                    "-fx-border-width: 2;");
        });

        return button;
    }

    // Mapa para rastrear entrenamientos en progreso
    private final Map<String, UnitTrainingInfo> unitTrainingMap = new HashMap<>();

    private class UnitTrainingInfo {
        String unitType;
        String barracksId;
        ImageView barracksView;
        long startTime;

        UnitTrainingInfo(String unitType, ImageView barracksView) {
            this.unitType = unitType;
            this.barracksView = barracksView;
            this.startTime = System.currentTimeMillis();
        }
    }

    // Obtener MilitaryBase del backend para un cuartel específico
    private MilitaryBase getMilitaryBaseForBarracks(ImageView barracksView) {
        // Asumiendo que hay un MilitaryBase por territorio
        if (territory1 != null && territory1.getTownHall() != null) {
            // Buscar MilitaryBase en los edificios del territorio
            for (Object building : territory1.getTownHall().getOwnedBuildings()) {
                if (building instanceof MilitaryBase) {
                    return (MilitaryBase) building;
                }
            }
        }
        return null;
    }

    // Obtener la orden de entrenamiento más reciente
    private UnitCreationOrder getLatestTrainingOrder(MilitaryBase militaryBase) {
        Deque<UnitCreationOrder> trainingQueue = militaryBase.getTrainingQueue();
        if (trainingQueue.isEmpty()) {
            return null;
        }

        // Buscar la última orden (más reciente)
        UnitCreationOrder latest = null;
        for (UnitCreationOrder order : trainingQueue) {
            latest = order;
        }
        return latest;
    }

    // Guardar información del entrenamiento
    private void saveUnitTrainingInfo(String unitId, String unitType, ImageView barracksView) {
        unitTrainingMap.put(unitId, new UnitTrainingInfo(unitType, barracksView));
    }

    // Mostrar entrenamiento en progreso
    private void showKnightTrainingInProgress(ImageView barracksView, String unitId) {
        try {
            double barracksX = barracksView.getX();
            double barracksY = barracksView.getY();
            double barracksWidth = barracksView.getFitWidth();

            // Crear visualización de entrenamiento en progreso
            Image trainingImage = new Image("file:src/main/resources/images/Construccion.png");
            ImageView trainingView = new ImageView(trainingImage);

            double size = 60; // Tamaño para indicador de entrenamiento
            trainingView.setFitWidth(size);
            trainingView.setFitHeight(size);
            trainingView.setPreserveRatio(true);

            // Posicionar cerca del cuartel
            double x = barracksX + barracksWidth/2 - size/2;
            double y = barracksY - size - 10;
            trainingView.setX(x);
            trainingView.setY(y);
            trainingView.setOpacity(0.7);
            trainingView.setId("training_" + unitId);

            // Crear barra de progreso para entrenamiento
            createTrainingProgressBar(unitId, x, y, size, 40); // 40 segundos para caballero

            // Guardar referencia
            unitTrainingMap.get(unitId).barracksView = barracksView;

            // Añadir a la escena
            root.getChildren().add(trainingView);

            System.out.println("⏳ Mostrando entrenamiento en progreso para: " + unitId);

        } catch (Exception e) {
            System.err.println("❌ Error al mostrar entrenamiento: " + e.getMessage());
        }
    }

    // Crear barra de progreso para entrenamiento
    private void createTrainingProgressBar(String unitId, double x, double y, double width, int totalTime) {
        double alturaBarra = 6;
        double barraY = y - alturaBarra - 5;

        // Fondo de la barra
        Rectangle fondo = new Rectangle(width, alturaBarra);
        fondo.setFill(Color.rgb(100, 100, 100, 0.7));
        fondo.setX(x);
        fondo.setY(barraY);
        fondo.setArcWidth(3);
        fondo.setArcHeight(3);

        // Barra de progreso
        Rectangle barraProgreso = new Rectangle(0, alturaBarra);
        barraProgreso.setFill(Color.rgb(184, 134, 11, 0.8)); // Dorado para caballero
        barraProgreso.setX(x);
        barraProgreso.setY(barraY);
        barraProgreso.setArcWidth(3);
        barraProgreso.setArcHeight(3);

        // Animación de progreso
        Timeline animacion = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(barraProgreso.widthProperty(), 0)),
                new KeyFrame(Duration.seconds(totalTime),
                        new KeyValue(barraProgreso.widthProperty(), width))
        );

        animacion.setCycleCount(1);
        animacion.play();

        // Contenedor
        Pane contenedor = new Pane(fondo, barraProgreso);
        contenedor.setId("training_progress_" + unitId);
        contenedor.setMouseTransparent(true);

        root.getChildren().add(contenedor);
    }

    /**
     * Encuentra el cuartel más cercano
     */
    private ImageView findNearestBarracks() {
        ImageView nearestBarracks = null;
        double minDistance = Double.MAX_VALUE;

        // Buscar entre todos los nodos del root
        for (Node node : root.getChildren()) {
            if (node instanceof ImageView imageView) {

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
                createKnightAtPosition("caballero", "Caballero.png", validPosition.x, validPosition.y, knightSize);
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

    private Map<ResourceType, Integer> getKnightCost() {
        return Map.of(ResourceType.GOLD, 80); // Según tu backend
    }

    /**
     * Busca posición compacta para caballero (uno al lado del otro)
     */
    public Position findPositionForKnightCompact(double barracksX, double barracksY,
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
            if (node instanceof ImageView existing) {

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
            if (node instanceof ImageView imageView) {

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
            if (node instanceof ImageView imageView) {

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

        Label detailLabel = new Label("Necesitas 80 Oro \npara crear un Caballero");
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


    //METODOS DE LA BARRA DE CONSTRUCCION =========
    // Reemplaza la clase BarraProgresoAnimadaManager actual con esta versión completa:

    private class BarraProgresoAnimadaManager {
        private final Map<String, ProgressBarData> barrasPorConstruccion = new HashMap<>();

        private class ProgressBarData {
            Pane contenedor;
            Rectangle barraProgreso;
            Rectangle fondo;
            Timeline animacion;
            double progresoActual = 0.0;
            double duracionTotal; // en segundos
        }

        public void crearBarraProgresoAnimada(String buildingId, ImageView constructionView, int tiempoConstruccionSegundos) {
            if (barrasPorConstruccion.containsKey(buildingId)) {
                return; // Ya existe
            }

            double x = constructionView.getX();
            double y = constructionView.getY();
            double width = constructionView.getFitWidth();
            double alturaBarra = 6;
            double barraY = y - alturaBarra - 5;

            // Crear datos de la barra
            ProgressBarData data = new ProgressBarData();
            data.duracionTotal = tiempoConstruccionSegundos;

            // Fondo de la barra
            data.fondo = new Rectangle(width, alturaBarra);
            data.fondo.setFill(Color.rgb(100, 100, 100, 0.7));
            data.fondo.setX(x);
            data.fondo.setY(barraY);
            data.fondo.setArcWidth(3);
            data.fondo.setArcHeight(3);

            // Barra de progreso (empieza en 0)
            data.barraProgreso = new Rectangle(0, alturaBarra);
            data.barraProgreso.setFill(Color.rgb(0, 200, 0, 0.8));
            data.barraProgreso.setX(x);
            data.barraProgreso.setY(barraY);
            data.barraProgreso.setArcWidth(3);
            data.barraProgreso.setArcHeight(3);

            // Contenedor - CRÍTICO: hacerlo transparente al mouse
            data.contenedor = new Pane(data.fondo, data.barraProgreso);
            data.contenedor.setId("progress_" + buildingId);

            // HACER TODO EL CONTENEDOR TRANSPARENTE AL MOUSE
            data.contenedor.setMouseTransparent(true);
            data.contenedor.setPickOnBounds(false);
            data.contenedor.setFocusTraversable(false);

            // También hacer los rectángulos individuales transparentes al mouse
            data.fondo.setMouseTransparent(true);
            data.barraProgreso.setMouseTransparent(true);

            // Crear animación suave
            data.animacion = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(data.barraProgreso.widthProperty(), 0)),
                    new KeyFrame(Duration.seconds(tiempoConstruccionSegundos),
                            new KeyValue(data.barraProgreso.widthProperty(), width))
            );

            // Actualizar color durante la animación
            data.animacion.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                double progreso = newTime.toSeconds() / tiempoConstruccionSegundos;
                data.progresoActual = progreso;
                actualizarColorBarra(data.barraProgreso, progreso);
            });

            // Configurar animación
            data.animacion.setCycleCount(1); // Solo una vez

            barrasPorConstruccion.put(buildingId, data);
            root.getChildren().add(data.contenedor);
            data.contenedor.toFront();

            System.out.println("📊 Barra animada creada para " + buildingId +
                    " - Duración: " + tiempoConstruccionSegundos + "s");
        }

        private void actualizarColorBarra(Rectangle barra, double progreso) {
            if (progreso >= 1.0) {
                barra.setFill(Color.rgb(0, 255, 0, 1.0)); // Verde brillante al 100%
            } else if (progreso >= 0.75) {
                barra.setFill(Color.rgb(0, 200, 0, 0.8)); // Verde
            } else if (progreso >= 0.5) {
                barra.setFill(Color.rgb(255, 165, 0, 0.8)); // Naranja
            } else {
                barra.setFill(Color.rgb(255, 0, 0, 0.8)); // Rojo
            }
        }

        public void iniciarAnimacion(String buildingId) {
            ProgressBarData data = barrasPorConstruccion.get(buildingId);
            if (data != null && data.animacion != null) {
                data.animacion.play();
                System.out.println("▶️ Iniciando animación de barra para: " + buildingId);
            }
        }

        public void iniciarAnimacionDesde(String buildingId, int tiempoTranscurrido) {
            ProgressBarData data = barrasPorConstruccion.get(buildingId);
            if (data != null && data.animacion != null) {
                // Saltar al tiempo ya transcurrido
                data.animacion.jumpTo(Duration.seconds(tiempoTranscurrido));
                data.animacion.play();
                System.out.println("⏩ Iniciando animación desde " + tiempoTranscurrido + "s para: " + buildingId);
            }
        }

        public void actualizarProgreso(String buildingId, double progreso, int tiempoTranscurrido) {
            ProgressBarData data = barrasPorConstruccion.get(buildingId);
            if (data != null && data.barraProgreso != null) {
                // Actualizar el ancho de la barra basado en el progreso
                double width = data.fondo.getWidth();
                data.barraProgreso.setWidth(width * progreso);
                data.progresoActual = progreso;
                actualizarColorBarra(data.barraProgreso, progreso);
            }
        }

        public void pausarAnimacion(String buildingId) {
            ProgressBarData data = barrasPorConstruccion.get(buildingId);
            if (data != null && data.animacion != null) {
                data.animacion.pause();
                System.out.println("⏸️ Pausando animación de barra para: " + buildingId);
            }
        }

        public void reanudarAnimacion(String buildingId) {
            ProgressBarData data = barrasPorConstruccion.get(buildingId);
            if (data != null && data.animacion != null &&
                    data.animacion.getStatus() == Animation.Status.PAUSED) {
                data.animacion.play();
                System.out.println("▶️ Reanudando animación de barra para: " + buildingId);
            }
        }

        public void detenerAnimacion(String buildingId) {
            ProgressBarData data = barrasPorConstruccion.get(buildingId);
            if (data != null && data.animacion != null) {
                data.animacion.stop();
                System.out.println("⏹️ Deteniendo animación para: " + buildingId);
            }
        }

        public void eliminarBarraProgreso(String buildingId) {
            ProgressBarData data = barrasPorConstruccion.remove(buildingId);
            if (data != null) {
                if (data.animacion != null) {
                    data.animacion.stop();
                }
                root.getChildren().remove(data.contenedor);
                System.out.println("🗑️ Barra animada eliminada: " + buildingId);
            }
        }

        public double obtenerProgresoActual(String buildingId) {
            ProgressBarData data = barrasPorConstruccion.get(buildingId);
            return data != null ? data.progresoActual : 0.0;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}