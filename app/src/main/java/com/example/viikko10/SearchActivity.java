package com.example.viikko10;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class SearchActivity extends AppCompatActivity {
    private EditText cityNameEdit, yearEdit;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        cityNameEdit = findViewById(R.id.CityNameEdit);
        yearEdit = findViewById(R.id.YearEdit);
        statusText = findViewById(R.id.StatusText);
        Button searchButton = findViewById(R.id.SearchButton);
        Button listButton = findViewById(R.id.ListInfoActivityButton);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateSearch();
            }
        });
    }

    public void switchToListInfo(View view) {
        Intent intent = new Intent(this, ListInfoActivity.class);
        startActivity(intent);
    }

    public void validateSearch() {
        String city = cityNameEdit.getText().toString().trim();
        String yearString = yearEdit.getText().toString().trim();

        statusText.setText("Haetaan tietoja...");

        if (city.isEmpty()) {
            statusText.setText("Haku epäonnistui, kaupunki puuttuu");
            return;
        }

        try {
            int year = Integer.parseInt(yearString);
            getData(this, city, year);
        } catch (NumberFormatException e) {
            statusText.setText("Haku epäonnistui, vuosi ei ole numero");
        }
    }

    public void getData(Context context, String city, int year) {

        runOnUiThread(() -> statusText.setText("Haetaan tietoja..."));

        CarDataStorage storage = CarDataStorage.getInstance();
        storage.clearData();
        storage.setCity(city);
        storage.setYear(year);

        new Thread(() -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();

                URL metadataUrl = new URL("https://pxdata.stat.fi:443/PxWeb/api/v1/fi/StatFin/mkan/statfin_mkan_pxt_11ic.px");
                JsonNode areas = objectMapper.readTree(metadataUrl);

                ArrayList<String> areaCodes = new ArrayList<>();
                ArrayList<String> areaNames = new ArrayList<>();

                for (JsonNode node : areas.get("variables").get(1).get("values")) {
                    areaNames.add(node.asText());
                }
                for (JsonNode node : areas.get("variables").get(1).get("valueTexts")) {
                    areaCodes.add(node.asText());
                }

                HashMap<String, String> cityCodes = new HashMap<>();
                for (int i = 0; i < areaCodes.size(); i++) {
                    cityCodes.put(areaCodes.get(i), areaNames.get(i));
                }

                String code = cityCodes.get(city);

                if (code == null) {
                    runOnUiThread(() -> statusText.setText("Haku epäonnistui, kaupunkia ei löydy"));
                    return;
                }

                URL dataUrl = new URL("https://pxdata.stat.fi:443/PxWeb/api/v1/fi/StatFin/mkan/statfin_mkan_pxt_11ic.px");
                HttpURLConnection con = (HttpURLConnection) dataUrl.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; utf-8");
                con.setRequestProperty("Accept", "application/json");
                con.setDoOutput(true);

                ObjectNode queryObj = objectMapper.createObjectNode();
                ArrayNode queryArray = objectMapper.createArrayNode();

                ObjectNode vehicleClassFilter = objectMapper.createObjectNode();
                vehicleClassFilter.put("code", "Ajoneuvoluokka");
                ObjectNode vehicleSelection = objectMapper.createObjectNode();
                vehicleSelection.put("filter", "item");
                ArrayNode vehicleValues = vehicleSelection.putArray("values");
                vehicleValues.add("01");
                vehicleValues.add("02");
                vehicleValues.add("03");
                vehicleValues.add("04");
                vehicleValues.add("05");
                vehicleClassFilter.set("selection", vehicleSelection);
                queryArray.add(vehicleClassFilter);

                ObjectNode trafficFilter = objectMapper.createObjectNode();
                trafficFilter.put("code", "Liikennekäyttö");
                ObjectNode trafficSelection = objectMapper.createObjectNode();
                trafficSelection.put("filter", "item");
                ArrayNode trafficValues = trafficSelection.putArray("values");
                trafficValues.add("0");
                trafficFilter.set("selection", trafficSelection);
                queryArray.add(trafficFilter);

                ObjectNode yearFilter = objectMapper.createObjectNode();
                yearFilter.put("code", "Vuosi");
                ObjectNode yearSelection = objectMapper.createObjectNode();
                yearSelection.put("filter", "item");
                ArrayNode yearValues = yearSelection.putArray("values");
                yearValues.add(String.valueOf(year));
                yearFilter.set("selection", yearSelection);
                queryArray.add(yearFilter);

                ObjectNode areaFilter = objectMapper.createObjectNode();
                areaFilter.put("code", "Alue");
                ObjectNode areaSelection = objectMapper.createObjectNode();
                areaSelection.put("filter", "item");
                ArrayNode areaValues = areaSelection.putArray("values");
                areaValues.add(code);
                areaFilter.set("selection", areaSelection);
                queryArray.add(areaFilter);

                queryObj.set("query", queryArray);

                ObjectNode responseFormat = objectMapper.createObjectNode();
                responseFormat.put("format", "json-stat2");
                queryObj.set("response", responseFormat);

                ObjectNode fullRequest = objectMapper.createObjectNode();
                fullRequest.set("queryObj", queryObj);
                fullRequest.put("tableIdForQuery", "statfin_mkan_pxt_11ic.px");

                OutputStream os = con.getOutputStream();
                os.write(objectMapper.writeValueAsBytes(fullRequest));
                os.close();

                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                JsonNode vehicleData = objectMapper.readTree(response.toString());

                JsonNode vehicleTypes = vehicleData.get("dataset").get("dimension").get("Ajoneuvoluokka").get("category").get("label");
                JsonNode vehicleCounts = vehicleData.get("dataset").get("value");

                int totalVehicles = 0;
                ArrayList<String> typeNames = new ArrayList<>();

                if (vehicleTypes.isObject()) {
                    for (JsonNode type : vehicleTypes) {
                        typeNames.add(type.asText());
                    }
                } else if (vehicleTypes.isArray()) {
                    for (JsonNode type : vehicleTypes) {
                        typeNames.add(type.asText());
                    }
                }

                for (int i = 0; i < vehicleCounts.size(); i++) {
                    String vehicleType = (i < typeNames.size()) ? typeNames.get(i) : "Type " + (i + 1);
                    int amount = vehicleCounts.get(i).asInt();
                    storage.addCarData(new CarData(vehicleType, amount));
                    totalVehicles += amount;
                }

                storage.addCarData(new CarData("Yhteensä", totalVehicles));

                runOnUiThread(() -> statusText.setText("Haku onnistui"));

            } catch (Exception e) {
                e.printStackTrace();
                final String errorMsg = e.getMessage();
                runOnUiThread(() -> statusText.setText("Haku epäonnistui, " + errorMsg));
            }
        }).start();
    }
}
