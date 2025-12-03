package dominion.model.buildings;

import dominion.model.resources.ResourceCollection;
import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;

import java.util.*;

public class TownHall {
    private static final int INITIAL_CAPACITY = 5;
    private static final int BUILDINGS_HEALTH = 100;
    private final String id;
    private final Territory territory;
    private int currentHealth;
    private ResourceCollection storedResources;
    private int workerCreationTime;
    private int maxPopulationCapacity;
    private int currentPopulation;
    private List<Building> ownedBuildings;
    private int level;
    private final Deque<ConstructionOrder> constructionQueue;


    public TownHall(String id, Territory territory, int currentHealth,
                    int workerCreationTime) {
        this.id = id;
        this.territory = territory;
        this.territory.setTownHall(this);
        this.currentHealth = currentHealth;
        this.storedResources = new ResourceCollection();
        this.maxPopulationCapacity = INITIAL_CAPACITY;
        this.workerCreationTime = workerCreationTime;
        this.currentPopulation = 0; // después podemos ajustar esto
        this.ownedBuildings = new ArrayList<>();
        this.constructionQueue = new ArrayDeque<>();
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

    public Deque<ConstructionOrder> getConstructionQueue() {
        return constructionQueue;
    }

    public void increasePopulationCapacity(int amount){
        if (amount > 0) {
            this.maxPopulationCapacity += amount;
        }
    }
    private boolean startBuildingCreation(BuildingType type,Map<ResourceType,Integer> cost,int buildTime){
        if(getStoredResources().canAfford(cost)){
            storedResources.spend(cost);
            ConstructionOrder order = new ConstructionOrder(
                    UUID.randomUUID().toString(),
                    type,
                    buildTime
            );
            constructionQueue.add(order);
            return true;
        }
        return false;
    }
    public boolean createHouse(){
        final Map<ResourceType,Integer> HOUSE_COST = Map.of(ResourceType.WOOD,60);
        final int HOUSE_BUILD_TIME = 30;
        return startBuildingCreation(BuildingType.HOUSE,HOUSE_COST,HOUSE_BUILD_TIME);
    }
    public boolean createMilitaryBase(){
        final Map<ResourceType,Integer> MILITARY_BASE_COST = Map.of(ResourceType.WOOD,100);
        final int MILITARY_BASE_BUILD_TIME = 50;
        return startBuildingCreation(BuildingType.MILITARY_BASE,MILITARY_BASE_COST,MILITARY_BASE_BUILD_TIME);
    }
    public void processConstructionQueue(){
        ConstructionOrder currentOrder = constructionQueue.peek();
        if (currentOrder != null){
            currentOrder.tick();
            if (currentOrder.isComplete()){
                completeConstruction(currentOrder);
                constructionQueue.poll();
            }
        }
    }
    public void completeConstruction(ConstructionOrder order){
        Building newBuilding = null;
        switch (order.getType()){
            case HOUSE -> newBuilding = new House(
                    order.getBuildingId(),
                    this.territory,
                    BUILDINGS_HEALTH
            );
            case MILITARY_BASE -> newBuilding = new MilitaryBase(
                    order.getBuildingId(),
                    this.territory,
                    BUILDINGS_HEALTH
            );
            default -> { return;}
        }
        this.ownedBuildings.add(newBuilding);
    }
}
