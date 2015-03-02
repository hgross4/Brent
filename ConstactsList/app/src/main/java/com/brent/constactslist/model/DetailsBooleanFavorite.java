package com.brent.constactslist.model;

/**
 * Created by Brent on 2/28/15.
 * Contains much of the info. shown
 * on the Details screen for each contact,
 * for those contacts whose "favorite" field is a boolean
 */
public class DetailsBooleanFavorite {

    private int employeeId;
    private boolean favorite;
    private String largeImageURL;
    private String email;
    private String website;
    private Address address;

    public int getEmployeeId() {
        return employeeId;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public String getLargeImageURL() {
        return largeImageURL;
    }

    public String getEmail() {
        return email;
    }

    public String getWebsite() {
        return website;
    }

    public Address getAddress() {
        return address;
    }
}
