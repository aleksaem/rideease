package com.example.riderapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.riderapp.R;
import com.example.riderapp.adapter.recyclerviewplaces.PlacesAdapter;
import com.example.riderapp.common.Common;
import com.example.riderapp.common.ConfigApp;
import com.example.riderapp.fragment.BottomSheetRiderFragment;
import com.example.riderapp.helper.CustomInfoWindow;
import com.example.riderapp.interfaces.IFCMService;
import com.example.riderapp.messages.Errors;
import com.example.riderapp.messages.ShowMessage;
import com.example.riderapp.model.firebase.User;
import com.example.riderapp.model.places.PlacesResponse;
import com.example.riderapp.model.places.Results;
import com.example.riderapp.utils.Location;
import com.example.riderapp.utils.NetworkUtil;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;

public class HomeActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener {

    private static final int LIMIT = 3;
    public static boolean driverFound = false;
    private ImageView carUberX, carUberBlack;
    private Button btnRequestPickup;
    private Toolbar toolbar;
    private GoogleMap mMap;
    private LinearLayout llPickupInput, llDestinationInput, llPickupPlace, llDestinationPlace;
    private EditText etFinalPickup, etFinalDestination, etPickup, etDestination;
    private RecyclerView rvPickupPlaces, rvDestinationPlaces;
    private SupportMapFragment mapFragment;
    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInAccount account;
    private Marker riderMarket, destinationMarker;
    private final ArrayList<Marker> driverMarkers = new ArrayList<>();
    private DatabaseReference driversAvailable;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private IFCMService ifcmService;
    private Location location;
    private NetworkUtil networkUtil;
    private String mPlaceLocation, mPlaceDestination;
    private Double currentLat, currentLng;
    private boolean isUberX = false, pickupPlacesSelected = false;
    private int radius = 1, distance = 1; // km
    private final String URL_BASE_API_PLACES = "https://maps.googleapis" +
            ".com/maps/api/place/textsearch/json?";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initViews();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        ifcmService = Common.getFCMService();
        networkUtil = new NetworkUtil(this);
        location = new Location(this, response -> {
            // Add a icon_marker in Sydney and move the camera
            currentLat = response.getLastLocation().getLatitude();
            currentLng = response.getLastLocation().getLongitude();
            Log.d("HomeActivity", "Lat: " + currentLat + ", Lng: " + currentLng);
            Common.currentLocation = new LatLng(response.getLastLocation().getLatitude(),
                    response.getLastLocation().getLongitude());
            displayLocation();
            if (mPlaceLocation == null) {
                driversAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
                driversAvailable.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        loadAllAvailableDriver(new LatLng(currentLat, currentLng));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        carUberX = findViewById(R.id.selectedUberX);
        carUberBlack = findViewById(R.id.selectedUberBlack);

        carUberX.setOnClickListener(v -> {
            boolean isToggle = !isUberX;
            isUberX = true;
            if (isToggle) {
                carUberX.setImageResource(R.drawable.car_cui_select);
                carUberBlack.setImageResource(R.drawable.car_vip);
            }
            loadAllAvailableDriver(new LatLng(currentLat, currentLng));
        });

        carUberBlack.setOnClickListener(v -> {
            boolean isToggle = isUberX;
            isUberX = false;
            if (isToggle) {
                carUberX.setImageResource(R.drawable.car_cui);
                carUberBlack.setImageResource(R.drawable.car_vip_select);
            }
            loadAllAvailableDriver(new LatLng(currentLat, currentLng));
        });

        btnRequestPickup = findViewById(R.id.btnPickupRequest);
        btnRequestPickup.setOnClickListener(v -> {
            if (currentLat != null && currentLng != null) {
                if (!driverFound)
                    requestPickup(Common.userID);
                else
                    Common.sendRequestToDriver(Common.driverID, ifcmService,
                            getApplicationContext(), Common.currentLocation);
            }
        });
        etFinalPickup.setOnFocusChangeListener((view, b) -> {
            if (b) {
                llPickupInput.setVisibility(View.VISIBLE);
                llPickupPlace.setVisibility(View.GONE);
                llDestinationInput.setVisibility(View.GONE);
                llDestinationPlace.setVisibility(View.GONE);
                etPickup.requestFocus();
            }
        });
        etFinalDestination.setOnFocusChangeListener((view, b) -> {
            if (b) {
                llPickupInput.setVisibility(View.GONE);
                llPickupPlace.setVisibility(View.GONE);
                llDestinationInput.setVisibility(View.VISIBLE);
                llDestinationPlace.setVisibility(View.GONE);
                etDestination.requestFocus();
            }
        });
        etPickup.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                getPlacesByString(charSequence.toString(), true);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        etDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                getPlacesByString(charSequence.toString(), false);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        updateFirebaseToken();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_home_drawer, menu);
        return true;
    }

    private void initViews() {
        llPickupInput = findViewById(R.id.ll_pickup_input);
        llPickupPlace = findViewById(R.id.ll_pickup_place);
        llDestinationInput = findViewById(R.id.ll_destination_input);
        llDestinationPlace = findViewById(R.id.ll_destination_place);
        etFinalPickup = findViewById(R.id.et_final_pickup_location);
        etFinalDestination = findViewById(R.id.et_final_destination);
        etDestination = findViewById(R.id.et_destination);
        etPickup = findViewById(R.id.et_pickup);
        rvPickupPlaces = findViewById(R.id.rv_pickup_places);
        rvPickupPlaces.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        rvDestinationPlaces = findViewById(R.id.rv_destination_places);
        rvDestinationPlaces.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void loadUser() {
        FirebaseDatabase.getInstance().getReference(Common.user_rider_tbl)
                .child(Common.userID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Common.currentUser = dataSnapshot.getValue(User.class);
                        initDrawer();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void verifyGoogleAccount() {
        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        OptionalPendingResult<GoogleSignInResult> opr =
                Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            opr.setResultCallback(googleSignInResult -> handleSignInResult(googleSignInResult));
        }
    }

    public void initDrawer() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View navigationHeaderView = navigationView.getHeaderView(0);
        TextView tvName = navigationHeaderView.findViewById(R.id.tvRiderName);
        TextView tvStars = findViewById(R.id.tvStars);
        CircleImageView imageAvatar =
                navigationHeaderView.findViewById(R.id.imgAvatar);

        tvName.setText(Common.currentUser.getName());
        if (Common.currentUser.getRates() != null &&
                !TextUtils.isEmpty(Common.currentUser.getRates()))
            tvStars.setText(Common.currentUser.getRates());
        if (Common.currentUser.getAvatarUrl() != null &&
                !TextUtils.isEmpty(Common.currentUser.getAvatarUrl()))
            Picasso.get().load(Common.currentUser.getAvatarUrl()).into(imageAvatar);
    }

    private void updateFirebaseToken() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        final DatabaseReference tokens = db.getReference(Common.token_tbl);


        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FIREBASE_TOKEN", "Fetching FCM registration token failed",
                                task.getException());
                        return;
                    }
                    String token = task.getResult();
                    tokens.child(FirebaseAuth.getInstance().getUid()).setValue(token);
                });
    }

    private void requestPickup(String uid) {
        DatabaseReference dbRequest =
                FirebaseDatabase.getInstance().getReference(Common.pickup_request_tbl);
        GeoFire mGeofire = new GeoFire(dbRequest);
        mGeofire.setLocation(uid, new GeoLocation(Common.currentLocation.latitude,
                        Common.currentLocation.longitude),
                (key, error) -> {

                });
        if (riderMarket.isVisible()) riderMarket.remove();
        riderMarket =
                mMap.addMarker(new MarkerOptions().title(getResources().getString(R.string.pickup_here))
                        .snippet("").position(new LatLng(Common.currentLocation.latitude,
                                Common.currentLocation.longitude))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        riderMarket.showInfoWindow();
        btnRequestPickup.setText(getResources().getString(R.string.getting_uber));
        findDriver();
    }

    private void findDriver() {
        DatabaseReference driverLocation;
        if (isUberX)
            driverLocation =
                    FirebaseDatabase.getInstance().getReference(Common.driver_tbl).child("UberX");
        else
            driverLocation =
                    FirebaseDatabase.getInstance().getReference(Common.driver_tbl).child("Uber " +
                            "Black");
        GeoFire geoFire = new GeoFire(driverLocation);
        GeoQuery geoQuery =
                geoFire.queryAtLocation(new GeoLocation(Common.currentLocation.latitude,
                        Common.currentLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound) {
                    driverFound = true;
                    Common.driverID = key;
                    btnRequestPickup.setText(getApplicationContext().getResources().getString(R.string.call_driver));
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onGeoQueryReady() {
                if (!driverFound && radius < LIMIT) {
                    radius++;
                    findDriver();
                } else {
                    if (!driverFound) {
                        Toast.makeText(HomeActivity.this, "No available any driver near you",
                                Toast.LENGTH_SHORT).show();
                        btnRequestPickup.setText("REQUEST PICKUP");
                    }
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            account = result.getSignInAccount();
            Common.userID = account.getId();
            loadUser();
        } else {
            Common.userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadUser();
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_trip_history:
                Log.d("HomeActivity", "Option Trip history selected");
                showTripHistory();
                break;
            case R.id.nav_updateInformation:
                Log.d("HomeActivity", "Option Update information selected");
                showDialogUpdateInfo();
                break;
            case R.id.nav_signOut:
                Log.d("HomeActivity", "Option Sign out selected");
                signOut();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showTripHistory() {
        Intent intent = new Intent(HomeActivity.this, TripHistoryActivity.class);
        startActivity(intent);
    }

    @SuppressLint("MissingInflatedId")
    private void showDialogUpdateInfo() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(HomeActivity.this);
        alertDialog.setTitle("UPDATE INFORMATION");
        LayoutInflater inflater = this.getLayoutInflater();
        View layout_pwd = inflater.inflate(R.layout.layout_update_information, null);
        final MaterialEditText etName = layout_pwd.findViewById(R.id.etName);
        final MaterialEditText etPhone = layout_pwd.findViewById(R.id.etPhone);
        final ImageView image_upload = layout_pwd.findViewById(R.id.imageUpload);
        image_upload.setOnClickListener(v -> chooseImage());
        alertDialog.setView(layout_pwd);
        alertDialog.setPositiveButton("UPDATE", (dialogInterface, i) -> {
            dialogInterface.dismiss();
            final android.app.AlertDialog waitingDialog =
                    new SpotsDialog.Builder().setContext(HomeActivity.this).build();
            waitingDialog.show();
            String name = etName.getText().toString();
            String phone = etPhone.getText().toString();

            Map<String, Object> updateInfo = new HashMap<>();
            if (!TextUtils.isEmpty(name))
                updateInfo.put("name", name);
            if (!TextUtils.isEmpty(phone))
                updateInfo.put("phone", phone);
            DatabaseReference driverInformation =
                    FirebaseDatabase.getInstance().getReference(Common.user_rider_tbl);
            driverInformation.child(Common.userID)
                    .updateChildren(updateInfo)
                    .addOnCompleteListener(task -> {
                        waitingDialog.dismiss();
                        if (task.isSuccessful())
                            Toast.makeText(HomeActivity.this, "Information Updated!",
                                    Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(HomeActivity.this, "Information Update " +
                                    "Failed!", Toast.LENGTH_SHORT).show();

                    });
        });
        alertDialog.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss());
        alertDialog.show();
    }

    private void chooseImage() {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            Intent intent = new Intent(Intent.ACTION_PICK);
                            intent.setType("image/*");
                            startActivityForResult(intent, Common.PICK_IMAGE_REQUEST);
                        } else {
                            Toast.makeText(getApplicationContext(), "Permission denied",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri saveUri = data.getData();
            if (saveUri != null) {
                final ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("Uploading...");
                progressDialog.show();

                String imageName = UUID.randomUUID().toString();
                final StorageReference imageFolder = storageReference.child("images/" + imageName);

                imageFolder.putFile(saveUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            progressDialog.dismiss();
                            Toast.makeText(HomeActivity.this, "Uploaded!",
                                    Toast.LENGTH_SHORT).show();
                            imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {

                                Map<String, Object> avatarUpdate = new HashMap<>();
                                avatarUpdate.put("avatarUrl", uri.toString());


                                DatabaseReference driverInformations =
                                        FirebaseDatabase.getInstance().getReference(Common.user_rider_tbl);
                                driverInformations.child(Common.userID).updateChildren(avatarUpdate)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful())
                                                Toast.makeText(HomeActivity.this,
                                                        "Uploaded!",
                                                        Toast.LENGTH_SHORT).show();
                                            else
                                                Toast.makeText(HomeActivity.this,
                                                        "Uploaded error!",
                                                        Toast.LENGTH_SHORT).show();

                                        });
                            });
                        }).addOnProgressListener(taskSnapshot -> {
                            double progress =
                                    (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            progressDialog.setMessage("Uploaded " + progress + "%");
                        });
            }
        }
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void displayLocation() {
        if (currentLat != null && currentLng != null) {
            //presence system
            driversAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
            driversAvailable.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //if have change from drivers table, we will reload all drivers available
                    loadAllAvailableDriver(new LatLng(currentLat, currentLng));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            loadAllAvailableDriver(new LatLng(currentLat, currentLng));

        } else {
            ShowMessage.messageError(this, Errors.WITHOUT_LOCATION);
        }

    }

    private void loadAllAvailableDriver(final LatLng location) {
        for (Marker driverMarker : driverMarkers) {
            driverMarker.remove();
        }
        driverMarkers.clear();
        if (!pickupPlacesSelected) {
            if (riderMarket != null)
                riderMarket.remove();

            riderMarket = mMap.addMarker(new MarkerOptions().position(location)
                    .title(getResources().getString(R.string.you))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_marker)));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f));
        }


        DatabaseReference driverLocation;
        if (isUberX)
            driverLocation =
                    FirebaseDatabase.getInstance().getReference(Common.driver_tbl).child("UberX");
        else
            driverLocation =
                    FirebaseDatabase.getInstance().getReference(Common.driver_tbl).child("Uber " +
                            "Black");
        GeoFire geoFire = new GeoFire(driverLocation);

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(location.latitude,
                location.longitude), distance);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {
                FirebaseDatabase.getInstance().getReference(Common.user_driver_tbl).child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        User driver = dataSnapshot.getValue(User.class);
                        String name;
                        String phone;

                        if (driver.getName() != null) name = driver.getName();
                        else name = "not available";

                        if (driver.getPhone() != null) phone = "Phone: " + driver.getPhone();
                        else phone = "Phone: none";


                        driverMarkers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude)).flat(true)
                                .title(name).snippet("Driver ID: " + dataSnapshot.getKey()).icon(BitmapDescriptorFactory.fromResource(R.drawable.car))));

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (distance <= LIMIT) {
                    distance++;
                    loadAllAvailableDriver(location);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setInfoWindowAdapter(new CustomInfoWindow(this));
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_style_map));

        mMap.setOnMapClickListener(latLng -> {
            if (destinationMarker != null)
                destinationMarker.remove();
            destinationMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_destination_marker))
                    .title("Destination"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));

            @SuppressLint("DefaultLocale") BottomSheetRiderFragment mBottomSheet =
                    BottomSheetRiderFragment.newInstance(String.format("%f,%f", currentLat,
                                    currentLng),
                            String.format("%f,%f", latLng.latitude, latLng.longitude), true);
            mBottomSheet.show(getSupportFragmentManager(), mBottomSheet.getTag());
        });
        mMap.setOnInfoWindowClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        location.onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStop() {
        super.onStop();
        location.stopUpdateLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        displayLocation();
        location.initializeLocation();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (!marker.getTitle().equals("You")) {
            Intent intent = new Intent(HomeActivity.this, CallDriverActivity.class);
            String ID = marker.getSnippet().replace("Driver ID: ", "");
            intent.putExtra("driverID", ID);
            intent.putExtra("lat", currentLat);
            intent.putExtra("lng", currentLng);
            startActivity(intent);
        }
    }

    private void getPlacesByString(String s, final boolean isPickup) {
        String queryEncode = s;
        try {
            queryEncode = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String query = "&query=" + queryEncode;
        String location =
                "&location=" + Common.currentLocation.latitude + "," + Common.currentLocation.longitude;
        String radius = "radius=1500";
        String key = "&key=" + ConfigApp.GOOGLE_API_KEY;
        String url = (URL_BASE_API_PLACES + radius + location + query + key).replaceAll(" ", "%20");

        Log.d("URL_PLACES", url);
        networkUtil.httpRequest(url, response -> {
            pickupPlacesSelected = true;
            Gson gson = new Gson();
            PlacesResponse placesResponse = gson.fromJson(response, PlacesResponse.class);
            for (Results result : placesResponse.results) {
                if (result.geometry.location == null) {
                    placesResponse.results.remove(result);
                } else if (result.geometry.location.lat == null || result.geometry.location.lat.equals("")
                        || result.geometry.location.lat.equals("0.0")) {
                    placesResponse.results.remove(result);
                } else if (result.geometry.location.lng == null || result.geometry.location.lng.equals("")
                        || result.geometry.location.lng.equals("0.0")) {
                    placesResponse.results.remove(result);
                }
            }
            if (isPickup)
                implementPickupRecyclerView(placesResponse.results);
            else
                implementDestinationRecyclerView(placesResponse.results);

        });
    }

    private void implementPickupRecyclerView(final ArrayList<Results> results) {
        PlacesAdapter placesAdapter = new PlacesAdapter(this, results, (view, index) -> {
            mPlaceLocation = results.get(index).formatted_address;
            etFinalPickup.setText(mPlaceLocation);

            llPickupInput.setVisibility(View.GONE);
            llPickupPlace.setVisibility(View.VISIBLE);
            llDestinationInput.setVisibility(View.GONE);
            llDestinationPlace.setVisibility(View.VISIBLE);

            double lat = Double.parseDouble(results.get(index).geometry.location.lat);
            double lng = Double.parseDouble(results.get(index).geometry.location.lng);
            LatLng latLng = new LatLng(lat, lng);
            if (riderMarket != null)
                riderMarket.remove();
            riderMarket = mMap.addMarker(new MarkerOptions().position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_marker))
                    .title("Pickup Here"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
        });
        rvPickupPlaces.setAdapter(placesAdapter);
    }

    private void implementDestinationRecyclerView(final ArrayList<Results> results) {
        PlacesAdapter placesAdapter = new PlacesAdapter(this, results, (view, index) -> {
            mPlaceDestination = results.get(index).formatted_address;
            etFinalDestination.setText(mPlaceDestination);

            llPickupInput.setVisibility(View.GONE);
            llPickupPlace.setVisibility(View.VISIBLE);
            llDestinationInput.setVisibility(View.GONE);
            llDestinationPlace.setVisibility(View.VISIBLE);

            double lat = Double.parseDouble(results.get(index).geometry.location.lat);
            double lng = Double.parseDouble(results.get(index).geometry.location.lng);
            LatLng latLng = new LatLng(lat, lng);
            if (destinationMarker != null)
                destinationMarker.remove();
            destinationMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_destination_marker))
                    .title("Destination"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));

            BottomSheetRiderFragment mBottomSheet =
                    BottomSheetRiderFragment.newInstance(mPlaceLocation, mPlaceDestination, false);
            mBottomSheet.show(getSupportFragmentManager(), mBottomSheet.getTag());
        });
        rvDestinationPlaces.setAdapter(placesAdapter);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
