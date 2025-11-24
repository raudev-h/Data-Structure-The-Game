public abstract class NaturalStructure {

    protected int maxHP;
    protected int currentHP;

    public NaturalStructure(int maxHP) {
        this.maxHP = maxHP;
        this.currentHP = maxHP;
    }

    // Daño recibido por mineros/leñadores
    public int receiveDamage(int damage) {
        int appliedDamage = Math.min(damage, currentHP);
        currentHP -= appliedDamage;
        return appliedDamage;
    }

    public boolean isDepleted() {
        return currentHP <= 0;
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public int getMaxHP() {
        return maxHP;
    }

    // Cada estructura sabe cómo transformar daño en recurso
    public abstract int extractResource(int damageDone);
}
