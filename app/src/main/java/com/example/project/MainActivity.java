package com.example.hongkongmonuments;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.location.RouteTrackerLocationDataSource;
import com.esri.arcgisruntime.location.SimulatedLocationDataSource;
import com.esri.arcgisruntime.location.SimulationParameters;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.navigation.ReroutingParameters;
import com.esri.arcgisruntime.navigation.RouteTracker;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.geocode.SuggestParameters;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {

    Double initLat;
    Double initLng;
    Double finalLat = 0.0;
    Double finalLng = 0.0;
    ProgressDialog mProgressDialog;
    Button btnSearch;
    EditText etMonLoc;
    EditText etMyLocation;
    private MapView mMapView;
    private Callout mCallout;
    private RouteTask mRouteTask;
    private RouteTracker mRouteTracker;
    private Graphic mRouteAheadGraphic;
    private Graphic mRouteTraveledGraphic;
    private SimulatedLocationDataSource mSimulatedLocationDataSource;
    private LocatorTask mLocatorTask;
    private static final String TAG = "MainActivity";

    private final String COLUMN_NAME_ADDRESS = "address";
    private final String[] mColumnNames = {BaseColumns._ID, COLUMN_NAME_ADDRESS};
    private SearchView mAddressSearchView;


    private GeocodeParameters mAddressGeocodeParameters;
    private PictureMarkerSymbol mPinSourceSymbol;

    private ServiceFeatureTable mServiceFeatureTable;

    private MapView widget_myMapView;



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading...");

        // authentication with an API key or named user is required to access basemaps and other
        // location services
        ArcGISRuntimeEnvironment.setApiKey("AAPKf833cd8d695b45ab8b6f6efd78f92d9coB6kIROKSbo51dhvmMW2E0-SM2K1SkxDUeCa1yUoTLbljUMC3ZiuVX7GY0sKcmcV");

        // inflate MapView from layout
        mMapView = findViewById(R.id.mapView);
        // create an ArcGISMap with a topographic basemap
        btnSearch = findViewById(R.id.btn_direction);
        etMonLoc = findViewById(R.id.et_mon_loc);
        etMyLocation = findViewById(R.id.et_my_loc);
        mAddressSearchView = findViewById(R.id.addressSearchView);

      /*  ArcGISVectorTiledLayer mVectorTiledLayer = new ArcGISVectorTiledLayer(getString(R.string.nrouting_service));
        // set tiled layer as basemap
        Basemap basemap = new Basemap(mVectorTiledLayer);
        // create a map with the basemap*/

        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_STREETS);
        //ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);
        // set the ArcGISMap to the MapView
        mMapView.setMap(map);
        // set a viewpoint

        mMapView.setViewpoint(new Viewpoint(22.3193, 114.1694, 200000.0));
        // get the callout that shows attributes
        mCallout = mMapView.getCallout();
        // create the service feature table
        mServiceFeatureTable = new ServiceFeatureTable(getResources().getString(R.string.hong_kong_service));
        // create the feature layer using the service feature table
        final FeatureLayer featureLayer = new FeatureLayer(mServiceFeatureTable);
        // add the layer to the map
        map.getOperationalLayers().add(featureLayer);

        map.addDoneLoadingListener(() -> {
            if (map.getLoadStatus() == LoadStatus.LOADED) {
                setupAddressSearchView();
            } else {
                Log.e(TAG, "Map failed to load: " + map.getLoadError().getMessage());
                Toast.makeText(
                        getApplicationContext(),
                        "Map failed to load: " + map.getLoadError().getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        // create a LocatorTask from an online service
        mLocatorTask = new LocatorTask("https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer");

        // set an on touch listener to listen for click events
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // remove any existing callouts
                if (mCallout.isShowing()) {
                    mCallout.dismiss();
                }
                // get the point that was clicked and convert it to a point in map coordinates
                final Point screenPoint = new Point(Math.round(e.getX()), Math.round(e.getY()));
                // create a selection tolerance
                int tolerance = 10;
                // use identifyLayerAsync to get tapped features
                final ListenableFuture<IdentifyLayerResult> identifyLayerResultListenableFuture = mMapView
                        .identifyLayerAsync(featureLayer, screenPoint, tolerance, false, 1);
                identifyLayerResultListenableFuture.addDoneListener(() -> {
                    try {
                        IdentifyLayerResult identifyLayerResult = identifyLayerResultListenableFuture.get();
                        // create a textview to display field values
                        TextView calloutContent = new TextView(getApplicationContext());
                        calloutContent.setTextColor(Color.BLACK);
                        calloutContent.setSingleLine(false);
                        calloutContent.setVerticalScrollBarEnabled(true);
                        calloutContent.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                        calloutContent.setMovementMethod(new ScrollingMovementMethod());
                        calloutContent.setLines(5);
                        for (GeoElement element : identifyLayerResult.getElements()) {
                            Feature feature = (Feature) element;
                            // create a map of all available attributes as name value pairs
                            Map<String, Object> attr = feature.getAttributes();
                            Set<String> keys = attr.keySet();
                            for (String key : keys) {
                                Object value = attr.get(key);
                                // format observed field value as date
                                Log.i(TAG, "onSingleTapConfirmed: " + key + ":" + value);
                                if (value instanceof GregorianCalendar) {
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
                                    value = simpleDateFormat.format(((GregorianCalendar) value).getTime());
                                }
                                if (key.equalsIgnoreCase("Latitude")) {
                                    initLat = (Double) value;
                                    Log.i(TAG, "onSingleTapConfirmed: ---->" + initLat);
                                }

                                if (key.equalsIgnoreCase("Longitude")) {
                                    initLng = (Double) value;
                                    Log.i(TAG, "onSingleTapConfirmed:-----> " + initLng);
                                }

                                if (key.equalsIgnoreCase("Name")) {
                                    setEditValue((String) value);
                                    Log.i(TAG, "onSingleTapConfirmed: " + value);
                                }
                                // append name value pairs to text view
                                calloutContent.append(key + " | " + value + "\n");
                            }
                            // center the mapview on selected feature
                            Envelope envelope = feature.getGeometry().getExtent();
                            mMapView.setViewpointGeometryAsync(envelope, 200);
                            // show callout
                            mCallout.setLocation(envelope.getCenter());
                            mCallout.setContent(calloutContent);
                            mCallout.show();
                        }
                    } catch (Exception e1) {
                        Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e1.getMessage());
                    }
                });
                return super.onSingleTapConfirmed(e);
            }
        });

        // create a graphics overlay to hold our route graphics
        GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
        mMapView.getGraphicsOverlays().add(graphicsOverlay);

        //mRouteTask = new RouteTask(getApplicationContext(), getString(R.string.routing_word_service));
        btnSearch.setOnClickListener(v -> {

            String place1 = etMonLoc.getText().toString().trim();

            if (finalLat == 0.0 || finalLng == 0.0) {
                Toast.makeText(this, "Please select your location", Toast.LENGTH_SHORT).show();
                return;
            }

            if (place1.isEmpty()) {
                Toast.makeText(this, "Please select position of the monument.", Toast.LENGTH_SHORT).show();
                return;
            }

            // mProgressDialog.show();
            // clear any graphics from the current graphics overlay
            mMapView.getGraphicsOverlays().get(0).getGraphics().clear();

            RouteTask routeTask = new RouteTask(this, getString(R.string.routing_word_service));
            ListenableFuture<RouteParameters> routeParametersFuture = routeTask.createDefaultParametersAsync();
            routeParametersFuture.addDoneListener(() -> {

                try {
                    // define the route parameters
                    RouteParameters routeParameters = routeParametersFuture.get();
                    routeParameters.setStops(getStops());
                    routeParameters.setReturnDirections(true);
                    routeParameters.setReturnStops(true);
                    routeParameters.setReturnRoutes(true);
                    ListenableFuture<RouteResult> routeResultFuture = routeTask.solveRouteAsync(routeParameters);
                    routeParametersFuture.addDoneListener(() -> {
                        try {
                            // get the route geometry from the route result
                            RouteResult routeResult = routeResultFuture.get();
                            Polyline routeGeometry = routeResult.getRoutes().get(0).getRouteGeometry();
                            // create a graphic for the route geometry
                            Graphic routeGraphic = new Graphic(routeGeometry,
                                    new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 5f));
                            // add it to the graphics overlay
                            mMapView.getGraphicsOverlays().get(0).getGraphics().add(routeGraphic);
                            // set the map view view point to show the whole route
                            mMapView.setViewpointAsync(new Viewpoint(routeGeometry.getExtent()));

                            // create a button to start navigation with the given route

                            startNavigation(routeTask, routeParameters, routeResult);
                        } catch (ExecutionException | InterruptedException e) {
                            String error = "Error creating default route parameters: " + e.getMessage();
                            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, error);
                        }
                    });
                } catch (InterruptedException | ExecutionException e) {
                    String error = "Error getting the route result " + e.getMessage();
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, error);
                }
            });


        });

    }

    private void setEditValue(String key) {
        if (key != null)
            etMonLoc.setText(key);
    }

    private void startNavigation(RouteTask routeTask, RouteParameters routeParameters, RouteResult routeResult) {

        // clear any graphics from the current graphics overlay
        mMapView.getGraphicsOverlays().get(0).getGraphics().clear();

        // get the route's geometry from the route result
        Polyline routeGeometry = routeResult.getRoutes().get(0).getRouteGeometry();
        // create a graphic (with a dashed line symbol) to represent the route
        mRouteAheadGraphic = new Graphic(routeGeometry,
                new SimpleLineSymbol(SimpleLineSymbol.Style.DASH, Color.MAGENTA, 5f));
        mMapView.getGraphicsOverlays().get(0).getGraphics().add(mRouteAheadGraphic);
        // create a graphic (solid) to represent the route that's been traveled (initially empty)
        mRouteTraveledGraphic = new Graphic(routeGeometry,
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLUE, 5f));
        mMapView.getGraphicsOverlays().get(0).getGraphics().add(mRouteTraveledGraphic);

        // get the map view's location display
        LocationDisplay locationDisplay = mMapView.getLocationDisplay();
        // set up a simulated location data source which simulates movement along the route
        mSimulatedLocationDataSource = new SimulatedLocationDataSource();
        SimulationParameters simulationParameters = new SimulationParameters(Calendar.getInstance(), 35, 5, 5);
        mSimulatedLocationDataSource.setLocations(routeGeometry, simulationParameters);

        // set up a RouteTracker for navigation along the calculated route
        mRouteTracker = new RouteTracker(getApplicationContext(), routeResult, 0, true);
        ReroutingParameters reroutingParameters = new ReroutingParameters(routeTask, routeParameters);
        mRouteTracker.enableReroutingAsync(reroutingParameters);

        // create a route tracker location data source to snap the location display to the route
        RouteTrackerLocationDataSource routeTrackerLocationDataSource = new RouteTrackerLocationDataSource(mRouteTracker, mSimulatedLocationDataSource);
        // set the route tracker location data source as the location data source for this app
        locationDisplay.setLocationDataSource(routeTrackerLocationDataSource);
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION);
        // if the user navigates the map view away from the location display, activate the recenter button
        //locationDisplay.addAutoPanModeChangedListener(autoPanModeChangedEvent -> mRecenterButton.setEnabled(true));

