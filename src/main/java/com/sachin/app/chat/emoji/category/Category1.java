package com.sachin.app.chat.emoji.category;

import androidx.annotation.NonNull;

import com.vanniktech.emoji.emoji.Emoji;
import com.vanniktech.emoji.emoji.EmojiCategory;

public class Category1 implements EmojiCategory {
    @NonNull
    @Override
    public Emoji[] getEmojis() {
        return new Emoji[0];
    }

    @Override
    public int getIcon() {
        return 0;
    }
}
