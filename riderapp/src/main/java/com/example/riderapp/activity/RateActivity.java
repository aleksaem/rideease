package com.example.riderapp.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.riderapp.R;
import com.example.riderapp.common.Common;
import com.example.riderapp.model.firebase.Rate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

public class RateActivity extends AppCompatActivity {
    Button btnSubmit;
    MaterialRatingBar ratingBar;
    MaterialEditText etComment;

    FirebaseDatabase database;
    DatabaseReference rateDetailRef, driverInformationRef;

    double ratingStars = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate);

        database = FirebaseDatabase.getInstance();
        rateDetailRef = database.getReference(Common.rate_detail_tbl);
        driverInformationRef = database.getReference(Common.user_driver_tbl);

        btnSubmit = findViewById(R.id.btnSubmit);
        ratingBar = findViewById(R.id.ratingBar);
        etComment = findViewById(R.id.etComment);

        ratingBar.setOnRatingChangeListener((ratingBar, rating) -> ratingStars = rating);
        btnSubmit.setOnClickListener(v -> submitRateDetails());
    }

    private void submitRateDetails() {
        final AlertDialog alertDialog = new SpotsDialog.Builder().setContext(this).build();
        alertDialog.show();

        Rate rate = new Rate();
        rate.setRates(String.valueOf(ratingStars));
        rate.setComment(etComment.getText().toString());

        rateDetailRef.child(Common.driverID)
                .push()
                .setValue(rate)
                .addOnCompleteListener(task -> rateDetailRef.child(Common.driverID)
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                double averageStars = 0.0;
                                int count = 0;
                                for (DataSnapshot postSnapshot :
                                        dataSnapshot.getChildren()) {
                                    Rate rate1 = postSnapshot.getValue(Rate.class);
                                    averageStars += Double.parseDouble(rate1.getRates());
                                    count++;
                                }
                                double finalAverge = averageStars / count;
                                DecimalFormat df = new DecimalFormat("#.#");
                                String valueUpdate = df.format(finalAverge);

                                Map<String, Object> driverUpdateRate = new HashMap<>();
                                driverUpdateRate.put("rates", valueUpdate);

                                driverInformationRef.child(Common.driverID)
                                        .updateChildren(driverUpdateRate)
                                        .addOnCompleteListener(task1 -> {
                                            alertDialog.dismiss();
                                            Toast.makeText(getApplicationContext(),
                                                    "Thank you for submit",
                                                    Toast.LENGTH_SHORT).show();
                                            finish();
                                        }).addOnFailureListener(e -> {
                                            alertDialog.dismiss();
                                            Toast.makeText(getApplicationContext(),
                                                    "Rate updated but can't write to " +
                                                            "driver information",
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        })).addOnFailureListener(e -> {
                    alertDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Rate failed!",
                            Toast.LENGTH_SHORT).show();
                });
    }
}
