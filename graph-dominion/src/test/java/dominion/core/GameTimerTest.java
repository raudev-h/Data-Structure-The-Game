package java.dominion.core;

import dominion.core.GameTimer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameTimerTest {

    @Test
    public void testStart() {
        GameTimer timer = new GameTimer();
        boolean result = timer.start();
        assertTrue(result);
    }

    @Test
    public void testStartBehavior() throws InterruptedException {
        GameTimer timer = new GameTimer();

        // Antes de start

        assertFalse(timer.getIsRunning());
        assertEquals(0,timer.getStartTime());
        assertEquals(0, timer.getElapsedSeconds());

        // Después de start
        timer.start();
        Thread.sleep(2000);
        assertTrue(timer.getIsRunning());
        assertTrue(timer.getElapsedSeconds() > 0);


    }

    @Test
    public void testPauseWhenNotRunning() {
        GameTimer timer = new GameTimer();
        boolean result = timer.pause();
        assertFalse(result);
    }

    @Test
    public void testPauseWhenRunning() {
        GameTimer timer = new GameTimer();
        timer.start();
        boolean result = timer.pause();
        assertTrue(result);
    }

    @Test
    public void testPassedTime() throws InterruptedException {
        GameTimer timer = new GameTimer();
        timer.start();

        Thread.sleep(2000);
        assertEquals("00:00:02",timer.secondToHour(timer.getElapsedSeconds()));
        
    }




}