package com.example.viikko10;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class ListInfoActivity extends AppCompatActivity {
    private TextView cityText, yearText, carInfoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_info);

        cityText = findViewById(R.id.CityText);
        yearText = findViewById(R.id.YearText);
        carInfoText = findViewById(R.id.CarInfoText);

        displayInfo();

    }

    private void displayInfo() {
        CarDataStorage storage = CarDataStorage.getInstance();

        cityText.setText(storage.getCity());
        yearText.setText(String.valueOf(storage.getYear()));

        StringBuilder infoBuilder = new StringBuilder();
        ArrayList<CarData> carDataList = storage.getCarData();

        if (carDataList.isEmpty()) {
            infoBuilder.append("Ei haettuja tietoja.");
        } else {
            for (CarData data : carDataList) {
                infoBuilder.append(data.getType()).append(": ").append(data.getAmount()).append("\n");
            }
        }

        carInfoText.setText(infoBuilder.toString());

    }
}