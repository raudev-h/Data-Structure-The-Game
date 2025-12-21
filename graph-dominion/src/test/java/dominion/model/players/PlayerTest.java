package dominion.model.players;

import dominion.core.Color;
import dominion.model.buildings.MilitaryBase;
import dominion.model.buildings.TownHall;
import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;
import dominion.model.units.Knight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {
    private Player player1;
    private Territory territory;

    @BeforeEach
    void setUP(){
        player1 = new Player("Principal Player", Color.Azul );
        territory = new Territory();
    }

    private void setupComplexMilitaryScenario() {
        TownHall townHall = new TownHall("1",territory,100, 5);
        territory.setTownHall(townHall);
        territory.setPlayer(player1);
        player1.addTerritory(territory);

        MilitaryBase mb1 = new MilitaryBase("MB-01", territory, 50);
        MilitaryBase mb2 = new MilitaryBase("MB-02", territory, 50);

        // Military Base 1
        Knight knight1 = new Knight(100, 8, 3, "K-001", townHall, territory);
        Knight knight2 = new Knight(100, 8, 3, "K-002", townHall, territory);
        Knight knight3 = new Knight(100, 8, 3, "K-003", townHall, territory);
        mb1.getKnights().addAll(Arrays.asList(knight1, knight2, knight3));

        // Military Base 2
        Knight knight4 = new Knight(100, 4, 3, "K-004", townHall, territory);
        Knight knight5 = new Knight(100, 19, 3, "K-005", townHall, territory);
        Knight knight6 = new Knight(100, 28, 3, "K-006", townHall, territory);
        mb2.getKnights().addAll(Arrays.asList(knight4, knight5, knight6));

        townHall.getOwnedBuildings().addAll(Arrays.asList(mb1, mb2));

        Territory territory1 = new Territory();
        TownHall townHall1 = new TownHall("2", territory1,100, 5);
        territory1.setPlayer(player1);
        territory1.setTownHall(townHall1);
        territory1.setPlayer(player1);
        player1.addTerritory(territory1);

        MilitaryBase mb3 = new MilitaryBase("MB-03", territory, 50);
        Knight knight7 = new Knight(100, 23, 3, "K-004", townHall1, territory1);
        mb3.getKnights().add(knight7);
        townHall1.getOwnedBuildings().add(mb3);

    }

    @Test
    void add_territoryTest(){
        Territory t = new Territory();
        assertTrue(player1.addTerritory(t));
        assertEquals(1, player1.getTerritories().size());
    }

    @Test
    void get_TotalEffectiveDefense_All_Territories(){
        setupComplexMilitaryScenario();
        assertEquals(798, player1.calculateTotalDefence());
    }

    @Test
    void get_Knights_amout(){
        setupComplexMilitaryScenario();
        assertEquals(7, player1.getKnights().size());
    }

    @Test
    void get_Total_Attack(){
        setupComplexMilitaryScenario();
        assertEquals(70, player1.calculateAttackForce());
    }



}
