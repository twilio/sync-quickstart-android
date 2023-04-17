package com.twilio.sync.quickstart;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.twilio.sync.EventContext;
import com.twilio.sync.SuccessListener;
import com.twilio.sync.SyncClient;
import com.twilio.sync.SyncDocument;
import com.twilio.sync.SyncDocumentObserver;
import com.twilio.sync.SyncList;
import com.twilio.sync.SyncListObserver;
import com.twilio.sync.SyncListPaginator;
import com.twilio.sync.SyncMap;
import com.twilio.sync.SyncMapObserver;
import com.twilio.sync.SyncMutator;
import com.twilio.sync.SyncOptions;
import com.twilio.sync.quickstart.utils.SyncClientUtils;
import com.twilio.util.ErrorInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import timber.log.Timber;

import static com.twilio.sync.quickstart.utils.SyncClientUtils.Where.SYNC_CLIENT_CPP;
import static com.twilio.sync.quickstart.utils.SyncClientUtils.Where.TS_CLIENT_CPP;

public class TicTacActivity extends AppCompatActivity {
    private SyncClient syncClient;
    private SyncDocument boardState;
    private SyncList playerMoveLog;
    private SyncMap gameStateMap;
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
        Timber.d("onCreate");

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

        if (BuildConfig.DEBUG) {
            SyncClient.setLogLevel(Log.DEBUG);
        }

        authenticateAndStartSync();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tictak, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_crash_in_java:
                throw new RuntimeException("Simulated crash");

            case R.id.action_crash_in_sync_client:
                SyncClientUtils.simulateCrash(syncClient, SYNC_CLIENT_CPP);
                return true;

