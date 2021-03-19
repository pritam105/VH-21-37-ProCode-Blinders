package com.spithackathon.Krishi.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.spithackathon.Krishi.R;
import com.spithackathon.Krishi.modals.CitySearch;
import com.spithackathon.Krishi.modals.Weather;
import com.spithackathon.Krishi.utilities.AppPreference;
import com.spithackathon.Krishi.utilities.Connection;
import com.spithackathon.Krishi.utilities.Constants;
import com.spithackathon.Krishi.utilities.CurrentWeatherService;
import com.spithackathon.Krishi.utilities.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static com.spithackathon.Krishi.utilities.AppPreference.saveLastUpdateTimeMillis;

public class WeatherActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener{
    private TextView mIconWeatherView;
    private TextView mTemperatureView;
    private TextView mDescriptionView;
    private TextView mHumidityView;
    private TextView mWindSpeedView;
    private TextView mPressureView;
    private TextView mCloudinessView;
    private TextView mLastUpdateView;
    private TextView mSunriseView;
    private TextView mSunsetView;
    private AppBarLayout mAppBarLayout;
    private TextView mIconWindView;
    private TextView mIconHumidityView;
    private TextView mIconPressureView;
    private TextView mIconCloudinessView;
    private TextView mIconSunriseView;
    private TextView mIconSunsetView;

    private Connection connectionDetector;
    private Boolean isNetworkAvailable;
    private ProgressDialog mProgressDialog;
    private LocationManager locationManager;
    private Menu mToolbarMenu;
    private BroadcastReceiver mWeatherUpdateReceiver;

    private SwipeRefreshLayout mSwipeRefresh;

    private String mSpeedScale;
    private String mIconWind;
    private String mIconHumidity;
    private String mIconPressure;
    private String mIconCloudiness;
    private String mIconSunrise;
    private String mIconSunset;
    private String mPercentSign;
    private String mPressureMeasurement;

    private SharedPreferences mPrefWeather;
    private SharedPreferences mSharedPreferences;

    public static Weather mWeather;
    public static CitySearch mCitySearch;

