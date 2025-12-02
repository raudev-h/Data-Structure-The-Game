package dominion.model.buildings;

import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;

import java.util.Map;

public class MilitaryBase extends Building{
    private static final Map<ResourceType,Integer> KNIGHT_COST = Map.of(ResourceType.GOLD,50);

    public MilitaryBase(String id, Territory territory, int currentHealth) {
        super(id, territory, currentHealth);
    }

}
