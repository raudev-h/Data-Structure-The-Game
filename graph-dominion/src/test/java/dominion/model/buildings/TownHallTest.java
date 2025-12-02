package dominion.model.buildings;

import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class TownHallTest {
    private TownHall townHall;
    private Territory territory;

    @BeforeEach
    void setUp(){
        territory = new Territory();
        townHall = new TownHall("",territory,100,0,5);
    }
    @Test
    void createHouse_sufficientResources_shouldIncreaseSizeOfOwnedBuildings(){
        townHall.getStoredResources().addResource(ResourceType.WOOD,60);
        townHall.createHouse();
        assertEquals(1,townHall.getOwnedBuildings().size(),
                "Debe haber aumentado la cantidad de construcciones que le pertenecen");
    }
}