            case R.id.action_crash_in_ts_client:
                SyncClientUtils.simulateCrash(syncClient, TS_CLIENT_CPP);
                return true;
        }
        return super.onOptionsItemSelected(item);
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
        gameStateMap.setItem("turn", UnaryJson(turn), new SuccessListener<SyncMap.Item>() {
            @Override
            public void onSuccess(SyncMap.Item result) {
                statusView.setText("Next up: Player " + turn +"\n");
            }
        });
    }

    void syncBoard() {
        try {
            JSONObject newData = serialiseBoard();
            boardState.setData(newData, new SuccessListener<JSONObject>() {
                @Override
                public void onSuccess(JSONObject dummy) {
                    Timber.d("Board: Synced game state successfully");
                }
            });
        } catch (JSONException xcp) {
            Timber.e(xcp, "Board: Cannot serialize board");
        }
    }

    int checkWinner() {
        // horizontal
        if (!board.getItem(0).equals(R.drawable.empty) && board.getItem(0).equals(board.getItem(1)) && board.getItem(1).equals(board.getItem(2)))
            return (Integer)board.getItem(0);
        if (!board.getItem(3).equals(R.drawable.empty) && board.getItem(3).equals(board.getItem(4)) && board.getItem(4).equals(board.getItem(5)))
            return (Integer)board.getItem(3);
        if (!board.getItem(6).equals(R.drawable.empty) && board.getItem(6).equals(board.getItem(7)) && board.getItem(7).equals(board.getItem(8)))
            return (Integer)board.getItem(6);
        // vertical
        if (!board.getItem(0).equals(R.drawable.empty) && board.getItem(0).equals(board.getItem(3)) && board.getItem(3).equals(board.getItem(6)))
            return (Integer)board.getItem(0);
        if (!board.getItem(1).equals(R.drawable.empty) && board.getItem(1).equals(board.getItem(4)) && board.getItem(4).equals(board.getItem(7)))
            return (Integer)board.getItem(1);
        if (!board.getItem(2).equals(R.drawable.empty) && board.getItem(2).equals(board.getItem(5)) && board.getItem(5).equals(board.getItem(8)))
            return (Integer)board.getItem(2);
        // diagonal
        if (!board.getItem(0).equals(R.drawable.empty) && board.getItem(0).equals(board.getItem(4)) && board.getItem(4).equals(board.getItem(8)))
            return (Integer)board.getItem(0);
        if (!board.getItem(2).equals(R.drawable.empty) && board.getItem(2).equals(board.getItem(4)) && board.getItem(4).equals(board.getItem(6)))
            return (Integer)board.getItem(2);
        return R.drawable.empty;
    }

    void shutdownClient() {
        syncClient.shutdown();
        Timber.e("CLIENT SHUTDOWN COMPLETED");
    }

    void endGameOnWin() {
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
        // Whose turn is it?
        gameStateMap.getItem("turn", new SuccessListener<SyncMap.Item>() {
            @Override
            public void onSuccess(SyncMap.Item result) {
                String currentPlayer = result.getData().optString("value", "X");

                // Make our move
                final Integer move = currentPlayer.contentEquals("O") ? R.drawable.naught : R.drawable.cross;
                board.setItem(position, move);
                syncBoard();

                // Now advance to the next player's turn.
                if (currentPlayer.contentEquals("X")) {
                    setTurn("O");
                    gameStateMap.setItem("playerX", UnaryJson(identity), new SuccessListener<SyncMap.Item>() {
                        @Override
                        public void onSuccess(SyncMap.Item result) {
                            Timber.d("Set playerX id");
                        }
                    });
                } else {
                    setTurn("X");
                    gameStateMap.setItem("playerO", UnaryJson(identity), new SuccessListener<SyncMap.Item>() {
                        @Override
                        public void onSuccess(SyncMap.Item result) {
                            Timber.d("Set playerO id");
                        }
                    });
                }

                // Now record this move in the game log.
                try {
                    final JSONObject item = new JSONObject();
                    item.put("turn", intToSymbol(move));
                    JSONArray loc = new JSONArray();
                    loc.put(position / 3);
                    loc.put(position % 3);
                    item.put("location", loc);
                    item.put("when", new Date().getTime() / 1000);

                    playerMoveLog.addItem(item, SyncList.Item.Metadata.withTtl(600), new SuccessListener<SyncList.Item>() {
                        @Override
                        public void onSuccess(SyncList.Item result) {
                            logView.append(item.toString() + "\n");
                        }
                    });
                } catch (JSONException xcp) {
                    Timber.e(xcp, "Cannot serialize log entry");
                }
            }
        });
    }

    private void authenticateAndStartSync() {
        String url = Uri.parse(BuildConfig.ACCESS_TOKEN_SERVICE_URL)
                         .buildUpon()
                         .appendQueryParameter("identity", identity)
                         .build()
                         .toString();
        Timber.d("Fetching Token from " + url);
        Ion.with(this)
            .load(url)
            .asString()
            .setCallback(new FutureCallback<String>() {
                @Override
                public void onCompleted(Exception e, String tokenServiceResponse) {
                    if (e == null) {
                        String accessToken = tokenServiceResponse;

                        try {
                            accessToken = new JSONObject(tokenServiceResponse).getString("token");
                        } catch (JSONException ex) {
                            // swallow this; assume the response is a simple string.
                        }

                        Timber.d("Retrieved token: " + accessToken);
                        SyncClient.create(getApplicationContext(), accessToken, SyncClient.Properties.defaultProperties(),
                                new SuccessListener<SyncClient>() {
                                    @Override
                                    public void onSuccess(SyncClient client) {
                                        syncClient = client;

                                        syncClient.setListener(new SyncClient.SyncClientListener() {
                                            @Override
                                            public void onConnectionStateChanged(final SyncClient.ConnectionState newState) {
                                                Timber.d("Connection status: " + newState.toString());

                                                if (newState == SyncClient.ConnectionState.CONNECTED) {
                                                    openBoardState();
                                                    openMoveHistory();
                                                    openGameState();
                                                }
                                            }
                                        });
                                    }
                                }
                        );
                    } else {
                        Timber.e("Error gathering a token: " + e);
                        Toast.makeText(TicTacActivity.this,
                                R.string.error_retrieving_access_token, Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
    }

    void documentUpdate() {
        try {
            renderBoard(boardState.getData());
            endGameOnWin();
        } catch (JSONException xcp) {
            Timber.e(xcp, "Exception in JSON");
        }
    }

    void openBoardState() {
        syncClient.openDocument(SyncOptions.create().withUniqueName("SyncGame").withTtl(3600), new SyncDocumentObserver() {
            @Override
            public void onUpdated(EventContext context, JSONObject data, JSONObject prevData) {
                documentUpdate();
            }
        }, new SuccessListener<SyncDocument>() {
            @Override
            public void onSuccess(SyncDocument doc) {
                boardState = doc;
                documentUpdate();
            }
        });
    }

    void openMoveHistory() {
        syncClient.openList(SyncOptions.create().withUniqueName("SyncGameLog").withTtl(3600), new SyncListObserver() {
            @Override
            public void onItemAdded(EventContext context, final SyncList.Item itemSnapshot) {
                logView.append(itemSnapshot.getData().toString() + "\n");
            }
        }, new SuccessListener<SyncList>() {
            @Override
            public void onSuccess(SyncList result) {
                playerMoveLog = result;

                playerMoveLog.queryItems(playerMoveLog.queryOptions(), new SuccessListener<SyncListPaginator>() {
                    @Override
                    public void onSuccess(SyncListPaginator paginator) {
                        final long size = paginator.getPageSize();
                        final ArrayList<SyncList.Item> items = paginator.getItems();
                        for (SyncList.Item item : items) {
                            Timber.d(item.getData().toString());
                            logView.append(item.getData().toString()+"\n");
                        }
                    }
                });
            }
        });
    }

    void openGameState() {
        syncClient.openMap(SyncOptions.create().withUniqueName("SyncGameState").withTtl(3600), new SyncMapObserver() {
            @Override
            public void onItemUpdated(EventContext context, SyncMap.Item itemSnapshot, JSONObject prevItemSnapshot) {
                logView.append(itemSnapshot.getKey() + " changed: " + itemSnapshot.getData().toString()+"\n");
            }
        }, new SuccessListener<SyncMap>() {
            @Override
            public void onSuccess(SyncMap thisSyncMap) {
                Timber.d("Opened game state");
                gameStateMap = thisSyncMap;

                // If need be (and only if no game already in play), pick a first player.
                gameStateMap.mutateItem("turn", new SyncMutator() {
                    @Override
                    public JSONObject onApplied(JSONObject currentData) {
                        if (currentData != null) {
                            Timber.d(currentData.toString());
                            if (!currentData.keys().hasNext())
                                return TicTacActivity.UnaryJson("X");
                            else
                                return null;  // i.e. do not change the current turn.
                        } else {
                            return TicTacActivity.UnaryJson("X"); // there was nothing so start anew
                        }
                    }
                }, new SuccessListener<SyncMap.Item>() {
                    @Override
                    public void onSuccess(SyncMap.Item item) {
                        statusView.setText("Up next is Player " + item.getData().optString("value", "<corrupt>"));
                    }

                    @Override
                    public void onError(ErrorInfo e) {
                        // Likely, our mutator returned 'null', aborting this mutation. Double-check.
                        gameStateMap.getItem("turn", new SuccessListener<SyncMap.Item>() {
                            @Override
                            public void onSuccess(SyncMap.Item item) {
                                statusView.setText("Joined during the turn of Player " + item.getData().optString("value", "<corrupt>"));
                            }

                            @Override
                            public void onError(ErrorInfo e) {
                                statusView.setText("Game state corrupt… " + e.getMessage());
                            }
                        });
                    }
                });
            }
            @Override
            public void onError(ErrorInfo errorInfo) {
                statusView.setText("FAILED TO OBTAIN MAP");
            }
        });
    }

    void newGame() {
        winText.setVisibility(View.GONE);
        for (int position = 0; position < 9; ++position) {
            board.setItem(position, R.drawable.empty);
        }
        syncBoard();
        setTurn("X");
        boardView.setEnabled(true);
    }

    void renderBoard(JSONObject data) throws JSONException {
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

    JSONObject serialiseBoard() throws JSONException {
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

    /**
     * A helper, to reduce code mass where we store single values in Sync objects.
     * @param contents must not be null.
     * @return The given string wrapped in { "value": … }.
     */
    private static JSONObject UnaryJson(String contents) {
        JSONObject X = new JSONObject();
        try {
            X.put("value", contents);
        } catch (JSONException e) {
            // Impossible; it's a String.
        }
        return X;
    }
}
