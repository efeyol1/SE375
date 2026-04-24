public interface Iterator {
    void first();
    void next();
    boolean isDone();
    Car currentItem();
}