package dominion.model.buildings;

import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;
import dominion.model.units.Knight;
import dominion.model.units.UnitType;
import dominion.model.units.UnitType.*;

import java.util.*;

public class MilitaryBase extends Building{
    private final int KNIGHT_MAX_HEALTH = 100;
    private final int KNIGHT_MOVEMENT_SPEED = 20;
    private final int KNIGHT_DEFENSE = 50;
    private List<Knight> knights;
    private final Deque<UnitCreationOrder> trainingQueue;

    public MilitaryBase(String id, Territory territory, int currentHealth) {
        super(id, territory, currentHealth);
        knights = new ArrayList<>();
        trainingQueue = new ArrayDeque<>();
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
    private boolean startUnitCreation(UnitType type,Map<ResourceType,Integer> cost, int creationTime){
        if (territory.getTownHall().getStoredResources().canAfford(cost)){
            territory.getTownHall().getStoredResources().spend(cost);
            UnitCreationOrder order = new UnitCreationOrder(
                    UUID.randomUUID().toString(),
                    type,
                    creationTime
            );
            trainingQueue.add(order);
            return true;
        }
        return false;
    }
    public boolean createKnight(){
        final Map<ResourceType,Integer> KNIGHT_COST = Map.of(ResourceType.GOLD,50);
        final int KNIGHT_TRAINING_TIME = 40;
        return startUnitCreation(UnitType.KNIGHT,KNIGHT_COST,KNIGHT_TRAINING_TIME);
    }
    public void processTrainingQueue(){
        UnitCreationOrder currentOrder = trainingQueue.peek();
        if (currentOrder != null){
            currentOrder.tick();
            if(currentOrder.isComplete()){
                completeTraining(currentOrder);
                trainingQueue.poll();
            }
        }
    }
    public void completeTraining(UnitCreationOrder order){
        Knight newKnight = null;
        switch (order.getType()){
            case KNIGHT -> newKnight = new Knight(
                    KNIGHT_MAX_HEALTH,
                    KNIGHT_DEFENSE,
                    KNIGHT_MOVEMENT_SPEED,
                    order.getUnitId(),
                    this.territory.getTownHall(),
                    this.territory
            );
            default -> {return;}
        }
        this.knights.add(newKnight);
    }
}
