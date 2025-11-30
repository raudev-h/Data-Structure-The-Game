package dominion.model.buildings;

import dominion.model.territories.Territory;

public class House extends Building{
    private static final int POPULATION_BONUS = 5;

    public House(String id, Territory territory, int currentHealth, TownHall ownerTownHall) {
        super(id, territory, currentHealth, ownerTownHall);
        ownerTownHall.increasePopulationCapacity(POPULATION_BONUS);
    }
}
