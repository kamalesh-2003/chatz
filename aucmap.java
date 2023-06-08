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
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class aucmap extends AppCompatActivity {

    private FusedLocationProviderClient mFusedLocationClient;
    private TextView cityTextView;
    private EditText messageEditText;
    private Button sendButton;
    private ImageButton picsendbutton;
    private LinearLayout messageContainer;
    private static final int PERMISSION_ID = 44;
    private int SELECT_PICTURE = 200;
    // Firebase Firestore
    private FirebaseFirestore firestore;
    private CollectionReference comments,picsbase;

    private FirebaseStorage storage;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aucmap);
        cityTextView = findViewById(R.id.cityTextView);
        messageEditText = findViewById(R.id.commentEditText);
        sendButton = findViewById(R.id.postCommentButton);
        messageContainer = findViewById(R.id.messageContainer);
        picsendbutton = findViewById(R.id.picsend);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

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
        picsendbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = messageEditText.getText().toString().trim();
                picupload();
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

        // Get the current date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date currentDate = new Date();
        String date = dateFormat.format(currentDate);
        String time = timeFormat.format(currentDate);

        // Add date and time to the comment data
        commentData.put("date", date);
        commentData.put("time", time);

        comments.add(commentData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Comment sent successfully
                        // Retrieve the comment message
                        String message = messageEditText.getText().toString().trim();

                        // Add the comment dynamically to the ScrollView
                        addcomment(message, date, time);
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
        comments
                .orderBy("time", Query.Direction.ASCENDING)
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
                                    String date = documentSnapshot.getString("date");
                                    String time = documentSnapshot.getString("time");
                                    addcomment(message, date, time);
                                }
                            }
                        } else {
                            // Error occurred while fetching comments
                            Toast.makeText(aucmap.this, "Failed to fetch comments", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void addcomment(String message, String date, String time) {
        View commentView = getLayoutInflater().inflate(R.layout.post_item, null);
        TextView messageTextView = commentView.findViewById(R.id.messageTextView);
        TextView dateTextView = commentView.findViewById(R.id.dateTextView);
        TextView timeTextView = commentView.findViewById(R.id.timeTextView);

        // Set the comment message, date, and time
        messageTextView.setText(message);
        dateTextView.setText(date);
        timeTextView.setText(time);

        // Add the click listener to the comment view
        commentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send the message to another activity
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

    private void picupload() {
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        // pass the constant to compare it
        // with the returned requestCode
        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            // compare the resultCode with the
            // SELECT_PICTURE constant
            if (requestCode == SELECT_PICTURE) {
                // Get the url of the image from data
                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {
                    String imageName = UUID.randomUUID().toString();
                    final StorageReference imageRef = storage.getReference().child("images").child(imageName);

                    // Upload the image to Firebase Storage
                    imageRef.putFile(selectedImageUri)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // Image uploaded successfully
                                    // Retrieve the download URL of the image
                                    imageRef.getDownloadUrl()
                                            .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                @Override
                                                public void onSuccess(Uri uri) {
                                                    // Download URL received
                                                    String imageURL = uri.toString();
                                                    // Process the image URL (e.g., add to the Firestore document)
                                                    // ...
                                                    Toast.makeText(aucmap.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                                                    View commentView = getLayoutInflater().inflate(R.layout.pic_post_item, null);
                                                    TextView messageTextView = commentView.findViewById(R.id.messageTextView);
                                                    TextView dateTextView = commentView.findViewById(R.id.dateTextView);
                                                    TextView timeTextView = commentView.findViewById(R.id.timeTextView);
                                                    ImageView imgpicpost = commentView.findViewById(R.id.pic1);

                                                    imgpicpost.setImageURI(selectedImageUri);
                                                    String message= messageEditText.getText().toString().trim();
                                                    if(!message.isEmpty()){
                                                        messageTextView.setText(message);
                                                        messageEditText.setText("");
                                                    }
                                                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                                    );

                                                    layoutParams.setMargins(0, 0, 0, 10);
                                                    commentView.setLayoutParams(layoutParams);

                                                    // Add the comment view to the messageContainer LinearLayout
                                                    messageContainer.addView(commentView, 0);
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    // Failed to retrieve download URL
                                                    Toast.makeText(aucmap.this, "Failed to retrieve image URL", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Failed to upload image
                                    Toast.makeText(aucmap.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        }
    }
}
