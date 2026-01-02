package dominion.model.territories;

import java.util.ArrayList;
import java.util.List;
import dominion.model.units.Unit;
import dominion.model.buildings.TownHall;
import dominion.model.players.Player;

public class Territory {

    // FIELDS ===========================================
    private Player owner;
    private TownHall townHall;
    private double positionX;
    private double positionY;

    // NUEVO: lista de vecinos
    private List<Territory> neighbors = new ArrayList<>();

    private List<Unit> units = new ArrayList<>();

    // CONSTRUCTORS =====================================
    public Territory() {
    }

    // GETTERS Y SETTERS ===============================
    public void setTownHall(TownHall townHall) {
        this.townHall = townHall;
    }

    public TownHall getTownHall() {
        return townHall;
    }

    public Player getPlayerOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public void addUnit(Unit unit) {
        units.add(unit);
    }

    public void removeUnit(Unit unit) {
        units.remove(unit);
    }

    // NUEVO: vecinos
    public List<Territory> getNeighbors() {
        return neighbors;
    }

    public void addNeighbor(Territory neighbor) {
        neighbors.add(neighbor);
    }
}