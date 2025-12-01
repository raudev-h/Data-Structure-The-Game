package main.java.dominion.model.players;

import main.java.dominion.core.Color;
import main.java.dominion.core.GameMap;
import main.java.dominion.model.territories.Territory;

import java.util.ArrayList;

public class Player {

    //FIELDS ==================================================
    private String name;
    private ArrayList<Territory> territories;
    private Color color;

    //CONSTRUCTOR ===============================================
    public Player(String name, Color color){
        this.name = name;
        this.color = color;
    }

    // GETTERS AND SETTERS ==========================================
    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }


    //METHODS ======================================================






}
