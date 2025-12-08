package dominion.model.buildings;

public class ConstructionOrder {
    private final String buildingId;
    private final BuildingType type;
    private int remainingTime;

    public ConstructionOrder(String id, BuildingType type, int duration) {
        this.buildingId = id;
        this.type = type;
        this.remainingTime = duration;
    }

    public String getBuildingId() {
        return buildingId;
    }

    public BuildingType getType() {
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
