package com.twilio.sync.quickstart;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.net.Uri;
import android.provider.Settings.Secure;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import com.twilio.sync.SyncClient;
import com.twilio.sync.ErrorInfo;
import com.twilio.sync.List;
import com.twilio.sync.ListObserver;
import com.twilio.sync.ListPaginator;
import com.twilio.sync.Map;
import com.twilio.sync.MapObserver;
import com.twilio.sync.MapPaginator;
import com.twilio.sync.Options;
import com.twilio.sync.Document;
import com.twilio.sync.DocumentObserver;
import com.twilio.sync.SuccessListener;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import com.google.gson.JsonObject;//for login, temp

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.text.DecimalFormat;

import timber.log.Timber;

public class TicTacActivity extends AppCompatActivity {
    private SyncClient syncClient;
    private Document syncDoc;
    private List syncLog;
    private Map syncState;
    private GridView boardView;
    private ImageAdapter board;
    private TextView logView;
    private TextView statusView;
    private TextView winText;
    private String identity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.plant(new Timber.DebugTree());

        setContentView(R.layout.activity_tic_tac);

        logView = (TextView) findViewById(R.id.logView);
        statusView = (TextView) findViewById(R.id.statusView);
        winText = (TextView) findViewById(R.id.winText);

        identity = generateRandomIdentity();
        logView.append("I am player "+identity+"\n");

        boardView = (GridView) findViewById(R.id.board);
        board = new ImageAdapter(this);
        boardView.setAdapter(board);

