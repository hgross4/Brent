package com.brent.constactslist.controllers;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.brent.constactslist.R;
import com.brent.constactslist.model.Address;
import com.brent.constactslist.model.Contact;
import com.brent.constactslist.model.Details;
import com.brent.constactslist.model.Phone;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class DetailsFragment extends Fragment {

    private static final String TAG = DetailsFragment.class.getSimpleName();
    private ImageView largeImage;
    private TextView name;
    private TextView company;
    private TextView homePhone;
    private TextView workPhone;
    private TextView mobilePhone;
    private TextView streetAddress;
    private TextView cityStateZip;
    private TextView birthday;
    private TextView email;
    private TextView website;
    private ImageView star;

    public DetailsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_details, container, false);
        star = (ImageView) rootView.findViewById(R.id.star);
        largeImage = (ImageView) rootView.findViewById(R.id.large_image);
        name = (TextView) rootView.findViewById(R.id.name);
        company = (TextView) rootView.findViewById(R.id.company);
        homePhone = (TextView) rootView.findViewById(R.id.home_phone);
        workPhone = (TextView) rootView.findViewById(R.id.work_phone);
        mobilePhone = (TextView) rootView.findViewById(R.id.mobile_phone);
        streetAddress = (TextView) rootView.findViewById(R.id.street_address);
        cityStateZip = (TextView) rootView.findViewById(R.id.city_state_zip);
        birthday = (TextView) rootView.findViewById(R.id.birthday);
        email = (TextView) rootView.findViewById(R.id.email);
        website = (TextView) rootView.findViewById(R.id.website);

        return rootView;
    }

    public void loadDetails(final Contact contact) {

        // Populate the fields whose data is contained in the contact object
        name.setText(contact.getName());
        company.setText(contact.getCompany());
        Phone phone = contact.getPhone();
        homePhone.setText(phone.getHome());
        workPhone.setText(phone.getWork());
        mobilePhone.setText(phone.getMobile());
        long msBirthdate= Long.parseLong(contact.getBirthdate());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(msBirthdate);
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
        birthday.setText(formatter.format(calendar.getTime()));

        // Get the contact details and populate the details fields
        String endpoint = contact.getDetailsURL();
        Ion.with(getActivity())
                .load(endpoint)
                .as(new TypeToken<Details>(){})
                .setCallback(new FutureCallback<Details>() {
                    @Override
                    public void onCompleted(Exception e, Details details) {
                        if (e != null ) {
                            Log.e(TAG, "error: " + e.getLocalizedMessage());
                            Toast.makeText(getActivity(), "Are you connected to the Internet?",
                                    Toast.LENGTH_LONG);
                        }
                        else if (e == null && details != null) {
                            Ion.with(largeImage)
                                    .load(details.getLargeImageURL());
//                            if (details.isFavorite()) {
//                                star.setImageResource(android.R.drawable.star_on);
//                            }
//                            else {
//                                star.setImageResource(android.R.drawable.star_off);
//                            }
                            Address address = details.getAddress();
                            streetAddress.setText(address.getStreet());
                            cityStateZip.setText(address.getCity() + ", " +
                                    address.getState() + " " + address.getZip());
                            email.setText(details.getEmail());
                            website.setText(details.getWebsite());
                        }
                    }
                });
    }


}
