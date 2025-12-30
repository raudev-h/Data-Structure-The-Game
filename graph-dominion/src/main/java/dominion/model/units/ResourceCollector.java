package dominion.model.units;
import dominion.model.territories.Territory;

public class ResourceCollector {



    protected Territory currentTerritory;

    public Territory getCurrentTerritory() {
        return currentTerritory;
    }

    public void setCurrentTerritory(Territory territory) {
        this.currentTerritory = territory;
    }

}
