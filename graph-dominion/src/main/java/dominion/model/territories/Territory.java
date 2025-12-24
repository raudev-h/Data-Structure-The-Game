package dominion.model.territories;

import dominion.model.buildings.TownHall;
import dominion.model.players.Player;

public class Territory {

    //FIELDS ===========================================
    private Player owner;
    private TownHall townHall;
    private double positionX;
    private double positionY;

    public Territory(){
    }
    public void setTownHall(TownHall townHall){
        this.townHall = townHall;
    }

    public void setPlayer(Player player){
        this.owner = player;
    }

    public TownHall getTownHall() {
        return townHall;
    }

    public Player getPlayerOwner(){
        return owner;
    }

}
