public class AllCarIterator implements Iterator {
    private CarGarage garage;
    private int index;

    public AllCarIterator(CarGarage garage) {
        this.garage = garage;
        first();
    }

    @Override
    public void first() {
        index = 0;
    }

    @Override
    public void next() {
        index++;
    }

    @Override
    public boolean isDone() {
        return index >= garage.size();
    }

    @Override
    public Car currentItem() {
        if (isDone()) return null;
        return garage.get(index);
    }
}