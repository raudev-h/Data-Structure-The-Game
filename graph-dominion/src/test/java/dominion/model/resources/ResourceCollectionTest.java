package dominion.model.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;


public class ResourceCollectionTest {
    private ResourceCollection resourceCollection;

    @BeforeEach
    void setUp(){
        resourceCollection = new ResourceCollection();
    }
    @Test
    void getAmount_untrackedResource_returnsZero(){
        assertEquals(0,resourceCollection.getAmount(ResourceType.GOLD),
                "la cantidad de GOLD debe ser cero");
    }

    @Test
    void addResource_newResource_shouldInitializeAmount(){
        resourceCollection.addResource(ResourceType.WOOD,100);

        assertEquals(100,resourceCollection.getAmount(ResourceType.WOOD),
                "La cantidad inicial de WOOD debe ser 100");
    }
    @Test
    void addResource_existingResource_shouldIncreaseAmount(){
        resourceCollection.addResource(ResourceType.GOLD,50);

        resourceCollection.addResource(ResourceType.GOLD,25);

        assertEquals(75,resourceCollection.getAmount(ResourceType.GOLD),
                "La cantidad de GOLD debe ser 75");
    }
    @Test
    void addResource_zeroOrNegativeAmount_shouldDoNothing(){
        resourceCollection.addResource(ResourceType.GOLD,50);

        resourceCollection.addResource(ResourceType.GOLD,0);
        resourceCollection.addResource(ResourceType.GOLD,-10);

        assertEquals(50,resourceCollection.getAmount(ResourceType.GOLD),
                "Añadir 0 o valores negativos no debe cambiar la cantidad.");
    }
    @Test
    void canAfford_sufficientResources_shouldReturnTrue(){
        resourceCollection.addResource(ResourceType.GOLD,100);
        resourceCollection.addResource(ResourceType.WOOD,50);

    Map<ResourceType,Integer> cost = Map.of(
            ResourceType.GOLD, 80,
            ResourceType.WOOD,40
    );

    assertTrue(resourceCollection.canAfford(cost),
            "Debería devolver True porque todos los recursos son suficientes");
    }
    @Test
    void canAfford_insufficientSingleResource_shouldReturnFalse(){
        resourceCollection.addResource(ResourceType.GOLD, 100);
        resourceCollection.addResource(ResourceType.WOOD, 50);

        Map<ResourceType,Integer> cost = Map.of(
                ResourceType.GOLD,101,
                ResourceType.WOOD,40
        );

        assertFalse(resourceCollection.canAfford(cost),
                "Debe devolver FALSE porque falta 1 unidad de ORO.");
    }
    @Test
    void canAfford_requiredResourceIsNotTracked_shouldReturnFalse(){
        resourceCollection.addResource(ResourceType.GOLD,50);

        Map<ResourceType,Integer> cost = Map.of(ResourceType.WOOD,1);

        assertFalse(resourceCollection.canAfford(cost),
                "Debe devolver False si se requiere recurso no trackeado");
    }
    @Test
    void canAfford_costIsZero_shouldReturnTrue() {
        Map<ResourceType, Integer> emptyCost = Map.of();
        assertTrue(resourceCollection.canAfford(emptyCost),
                "Un mapa de costo vacío siempre debe retornar TRUE.");

        Map<ResourceType, Integer> zeroCost = Map.of(
                ResourceType.GOLD, 0,
                ResourceType.WOOD, 0
        );
        assertTrue(resourceCollection.canAfford(zeroCost),
                "Si el costo es 0 para todos, debe retornar TRUE.");
    }
    @Test
    void spend_sufficientResources_shouldDecreaseAmount(){
        resourceCollection.addResource(ResourceType.WOOD, 100);

        Map<ResourceType,Integer> cost = Map.of(
                ResourceType.WOOD,60
        );
        resourceCollection.spend(cost);
        assertEquals(40,resourceCollection.getAmount(ResourceType.WOOD),
                "Debe reducir la cantidad de madera en 60"
        );
    }
    @Test
    void spend_zeroOrNegativeAmount_shouldDoNothing(){
        resourceCollection.addResource(ResourceType.WOOD, 100);

        Map<ResourceType,Integer> costZero = Map.of(
                ResourceType.WOOD,0
        );
        Map<ResourceType,Integer> costNegative = Map.of(
                ResourceType.WOOD,-10
        );
        resourceCollection.spend(costZero);
        resourceCollection.spend(costNegative);

        assertEquals(100,resourceCollection.getAmount(ResourceType.WOOD),
                "Gastar 0 o valores negativos no debe cambiar la cantidad.");
    }
}
