package dominion.core;

import dominion.model.territories.Territory;

import java.util.ArrayList;

public class GameMap {

    private ArrayList<Territory> territories;




    public boolean areAdjacent(Territory a, Territory b) {
        return a.getNeighbors().contains(b);
    }

}
