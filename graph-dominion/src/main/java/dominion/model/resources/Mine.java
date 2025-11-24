public class Mine extends NaturalStructure {

    private int goldValue;

    public Mine(int maxHP, int goldValue) {
        super(maxHP);
        this.goldValue = goldValue;
    }

    @Override
    public int extractResource(int damageDone) {
        double ratio = (double) goldValue / maxHP;
        return (int)(damageDone * ratio);
    }

    public int getGoldValue() {
        return goldValue;
    }
}
