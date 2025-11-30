package dominion.model.resources;
import dominion.model.resources.ResourceCollection;
import dominion.model.resources.ResourceType;
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
}