/*        // get a reference to navigation text views
        TextView distanceRemainingTextView = findViewById(R.id.distanceRemainingTextView);
        TextView timeRemainingTextView = findViewById(R.id.timeRemainingTextView);
        TextView nextDirectionTextView = findViewById(R.id.nextDirectionTextView);*/

        // start the LocationDisplay, which starts the RouteTrackerLocationDataSource and SimulatedLocationDataSource
        locationDisplay.startAsync();
        Toast.makeText(this, "Navigating to the first stop, the USS San Diego Memorial.", Toast.LENGTH_LONG).show();
    }


    private List<Stop> getStops() {
        List<Stop> stops = new ArrayList<>(3);
        // San Diego Convention Center
        Stop conventionCenter = new Stop(new com.esri.arcgisruntime.geometry.Point(finalLng, finalLat, SpatialReferences.getWgs84()));
        //Stop conventionCenter = new Stop(new com.esri.arcgisruntime.geometry.Point(114.1694, 22.3193, SpatialReferences.getWgs84()));
        stops.add(conventionCenter);
        // USS San Diego Memorial
        Stop memorial = new Stop(new com.esri.arcgisruntime.geometry.Point(initLng, initLat, SpatialReferences.getWgs84()));
        stops.add(memorial);
        return stops;
    }


    private void setupAddressSearchView() {

        mAddressGeocodeParameters = new GeocodeParameters();
        // get place name and address attributes
        mAddressGeocodeParameters.getResultAttributeNames().add("Hong Kong");
        mAddressGeocodeParameters.getResultAttributeNames().add("Place_addr");
        mAddressGeocodeParameters.getCountryCode().contains("Hong Kong");
        /*mAddressGeocodeParameters.getCategories().set()*/

        Log.i(TAG, "setupAddressSearchView: -------------------------->");

        // return only the closest result
        mAddressGeocodeParameters.setMaxResults(1);
        mAddressSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String address) {
                // geocode typed address
                geoCodeTypedAddress(address);
                // clear focus from search views
                mAddressSearchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // as long as newText isn't empty, get suggestions from the locatorTask
                if (!newText.equals("")) {
                    SuggestParameters suggestParameters = new SuggestParameters();
                    suggestParameters.setCountryCode("HK");
                    final ListenableFuture<List<SuggestResult>> suggestionsFuture = mLocatorTask.suggestAsync(newText,suggestParameters);
                    suggestionsFuture.addDoneListener(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                // get the results of the async operation
                                List<SuggestResult> suggestResults = suggestionsFuture.get();

                                MatrixCursor suggestionsCursor = new MatrixCursor(mColumnNames);
                                int key = 0;
                                // add each address suggestion to a new row
                                for (SuggestResult result : suggestResults) {
                                    Log.i(TAG, "run: "+result);
                                    suggestionsCursor.addRow(new Object[]{key++, result.getLabel()});
                                }
                                // define SimpleCursorAdapter
                                String[] cols = new String[]{COLUMN_NAME_ADDRESS};
                                int[] to = new int[]{R.id.suggestion_address};
                                final SimpleCursorAdapter suggestionAdapter = new SimpleCursorAdapter(MainActivity.this,
                                        R.layout.find_place_suggestion, suggestionsCursor, cols, to, 0);
                                mAddressSearchView.setSuggestionsAdapter(suggestionAdapter);
                                // handle an address suggestion being chosen
                                mAddressSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                                    @Override
                                    public boolean onSuggestionSelect(int position) {
                                        return false;
                                    }

                                    @Override
                                    public boolean onSuggestionClick(int position) {
                                        // get the selected row
                                        MatrixCursor selectedRow = (MatrixCursor) suggestionAdapter.getItem(position);
                                        // get the row's index
                                        int selectedCursorIndex = selectedRow.getColumnIndex(COLUMN_NAME_ADDRESS);
                                        // get the string from the row at index
                                        String address = selectedRow.getString(selectedCursorIndex);
                                        // use clicked suggestion as query
                                        mAddressSearchView.setQuery(address, true);
                                        return true;
                                    }
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Geocode suggestion error: " + e.getMessage());
                            }
                        }
                    });
                }
                return true;
            }
        });
    }

    private void displaySearchResult(GeocodeResult geocodeResult) {
        // dismiss any callout
        Log.i(TAG, "displaySearchResult: " + geocodeResult);
        Log.i(TAG, "displaySearchResult: Location->" + geocodeResult.getDisplayLocation());
        Log.i(TAG, "displaySearchResult: Lat->" + geocodeResult.getDisplayLocation().getX());
        Log.i(TAG, "displaySearchResult: Lat->" + geocodeResult.getDisplayLocation().getY());
        finalLat = geocodeResult.getDisplayLocation().getY();
        finalLng = geocodeResult.getDisplayLocation().getX();
    }

    private void geoCodeTypedAddress(final String address) {
        // check that address isn't null
        Log.i(TAG, "run: 1-------------------->"+address);
        if (address != null) {

            Log.i(TAG, "run: 2-------------------->"+address);
            // Execute async task to find the address
            mLocatorTask.addDoneLoadingListener(new Runnable() {
                @Override
                public void run() {
                    if (mLocatorTask.getLoadStatus() == LoadStatus.LOADED) {
                        Log.i(TAG, "run: -------------------->"+mAddressGeocodeParameters);
                        // Call geocodeAsync passing in an address
                        final ListenableFuture<List<GeocodeResult>> geocodeResultListenableFuture = mLocatorTask
                                .geocodeAsync(address, mAddressGeocodeParameters);
                        geocodeResultListenableFuture.addDoneListener(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // Get the results of the async operation
                                    List<GeocodeResult> geocodeResults = geocodeResultListenableFuture.get();
                                    if (geocodeResults.size() > 0) {
                                        displaySearchResult(geocodeResults.get(0));
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Location not found" + address,
                                                Toast.LENGTH_LONG).show();
                                    }
                                } catch (InterruptedException | ExecutionException e) {
                                    Log.e(TAG, "Geocode error: " + e.getMessage());
                                    Toast.makeText(getApplicationContext(), "Do not find location", Toast.LENGTH_LONG)
                                            .show();
                                }
                            }
                        });
                    } else {
                        Log.i(TAG, "Trying to reload locator task");
                        mLocatorTask.retryLoadAsync();
                    }
                }
            });
            mLocatorTask.loadAsync();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.dispose();
    }
}







