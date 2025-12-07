public abstract class Worker {

    protected int damagePerSecond;
    protected WorkerState state;
    protected NaturalResource target;

    public Worker(int damagePerSecond) {
        this.damagePerSecond = damagePerSecond;
        this.state = WorkerState.IDLE;
        this.target = null;
    }

    public WorkerState getState() {
        return state;
    }

    public NaturalResource getTarget() {
        return target;
    }
// asignar a un worker una estructura
    public void assignTo(NaturalResource resource) {
        this.target = resource;
        this.state = WorkerState.WORKING;
    }

    public void unassign() {
        this.target = null;
        this.state = WorkerState.IDLE;
    }


    public  int workOneSecond() {
        if (state != WorkerState.WORKING || target == null) return 0; // solo entra cuando ya termino de talarlo

        target.receiveDamage(damagePerSecond); // envio el daño por segundo que es 10

        if (target.isDepleted()) { // cuando la vida de la estructura  llegue a 0
            int reward = target.getResourceTotalOnDepletion();
            unassign(); //vulve a estar sin pincha
            return reward; // la cantidad de recurso que tenia esa estructura
        }

        return 0;
    }
}
