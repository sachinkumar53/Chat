package com.sachin.app.chat.emoji;

import androidx.annotation.NonNull;

import com.sachin.app.chat.emoji.category.Category1;
import com.vanniktech.emoji.EmojiProvider;
import com.vanniktech.emoji.emoji.EmojiCategory;

public class JoyPixelsEmojiProvider implements EmojiProvider {

    private JoyPixelsEmojiProvider(){
    }

    @NonNull
    @Override
    public EmojiCategory[] getCategories() {
        return new EmojiCategory[]{
                new Category1(), //smiley and people
                new Category1(), //Animals and nature
                new Category1(), //food and drinks
                new Category1(), //activities
                new Category1(), //travel and places
                new Category1(), //objects
                new Category1(), //symbols
                new Category1() //flags
        };
    }
}
