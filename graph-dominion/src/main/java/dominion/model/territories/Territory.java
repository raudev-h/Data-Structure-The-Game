package dominion.model.territories;

import dominion.model.buildings.TownHall;
import dominion.model.players.Player;

public class Territory {

    //FIELDS ===========================================
    private Player owner;
    private TownHall townHall;
    private double positionX;
    private double positionY;

    //CONSTRUCTOR ====================================

    //GETTERS AND SETTERS ================================
    public TownHall getTownHall(){
        return townHall;
    }

    //METHODS ==============================================
}
