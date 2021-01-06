package com.sachin.app.chat.util;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class ValueListener implements ValueEventListener {

    @Override
    public void onDataChange(@NonNull DataSnapshot snapshot) {

    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
        Log.e("MyValueEventListener", "Load data error", databaseError.toException());
    }
}
