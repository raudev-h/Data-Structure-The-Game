package dominion.model.units;

import dominion.model.buildings.TownHall;
import dominion.model.territories.Territory;

public class Knight {
    private final int maxHealth;
    private static final int ATTACKDAMAGE = 0;
    private final int armorDefense;
    private final int movementSpeed;
    private final String id;
    private final TownHall ownerTownHall;
    private int currentHealth;
    private Territory location;

    public Knight(int maxHP, int defense, int movementSpeed,
                  String id, TownHall owner, Territory initialLocation) {
        this.maxHealth = maxHP;
        this.armorDefense = defense;
        this.movementSpeed = movementSpeed;
        this.id = id;
        this.ownerTownHall = owner;
        this.currentHealth = maxHP;
        this.location = initialLocation;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getAttackDamage() {
        return ATTACKDAMAGE;
    }

    public int getArmorDefense() {
        return armorDefense;
    }

    public int getMovementSpeed() {
        return movementSpeed;
    }

    public String getId() {
        return id;
    }

    public TownHall getOwnerTownHall() {
        return ownerTownHall;
    }

    public static int getAttackdamage(){
        return ATTACKDAMAGE;
    }

    public int getEffectiveDefense(){
        return armorDefense + currentHealth;
    }
}
