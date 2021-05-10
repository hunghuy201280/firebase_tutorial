package com.example.messageapp;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;


import java.util.List;
import java.util.Optional;


public class MessageAdapter extends ArrayAdapter<FriendlyMessage> {
    private List<FriendlyMessage> FriendlyMessages;

    public MessageAdapter(Context context, int resource, List<FriendlyMessage> objects) {
        super(context, resource, objects);
        FriendlyMessages=objects;
        mFirebaseAuth=FirebaseAuth.getInstance();
    }
    private final int LAYOUT_LEFT=0;
    private final int LAYOUT_RIGHT=1;
    private FirebaseAuth mFirebaseAuth;
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (this.getItemViewType(position) == LAYOUT_LEFT)
        {
           
            convertView =((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_message_left, parent, false);;
        }
        else if(this.getItemViewType(position) == LAYOUT_RIGHT)
        {
            convertView =((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_message_right, parent, false);;
        }

        ImageView photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
        TextView messageTextView = (TextView) convertView.findViewById(R.id.messageTextView);
        TextView authorTextView = (TextView) convertView.findViewById(R.id.nameTextView);

        FriendlyMessage message = getItem(position);


        boolean isPhoto = message.getPhotoUrl() != null;
        if (isPhoto) {
            messageTextView.setVisibility(View.GONE);
            photoImageView.setVisibility(View.VISIBLE);
            Glide.with(photoImageView.getContext())
                    .load(message.getPhotoUrl())
                    .into(photoImageView);
        } else {
            messageTextView.setVisibility(View.VISIBLE);
            photoImageView.setVisibility(View.GONE);
            messageTextView.setText(message.getText());
        }
        authorTextView.setText(message.getName());

        return convertView;
    }

    @Override
    public int getViewTypeCount()
    {
        return 2;
    }
    @Override
    public int getItemViewType(int position)
    {
        FriendlyMessage message = getItem(position);
        if (mFirebaseAuth.getCurrentUser()!=null && message.getYou(mFirebaseAuth.getCurrentUser().getDisplayName()))
        {
            return LAYOUT_RIGHT;
        }
        else if (mFirebaseAuth.getCurrentUser()==null && message.getName().equals("anonymous"))
        {
            return LAYOUT_RIGHT;
        }

        return LAYOUT_LEFT;
    }

    public Integer findMessageWithID(String messageID)
    {
        Optional<FriendlyMessage> result = FriendlyMessages.stream()
                .filter(item -> item.getMessageID().equals(messageID))
                .findFirst();
        if(result.isPresent())
        {
            return FriendlyMessages.indexOf(result.get());
        }
        else
        {
            return null;
        }
    }
    public void updateItem(int position,FriendlyMessage newValue)
    {
        FriendlyMessages.set(position,newValue );

    }
}
