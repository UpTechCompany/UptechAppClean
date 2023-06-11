package com.example.uptechapp.dao;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.uptechapp.R;
import com.example.uptechapp.api.CompleteListener;
import com.example.uptechapp.api.EmergencyApiService;
import com.example.uptechapp.databinding.DialogFragmentBinding;
import com.example.uptechapp.model.Emergency;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapService implements OnMapReadyCallback, GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener{
    private static final String TAG = "MapService";
    private final Context context;
    private Activity activity;
    private List<Emergency> myEmergencyList;
    private Uri uriImage;
    private StorageReference storageReference;
    private ActivityResultLauncher<String> mGetContent;
    private Dialog dialog;
    private LatLng location;

    private TextView editTextLabel;
    private Button btnChoose;
    private TextView editTextDesc;
    private Button btnShare;
    private ImageView emergencyImg;




    public MapService(Context context, Activity activity, ActivityResultLauncher<String> mGetContent) {
        this.context = context;
        this.activity = activity;
        this.mGetContent = mGetContent;
        storageReference = FirebaseStorage.getInstance().getReference("Emergency");
        myEmergencyList = MyViewModel.getInstance().getEmergencyLiveData().getValue();
    }


    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        Toast.makeText(context, latLng.latitude + " "
                + latLng.longitude, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        location = latLng;

        dialog = new Dialog(context);
        dialog.setContentView(R.layout.fragment_create_emergency);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(R.drawable.round_view));
        dialog.show();

        editTextLabel = dialog.getWindow().findViewById(R.id.editTextLabel);
        btnChoose = dialog.getWindow().findViewById(R.id.btnChoosePicture);
        editTextDesc = dialog.getWindow().findViewById(R.id.editTextDescription);
        btnShare = dialog.getWindow().findViewById(R.id.btnShare);
        emergencyImg = dialog.getWindow().findViewById(R.id.emergencyImg);

        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareEmergency();
            }
        });
    }

    private void openFileChooser() {
        mGetContent.launch("image/*");
    }

    public void setImage(Uri uri) {
        uriImage = uri;
        emergencyImg.setImageURI(uriImage);
    }

    private String getFileExtension(Uri uriImage) {
        ContentResolver contentResolver = activity.getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uriImage));
    }

    private void shareEmergency() {
        if (uriImage != null) {

            int id = 1;

            StorageReference fileReference = storageReference.child(String.valueOf(id) + "/Photo." + getFileExtension(uriImage));

            fileReference.putFile(uriImage).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Uri downloadUri = uri;

                            String url = downloadUri.toString();
                            String[] time = new String[0];
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                time = LocalDateTime.now(ZoneOffset.UTC).toString().substring(0, 16).split("T");
                            }
                            Log.i("time", "Time" + Arrays.toString(time));
                            String emTime = time[1] + " " + time[0].substring(8) + "." + time[0].substring(5,7) + "." + time[0].substring(0,4);
                            Log.d("time", emTime);
                            Emergency emergency = null;
                            emergency = new Emergency(
                                    "-1",
                                    editTextLabel.getText().toString(),
                                    editTextDesc.getText().toString(),
                                    emTime,
                                    url,
                                    11,
                                    11
                            );


                            EmergencyApiService.getInstance().postJson(emergency).enqueue(new Callback<Emergency>() {
                                @Override
                                public void onResponse(@NonNull Call<Emergency> call, @NonNull Response<Emergency> response) {
                                    Log.i(TAG, "Response - " + call.toString());
                                }

                                @Override
                                public void onFailure(@NonNull Call<Emergency> call, @NonNull Throwable t) {
                                    Log.i(TAG, "FAIL - " + t.getMessage());
                                }
                            });

                        }
                    });
                    Navigation.findNavController(activity, R.id.mainFragmentContainer).navigate(R.id.fragment_emergency_feed);
                    dialog.hide();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(context, "File was not selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.setOnMapClickListener(this);
        googleMap.setOnMapLongClickListener(this);
//
//        for (Emergency emergency: myEmergencyList) {
//            emergency.setLocation(emergency.getLattitude(), emergency.getLongitude());
//            googleMap.addMarker(new MarkerOptions().position(emergency.getLocation()).title(emergency.getTitle()));
//        }


        Log.i(TAG, "eml before click: " + MyViewModel.getInstance().getEmergencyLiveData().getValue());
        googleMap.setOnMarkerClickListener(marker -> {
            Log.i(TAG, "eml after click: " + MyViewModel.getInstance().getEmergencyLiveData().getValue());
            Emergency emergency = Database.getEmergencyByTitle(marker.getTitle(), MyViewModel.getInstance().getEmergencyLiveData().getValue());

            Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.dialog_fragment);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            TextView tv_name = dialog.getWindow().findViewById(R.id.tv_name);
            TextView tv_time = dialog.getWindow().findViewById(R.id.tv_time);
            TextView tv_info = dialog.getWindow().findViewById(R.id.tv_description);

            tv_name.setText(emergency.getTitle());
            tv_info.setText(emergency.getDescription());
            tv_time.setText(emergency.getTime().toString());


            ImageView imageView = dialog.getWindow().findViewById(R.id.iv_image);
            Log.i(TAG, "photo url: " + emergency.getPhotoUrl());
            Glide.with(context).load(emergency.getPhotoUrl()).into(imageView);

            return false;
        });
    }
}
