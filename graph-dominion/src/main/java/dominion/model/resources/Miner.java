package dominion.model.resources;
public class Miner extends Worker {

    public Miner(int damagePerSecond) {
        super(damagePerSecond);
    }

    @Override
    public int workOneSecond() {
        if (target instanceof Mine) {
            return super.workOneSecond();
        }
        System.out.print("el leñador no pica");
        unassign();
        return 0;
    }
}
