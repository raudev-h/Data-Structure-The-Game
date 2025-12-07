public class Mine extends NaturalResource {

    public Mine(int maxHp, int goldTotal) {
        super(maxHp, goldTotal);
    }

    @Override
    public String toString() {
        return "Mine{hp=" + currentHp + "/" + maxHp + ", goldTotal=" + resourceTotal + "}";
    }
}
