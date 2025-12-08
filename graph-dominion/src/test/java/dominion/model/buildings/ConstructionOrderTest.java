package dominion.model.buildings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ConstructionOrderTest {
    private static final BuildingType HOUSE_TEST_TYPE = BuildingType.HOUSE;
    private static final int INITIAL_TIME = 5;
    private ConstructionOrder order;

    @BeforeEach
    void setUp(){
        order = new ConstructionOrder(
                UUID.randomUUID().toString(),
                HOUSE_TEST_TYPE,
                INITIAL_TIME
        );
    }
    @Test
    void constructor_setInitialTimeAndType(){
        assertEquals(INITIAL_TIME,order.getRemainingTime(),
                "El tiempo inicial debe ser 5");
        assertEquals(HOUSE_TEST_TYPE,order.getType(),
                "El type debe ser House");
    }
    @Test
    void tick_decrementsRemainingTime(){
        order.tick();
        assertEquals(INITIAL_TIME - 1,order.getRemainingTime(),
                "El tiempo inicial debe decrementar en 1 después de un ticket");
    }
    @Test
    void isComplete_afterCompletingTime_shouldReturnTrue(){
        for(int i = 0; i < INITIAL_TIME;i++){
            order.tick();
        }
        assertTrue(order.isComplete(),
                "Debe ser True cuando el tiempo restante es 0");
    }
    @Test
    void tick_doesNotGoBelowZero(){
        for(int i = 0; i < INITIAL_TIME;i++){
            order.tick();
        }
        order.tick();
        assertEquals(0,order.getRemainingTime(),
                "El tiempo no debe ser negativo");
    }
}
