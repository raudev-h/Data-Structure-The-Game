 package dominion.menu;
 import dominion.model.territories.Territory;
 import dominion.model.buildings.TownHall;

public abstract class Building {
    protected final String id;
    protected final Territory territory;
    protected int currentHealth;
    protected final TownHall ownerTownHall;

    public Building(String id, Territory territory, int currentHealth, TownHall ownerTownHall) {
        this.id = id;
        this.territory = territory;
        this.currentHealth = currentHealth;
        this.ownerTownHall = ownerTownHall;
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

    public TownHall getOwnerTownHall() {
        return ownerTownHall;
    }
}
