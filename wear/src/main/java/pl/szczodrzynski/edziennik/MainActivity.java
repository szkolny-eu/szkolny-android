package pl.szczodrzynski.edziennik;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.wear.widget.drawer.WearableDrawerLayout;
import androidx.wear.widget.drawer.WearableDrawerView;
import androidx.wear.widget.drawer.WearableNavigationDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Arrays;
import java.util.Set;

public class MainActivity extends WearableActivity {

    private static final String TAG = "MainActivity";
    private ProgressBar progressBar;
    private WearableDrawerLayout wearableDrawerLayout;
    private WearableNavigationDrawerView mWearableNavigationDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Enables Always-on
        setAmbientEnabled();

        progressBar = findViewById(R.id.progressBar);

        wearableDrawerLayout = findViewById(R.id.drawer_layout);
        wearableDrawerLayout.setDrawerStateCallback(new WearableDrawerLayout.DrawerStateCallback() {
            @Override
            public void onDrawerOpened(WearableDrawerLayout layout, WearableDrawerView drawerView) {
                super.onDrawerOpened(layout, drawerView);
            }

            @Override
            public void onDrawerClosed(WearableDrawerLayout layout, WearableDrawerView drawerView) {
                super.onDrawerClosed(layout, drawerView);
                progressBar.setVisibility(View.GONE);
            }
        });

        mWearableNavigationDrawer = (WearableNavigationDrawerView) findViewById(R.id.top_navigation_drawer);
        WearableNavigationDrawerView.WearableNavigationDrawerAdapter navigationDrawerAdapter = new NavigationDrawerAdapter(this);
        mWearableNavigationDrawer.setAdapter(navigationDrawerAdapter);
        mWearableNavigationDrawer.addOnItemSelectedListener(new WearableNavigationDrawerView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int i) {
                //Toast.makeText(MainActivity.this, "Selected item "+i, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.VISIBLE);
            }
        });
        // Peeks navigation drawer on the top.
        mWearableNavigationDrawer.getController().peekDrawer();

        Wearable.getMessageClient(this).addListener(messageEvent -> {
            Log.d(TAG, messageEvent.getPath()+" :: "+ Arrays.toString(messageEvent.getData()));
        });

        Task<CapabilityInfo> capabilityInfoTask =
                Wearable.getCapabilityClient(this)
                        .getCapability("edziennik_phone_app", CapabilityClient.FILTER_REACHABLE);
        capabilityInfoTask.addOnCompleteListener((task) -> {
            if (task.isSuccessful()) {
                CapabilityInfo capabilityInfo = task.getResult();
                assert capabilityInfo != null;
                Set<Node> nodes;
                nodes = capabilityInfo.getNodes();
                Log.d(TAG, "Nodes "+nodes);
            } else {
                Log.d(TAG, "Capability request failed to return any results.");
            }
        });

        Wearable.getDataClient(this).addListener(dataEventBuffer -> {
            Log.d(TAG, "onDataChanged(): " + dataEventBuffer);

            for (DataEvent event : dataEventBuffer) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    String path = event.getDataItem().getUri().getPath();
                    Log.d(TAG, "Data "+path+ " :: "+Arrays.toString(event.getDataItem().getData()));
                }
            }
        });

        findViewById(R.id.test).setOnClickListener((v -> {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/ping");
            putDataMapRequest.getDataMap().putLong("millis", System.currentTimeMillis());

            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setData("Hello".getBytes());
            request.setUrgent();

            Log.d(TAG, "Generating DataItem: " + request);

            Task<DataItem> dataItemTask =
                    Wearable.getDataClient(getApplicationContext()).putDataItem(request);
            dataItemTask.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {

                    Log.d(TAG, "success");
                } else {
                    Log.d(TAG, "Capability request failed to return any results.");
                }
            });
        }));



        // Block on a task and get the result synchronously (because this is on a background
        // thread).
        //DataItem dataItem = dataItemTask.getResult();

        //Log.d(TAG, "DataItem saved: " + dataItem);

    }


    private class NavigationDrawerAdapter extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {
        public NavigationDrawerAdapter(Activity activity) {

        }

        @Override
        public CharSequence getItemText(int i) {
            return "Item "+i;
        }

        @Override
        public Drawable getItemDrawable(int i) {
            return null;
        }

        @Override
        public int getCount() {
            return 5;
        }
    }
}
