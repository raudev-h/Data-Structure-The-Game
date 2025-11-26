package main.java.dominion.core;

public class GameTimer {

    //FIELDS
    private long TotalGameTime;
    private boolean isRunning;
    private long pausedTime;

    //CONSTRUCTOR
    public GameTimer(){
        TotalGameTime = 0;
        isRunning = false;
        pausedTime  = 0;
    }

}

