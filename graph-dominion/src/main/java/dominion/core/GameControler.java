package dominion.core;

import dominion.model.buildings.MilitaryBase;
import dominion.model.buildings.TownHall;
import dominion.model.players.Player;
import dominion.model.territories.Territory;

import java.util.ArrayList;

public class GameControler {

    //FIELDS

    private GameTimer gametimer;
    private GameMap gameMap;
    private ArrayList<Player> players;
    private boolean gameRunning;
    private Player currentPlayer;

    //TODO:CONSTRUCTOR
    public GameControler(){
        gameRunning = true;
        players = new ArrayList<>();
        gameMap = new GameMap();
        inicalizar();
    }

    public void inicalizar(){
        //Creacion de jugadores
        this.currentPlayer = createPlayer("Player", Color.BLUE);
        Player enemy = createPlayer("Nivel 1", Color.RED);
        Player finalEnemy = createPlayer("Boss", Color.RED);

        //Crear Territorio correspondientes
        Territory playerTerritory = new Territory();
        Territory enemyTerritory = new Territory();
        Territory bossTerritory = new Territory();

        //Asignacion  de territorios a player
        currentPlayer.addTerritory(playerTerritory);
        enemy.addTerritory(enemyTerritory);
        finalEnemy.addTerritory(bossTerritory);

        //Asignacion de players a territorios
        playerTerritory.setPlayerOwner(currentPlayer);
        enemyTerritory.setPlayerOwner(enemy);
        bossTerritory.setPlayerOwner(finalEnemy);

        //Agregar territorios a gameMap
        gameMap.addTerritory(playerTerritory);
        gameMap.addTerritory(enemyTerritory);
        gameMap.addTerritory(bossTerritory);

        //Configurar territorios en el grafo
        gameMap.addRoute(playerTerritory, enemyTerritory);
        gameMap.addRoute(enemyTerritory, bossTerritory);

        //Crear TownHall a territorios enemigos
        TownHall enemyTownHall = new TownHall("1",enemyTerritory, 10, 5);
        TownHall finalEnemyTownHall = new TownHall("2",bossTerritory, 10, 5);
        enemyTerritory.setTownHall(enemyTownHall);
        bossTerritory.setTownHall(finalEnemyTownHall);

        //Crear militaryBase a territorios enemigos
        enemyTownHall.addMilitaryBase(enemyTerritory);
        finalEnemyTownHall.addMilitaryBase(bossTerritory);

        //Añadir caballeros a las militaryBase
        enemyTownHall.getMilitaryBases().getFirst().addKnights(1);
        finalEnemyTownHall.getMilitaryBases().getFirst().addKnights(2);


    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public GameMap getGameMap(){
        return gameMap;
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

    // Atack methods - YA CON VALIDACIÓN DE ADYACENCIA
    public AttackResult handleAttack(Player attacker, Territory target) {
        // Validaciones básicas
        /*
        if (!gameRunning || gametimer.getElapsedSeconds() < 5 * 60_000) {
            return AttackResult.INVALID;
        }
        /
         */

        if (attacker == null || target == null) {
            return AttackResult.INVALID;
        }

        if (attacker.getKnightAmount() == 0) {
            return AttackResult.INVALID;
        }

        if (target.getPlayerOwner().equals(attacker)) {
            return AttackResult.INVALID;
        }

        AttackResult result = attacker.attack(target);


        if (result.equals(AttackResult.VICTORY)) {
            for (MilitaryBase mb : target.getTownHall().getMilitaryBases()) {
                mb.removeAllKnights();
            }

            // Transferir territorio al atacante después de victoria
            Player defender = target.getPlayerOwner();
            defender.deleteTerritory(target);
            attacker.addTerritory(target);
            target.setPlayerOwner(attacker);
    }

        return result;
    }

    // Método para obtener territorios atacables desde un jugador
    public ArrayList<Territory> getAttackableTerritories(Player player) {
        ArrayList<Territory> attackable = new ArrayList<>();

        for (Player otherPlayer : players) {
            if (!otherPlayer.equals(player) && otherPlayer.isAlive()) {
                for (Territory enemyTerritory : otherPlayer.getTerritories()) {
                    // Verificar si es adyacente a algún territorio del jugador
                    for (Territory playerTerritory : player.getTerritories()) {
                        if (gameMap.areAdjacent(playerTerritory, enemyTerritory)) {
                            attackable.add(enemyTerritory);
                            break; // No necesitamos verificar más territorios del jugador
                        }
                    }
                }
            }
        }

        return attackable;
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


}
