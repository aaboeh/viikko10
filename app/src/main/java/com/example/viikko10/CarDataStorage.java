package com.example.viikko10;

import java.util.ArrayList;
import java.util.Calendar;

public class CarDataStorage {
    protected String city;
    protected int year;
    protected ArrayList<CarData> carDataList = new ArrayList<>();
    private static CarDataStorage carDataStorage =  null;

    private CarDataStorage() {

    }

    public static CarDataStorage getInstance() {
        if(carDataStorage == null) {
            carDataStorage = new CarDataStorage();
        }
        return carDataStorage;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public ArrayList<CarData> getCarData() {
        return carDataList;
    }

    public void setCarData(ArrayList<CarData> carDataList) {
        this.carDataList = carDataList;
    }

    public void clearData() {
        carDataList.clear();
    }

    public void addCarData(CarData carData) {
        carDataList.add(carData);
    }

}
