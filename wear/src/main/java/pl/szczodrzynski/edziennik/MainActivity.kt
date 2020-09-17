/*
 * Copyright (c) Kacper Ziubryniewicz 2020-9-17
 */

package pl.szczodrzynski.edziennik

import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import com.google.android.gms.wearable.*

class MainActivity : WearableActivity(), DataClient.OnDataChangedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enables Always-on
        setAmbientEnabled()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                event.dataItem.also { item ->
                    if (item?.uri?.path?.compareTo("/test") == 0) {
                        DataMapItem.fromDataItem(item).dataMap.apply {
                            getInt("test")
                        }
                    }
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }

        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }
}
