package com.example.riderapp.common;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.riderapp.interfaces.IFCMService;
import com.example.riderapp.messages.Errors;
import com.example.riderapp.messages.Messages;
import com.example.riderapp.messages.ShowMessage;
import com.example.riderapp.model.fcm.FCMResponse;
import com.example.riderapp.model.fcm.Notification;
import com.example.riderapp.model.fcm.Sender;
import com.example.riderapp.model.firebase.Pickup;
import com.example.riderapp.model.firebase.User;
import com.example.riderapp.retrofit.GoogleMapsAPI;
import com.example.riderapp.retrofit.IFCMClient;
import com.example.riderapp.retrofit.IGoogleAPI;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Common {
    public static final String driver_tbl = "Drivers";
    public static final String user_driver_tbl = "DriversInformation";
    public static final String history_rider = "RiderHistory";
    public static final String user_rider_tbl = "RidersInformation";
    public static final String pickup_request_tbl = "PickupRequest";
    public static final String CHANNEL_ID_ARRIVED = "ARRIVED";
    public static final int PICK_IMAGE_REQUEST = 9999;
    public static String token_tbl = "Tokens";
    public static String rate_detail_tbl = "RateDetails";
    public static User currentUser = new User();
    public static String userID = "";

    public static String driverID = "";
    public static LatLng currentLocation;

    public static double getPrice(double km, int min) {
        return ConfigApp.baseFare + (ConfigApp.timeRate * min) + (ConfigApp.distanceRate * km);
    }

    public static IFCMService getFCMService() {
        final String fcmURL = "https://fcm.googleapis.com/";
        return IFCMClient.getClient(fcmURL).create(IFCMService.class);
    }

    public static IGoogleAPI getGoogleService() {
        final String googleAPIUrl = "https://maps.googleapis.com";
        return GoogleMapsAPI.getClient(googleAPIUrl).create(IGoogleAPI.class);
    }

    public static void sendRequestToDriver(final String driverID, final IFCMService mService,
                                           final Context context, final LatLng lastLocation) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference(Common.token_tbl);

        tokens.orderByKey().equalTo(driverID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                    String token = postSnapShot.getValue(String.class);
                    Pickup pickup = new Pickup();
                    pickup.setLat(lastLocation.latitude);
                    pickup.setLng(lastLocation.longitude);
                    pickup.setID(userID);
                    pickup.setToken(token);
                    String json_pickup = new Gson().toJson(pickup);

                    Notification data = new Notification("Pickup", json_pickup);
                    Sender content = new Sender(token, data);

                    mService.sendMessage(content).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call,
                                               Response<FCMResponse> response) {

                            if (response.body().success == 1)
                                ShowMessage.message(context, Messages.REQUEST_SUCCESS);
                            else
                                ShowMessage.messageError(context, Errors.SENT_FAILED);
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Log.d("ERROR", t.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
