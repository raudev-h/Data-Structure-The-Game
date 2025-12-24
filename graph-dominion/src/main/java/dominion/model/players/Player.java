package dominion.model.players;


import dominion.core.AttackResult;
import dominion.model.buildings.MilitaryBase;
import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;
import dominion.core.Color;
import dominion.model.units.Knight;

import java.util.ArrayList;
import java.util.List;

public class Player {

    //FIELDS ==================================================
    private String name;
    private ArrayList<Territory> territories;
    private Color color;

    //CONSTRUCTOR ===============================================
    public Player(String name, Color color){
        this.name = name;
        this.color = color;
        this.territories = new ArrayList<>();
    }

    // GETTERS AND SETTERS ==========================================
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }

    public Color getColor(){
        return color;
    }
    public void setColor(Color color){
        this.color = color;
    }

    public ArrayList<Territory> getTerritories(){
        return territories;
    }


    //METHODS ======================================================

    //Core
    public boolean isAlive(){
        return !territories.isEmpty();
    }

    //Territories
    public boolean addTerritory(Territory t){
        boolean added = false;

        if( t != null){
            territories.add(t);
            added = true;
        }
        return added;
    }

    public boolean deleteTerritory(Territory t){
        boolean deleted = false;
        if(t != null) {
            territories.remove(t);
            deleted = true;
        }

        return deleted;
    }

    //Resources
    public int getTotalWoodAmount(){
        int totalWood = 0;

        for(Territory t: territories) {
            totalWood += t.getTownHall().getStoredResources().getAmount(ResourceType.WOOD);
        }

        return totalWood;
    }

    public int getTotalGoldAmount(){
        int totalGold = 0;

        for(Territory t: territories) {
            totalGold += t.getTownHall().getStoredResources().getAmount(ResourceType.GOLD);
        }

        return totalGold;
    }

    //Military

    public int getKnightAmount(){

        return getKnights().size();
    }


    public ArrayList<Knight> getKnights() {
        ArrayList<Knight> knights = new ArrayList<>();

        for (Territory t : territories) {
            List<MilitaryBase> militaryBases = t.getTownHall().getMilitaryBases();
            for (MilitaryBase mb : militaryBases) {
                knights.addAll(mb.getKnights());
            }

        }
        return knights;
    }

    public int calculateAttackForce(){
        return getKnightAmount() * Knight.getAttackdamage();
    }

    public int calculateTotalDefence(){
        int totalDefence = 0;

        for(Territory t: territories){
            totalDefence += t.getTownHall().getTotalEffectiveDefenceBases();

        }

        return totalDefence;
    }

    //Villagers

    public int getWoodCutterAmount(){
        int total = 0;

        for(Territory t: territories){
            total += t.getTownHall().getWoodCutters().size();

        }

        return total;
    }

    public int getMinerAmount(){
        int total = 0;

        for(Territory t: territories){
            total += t.getTownHall().getMiners().size();

        }
        return total;
    }


    public int getVillagersAmount(){
        return getMinerAmount() + getWoodCutterAmount();
    }

    //Atack
    public AttackResult attack(Territory target){
        if(attackResult(target)){

            eliminateKnigths(calculateDeadKnights(target));

            return AttackResult.VICTORY;
        }
        else{
            return AttackResult.DEFEAT;
        }

    }

    public boolean attackResult(Territory target){

        return calculateAttackForce() > target.getPlayerOwner().calculateTotalDefence();
    }

    public int calculateDeadKnights(Territory target){
        return (calculateAttackForce() - target.getPlayerOwner().calculateTotalDefence())/10;
    }



    public void eliminateKnigths(int amount){
        int toEliminate = amount;


        for(int i = 0; i < territories.size() && toEliminate != 0; i++) {
           toEliminate = territories.get(i).getTownHall().eliminateKnightsAndGetRemainingBases(toEliminate);
        }

    }













}