/*public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    private Callout mCallout;
    private static final String TAG = "MainActivity";

    private ServiceFeatureTable mServiceFeatureTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // authentication with an API key or named user is required to access basemaps and other
        // location services
        ArcGISRuntimeEnvironment.setApiKey("AAPKaed993cafc4f4d858082c5c1256d338fcpBYWemA1WpWlYV-S8ZiXNtre_5h3c0zyrSc7BUHCgQunzpJW58lLiku77q1JSft");

        // inflate MapView from layout
        mMapView = findViewById(R.id.mapView);
        // create an ArcGISMap with a topographic basemap

      *//*  ArcGISVectorTiledLayer mVectorTiledLayer = new ArcGISVectorTiledLayer(getString(R.string.navigation_vector));
        // set tiled layer as basemap
        Basemap basemap = new Basemap(mVectorTiledLayer);
        // create a map with the basemap*//*

        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);
        //ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);
        // set the ArcGISMap to the MapView
        mMapView.setMap(map);
        // set a viewpoint

        mMapView.setViewpoint(new Viewpoint(22.3193, 114.1694, 200000.0));
        // get the callout that shows attributes
        mCallout = mMapView.getCallout();
        // create the service feature table
        mServiceFeatureTable = new ServiceFeatureTable(getResources().getString(R.string.hong_kong_service));
        // create the feature layer using the service feature table
        final FeatureLayer featureLayer = new FeatureLayer(mServiceFeatureTable);
        // add the layer to the map
        map.getOperationalLayers().add(featureLayer);

        // set an on touch listener to listen for click events
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // remove any existing callouts
                if (mCallout.isShowing()) {
                    mCallout.dismiss();
                }
                // get the point that was clicked and convert it to a point in map coordinates
                final Point screenPoint = new Point(Math.round(e.getX()), Math.round(e.getY()));
                // create a selection tolerance
                int tolerance = 10;
                // use identifyLayerAsync to get tapped features
                final ListenableFuture<IdentifyLayerResult> identifyLayerResultListenableFuture = mMapView
                        .identifyLayerAsync(featureLayer, screenPoint, tolerance, false, 1);
                identifyLayerResultListenableFuture.addDoneListener(() -> {
                    try {
                        IdentifyLayerResult identifyLayerResult = identifyLayerResultListenableFuture.get();
                        // create a textview to display field values
                        TextView calloutContent = new TextView(getApplicationContext());
                        calloutContent.setTextColor(Color.BLACK);
                        calloutContent.setSingleLine(false);
                        calloutContent.setVerticalScrollBarEnabled(true);
                        calloutContent.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                        calloutContent.setMovementMethod(new ScrollingMovementMethod());
                        calloutContent.setLines(5);
                        for (GeoElement element : identifyLayerResult.getElements()) {
                            Feature feature = (Feature) element;
                            // create a map of all available attributes as name value pairs
                            Map<String, Object> attr = feature.getAttributes();
                            Set<String> keys = attr.keySet();
                            for (String key : keys) {
                                Object value = attr.get(key);
                                // format observed field value as date
                                Log.i(TAG, "onSingleTapConfirmed: "+key+":"+value);
                                if (value instanceof GregorianCalendar) {
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
                                    value = simpleDateFormat.format(((GregorianCalendar) value).getTime());
                                }
                                // append name value pairs to text view
                                calloutContent.append(key + " | " + value + "\n");
                            }
                            // center the mapview on selected feature
                            Envelope envelope = feature.getGeometry().getExtent();
                            mMapView.setViewpointGeometryAsync(envelope, 200);
                            // show callout
                            mCallout.setLocation(envelope.getCenter());
                            mCallout.setContent(calloutContent);
                            mCallout.show();
                        }
                    } catch (Exception e1) {
                        Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e1.getMessage());
                    }
                });
                return super.onSingleTapConfirmed(e);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.dispose();
    }
}*/
