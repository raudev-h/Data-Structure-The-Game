package dominion.model.buildings;
import dominion.model.territories.Territory;

public abstract class Building {
    protected final String id;
    protected final Territory territory;
    protected int currentHealth;

    public Building(String id, Territory territory, int currentHealth) {
        this.id = id;
        this.territory = territory;
        this.currentHealth = currentHealth;
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

    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }

}
