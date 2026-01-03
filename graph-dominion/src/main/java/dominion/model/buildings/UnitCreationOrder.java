package dominion.model.buildings;

import dominion.model.units.UnitType;

public class UnitCreationOrder {
    private final String unitId;
    private final UnitType type;
    private int remainingTime;

    public UnitCreationOrder(String id, UnitType type, int duration) {
        this.unitId = id;
        this.type = type;
        this.remainingTime = duration;
    }

    public String getUnitId() {
        return unitId;
    }

    public UnitType getType() {
        return type;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void tick(){
        if (remainingTime > 0)
            this.remainingTime--;
    }
    public boolean isComplete(){
        return remainingTime <= 0;
    }
}
