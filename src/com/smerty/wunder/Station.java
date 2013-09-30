package com.smerty.wunder;

/**
 * Created by paul on 9/29/13.
 */
public class Station {

    public String neighborhood;
    public String city;
    public String state;
    public String country;
    public String id;
    public String distance_km;
    public String distance_mi;

    public String displayName() {
        StringBuilder sb = new StringBuilder();

        if (neighborhood != null && !neighborhood.isEmpty()) {
            sb.append(neighborhood).append("\n");
        }

        sb.append(city).append(", ").append(state);

        return sb.toString();
    }

}
