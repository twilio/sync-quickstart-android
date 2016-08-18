package com.twilio.sync.testapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.net.Uri;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import com.twilio.sync.Client;
import com.twilio.sync.List;
import com.twilio.sync.ListObserver;
import com.twilio.sync.Options;
import com.twilio.sync.Document;
import com.twilio.sync.DocumentObserver;
import com.twilio.sync.SuccessListener;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.gson.JsonObject;//for login, temp

public class TicTacActivity extends AppCompatActivity {
    Client syncClient;
    Document syncDoc;
    List syncLog;
    ImageAdapter board;

    String TAG = "TicTacActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tic_tac);

        GridView gridview = (GridView) findViewById(R.id.board);
        board = new ImageAdapter(this);
        gridview.setAdapter(board);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
            toggleCellValue(position);
            try {
                JSONObject newData = serialiseBoard();
                syncDoc.setData(newData, 0, new SuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void dummy) {
                    Log.d("Board", "Synced game state successfully");
                    }
                });
            } catch (JSONException ex) {
                Log.e("Board", "Cannot serialize board", ex);
                // so what
            }
            }
        });

        retrieveAccessTokenFromServer();
    }

    // Cycle X, O, empty in cell
    private void toggleCellValue(int position) {
        Integer val = (Integer)board.getItem(position);
        if (val == R.drawable.cross) {
            val = R.drawable.naught;
        } else if (val == R.drawable.naught) {
            val = R.drawable.empty;
        } else {
            val = R.drawable.cross;
        }
        board.setItem(position, val);
    }

    private void retrieveAccessTokenFromServer() {
        String endpoint_id =
            Secure.getString(this.getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
        String idChosen = "berkus";
        String endpointIdFull =
            idChosen + "-" + endpoint_id + "-android-" + getApplication().getPackageName();

        String url = Uri.parse(BuildConfig.SERVER_TOKEN_URL)
                         .buildUpon()
                         .appendQueryParameter("identity", idChosen)
                         .appendQueryParameter("endpoint_id", endpointIdFull)
                         .build()
                         .toString();
        Log.d(TAG, "url string : " + url);

        Log.d(TAG, "retrieveAccessTokenfromServer");
        Ion.with(this)
            .load(url)
            .asString()
            .setCallback(new FutureCallback<String>() {
                @Override
                public void onCompleted(Exception e, String accessToken) {
                    Log.d(TAG, "Ion.onCompleted");
                    if (e == null) {
                        createSyncClient(accessToken);

                        Log.d(TAG, "created sync client with token "+accessToken);
                    } else {
                        Log.e(TAG, "Error syncing", e);
                        Toast.makeText(TicTacActivity.this,
                                R.string.error_retrieving_access_token, Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
    }

    void createSyncClient(String token)
    {
        syncClient = Client.create(getApplicationContext(), token, Client.Properties.defaultProperties());

        syncClient.openDocument(new Options().withUniqueName("SyncGame"), new DocumentObserver() {
            @Override
            public void onRemoteUpdated(JSONObject data) {
                Log.d(TAG, "Remote game document update");
                try {
                    renderBoard(data);
                } catch (JSONException ex) {
                    Log.e("TicTacTwilio", "Exception in JSON", ex);
                }
            }
            @Override
            public void onResultUpdated(long flowId, JSONObject data) {
                Log.d(TAG, "Local game document update");
                try {
                    renderBoard(data);
                } catch (JSONException ex) {
                    Log.e("TicTacTwilio", "Exception in JSON", ex);
                }
            }
        }, new SuccessListener<Document>() {
            @Override
            public void onSuccess(Document doc) {
                Log.d(TAG, "Opened game document");
                syncDoc = doc;
                try {
                    renderBoard(syncDoc.getData());
                } catch (JSONException ex) {
                    Log.e("TicTacTwilio", "Exception in JSON", ex);
                }
            }
        });

        syncClient.openList(new Options().withUniqueName("SyncGameLog"), new ListObserver() {
            @Override
            public void onResultItemAdded(long flowId, long itemIndex) {
                Log.d("List", "Local item added");
            }

            @Override
            public void onRemoteItemAdded(long itemIndex, final JSONObject itemData) {
                Log.d("List", "Remote item "+itemIndex+" added "+itemData.toString());
            }
        }, new SuccessListener<List>() {
            @Override
            public void onSuccess(List result) {
                Log.d(TAG, "Opened game log");
                syncLog = result;
            }
        });
    }

    void renderBoard(JSONObject data) throws JSONException
    {
        Log.d("Board", "Received SyncGame "+data.toString());
        if (data.has("board")) {
            JSONArray obj = data.getJSONArray("board");
            for (int row = 0; row < 3; ++row) {
                JSONArray arr = obj.getJSONArray(row);
                for (int col = 0; col < 3; ++col) {
                    String item = (String)arr.get(col);
                    board.setItem(row * 3 + col,
                            item.contentEquals("X")?R.drawable.cross:
                            item.contentEquals("O")?R.drawable.naught:
                                                    R.drawable.empty);
                    Log.d("Board", "Item at "+row+", "+col+": `"+item+"`");
                }
            }
        }
        runOnUiThread(new Runnable() {
            public void run() {
                board.notifyDataSetChanged();
            }
        });
    }

    JSONObject serialiseBoard() throws JSONException
    {
        JSONArray obj = new JSONArray();
        for (int row = 0; row < 3; ++row) {
            JSONArray arr = new JSONArray();
            for (int col = 0; col < 3; ++col) {
                Integer val = (Integer)board.getItem(row * 3 + col);
                arr.put(col, val == R.drawable.cross?"X":
                             val == R.drawable.naught?"O":
                                     "");
            }
            obj.put(row, arr);
        }
        JSONObject out = new JSONObject();
        out.put("board", obj);
        Log.d("Board", "Saving board "+out.toString());
        return out;
    }
}
