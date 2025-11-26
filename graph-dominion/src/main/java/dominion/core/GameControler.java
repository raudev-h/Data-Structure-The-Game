package main.java.dominion.core;

import main.java.dominion.model.players.Player;

import java.util.ArrayList;

public class GameControler {

    //FIELDS

    private GameTimer gametimer;
    private GameMap gameMap;
    private ArrayList<Player> players;
    private boolean gameRunning;

    //TODO:CONSTRUCTOR

    //METHODS
    
    //Clock Methods

    public boolean pauseClock(){
        return gametimer.pause();
    }

    public boolean startClock(){
        return gametimer.start();
    }

    public long getElapsedSeconds(){
        return gametimer.getElapsedSeconds();
    }

}
