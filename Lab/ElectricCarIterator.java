public class ElectricCarIterator implements Iterator {
    private CarGarage garage;
    private int index;

    public ElectricCarIterator(CarGarage garage) {
        this.garage = garage;
        first();
    }

    @Override
    public void first() {
        index = 0;
        skipNonMatching();
    }

    @Override
    public void next() {
        index++;
        skipNonMatching();
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

    private void skipNonMatching() {
        while (index < garage.size() &&
                !"Electric".equalsIgnoreCase(garage.get(index).getEngineType())) {
            index++;
        }
    }
}