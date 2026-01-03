package dominion.core;

import com.almasb.fxgl.app.GameController;
import dominion.model.buildings.MilitaryBase;
import dominion.model.players.Player;
import dominion.model.territories.Territory;
import dominion.model.units.Unit;
import javafx.scene.canvas.Canvas;

import java.util.ArrayList;

public class GameControler {

    //FIELDS

    private GameTimer gametimer;
    private GameMap gameMap;
    private ArrayList<Player> players;
    private boolean gameRunning;
    private GameController gameController;
    private Canvas canvas;

    //TODO:CONSTRUCTOR
    public GameControler(){
        gameRunning = true;
        players = new ArrayList<>();
    }

    //METHODS

    //Clock Methods

    public boolean pauseClock(){

        return gametimer.pause();
    }

    public boolean startClock(){
        return gametimer.start();
    }

    public long getElapsedSeconds(){

        return gametimer.getElapsedSeconds();
    }

    public void resetClock(){
        gametimer.reset();
    }

    //Atack methods TODO: Validar que el target sea vecino en el grafo
    public AttackResult handleAttack(Player attacker, Territory target) {
        if ((!gameRunning || gametimer.getElapsedSeconds() < 5*60_000) || (attacker == null || target == null) ||
                attacker.getKnightAmount() == 0 || target.getPlayerOwner().equals(attacker) ){

            return AttackResult.INVALID;
        }
        // --- todas las reglas pasadas ---

        AttackResult result = attacker.attack(target);

        if(result.equals(AttackResult.VICTORY)) {
            for (MilitaryBase mb : target.getTownHall().getMilitaryBases()) {
                mb.removeAllKnights();
            }
        }

        return result;// devuelve Victoria o Derrota
    }

    //Player
    public Player createPlayer(String nombre, Color color){
        Player newPlayer = new Player(nombre, color);
        players.add(newPlayer);

        return newPlayer;
    }

    public GameMap createGameMap(){
        GameMap gameMap = new GameMap();
        return  gameMap;
    }
    public void moveUnit(Unit unit, Territory destination) {
        Territory origin = unit.getCurrentTerritory();

        if (origin != null) {
            origin.removeUnit(unit);
        }

        destination.addUnit(unit);
        unit.setCurrentTerritory(destination);
    }

    private ArrayList<Unit> selectedUnits = new ArrayList<>();

    public void selectUnit(Unit u) {
        if (!selectedUnits.contains(u)) {
            selectedUnits.add(u);
            u.selected = true;
        }
    }

    public void clearSelection() {
        for (Unit u : selectedUnits) {
            u.selected = false;
        }
        selectedUnits.clear();
    }

    public void handleClick(double x, double y, boolean shift) {

    }





}