    private static final int REQUEST_LOCATION = 0;
    private static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};

    public Context storedContext;
    private static final long LOCATION_TIMEOUT_IN_MS = 30000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        mWeather = new Weather();
        mCitySearch = new CitySearch();

        weatherConditionsIcons();
        initializeTextView();
        initializeWeatherReceiver();
        connectionDetector = new Connection(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mPrefWeather = getSharedPreferences(Constants.PREF_WEATHER_NAME, Context.MODE_PRIVATE);
        mSharedPreferences = getSharedPreferences(Constants.APP_SETTINGS_NAME,
                Context.MODE_PRIVATE);
        setTitle(Utils.getCityAndCountry(this));

        mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.main_swipe_refresh);
        int top_to_padding = 150;
        mSwipeRefresh.setProgressViewOffset(false, 0, top_to_padding);
        mSwipeRefresh.setColorSchemeResources(R.color.swipe_red, R.color.swipe_green,
                R.color.swipe_blue);
        mSwipeRefresh.setOnRefreshListener(swipeRefreshListener);

        requestLocation();
    }

    private SwipeRefreshLayout.OnRefreshListener swipeRefreshListener =
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    isNetworkAvailable = connectionDetector.isInternet();
                    if (isNetworkAvailable) {
                        startService(new Intent(WeatherActivity.this, CurrentWeatherService.class));
                    } else {
                        Toast.makeText(WeatherActivity.this,
                                R.string.connection_not_found,
                                Toast.LENGTH_SHORT).show();
                        mSwipeRefresh.setRefreshing(false);
                    }
                }
            };

    private void requestLocation() {
        int fineLocationPermission = ContextCompat.checkSelfPermission(WeatherActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else {
            detectLocation();
        }
    }

    private void detectLocation() {
        boolean isGPSEnabled = locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        mProgressDialog = new ProgressDialog(WeatherActivity.this);
        mProgressDialog.setMessage(getString(R.string.progressDialog_gps_locate));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    locationManager.removeUpdates(mLocationListener);
                } catch (SecurityException e) {
                    Log.e("TAG", "Cancellation error", e);
                }
            }
        });

        if (isNetworkEnabled) {
            networkRequestLocation();
            mProgressDialog.show();
        } else {
            if (isGPSEnabled) {
                gpsRequestLocation();
                mProgressDialog.show();
            } else {
                showSettingsAlert();
            }
        }
    }

    public void networkRequestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Looper locationLooper = Looper.myLooper();
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, mLocationListener, locationLooper);
            final Handler locationHandler = new Handler(locationLooper);
            locationHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    locationManager.removeUpdates(mLocationListener);
                    if (ContextCompat.checkSelfPermission(WeatherActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Location lastNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        Location lastGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                        if ((lastGpsLocation == null) && (lastNetworkLocation != null)) {
                            mLocationListener.onLocationChanged(lastNetworkLocation);
                        } else if ((lastGpsLocation != null) && (lastNetworkLocation == null)) {
                            mLocationListener.onLocationChanged(lastGpsLocation);
                        } else {
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
                        }
                    }
                }
            }, LOCATION_TIMEOUT_IN_MS);
        }
    }

    public void gpsRequestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Looper locationLooper = Looper.myLooper();
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, locationLooper);
            final Handler locationHandler = new Handler(locationLooper);
            locationHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    locationManager.removeUpdates(mLocationListener);
                    if (ContextCompat.checkSelfPermission(WeatherActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastLocation != null) {
                            mLocationListener.onLocationChanged(lastLocation);
                        } else {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
                        }
                    }
                }
            }, LOCATION_TIMEOUT_IN_MS);
        }
    }

    public void showSettingsAlert() {
        AlertDialog.Builder settingsAlert = new AlertDialog.Builder(WeatherActivity.this);
        settingsAlert.setTitle(R.string.alertDialog_gps_title);
        settingsAlert.setMessage(R.string.alertDialog_gps_message);

        settingsAlert.setPositiveButton(R.string.alertDialog_gps_positiveButton,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent goToSettings = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(goToSettings);
                    }
                });

        settingsAlert.setNegativeButton(R.string.alertDialog_gps_negativeButton,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        settingsAlert.show();
    }

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mProgressDialog.cancel();
            String latitude = String.format("%1$.2f", location.getLatitude());
            String longitude = String.format("%1$.2f", location.getLongitude());

            if (ContextCompat.checkSelfPermission(WeatherActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.removeUpdates(mLocationListener);
            }

            connectionDetector = new Connection(WeatherActivity.this);
            isNetworkAvailable = connectionDetector.isInternet();

            mSharedPreferences = getSharedPreferences(Constants.APP_SETTINGS_NAME,
                    Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(Constants.APP_SETTINGS_LATITUDE, latitude);
            editor.putString(Constants.APP_SETTINGS_LONGITUDE, longitude);
            getAndWriteAddressFromGeocoder(latitude, longitude, editor);
            editor.apply();

            if (isNetworkAvailable) {
                startService(new Intent(WeatherActivity.this, CurrentWeatherService.class));
                sendBroadcast(new Intent(Constants.ACTION_FORCED_APPWIDGET_UPDATE));
            } else {
                Toast.makeText(WeatherActivity.this, R.string.connection_not_found, Toast.LENGTH_SHORT)
                        .show();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };


    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(findViewById(android.R.id.content), R.string.permission_location_rationale, Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(WeatherActivity.this, PERMISSIONS_LOCATION, REQUEST_LOCATION);
                        }
                    }).show();
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS_LOCATION, REQUEST_LOCATION);
        }
    }

        private void getAndWriteAddressFromGeocoder(String latitude, String longitude, SharedPreferences.Editor editor) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                String latitudeEn = latitude.replace(",", ".");
                String longitudeEn = longitude.replace(",", ".");
                List<Address> addresses = geocoder.getFromLocation(Double.parseDouble(latitudeEn), Double.parseDouble(longitudeEn), 1);
                if ((addresses != null) && (addresses.size() > 0)) {
                    editor.putString(Constants.APP_SETTINGS_GEO_CITY, addresses.get(0).getLocality());
                    editor.putString(Constants.APP_SETTINGS_GEO_COUNTRY_NAME, addresses.get(0).getCountryName());
                }
            } catch (IOException | NumberFormatException ex) {
                Log.e("TAG", "Unable to get address from latitude and longitude", ex);
            }
        }


    private void weatherConditionsIcons() {
        mIconWind = getString(R.string.icon_wind);
        mIconHumidity = getString(R.string.icon_humidity);
        mIconPressure = getString(R.string.icon_barometer);
        mIconCloudiness = getString(R.string.icon_cloudiness);
        mPercentSign = getString(R.string.percent_sign);
        mPressureMeasurement = getString(R.string.pressure_measurement);
        mIconSunrise = getString(R.string.icon_sunrise);
        mIconSunset = getString(R.string.icon_sunset);
    }

    private void initializeTextView() {
        /**
         * Create typefaces from Asset
         */
        Typeface weatherFontIcon = Typeface.createFromAsset(this.getAssets(),
                "fonts/weathericons-regular-webfont.ttf");
        Typeface robotoThin = Typeface.createFromAsset(this.getAssets(),
                "fonts/Roboto-Thin.ttf");
        Typeface robotoLight = Typeface.createFromAsset(this.getAssets(),
                "fonts/Roboto-Light.ttf");

        mIconWeatherView = (TextView) findViewById(R.id.main_weather_icon);
        mTemperatureView = (TextView) findViewById(R.id.main_temperature);
        mDescriptionView = (TextView) findViewById(R.id.main_description);
        mPressureView = (TextView) findViewById(R.id.main_pressure);
        mHumidityView = (TextView) findViewById(R.id.main_humidity);
        mWindSpeedView = (TextView) findViewById(R.id.main_wind_speed);
        mCloudinessView = (TextView) findViewById(R.id.main_cloudiness);
        mLastUpdateView = (TextView) findViewById(R.id.main_last_update);
        mSunriseView = (TextView) findViewById(R.id.main_sunrise);
        mSunsetView = (TextView) findViewById(R.id.main_sunset);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.main_app_bar);

        mIconWeatherView.setTypeface(weatherFontIcon);
        mTemperatureView.setTypeface(robotoThin);
        mWindSpeedView.setTypeface(robotoLight);
        mHumidityView.setTypeface(robotoLight);
        mPressureView.setTypeface(robotoLight);
        mCloudinessView.setTypeface(robotoLight);
        mSunriseView.setTypeface(robotoLight);
        mSunsetView.setTypeface(robotoLight);

        /**
         * Initialize and configure weather icons
         */
        mIconWindView = (TextView) findViewById(R.id.main_wind_icon);
        mIconWindView.setTypeface(weatherFontIcon);
        mIconWindView.setText(mIconWind);
        mIconHumidityView = (TextView) findViewById(R.id.main_humidity_icon);
        mIconHumidityView.setTypeface(weatherFontIcon);
        mIconHumidityView.setText(mIconHumidity);
        mIconPressureView = (TextView) findViewById(R.id.main_pressure_icon);
        mIconPressureView.setTypeface(weatherFontIcon);
        mIconPressureView.setText(mIconPressure);
        mIconCloudinessView = (TextView) findViewById(R.id.main_cloudiness_icon);
        mIconCloudinessView.setTypeface(weatherFontIcon);
        mIconCloudinessView.setText(mIconCloudiness);
        mIconSunriseView = (TextView) findViewById(R.id.main_sunrise_icon);
        mIconSunriseView.setTypeface(weatherFontIcon);
        mIconSunriseView.setText(mIconSunrise);
        mIconSunsetView = (TextView) findViewById(R.id.main_sunset_icon);
        mIconSunsetView.setTypeface(weatherFontIcon);
        mIconSunsetView.setText(mIconSunset);
    }

    private void initializeWeatherReceiver() {
        mWeatherUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getStringExtra(CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT)) {
                    case CurrentWeatherService.ACTION_WEATHER_UPDATE_OK:
                        //mSwipeRefresh.setRefreshing(false);
                        //setUpdateButtonState(false);
                        updateCurrentWeather();
                        break;
                    case CurrentWeatherService.ACTION_WEATHER_UPDATE_FAIL:
                        //mSwipeRefresh.setRefreshing(false);
                        //setUpdateButtonState(false);
                        Toast.makeText(WeatherActivity.this,
                                getString(R.string.toast_parse_error),
                                Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void updateCurrentWeather() {
        AppPreference.saveWeather(WeatherActivity.this, mWeather);
        mSharedPreferences = getSharedPreferences(Constants.APP_SETTINGS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor configEditor = mSharedPreferences.edit();

        mSpeedScale = Utils.getSpeedScale(WeatherActivity.this);
        String temperature = String.format(Locale.getDefault(), "%.0f",
                mWeather.temperature.getTemp());
        String pressure = String.format(Locale.getDefault(), "%.1f",
                mWeather.currentCondition.getPressure());
        String wind = String.format(Locale.getDefault(), "%.1f", mWeather.wind.getSpeed());

        String lastUpdate = Utils.setLastUpdateTime(WeatherActivity.this,
                saveLastUpdateTimeMillis(WeatherActivity.this));
        String sunrise = Utils.unixTimeToFormatTime(WeatherActivity.this, mWeather.sys.getSunrise());
        String sunset = Utils.unixTimeToFormatTime(WeatherActivity.this, mWeather.sys.getSunset());

        mIconWeatherView.setText(
                Utils.getStrIcon(WeatherActivity.this, mWeather.currentWeather.getIdIcon()));
        mTemperatureView.setText(getString(R.string.temperature_with_degree, temperature));
        if (!AppPreference.hideDescription(WeatherActivity.this))
            mDescriptionView.setText(mWeather.currentWeather.getDescription());
        else
            mDescriptionView.setText(" ");
        mHumidityView.setText(getString(R.string.humidity_label,
                String.valueOf(mWeather.currentCondition.getHumidity()),
                mPercentSign));
        mPressureView.setText(getString(R.string.pressure_label, pressure,
                mPressureMeasurement));
        mWindSpeedView.setText(getString(R.string.wind_label, wind, mSpeedScale));
        mCloudinessView.setText(getString(R.string.cloudiness_label,
                String.valueOf(mWeather.cloud.getClouds()),
                mPercentSign));
        mLastUpdateView.setText(getString(R.string.last_update_label, lastUpdate));
        mSunriseView.setText(getString(R.string.sunrise_label, sunrise));
        mSunsetView.setText(getString(R.string.sunset_label, sunset));

        configEditor.putString(Constants.APP_SETTINGS_CITY, mWeather.location.getCityName());
        configEditor.putString(Constants.APP_SETTINGS_COUNTRY_CODE,
                mWeather.location.getCountryCode());
        configEditor.apply();
    }

    private void preLoadWeather() {
        mSpeedScale = Utils.getSpeedScale(this);
        String lastUpdate = Utils.setLastUpdateTime(this,
                AppPreference.getLastUpdateTimeMillis(this));

        String iconId = mPrefWeather.getString(Constants.WEATHER_DATA_ICON, "01d");
        float temperaturePref = mPrefWeather.getFloat(Constants.WEATHER_DATA_TEMPERATURE, 0);
        String description = mPrefWeather.getString(Constants.WEATHER_DATA_DESCRIPTION,
                "clear sky");
        int humidity = mPrefWeather.getInt(Constants.WEATHER_DATA_HUMIDITY, 0);
        float pressurePref = mPrefWeather.getFloat(Constants.WEATHER_DATA_PRESSURE, 0);
        float windPref = mPrefWeather.getFloat(Constants.WEATHER_DATA_WIND_SPEED, 0);
        int clouds = mPrefWeather.getInt(Constants.WEATHER_DATA_CLOUDS, 0);
        long sunrisePref = mPrefWeather.getLong(Constants.WEATHER_DATA_SUNRISE, -1);
        long sunsetPref = mPrefWeather.getLong(Constants.WEATHER_DATA_SUNSET, -1);

        String temperature = String.format(Locale.getDefault(), "%.0f", temperaturePref);
        String pressure = String.format(Locale.getDefault(), "%.1f", pressurePref);
        String wind = String.format(Locale.getDefault(), "%.1f", windPref);
        String sunrise = Utils.unixTimeToFormatTime(this, sunrisePref);
        String sunset = Utils.unixTimeToFormatTime(this, sunsetPref);

        mIconWeatherView.setText(Utils.getStrIcon(this, iconId));
        mTemperatureView.setText(getString(R.string.temperature_with_degree, temperature));
        mDescriptionView.setText(description);
        mLastUpdateView.setText(getString(R.string.last_update_label, lastUpdate));
        mHumidityView.setText(getString(R.string.humidity_label,
                String.valueOf(humidity),
                mPercentSign));
        mPressureView.setText(getString(R.string.pressure_label,
                pressure,
                mPressureMeasurement));
        mWindSpeedView.setText(getString(R.string.wind_label, wind, mSpeedScale));
        mCloudinessView.setText(getString(R.string.cloudiness_label,
                String.valueOf(clouds),
                mPercentSign));
        mSunriseView.setText(getString(R.string.sunrise_label, sunrise));
        mSunsetView.setText(getString(R.string.sunset_label, sunset));
        setTitle(Utils.getCityAndCountry(this));
    }

    @Override
    public void onResume() {
        super.onResume();
        preLoadWeather();
        mAppBarLayout.addOnOffsetChangedListener(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(mWeatherUpdateReceiver,
                new IntentFilter(
                        CurrentWeatherService.ACTION_WEATHER_UPDATE_RESULT));
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        mSwipeRefresh.setEnabled(i == 0);
    }
}