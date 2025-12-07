public abstract class NaturalResource {
    protected final int maxHp;
    protected int currentHp;
    protected final int resourceTotal;

    public NaturalResource(int maxHp, int resourceTotal) {
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.resourceTotal = resourceTotal;
    }

    public int receiveDamage(int damage) {
        if (isDepleted() || damage <= 0) return 0;
        int applied = Math.min(damage, currentHp);
        currentHp -= applied;
        return applied;
    }

    public boolean isDepleted() {
        return currentHp <= 0;
    }

    public int getResourceTotalOnDepletion() {
        return resourceTotal;
    }
}
