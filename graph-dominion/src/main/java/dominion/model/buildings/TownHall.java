package dominion.model.buildings;

import dominion.model.resources.ResourceCollection;
import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;
import dominion.model.units.Miner;
import dominion.model.units.ResourceCollector;
import dominion.model.units.WoodCutter;

import javax.sound.midi.Soundbank;
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
    private List<ResourceCollector> resourceCollectors;
    private final Deque<ConstructionOrder> constructionQueue;


    public TownHall(String id, Territory territory, int currentHealth,
                    int workerCreationTime) {
        this.id = id;
        this.territory = territory;
        this.territory.setTownHall(this);
        this.currentHealth = currentHealth;
        this.storedResources = new ResourceCollection();
        this.maxPopulationCapacity = INITIAL_CAPACITY;
        this.currentPopulation = 0; // después podemos ajustar esto
        this.ownedBuildings = new ArrayList<>();
        this.constructionQueue = new ArrayDeque<>();
        this.resourceCollectors = new ArrayList<>();
    }
    // GETTERS AND SETTERS

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



    //METHODS

    public void increasePopulationCapacity(int amount){
        if (amount > 0) {
            this.maxPopulationCapacity += amount;
        }
    }

    public ArrayList<MilitaryBase> getMilitaryBases(){
        ArrayList<MilitaryBase> militaryBases = new ArrayList<>();

        for(Building b: ownedBuildings){
            if(b instanceof MilitaryBase militaryBase){
                militaryBases.add(militaryBase);
            }
        }

        return militaryBases;
    }

    public ArrayList<House> getHouses(){
        ArrayList<House> houses = new ArrayList<>();

        for(Building b: ownedBuildings){
            if(b instanceof House house){
                houses.add(house);
            }
        }

        return houses;
    }

    public ArrayList<WoodCutter> getWoodCutters(){
        ArrayList<WoodCutter> woodCutters = new ArrayList<>();

        for( ResourceCollector rc: resourceCollectors){
            if(rc instanceof WoodCutter wc){
                woodCutters.add(wc);
            }
        }

        return woodCutters;
    }

    public ArrayList<Miner> getMiners(){
        ArrayList<Miner> miners = new ArrayList<>();

        for( ResourceCollector rc: resourceCollectors){
            if(rc instanceof Miner m){
                miners.add(m);
            }
        }

        return miners;
    }

    public int getTotalEffectiveDefenceBases(){
        int total = 0;
        for(MilitaryBase mb: getMilitaryBases()){
            total += mb.getTotalEffectiveDefenceKnights();

        }
        return total;
    }

    public int eliminateKnightsAndGetRemainingBases(int amount){
        int toEliminate = amount;
        for(int i = 0 ; i < getMilitaryBases().size() && toEliminate != 0; i++ ){
            toEliminate  = getMilitaryBases().get(i).removeKnightsAndGetRemaining(toEliminate);

        }
        return toEliminate;
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

    private boolean canStartBuildingCreation(BuildingType type,Map<ResourceType,Integer> cost,int buildTime){
        if(getStoredResources().canAfford(cost)){
            return true;
        }
        return false;
    }

    public boolean createHouse(){
        final Map<ResourceType,Integer> HOUSE_COST = Map.of(ResourceType.WOOD,60);
        final int HOUSE_BUILD_TIME = 30;
        return startBuildingCreation(BuildingType.HOUSE,HOUSE_COST,HOUSE_BUILD_TIME);
    }
    public boolean canCreateHouse(){
        final Map<ResourceType,Integer> HOUSE_COST = Map.of(ResourceType.WOOD,60);
        final int HOUSE_BUILD_TIME = 30;
        return canStartBuildingCreation(BuildingType.HOUSE,HOUSE_COST,HOUSE_BUILD_TIME);
    }
    public boolean createMilitaryBase(){
        final Map<ResourceType,Integer> MILITARY_BASE_COST = Map.of(ResourceType.WOOD,100);
        final int MILITARY_BASE_BUILD_TIME = 1;
        return startBuildingCreation(BuildingType.MILITARY_BASE,MILITARY_BASE_COST,MILITARY_BASE_BUILD_TIME);
    }
    public void addMilitaryBase(Territory territory){
        ownedBuildings.add(new MilitaryBase("1",territory,10));
    }

    public boolean canCreateMilitaryBase(){
        final Map<ResourceType,Integer> MILITARY_BASE_COST = Map.of(ResourceType.WOOD,100);
        final int MILITARY_BASE_BUILD_TIME = 50;
        return canStartBuildingCreation(BuildingType.MILITARY_BASE,MILITARY_BASE_COST,MILITARY_BASE_BUILD_TIME);
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

    public MilitaryBase getMilitaryBase(String id){
        MilitaryBase militaryBase = null;
        for(Building b: ownedBuildings){
            if(b instanceof MilitaryBase){
                MilitaryBase mb = (MilitaryBase) b;
                if(mb.getId().equalsIgnoreCase(id)){
                    militaryBase = mb;
                }
            }
        }
        return militaryBase;

    }

    public void createUnit(String type){
        if(type.equalsIgnoreCase("minero"))
            resourceCollectors.add(new Miner());
        else if(type.equalsIgnoreCase("leñador"))
            resourceCollectors.add(new WoodCutter());
    }






}
