package dominion.model.buildings;

import dominion.model.territories.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HouseTest {
    private TownHall townHall;
    private Territory territory;

    @BeforeEach
    void setUp(){
        territory = new Territory();
        townHall = new TownHall("wasd",territory,100,
        2);
    }
    @Test
    void House_instanceHouse_shouldIncreaseMaxPopulationCapacity(){

        assertEquals(0,townHall.getMaxPopulationCapacity(),
                "Debe devolver 0");
        House house = new House("ewer", territory, 100);
        assertEquals(5,townHall.getMaxPopulationCapacity(),
                "La cantidad máxima de población debe aumentar en 5");
    }
}
