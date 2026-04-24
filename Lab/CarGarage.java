import java.util.ArrayList;
import java.util.List;

public class CarGarage implements CarCollection {
    private List<Car> cars = new ArrayList<>();

    public void addCar(Car car) {
        cars.add(car);
    }

    public int size() { return cars.size(); }
    public Car get(int i) { return cars.get(i); }

    @Override
    public Iterator createIterator(String type) {
        switch (type) {
            case "Electric":   return new ElectricCarIterator(this);
            case "Affordable": return new AffordableCarIterator(this, 30000);
            default:           return new AllCarIterator(this);
        }
    }
}