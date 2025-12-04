package dominion.model.territories;

import dominion.model.buildings.TownHall;
import dominion.model.players.Player;

public class Territory {

    private Player owner;
    private TownHall townHall;
    private double positionX;
    private double positionY;

    public Territory(){
    }
    public void setTownHall(TownHall townHall){
        this.townHall = townHall;
    }
    public TownHall getTownHall() {
        return townHall;
    }
}
