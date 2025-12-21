package dominion.model.buildings;

import dominion.model.resources.ResourceCollection;
import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;
import dominion.model.units.Knight;
import dominion.model.units.UnitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MilitaryBaseTest {
    private MilitaryBase mb;
    private Territory territory;
    private TownHall townHall;

    @BeforeEach
    void setUp() {
        territory = new Territory();
        townHall = new TownHall("", territory, 100, 10);
        mb = new MilitaryBase("MB-01", territory, 50);

        Knight knight1 = new Knight(100, 8, 3, "K-001", null, null);
        Knight knight2 = new Knight(100, 8, 3, "K-002", null, null);
        Knight knight3 = new Knight(100, 8, 3, "K-003", null, null);
    }

    @Test
    @DisplayName("total effective defense of three full-health knights is 324")
    public void getTotalEffectiveDefense() {

        mb.getKnights().add(knight1);
        mb.getKnights().add(knight2);
        mb.getKnights().add(knight3);

        assertEquals(324, mb.getTotalEffectiveDefenceKnights());

    }

    @Test
    void createKnight_sufficientResources_startsOrderAndSpendsCost() {
        townHall.getStoredResources().addResource(ResourceType.GOLD, 80);
        boolean success = mb.createKnight();

        assertTrue(success, "Debe devolver True cuando los recursos son suficientes");

        assertEquals(1, mb.getTrainingQueue().size(),
                "Debe haber 1 orden en la cola de entrenamiento.");

        assertEquals(0, townHall.getStoredResources().getAmount(ResourceType.WOOD));
    }

    @Test
    void createKnight_insufficientResources_returnsFalseAndDoesNothing() {
        mb.getTerritory().getTownHall().getStoredResources().addResource(ResourceType.GOLD, 79);
        ;
        int initialGold = townHall.getStoredResources().getAmount(ResourceType.GOLD);
        boolean success = mb.createKnight();

        assertFalse(success, "El método debe devolver False si los recursos son insuficientes.");

        assertEquals(initialGold, townHall.getStoredResources().getAmount(ResourceType.GOLD),
                "El saldo de oro no debe cambiar.");

        assertEquals(0, mb.getTrainingQueue().size(),
                "No se debe iniciar ninguna orden de entrenamiento.");
    }

    @Test
    void completeTraining_KnightType_createsKnightAndIncreaseKnights() {
        UnitCreationOrder order = new UnitCreationOrder("", UnitType.KNIGHT, 0);
        mb.completeTraining(order);

        assertEquals(1, mb.getKnights().size(),
                "Debe haber un caballero creado");
        assertInstanceOf(Knight.class, mb.getKnights().get(0),
                "Debe ser un caballero el que esté en la lista");
    }
    @Test
    void completeTraining_unrecognizedType_doesNothingAndReturn(){
        int initialUnits = mb.getKnights().size();
        UnitCreationOrder order = new UnitCreationOrder("unknow",UnitType.ARCHER,0);
        mb.completeTraining(order);
        assertEquals(0, mb.getKnights().size(),
                "No se debe crear ni registrar ninguna tropa.");

        assertEquals(initialUnits, mb.getKnights().size(),
                "La cantidad de caballeros debe permanecer sin cambios.");
    }
    @Test
    void completeTraining_idAndTerritoryArePassedCorrectly(){
        String uniqueId = UUID.randomUUID().toString();
        UnitCreationOrder order = new UnitCreationOrder(uniqueId,UnitType.KNIGHT,0);
        mb.completeTraining(order);
        Knight newKnight = mb.getKnights().get(0);

        assertEquals(uniqueId,newKnight.getId(),
                "El id de knight debe ser el mismo que order");

        assertEquals(territory,newKnight.getLocation(),
                "El Territorio del caballero debe coincidir");
    }
    @Test
    void processTrainingQueue_emptyQueue_doesNothing() {
        assertDoesNotThrow(() -> mb.getTrainingQueue());
        assertEquals(0, mb.getTrainingQueue().size(), "La cola debe seguir vacía.");
    }
    @Test
    void processTrainingQueue_orderInProgress_timeDecrements() {
        int initialTime = 3;
        UnitCreationOrder order = new UnitCreationOrder("test-run", UnitType.KNIGHT, initialTime);
        mb.getTrainingQueue().add(order);

       mb.processTrainingQueue();

        assertEquals(initialTime - 1, order.getRemainingTime(),
                "El tiempo restante debe ser 2.");

        assertEquals(1, mb.getTrainingQueue().size(),
                "La orden no debe ser removida hasta que termine.");

        assertEquals(0, mb.getKnights().size(),
                "La lista de caballeros debe estar vacía.");
    }
    @Test
    void processTrainingQueue_orderFinishes_knightCreatedAndOrderRemoved() {
        int knightBeforeTraining = mb.getKnights().size();

        UnitCreationOrder finalOrder = new UnitCreationOrder("test-finish",UnitType.KNIGHT, 1);
        mb.getTrainingQueue().add(finalOrder);

        mb.processTrainingQueue();

        assertEquals(0, mb.getTrainingQueue().size(),
                "La cola de entrenamiento debe estar vacía.");

        assertEquals(1,  mb.getKnights().size(),
                "El caballero debe haber sido creado y registrado.");
    }

    @Test
    void
}
