package dominion.model.shop;

import dominion.model.buildings.TownHall;
import dominion.model.units.Miner;
import dominion.model.units.WoodCutter;
import dominion.model.units.ResourceCollector;
import dominion.model.resources.ResourceCollection;
import dominion.model.resources.ResourceType;

import java.util.Map;

public class WorkerShop {

    private final TownHall townHall;

    // Costs
    private static final Map<ResourceType, Integer> WOODCUTTER_COST =
            Map.of(ResourceType.GOLD, 50);

    private static final Map<ResourceType, Integer> MINER_COST =
            Map.of(ResourceType.GOLD, 75);

    public WorkerShop(TownHall townHall) {
        this.townHall = townHall;
    }

    // --- BUY WOODCUTTER ---
    public ResourceCollector buyWoodcutter() {

        // 1. Population limit
        if (!townHall.canAddWorker()) {
            System.out.println("no mas gente ");
            return null;
        }

        // 2. Check resources
        ResourceCollection storage = townHall.getStoredResources();
        if (!storage.canAfford(WOODCUTTER_COST)) {
            System.out.println("no hay plata");
            return null;
        }

        // 3. Pay cost
        storage.spend(WOODCUTTER_COST);

        // 4. Create worker
        ResourceCollector wc = new WoodCutter(
                "Woodcutter" + System.nanoTime(),
                townHall.getTerritory(),
                10   // damage per second
        );

        // 5. Register in TownHall
        townHall.addResourceCollector(wc);

        System.out.println("✔ Woodcutter created!");
        return wc;
    }

    // --- BUY MINER ---
    public ResourceCollector buyMiner() {

        if (!townHall.canAddWorker()) {
            System.out.println("no mas gente");
            return null;
        }

        ResourceCollection storage = townHall.getStoredResources();
        if (!storage.canAfford(MINER_COST)) {
            System.out.println("no hay plata ");
            return null;
        }

        storage.spend(MINER_COST);

        ResourceCollector miner = new Miner(
                "Miner" + System.nanoTime(),
                townHall.getTerritory(),
                10
        );

        townHall.addResourceCollector(miner);

        System.out.println("✔ Minero creado");
        return miner;
    }
}
