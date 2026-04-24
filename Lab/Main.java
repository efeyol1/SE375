public class Main {

    static void print(String title, Iterator it) {
        System.out.println("=== " + title + " ===");
        for (it.first(); !it.isDone(); it.next()) {
            System.out.println(it.currentItem());
        }
        System.out.println();
    }

    public static void main(String[] args) {
        CarGarage garage = new CarGarage();

        garage.addCar(new Car("Tesla Model 3",  "Electric",  42000));
        garage.addCar(new Car("Fiat 500",        "Gasoline",  18000));
        garage.addCar(new Car("Toyota Corolla",  "Hybrid",    28000));
        garage.addCar(new Car("Porsche Taycan",  "Electric",  90000));
        garage.addCar(new Car("Hyundai i20",     "Gasoline",  15000));
        garage.addCar(new Car("Honda Civic",     "Hybrid",    30000));

        print("ALL Cars (should be 6)",              garage.createIterator("All"));
        print("ELECTRIC Cars (should be 2)",         garage.createIterator("Electric"));
        print("AFFORDABLE <= $30,000 (should be 4)", garage.createIterator("Affordable"));
    }
}
