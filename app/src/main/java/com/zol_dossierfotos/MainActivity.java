// Package name.
package com.zol_dossierfotos;

// Imports from official sources.
import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URL;

// Import external sources(ZXing, Glide, Cropper classes).
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

public class MainActivity extends AppCompatActivity implements OnClickListener,
        OnItemSelectedListener {

    private static final String MIRTH_SERVERLINK = "mirthURL";

    // Declarations of our variables and constants.
    private static final int REQUEST_MULTIPLE_PERMISSIONS = 201;
    private static final int BARCODE_REQUEST = 0x0000c0de;
    private static final int CAMERA_REQUEST = 202;
    private static final int CROP_IMAGE_ACTIVITY_REQUEST_CODE = 203;

    protected static final String PREFS_NAME = "FotoCategorie";
    protected SharedPreferences fotoCategorie;

    private int indexPhoto;
    private Boolean permissionsGranted;
    private String imgPath;
    private Patient patient;
    private List<Photo> photos;
    private ArrayAdapter<Category> adapterCategories;
    private ArrayList<Category> categoryList;

    private RVAdapter adapter;
    private TextView patientNaam, patientInfo, photoIndex;
    private ImageView infoButton, imageView, editButton, deleteButton;
    private Spinner categorieSpinner;
    private RelativeLayout rlSpinner;
    private LinearLayout photoButton, uploadButton, leftButton, rightButton;
    private RecyclerView rv;
    private PowerManager.WakeLock wakeLock;

    // Handles starting the activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout.
        setContentView(R.layout.activity_main);

        // Get UI elements out of the view.
        patientNaam = (TextView) this.findViewById(R.id.patientNaam);
        patientInfo = (TextView) this.findViewById(R.id.patientInfo);
        infoButton = (ImageView) this.findViewById(R.id.info_button);
        ImageView logout = (ImageView) this.findViewById(R.id.logout);
        photoIndex = (TextView) this.findViewById(R.id.photo_index);
        imageView = (ImageView) this.findViewById(R.id.photo_view);
        editButton = (ImageView)this.findViewById(R.id.edit_image);
        deleteButton = (ImageView) this.findViewById(R.id.delete_image);
        leftButton = (LinearLayout) this.findViewById(R.id.left_image);
        rightButton = (LinearLayout) this.findViewById(R.id.right_image);
        categorieSpinner = (Spinner) this.findViewById(R.id.categorieSpinner);
        rlSpinner = (RelativeLayout) this.findViewById(R.id.rlSpinner);
        rv = (RecyclerView) this.findViewById(R.id.rv);
        LinearLayout scanButton = (LinearLayout) this.findViewById(R.id.scan_button);
        photoButton = (LinearLayout) this.findViewById(R.id.camera_button);
        uploadButton = (LinearLayout) this.findViewById(R.id.upload_button);

        // Set the layout for our recyclerview, for this we use a horizontal recyclerview.
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rv.setHasFixedSize(true);

        // Create our ArrayList<> as dataset for our adapter that is used in the recyclerview.
        photos = new ArrayList<>();
        adapter = new RVAdapter(photos, this);
        rv.setAdapter(adapter);

        // Create and set array adapter for specified array found in strings.xml,
        // set the layout and listener for the spinner.
        categoryList = new ArrayList<>();
        categoryList.add(new Category("","Selecteer type foto:"));
        adapterCategories = new ArrayAdapter<>(this, R.layout.spinner_item, categoryList);
        adapterCategories.setDropDownViewResource(R.layout.spinner_item);
        categorieSpinner.setAdapter(adapterCategories);
        categorieSpinner.setOnItemSelectedListener(this);

        // Set ClickListeners for buttons.
        infoButton.setOnClickListener(this);
        logout.setOnClickListener(this);
        editButton.setOnClickListener(this);
        deleteButton.setOnClickListener(this);
        scanButton.setOnClickListener(this);
        photoButton.setOnClickListener(this);
        uploadButton.setOnClickListener(this);
        leftButton.setOnClickListener(this);
        rightButton.setOnClickListener(this);

        // Set default value and already check if permissions are set and still active.
        permissionsGranted = false;
        checkAndRequestPermissions();

        // Get our sharedPreferences.
        fotoCategorie = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Default state for our buttons when loaded.
        categorieSpinner.setSelection(0);
        categorieSpinner.setEnabled(false);
        rlSpinner.setBackgroundColor(Color.parseColor("#808080"));
        photoButton.setClickable(false);
        photoButton.setBackgroundColor(Color.parseColor("#808080"));
        uploadButton.setClickable(false);
        uploadButton.setBackgroundColor(Color.parseColor("#808080"));

        // Set the swipe listener for our imageView.
        imageView.setOnTouchListener(new OnSwipeTouchListener(this) {

            @Override
            public void onSwipeLeft() {
                // Move to next image on the right if not already the last.
                if (photos.size() > 1 && indexPhoto < (photos.size() - 1)) {
                    indexPhoto = indexPhoto + 1;
                    // Display the image and set the buttons.
                    displaySelectedPhoto(indexPhoto);
                    // Highlight the image in the recyclerView.
                    adapter.notifyItemChanged(adapter.selectedPosition);
                    adapter.selectedPosition = (indexPhoto);
                    adapter.notifyItemChanged(adapter.selectedPosition);
                    // Scroll to the index.
                    rv.scrollToPosition(indexPhoto);
                }
            }

            @Override
            public void onSwipeRight() {
                // Move to next image on the left if not already the first.
                if (photos.size() > 1 && indexPhoto > 0) {
                    indexPhoto = indexPhoto - 1;
                    // Display the image and set the buttons.
                    displaySelectedPhoto(indexPhoto);
                    // Highlight the image in the recyclerView.
                    adapter.notifyItemChanged(adapter.selectedPosition);
                    adapter.selectedPosition = (indexPhoto);
                    adapter.notifyItemChanged(adapter.selectedPosition);
                    // Scroll to the index.
                    rv.scrollToPosition(indexPhoto);
                }
            }

        });

        // Check if the bundle contains our key, if so remove it and launch the scanner.
        if(getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();

            try {
                if (extras.getInt("key") == 1) {
                    getIntent().removeExtra("key");
                    scanButton.callOnClick();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    // Handles press of the back button.
    @Override
    public void onBackPressed() {
        // Create a dialog for logout, if positive goto login else cancel the dialog.
        new AlertDialog.Builder(this,R.style.AlertDialogTheme)
                .setMessage("Wilt u uitloggen?")
                .setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                dialog.dismiss();
                                fotoCategorie.edit().clear().apply();
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }
                        })
                .setNegativeButton("Annuleer",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                dialog.dismiss();
                            }
                        })
                .create()
                .show();
    }

    // Handles the click events from the view.
    public void onClick(View v) {
        // Check which button is triggered.
        switch (v.getId()) {

            // Info button triggered.
            case R.id.info_button:
                if (checkAndRequestPermissions()) {
                    if (permissionsGranted) {
                        // Show patient information.
                        new AlertDialog.Builder(this,R.style.AlertDialogTheme)
                                .setMessage("Opnamenummer: " + patient.getVisitId() + "\n" +
                                        "Patiëntennummer: " + patient.getPatientId() + "\n" +
                                        "Naam: " + patient.getLastName() + "\n" +
                                        "Voornaam: " + patient.getFirstName() + "\n" +
                                        "Geslacht: " + patient.getSex() + "\n" +
                                        "Geboortedatum: " + patient.getBirthDate() + "\n" +
                                        "Leeftijd: " + patient.getAge() + " jaar")
                                .setTitle("Informatie patiënt:")
                                .setNegativeButton("Sluiten",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                dialog.dismiss();
                                            }
                                        })
                                .create()
                                .show();

                    }
                }

                break;

            // Logout button triggered.
            case R.id.logout:
                if (checkAndRequestPermissions()) {
                    if (permissionsGranted) {
                        // Handle logout.
                        this.onBackPressed();
                    }
                }

                break;

            // Edit button triggered.
            case R.id.edit_image:
                if (checkAndRequestPermissions()) {
                    if (permissionsGranted) {
                        // Edit the selected image from the filepath.
                        CropImage.activity(Uri.fromFile(new File(
                                photos.get(indexPhoto).getPhotoPath())))
                                .setCropShape(CropImageView.CropShape.RECTANGLE)
                                // Set the crop shape.
                                .setSnapRadius(5)           // Sets the offset for the crop shape.
                                .setGuidelines(CropImageView.Guidelines.ON) // Sets guidelines.
                                .setShowCropOverlay(true)   // Show an overlay for ease of use.
                                .setAutoZoomEnabled(true)   // Automatically recenter the view.
                                .setMultiTouchEnabled(true) // Enable multitouch gestures
                                .setMaxZoom(4)              // The fraction the crop view will take.
                                .setFixAspectRatio(false)   // Chose to fix the aspect ratio.
                                .setBorderLineThickness(5)  // Set the thickness of the crop shape.
                                .setBackgroundColor(Color.argb(170,0,177,195))
                                // Set color of the background (ZOL blue)
                                .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                                // PNG format default JPEG
                                .setOutputCompressQuality(100)  // Set the quality level to (#)
                                .start(this);

                    }
                }

                break;

            // Delete button triggered.
            case R.id.delete_image:
                if (checkAndRequestPermissions()) {
                    if (permissionsGranted) {
                        // Delete the shown image.
                        deletePhoto();

                    }
                }

                break;

            // Left button triggered.
            case R.id.left_image:

                if (photos.size()>1 && indexPhoto > 0) {
                    indexPhoto = indexPhoto - 1;
                    // Display the image and set the buttons.
                    displaySelectedPhoto(indexPhoto);
                    // Highlight the image in the recyclerView.
                    adapter.notifyItemChanged(adapter.selectedPosition);
                    adapter.selectedPosition = (indexPhoto);
                    adapter.notifyItemChanged(adapter.selectedPosition);
                    rv.scrollToPosition(indexPhoto);
                }

                break;

            // Right button triggered.
            case R.id.right_image:

                if (photos.size()>1 && indexPhoto < (photos.size()-1)) {
                    indexPhoto = indexPhoto + 1;
                    // Display the image and set the buttons.
                    displaySelectedPhoto(indexPhoto);
                    // Highlight the image in the recyclerView.
                    adapter.notifyItemChanged(adapter.selectedPosition);
                    adapter.selectedPosition = (indexPhoto);
                    adapter.notifyItemChanged(adapter.selectedPosition);
                    rv.scrollToPosition(indexPhoto);
                }

                break;


            // Scan button triggered.
            case R.id.scan_button:
                if (checkAndRequestPermissions()) {
                    if (permissionsGranted) {
                        // If there are photo(s) taken show the message
                        // that they can delete the photo(s) or leave them for a max of 5 patients.
                        if (photos.size()!=0) {
                            new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                                    .setMessage("Foto's voor patiënt met opnamenummer " +
                                            patient.getVisitId() +
                                            " zijn niet opgeladen, wilt u deze verwijderen? " +
                                            "Indien u bewaar selecteert blijven de fotos " +
                                            "opgeslagen. (maximum 5 patiënten)")
                                    .setPositiveButton("Verwijderen",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int id) {
                                                    dialog.dismiss();
                                                    cleanImageDirectory();
                                                    Intent intent = new Intent(MainActivity.this,
                                                            MainActivity.class);
                                                    Bundle bundle = new Bundle();
                                                    bundle.putInt("key", 1);
                                                    intent.putExtras(bundle);
                                                    startActivity(intent);
                                                    MainActivity.this.finish();
                                                }
                                            })
                                    .setNegativeButton("Bewaar",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int id) {
                                                    dialog.dismiss();
                                                    Intent intent = new Intent(MainActivity.this,
                                                            MainActivity.class);
                                                    Bundle bundle = new Bundle();
                                                    bundle.putInt("key", 1);
                                                    intent.putExtras(bundle);
                                                    startActivity(intent);
                                                    MainActivity.this.finish();
                                                }
                                            })
                                    .setNeutralButton("Sluiten",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int id) {
                                                    dialog.dismiss();
                                                }
                                            })
                                    .create()
                                    .show();

                        }
                        else {
                            // Building ZXing barcode and QR intent.
                            IntentIntegrator integrator = new IntentIntegrator(this);
                            integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
                            // Only barcodes.
                            integrator.setCameraId(0);              // Just the rear camera.
                            integrator.setPrompt("Scan de barcode van het polsbandje");
                            // Set prompt message.
                            integrator.setBeepEnabled(true);       // Beep when scanned.

                            integrator.initiateScan();

                        }

                    }
                }

                break;

            // Camera button triggered.
            case R.id.camera_button:
                if (checkAndRequestPermissions()) {
                    if (permissionsGranted) {
                        // Building cameraIntent.
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        // Add an output URI so that we can easily find the photo later regardless
                        // of the device.
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, setImageUri());
                        // Protection that our app can use the camera.
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(takePictureIntent, CAMERA_REQUEST);
                        }

                    }
                }

                break;

            // Upload button triggered.
            case R.id.upload_button:
                if (checkAndRequestPermissions()) {
                    if (permissionsGranted) {

                        Boolean categorieSet = true;
                        Boolean encodedSet = true;
                        Integer indexCat = 0;
                        Integer indexCoded = 0;
                        ArrayList<Integer> indexesCat = new ArrayList<>();
                        ArrayList<Integer> indexesCoded = new ArrayList<>();

                        // Make sure all photo(s) categories are set.
                        for (Photo photo: photos) {
                            indexCat++;
                            if (photo.categorieKey == null || photo.categorieKey.equals("")) {
                                indexesCat.add(indexCat);
                                categorieSet = false;
                            }
                        }

                        // Categories for one or more photo(s) aren't set show a popup with the
                        // indexes so the user can see which ones are missing.
                        if (!categorieSet) {

                            StringBuilder stringBuilder = new StringBuilder();
                            String notSet;

                            for (Integer photoIndex : indexesCat) {
                                stringBuilder.append(Integer.toString(photoIndex));
                                stringBuilder.append(", ");
                            }

                            notSet = stringBuilder.toString();
                            notSet = notSet.substring(0, notSet.length() - 2);
                            // Show a dialog that they need to set the category.
                            new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme)
                                    .setMessage("U moet voor elke foto een categorie selecteren. " +
                                            "(Categorieën voor foto: " + notSet + " ontbreken)")
                                    .setNegativeButton("Sluiten",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int id) {
                                                    dialog.dismiss();
                                                }
                                            })
                                    .create()
                                    .show();

                            uploadButton.setClickable(true);

                            break;

                        }

                        // Make sure all photo(s) are encoded.
                        for (Photo photo: photos) {
                            indexCoded++;
                            if (photo.encodedB64 == null || photo.encodedB64.equals("") ||
                                    photo.angle == null || photo.angle.equals("")) {
                                indexesCoded.add(indexCoded);
                                encodedSet = false;
                            }
                        }

                        if (!encodedSet) {

                            StringBuilder stringBuilder = new StringBuilder();
                            String notSet;

                            for (Integer photoIndex : indexesCoded) {
                                stringBuilder.append(Integer.toString(photoIndex));
                                stringBuilder.append(", ");
                            }

                            notSet = stringBuilder.toString();
                            notSet = notSet.substring(0, notSet.length() - 2);
                            // Show a dialog that they need to set the category.
                            new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme)
                                    .setMessage("De foto's zijn nog niet allemaal verwerkt." +
                                            "(Foto('s) "+notSet+ " zijn nog aan het verwerken.)")
                                    .setNegativeButton("Sluiten",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int id) {
                                                    dialog.dismiss();
                                                }
                                            })
                                    .create()
                                    .show();

                            uploadButton.setClickable(true);

                            break;

                        }
                        // Categories are all set and photo(s) are all coded.
                        else {
                            uploadButton.setClickable(false);

                            try {
                                if (new checkServiceAvailable().execute(MIRTH_SERVERLINK).get()) {
                                    // Show a dialog if they want to upload photo(s)
                                    // for the patient <NAME>.
                                    new AlertDialog.Builder(MainActivity.this,
                                            R.style.AlertDialogTheme)
                                            .setMessage("Foto's uploaden voor patiënt: "+
                                                    patient.getLastName()+", "+
                                                    patient.getFirstName()+"?")
                                            .setPositiveButton("Ok",
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog,
                                                                            int id) {
                                                            dialog.dismiss();
                                                            new uploadPhotos().execute();
                                                        }
                                                    })
                                            .setNegativeButton("Annuleren",
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog,
                                                                            int id) {
                                                            dialog.dismiss();
                                                        }
                                                    })
                                            .create()
                                            .show();

                                } else

                                    new AlertDialog.Builder(this,R.style.AlertDialogTheme)
                                            .setMessage("Service niet bereikbaar.")
                                            .setNegativeButton("Sluiten",
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog,
                                                                            int id) {
                                                            dialog.dismiss();
                                                        }
                                                    })
                                            .create()
                                            .show();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            uploadButton.setClickable(true);

                        }

                    }

                }

                break;

            default:

                break;

        }

    }

    // Checks for permissions if they are set or not and sets permissionsGranted accordingly.
    private boolean checkAndRequestPermissions() {

        // Reads permissions from the manifest file.
        int permissionWriteStorage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionReadStorage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionCamera = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);

        List<String> listPermissionsNeeded = new ArrayList<>();

        // Checks for each permission if they are granted by the system, if not they are written
        // to listPermissionsNeeded.
        if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionReadStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }

        // If the list is empty, continue because the permissions are there.
        // Else we need to request them.
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(
                    new String[listPermissionsNeeded.size()]),REQUEST_MULTIPLE_PERMISSIONS);
            return false;
        }

        // Permissions are there, set permissionsGranted to TRUE.
        permissionsGranted = true;
        return true;
    }

    // Handles the result from the permission requests to the system.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_MULTIPLE_PERMISSIONS) {
            Map<String, Integer> perms = new HashMap<>();
            // Initialize the map with the permissions.
            perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    PackageManager.PERMISSION_GRANTED);
            perms.put(Manifest.permission.READ_EXTERNAL_STORAGE,
                    PackageManager.PERMISSION_GRANTED);
            perms.put(Manifest.permission.CAMERA,
                    PackageManager.PERMISSION_GRANTED);
            // Fill with actual results from user.
            if (grantResults.length > 0) {
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for all permissions.
                if (perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED) {
                    // Continue with the program
                    // else any one or multiple the permissions are not granted.
                    permissionsGranted = true;
                    // Create our storage directory for the patients.
                    createStorageDir();
                } else {
                    // Permission is denied (this is the first time,
                    // when "never ask again" is not checked).
                    // So ask again explaining the usage of permission
                    // shouldShowRequestPermissionRationale will return true,
                    // show the dialog or snackbar saying its necessary,
                    // and try again otherwise proceed with setup.
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            || ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)
                            || ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.CAMERA)) {
                                // Show the dialog.
                                new AlertDialog.Builder(MainActivity.this,
                                        R.style.AlertDialogTheme)
                                        .setMessage("Deze permissie is nodig voor de juiste " +
                                                "werking van de app")
                                        .setPositiveButton("Ok",
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog,
                                                                        int id) {
                                                        checkAndRequestPermissions();
                                                        dialog.dismiss();
                                                    }
                                                })
                                        .setNegativeButton("Annuleren",
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog,
                                                                        int id) {
                                                        permissionsGranted = false;
                                                        dialog.dismiss();
                                                    }
                                                })
                                        .create()
                                        .show();

                    }
                    // Permission is denied (and never ask again is checked),
                    // shouldShowRequestPermissionRationale will return false.
                    else {
                        // Show the dialog that the user must grant permissions
                        // in the system settings.
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this,
                                R.style.AlertDialogTheme);
                        builder.setMessage("Voor de juiste werking van de app activeer " +
                                "Camera en Opslag machtiging in: Instellingen > Applicaties " +
                                "> ZOL Dossierfoto's > Machtigingen.")
                                .setTitle("Machtigingen:")
                                .setNegativeButton("Annuleer",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                dialog.dismiss();
                                            }
                                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                }
            }
        }
    }

    // Callback when an intent is finished, so here we check which intent result to handle,
    // and act accordingly.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            // After the barcode intent.
            case BARCODE_REQUEST :
                // Retrieve the result from the barcode(ZXing) intent
                IntentResult resultBarcodeScan = IntentIntegrator.parseActivityResult(
                        requestCode, resultCode, data);

                // Check we have the barcode in the result and that the scanner wasn't cancelled.
                if (resultBarcodeScan != null && resultCode==RESULT_OK) {
                    if (resultBarcodeScan.getFormatName().equals("CODE_128")) {
                        // Handle the barcode result.
                        // Get the patient information from the service.
                        if (patient != null &&
                                !resultBarcodeScan.getContents().equals(patient.getVisitId())) {
                            cleanImageDirectory();
                        }

                        new getJsonForPatient().execute(resultBarcodeScan.getContents());

                    } else
                        // Show a dialog that they need to set the category.
                        new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme)
                                .setMessage("Foutieve barcode.")
                                .setNegativeButton("Sluiten",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                dialog.dismiss();
                                            }
                                        })
                                .create()
                                .show();

                } else {
                    // The scanner was cancelled.
                    super.onActivityResult(requestCode, resultCode, data);
                }

                break;

            // After the camera intent.
            case CAMERA_REQUEST:
                // Check if the intent wasn't cancelled.
                if (resultCode == Activity.RESULT_OK) {

                    // Process the image and place it in the view.
                    Glide.with(this).load(imgPath).fitCenter().into(imageView);
                    editButton.setVisibility(View.VISIBLE);
                    deleteButton.setVisibility(View.VISIBLE);

                    // Add the snapped photo to our adapter for the recyclerview and then notify
                    // the adapter that we added one to the end so the UI updates.
                    photos.add(photos.size(), new Photo(imgPath));
                    adapter.notifyItemInserted(photos.size() - 1);

                    // Re-index.
                    indexPhoto = (photos.size() - 1);

                    if (indexPhoto==0 && photos.size()==1) {
                        leftButton.setVisibility(View.INVISIBLE);
                        rightButton.setVisibility(View.INVISIBLE);
                    } else if (indexPhoto==0) {
                        leftButton.setVisibility(View.INVISIBLE);
                        rightButton.setVisibility(View.VISIBLE);
                    } else if (indexPhoto == photos.size()-1) {
                        rightButton.setVisibility(View.INVISIBLE);
                        leftButton.setVisibility(View.VISIBLE);
                    } else {
                        leftButton.setVisibility(View.VISIBLE);
                        rightButton.setVisibility(View.VISIBLE);
                    }

                    photoIndex.setText((indexPhoto+1)+"/"+photos.size());
                    // Encode the snapped photo.
                    new encodePhoto().execute(imgPath);
                    new photoAngle().execute(imgPath);

                    // Highlight the inserted photo cardView.
                    adapter.notifyItemChanged(adapter.selectedPosition);
                    adapter.selectedPosition = (indexPhoto);
                    adapter.notifyItemChanged(adapter.selectedPosition);
                    rv.scrollToPosition(indexPhoto);

                    // Enable the upload button and category spinner.
                    if (patient.getPatientId()!=null) {
                        uploadButton.setClickable(true);
                        uploadButton.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        categorieSpinner.setEnabled(true);
                        rlSpinner.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    }

                    if (photos.size()>=20) {
                        photoButton.setClickable(false);
                        photoButton.setBackgroundColor(Color.parseColor("#808080"));
                    }
                    //Default spinner selection.
                    categorieSpinner.setSelection(0);

                }

                break;

            // After crop intent.
            case CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                CropImage.ActivityResult resultCrop = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    // Make a file for the cropped photo
                    File cropFile = new File(resultCrop.getUri().getPath());
                    String cropPath = cropFile.getAbsolutePath();
                    Bitmap cropPhoto = BitmapFactory.decodeFile(cropPath);
                    // Set the new path.
                    Photo foto = photos.get(indexPhoto);
                    String oldPath = foto.photoPath;
                    File oldPhoto = new File(oldPath);
                    if (oldPhoto.delete()) {
                        try {
                            FileOutputStream out = new FileOutputStream(oldPath);
                            cropPhoto.compress(Bitmap.CompressFormat.PNG, 100, out);
                            out.flush();
                            out.close();
                            // Display photo.
                            displaySelectedPhoto(indexPhoto);

                            adapter.notifyItemChanged(indexPhoto);
                            // Encode the cropped image.
                            new encodePhoto().execute(oldPath);
                            // Notify the adapter.
                            adapter.notifyItemChanged(indexPhoto);
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                break;

            default:

                break;

        }

    }

    // Creates a directory for our subfolders for the patients.
    private void createStorageDir() {
        // Directory for our patient folders.
        File folder = new File(Environment.getExternalStorageDirectory() +
                "/DCIM/Dossierfotos/");

        boolean success = true;

        if (!folder.exists()) {
            success = folder.mkdir();
        }

        if (!success) {
            new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme)
                    .setMessage("Fout bij aanmaken map voor opslag van de foto's.")
                    .setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    dialog.dismiss();
                                }
                            })
                    .create()
                    .show();

        }

    }

    // Makes a file and returns the URI to write to.
    private Uri setImageUri() {
        // Store photo in DCIM directory under the <visitId> folder.
        File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/Dossierfotos/" +
                patient.getVisitId() + "/" + "image_" + new Date().getTime() + ".png");
        // Get the URI and PATH for the image file, set and return them.
        Uri imgUri = FileProvider.getUriForFile(MainActivity.this,
                BuildConfig.APPLICATION_ID + ".provider", file);

        this.imgPath = file.getAbsolutePath();
        return imgUri;
    }

    // Deletes the photo with the current index.
    private void deletePhoto() {
        // Enable the cameraButton again.
        if (photos.size()>=20) {
            photoButton.setClickable(true);
            photoButton.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
        // Get the path for our photo we need to delete, and create a file for it.
        Photo deletePhoto = photos.get(indexPhoto);
        final File fDelete = new File(deletePhoto.getPhotoPath());
        // If the index is one or higher we display the previous image of our recyclerview,
        // then delete our image we selected to delete.
        if (indexPhoto >= 1) {
            // Get the path to the previous photo in the recyclerview
            // so we can display that one in our large imageview.
            Photo displayImage = photos.get(indexPhoto-1);
            // Check if our file does exist.
            if (fDelete.exists()) {
                try {
                    // Remove the file from our adapterlist, then delete it from our storage,
                    // notify our adapter of the change, set our imageview, lastly re-index.
                    if(fDelete.delete()) {
                        photos.remove(indexPhoto);
                        adapter.notifyItemRemoved(indexPhoto);
                        adapter.notifyItemRangeChanged(indexPhoto, adapter.getItemCount());
                        Glide.with(this).load(displayImage.getPhotoPath())
                                .fitCenter().into(imageView);
                        indexPhoto -= 1;
                        adapter.drawBorder(indexPhoto);
                        if (indexPhoto==0) {
                            leftButton.setVisibility(View.INVISIBLE);
                        }
                        if (indexPhoto==0 && photos.size()==1) {
                            rightButton.setVisibility(View.INVISIBLE);
                        }
                        if (indexPhoto==(photos.size()-1)) {
                            rightButton.setVisibility(View.INVISIBLE);
                        }
                    } // If it failed to delete the photo using the normal way,
                    // the device needs to use the MediaStore method to delete the photo.
                    else {
                        ContentResolver contentResolver = getContentResolver();
                        deleteFileFromMediaStore(contentResolver, fDelete);
                        photos.remove(indexPhoto);
                        adapter.notifyItemRemoved(indexPhoto);
                        adapter.notifyItemRangeChanged(indexPhoto, adapter.getItemCount());
                        Glide.with(this).load(displayImage.getPhotoPath())
                                .fitCenter().into(imageView);
                        indexPhoto -= 1;
                        adapter.drawBorder(indexPhoto);
                        if (indexPhoto==0) {
                            leftButton.setVisibility(View.INVISIBLE);
                        }
                        if (indexPhoto==0 && photos.size()==1) {
                            rightButton.setVisibility(View.INVISIBLE);
                        }
                        if (indexPhoto==(photos.size()-1)) {
                            rightButton.setVisibility(View.INVISIBLE);
                        }
                    }
                } // Case any exceptions occur.
                catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        // Else if the index is zero and we have more then one photo we display the next image
        // of our recyclerview, then delete our photo we selected to delete.
        else if (indexPhoto == 0 && (photos.size() > 1)) {
            // Get the path to the next photo in the recyclerview
            // so we can display that one in our large imageview.
            Photo displayImage = photos.get(indexPhoto+1);
            // Check if our file does exist.
            if (fDelete.exists()) {
                try {
                    // Remove the file from our adapterlist, then delete it from our storage,
                    // notify our adapter of the change, set our imageview, lastly re-index.
                    if(fDelete.delete()) {
                        photos.remove(indexPhoto);
                        adapter.notifyItemRemoved(indexPhoto);
                        adapter.notifyItemRangeChanged(indexPhoto, adapter.getItemCount());
                        Glide.with(this).load(displayImage.getPhotoPath())
                                .fitCenter().into(imageView);
                        indexPhoto = 0;
                        adapter.drawBorder(indexPhoto);
                        if (indexPhoto==0) {
                            leftButton.setVisibility(View.INVISIBLE);
                        }
                        if (indexPhoto==0 && photos.size()==1) {
                            rightButton.setVisibility(View.INVISIBLE);
                        }
                    } // If it failed to delete the photo using the normal way,
                    // the device needs to use the MediaStore method to delete the photo.
                    else {
                        ContentResolver contentResolver = getContentResolver();
                        deleteFileFromMediaStore(contentResolver, fDelete);
                        photos.remove(indexPhoto);
                        adapter.notifyItemRemoved(indexPhoto);
                        adapter.notifyItemRangeChanged(indexPhoto, adapter.getItemCount());
                        Glide.with(this).load(displayImage.getPhotoPath())
                                .fitCenter().into(imageView);
                        indexPhoto = 0;
                        adapter.drawBorder(indexPhoto);
                        if (indexPhoto==0) {
                            leftButton.setVisibility(View.INVISIBLE);
                        }
                        if (indexPhoto==0 && photos.size()==1) {
                            rightButton.setVisibility(View.INVISIBLE);
                        }
                    }
                } // Case any exceptions occur.
                catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            // If the index is zero and we only have one photo we display the "no_photo_icon" from
            // our resources, then delete our photo we selected to delete.
        } else if (indexPhoto == 0 && (photos.size() == 1)){
            // Check if our file does exist.
            if (fDelete.exists()) {
                try {
                    // Remove the file from our adapterlist, then delete it from our storage,
                    // notify our adapter of the change,
                    // set our large imageview and edit/delete button accordingly, lastly re-index.
                    if(fDelete.delete()) {
                        photos.remove(indexPhoto);
                        adapter.notifyItemRemoved(indexPhoto);
                        adapter.notifyItemRangeChanged(indexPhoto, adapter.getItemCount());
                        imageView.setImageResource(R.mipmap.no_image_icon);
                        editButton.setVisibility(View.INVISIBLE);
                        deleteButton.setVisibility(View.INVISIBLE);
                        leftButton.setVisibility(View.INVISIBLE);
                        rightButton.setVisibility(View.INVISIBLE);
                        indexPhoto = 0;
                        adapter.drawBorder(indexPhoto);
                    } // If it failed to delete the photo using the normal way,
                    // the device needs to use the MediaStore method to delete the photo.
                    else {
                        ContentResolver contentResolver = getContentResolver();
                        deleteFileFromMediaStore(contentResolver,fDelete);
                        adapter.notifyItemRemoved(indexPhoto);
                        adapter.notifyItemRangeChanged(indexPhoto, adapter.getItemCount());
                        imageView.setImageResource(R.mipmap.no_image_icon);
                        editButton.setVisibility(View.INVISIBLE);
                        deleteButton.setVisibility(View.INVISIBLE);
                        leftButton.setVisibility(View.INVISIBLE);
                        rightButton.setVisibility(View.INVISIBLE);
                        indexPhoto = 0;
                        adapter.drawBorder(indexPhoto);
                    }
                    // Disable the upload button and category spinner as well.
                    categorieSpinner.setSelection(0);
                    categorieSpinner.setEnabled(false);
                    rlSpinner.setBackgroundColor(Color.parseColor("#808080"));
                    uploadButton.setClickable(false);
                    uploadButton.setBackgroundColor(Color.parseColor("#808080"));
                } // Case any exceptions occur.
                catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        if (photos.size()>=1) {
            photoIndex.setText((indexPhoto + 1) + "/" + photos.size());
            rv.scrollToPosition(indexPhoto);
        } else
            photoIndex.setText("");
    }

    // Deletes the photo from the MediaStore.
    private static void deleteFileFromMediaStore(final ContentResolver contentResolver,
                                                final File file) {
        // Get path for file in the MediaStore.
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        }
        catch (IOException e) {
            canonicalPath = file.getAbsolutePath();
        }

        final Uri uri = MediaStore.Files.getContentUri("external");
        // Delete the file using contentResolver.
        final int result = contentResolver.delete(uri,
                MediaStore.Files.FileColumns.DATA + "=?", new String[]{canonicalPath});

        if (result == 0) {
            final String absolutePath = file.getAbsolutePath();
            if (!absolutePath.equals(canonicalPath)) {
                contentResolver.delete(uri,
                        MediaStore.Files.FileColumns.DATA + "=?", new String[]{absolutePath});
            }
        }

    }

    // Cleans the directory of all photos for storage management and the directory itself.
    private void cleanImageDirectory() {
        // Get the directory.
        File dir = new File(Environment.getExternalStorageDirectory() + "/DCIM/Dossierfotos/" +
                patient.getVisitId() + "/");
        // Each file in the directory gets scanned and deleted, directories excluded.
        Boolean succes = true;
        try {
            if (dir.exists()) {
                for (File file : dir.listFiles()) {
                    if (!file.isDirectory())
                        if (!file.delete()) {
                            succes = false;
                        }
                }
                if (succes) {
                    succes = dir.delete();
                    if (succes) {
                        // Also delete all preferences from the photo(s).
                        for (Photo photo: photos) {
                            SharedPreferences.Editor editor = fotoCategorie.edit();
                            editor.remove(photo.getPhotoPath());
                            editor.apply();
                        }

                    }
                }
            }
        } // Case any exceptions occur.
        catch (NullPointerException e) {
            e.printStackTrace();
        }

    }

    // Cleans the directory of all photos for storage management and the directory itself.
    private void cleanImageDirectory(File dir) {
        // Each file in the directory gets scanned and deleted, directories excluded.
        Boolean succes = true;
        try {
            if (dir.exists()) {
                for (File file : dir.listFiles()) {
                    if (!file.isDirectory())
                        if (!file.delete()) {
                            succes = false;
                        }
                }
                if (succes) {
                    succes = dir.delete();
                    if (succes) {
                        // Also delete all preferences from the photo(s).
                        for (Photo photo: photos) {
                            SharedPreferences.Editor editor = fotoCategorie.edit();
                            editor.remove(photo.getPhotoPath());
                            editor.apply();
                        }

                    }
                }
            }
        } // Case any exceptions occur.
        catch (NullPointerException e) {
            e.printStackTrace();
        }

    }

    // Displays photo ticked that is ticked on in the recyclerview,
    // that is added last to the recyclerview or that is edited.
    protected void displaySelectedPhoto(int index) {
        // Get the photo object.
        Photo photo = photos.get(index);
        File file = new File(photo.getPhotoPath());
        // Set the photo in our view, we use the path of our photo object.
        Glide.with(this).load(photo.getPhotoPath())
                .signature(new StringSignature(String.valueOf(file.lastModified())))
                .fitCenter().into(imageView);
        // Re-index accordingly.
        String categorie = photo.getCategorieDescription();
        indexPhoto = index;

        // Set the left and right buttons accordingly.
        if (indexPhoto==0 && photos.size()==1) {
            leftButton.setVisibility(View.INVISIBLE);
            rightButton.setVisibility(View.INVISIBLE);
        } else if (indexPhoto==0) {
            leftButton.setVisibility(View.INVISIBLE);
            rightButton.setVisibility(View.VISIBLE);
        } else if (indexPhoto == photos.size()-1) {
            rightButton.setVisibility(View.INVISIBLE);
            leftButton.setVisibility(View.VISIBLE);
        } else {
            leftButton.setVisibility(View.VISIBLE);
            rightButton.setVisibility(View.VISIBLE);
        }

        photoIndex.setText((indexPhoto+1)+"/"+photos.size());
        // Set the spinner.
        if (photo.categorieDescription!=null){
            int indexSpinner = 0;
            for (int i=0;i<categorieSpinner.getCount();i++){
                if (categorieSpinner.getItemAtPosition(i).toString().equalsIgnoreCase(categorie)) {
                    indexSpinner = i;
                    break;
                }
            }
            categorieSpinner.setSelection(indexSpinner);
        } else
            categorieSpinner.setSelection(0);

    }

    // Set the current patient object.
    protected void setPatient(Patient patient) {
        this.patient = patient;
    }

    // Callback to when we select an item in the spinner dropdown.
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Check which position was selected.
        if (position!=0) {
            if (photos.size()!=0) {
                // Get the category from the spinner.
                Category category = (Category) parent.getSelectedItem();
                // Get the currently displayed photo and set the category.
                Photo foto = photos.get(indexPhoto);
                foto.categorieKey = category.getKey();
                foto.categorieDescription = category.getDescription();
                // Save the category to our sharedPreferences for later use.
                SharedPreferences.Editor editor = fotoCategorie.edit();
                editor.putString(foto.getPhotoPath()+"description", category.getDescription());
                editor.putString(foto.getPhotoPath()+"key", category.getKey());
                editor.apply();
                // Set the photo object and notify the recyclerView.
                photos.set(indexPhoto, foto);
                adapter.notifyItemChanged(indexPhoto);
            }
        } else {
            if (photos.size()!=0) {
                // Get the currently displayed photo and set the category to nothing.
                Photo foto = photos.get(indexPhoto);
                foto.categorieKey = "";
                foto.categorieDescription = "";
                // Save the category to our sharedPreferences for later use.
                SharedPreferences.Editor editor = fotoCategorie.edit();
                editor.putString(foto.getPhotoPath()+"description", "");
                editor.putString(foto.getPhotoPath()+"key", "");
                editor.apply();
                // Set the photo object and notify the recyclerView.
                photos.set(indexPhoto, foto);
                adapter.notifyItemChanged(indexPhoto);
            }
        }
    }

    // Callback that is triggered when the item currently selected in the spinner is removed,
    // so we can set it to another value from the spinner.
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        categorieSpinner.setSelection(0);
    }

    // This AsyncTask we use to check if the service link is available.
    private static class checkServiceAvailable extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(String... params) {

            try {
                HttpURLConnection.setFollowRedirects(false);
                HttpURLConnection con =  (HttpURLConnection) new URL(params[0]).openConnection();
                con.setRequestMethod("HEAD");
                System.out.println(con.getResponseCode());
                return (con.getResponseCode() == HttpURLConnection.HTTP_OK);

            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }

        @Override
        protected void onPostExecute(Boolean result) {

        }

    }

    // This AsyncTask we use to handle getting the categories JSON data from the service.
    private class getJsonCategories extends AsyncTask<String, Void, String> {

        // Makes a Post Request from the service URL and Body strings and returns the JSON response.
        private String makePostRequest(String stringUrl, String body) {
            try {
                // Open connection to service.
                URL url = new URL(stringUrl);
                HttpURLConnection uc = (HttpURLConnection) url.openConnection();
                String line;
                StringBuilder jsonString = new StringBuilder();

                // Set our parameters to connection and connect.
                uc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                uc.setRequestMethod("POST");
                uc.setDoInput(true);
                uc.setInstanceFollowRedirects(false);
                uc.connect();

                // Write the body to the output stream.
                OutputStreamWriter writer = new OutputStreamWriter(uc.getOutputStream(), "UTF-8");
                writer.write(body);
                writer.close();

                // Read the response stream and return it.
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(uc.getInputStream(), "UTF8"));
                while((line = br.readLine()) != null) {
                    jsonString.append(line);
                }
                br.close();
                uc.disconnect();
                return jsonString.toString();
            } // If any exceptions occur during the process return nothing.
            catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        // Handled on UI thread before execution of doInBackground.
        @Override
        protected void onPreExecute() {

        }

        // Threaded method.
        @Override
        protected String doInBackground(String... params) {
            // Create an instance of our patient object.
            String status = "";
            try {
                // Get the patient his credentials from the service.
                String result = makePostRequest(MIRTH_SERVERLINK,
                        "{\"action\":\"fetchCategories\"}");
                // Check that the result isn't empty.
                if (!result.equals("")) {
                    // Create our JSON object.
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject.getString("error").equals("0")){
                        // Create a HashMap for our category values.
                        JSONArray categories = jsonObject.getJSONArray("categories");
                        for (int i = 0; i < categories.length(); i++) {
                            JSONObject row = categories.getJSONObject(i);
                            String key = row.getString("key");
                            String value = row.getString("description");
                            categoryList.add(new Category(key,value));
                        }
                        status = "SUCCESS";
                    } else
                        // Patient not found or wrong request.
                        status = "SOMETHING WENT WRONG WITH JSON";
                } else
                    // Service unavailable.
                    status = "SERVICE UNAVAILABLE";
            } // If any exceptions occur during the parsing to JSON.
            catch (Exception e){
                e.printStackTrace();
            }

            return status;

        }

        // Handled on UI thread after execution of doInBackground.
        @Override
        protected void onPostExecute(String status) {
            // Check which status we have.
            switch (status) {
                case "SUCCESS":

                    adapterCategories.notifyDataSetChanged();

                    break;

                case "SOMETHING WENT WRONG WITH JSON":
                    // Show a dialog that the service cannot be accessed.
                    new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme)
                            .setMessage("Fout bij ophalen van categorieën.")
                            .setNegativeButton("Sluiten",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            dialog.dismiss();
                                        }
                                    })
                            .create()
                            .show();

                    break;


                /*case "SERVICE UNAVAILABLE":

                    // Show a dialog that the service cannot be accessed.
                    new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme)
                            .setMessage("De service is niet beschikbaar of bereikbaar.")
                            .setNegativeButton("Sluiten",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            dialog.dismiss();
                                        }
                                    })
                            .create()
                            .show();

                    break;*/

                default:

                    break;

            }

        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

    }

    // This AsyncTask we use to handle getting the patient his JSON data from the service,
    // after we get a response we check the JSON to see if all went to plan and handle accordingly.
    // We make a folder for the visitId, or if present on the device already
    // check the folder and get the photo(s) if there are any at all.
    private class getJsonForPatient extends AsyncTask<String, Void, Patient>   {

        // Makes a Post Request from the service URL and Body strings and returns the JSON response.
        private String makePostRequest(String stringUrl, String body) {
            try {
                // Open connection to service.
                URL url = new URL(stringUrl);
                HttpURLConnection uc = (HttpURLConnection) url.openConnection();
                String line;
                StringBuilder jsonString = new StringBuilder();

                // Set our parameters to connection and connect.
                uc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                uc.setRequestMethod("POST");
                uc.setDoInput(true);
                uc.setInstanceFollowRedirects(false);
                uc.connect();

                // Write the body to the output stream.
                OutputStreamWriter writer = new OutputStreamWriter(uc.getOutputStream(), "UTF-8");
                writer.write(body);
                writer.close();

                // Read the response stream and return it.
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(uc.getInputStream(), "UTF8"));
                while((line = br.readLine()) != null) {
                    jsonString.append(line);
                }
                br.close();
                uc.disconnect();
                return jsonString.toString();
            } // If any exceptions occur during the process return nothing.
            catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        // Handled on UI thread before execution of doInBackground.
        @Override
        protected void onPreExecute() {
            if (categoryList.size()==1) {
                new getJsonCategories().execute();
            }
            photos.clear();
            adapter.notifyDataSetChanged();

        }

        // Threaded method.
        @Override
        protected Patient doInBackground(String... params) {
            // Create an instance of our patient object.
            Patient patientGegevens = null;
            try {
                // Get the patient his credentials from the service.
                String result = makePostRequest(MIRTH_SERVERLINK,
                        "{\"visitId\":\"" + params[0] + "\", \"action\":\"query\"}");
                // Check that the result isn't empty.
                if (!result.equals("")) {
                    // Create our JSON object.
                    JSONObject patientJson = new JSONObject(result);
                    if (patientJson.getString("error").equals("0")){
                        // Create a patient object with the credentials.
                        patientGegevens = new Patient(patientJson, "OK");
                    } else
                        // Patient not found or wrong request.
                        patientGegevens = new Patient("NOT FOUND", params[0]);
                } else
                    // Service unavailable.
                    patientGegevens = new Patient("SERVICE UNAVAILABLE", params[0]);
            } // If any exceptions occur during the parsing to JSON.
            catch (JSONException e){
                e.printStackTrace();
            }

            return patientGegevens;

        }

        // Handled on UI thread after execution of doInBackground.
        @Override
        protected void onPostExecute(Patient patient) {
            // Check which status we have.
            File storageDir = new File(Environment.getExternalStorageDirectory() +
                    "/DCIM/Dossierfotos/");
            switch (patient.getStatus()) {
                // Then we have the patient his credentials.
                case "OK":
                    // Set the patient information in the UI.
                    setPatient(patient);

                    patientNaam.setText("Patiënt: " + patient.getLastName() +
                            ", " + patient.getFirstName());
                    patientInfo.setText("Geslacht: " + patient.getSex() + " - Geboortedatum: " +
                            patient.getBirthDate() + " - Leeftijd: " + patient.getAge() + " jaar");

                    infoButton.setVisibility(View.VISIBLE);

                    createStorageDir();

                    // Get photo(s) that are already on the device or create a directory for them.
                    File folder = new File(Environment.getExternalStorageDirectory() +
                            "/DCIM/Dossierfotos/" + patient.getVisitId());

                    boolean success = true;

                    if (folder.exists()) {
                        // If the folder exists we read all the photo(s) for the visitId.
                        try {
                            File[] pictures = folder.listFiles();
                            if (pictures.length != 0) {
                                for (File picture : pictures) {
                                    // Get each photo and put them in the recyclerView.
                                    Photo foto = new Photo(picture.getAbsolutePath());
                                    foto.categorieDescription = fotoCategorie.getString(
                                            picture.getAbsolutePath()+"description", "");
                                    foto.categorieKey = fotoCategorie.getString(
                                            picture.getAbsolutePath()+"key", "");
                                    boolean exists = false;
                                    for (Photo photo: photos) {
                                        if (photo.getPhotoPath().equals(picture.getAbsolutePath()))
                                        {
                                            exists = true;
                                            break;
                                        }else
                                            exists = false;
                                    }
                                    if (!exists) {
                                        photos.add(foto);
                                        adapter.notifyItemInserted(photos.size() - 1);
                                    }
                                }

                                new encodePhotos().execute();
                                // Re-index.
                                indexPhoto = photos.size() - 1;
                                // Display last photo.
                                displaySelectedPhoto(indexPhoto);

                                // Highlight the last cardView item.
                                adapter.notifyItemChanged(adapter.selectedPosition);
                                adapter.selectedPosition = (photos.size() - 1);
                                adapter.notifyItemChanged(adapter.selectedPosition);
                                rv.scrollToPosition(indexPhoto);

                                // Set the edit and delete button as visible.
                                editButton.setVisibility(View.VISIBLE);
                                deleteButton.setVisibility(View.VISIBLE);

                                // Set the buttons.
                                categorieSpinner.setEnabled(true);
                                rlSpinner.setBackgroundColor(Color.parseColor("#FFFFFF"));
                                uploadButton.setClickable(true);
                                uploadButton.setBackgroundColor(Color.parseColor("#FFFFFF"));

                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Else make a folder for the visitId.
                        success = folder.mkdir();
                    }

                    if (success) {
                        // Enable photo button.
                        photoButton.setClickable(true);
                        photoButton.setBackgroundColor(Color.parseColor("#FFFFFF"));

                        if (photos.size()>=20) {
                            photoButton.setClickable(false);
                            photoButton.setBackgroundColor(Color.parseColor("#808080"));
                        }
                    }

                    try {
                        if (storageDir.listFiles().length>5) {
                            File[] files = storageDir.listFiles();
                            File oldestDir = files[0];
                            for (int i = 1; i < files.length; i++) {
                                if (oldestDir.lastModified() >
                                        files[i].lastModified()) {
                                    oldestDir = files[i];
                                }
                            }
                            cleanImageDirectory(oldestDir);
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                    break;

                // Patient not found or wrong request.
                case "NOT FOUND":
                    // Set the visitId in the UI.
                    setPatient(patient);

                    patientNaam.setText(
                            getResources().getString(R.string.opname, patient.getVisitId()));

                    patientInfo.setText("");

                    createStorageDir();

                    // Get photo(s) that are already on the device or create a directory for them.
                    folder = new File(Environment.getExternalStorageDirectory() +
                            "/DCIM/Dossierfotos/" + patient.getVisitId());

                    success = true;

                    if (folder.exists()) {
                        // If the folder exists we read all the photo(s) for the visitId.
                        try {
                            File[] pictures = folder.listFiles();
                            if (pictures.length != 0) {
                                for (File picture : pictures) {
                                    // Get each photo and put them in the recyclerView.
                                    Photo foto = new Photo(picture.getAbsolutePath());
                                    foto.categorieDescription = fotoCategorie.getString(
                                            picture.getAbsolutePath()+"description", "");
                                    foto.categorieKey = fotoCategorie.getString(
                                            picture.getAbsolutePath()+"key", "");
                                    boolean exists = false;
                                    for (Photo photo: photos) {
                                        if (photo.getPhotoPath().equals(picture.getAbsolutePath()))
                                        {
                                            exists = true;
                                            break;
                                        }else
                                            exists = false;
                                    }
                                    if (!exists) {
                                        photos.add(foto);
                                        adapter.notifyItemInserted(photos.size() - 1);
                                    }
                                }

                                // Re-index.
                                indexPhoto = photos.size() - 1;
                                // Display last photo.
                                displaySelectedPhoto(indexPhoto);

                                // Highlight the last cardView item.
                                adapter.notifyItemChanged(adapter.selectedPosition);
                                adapter.selectedPosition = (photos.size() - 1);
                                adapter.notifyItemChanged(adapter.selectedPosition);
                                rv.scrollToPosition(indexPhoto);

                                // Set the edit and delete button as visible.
                                editButton.setVisibility(View.VISIBLE);
                                deleteButton.setVisibility(View.VISIBLE);

                                // Set the buttons.
                                categorieSpinner.setSelection(0);
                                categorieSpinner.setEnabled(false);
                                rlSpinner.setBackgroundColor(Color.parseColor("#808080"));
                                uploadButton.setClickable(false);
                                uploadButton.setBackgroundColor(Color.parseColor("#808080"));

                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Else make a folder for the visitId.
                        success = folder.mkdir();
                    }
                    if (success) {
                        // Enable photo button.
                        photoButton.setClickable(true);
                        photoButton.setBackgroundColor(Color.parseColor("#FFFFFF"));

                        if (photos.size()>=20) {
                            photoButton.setClickable(false);
                            photoButton.setBackgroundColor(Color.parseColor("#808080"));
                        }
                    }

                    try {
                        if (storageDir.listFiles().length>5) {
                            File[] files = storageDir.listFiles();
                            File oldestDir = files[0];
                            for (int i = 1; i < files.length; i++) {
                                if (oldestDir.lastModified() >
                                        files[i].lastModified()) {
                                    oldestDir = files[i];
                                }
                            }
                            cleanImageDirectory(oldestDir);
                        }
                    } catch (NullPointerException e) {
                            e.printStackTrace();
                    }

                    // Show a dialog that the patient isn't found on our server.
                    new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme)
                            .setMessage("De patient is niet gevonden in het systeem.")
                            .setNegativeButton("Sluiten",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            dialog.dismiss();
                                        }
                                    })
                            .create()
                            .show();

                    break;

                // Service isn't available.
                case "SERVICE UNAVAILABLE":
                    // Set the visitId in the UI.
                    setPatient(patient);

                    patientNaam.setText(
                            getResources().getString(R.string.opname, patient.getVisitId()));

                    patientInfo.setText("");

                    createStorageDir();

                    // Get photo(s) that are already on the device or create a directory for them.
                    folder = new File(Environment.getExternalStorageDirectory() +
                            "/DCIM/Dossierfotos/" + patient.getVisitId());

                    success = true;

                    if (folder.exists()) {
                        // If the folder exists we read all the photo(s) for the visitId.
                        try {
                            File[] pictures = folder.listFiles();
                            if (pictures.length != 0) {
                                for (File picture : pictures) {
                                    // Get each photo and put them in the recyclerView.
                                    Photo foto = new Photo(picture.getAbsolutePath());
                                    foto.categorieDescription = fotoCategorie.getString(
                                            picture.getAbsolutePath()+"description", "");
                                    foto.categorieKey = fotoCategorie.getString(
                                            picture.getAbsolutePath()+"key", "");
                                    boolean exists = false;
                                    for (Photo photo: photos) {
                                        if (photo.getPhotoPath().equals(picture.getAbsolutePath()))
                                        {
                                            exists = true;
                                            break;
                                        }else
                                            exists = false;
                                    }
                                    if (!exists) {
                                        photos.add(foto);
                                        adapter.notifyItemInserted(photos.size() - 1);
                                    }
                                }

                                // Re-index.
                                indexPhoto = photos.size() - 1;
                                // Display last photo.
                                displaySelectedPhoto(indexPhoto);

                                // Highlight the last cardView item.
                                adapter.notifyItemChanged(adapter.selectedPosition);
                                adapter.selectedPosition = (photos.size() - 1);
                                adapter.notifyItemChanged(adapter.selectedPosition);
                                rv.scrollToPosition(indexPhoto);

                                // Set the edit and delete button as visible.
                                editButton.setVisibility(View.VISIBLE);
                                deleteButton.setVisibility(View.VISIBLE);

                                // Set the buttons.
                                categorieSpinner.setSelection(0);
                                categorieSpinner.setEnabled(false);
                                rlSpinner.setBackgroundColor(Color.parseColor("#808080"));
                                uploadButton.setClickable(false);
                                uploadButton.setBackgroundColor(Color.parseColor("#808080"));

                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Else make a folder for the visitId.
                        success = folder.mkdir();
                    }

                    if (success) {
                        // Enable photo button.
                        photoButton.setClickable(true);
                        photoButton.setBackgroundColor(Color.parseColor("#FFFFFF"));

                        if (photos.size()>=20) {
                            photoButton.setClickable(false);
                            photoButton.setBackgroundColor(Color.parseColor("#808080"));
                        }
                    }

                    try {
                        if (storageDir.listFiles().length>5) {
                            File[] files = storageDir.listFiles();
                            File oldestDir = files[0];
                            for (int i = 1; i < files.length; i++) {
                                if (oldestDir.lastModified() >
                                        files[i].lastModified()) {
                                    oldestDir = files[i];
                                }
                            }
                            cleanImageDirectory(oldestDir);
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                    // Show a dialog that the service cannot be accessed.
                    new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogTheme)
                            .setMessage("De service is niet beschikbaar of bereikbaar.")
                            .setNegativeButton("Sluiten",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            dialog.dismiss();
                                        }
                                    })
                            .create()
                            .show();

                    break;

                default:

                    break;
            }

        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

    }

    // This AsyncTask handles the asynchronous getting of the image angle for orientation.
    private class photoAngle extends AsyncTask<String, Void, String> {

        // Handled on UI thread before execution of doInBackground.
        @Override
        protected void onPreExecute() {

        }

        // Threaded method.
        @Override
        protected String doInBackground(String... params) {
            try {
                // Get the EXIF data of the image.
                ExifInterface exif = new ExifInterface(params[0]);
                // Get the orientation value from the image.
                String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                int orientation = orientString != null ? Integer.parseInt(orientString) :
                        ExifInterface.ORIENTATION_NORMAL;
                // Check the orientation value.
                int rotationAngle = 0;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

                return Integer.toString(rotationAngle);

            } catch (Throwable e) {
                e.printStackTrace();
                return "";
            }

        }

        // Handled on UI thread after execution of doInBackground.
        @Override
        protected void onPostExecute(String rotationAngle) {
            Photo originalPhoto = photos.get(indexPhoto);
            originalPhoto.angle = rotationAngle;
            photos.set(indexPhoto, originalPhoto);
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

    }

    // This AsyncTask handles the asynchronous coding of a photoFile to a BASE64 string
    // which is then placed in the photo objects "encodedB64" parameter.
    private class encodePhoto extends AsyncTask<String, Void, Photo> {

        // Handled on UI thread before execution of doInBackground.
        @Override
        protected void onPreExecute() {

        }

        // Threaded method.
        @Override
        protected Photo doInBackground(String... params) {
            // Create a photo object and a file from the path.
            Photo photo = new Photo(params[0]);
            File file = new File(params[0]);
            InputStream inputStream = null;

            try {
                // Read the file and encode it to BASE64, set the photo objects parameter.
                inputStream = new FileInputStream(file);
                byte[] bytes;
                byte[] buffer = new byte[8192];
                int bytesRead;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                bytes = output.toByteArray();
                String encodedString = Base64.encodeToString(bytes, Base64.DEFAULT);
                photo.encodedB64 = encodedString.replaceAll("\n", "");

            } // Case any exceptions occur during the parsing.
            catch (Exception e){
                e.printStackTrace();
            }
            finally {
                try {
                    assert inputStream != null;
                    inputStream.close();

                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            return photo;

        }

        // Handled on UI thread after execution of doInBackground.
        @Override
        protected void onPostExecute(Photo photo) {
            // Replace the original photo object in our adapter
            // with the new one that has the encoding done.
            Photo originalPhoto = photos.get(indexPhoto);
            originalPhoto.encodedB64 = photo.getEncodedB64();
            photos.set(indexPhoto, originalPhoto);
        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

    }

    // This AsyncTask handles the asynchronous coding of multiple photoFiles to a BASE64 string
    // which is then placed in the photo objects "encodedB64" parameter.
    private class encodePhotos extends AsyncTask<String, Void, List<Photo>> {

        // Handled on UI thread before execution of doInBackground.
        @Override
        protected void onPreExecute() {

        }

        // Threaded method.
        @Override
        protected List<Photo> doInBackground(String... params) {
            List<Photo> encodedPhotos = new ArrayList<>();
            for (Photo photo : photos) {
                File file = new File(photo.getPhotoPath());
                try {
                    // Read the file and encode it to BASE64, set the photo objects parameter.
                    InputStream inputStream = new FileInputStream(file);
                    byte[] bytes;
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                    bytes = output.toByteArray();
                    inputStream.close();
                    String encodedString = Base64.encodeToString(bytes, Base64.DEFAULT);
                    photo.encodedB64 = encodedString.replaceAll("\n", "");

                    // Get the EXIF data of the image.
                    ExifInterface exif = new ExifInterface(photo.photoPath);
                    // Get the orientation value from the image.
                    String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                    int orientation = orientString != null ? Integer.parseInt(orientString) :
                            ExifInterface.ORIENTATION_NORMAL;
                    // Check the orientation value.
                    int rotationAngle = 0;
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;
                    photo.angle = Integer.toString(rotationAngle);

                    encodedPhotos.add(photo);
                } // If any exceptions occur during the parsing.
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            return encodedPhotos;

        }

        // Handled on UI thread after execution of doInBackground.
        @Override
        protected void onPostExecute(List<Photo> encodedPhotos) {
            for (int i = 0 ; i < encodedPhotos.size(); i++) {
                Photo originalPhoto = photos.get(i);
                originalPhoto.encodedB64 = encodedPhotos.get(i).getEncodedB64();
                photos.set(i, originalPhoto);
            }

        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

    }

    // This AsyncTask handles the asynchronous sending of the photoFiles to the service.
    private class uploadPhotos extends AsyncTask<String, Void, Boolean> {

        // ProgressDialog instance.
        private ProgressDialog progressDialog = new ProgressDialog(
                MainActivity.this, R.style.AlertDialogThemeLoading);

        // Makes a Post Request from the service URL and Body strings and returns the JSON response.
        private String makePostRequest(String stringUrl, String body) {
            try {
                // Open connection to service.
                URL url = new URL(stringUrl);
                HttpURLConnection uc = (HttpURLConnection) url.openConnection();
                String line;
                StringBuilder jsonString = new StringBuilder();

                // Set our parameters to connection and connect.
                uc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                uc.setRequestMethod("POST");
                uc.setDoInput(true);
                uc.setInstanceFollowRedirects(false);
                uc.connect();

                // Write the body to the output stream.
                OutputStreamWriter writer = new OutputStreamWriter(uc.getOutputStream(), "UTF-8");
                writer.write(body);
                writer.close();

                // Read the response stream and return it.
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(uc.getInputStream(), "UTF8"));
                while((line = br.readLine()) != null) {
                    jsonString.append(line);
                }
                br.close();
                uc.disconnect();
                return jsonString.toString();
            } // If any exceptions occur during the process return nothing.
            catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        // Handled on UI thread before execution of doInBackground.
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, "WakelockUpload");
            wakeLock.acquire();
            // Set our parameters for the progressDialog.
            progressDialog.setMessage("Uploaden...");
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
            progressDialog.setCanceledOnTouchOutside(false);

        }

        // Threaded method.
        @Override
        protected Boolean doInBackground(String... params) {
            Boolean succes = true;
            for (Photo photo : photos) {
                try {
                    String result = makePostRequest(MIRTH_SERVERLINK,
                            "{\"visitId\":\"" + patient.getVisitId() + "\", " +
                            "\"action\":\"upload\", " +
                            "\"picture\":\"" + photo.getEncodedB64() + "\", " +
                            "\"category\":\""+ photo.categorieKey + "\", " +
                            "\"rotation\":\""+ photo.angle + "\"}");
                    if (result.equals("")) {
                        succes = false;

                        break;

                    } else {
                        // Create our JSON object.
                        JSONObject uploadResponseJson = new JSONObject(result);
                        if (!uploadResponseJson.getString("error").equals("0")){
                            succes = false;

                            break;

                        } else {
                            final File fDelete = new File(photo.getPhotoPath());
                            if(!fDelete.delete()) {
                                succes = false;

                                break;

                            }
                        }
                    }
                } // If any exceptions occur during the parsing.
                catch (Exception e){
                    e.printStackTrace();
                }
            }

            return succes;

        }

        // Handled on UI thread after execution of doInBackground.
        @Override
        protected void onPostExecute(Boolean succes) {
            if (succes) {
                // Delete all photo(s) for the visitId and reload the (empty) activity.
                cleanImageDirectory();
                progressDialog.dismiss();
                categorieSpinner.setSelection(0);
                recreate();
                wakeLock.release();

            } else
                // Show a dialog that the uploading failed to the service.
                new AlertDialog.Builder(MainActivity.this,R.style.AlertDialogTheme)
                        .setMessage("Foto('s) verzenden mislukt, probeer het nog eens.")
                        .setNegativeButton("Sluiten",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        dialog.dismiss();
                                        progressDialog.dismiss();
                                        patientInfo.setText("");
                                        infoButton.setVisibility(View.INVISIBLE);
                                        new getJsonForPatient().execute(patient.getVisitId());
                                    }
                                })
                        .create()
                        .show();

        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

    }

}