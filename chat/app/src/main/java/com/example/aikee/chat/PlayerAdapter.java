package com.example.aikee.chat;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class PlayerAdapter extends ArrayAdapter<Player> {
    public PlayerAdapter(Context context, int resource, List<Player> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_player, parent, false);
        }

        TextView playerTextView = (TextView) convertView.findViewById(R.id.playerTextView);

        Player player = getItem(position);

        playerTextView.setVisibility(View.VISIBLE);
        playerTextView.setText(player.getDisplayName());

        return convertView;
    }
}
