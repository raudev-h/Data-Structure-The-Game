package dominion.model.units;

import dominion.model.buildings.TownHall;
import dominion.model.territories.Territory;

public class Knight {
    private final int maxHealth;
    private final int attackDamage;
    private final int armorDefense;
    private final int movementSpeed;
    private final String id;
    private final TownHall ownerTownHall;
    private int currentHealth;
    private Territory location;

    public Knight(int maxHP, int attack, int defense, int movementSpeed,
                  String id, TownHall owner, Territory initialLocation) {
        this.maxHealth = maxHP;
        this.attackDamage = attack;
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
        return attackDamage;
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
}
