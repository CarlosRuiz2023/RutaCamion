package com.itsmarts.SmartRouteTruckApp;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.AnimationUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoCoordinatesUpdate;
import com.here.sdk.core.GeoOrientationUpdate;
import com.here.sdk.core.engine.SDKNativeEngine;
import com.here.sdk.core.engine.SDKOptions;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.mapview.LocationIndicator;
import com.here.sdk.mapview.MapCameraAnimation;
import com.here.sdk.mapview.MapCameraAnimationFactory;
import com.here.sdk.mapview.MapError;
import com.here.sdk.mapview.MapMeasure;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapScene;
import com.here.sdk.mapview.MapScheme;
import com.here.sdk.mapview.MapView;
import com.here.sdk.transport.TruckSpecifications;
import com.here.time.Duration;

public class mapHelper{
    private static final int REQUEST_INTERNET_PERMISSION = 1;
    static final int REQUEST_LOCATION_PERMISSION = 2;
    private PermissionResultCallback permissionResultCallback;
    private LocationIndicator currentLocationIndicator;
    private int toneladas, alto, ancho, largo;
    public static final int DEFAULT_TONELADAS = 17;
    public static final int DEFAULT_ALTO = 3;
    public static final int DEFAULT_ANCHO = 4;
    public static final int DEFAULT_LARGO = 8;
    private Dialog gpsDialog;

    public void initializeHERESDK(Context context) {
        String accessKeyID = "NelTCYuCjtoWMisV8QyHyw";
        String accessKeySecret = "5MR6njdMDh_GwgfaZPLEuo_psaA41KWyqY-uGjEZ2deOSvzZ4U4kB7vlZga97yV05IJr18K2Zt_rdKieWzPmbw";
        SDKOptions options = new SDKOptions(accessKeyID, accessKeySecret);
        try {
            SDKNativeEngine.makeSharedInstance(context, options);
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization of HERE SDK failed: " + e.error.name());
        }
    }

