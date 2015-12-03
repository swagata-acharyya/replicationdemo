package com.simpragma.cssample;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.datastore.DatastoreNotCreatedException;
import com.cloudant.sync.replication.PullFilter;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorBuilder;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {

    String REPLICATION_FILTER = "Product/airline";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new AsyncClass().execute();
    }

    private class AsyncClass extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.d("REPLTEST","Starting replication");
                startReplication();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (DatastoreNotCreatedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void startReplication() throws URISyntaxException, DatastoreNotCreatedException, InterruptedException {
        URI uri = new URI("http://android-app:k2MVvimRRXC4@OC.sync.api.staging.miairline.com/mi-master");

        File path = getApplicationContext().getDir("datastores", Context.MODE_PRIVATE);
        DatastoreManager manager = new DatastoreManager(path.getAbsolutePath());

        Datastore ds = manager.openDatastore("my_datastore");

        final Map<String, String> filterParameters = ImmutableMap.of("airline_iata_code", "oc", "client",
                "APPIDENTIFIER", "tabletId", "TABID");
        PullFilter filter = new PullFilter(REPLICATION_FILTER,filterParameters);
        Log.d("REPLTEST", "Setting up replication");
// Create a replicator that replicates changes from the remote
// database to the local datastore.
        Replicator replicator = ReplicatorBuilder.pull().from(uri).to(ds).filter(filter).build();
//        Replicator replicator = ReplicatorBuilder.pull().from(uri).to(ds).build();


// Use a CountDownLatch to provide a lightweight way to wait for completion
        CountDownLatch latch = new CountDownLatch(1);
        Listener listener = new Listener(latch);
        replicator.getEventBus().register(listener);
        Log.d("REPLTEST", "Starting replication");
        replicator.start();
        latch.await();
        replicator.getEventBus().unregister(listener);
        if (replicator.getState() != Replicator.State.COMPLETE) {
            Log.d("REPLTEST", "Error in replication");
            System.out.println("REPLICATION Error replicating FROM remote");
            System.out.println(listener.error);
        }else if (replicator.getState() == Replicator.State.COMPLETE){
            Log.d("REPLTEST", "Successful replication");
        }else{
            Log.d("REPLTEST", "Replication unknown status");
        }
    }
}
