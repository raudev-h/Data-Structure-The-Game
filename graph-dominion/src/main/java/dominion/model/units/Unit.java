package dominion.model.units;
import dominion.model.territories.Territory;



public abstract class Unit {

    protected double x, y;
    protected double targetX, targetY;
    public boolean selected;

    public void moveTo(double x, double y) {
        targetX = x;
        targetY = y;
    }

    public void update(double dt) {
        double speed = 120;

        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 1) {
            x += (dx / dist) * speed * dt;
            y += (dy / dist) * speed * dt;
        }
    }

    protected Territory currentTerritory;

    public Territory getCurrentTerritory() {
        return currentTerritory;
    }

    public void setCurrentTerritory(Territory territory) {
        this.currentTerritory = territory;
    }


}