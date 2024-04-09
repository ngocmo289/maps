package com.nnmo.mymaps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;


import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final int FINE_PERMISSION_CODE = 1;
    private GoogleMap myMap;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private SearchView mapSearchview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapSearchview = findViewById(R.id.mapSearch);

        mapSearchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {

                String location = mapSearchview.getQuery().toString();
                List<Address> addressesList = null;

                if (location != null) {

                    Geocoder geocoder = new Geocoder(MainActivity.this);
                    try {
                        addressesList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Address address = addressesList.get(0);
                    LatLng latLng= new LatLng(address.getLatitude(),address.getLongitude());
                    myMap.addMarker(new MarkerOptions().position(latLng).title(location));
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,10));
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Kiểm tra và yêu cầu quyền truy cập vị trí nếu chưa được cấp
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
        } else {
            // Nếu quyền đã được cấp, tiến hành lấy vị trí hiện tại
            getLastLocation();
        }

        // Khởi tạo bản đồ
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MainActivity.this);
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Task<Location> task = fusedLocationProviderClient.getLastLocation();
            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        currentLocation = location;
                        // Gọi onMapReady() khi có vị trí hiện tại
                        if (myMap != null) {
                            onMapReady(myMap);
                        }
                    } else {
                        Log.e("Error", "Failed to get location");
                    }
                }
            });
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        // Set listener để bắt sự kiện click trên bản đồ
        myMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Xử lý sự kiện khi người dùng chạm vào bản đồ
                moveCameraToLocation(latLng);
            }
        });


        if (currentLocation != null) {
            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            myMap.addMarker(new MarkerOptions().position(latLng).title("My Location"));
            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10)); // Zoom level 15
        } else {
            Log.e("Error", "Current location is null");
            // Hiển thị một vị trí mặc định nếu không thể lấy được vị trí hiện tại
            LatLng defaultLocation = new LatLng(-34, 151);
            myMap.addMarker(new MarkerOptions().position(defaultLocation).title("Default Location"));
            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10)); // Zoom level 10
        }
    }

    private void moveCameraToLocation(LatLng latLng) {
        // Xóa các marker cũ (nếu có) và thêm marker mới vào vị trí latLng
        myMap.clear();
        myMap.addMarker(new MarkerOptions().position(latLng));

        // Di chuyển camera đến vị trí latLng với zoom level 15
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền truy cập vị trí đã được cấp, tiến hành lấy vị trí hiện tại
                getLastLocation();
            } else {
                // Người dùng từ chối cấp quyền, hiển thị thông báo
                Toast.makeText(this, "Bạn cần cấp quyền truy cập vị trí để sử dụng ứng dụng này", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.manu_main, menu);
        return true;
    }

    private static final int MAP_NONE_ID = R.id.mapNone;
    private static final int MAP_NORMAL_ID = R.id.mapNormal;
    private static final int MAP_SATELLITE_ID = R.id.mapSatellite;
    private static final int MAP_HYBRID_ID = R.id.mapHybrid;
    private static final int MAP_TERRAIN_ID = R.id.mapTerrain;

    // Sau đó, sử dụng các hằng số này trong câu lệnh switch case
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id == MAP_NONE_ID){
            myMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        }

        if(id == MAP_NORMAL_ID){
            myMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        if(id == MAP_SATELLITE_ID){
            myMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }

        if(id == MAP_HYBRID_ID){
            myMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }

        if(id == MAP_TERRAIN_ID){
            myMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
        return super.onOptionsItemSelected(item);
    }

}
