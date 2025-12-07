public class Woodcutter extends Worker {

    public Woodcutter(int damagePerSecond) {
        super(damagePerSecond);
    }

    @Override
    public int workOneSecond() {
        if (target instanceof Tree) {
            return super.workOneSecond();
        }

        unassign();
        return 0;
    }
}
