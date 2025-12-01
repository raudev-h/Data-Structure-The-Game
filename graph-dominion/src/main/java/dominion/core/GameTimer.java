package main.java.dominion.core;

public class GameTimer {

    //FIELDS

    private long totalGameTime; // tiempo jugados (sin contar pausas)
    private boolean isRunning;
    private long startTime;  // tiempo del último arranque (marcador)

    //CONSTRUCTOR

    public GameTimer(){
        totalGameTime = 0;
        isRunning = false;
        startTime  = 0;
    }

    //GETTERS AND SETTERS
    public long getStartTime(){
        return startTime;
    }

    public boolean getIsRunning(){
        return isRunning;
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

    public void reset(){
        isRunning = false;
        totalGameTime = 0;
    }

    public String secondToHour(long TotalSeconds) {
        long hours = TotalSeconds / 3600;
        long minutes = (TotalSeconds % 3600) / 60;
        long seconds = TotalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }


}

