package dominion.model.buildings;

import dominion.model.resources.ResourceCollection;
import dominion.model.territories.Territory;

public class TownHall extends Building{
    private ResourceCollection storedResources;
    private int workerCreationTime;
    private int maxPopulationCapacity;
    private int currentPopulation;


    public TownHall(String id, Territory territory, int currentHealth,
                    int initialCapacity, int creationTime) {
        super(id, territory, currentHealth);
        this.storedResources = new ResourceCollection();
        this.maxPopulationCapacity = initialCapacity;
        this.workerCreationTime = creationTime;
        this.currentPopulation = 0; // después podemos ajustar esto
    }

    public ResourceCollection getStoredResources() {
        return storedResources;
    }

    public int getWorkerCreationTime() {
        return workerCreationTime;
    }

    public int getMaxPopulationCapacity() {
        return maxPopulationCapacity;
    }

    public int getCurrentPopulation() {
        return currentPopulation;
    }
}
