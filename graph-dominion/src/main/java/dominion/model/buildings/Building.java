package dominion.model.buildings;

import dominion.model.territories.Territory;

public abstract class Building {
    protected String id;
    protected Territory territory;
    protected int currentHealth;

    public Building(String id, Territory territory, int currentHealth) {
        this.id = id;
        this.territory = territory;
        this.currentHealth = currentHealth;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Territory getTerritory() {
        return territory;
    }

    public void setTerritory(Territory territory) {
        this.territory = territory;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }
}
