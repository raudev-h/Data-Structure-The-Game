package dominion.model.resources;

import java.util.HashMap;
import java.util.Map;

public class ResourceCollection {
    private final Map<ResourceType, Integer> storage = new HashMap<>();

    public ResourceCollection() {
    }
    public void addResource(ResourceType type, int amount) {
        if (amount > 0) {
            this.storage.put(type, this.storage.getOrDefault(type, 0) + amount);
        }
    }
    public boolean canAfford(Map<ResourceType, Integer> costMap) {
        for (Map.Entry<ResourceType, Integer> entry : costMap.entrySet()) {
            ResourceType type = entry.getKey();
            int requiredAmount = entry.getValue();

            int currentAmount = storage.getOrDefault(type, 0);

            if (currentAmount < requiredAmount) {
                return false;
            }
        }
        return true;
    }

    public int getAmount(ResourceType type) {
        return storage.getOrDefault(type, 0);
    }

    public void spend(Map<ResourceType, Integer> costMap) {
        for (Map.Entry<ResourceType, Integer> entry : costMap.entrySet()) {
            ResourceType type = entry.getKey();
            int requiredAmount = entry.getValue();

           if(requiredAmount > 0){
               storage.put(
                       type,
                       storage.getOrDefault(type,0) - requiredAmount);
           }
        }
    }

    public boolean removeResource(ResourceType type, int amount) {
        if (amount <= 0) {
            return false; // Cantidad inválida
        }

        int currentAmount = storage.getOrDefault(type, 0);

        if (currentAmount < amount) {
            return false; // No hay suficientes recursos
        }

        // Restar la cantidad
        storage.put(type, currentAmount - amount);

        return true;
    }

}
