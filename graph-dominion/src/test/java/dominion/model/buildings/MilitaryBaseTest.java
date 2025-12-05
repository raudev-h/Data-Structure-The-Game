package dominion.model.buildings;

import dominion.model.units.Knight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MilitaryBaseTest {

    private Knight knight1;
    private Knight knight2;
    private Knight knight3;
    private MilitaryBase mb;

    @BeforeEach
    void addKnights(){

        mb = new MilitaryBase("MB-01", null, 50);
        Knight knight1 = new Knight(100, 8, 3, "K-001", null, null);
        Knight knight2 = new Knight(100, 8, 3, "K-001", null, null);
        Knight knight3 = new Knight(100, 8, 3, "K-001", null, null);
        mb.getKnights().add(knight1);
        mb.getKnights().add(knight2);
        mb.getKnights().add(knight3);
    }

    @Test
    @DisplayName("total effective defense of three full-health knights is 324")
    public void getTotalEffectiveDefenseTest(){
        assertEquals(324, mb.getTotalEffectiveDefenceKnights());

    }
}
