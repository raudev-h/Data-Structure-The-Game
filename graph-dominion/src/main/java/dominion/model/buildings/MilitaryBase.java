package dominion.model.buildings;

import dominion.model.resources.ResourceCollection;
import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;
import dominion.model.units.Knight;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MilitaryBase extends Building{
    private static final Map<ResourceType,Integer> KNIGHT_COST = Map.of(ResourceType.GOLD,50);
    private List<Knight> knights;

    public MilitaryBase(String id, Territory territory, int currentHealth, TownHall ownerTownHall) {
        super(id, territory, currentHealth, ownerTownHall);
        knights = new ArrayList<>();
    }

    public List<Knight> getKnights(){
        return  knights;
    }

}
