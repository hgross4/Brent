package com.brent.constactslist.model;

/**
 * Created by Brent on 2/28/15.
 */
public class Contact {

    private String name;
    private int employeeId;
    private String company;
    private String detailsURL;
    private String smallImageURL;
    private String birthdate;
    private Phone phone;

    public String getName() {
        return name;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getCompany() {
        return company;
    }

    public String getDetailsURL() {
        return detailsURL;
    }

    public String getSmallImageURL() {
        return smallImageURL;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public Phone getPhone() {
        return phone;
    }
}
