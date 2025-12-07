public class Tree extends NaturalResource {

    public Tree(int maxHp, int woodTotal) {
        super(maxHp, woodTotal);
    }

    @Override
    public String toString() {
        return "Tree{hp=" + currentHp + "/" + maxHp + ", woodTotal=" +
                resourceTotal + "}";
    }
}