    public void loadMapScene(MapView mapView, Context context) {
        mapView.getMapScene().loadScene(MapScheme.NORMAL_DAY, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapError mapError) {
                if (mapError == null) {
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastKnownLocation != null) {
                            double distanceInMeters = 1000 * 0.5;
                            MapMeasure mapMeasureZoom = new MapMeasure(MapMeasure.Kind.DISTANCE, distanceInMeters);
                            GeoCoordinates userCoordinates = new GeoCoordinates(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                            mapView.getCamera().lookAt(userCoordinates, mapMeasureZoom);
                        } else {
                            Log.d("loadMapScene()", "No se pudo obtener la última ubicación conocida");
                        }
                    } else {
                        Log.d("loadMapScene()", "Permiso de ubicación no concedido");
                    }
                } else {
                    Log.d("loadMapScene()", "Error al cargar la escena del mapa: " + mapError.name());
                }
            }
        });
    }

    public void permisoInternet(Activity activity, PermissionResultCallback callback) {
        this.permissionResultCallback = callback;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET_PERMISSION);
        } else {
            callback.permisoConcedido();
        }
    }

    public void permisoLocalizacion(Activity activity, PermissionResultCallback callback) {
        this.permissionResultCallback = callback;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            callback.permisoConcedido();
        }
    }

    public interface PermissionResultCallback {
        void permisoConcedido();
        void permisoDenegado();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_INTERNET_PERMISSION:
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (permissionResultCallback != null) {
                        permissionResultCallback.permisoConcedido();
                    }
                } else {
                    if (permissionResultCallback != null) {
                        permissionResultCallback.permisoDenegado();
                    }
                }
                break;
        }
    }

    public void tiltMap(MapView mapView) {
        double bearing = mapView.getCamera().getState().orientationAtTarget.bearing;
        double tilt =  10;
        mapView.getCamera().setOrientationAtTarget(new GeoOrientationUpdate(bearing, tilt));
    }

    public void showGPSDisabledDialog(final Activity activity) {
        if (gpsDialog != null && gpsDialog.isShowing()) {
            return;
        }

        gpsDialog = new Dialog(activity);
        gpsDialog.setContentView(R.layout.activar_gps);
        gpsDialog.setCancelable(false);
        gpsDialog.setCanceledOnTouchOutside(false);

        Button btnEnableGPS = gpsDialog.findViewById(R.id.btnEnableGPS);
        Button btnCancel = gpsDialog.findViewById(R.id.btnCancel);

        btnEnableGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                activity.startActivity(intent);
                gpsDialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpsDialog.dismiss();
                Toast.makeText(activity, "El GPS es necesario para usar esta app", Toast.LENGTH_LONG).show();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isGPSEnabled(activity)) {
                            showGPSDisabledDialog(activity);
                        }
                    }
                }, 100);
            }
        });
        gpsDialog.show();
    }

    public boolean isGPSEnabled(Activity activity) {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void disposeHERESDK() {
        SDKNativeEngine sdkNativeEngine = SDKNativeEngine.getSharedInstance();
        if (sdkNativeEngine != null) {
            sdkNativeEngine.dispose();
            SDKNativeEngine.setSharedInstance(null);
        }
    }

    public GeoCoordinates generateCoords(Activity activity, Intent intent) {
        GeoCoordinates coordenadasDestino = null;
        try {
            String dat = "";
            if (intent != null && intent.getData() != null) {
                if (intent.getScheme().equals("geo")) {
                    Uri data = intent.getData();
                    dat = data.toString();
                    Log.d("mapHelper", "URI recibida: " + dat);
                    Pattern p = Pattern.compile("(-?\\d+\\.\\d+)");
                    Matcher m = p.matcher(dat);
                    List<Double> coordinates = new ArrayList<>();
                    while (m.find()) {
                        coordinates.add(Double.parseDouble(m.group()));
                    }
                    if (coordinates.size() >= 2) {
                        coordenadasDestino = new GeoCoordinates(coordinates.get(0), coordinates.get(1));
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(activity.getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return coordenadasDestino;
    }

    public void generarRuta(Activity activity, MapView mapView, GeoCoordinates coordenadasDestino, TruckSpecifications truckSpecifications) {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                GeoCoordinates coordenadasInicio = new GeoCoordinates(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                RoutingExample routingExample = new RoutingExample(activity, mapView, coordenadasInicio);
                routingExample.clearRoute();
                routingExample.addRoute(coordenadasInicio, coordenadasDestino, truckSpecifications);
            } else {
                Toast.makeText(activity, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(activity, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show();
        }
    }

    public void addLocationIndicator(MapView mapView, GeoCoordinates geoCoordinates, LocationIndicator.IndicatorStyle indicatorStyle) {
        removeLocationIndicator();

        LocationIndicator locationIndicator = new LocationIndicator();
        locationIndicator.setLocationIndicatorStyle(indicatorStyle);
        com.here.sdk.core.Location location = new com.here.sdk.core.Location(geoCoordinates);
        location.time = new Date();
        location.bearingInDegrees = getRandom(0, 360);

        locationIndicator.updateLocation(location);

        locationIndicator.enable(mapView);

        currentLocationIndicator = locationIndicator;
    }

    public void removeLocationIndicator() {
        if (currentLocationIndicator != null) {
            currentLocationIndicator.disable();
            currentLocationIndicator = null;
        }
    }

    private double getRandom(double min, double max) {
        return min + Math.random() * (max - min);
    }

    public TruckSpecifications createTruckSpecifications() {
        TruckSpecifications truckSpecifications = new TruckSpecifications();
        truckSpecifications.grossWeightInKilograms = DEFAULT_TONELADAS * 1000;
        truckSpecifications.heightInCentimeters = DEFAULT_ALTO * 100;
        truckSpecifications.widthInCentimeters = DEFAULT_ANCHO * 100;
        truckSpecifications.lengthInCentimeters = DEFAULT_LARGO * 100;

        if (toneladas > 0) {
            truckSpecifications.grossWeightInKilograms = toneladas * 1000;
        }
        if (alto > 0) {
            truckSpecifications.heightInCentimeters = alto * 100;
        }
        if (ancho > 0) {
            truckSpecifications.widthInCentimeters = ancho * 100;
        }
        if (largo > 0) {
            truckSpecifications.lengthInCentimeters = largo * 100;
        }
        return truckSpecifications;
    }

    public void setTruckSpecifications(int toneladas, int alto, int ancho, int largo) {
        this.toneladas = toneladas;
        this.alto = alto;
        this.ancho = ancho;
        this.largo = largo;
    }

    public void mostrarMenu(Activity activity) {
        View viewDetalles = activity.findViewById(R.id.viewDetalles);
        TextView txtCamion = activity.findViewById(R.id.txtCamion);
        TextView txtIndicacion = activity.findViewById(R.id.txtIndicacion);
        TextInputLayout etPeso = activity.findViewById(R.id.etPeso);
        TextInputLayout etAltura = activity.findViewById(R.id.etAltura);
        TextInputLayout etAncho = activity.findViewById(R.id.etAncho);
        TextInputLayout etLongitud = activity.findViewById(R.id.etLongitud);
        TextInputLayout etSpinner = activity.findViewById(R.id.etSpinner);
        AutoCompleteTextView AutOpciones = activity.findViewById(R.id.AutOpciones);
        ImageView imgTonelada = activity.findViewById(R.id.imgTonelada);
        ImageView imgAltura = activity.findViewById(R.id.imgAltura);
        ImageView imgAncho = activity.findViewById(R.id.imgAncho);
        ImageView imgLargo = activity.findViewById(R.id.imgLargo);
        Button btnEnviar = activity.findViewById(R.id.btnEnviar);
        Button btnGuardar = activity.findViewById(R.id.btnGuardar);
        ImageButton btnCerrar = activity.findViewById(R.id.btnCerrar);
        Animation animEntrada = AnimationUtils.loadAnimation(activity, R.anim.entrada);
        Animation animSalida = AnimationUtils.loadAnimation(activity, R.anim.salida);

        int visibility = (viewDetalles.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE;

        if (visibility == View.VISIBLE) {
            viewDetalles.startAnimation(animEntrada);
            txtCamion.startAnimation(animEntrada);
            txtIndicacion.startAnimation(animEntrada);
            etPeso.startAnimation(animEntrada);
            etAltura.startAnimation(animEntrada);
            etAncho.startAnimation(animEntrada);
            etLongitud.startAnimation(animEntrada);
            etSpinner.startAnimation(animEntrada);
            AutOpciones.startAnimation(animEntrada);
            imgTonelada.startAnimation(animEntrada);
            imgAltura.startAnimation(animEntrada);
            imgAncho.startAnimation(animEntrada);
            imgLargo.startAnimation(animEntrada);
            btnEnviar.startAnimation(animEntrada);
            btnGuardar.startAnimation(animEntrada);
            btnCerrar.startAnimation(animEntrada);
        } else {
            viewDetalles.startAnimation(animSalida);
            txtCamion.startAnimation(animSalida);
            txtIndicacion.startAnimation(animSalida);
            etPeso.startAnimation(animSalida);
            etAltura.startAnimation(animSalida);
            etAncho.startAnimation(animSalida);
            etLongitud.startAnimation(animSalida);
            etSpinner.startAnimation(animSalida);
            AutOpciones.startAnimation(animSalida);
            imgTonelada.startAnimation(animSalida);
            imgAltura.startAnimation(animSalida);
            imgAncho.startAnimation(animSalida);
            imgLargo.startAnimation(animSalida);
            btnEnviar.startAnimation(animSalida);
            btnGuardar.startAnimation(animSalida);
            btnCerrar.startAnimation(animSalida);
        }

        viewDetalles.setVisibility(visibility);
        txtCamion.setVisibility(visibility);
        txtIndicacion.setVisibility(visibility);
        etPeso.setVisibility(visibility);
        etAltura.setVisibility(visibility);
        etAncho.setVisibility(visibility);
        etLongitud.setVisibility(visibility);
        etSpinner.setVisibility(visibility);
        AutOpciones.setVisibility(visibility);
        imgTonelada.setVisibility(visibility);
        imgAltura.setVisibility(visibility);
        imgAncho.setVisibility(visibility);
        imgLargo.setVisibility(visibility);
        btnEnviar.setVisibility(visibility);
        btnGuardar.setVisibility(visibility);
        btnCerrar.setVisibility(visibility);
    }

    public void flyTo(MapView mapView, GeoCoordinates geoCoordinates) {
        GeoCoordinatesUpdate geoCoordinatesUpdate = new GeoCoordinatesUpdate(geoCoordinates);
        double bowFactor = 1;
        MapCameraAnimation animation = MapCameraAnimationFactory.flyTo(geoCoordinatesUpdate, bowFactor, Duration.ofSeconds(3));
        mapView.getCamera().startAnimation(animation);
    }

    public void checkGPSStatus(Activity activity) {
        if (!isGPSEnabled(activity)) {
            showGPSDisabledDialog(activity);
            Toast.makeText(activity, "Activa el gps", Toast.LENGTH_SHORT).show();
        } else {
        }
    }
}