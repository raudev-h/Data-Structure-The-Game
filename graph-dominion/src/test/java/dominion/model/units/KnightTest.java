package dominion.model.units;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class KnightTest {

    private Knight knight;

    @BeforeEach
    void setKnight(){
        knight = new Knight(100,      // maxHP
                8,        // armorDefense
                3,        // movementSpeed (casillas por turno)
                "K-001",  // id
                null,       // ownerTownHall
                null     // initialLocation
        );
    }

    @Test
    public void testGetEffectiveDefense(){

        assertEquals(108, knight.getEffectiveDefense());
    }
}
