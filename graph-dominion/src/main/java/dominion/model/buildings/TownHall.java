package dominion.model.buildings;

import dominion.model.resources.ResourceCollection;
import dominion.model.territories.Territory;
import dominion.model.units.Miner;
import dominion.model.units.ResourceCollector;
import dominion.model.units.WoodCutter;

import java.util.ArrayList;
import java.util.List;

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
    private List<ResourceCollector> resourceCollectors;


    public TownHall(String id, Territory territory, int currentHealth,
                    int initialCapacity, int creationTime) {
        this.id = id;
        this.territory = territory;
        this.currentHealth = currentHealth;
        this.storedResources = new ResourceCollection();
        this.maxPopulationCapacity = initialCapacity;
        this.workerCreationTime = creationTime;
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

    public ArrayList<MilitaryBase> getMilitaryBases(){
        ArrayList<MilitaryBase> militaryBases = new ArrayList<>();

        for(Building b: ownedBuildings){
            if(b instanceof MilitaryBase militaryBase){
                militaryBases.add(militaryBase);
            }
        }

        return militaryBases;
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
        ArrayList<Miner> woodCutters = new ArrayList<>();

        for( ResourceCollector rc: resourceCollectors){
            if(rc instanceof Miner m){
                woodCutters.add(m);
            }
        }

        return woodCutters;
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

}
