public class Tree extends NaturalStructure {

    private int woodValue; // madera total que contiene

    public Tree(int maxHP, int woodValue) {
        super(maxHP);
        this.woodValue = woodValue;
    }

    @Override
    public int extractResource(int damageDone) {
        double ratio = (double) woodValue / maxHP;
        return (int)(damageDone * ratio);
    }

    public int getWoodValue() {
        return woodValue;
    }
}
