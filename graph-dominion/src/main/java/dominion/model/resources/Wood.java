public class Wood {

    private int amount;

    public Wood() {
        this.amount = 0;
    }

    public void add(int value) {
        this.amount += value;
    }

    public void remove(int value) {
        this.amount = Math.max(0, amount - value);
    }

    public int getAmount() {
        return amount;
    }
}
