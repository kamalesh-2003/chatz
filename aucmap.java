package com.example.auctioneer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class aucmap extends AppCompatActivity {

    private FusedLocationProviderClient mFusedLocationClient;
    private TextView cityTextView;
    private EditText messageEditText;
    private Button sendButton;
    private LinearLayout messageContainer;
    private static final int PERMISSION_ID = 44;

    // Firebase Firestore
    private FirebaseFirestore firestore;
    private CollectionReference comments;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aucmap);
        cityTextView = findViewById(R.id.cityTextView);
        messageEditText = findViewById(R.id.commentEditText);
        sendButton = findViewById(R.id.postCommentButton);
        messageContainer = findViewById(R.id.messageContainer);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firestore = FirebaseFirestore.getInstance();

        getLastLocation();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageEditText.getText().toString().trim();
                if (!message.isEmpty()) {
                    String cityName = cityTextView.getText().toString().substring(19); // Extract city name from the TextView
                    sendComment(message); // Call the sendComment method
                    //messageEditText.setText("");
                }
            }
        });

    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        try {
                            Location location = task.getResult();
                            if (location == null) {
                                requestNewLocationData();
                            } else {
                                // Reverse geocoding to get the city name
                                Geocoder geocoder = new Geocoder(aucmap.this, Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    if (addresses.size() > 0) {
                                        String cityName = addresses.get(0).getLocality();
                                        updateCityTextView(cityTextView, cityName);
                                        comments = firestore.collection("chatrooms").document(cityName).collection("crmessages");
                                        fetchComments();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please enable location", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }

    public static void updateCityTextView(TextView cityTextView, String cityName) {
        cityTextView.setText("Trending posts in " + cityName);
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Please allow location tracking to continue", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestNewLocationData() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setNumUpdates(1);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
            }
        };
    }

    private void sendComment(String message) {
        // Create a new comment document in Firestore
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("message", message);

        comments.add(commentData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Comment sent successfully
                        // Retrieve the comment message
                        String message = messageEditText.getText().toString().trim();

                        // Add the comment dynamically to the ScrollView
                        addcomment(message);
                        messageEditText.setText("");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to send comment
                        Toast.makeText(aucmap.this, "Failed to send comment", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void fetchComments() {
        comments.orderBy("timestamp")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null) {
                                List<DocumentSnapshot> documentSnapshots = querySnapshot.getDocuments();
                                for (DocumentSnapshot documentSnapshot : documentSnapshots) {
                                    String message = documentSnapshot.getString("message");
                                    String cityName = documentSnapshot.getString("city");
                                    addcomment(message);
                                }
                            }
                        } else {
                            // Error occurred while fetching comments
                            Toast.makeText(aucmap.this, "Failed to fetch comments", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }



    private void addcomment(String message) {
        View commentView = getLayoutInflater().inflate(R.layout.post_item, null);
        TextView messageTextView = commentView.findViewById(R.id.messageTextView);
        TextView dateTextView = commentView.findViewById(R.id.dateTextView);
        TextView timeTextView = commentView.findViewById(R.id.timeTextView);

        // Set the comment message and other details
        messageTextView.setText(message);
        // Set the date and time (if needed)

        // Add the click listener to the comment view
        commentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(aucmap.this, post_holder_acti.class);
                intent.putExtra("message", message);
                startActivity(intent);
            }
        });

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        layoutParams.setMargins(0, 0, 0, 10);
        commentView.setLayoutParams(layoutParams);
        // Add the comment view to the messageContainer LinearLayout
        messageContainer.addView(commentView, 0);
    }


    private void addcomment1(String message) {


        // Create a query to fetch the most recent comment from the crmessages subcollection
        comments.limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                                String latestMessage = document.getString("message");

                                // Create the comment view and add it to the messageContainer LinearLayout
                                View commentView = getLayoutInflater().inflate(R.layout.post_item, null);
                                TextView messageTextView = commentView.findViewById(R.id.messageTextView);
                                TextView dateTextView = commentView.findViewById(R.id.dateTextView);
                                TextView timeTextView = commentView.findViewById(R.id.timeTextView);

                                // Set the comment message and other details
                                messageTextView.setText(latestMessage);
                                // Set the date and time (if needed)

                                // Add the comment view to the messageContainer LinearLayout
                                messageContainer.addView(commentView, 0);
                            } else {
                                // No comments found
                                Toast.makeText(aucmap.this, "No comments found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Error occurred while fetching the most recent comment
                            Toast.makeText(aucmap.this, "Failed to fetch the most recent comment", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

}
