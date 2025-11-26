package main.java.dominion.core;

public class GameTimer {

    //FIELDS

    private long totalGameTime;
    private boolean isRunning;
    private long startTime;

    //CONSTRUCTOR

    public GameTimer(){
        totalGameTime = 0;
        isRunning = false;
        startTime  = 0;
    }

    //METHODS

    //Marcar el marcador en el inicio de juego o despues de una pausa
    public boolean start(){
        if(!isRunning){
            isRunning = true;
            startTime = System.currentTimeMillis();
        }
        return isRunning;
    }

    /* Devolver el tiempo transcurridp desde que el usuario comenzó el juego o salió
    de la pantalla de pausa + el tiempo juegado anteriormente */
    public long getElapsedSeconds(){
        long t = totalGameTime;
        if(isRunning){
            t += System.currentTimeMillis() - startTime;

        }
        return t/1_000;
    }

    //Pausar el juego y agregar el tiempo jugado transcurrido al total
    public boolean pause(){
        boolean pausado = false;
        if(isRunning){
            pausado = true;
            isRunning = false;
            totalGameTime += System.currentTimeMillis() - startTime;
        }
        return pausado;
    }


}