        boardView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                toggleCellValue(position);
                syncBoard();
            }
        });

        Button newGame = (Button) findViewById(R.id.newGame);
        newGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGame();
            }
        });

        Button shutdown = (Button) findViewById(R.id.shutdownBtn);
        shutdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shutdownClient();
            }
        });

        retrieveAccessTokenFromServer();
    }

    private String generateRandomIdentity() {
        final String[] names = {
                "Andres", "Aleks",
                "Boris",
                "Colin", "Carl",
                "Danila",
                "John", "Joe", "Jill",
                "Kate", "Kevin", "Kronar",
                "Lembit",
                "Mihkel",
                "Silver"
        };
        return names[new Random(System.nanoTime()).nextInt(names.length)];
    }

    void setTurn(final String turn) {
        JSONObject obj1 = new JSONObject();
        try {
            obj1.put("value", turn);
        }catch (JSONException xcp) {
            Timber.e(xcp, "Failed to set json value");
        }
        syncState.setItem("turn", obj1, 0, new SuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Timber.d("Set turn to "+turn);
            }
        });
    }

    void syncBoard() {
        try {
            Long flowId = System.currentTimeMillis();
            Timber.e("Set data flow id:" + new DecimalFormat("#").format(flowId));

            JSONObject newData = serialiseBoard();
            syncDoc.setData(newData, flowId, new SuccessListener<Void>() {
                @Override
                public void onSuccess(Void dummy) {
                    Timber.d("Board: Synced game state successfully");
                }
            });
        } catch (JSONException xcp) {
            Timber.e(xcp, "Board: Cannot serialize board");
        }
    }

    int checkWinner()
    {
        // horizontal
        if (board.getItem(0).equals(board.getItem(1)) && board.getItem(1).equals(board.getItem(2)))
            return (Integer)board.getItem(0);
        if (board.getItem(3).equals(board.getItem(4)) && board.getItem(4).equals(board.getItem(5)))
            return (Integer)board.getItem(3);
        if (board.getItem(6).equals(board.getItem(7)) && board.getItem(7).equals(board.getItem(8)))
            return (Integer)board.getItem(6);
        // vertical
        if (board.getItem(0).equals(board.getItem(3)) && board.getItem(3).equals(board.getItem(6)))
            return (Integer)board.getItem(0);
        if (board.getItem(1).equals(board.getItem(4)) && board.getItem(4).equals(board.getItem(7)))
            return (Integer)board.getItem(1);
        if (board.getItem(2).equals(board.getItem(5)) && board.getItem(5).equals(board.getItem(8)))
            return (Integer)board.getItem(2);
        // diagonal
        if (board.getItem(0).equals(board.getItem(4)) && board.getItem(4).equals(board.getItem(8)))
            return (Integer)board.getItem(0);
        if (board.getItem(2).equals(board.getItem(4)) && board.getItem(4).equals(board.getItem(6)))
            return (Integer)board.getItem(2);
        return R.drawable.empty;
    }

    void shutdownClient()
    {
        syncClient.shutdown();
        Timber.e("CLIENT SHUTDOWN COMPLETED");
    }

    void endGameOnWin()
    {
        final int winner = checkWinner();
        Timber.d("Winner check: " + winner);
        if (winner == R.drawable.empty) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.setEnabled(false);
                winText.setVisibility(View.VISIBLE);
                winText.setText(winner == R.drawable.cross ? "X has won" : "O has won");
            }
        });
    }

    // Set cell value according to current move order
    private void toggleCellValue(final int position) {
        if ((Integer)board.getItem(position) != R.drawable.empty) {
            return;
        }

        syncState.getItem("turn", 0, new SuccessListener<Map.Item>() {
            @Override
            public void onSuccess(Map.Item result) {
                Timber.d("Received map item "+result.getData().toString());
                String state = result.getData().optString("value", "E");

                if (state.contentEquals("E")) {
                    // start a new game
                    setTurn("X");
                    state = "X";
                    JSONObject obj2 = new JSONObject();
                    try {
                        obj2.put("value", identity);
                    }catch (JSONException xcp) {
                        Timber.e(xcp, "Failed to set json value");
                    }
                    syncState.setItem("playerX", obj2, 0, new SuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Timber.d("Set playerX id");
                        }
                    });
                } else if (state.contentEquals("X")) {
                    setTurn("O");
                    state = "O";
                    JSONObject obj2 = new JSONObject();
                    try {
                        obj2.put("value", identity);
                    }catch (JSONException xcp) {
                        Timber.e(xcp, "Failed to set json value");
                    }
                    syncState.setItem("playerO", obj2, 0, new SuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Timber.d("Set playerO id");
                        }
                    });
                } else if (state.contentEquals("O")) {
                    setTurn("X");
                    state = "X";
                }

                // Now make a turn
                final Integer newVal = state.contentEquals("X") ? R.drawable.cross : R.drawable.naught;
                board.setItem(position, newVal);

                try {
                    JSONObject item = new JSONObject();
                    item.put("turn", intToSymbol(newVal));
                    JSONArray loc = new JSONArray();
                    loc.put(position / 3);
                    loc.put(position % 3);
                    item.put("location", loc);
                    item.put("when", new Date().getTime() / 1000);

                    syncLog.addItem(item, 0, new SuccessListener<Long>() {
                        @Override
                        public void onSuccess(Long result) {
                            Timber.d("SyncLog item added");
                        }
                    });
                } catch (JSONException xcp) {
                    Timber.e(xcp, "Cannot serialize log entry");
                }
            }
        });
    }

    private void retrieveAccessTokenFromServer() {
        String endpoint_id =
            Secure.getString(this.getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
        String endpointIdFull =
            identity + "-" + endpoint_id + "-android-" + getApplication().getPackageName();

        String url = Uri.parse(BuildConfig.SERVER_TOKEN_URL)
                         .buildUpon()
                         .appendQueryParameter("identity", identity)
                         .build()
                         .toString();
        Timber.d("url string : " + url);

        Timber.d("retrieveAccessTokenfromServer");
        Ion.with(this)
            .load(url)
            .asJsonObject()
            .setCallback(new FutureCallback<JsonObject>() {
                @Override
                public void onCompleted(Exception e, JsonObject tokenServiceResponse) {
                    final String accessToken = tokenServiceResponse.get("token").getAsString();
                    Timber.d("Retrieved token: " + accessToken);
                    if (e == null) {
                        createSyncClient(accessToken);

                        Timber.d("created sync client as " + tokenServiceResponse.get("identity").getAsString());
                    } else {
                        Timber.e("Error syncing: " + e);
                        Toast.makeText(TicTacActivity.this,
                                R.string.error_retrieving_access_token, Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
    }

    void createSyncClient(String token)
    {
        syncClient = SyncClient.create(getApplicationContext(), token, SyncClient.Properties.defaultProperties());

        syncClient.openDocument(new Options().withUniqueName("SyncGame"), new DocumentObserver() {
            @Override
            public void onRemoteUpdated(JSONObject data) {
                Timber.d("Remote game document update");
                try {
                    renderBoard(data);
                    endGameOnWin();
                } catch (JSONException xcp) {
                    Timber.e(xcp, "Exception in JSON");
                }
            }
            @Override
            public void onResultUpdated(long flowId, JSONObject data) {
                Timber.d("Local game document update");
                Timber.e("On result updated with flow id:" + new DecimalFormat("#").format(flowId));
                try {
                    renderBoard(data);
                    endGameOnWin();
                } catch (JSONException xcp) {
                    Timber.e(xcp, "Exception in JSON");
                }
            }
        }, new SuccessListener<Document>() {
            @Override
            public void onSuccess(Document doc) {
                Timber.d("Opened game document");
                syncDoc = doc;
                try {
                    renderBoard(syncDoc.getData());
                } catch (JSONException xcp) {
                    Timber.e(xcp, "Exception in JSON");
                }
            }
        });

        syncClient.openList(new Options().withUniqueName("SyncGameLog"), new ListObserver() {
            @Override
            public void onResultItemAdded(long flowId, long itemIndex) {
                Timber.d("List: Local item added");
            }

            @Override
            public void onRemoteItemAdded(long itemIndex, final JSONObject itemData) {
                Timber.d("List: Remote item "+itemIndex+" added "+itemData.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logView.append(itemData.toString()+"\n");
                    }
                });
            }
        }, new SuccessListener<List>() {
            @Override
            public void onSuccess(List result) {
                Timber.d("Opened game log");
                syncLog = result;

                syncLog.queryItems(syncLog.queryOptions(), new SuccessListener<ListPaginator>() {
                    @Override
                    public void onSuccess(ListPaginator paginator) {
                        final long size = paginator.getPageSize();
                        final ArrayList<List.Item> items = paginator.getItems();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Timber.d("Received page with "+size+" items");
                                logView.append("Received page with "+size+" items\n");
                                for (List.Item item : items) {
                                    Timber.d(item.getData().toString());
                                    logView.append(item.getData().toString()+"\n");
                                }
                            }
                        });
                    }
                });
            }
        });

        syncClient.openMap(new Options().withUniqueName("SyncGameState"), new MapObserver() {
            @Override
            public void onResultItemSet(long flowId, String itemKey) {
                Timber.d("Map: Local updated item");
            }

            @Override
            public void onResultItemRemoved(long flowId, String itemKey) {
                Timber.d("Map: Local removed item");
            }

            @Override
            public void onResultErrorOccurred(long flowId, ErrorInfo errorCode) {
                Timber.d("Map: Local error occurred");
            }

            @Override
            public void onRemoteItemSet(final String itemKey, final JSONObject itemData) {
                Timber.d("Map: Remote updated item");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusView.append(itemKey + " changed: " + itemData.toString()+"\n");
                    }
                });
            }

            @Override
            public void onRemoteItemRemoved(String itemKey) {
                Timber.d("Map: Remote removed item "+itemKey);
            }

            @Override
            public void onRemoteErrorOccurred(ErrorInfo errorCode) {
                Timber.d("Map: Remote error occurred");
            }
        }, new SuccessListener<Map>() {
            @Override
            public void onSuccess(Map result) {
                Timber.d("Opened game state");
                syncState = result;
                setTurn("E"); // force game start

                syncState.queryItems(syncState.queryOptions(), new SuccessListener<MapPaginator>() {
                    @Override
                    public void onSuccess(MapPaginator paginator) {
                        final long size = paginator.getPageSize();
                        final ArrayList<Map.Item> items = paginator.getItems();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Timber.d("Received page with "+size+" items");
                                logView.append("Received page with "+size+" items\n");
                                for (Map.Item item : items) {
                                    Timber.d(item.getKey() + " => " + item.getData().toString());
                                    statusView.append(item.getKey() + " => " + item.getData().toString()+"\n");
                                }
                            }
                        });
                    }
                });

            }
        });
    }

    void newGame()
    {
        winText.setVisibility(View.GONE);
        for (int position = 0; position < 9; ++position) {
            board.setItem(position, R.drawable.empty);
        }
        syncBoard();
        setTurn("E");
        boardView.setEnabled(true);
    }

    void renderBoard(JSONObject data) throws JSONException
    {
        Timber.d("Board: Received SyncGame "+data.toString());
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
                    Timber.d("Board: Item at "+row+", "+col+": `"+item+"`");
                }
            }
        }
        runOnUiThread(new Runnable() {
            public void run() {
                board.notifyDataSetChanged();
            }
        });
    }

    private String intToSymbol(Integer val) {
        return val == R.drawable.cross?"X":
                val == R.drawable.naught?"O":
                        "";
    }

    JSONObject serialiseBoard() throws JSONException
    {
        JSONArray obj = new JSONArray();
        for (int row = 0; row < 3; ++row) {
            JSONArray arr = new JSONArray();
            for (int col = 0; col < 3; ++col) {
                arr.put(col, intToSymbol((Integer)board.getItem(row * 3 + col)));
            }
            obj.put(row, arr);
        }
        JSONObject out = new JSONObject();
        out.put("board", obj);
        Timber.d("Board: Saving board "+out.toString());
        return out;
    }
}
