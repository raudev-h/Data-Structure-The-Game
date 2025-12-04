package dominion.model.buildings;

import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;
import dominion.model.units.Knight;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MilitaryBase extends Building{
    private static final Map<ResourceType,Integer> KNIGHT_COST = Map.of(ResourceType.GOLD,50);
    private List<Knight> knights;

    public MilitaryBase(String id, Territory territory, int currentHealth) {
        super(id, territory, currentHealth);
        knights = new ArrayList<>();
    }

    public List<Knight> getKnights(){
        return  knights;
    }

    public void addKnight(int maxHP, int defense, int movementSpeed,
                          String id, TownHall owner, Territory initialLocation){
        knights.add(new Knight(maxHP,defense,movementSpeed,id,owner,initialLocation));
    }

    public int getTotalEffectiveDefenceKnights(){
        int total = 0;
        for(Knight k: knights){
            total += k.getEffectiveDefense();
        }

        return total;
    }

    public void removeAllKnights(){
        knights.clear();
    }

    //Eliminar caballeros y devolver el resto faltante a eliminar
    public int removeKnightsAndGetRemaining(int amount){
        int eliminated = 0;

       Iterator<Knight> it = knights.iterator();
       while(it.hasNext() && eliminated != amount){
           Knight k = it.next();
           it.remove();
           eliminated ++;
       }


        return amount - eliminated;
    }


}
