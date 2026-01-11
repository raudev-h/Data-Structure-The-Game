package dominion.core;

import dominion.model.buildings.Building;
import dominion.model.buildings.MilitaryBase;
import dominion.model.buildings.TownHall;
import dominion.model.buildings.TownHallTest;
import dominion.model.players.Player;
import dominion.model.territories.Territory;
import dominion.model.units.Knight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


public class GameControllerTest {
    private GameControler gameControler;

    private Player player1;
    private Player player2;

    private Territory territory;
    private TownHall townHall;

    private Territory territory2;
    private TownHall townHall2;

    @BeforeEach
    void setUp(){
         gameControler = new GameControler();
         player1 = new Player("Atacante",Color.Azul );
         player2 = new Player("Defensor", Color.Rojo);

    }

    private void setupComplexMilitaryScenarioAttack(AttackResult attackResult) {
        MilitaryBase mb1 = new MilitaryBase("MB-01", territory, 50);
        MilitaryBase mb2 = new MilitaryBase("MB-02", territory2, 50);

        int maxHPKnights = 5;
        if(attackResult.equals(AttackResult.DEFEAT)){
            maxHPKnights = 10;
        }


        // Military Base 1
        Knight knight1 = new Knight(10, 8, 3, "K-001", townHall, territory);
        Knight knight2 = new Knight(10, 8, 3, "K-002", townHall, territory);
        Knight knight3 = new Knight(10, 8, 3, "K-003", townHall, territory);
        mb1.getKnights().addAll(Arrays.asList(knight1, knight2, knight3));

        // Military Base 2
        Knight knight4 = new Knight(maxHPKnights, 1, 3, "K-004", townHall2, territory2);
        Knight knight5 = new Knight(maxHPKnights, 1, 3, "K-005", townHall2, territory2);
        Knight knight6 = new Knight(maxHPKnights, 2, 3, "K-006", townHall2, territory2);
        mb2.getKnights().addAll(Arrays.asList(knight4, knight5, knight6));

        territory = new Territory();
        territory2 = new Territory();
        townHall = new TownHall("1",territory,100,5);
        townHall2 = new TownHall("2",territory2,100,5);
        territory.setPlayer(player1);
        territory2.setPlayer(player2);


        townHall.getOwnedBuildings().add(mb1);
        townHall2.getOwnedBuildings().add(mb2);

        territory.setTownHall(townHall);
        territory2.setTownHall(townHall2);

        player1.getTerritories().add(territory);
        player2.getTerritories().add(territory2);

    }


    @Test
    void testAttack_with_Defeat_Result(){
        setupComplexMilitaryScenarioAttack(AttackResult.DEFEAT);
        gameControler.starGame();
        gameControler.getGameTimer().forceAttackMode();
        assertEquals(AttackResult.DEFEAT,gameControler.handleAttack(player1,player2.getTerritories().get(0)));

    }

    @Test
    void testAttack_with_Victory_Result(){
        setupComplexMilitaryScenarioAttack(AttackResult.VICTORY);
        gameControler.starGame();
        gameControler.getGameTimer().forceAttackMode();
        assertEquals(AttackResult.VICTORY,gameControler.handleAttack(player1,player2.getTerritories().get(0)));


    }

    @Test
    void testAttack_with_time_under_5min(){
        setupComplexMilitaryScenarioAttack(AttackResult.VICTORY);
        gameControler.starGame();
        assertEquals(AttackResult.INVALID,gameControler.handleAttack(player1,player2.getTerritories().get(0)));

    }

    @Test
    void testAttack_with_game_not_started(){
        setupComplexMilitaryScenarioAttack(AttackResult.VICTORY);
        gameControler.getGameTimer().forceAttackMode();
        assertEquals(AttackResult.INVALID,gameControler.handleAttack(player1,player2.getTerritories().get(0)));

    }

    @Test
    void testAttack_with_same_player(){
        setupComplexMilitaryScenarioAttack(AttackResult.VICTORY);
        gameControler.getGameTimer().forceAttackMode();
        assertEquals(AttackResult.INVALID,gameControler.handleAttack(player1,player1.getTerritories().get(0)));

    }

    @Test
    void testAttack_with_no_knights(){
        setupComplexMilitaryScenarioAttack(AttackResult.VICTORY);
        gameControler.getGameTimer().forceAttackMode();

        // Eliminar knights de todas las MilitaryBases del player1
        for (Territory territory : player1.getTerritories()) {
            if (territory.getTownHall() != null) {
                for (Building building : territory.getTownHall().getOwnedBuildings()) {
                    if (building instanceof MilitaryBase) {
                        MilitaryBase militaryBase = (MilitaryBase) building;
                        militaryBase.getKnights().clear();
                    }
                }
            }
        }

        assertEquals(0, player1.getKnights().size());
        assertEquals(AttackResult.INVALID,gameControler.handleAttack(player1,player2.getTerritories().get(0)));

    }
}
