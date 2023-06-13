package com.example.auctioneer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import com.google.android.gms.ads.AdView;


import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class post_holder_acti extends AppCompatActivity {
    private FusedLocationProviderClient mFusedLocationClient;
    private TextView cityTextView;
    private EditText messageEditText;
    private ImageButton sendButton1;
    private LinearLayout messageContainer,topcontainer;
    private static final int PERMISSION_ID = 44;
    Intent collectdat;
    String message1;
    String date;
    String time;
    String boxtest;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageButton sendButton;
    private AdView mAdView;
    ScrollView scrollty;
    private ImageButton picsendbutton;
    private int SELECT_PICTURE = 200;
    // Firebase Firestore
    private FirebaseFirestore firestore;
    private CollectionReference comments;

    private FirebaseStorage storage;
    private StorageReference storageRef;
    Uri selectedImageUri;
    String downloadUrl;


    // Firebase Firestore
    private CollectionReference comments1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_holder);
        scrollty = findViewById(R.id.scroll1);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshPage();
            }
        });

        scrollty.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (scrollty.getScrollY() == 0) {
                    swipeRefreshLayout.setEnabled(true);
                } else {
                    swipeRefreshLayout.setEnabled(false);
                }
            }
        });

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Perform the refresh action here
                refreshPage();
            }
        });
        cityTextView = findViewById(R.id.cityTextView);
        messageEditText = findViewById(R.id.commentEditText);
        sendButton1 = findViewById(R.id.postCommentButton);
        messageContainer = findViewById(R.id.messageContainer);
        topcontainer = findViewById(R.id.topLinearLayout);
        picsendbutton = findViewById(R.id.picsend);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firestore = FirebaseFirestore.getInstance();
        collectdat =getIntent();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        message1 = collectdat.getStringExtra("message");
        date = collectdat.getStringExtra("date");
        time = collectdat.getStringExtra("time");
        boxtest = collectdat.getStringExtra("boxtest");
        comments1 = firestore.collection("crsubcomments")
                .document(message1).collection("crms");
        chekcerfunc(message1);
        fetchsubComments();


        getLastLocation();

        sendButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageEditText.getText().toString().trim();
                if (!message.isEmpty()) {
                        sendComment(message);
                        messageEditText.setText("");
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
                                Geocoder geocoder = new Geocoder(post_holder_acti.this, Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    if (addresses.size() > 0) {
                                        String cityName = addresses.get(0).getLocality();

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

        // Retrieve the comment message from the EditText
        String commentMessage = messageEditText.getText().toString().trim();

        if (!commentMessage.isEmpty()) {
            // Create a new comment document in Firestore
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("message", commentMessage);

            comments1.add(commentData)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            // Comment sent successfully
                            // Add the comment dynamically to the ScrollView
                            addcomment1(commentMessage);
                            messageEditText.setText("");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Failed to send comment
                            Toast.makeText(post_holder_acti.this, "Failed to send comment", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


    private void fetchsubComments() {
        comments1.get()
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
                                    String boxtext = documentSnapshot.getString("boxtext");
                                    String regex = "\\bhttps\\b";

                                    // Create a Pattern object
                                    Pattern pattern = Pattern.compile(regex);

                                    // Create a Matcher object
                                    Matcher matcher = pattern.matcher(message);
                                    if (matcher.find()) {
                                        Uri pichan= Uri.parse(message);
                                        picadd(pichan, date, time,boxtext);
                                    } else {
                                        addcomment1(message);
                                    }
                                }
                            }
                        } else {
                            // Error occurred while fetching comments
                            Toast.makeText(post_holder_acti.this, "Failed to fetch comments", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    private void chekcerfunc(String messaget){
        String regex = "\\bhttps\\b";

        // Create a Pattern object
        Pattern pattern = Pattern.compile(regex);

        // Create a Matcher object
        Matcher matcher = pattern.matcher(messaget);
        if (matcher.find()) {
            Uri pichan= Uri.parse(messaget);
            picadd(pichan, date, time,boxtest);
        } else {
            addComment(messaget);
        }
    }

    private void addComment(String message) {
        View commentView = getLayoutInflater().inflate(R.layout.post_item, null);
        TextView messageTextView = commentView.findViewById(R.id.messageTextView);
        TextView dateTextView = commentView.findViewById(R.id.dateTextView);
        TextView timeTextView = commentView.findViewById(R.id.timeTextView);

        // Set the comment message and other details
        messageTextView.setText(message);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        layoutParams.setMargins(0, 10, 0, 10);
        commentView.setLayoutParams(layoutParams);

        // Add the comment view to the messageContainer LinearLayout

        topcontainer.addView(commentView, 0);
    }
    private void addcomment1(String message) {
        View commentView = getLayoutInflater().inflate(R.layout.post_item, null);
        TextView messageTextView1 = commentView.findViewById(R.id.messageTextView);
        TextView dateTextView = commentView.findViewById(R.id.dateTextView);
        TextView timeTextView = commentView.findViewById(R.id.timeTextView);

        // Set the comment message and other details
        messageTextView1.setText(message);
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
        i.setAction(Intent.ACTION_OPEN_DOCUMENT);

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
                selectedImageUri = data.getData();
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
                                                    downloadUrl = uri.toString();
                                                    Toast.makeText(post_holder_acti.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                                                    Map<String, Object> commentData = new HashMap<>();
                                                    commentData.put("message", downloadUrl);
                                                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                                                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                                                    String boxtext = messageEditText.getText().toString();
                                                    Date currentDate = new Date();
                                                    String date = dateFormat.format(currentDate);
                                                    String time = timeFormat.format(currentDate);
                                                    commentData.put("date", date);
                                                    commentData.put("time", time);
                                                    commentData.put("boxtext",boxtext);
                                                    comments1.add(commentData)
                                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                                @Override
                                                                public void onSuccess(DocumentReference documentReference) {
                                                                    picadd(selectedImageUri,date,time,boxtext);
                                                                }
                                                            });

                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    // Failed to retrieve download URL
                                                    Toast.makeText(post_holder_acti.this, "Failed to retrieve image URL", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Failed to upload image
                                    Toast.makeText(post_holder_acti.this, "Failed to upload image", Toast.LENGTH_SHORT).show();

                                }
                            });
                }
            }
        }
    }
    private void picadd(Uri selectedImageUri, String date, String time,String boxtest) {
        View commentView = getLayoutInflater().inflate(R.layout.pic_post_item, null);
        TextView messagetextView = commentView.findViewById(R.id.messageTextView);
        TextView dateTextView = commentView.findViewById(R.id.dateTextView);
        TextView timeTextView = commentView.findViewById(R.id.timeTextView);
        ImageView imgpicpost = commentView.findViewById(R.id.pic1);
        String message = messageEditText.getText().toString().trim();
        if(!message.isEmpty()){
            messagetextView.setText(boxtest);
        }

        dateTextView.setText(date);
        timeTextView.setText(time);
        Picasso.get().load(selectedImageUri).into(imgpicpost);
        messageEditText.setText("");

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        layoutParams.setMargins(0, 0, 0, 10);
        commentView.setLayoutParams(layoutParams);

        messageContainer.addView(commentView, 0);
    }

    private void refreshPage() {

        // Simulate a delay before stopping the refresh animation
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Update your data or views here
                // Stop the refresh animation
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 2000); // Simulate a 2-second delay, replace with your actual logic
    }

}
