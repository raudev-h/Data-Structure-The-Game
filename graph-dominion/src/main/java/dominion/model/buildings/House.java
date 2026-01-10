package dominion.model.buildings;

import dominion.model.territories.Territory;

public class House extends Building{
    private static final int POPULATION_BONUS = 5;

    public House(String id, Territory territory, int currentHealth) {
        super(id, territory, currentHealth);
        territory.getTownHall().increasePopulationCapacity(POPULATION_BONUS);
    }
}
