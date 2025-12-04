package dominion.model.buildings;

import dominion.model.resources.ResourceType;
import dominion.model.territories.Territory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TownHallTest {
    private static final int POPULATION_BONUS = 5;
    private TownHall townHall;
    private Territory territory;

    @BeforeEach
    void setUp(){
        territory = new Territory();
        townHall = new TownHall("",territory,100, 5);
    }
    @Test
    void getInitialCapacity_shouldReturnFive(){
        assertEquals(5,townHall.getMaxPopulationCapacity(),
                "La capacidad inicial debe ser 5");
    }
    @Test
    void createHouse_sufficientResources_startsOrderAndSpendsCost() {
        townHall.getStoredResources().addResource(ResourceType.WOOD, 60);
        boolean success = townHall.createHouse();

        assertTrue(success,"Debe devolver True cuando los recursos son suficientes");

        assertEquals(1, townHall.getConstructionQueue().size(),
                "Debe haber 1 orden en la cola de construcción.");

        assertEquals(0, townHall.getStoredResources().getAmount(ResourceType.WOOD));
    }
    @Test
    void createHouse_insufficientResources_returnsFalseAndDoesNothing() {
        townHall.getStoredResources().addResource(ResourceType.WOOD, 59);
        int initialWood = townHall.getStoredResources().getAmount(ResourceType.WOOD);
        boolean success = townHall.createHouse();

        assertFalse(success, "El método debe devolver False si los recursos son insuficientes.");

        assertEquals(initialWood, townHall.getStoredResources().getAmount(ResourceType.WOOD),
                "El saldo de madera no debe cambiar.");

        assertEquals(0, townHall.getConstructionQueue().size(),
                "No se debe iniciar ninguna orden de construcción.");
    }
    @Test
    void createMilitaryBase_sufficientResources_startsOrderAndChecksRemainingResource(){
        townHall.getStoredResources().addResource(ResourceType.WOOD, 120);
        boolean success = townHall.createMilitaryBase();

        assertTrue(success,"Debe devolver True cuando los recursos son suficientes");

        assertEquals(1,townHall.getConstructionQueue().size(),
                "Debe haber 1 order en la cola de construcción");
        assertEquals(20,townHall.getStoredResources().getAmount(ResourceType.WOOD));
    }
    @Test
    void createMilitaryBase_insufficientResources_returnsFalseAndDoesNothing() {
        townHall.getStoredResources().addResource(ResourceType.WOOD, 90);
        int initialWood = townHall.getStoredResources().getAmount(ResourceType.WOOD);
        boolean success = townHall.createMilitaryBase();

        assertFalse(success, "El método debe devolver False si los recursos son insuficientes.");

        assertEquals(initialWood, townHall.getStoredResources().getAmount(ResourceType.WOOD),
                "El saldo de madera no debe cambiar.");

        assertEquals(0, townHall.getConstructionQueue().size(),
                "No se debe iniciar ninguna orden de construcción.");
    }
    @Test
    void completeConstruction_houseType_createsHouseAndIncreaseCapacity(){
        String houseId = "sad";
        int capacityBeforeConstruction = townHall.getMaxPopulationCapacity();
        ConstructionOrder order = new ConstructionOrder(houseId,BuildingType.HOUSE,0);
        townHall.completeConstruction(order);

        assertEquals(1,townHall.getOwnedBuildings().size(),
                "Debe haber un edificio en la lista");

        assertInstanceOf(House.class,townHall.getOwnedBuildings().get(0),
                "El objeto debe ser uns instancia de House");

        int expectedCapacity = capacityBeforeConstruction + POPULATION_BONUS;
        assertEquals(expectedCapacity,townHall.getMaxPopulationCapacity(),
                "La capacidad de población de TH debe aumentar en 5");
    }
    @Test
    void completeConstruction_militaryBaseType_createsMilitaryBase(){
        String mbId = "mbid";
        int initialCapacity = townHall.getMaxPopulationCapacity();
        ConstructionOrder order = new ConstructionOrder(mbId,BuildingType.MILITARY_BASE,0);
        townHall.completeConstruction(order);

        assertEquals(1,townHall.getOwnedBuildings().size(),
                "Debe haber un edificio en la lista");

        assertInstanceOf(MilitaryBase.class,townHall.getOwnedBuildings().get(0),
                "El edificio debe ser MilitaryBase");

        assertEquals(5,initialCapacity,
                "No se debe modificar la capacidad al crear una base militar");
    }
    @Test
    void completeConstruction_unrecognizedType_doesNothingAndReturn(){
        int initialCapacity = townHall.getMaxPopulationCapacity();
        ConstructionOrder order = new ConstructionOrder("unknow",BuildingType.CASTLE,0);
        townHall.completeConstruction(order);
        assertEquals(0, townHall.getOwnedBuildings().size(),
                "No se debe crear ni registrar ningún edificio.");

        assertEquals(initialCapacity, townHall.getMaxPopulationCapacity(),
                "La capacidad de población debe permanecer sin cambios.");
    }
    @Test
    void completeConstruction_idAndTerritoryArePassedCorrectly(){
        String uniqueId = UUID.randomUUID().toString();
        ConstructionOrder order = new ConstructionOrder(uniqueId,BuildingType.HOUSE,0);
        townHall.completeConstruction(order);
        Building newBuilding = townHall.getOwnedBuildings().get(0);

        assertEquals(uniqueId,newBuilding.getId(),
                "El id de building debe ser el mismo que order");

        assertEquals(territory,newBuilding.getTerritory(),
                "El Territorio del edificio debe coincidir");
    }
    @Test
    void completeConstruction_multipleTypes_addsAllCorrectly() {
        int initialCapacity = townHall.getMaxPopulationCapacity();
        ConstructionOrder houseOrder = new ConstructionOrder("h1", BuildingType.HOUSE, 0);
        ConstructionOrder mbOrder = new ConstructionOrder("mb1", BuildingType.MILITARY_BASE, 0);
        townHall.completeConstruction(houseOrder);
        townHall.completeConstruction(mbOrder);

        assertEquals(2, townHall.getOwnedBuildings().size(),
                "La lista debe contener dos edificios.");

        int expectedCapacity = initialCapacity + POPULATION_BONUS;
        assertEquals(expectedCapacity, townHall.getMaxPopulationCapacity(),
                "La capacidad debe reflejar solo el aumento de la House.");
    }
    @Test
    void processConstructionQueue_emptyQueue_doesNothing() {
        assertDoesNotThrow(() -> townHall.processConstructionQueue());
        assertEquals(0, townHall.getConstructionQueue().size(), "La cola debe seguir vacía.");
    }

    @Test
    void processConstructionQueue_orderInProgress_timeDecrements() {
        int initialTime = 3;
        ConstructionOrder order = new ConstructionOrder("test-run", BuildingType.HOUSE, initialTime);
        townHall.getConstructionQueue().add(order);

        townHall.processConstructionQueue();

        assertEquals(initialTime - 1, order.getRemainingTime(),
                "El tiempo restante debe ser 2.");

        assertEquals(1, townHall.getConstructionQueue().size(),
                "La orden no debe ser removida hasta que termine.");

        assertEquals(0, townHall.getOwnedBuildings().size(),
                "La lista de edificios debe estar vacía.");
    }

    @Test
    void processConstructionQueue_orderFinishes_buildingCreatedAndOrderRemoved() {
        int capacityBeforeConstruction = townHall.getMaxPopulationCapacity();

        ConstructionOrder finalOrder = new ConstructionOrder("test-finish", BuildingType.HOUSE, 1);
        townHall.getConstructionQueue().add(finalOrder);

        townHall.processConstructionQueue();

        assertEquals(0, townHall.getConstructionQueue().size(),
                "La cola de construcción debe estar vacía.");

        assertEquals(1, townHall.getOwnedBuildings().size(),
                "La House debe haber sido creada y registrada.");

        assertEquals(capacityBeforeConstruction + POPULATION_BONUS, townHall.getMaxPopulationCapacity(),
                "La capacidad de población debe aumentar en 5.");
    }

}
