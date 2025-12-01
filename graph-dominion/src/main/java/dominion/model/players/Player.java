package dominion.model.players;


import dominion.model.buildings.MilitaryBase;
import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;
import dominion.core.COLOR;
import dominion.model.units.Knight;

import java.util.ArrayList;
import java.util.List;

public class Player {

    //FIELDS ==================================================
    private String name;
    private ArrayList<Territory> territories;
    private COLOR color;

    //CONSTRUCTOR ===============================================
    public Player(String name, COLOR color){
        this.name = name;
        this.color = color;
        territories = new ArrayList<>();
    }

    // GETTERS AND SETTERS ==========================================
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }

    public COLOR getColor(){
        return color;
    }
    public void setColor(COLOR color){
        this.color = color;
    }

    public ArrayList<Territory> getTerritories(){
        return territories;
    }


    //METHODS ======================================================

    //Resources
    public int getTotalWoodAmount(){
        int totalWood = 0;

        for(Territory t: territories) {
            totalWood += t.getTownHall().getStoredResources().getAmount(ResourceType.WOOD);
        }

        return totalWood;
    }

    public int getTotalGold(){
        int totalGold = 0;

        for(Territory t: territories) {
            totalGold += t.getTownHall().getStoredResources().getAmount(ResourceType.GOLD);
        }

        return totalGold;
    }

    //Military
    public int getKnightAmount(){
        int totalKnights = 0;

        for(Territory t: territories){
            List<MilitaryBase> militaryBases = t.getTownHall().getMilitaryBases();
            for(MilitaryBase mb: militaryBases){
                totalKnights += mb.getKnights().size();
            }
        }

        return totalKnights;
    }

    public int calculateAttackForce(){
        return getKnightAmount() * Knight.getAttackdamage();
    }











}
