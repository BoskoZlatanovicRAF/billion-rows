package core;

public class StationData {
    private int stationCount;
    private double temperatureSum;

    public synchronized void update(double temp) {
        stationCount++;
        temperatureSum += temp;
    }

    public int getCount() { return stationCount; }
    public double getSum() { return temperatureSum; }
}
