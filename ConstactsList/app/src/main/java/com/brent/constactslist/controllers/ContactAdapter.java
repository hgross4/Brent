package com.brent.constactslist.controllers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.brent.constactslist.R;
import com.brent.constactslist.model.Contact;
import com.koushikdutta.ion.Ion;

import java.util.List;

/**
 * Created by Brent on 2/28/15.
 */
public class ContactAdapter extends ArrayAdapter<Contact> {

    private Context context;
    private List<Contact> contacts;

    public ContactAdapter(Context context, List<Contact> contacts) {
        super(context, R.layout.row, contacts);
        this.context = context;
        this.contacts = contacts;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        if (convertView == null) {
            convertView=
                    LayoutInflater.from(context).inflate(R.layout.row, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        Contact contact = contacts.get(position);

        // Get the contact's thumbnail image and load it into the ImageView,
        // and get their name and work number and put in the corresponding TextViews
        // (First set the ImageView to have no image, to prevent an image from another row
        //  from incorrectly showing, if this row doesn't have one for some reason.)
        holder.thumbnail.setImageDrawable(null);
        Ion.with(holder.thumbnail)
                .load(contact.getSmallImageURL());
        holder.name.setText(contact.getName());
        holder.workNumber.setText(contact.getPhone().getWork());

        return convertView;
    }

    static class ViewHolder {
        ImageView thumbnail = null;
        TextView name = null;
        TextView workNumber = null;

        ViewHolder(View row) {
            thumbnail = (ImageView) row.findViewById(R.id.thumbnail);
            name = (TextView) row.findViewById(R.id.name);
            workNumber = (TextView) row.findViewById(R.id.work_number);
        }
    }
}
