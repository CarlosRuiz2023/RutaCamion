package com.itsmarts.SmartRouteTruckApp;/*
 * Copyright (C) 2019-2024 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.here.sdk.core.Color;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.mapview.LineCap;
import com.here.sdk.mapview.MapCamera;
import com.here.sdk.mapview.MapImage;
import com.here.sdk.mapview.MapImageFactory;
import com.here.sdk.mapview.MapMarker;
import com.here.sdk.mapview.MapMeasure;
import com.here.sdk.mapview.MapMeasureDependentRenderSize;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapView;
import com.here.sdk.mapview.RenderSize;
import com.here.sdk.routing.CalculateRouteCallback;
import com.here.sdk.routing.Maneuver;
import com.here.sdk.routing.ManeuverAction;
import com.here.sdk.routing.PaymentMethod;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.RoutingError;
import com.here.sdk.routing.Section;
import com.here.sdk.routing.SectionNotice;
import com.here.sdk.routing.Span;
import com.here.sdk.routing.Toll;
import com.here.sdk.routing.TollFare;
import com.here.sdk.routing.TrafficSpeed;
import com.here.sdk.routing.TruckOptions;
import com.here.sdk.routing.Waypoint;
import com.here.sdk.transport.TruckSpecifications;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class RoutingExample {
    private static final String TAG = RoutingExample.class.getName();
    private final Context context;
    private final MapView mapView;
    private final List<MapMarker> mapMarkerList = new ArrayList<>();
    private final List<MapPolyline> mapPolylines = new ArrayList<>();
    private static RoutingEngine routingEngine;
    private GeoCoordinates startGeoCoordinates, destinationGeoCoordinates;
    private final VoiceAssistant voiceAssistant;

    public RoutingExample(Context context, MapView mapView, GeoCoordinates coordendas) {
        this.context = context;
        this.mapView = mapView;
        this.voiceAssistant = new VoiceAssistant(context);

        MapCamera camera = mapView.getCamera();
        double distanceInMeters = 1000 * 10;
        MapMeasure mapMeasureZoom = new MapMeasure(MapMeasure.Kind.DISTANCE, distanceInMeters);
        camera.lookAt(coordendas, mapMeasureZoom);

        if (routingEngine == null) {
            try {
                routingEngine = new RoutingEngine();
            } catch (InstantiationErrorException e) {
                throw new RuntimeException("Initialization of RoutingEngine failed: " + e.error.name());
            }
        }
    }

    public void addRoute(@Nullable GeoCoordinates coordenadas_iniciales, @Nullable GeoCoordinates coordenadas_destino, TruckSpecifications truckSpecifications) {
        clearRoute();
        clearMap();
        if (coordenadas_iniciales == null || coordenadas_destino == null) {
            return;
        }

        startGeoCoordinates = coordenadas_iniciales;
        destinationGeoCoordinates = coordenadas_destino;
        Waypoint startWaypoint = new Waypoint(startGeoCoordinates);
        Waypoint destinationWaypoint = new Waypoint(destinationGeoCoordinates);

        List<Waypoint> waypoints = new ArrayList<>(Arrays.asList(startWaypoint, destinationWaypoint));

        TruckOptions truckOptions = new TruckOptions();
        truckOptions.routeOptions.enableTolls = true;
        truckOptions.truckSpecifications = truckSpecifications;

        if (routingEngine == null) {
            try {
                routingEngine = new RoutingEngine();
            } catch (InstantiationErrorException e) {
                throw new RuntimeException("Initialization of RoutingEngine failed: " + e.error.name());
            }
        }

        routingEngine.calculateRoute(
                waypoints,
                truckOptions,
                new CalculateRouteCallback() {
                    @Override
                    public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable List<Route> routes) {
                        if (routingError == null) {
                            Route route = routes.get(0);
                            showTrafficOnRoute(route);
                            showRouteDetails(route);
                            showRouteOnMap(route);
                            logRouteSectionDetails(route);
                            logRouteViolations(route);
                            logTollDetails(route);
                            provideVoiceInstructions(route);
                        } else {
                            showDialog("Error while calculating a route:", routingError.toString());
                        }
                    }
                }
        );
    }

    private void provideVoiceInstructions(Route route) {
        for (Section section : route.getSections()) {
            for (Maneuver maneuver : section.getManeuvers()) {
                String instruction = maneuver.getText();
                voiceAssistant.speak(instruction);
                Log.d("Buscar", instruction);
                Log.d("Buscar man", String.valueOf(maneuver));
            }
        }
    }

    private void logRouteViolations(Route route) {
        for (Section section : route.getSections()) {
            for (SectionNotice notice : section.getSectionNotices()) {
                Log.e(TAG, "This route contains the following warning: " + notice.code.toString());
            }
        }
    }

    private void logRouteSectionDetails(Route route) {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");

        for (int i = 0; i < route.getSections().size(); i++) {
            Section section = route.getSections().get(i);

            Log.d(TAG, "Route Section : " + (i + 1));
            Log.d(TAG, "Route Section Departure Time : "
                    + dateFormat.format(section.getDepartureLocationTime().localTime));
            Log.d(TAG, "Route Section Arrival Time : "
                    + dateFormat.format(section.getArrivalLocationTime().localTime));
            Log.d(TAG, "Route Section length : " + section.getLengthInMeters() + " m");
            Log.d(TAG, "Route Section duration : " + section.getDuration().getSeconds() + " s");
        }
    }

    private void logTollDetails(Route route) {
        for (Section section : route.getSections()) {
            List<Span> spans = section.getSpans();
            List<Toll> tolls = section.getTolls();
            if (!tolls.isEmpty()) {
                Log.d(TAG, "Attention: This route may require tolls to be paid.");
            }
            for (Toll toll : tolls) {
                Log.d(TAG, "Toll information valid for this list of spans:");
                Log.d(TAG, "Toll system: " + toll.tollSystem);
                Log.d(TAG, "Toll country code (ISO-3166-1 alpha-3): " + toll.countryCode);
                Log.d(TAG, "Toll fare information: ");
                for (TollFare tollFare : toll.fares) {
                    Log.d(TAG, "Toll price: " + tollFare.price + " " + tollFare.currency);
                    for (PaymentMethod paymentMethod : tollFare.paymentMethods) {
                        Log.d(TAG, "Accepted payment methods for this price: " + paymentMethod.name());
                    }
                }
            }
        }
    }

    private void showRouteDetails(Route route) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomDialogTheme);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_route_details, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView dialogDuration = dialogView.findViewById(R.id.dialog_duration);
        TextView dialogTrafficDelay = dialogView.findViewById(R.id.dialog_traffic_delay);
        TextView dialogDistance = dialogView.findViewById(R.id.dialog_distance);

        long estimatedTravelTimeInSeconds = route.getDuration().getSeconds();
        long estimatedTrafficDelayInSeconds = route.getTrafficDelay().getSeconds();
        int lengthInMeters = route.getLengthInMeters();

        String durationString = formatDuration(estimatedTravelTimeInSeconds);
        String trafficDelayString = formatDuration(estimatedTrafficDelayInSeconds);
        String distanceString = formatDistance(lengthInMeters);

        dialogTitle.setText("Detalles de la ruta");
        dialogDuration.setText("Trayecto: " + durationString);
        dialogTrafficDelay.setText("Retraso por tráfico: " + trafficDelayString);
        dialogDistance.setText("Distancia: " + distanceString);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(context, R.color.TRANSPARENT)));
            int maxWidth = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.5);
            dialog.getWindow().setLayout(maxWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void showRouteOnMap(Route route) {
        clearMap();

        GeoPolyline routeGeoPolyline = route.getGeometry();
        float widthInPixels = 20;
        Color polylineColor = new Color(0, (float) 0.56, (float) 0.54, (float) 0.63);
        MapPolyline routeMapPolyline = null;
        try {
            routeMapPolyline = new MapPolyline(routeGeoPolyline, new MapPolyline.SolidRepresentation(
                    new MapMeasureDependentRenderSize(RenderSize.Unit.PIXELS, widthInPixels),
                    polylineColor,
                    LineCap.ROUND));
        } catch (MapPolyline.Representation.InstantiationException e) {
            Log.e("MapPolyline Representation Exception:", e.error.name());
        } catch (MapMeasureDependentRenderSize.InstantiationException e) {
            Log.e("MapMeasureDependentRenderSize Exception:", e.error.name());
        }
        MainActivity mainActivity = (MainActivity) context;
        mainActivity.rutaActual = routeMapPolyline;

        mapView.getMapScene().addMapPolyline(routeMapPolyline);
        mapPolylines.add(routeMapPolyline);

        showTrafficOnRoute(route);

        List<Section> sections = route.getSections();
        for (Section section : sections) {
            logManeuverInstructions(section);
        }
    }

    private void logManeuverInstructions(Section section) {
        Log.d(TAG, "Log maneuver instructions per route section:");
        List<Maneuver> maneuverInstructions = section.getManeuvers();
        for (Maneuver maneuverInstruction : maneuverInstructions) {
            ManeuverAction maneuverAction = maneuverInstruction.getAction();
            GeoCoordinates maneuverLocation = maneuverInstruction.getCoordinates();
            String maneuverInfo = maneuverInstruction.getText()
                    + ", Action: " + maneuverAction.name()
                    + ", Location: " + maneuverLocation.toString();
            Log.d(TAG, maneuverInfo);
        }
    }

    public void clearMap() {
        clearWaypointMapMarker();
        clearRoute();
    }

    private void clearWaypointMapMarker() {
        for (MapMarker mapMarker : mapMarkerList) {
            mapView.getMapScene().removeMapMarker(mapMarker);
        }
        mapMarkerList.clear();
    }

    public void clearRoute() {
        for (MapPolyline mapPolyline : mapPolylines) {
            mapView.getMapScene().removeMapPolyline(mapPolyline);
        }
        mapPolylines.clear();
        clearWaypointMapMarker();
        clearPolylines();
    }

    private void showTrafficOnRoute(Route route) {
        if (route.getLengthInMeters() / 1000 > 5000) {
            Log.d(TAG, "Skip showing traffic-on-route for longer routes.");
            return;
        }
        for (Section section : route.getSections()) {
            for (Span span : section.getSpans()) {
                TrafficSpeed trafficSpeed = span.getTrafficSpeed();
                Color lineColor = getTrafficColor(trafficSpeed.jamFactor);
                if (lineColor == null) {
                    continue;
                }
                float widthInPixels = 10;
                MapPolyline trafficSpanMapPolyline = null;
                try {
                    trafficSpanMapPolyline = new MapPolyline(span.getGeometry(), new MapPolyline.SolidRepresentation(
                            new MapMeasureDependentRenderSize(RenderSize.Unit.PIXELS, widthInPixels),
                            lineColor,
                            LineCap.ROUND));
                } catch (MapPolyline.Representation.InstantiationException e) {
                    Log.e("MapPolyline Representation Exception:", e.error.name());
                } catch (MapMeasureDependentRenderSize.InstantiationException e) {
                    Log.e("MapMeasureDependentRenderSize Exception:", e.error.name());
                }

                mapView.getMapScene().addMapPolyline(trafficSpanMapPolyline);
                MainActivity mainActivity = (MainActivity) context;
                mainActivity.mapPolylinesTrafico.add(trafficSpanMapPolyline);
                mapPolylines.add(trafficSpanMapPolyline);
            }
        }
    }

    @Nullable
    private Color getTrafficColor(Double jamFactor) {
        if (jamFactor == null || jamFactor < 4) {
            return null;
        } else if (jamFactor >= 4 && jamFactor < 8) {
            return Color.valueOf(1, 1, 0, 0.63f);
        } else if (jamFactor >= 8 && jamFactor < 10) {
            return Color.valueOf(1, 0, 0, 0.63f);
        }
        return Color.valueOf(0, 0, 0, 0.63f);
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format(Locale.getDefault(), "%d horas %02d minutos", hours, minutes);
    }

    private String formatDistance(int meters) {
        int kilometers = meters / 1000;
        int remainingMeters = meters % 1000;
        return String.format(Locale.getDefault(), "%d.%03d km", kilometers, remainingMeters);
    }

    private void showDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("Aceptar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void clearPolylines() {
        for (MapPolyline mapPolyline : mapPolylines) {
            mapView.getMapScene().removeMapPolyline(mapPolyline);
        }
        mapPolylines.clear();
    }
}