package dominion.model.buildings;

import dominion.model.units.Knight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestMilitaryBase {

    private Knight knight1;
    private Knight knight2;
    private Knight knight3;
    private MilitaryBase mb;

    @BeforeEach
    void addKnights(){

        mb = new MilitaryBase("MB-01", null, 50, null);

        mb.addKnight(100, 8, 3, "K-001", null, null);
        mb.addKnight(100, 8, 3, "K-002", null, null);
        mb.addKnight(100, 8, 3, "K-003", null, null);
    }

    @Test
    @DisplayName("total effective defense of three full-health knights is 324")
    public void getTotalEffectiveDefenseTest(){
        assertEquals(324, mb.getTotalEffectiveDefenceKnights());

    }
}
