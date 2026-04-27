package com.example.cookup_app.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.cookup_app.R;
import com.google.android.material.card.MaterialCardView;

public class AvatarView extends MaterialCardView {

    private ImageView imgAvatar;

    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.avatar_view, this);
        imgAvatar = findViewById(R.id.imgAvatar);

        setRadius(999f);
        setCardElevation(6f);
        setCardBackgroundColor(getContext()
                .getColor(android.R.color.transparent));
    }

    public void loadUrl(String imageUrl) {
        Glide.with(getContext())
                .load(imageUrl)
                .circleCrop()
                .placeholder(R.drawable.avatar_default_unknown)
                .error(R.drawable.avatar_default_unknown)
                .into(imgAvatar);
    }

    public void setGender(String gender) {
        if ("male".equals(gender)) {
            imgAvatar.setImageResource(R.drawable.avatar_default_male);
        } else if ("female".equals(gender)) {
            imgAvatar.setImageResource(R.drawable.avatar_default_female);
        } else {
            imgAvatar.setImageResource(R.drawable.avatar_default_unknown);
        }
    }
}