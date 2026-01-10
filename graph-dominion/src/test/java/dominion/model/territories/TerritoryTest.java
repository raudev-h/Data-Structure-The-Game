package dominion.model.territories;

import dominion.model.buildings.TownHall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TerritoryTest {
    Territory territory;
    TownHall townHall;

    @BeforeEach
    void setUp(){
        territory = new Territory();
        townHall = new TownHall("",territory,100, 5);
    }
    @Test
    void getTownHall_initialize_shouldReturnTownHallObject(){
        assertSame(townHall,territory.getTownHall(),
                "Debe devolver el TownHall que posee");
    }
}
