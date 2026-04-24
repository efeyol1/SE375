public class AffordableCarIterator implements Iterator {
    private CarGarage garage;
    private int index;
    private double maxPrice;

    public AffordableCarIterator(CarGarage garage, double maxPrice) {
        this.garage = garage;
        this.maxPrice = maxPrice;
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
                garage.get(index).getPrice() > maxPrice) {
            index++;
        }
    }
}