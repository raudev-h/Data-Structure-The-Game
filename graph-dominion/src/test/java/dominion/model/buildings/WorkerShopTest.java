package dominion.model.buildings;

import dominion.model.resources.Miner;
import dominion.model.resources.ResourceType;
import dominion.model.resources.Woodcutter;
import dominion.model.territories.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorkerShopTest {
    private TownHall townHall;
    private Territory territory;
    @BeforeEach
    void setUp(){
        territory = new Territory();
        townHall = new TownHall("",territory,100, 5);
    }


    @Test
    void buyWoodcutter_success() {
        TownHall townHall = new TownHall("",territory,100, 5);
        WorkerShop shop = new WorkerShop(townHall);

        // Oro inicial suficiente
        townHall.getStoredResources().addResource(ResourceType.GOLD, 100);

        Woodcutter wc = shop.buyWoodcutter();

        assertNotNull(wc);
        assertEquals(50,
                townHall.getStoredResources().getAmount(ResourceType.GOLD),
                "Gold should be reduced after buying woodcutter");
    }

    @Test
    void buyWoodcutter_notEnoughGold() {
        TownHall townHall = new TownHall("",territory,100, 5);
        WorkerShop shop = new WorkerShop(townHall);

      Woodcutter wc = shop.buyWoodcutter();

        assertNull(wc, "Woodcutter should not be created without gold");
    }

    @Test
    void buyMiner_success() {
        TownHall townHall = new TownHall("",territory,100, 5);
        WorkerShop shop = new WorkerShop(townHall);

        townHall.getStoredResources().addResource(ResourceType.GOLD, 100);

        Miner miner = shop.buyMiner();

        assertNotNull(miner);
        assertEquals(25,
                townHall.getStoredResources().getAmount(ResourceType.GOLD),
                "Gold should be reduced after buying miner");
    }

    @Test
    void buyMiner_notEnoughGold() {
        TownHall townHall = new TownHall("",territory,100, 5);
        WorkerShop shop = new WorkerShop(townHall);

        Miner miner = shop.buyMiner();

        assertNull(miner, "Miner should not be created without gold");
    }
}
