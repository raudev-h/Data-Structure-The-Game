package dominion.model.buildings;

import dominion.model.resources.ResourceCollection;
import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TownHall {
    private final String id;
    private final Territory territory;
    private int currentHealth;
    private ResourceCollection storedResources;
    private int workerCreationTime;
    private int maxPopulationCapacity;
    private int currentPopulation;
    private List<Building> ownedBuildings;
    private int level;


    public TownHall(String id, Territory territory, int currentHealth,
                    int initialCapacity, int workerCreationTime) {
        this.id = id;
        this.territory = territory;
        this.territory.setTownHall(this);
        this.currentHealth = currentHealth;
        this.storedResources = new ResourceCollection();
        this.maxPopulationCapacity = initialCapacity;
        this.workerCreationTime = workerCreationTime;
        this.currentPopulation = 0; // después podemos ajustar esto
        this.ownedBuildings = new ArrayList<>();
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

    public String getId() {
        return id;
    }

    public Territory getTerritory() {
        return territory;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public List<Building> getOwnedBuildings() {
        return ownedBuildings;
    }

    public int getLevel() {
        return level;
    }

    public void increasePopulationCapacity(int amount){
        if (amount > 0) {
            this.maxPopulationCapacity += amount;
        }
    }
    public void createHouse(){
        final Map<ResourceType,Integer> HOUSE_COST = Map.of(ResourceType.WOOD,60);

        if(getStoredResources().canAfford(HOUSE_COST)){
            storedResources.spend(HOUSE_COST);
            House house = new House("sad",this.territory,100);
            ownedBuildings.add(house);
        }
    }
}
