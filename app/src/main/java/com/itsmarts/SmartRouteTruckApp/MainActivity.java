package com.itsmarts.SmartRouteTruckApp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.LocationListener;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.location.LocationAccuracy;
import com.here.sdk.mapview.LocationIndicator;
import com.here.sdk.mapview.MapCamera;
import com.here.sdk.mapview.MapError;
import com.here.sdk.mapview.MapMarker;
import com.here.sdk.mapview.MapMeasure;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapScene;
import com.here.sdk.mapview.MapScheme;
import com.here.sdk.mapview.MapView;
import com.here.sdk.navigation.MapMatchedLocation;
import com.here.sdk.routing.CalculateRouteCallback;
import com.here.sdk.routing.MapMatchedCoordinates;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.RoutingError;
import com.here.sdk.routing.TruckOptions;
import com.here.sdk.routing.Waypoint;
import com.here.sdk.search.Address;
import com.here.sdk.search.AddressQuery;
import com.here.sdk.search.Place;
import com.here.sdk.search.SearchCallback;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchError;
import com.here.sdk.search.SearchOptions;
import com.here.sdk.transport.TruckSpecifications;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity implements mapHelper.PermissionResultCallback {
    private mapHelper mMapHelper;
    private MapView mapView;
    private Button btnGenerarRuta, btnEnviar, btnGuardar;
    private GeoCoordinates coordenadasDestino;
    private ImageButton btnCerrar, btnRevertir, btnUbicar, btnRuta, btnAgregar, btnCerrarWaypoint1, btnCerrarWaypoint2, btnCerrarWaypoint3;
    private HEREPositioningProvider positioningProvider;
    private ImageView loading;
    private Animation rotateAnimation, cargaAnimacion;
    private boolean animacionEjecutada = false;
    private TextInputEditText txtToneladas, txtAltura, txtAncho, txtLargo, coordendas_finales_input, coordendas_iniciales_input, titWaypoint1, titWaypoint2, titWaypoint3;
    private TextInputLayout etInicio, etDestino, etWaypoint1, etWaypoint2, etWaypoint3;
    MapPolyline rutaActual;
    final List<MapPolyline> mapPolylinesTrafico = new ArrayList<>();
    private int toneladas = 0, alto = 0, ancho = 0, largo = 0;
    private boolean rutaGenerada = false;
    private SearchEngine searchEngine;
    private GeoCoordinates coordenada1, coordenada2;
    private RoutingExample routingExample;
    private MapCamera mapCamera;
    private EditText input_coordenada1, input_coordenada2;
    private RoutingExample2 routingExample2;
    private boolean isFirstClick = true, isNightMode = false, isMenuOpen = false;
    private FloatingActionButton fbPrincipal, fbLocalizacion, fbCamionConfig, fbReiniciar, fbRuta, fbLimpiar, fbSalir, fbMapas;
    private TextView txtLoca, txtMapas, txtConfig, txtRestablecer, txtRuta, txtEliminar, txtSalir;
    private int clickCount = 0;
    private List<TruckSpec> savedSpecs = new ArrayList<>();
    private static final int MAX_SAVED_SPECS = 3;
    private AutoCompleteTextView autoCompleteTextView;
    private HEREPositioningProvider herePositioningProvider;
    RoutingEngine routingEngine;
    private static final double ALPHA = 0.3; // Factor de suavizado
    private GeoCoordinates lastKnownPosition;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMapHelper = new mapHelper();
        mMapHelper.initializeHERESDK(this);

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        try {
            searchEngine = new SearchEngine();
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Fallo al inicializar el motor de búsqueda: " + e.error.name());
        }

        mMapHelper.loadMapScene(mapView, this);

        mMapHelper.permisoInternet(this, this);
        mMapHelper.permisoLocalizacion(this, this);

        mMapHelper.tiltMap(mapView);

        mMapHelper.checkGPSStatus(MainActivity.this);

        if (!mMapHelper.isGPSEnabled(MainActivity.this)) {
            mMapHelper.showGPSDisabledDialog(MainActivity.this);
        } else {
        }

        positioningProvider = new HEREPositioningProvider();

        routingExample2 = new RoutingExample2(this, mapView, mapView.getCamera().getState().targetCoordinates);
        routingExample = new RoutingExample(this, mapView, mapView.getCamera().getState().targetCoordinates);
        mapCamera = mapView.getCamera();

        loadSpecs();
        herePositioningProvider = new HEREPositioningProvider();

        fbPrincipal = findViewById(R.id.fbPrincipal);
        fbLocalizacion = findViewById(R.id.fbLocalizacion);
        txtLoca = findViewById(R.id.txtLoca);
        fbMapas = findViewById(R.id.fbMapas);
        txtMapas = findViewById(R.id.txtMapas);
        fbCamionConfig = findViewById(R.id.fbCamionConfig);
        txtConfig = findViewById(R.id.txtConfig);
        fbReiniciar = findViewById(R.id.fbReiniciar);
        txtRestablecer = findViewById(R.id.txtRestablecer);
        fbRuta = findViewById(R.id.fbRuta);
        txtRuta = findViewById(R.id.txtRuta);
        fbLimpiar = findViewById(R.id.fbLimpiar);
        txtEliminar = findViewById(R.id.txtEliminar);
        fbSalir = findViewById(R.id.fbSalir);
        txtSalir = findViewById(R.id.txtSalir);

        fbPrincipal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMenuOpen) {
                    expandMenu();
                } else {
                    collapseMenu();
                }
            }
        });

        fbLocalizacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomToast("Llevándote a tu ubicación");
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownLocation != null) {
                        GeoCoordinates userCoordinates = new GeoCoordinates(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                        mMapHelper.flyTo(mapView, userCoordinates);
                    } else {
                        showCustomToast("No se pudo obtener la última ubicación");
                    }
                } else {
                    showCustomToast("Permiso de ubicación no concedido");
                }

                if (isMenuOpen) {
                    collapseMenu();
                }
            }
        });

        fbMapas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomToast("Cambiando estilo del mapa");
                MapView mapView = findViewById(R.id.map_view);
                MapScheme mapScheme;

                if (isNightMode) {
                    mapScheme = MapScheme.NORMAL_DAY;
                    isNightMode = false;
                } else {
                    mapScheme = MapScheme.NORMAL_NIGHT;
                    isNightMode = true;
                }

                mapView.getMapScene().loadScene(mapScheme, new MapScene.LoadSceneCallback() {
                    @Override
                    public void onLoadScene(@Nullable MapError mapError) {
                        if (mapError == null) {
                        } else {
                            Log.d("loadMapScene()", "Error al cargar mapa: " + mapError.name());
                        }
                    }
                });
                if (isMenuOpen) {
                    collapseMenu();
                }
            }
        });

        fbCamionConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapHelper.mostrarMenu(MainActivity.this);
                if (isMenuOpen) {
                    collapseMenu();
                }
            }
        });

        fbReiniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toneladas = mapHelper.DEFAULT_TONELADAS;
                alto = mapHelper.DEFAULT_ALTO;
                ancho = mapHelper.DEFAULT_ANCHO;
                largo = mapHelper.DEFAULT_LARGO;

                mMapHelper.setTruckSpecifications(toneladas, alto, ancho, largo);
                routingExample2.setTruckSpecifications(toneladas, alto, ancho, largo);

                showCustomToast("Valores del camión restablecidos");
                if (isMenuOpen) {
                    collapseMenu();
                }
            }
        });

        fbRuta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation animEntrada = AnimationUtils.loadAnimation(MainActivity.this, R.anim.entrada2);
                Animation animSalida = AnimationUtils.loadAnimation(MainActivity.this, R.anim.salida2);

                boolean isVisible = etDestino.getVisibility() == View.VISIBLE;

                if (isVisible) {
                    etInicio.startAnimation(animSalida);
                    coordendas_iniciales_input.startAnimation(animSalida);
                    etDestino.startAnimation(animSalida);
                    coordendas_finales_input.startAnimation(animSalida);
                    btnUbicar.startAnimation(animSalida);
                    btnRuta.startAnimation(animSalida);
                    btnRevertir.startAnimation(animSalida);
                    btnAgregar.startAnimation(animSalida);
                    etWaypoint1.startAnimation(animSalida);
                    titWaypoint1.startAnimation(animSalida);
                    btnCerrarWaypoint1.startAnimation(animSalida);
                    etWaypoint2.startAnimation(animSalida);
                    titWaypoint2.startAnimation(animSalida);
                    btnCerrarWaypoint2.startAnimation(animSalida);
                    etWaypoint3.startAnimation(animSalida);
                    titWaypoint3.startAnimation(animSalida);
                    btnCerrarWaypoint3.startAnimation(animSalida);
                    etInicio.setVisibility(View.GONE);
                    coordendas_iniciales_input.setVisibility(View.GONE);
                    etDestino.setVisibility(View.GONE);
                    coordendas_finales_input.setVisibility(View.GONE);
                    btnUbicar.setVisibility(View.GONE);
                    btnRuta.setVisibility(View.GONE);
                    btnRevertir.setVisibility(View.GONE);
                    btnAgregar.setVisibility(View.GONE);
                    etWaypoint1.setVisibility(View.GONE);
                    titWaypoint1.setVisibility(View.GONE);
                    btnCerrarWaypoint1.setVisibility(View.GONE);
                    etWaypoint2.setVisibility(View.GONE);
                    titWaypoint2.setVisibility(View.GONE);
                    btnCerrarWaypoint2.setVisibility(View.GONE);
                    etWaypoint3.setVisibility(View.GONE);
                    titWaypoint3.setVisibility(View.GONE);
                    btnCerrarWaypoint3.setVisibility(View.GONE);
                } else {
                    btnUbicar.startAnimation(animEntrada);
                    btnRuta.startAnimation(animEntrada);
                    btnRevertir.startAnimation(animEntrada);
                    etInicio.startAnimation(animEntrada);
                    coordendas_iniciales_input.startAnimation(animEntrada);
                    etDestino.startAnimation(animEntrada);
                    coordendas_finales_input.startAnimation(animEntrada);
                    btnAgregar.startAnimation(animEntrada);
                    btnUbicar.setVisibility(View.VISIBLE);
                    btnRuta.setVisibility(View.VISIBLE);
                    btnRevertir.setVisibility(View.VISIBLE);
                    etDestino.setVisibility(View.VISIBLE);
                    etInicio.setVisibility(View.VISIBLE);
                    coordendas_iniciales_input.setVisibility(View.VISIBLE);
                    coordendas_finales_input.setVisibility(View.VISIBLE);
                    btnAgregar.setVisibility(View.VISIBLE);
                }
                if (isMenuOpen) {
                    collapseMenu();
                }
            }
        });

        fbLimpiar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.custom_confirm_dialog, null);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setView(dialogView);

                final AlertDialog dialog = builder.create();

                Button positiveButton = dialogView.findViewById(R.id.positiveButton);
                Button negativeButton = dialogView.findViewById(R.id.negativeButton);

                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (rutaActual != null) {
                            mapView.getMapScene().removeMapPolyline(rutaActual);
                            rutaActual = null;
                        }

                        List<MapMarker> startEndMarkers = new ArrayList<>();
                        for (MapMarker marker : routingExample2.mapMarkerList) {
                            if (marker.getCoordinates().equals(coordenada1) || marker.getCoordinates().equals(coordenada2)) {
                                startEndMarkers.add(marker);
                            }
                        }

                        for (MapMarker marker : routingExample2.mapMarkerList) {
                            if (!startEndMarkers.contains(marker)) {
                                mapView.getMapScene().removeMapMarker(marker);
                            }
                        }
                        routingExample2.mapMarkerList.clear();
                        routingExample2.mapMarkerList.addAll(startEndMarkers);

                        for (MapPolyline polyline : mapPolylinesTrafico) {
                            mapView.getMapScene().removeMapPolyline(polyline);
                        }
                        mapPolylinesTrafico.clear();

                        for (MapPolyline polyline : routingExample2.mapPolylines) {
                            mapView.getMapScene().removeMapPolyline(polyline);
                        }
                        routingExample2.mapPolylines.clear();

                        coordenada1 = null;
                        coordenada2 = null;

                        showCustomToast("Se ha eliminado la ruta");
                        dialog.dismiss();
                        if (isMenuOpen) {
                            collapseMenu();
                        }
                    }
                });

                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });

        fbSalir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.custom_exit_dialog, null);

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setView(dialogView);

                final AlertDialog dialog = builder.create();

                Button positiveButton = dialogView.findViewById(R.id.positiveButton);
                Button negativeButton = dialogView.findViewById(R.id.negativeButton);

                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCustomToast("Adiós");
                        Animation animacion = AnimationUtils.loadAnimation(MainActivity.this, R.anim.cerrar);
                        fbSalir.startAnimation(animacion);
                        routingExample2.clearMap();
                        for (MapPolyline mapPolyline : mapPolylinesTrafico) {
                            mapView.getMapScene().removeMapPolyline(mapPolyline);
                        }
                        mapPolylinesTrafico.clear();

                        if (rutaActual != null) {
                            mapView.getMapScene().removeMapPolyline(rutaActual);
                            rutaActual = null;
                        }
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finishAffinity();
                            }
                        }, 300);
                        dialog.dismiss();
                    }
                });

                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });


        btnGenerarRuta = findViewById(R.id.btn_generar_ruta);
        btnGenerarRuta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                routingExample2.clearMap();
                for (MapPolyline mapPolyline : mapPolylinesTrafico) {
                    mapView.getMapScene().removeMapPolyline(mapPolyline);
                }
                mapPolylinesTrafico.clear();

                if (rutaActual != null) {
                    mapView.getMapScene().removeMapPolyline(rutaActual);
                    rutaActual = null;
                }

                if (coordenadasDestino == null) {
                    showCustomToast("Faltan coordenadas de destino");
                    return;
                }

                TruckSpecifications truckSpecifications = mMapHelper.createTruckSpecifications();
                mMapHelper.generarRuta(MainActivity.this, mapView, coordenadasDestino, truckSpecifications);
                GeoCoordinates destinationCoordinates = coordenadasDestino;
                mMapHelper.flyTo(mapView, destinationCoordinates);
            }
        });

        Intent intent = getIntent();
        coordenadasDestino = mMapHelper.generateCoords(this, intent);

        loading = findViewById(R.id.loading_icon);
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        loading.startAnimation(rotateAnimation);
        this.rotateAnimation = rotateAnimation;
        btnEnviar = findViewById(R.id.btnEnviar);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCerrar = findViewById(R.id.btnCerrar);
        txtToneladas = findViewById(R.id.txtToneladas);
        txtAltura = findViewById(R.id.txtAltura);
        txtAncho = findViewById(R.id.txtAncho);
        txtLargo = findViewById(R.id.txtLargo);
        coordendas_iniciales_input = findViewById(R.id.coordendas_iniciales_input);
        coordendas_finales_input = findViewById(R.id.coordendas_finales_input);
        etInicio = findViewById(R.id.etInicio);
        etDestino = findViewById(R.id.etDestino);
        btnRevertir = findViewById(R.id.btnRevertir);
        btnRuta = findViewById(R.id.btnRuta);
        btnUbicar = findViewById(R.id.btnUbicar);
        btnAgregar = findViewById(R.id.btnAgregar);
        etWaypoint1 = findViewById(R.id.etWaypoint1);
        titWaypoint1 = findViewById(R.id.titWaypoint1);
        btnCerrarWaypoint1 = findViewById(R.id.btnCerrarWaypoint1);
        etWaypoint2 = findViewById(R.id.etWaypoint2);
        titWaypoint2 = findViewById(R.id.titWaypoint2);
        btnCerrarWaypoint2 = findViewById(R.id.btnCerrarWaypoint2);
        etWaypoint3 = findViewById(R.id.etWaypoint3);
        titWaypoint3 = findViewById(R.id.titWaypoint3);
        btnCerrarWaypoint3 = findViewById(R.id.btnCerrarWaypoint3);

        cargaAnimacion = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        btnCerrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapHelper.mostrarMenu(MainActivity.this);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });

        input_coordenada1 = findViewById(R.id.coordendas_iniciales_input);
        input_coordenada2 = findViewById(R.id.coordendas_finales_input);

        btnRevertir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final String direccion1 = input_coordenada1.getText().toString();
                final String direccion2 = input_coordenada2.getText().toString();

                Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.revertir);

                final int duracion = (int) anim.getDuration();
                final int tiempoMedio = duracion / 2;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        input_coordenada1.setText(direccion2);
                        input_coordenada2.setText(direccion1);
                    }
                }, tiempoMedio);

                v.startAnimation(anim);
            }
        });

        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toneladasStr = txtToneladas.getText().toString();
                String altoStr = txtAltura.getText().toString();
                String anchoStr = txtAncho.getText().toString();
                String largoStr = txtLargo.getText().toString();

                if (toneladasStr.isEmpty() || altoStr.isEmpty() || anchoStr.isEmpty() || largoStr.isEmpty()) {
                    showCustomToast("Ingrese todos los valores");
                    return;
                }

                int toneladas = 0;
                int alto = 0;
                int ancho = 0;
                int largo = 0;

                try {
                    toneladas = Integer.parseInt(toneladasStr);
                    alto = Integer.parseInt(altoStr);
                    ancho = Integer.parseInt(anchoStr);
                    largo = Integer.parseInt(largoStr);
                } catch (NumberFormatException e) {
                    showCustomToast("Valores ingresados no válidos");
                    return;
                }

                String errorMessage = "";
                if (toneladas > 360) errorMessage += "Max. 360 ton.";
                if (alto > 8) errorMessage += "Max. 8 m de altura.";
                if (ancho > 9) errorMessage += "Max. 9 m de ancho.";
                if (largo > 20) errorMessage += "Max. 20 m de largo.";

                if (!errorMessage.isEmpty()) {
                    showCustomToast("Valores excedidos: " + errorMessage);
                    return;
                }

                mMapHelper.setTruckSpecifications(toneladas, alto, ancho, largo);
                routingExample2.setTruckSpecifications(toneladas, alto, ancho, largo);

                for (MapPolyline mapPolyline : mapPolylinesTrafico) {
                    mapView.getMapScene().removeMapPolyline(mapPolyline);
                }
                mapPolylinesTrafico.clear();

                if (rutaActual != null) {
                    mapView.getMapScene().removeMapPolyline(rutaActual);
                    rutaActual = null;
                }

                mMapHelper.mostrarMenu(MainActivity.this);
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toneladasStr = txtToneladas.getText().toString();
                String alturaStr = txtAltura.getText().toString();
                String anchoStr = txtAncho.getText().toString();
                String largoStr = txtLargo.getText().toString();

                if (toneladasStr.isEmpty() || alturaStr.isEmpty() || anchoStr.isEmpty() || largoStr.isEmpty()) {
                    showCustomToast("Ingrese todos los valores para guardar la configuración");
                    return;
                }

                int toneladas = Integer.parseInt(toneladasStr);
                int altura = Integer.parseInt(alturaStr);
                int ancho = Integer.parseInt(anchoStr);
                int largo = Integer.parseInt(largoStr);

                String errorMessage = "";
                if (toneladas > 360) errorMessage += "Max. 360 ton.";
                if (altura > 8) errorMessage += "Max. 8 m de altura.";
                if (ancho > 9) errorMessage += "Max. 9 m de ancho.";
                if (largo > 20) errorMessage += "Max. 20 m de largo.";

                if (!errorMessage.isEmpty()) {
                    showCustomToast("Dimensiones excedidas: " + errorMessage);
                    return;
                }

                if (savedSpecs.size() >= MAX_SAVED_SPECS) {
                    showReplaceSpecDialog(toneladas, altura, ancho, largo);
                } else {
                    showSaveSpecDialog(toneladas, altura, ancho, largo);
                }

                SharedPreferences prefs = getSharedPreferences("TruckSpecs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("hasStoredValues", true);
                editor.apply();
            }
        });
        autoCompleteTextView = findViewById(R.id.AutOpciones);
        ArrayAdapter<TruckSpec> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, savedSpecs);
        autoCompleteTextView.setAdapter(adapter);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TruckSpec selectedSpec = (TruckSpec) parent.getItemAtPosition(position);
                txtToneladas.setText(String.valueOf(selectedSpec.toneladas));
                txtAltura.setText(String.valueOf(selectedSpec.altura));
                txtAncho.setText(String.valueOf(selectedSpec.ancho));
                txtLargo.setText(String.valueOf(selectedSpec.largo));

                mMapHelper.setTruckSpecifications(selectedSpec.toneladas, selectedSpec.altura, selectedSpec.ancho, selectedSpec.largo);
                routingExample2.setTruckSpecifications(selectedSpec.toneladas, selectedSpec.altura, selectedSpec.ancho, selectedSpec.largo);
            }
        });

        btnUbicar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownLocation != null) {
                        GeoCoordinates userCoordinates = new GeoCoordinates(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                        reverseGeocode(userCoordinates);
                    } else {
                        showCustomToast("No se obtuvo última ubicación conocida");
                    }
                } else {
                    showCustomToast("Permiso de ubicación no concedido");
                }

                Animation animacion = AnimationUtils.loadAnimation(MainActivity.this, R.anim.click);
                btnUbicar.startAnimation(animacion);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        animacion.cancel();
                    }
                }, 500);
            }
        });

        etInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                coordendas_iniciales_input.setText("");
                isFirstClick = true;
            }
        });

        coordendas_iniciales_input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFirstClick) {
                    coordendas_iniciales_input.setText("");
                    isFirstClick = false;
                }
            }
        });

        btnRuta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inicioText = coordendas_iniciales_input.getText().toString().trim();
                String destinoText = coordendas_finales_input.getText().toString().trim();

                if (!inicioText.isEmpty() && !destinoText.isEmpty()) {
                    calculateRoute();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    Animation animSalida = AnimationUtils.loadAnimation(MainActivity.this, R.anim.salida2);

                    etInicio.startAnimation(animSalida);
                    coordendas_iniciales_input.startAnimation(animSalida);
                    etDestino.startAnimation(animSalida);
                    coordendas_finales_input.startAnimation(animSalida);
                    btnRuta.startAnimation(animSalida);
                    btnUbicar.startAnimation(animSalida);
                    btnRevertir.startAnimation(animSalida);
                    btnAgregar.startAnimation(animSalida);
                    etWaypoint1.startAnimation(animSalida);
                    etWaypoint2.startAnimation(animSalida);
                    etWaypoint3.startAnimation(animSalida);
                    btnCerrarWaypoint1.startAnimation(animSalida);
                    btnCerrarWaypoint2.startAnimation(animSalida);
                    btnCerrarWaypoint3.startAnimation(animSalida);

                    etInicio.setVisibility(View.GONE);
                    coordendas_iniciales_input.setVisibility(View.GONE);
                    etDestino.setVisibility(View.GONE);
                    coordendas_finales_input.setVisibility(View.GONE);
                    btnRuta.setVisibility(View.GONE);
                    btnUbicar.setVisibility(View.GONE);
                    btnRevertir.setVisibility(View.GONE);
                    btnAgregar.setVisibility(View.GONE);
                    etWaypoint1.setVisibility(View.GONE);
                    etWaypoint2.setVisibility(View.GONE);
                    etWaypoint3.setVisibility(View.GONE);
                    btnCerrarWaypoint1.setVisibility(View.GONE);
                    btnCerrarWaypoint2.setVisibility(View.GONE);
                    btnCerrarWaypoint3.setVisibility(View.GONE);
                    clickCount = 0;
                } else {
                    showCustomToast("Ingrese ambas direcciones");
                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        titWaypoint1.setText("");
                        titWaypoint2.setText("");
                        titWaypoint3.setText("");
                    }
                }, 2000);
            }
        });

        btnAgregar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation animEntrada = AnimationUtils.loadAnimation(MainActivity.this, R.anim.entrada2);

                if (clickCount < 3) {
                    clickCount++;
                    switch (clickCount) {
                        case 1:
                            etWaypoint1.startAnimation(animEntrada);
                            titWaypoint1.startAnimation(animEntrada);
                            btnCerrarWaypoint1.startAnimation(animEntrada);
                            etWaypoint1.setVisibility(View.VISIBLE);
                            titWaypoint1.setVisibility(View.VISIBLE);
                            btnCerrarWaypoint1.setVisibility(View.VISIBLE);
                            break;
                        case 2:
                            etWaypoint2.startAnimation(animEntrada);
                            titWaypoint2.startAnimation(animEntrada);
                            btnCerrarWaypoint2.startAnimation(animEntrada);
                            etWaypoint2.setVisibility(View.VISIBLE);
                            titWaypoint2.setVisibility(View.VISIBLE);
                            btnCerrarWaypoint2.setVisibility(View.VISIBLE);
                            break;
                        case 3:
                            etWaypoint3.startAnimation(animEntrada);
                            titWaypoint3.startAnimation(animEntrada);
                            btnCerrarWaypoint3.startAnimation(animEntrada);
                            etWaypoint3.setVisibility(View.VISIBLE);
                            titWaypoint3.setVisibility(View.VISIBLE);
                            btnCerrarWaypoint3.setVisibility(View.VISIBLE);
                            break;
                    }
                } else {
                    showCustomToast("No se pueden agregar más destinos");
                }
            }
        });

        btnCerrarWaypoint1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeWaypoint(etWaypoint1, titWaypoint1, btnCerrarWaypoint1);
                if (etWaypoint2.getVisibility() == View.VISIBLE &&
                        titWaypoint2.getVisibility() == View.VISIBLE &&
                        btnCerrarWaypoint2.getVisibility() == View.VISIBLE) {
                    titWaypoint2.setText("");
                    closeWaypoint(etWaypoint2, titWaypoint2, btnCerrarWaypoint2);
                }
                if (etWaypoint3.getVisibility() == View.VISIBLE &&
                        titWaypoint3.getVisibility() == View.VISIBLE &&
                        btnCerrarWaypoint3.getVisibility() == View.VISIBLE) {
                    titWaypoint3.setText("");
                    closeWaypoint(etWaypoint3, titWaypoint3, btnCerrarWaypoint3);
                }
                titWaypoint1.setText("");
                clickCount--;
            }
        });


        btnCerrarWaypoint2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeWaypoint(etWaypoint2, titWaypoint2, btnCerrarWaypoint2);
                if (etWaypoint3.getVisibility() == View.VISIBLE &&
                        titWaypoint3.getVisibility() == View.VISIBLE &&
                        btnCerrarWaypoint3.getVisibility() == View.VISIBLE) {
                    titWaypoint3.setText("");
                    closeWaypoint(etWaypoint3, titWaypoint3, btnCerrarWaypoint3);
                }
                titWaypoint2.setText("");
                clickCount--;
            }
        });


        btnCerrarWaypoint3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeWaypoint(etWaypoint3, titWaypoint3, btnCerrarWaypoint3);
                titWaypoint3.setText("");
                clickCount--;
            }
        });

        updateAutoCompleteAdapter();

        txtToneladas.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});

        txtAltura.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});

        txtAncho.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});

        txtLargo.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});

        initializeDefaultValues();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mMapHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void permisoConcedido() {
    }

    @Override
    public void permisoDenegado() {
    }

    private void startLocationUpdates() {
        herePositioningProvider.startLocating(new LocationListener() {
            @Override
            public void onLocationUpdated(@NonNull com.here.sdk.core.Location location) {
                GeoCoordinates newCoordinates = location.coordinates;

                if (lastKnownPosition == null) {
                    lastKnownPosition = newCoordinates;
                } else {
                    double lat = ALPHA * newCoordinates.latitude + (1 - ALPHA) * lastKnownPosition.latitude;
                    double lon = ALPHA * newCoordinates.longitude + (1 - ALPHA) * lastKnownPosition.longitude;
                    lastKnownPosition = new GeoCoordinates(lat, lon);
                }

                mMapHelper.addLocationIndicator(mapView, lastKnownPosition, LocationIndicator.IndicatorStyle.NAVIGATION);

                if (rotateAnimation != null) {
                    loading.clearAnimation();
                    rotateAnimation.cancel();
                    rotateAnimation = null;
                }

                if (!rutaGenerada) {
                    mMapHelper.generarRuta(MainActivity.this, mapView, coordenadasDestino, mMapHelper.createTruckSpecifications());
                    rutaGenerada = true;
                }

                View fondoOscuro = findViewById(R.id.fondoOscuro);
                TextView txtEspera = findViewById(R.id.txtEspera);
                TextView txtMovil = findViewById(R.id.txtMovil);
                ImageView loadingIcon = findViewById(R.id.loading_icon);
                TextView txtLocalizando = findViewById(R.id.txtLocalizando);
                ImageView imgMapa = findViewById(R.id.imgMapa);

                fondoOscuro.setVisibility(View.GONE);
                txtEspera.setVisibility(View.GONE);
                txtMovil.setVisibility(View.GONE);
                loadingIcon.setVisibility(View.GONE);
                txtLocalizando.setVisibility(View.GONE);
                imgMapa.setVisibility(View.GONE);

                if (!animacionEjecutada) {
                    mapView.startAnimation(cargaAnimacion);
                    fbPrincipal.setVisibility(View.VISIBLE);
                    fbPrincipal.startAnimation(cargaAnimacion);
                    btnGenerarRuta.setVisibility(View.VISIBLE);
                    btnGenerarRuta.startAnimation(cargaAnimacion);
                    animacionEjecutada = true;
                }
            }
        }, LocationAccuracy.NAVIGATION);
    }

    private void stopLocationUpdates() {
        positioningProvider.stopLocating();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        startLocationUpdates();
        mMapHelper.checkGPSStatus(MainActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        mMapHelper.disposeHERESDK();
    }

    private void expandMenu() {
        Animation fbEntradaBoton = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fb_entrada_boton);
        Animation entrada = AnimationUtils.loadAnimation(MainActivity.this, R.anim.txt_entrada);
        Animation fbEntrada = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fb_entrada);
        fbPrincipal.startAnimation(fbEntrada);
        fbLocalizacion.startAnimation(fbEntradaBoton);
        txtLoca.startAnimation(entrada);
        fbMapas.startAnimation(fbEntradaBoton);
        txtMapas.startAnimation(entrada);
        fbCamionConfig.startAnimation(fbEntradaBoton);
        txtConfig.startAnimation(entrada);
        fbReiniciar.startAnimation(fbEntradaBoton);
        txtRestablecer.startAnimation(entrada);
        fbRuta.startAnimation(fbEntradaBoton);
        txtRuta.startAnimation(entrada);
        fbLimpiar.startAnimation(fbEntradaBoton);
        txtEliminar.startAnimation(entrada);
        fbSalir.startAnimation(fbEntradaBoton);
        txtSalir.startAnimation(entrada);

        fbLocalizacion.setVisibility(View.VISIBLE);
        txtLoca.setVisibility(View.VISIBLE);
        fbMapas.setVisibility(View.VISIBLE);
        txtMapas.setVisibility(View.VISIBLE);
        fbCamionConfig.setVisibility(View.VISIBLE);
        txtConfig.setVisibility(View.VISIBLE);
        fbReiniciar.setVisibility(View.VISIBLE);
        txtRestablecer.setVisibility(View.VISIBLE);
        fbRuta.setVisibility(View.VISIBLE);
        txtRuta.setVisibility(View.VISIBLE);
        fbLimpiar.setVisibility(View.VISIBLE);
        txtEliminar.setVisibility(View.VISIBLE);
        fbSalir.setVisibility(View.VISIBLE);
        txtSalir.setVisibility(View.VISIBLE);

        fbLocalizacion.setEnabled(true);
        txtLoca.setEnabled(true);
        fbMapas.setEnabled(true);
        txtMapas.setEnabled(true);
        fbCamionConfig.setEnabled(true);
        txtConfig.setEnabled(true);
        fbReiniciar.setEnabled(true);
        txtRestablecer.setEnabled(true);
        fbRuta.setEnabled(true);
        txtRuta.setEnabled(true);
        fbLimpiar.setEnabled(true);
        txtEliminar.setEnabled(true);
        fbSalir.setEnabled(true);
        txtSalir.setEnabled(true);

        isMenuOpen = true;
    }

    private void collapseMenu() {
        Animation salida = AnimationUtils.loadAnimation(MainActivity.this, R.anim.txt_salida);
        Animation animSalida = AnimationUtils.loadAnimation(MainActivity.this, R.anim.salida_fb);
        Animation fbSalida = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fb_salida);
        fbPrincipal.startAnimation(fbSalida);
        fbLocalizacion.startAnimation(animSalida);
        txtLoca.startAnimation(salida);
        fbMapas.startAnimation(animSalida);
        txtMapas.startAnimation(salida);
        fbCamionConfig.startAnimation(animSalida);
        txtConfig.startAnimation(salida);
        fbReiniciar.startAnimation(animSalida);
        txtRestablecer.startAnimation(salida);
        fbRuta.startAnimation(animSalida);
        txtRuta.startAnimation(salida);
        fbLimpiar.startAnimation(animSalida);
        txtEliminar.startAnimation(salida);
        fbSalir.startAnimation(animSalida);
        txtSalir.startAnimation(salida);

        long animationDuration = animSalida.getDuration();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fbLocalizacion.setVisibility(View.GONE);
                txtLoca.setVisibility(View.GONE);
                fbMapas.setVisibility(View.GONE);
                txtMapas.setVisibility(View.GONE);
                fbCamionConfig.setVisibility(View.GONE);
                txtConfig.setVisibility(View.GONE);
                fbReiniciar.setVisibility(View.GONE);
                txtRestablecer.setVisibility(View.GONE);
                fbRuta.setVisibility(View.GONE);
                txtRuta.setVisibility(View.GONE);
                fbLimpiar.setVisibility(View.GONE);
                txtEliminar.setVisibility(View.GONE);
                fbSalir.setVisibility(View.GONE);
                txtSalir.setVisibility(View.GONE);

                fbLocalizacion.setEnabled(false);
                txtLoca.setEnabled(false);
                fbMapas.setEnabled(false);
                txtMapas.setEnabled(false);
                fbCamionConfig.setEnabled(false);
                txtConfig.setEnabled(false);
                fbReiniciar.setEnabled(false);
                txtRestablecer.setEnabled(false);
                fbRuta.setEnabled(false);
                txtRuta.setEnabled(false);
                fbLimpiar.setEnabled(false);
                txtEliminar.setEnabled(false);
                fbSalir.setEnabled(false);
                txtSalir.setEnabled(false);

                isMenuOpen = false;
            }
        }, animationDuration);
    }

    private void closeWaypoint(TextInputLayout etWaypoint, TextView titWaypoint, ImageButton btnCerrarWaypoint) {
        Animation animSalida = AnimationUtils.loadAnimation(MainActivity.this, R.anim.salida2);
        etWaypoint.startAnimation(animSalida);
        titWaypoint.startAnimation(animSalida);
        btnCerrarWaypoint.startAnimation(animSalida);
        etWaypoint.setVisibility(View.GONE);
        titWaypoint.setVisibility(View.GONE);
        btnCerrarWaypoint.setVisibility(View.GONE);
        updateClickCount();
        clickCount--;
    }

    private void updateClickCount() {
        if (etWaypoint1.getVisibility() == View.GONE && etWaypoint2.getVisibility() == View.GONE && etWaypoint3.getVisibility() == View.GONE) {
            clickCount = 0;
        } else if (etWaypoint1.getVisibility() == View.GONE && etWaypoint2.getVisibility() == View.GONE) {
            clickCount = 1;
        } else if (etWaypoint1.getVisibility() == View.GONE) {
            clickCount = 2;
        } else {
            clickCount = 3;
        }
    }

    private void geocodeAddress(String address, final Consumer<GeoCoordinates> callback) {
        AddressQuery query = new AddressQuery(address);
        SearchOptions options = new SearchOptions();
        options.languageCode = LanguageCode.ES_ES;
        options.maxItems = 1;

        searchEngine.search(query, options, new SearchCallback() {
            @Override
            public void onSearchCompleted(SearchError searchError, List<Place> list) {
                if (searchError != null) {
                    callback.accept(null);
                    return;
                }
                if (list.isEmpty()) {
                    runOnUiThread(() -> {
                        showCustomToast("No se encontró la dirección");
                    });
                    callback.accept(null);
                    return;
                }
                Place place = list.get(0);
                callback.accept(place.getGeoCoordinates());
            }
        });
    }

    private void getWaypoints(final Consumer<List<GeoCoordinates>> callback) {
        List<GeoCoordinates> waypoints = new ArrayList<>();
        processInput(titWaypoint1.getText().toString(), waypoint1 -> {
            if (waypoint1 != null) waypoints.add(waypoint1);
            processInput(titWaypoint2.getText().toString(), waypoint2 -> {
                if (waypoint2 != null) waypoints.add(waypoint2);
                processInput(titWaypoint3.getText().toString(), waypoint3 -> {
                    if (waypoint3 != null) waypoints.add(waypoint3);
                    callback.accept(waypoints);
                });
            });
        });
    }

    private void calculateRoute() {
        String startInput = input_coordenada1.getText().toString().trim();
        String destInput = input_coordenada2.getText().toString().trim();

        if (startInput.isEmpty() || destInput.isEmpty()) {
            showCustomToast("Ingrese punto de inicio y destino");
            return;
        }

        processInput(startInput, startCoordinates -> {
            if (startCoordinates == null) {
                showCustomToast("No se procesó el punto de inicio");
                return;
            }
            processInput(destInput, destCoordinates -> {
                if (destCoordinates == null) {
                    showCustomToast("No se procesó el punto de destino");
                    return;
                }
                getWaypoints(waypoints -> {
                    routingExample2.addRoute(startCoordinates, destCoordinates, waypoints);
                });
            });
        });
    }

    private void processInput(String input, Consumer<GeoCoordinates> callback) {
        if (esCoordenada(input)) {
            callback.accept(obtenerCoordenadas(input));
        } else {
            geocodeAddress(input, callback);
        }
    }

    private boolean esCoordenada(String entrada) {
        String patternCoordenada = "^(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?)$";
        return entrada.matches(patternCoordenada);
    }

    private GeoCoordinates obtenerCoordenadas(String entrada) {
        String[] coordenadas = entrada.split(",");
        double latitud = Double.parseDouble(coordenadas[0].trim());
        double longitud = Double.parseDouble(coordenadas[1].trim());

        if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            showCustomToast("Coordenadas fuera de rango");
            return null;
        }

        return new GeoCoordinates(latitud, longitud);
    }

    private void reverseGeocode(GeoCoordinates coordinates) {
        SearchOptions searchOptions = new SearchOptions();
        searchOptions.languageCode = LanguageCode.ES_ES;
        searchOptions.maxItems = 1;

        searchEngine.search(coordinates, searchOptions, new SearchCallback() {
            @Override
            public void onSearchCompleted(SearchError searchError, List<Place> list) {
                if (searchError != null) {
                    runOnUiThread(() -> showCustomToast("Error al obtener la dirección: " + searchError.toString()));
                    return;
                }
                if (list.isEmpty()) {
                    runOnUiThread(() -> showCustomToast("No se encontró la dirección"));
                    return;
                }
                Place place = list.get(0);
                Address address = place.getAddress();
                String fullAddress = formatAddress(address);

                runOnUiThread(() -> {
                    TextInputEditText coordenadasInicialesInput = findViewById(R.id.coordendas_iniciales_input);
                    coordenadasInicialesInput.setText(fullAddress);
                    showCustomToast("Dirección obtenida");
                });
            }
        });
    }

    private String formatAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        if (address.street != null && !address.street.isEmpty()) {
            sb.append(address.street);
            if (address.houseNumOrName != null && !address.houseNumOrName.isEmpty()) {
                sb.append(" ").append(address.houseNumOrName);
            }
        }
        if (address.city != null && !address.city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.city);
        }
        if (address.state != null && !address.state.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.state);
        }
        if (address.postalCode != null && !address.postalCode.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(address.postalCode);
        }
        return sb.toString();
    }

    public class TruckSpec {
        public String name;
        public int toneladas;
        public int altura;
        public int ancho;
        public int largo;

        public TruckSpec(String name, int toneladas, int altura, int ancho, int largo) {
            this.name = name;
            this.toneladas = toneladas;
            this.altura = altura;
            this.ancho = ancho;
            this.largo = largo;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private void showSaveSpecDialog(final int toneladas, final int altura, final int ancho, final int largo) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_save_spec, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        Button buttonOK = dialogView.findViewById(R.id.button_ok);
        Button buttonCancel = dialogView.findViewById(R.id.button_cancel);
        EditText input = dialogView.findViewById(R.id.editText_specName);

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String specName = input.getText().toString();
                if (!specName.isEmpty()) {
                    TruckSpec newSpec = new TruckSpec(specName, toneladas, altura, ancho, largo);
                    savedSpecs.add(newSpec);
                    ((ArrayAdapter<TruckSpec>) autoCompleteTextView.getAdapter()).notifyDataSetChanged();
                    saveSpecs();
                    showCustomToast("Especificaciones guardadas");
                    dialog.dismiss();
                } else {
                    showCustomToast("Ingrese nombre de configuración");
                }
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showReplaceSpecDialog(final int toneladas, final int altura, final int ancho, final int largo) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_alert_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        Button positiveButton = dialogView.findViewById(R.id.positiveButton);
        Button negativeButton = dialogView.findViewById(R.id.negativeButton);

        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showSelectSpecToReplaceDialog(toneladas, altura, ancho, largo);
            }
        });

        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showSelectSpecToReplaceDialog(final int toneladas, final int altura, final int ancho, final int largo) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_select_spec_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        ListView listView = dialogView.findViewById(R.id.specListView);
        final String[] specNames = new String[savedSpecs.size()];
        for (int i = 0; i < savedSpecs.size(); i++) {
            specNames[i] = savedSpecs.get(i).name;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, specNames);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                replaceSpec(position, toneladas, altura, ancho, largo);
            }
        });

        Button cancelButton = dialogView.findViewById(R.id.negativeButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    private void replaceSpec(final int index, int toneladas, int altura, int ancho, int largo) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_replace_spec_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        final EditText input = dialogView.findViewById(R.id.specNameInput);

        Button positiveButton = dialogView.findViewById(R.id.positiveButton);
        Button negativeButton = dialogView.findViewById(R.id.negativeButton);

        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String specName = input.getText().toString();
                if (!specName.isEmpty()) {
                    TruckSpec newSpec = new TruckSpec(specName, toneladas, altura, ancho, largo);
                    savedSpecs.set(index, newSpec);
                    updateAutoCompleteAdapter();
                    saveSpecs();
                    showCustomToast("Especificaciones reemplazadas");
                    autoCompleteTextView.dismissDropDown();
                    autoCompleteTextView.setText("");
                    autoCompleteTextView.setHint("Selecciona una opción");
                    txtToneladas.setText("");
                    txtAltura.setText("");
                    txtAncho.setText("");
                    txtLargo.setText("");
                    dialog.dismiss();
                } else {
                    showCustomToast("Ingrese nombre de configuración");
                }
            }
        });

        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void saveSpecs() {
        SharedPreferences sharedPreferences = getSharedPreferences("TruckSpecs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(savedSpecs);
        editor.putString("savedSpecs", json);
        editor.apply();
    }

    private void loadSpecs() {
        SharedPreferences sharedPreferences = getSharedPreferences("TruckSpecs", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("savedSpecs", null);
        Type type = new TypeToken<ArrayList<TruckSpec>>() {
        }.getType();
        if (json != null) {
            savedSpecs = gson.fromJson(json, type);
        } else {
            savedSpecs = new ArrayList<>();
        }
    }

    public void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);

        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 200);
        toast.show();
    }

    private void updateAutoCompleteAdapter() {
        ArrayAdapter<TruckSpec> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, savedSpecs);
        autoCompleteTextView.setAdapter(adapter);
    }

    private void initializeDefaultValues() {
        txtToneladas.setText(String.valueOf(mMapHelper.DEFAULT_TONELADAS));
        txtAltura.setText(String.valueOf(mMapHelper.DEFAULT_ALTO));
        txtAncho.setText(String.valueOf(mMapHelper.DEFAULT_ANCHO));
        txtLargo.setText(String.valueOf(mMapHelper.DEFAULT_LARGO));

        mMapHelper.setTruckSpecifications(mMapHelper.DEFAULT_TONELADAS, mMapHelper.DEFAULT_ALTO, mMapHelper.DEFAULT_ANCHO, mMapHelper.DEFAULT_LARGO);
        routingExample2.setTruckSpecifications(mMapHelper.DEFAULT_TONELADAS, mMapHelper.DEFAULT_ALTO, mMapHelper.DEFAULT_ANCHO, mMapHelper.DEFAULT_LARGO);
    }
}