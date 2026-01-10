package dominion.model.buildings;

import dominion.model.units.UnitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnitCreationOrderTest {
    private static final UnitType KNIGHT_TEST_TYPE = UnitType.KNIGHT;
    private static final int INITIAL_TIME = 5;
    private UnitCreationOrder order;

    @BeforeEach
    void setUp(){
        order = new UnitCreationOrder(
                UUID.randomUUID().toString(),
                KNIGHT_TEST_TYPE,
                INITIAL_TIME);
    }
    @Test
    void constructor_setInitialTimeAndType(){
        assertEquals(INITIAL_TIME,order.getRemainingTime(),
                "El tiempo inicial debe ser 5");
        assertEquals(KNIGHT_TEST_TYPE,order.getType(),
                "El type debe ser Knight");
    }
    void tick_decrementsRemainingTime(){
        order.tick();
        assertEquals(4,order.getRemainingTime(),
                "El tiempo inicial debe decrementar en 1");
    }
    @Test
    void isComplete_afterCompletingTime_shouldReturnTrue(){
        for(int i = 0; i < INITIAL_TIME; i++){
            order.tick();
        }
        assertTrue(order.isComplete(),"" +
                "Debe ser true cuando el tiempo restante es 0");
    }
    @Test
    void tick_doesNotGoBelowZero(){
        for(int i = 0; i < INITIAL_TIME; i++){
            order.tick();
        }
        order.tick();
        assertEquals(0,order.getRemainingTime(),
                "El tiempo no debe ser negativo");
    }
}
