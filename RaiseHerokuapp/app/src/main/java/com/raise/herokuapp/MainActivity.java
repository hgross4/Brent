package com.raise.herokuapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends ActionBarActivity implements
        FutureCallback<JsonObject>{

    private ListView offersListView;
    private static String TAG = MainActivity.class.getSimpleName();
    SharedPreferences sharedPreferences;
    private TextView currentPointsTv;
    private TextView totalPointsTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);

        currentPointsTv = (TextView) findViewById(R.id.current_points);
        totalPointsTv = (TextView) findViewById(R.id.total_points);
        totalPointsTv.setText("" + sharedPreferences.getInt(getString(R.string.total_bonus), 0));

        offersListView = (ListView) findViewById(R.id.offers_list);
        offersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                JsonObject json = new JsonObject();
                json.addProperty("brand", ((String)((TextView)view).getText()));
                Ion.with(MainActivity.this)
                        .load("http://raise-interviews.herokuapp.com/offer")
                        .setJsonObjectBody(json)
                        .asJsonObject()
                        .setCallback(new FutureCallback<JsonObject>() {
                            @Override
                            public void onCompleted(Exception e, JsonObject result) {
                                if (e != null) {
                                    Log.e(TAG, e.getLocalizedMessage());
                                }
                                else {
                                    if (result != null) {
                                        if (result.getAsJsonObject("error") != null) {
                                            Log.e(TAG, "invalid post");
                                        }
                                        else {
                                            int bonus = result.get("bonus").getAsInt();
                                            currentPointsTv.setText("" + bonus);
                                            int totalBonus = sharedPreferences.getInt(getString(R.string.total_bonus), 0) + bonus;
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putInt(getString(R.string.total_bonus), totalBonus);
                                            editor.commit();
                                            if (totalBonus%500 == 0 && bonus != 0) {
                                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                                builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        dialog.dismiss();
                                                    }
                                                })
                                                .setMessage(getString(R.string.you_have_earned) + " " + totalBonus + " " + getString(R.string.points))
                                                .setTitle(getString(R.string.milestone));
                                                AlertDialog dialog = builder.create();
                                                dialog.show();
                                            }
                                            totalPointsTv.setText("" + totalBonus);
                                        }
                                    }
                                }
                            }
                        });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOffers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            loadOffers();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCompleted(Exception e, JsonObject jsonObject) {
        if (e != null) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG)
                    .show();
            Log.e(getClass().getSimpleName(),
                    "Exception from request to server", e);
        }

        if (jsonObject != null) {
            JsonArray offers = jsonObject.getAsJsonArray("offer");
            ArrayList<JsonObject> offersList = new ArrayList<JsonObject>();

            for (int i = 0; i < offers.size(); ++i) {
                offersList.add(offers.get(i).getAsJsonObject());
            }
            Collections.sort(offersList, new Comparator<JsonObject>() {
                @Override
                public int compare(JsonObject json1, JsonObject json2) {
                    return json1.get("brand").getAsString()
                            .compareTo(json2.get("brand").getAsString());
                }
            });

            offersListView.setAdapter(new ItemsAdapter(offersList));
        }

    }

    void loadOffers() {
        Ion.with(this, "http://raise-interviews.herokuapp.com/offer")
                .asJsonObject().setCallback(this);
    }

    class ItemsAdapter extends ArrayAdapter<JsonObject> {
        ItemsAdapter(List<JsonObject> items) {
            super(MainActivity.this, android.R.layout.simple_list_item_1, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row=super.getView(position, convertView, parent);
            TextView brandTv = (TextView)row.findViewById(android.R.id.text1);

            brandTv.setText((CharSequence) getItem(position).get("brand").getAsString());
            brandTv.setBackgroundResource(R.drawable.rectangle);
            brandTv.setGravity(Gravity.CENTER);

            return(row);
        }
    }
}
