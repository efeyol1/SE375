public class Car {
    private String model;
    private String engineType;
    private double price;

    public Car(String model, String engineType, double price) {
        this.model = model;
        this.engineType = engineType;
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getEngineType() {
        return engineType;
    }

    public String getModel() {
        return model;
    }

    @Override
    public String toString() {
        return model + "\t" + engineType + "\t" + price;
    }

    public static void main(String[] args) {

    }
}


