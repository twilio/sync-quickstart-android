package com.twilio.sync.quickstart;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.twilio.sync.ErrorInfo;
import com.twilio.sync.Mutator;
import com.twilio.sync.SyncClient;
import com.twilio.sync.List;
import com.twilio.sync.ListObserver;
import com.twilio.sync.ListPaginator;
import com.twilio.sync.Map;
import com.twilio.sync.MapObserver;
import com.twilio.sync.Options;
import com.twilio.sync.Document;
import com.twilio.sync.DocumentObserver;
import com.twilio.sync.SuccessListener;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.text.DecimalFormat;

import timber.log.Timber;

public class TicTacActivity extends AppCompatActivity {
    private SyncClient syncClient;
    private Document boardState;
    private List playerMoveLog;
    private Map gameStateMap;
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

        authenticateAndStartSync();
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
        gameStateMap.setItem("turn", UnaryJson(turn), 0, new SuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                statusView.setText("Next up: Player " + turn +"\n");
            }
        });
    }

    void syncBoard() {
        try {
            Long flowId = System.currentTimeMillis();
            Timber.e("Set data flow id:" + new DecimalFormat("#").format(flowId));

            JSONObject newData = serialiseBoard();
            boardState.setData(newData, flowId, new SuccessListener<Void>() {
                @Override
                public void onSuccess(Void dummy) {
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
        gameStateMap.getItem("turn", 0, new SuccessListener<Map.Item>() {
            @Override
            public void onSuccess(Map.Item result) {
                String currentPlayer = result.getData().optString("value", "X");

                // Make our move
                final Integer move = currentPlayer.contentEquals("O") ? R.drawable.naught : R.drawable.cross;
                board.setItem(position, move);
                syncBoard();

                // Now advance to the next player's turn.
                if (currentPlayer.contentEquals("X")) {
                    setTurn("O");
                    gameStateMap.setItem("playerX", UnaryJson(identity), 0, new SuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Timber.d("Set playerX id");
                        }
                    });
                } else {
                    setTurn("X");
                    gameStateMap.setItem("playerO", UnaryJson(identity), 0, new SuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void result) {
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

                    playerMoveLog.addItem(item, 0, new SuccessListener<Long>() {
                        @Override
                        public void onSuccess(Long result) {
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
        String url = Uri.parse(BuildConfig.SERVER_TOKEN_URL)
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
                                        openBoardState();
                                        openMoveHistory();
                                        openGameState();
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
        syncClient.openDocument(new Options().withUniqueName("SyncGame"), new DocumentObserver() {
            @Override
            public void onRemoteUpdated(JSONObject data, JSONObject prevData) {
                documentUpdate();
            }
            @Override
            public void onResultUpdated(long flowId) {
                documentUpdate();
            }
        }, new SuccessListener<Document>() {
            @Override
            public void onSuccess(Document doc) {
                boardState = doc;
                documentUpdate();
            }
        });
    }

    void openMoveHistory() {
        syncClient.openList(new Options().withUniqueName("SyncGameLog"), new ListObserver() {
            @Override
            public void onRemoteItemAdded(final List.Item itemSnapshot) {
                logView.append(itemSnapshot.getData().toString() + "\n");
            }
        }, new SuccessListener<List>() {
            @Override
            public void onSuccess(List result) {
                playerMoveLog = result;

                playerMoveLog.queryItems(playerMoveLog.queryOptions(), new SuccessListener<ListPaginator>() {
                    @Override
                    public void onSuccess(ListPaginator paginator) {
                        final long size = paginator.getPageSize();
                        final ArrayList<List.Item> items = paginator.getItems();
                        for (List.Item item : items) {
                            Timber.d(item.getData().toString());
                            logView.append(item.getData().toString()+"\n");
                        }
                    }
                });
            }
        });
    }

    void openGameState() {
        syncClient.openMap(new Options().withUniqueName("SyncGameState"), new MapObserver() {
            @Override
            public void onRemoteItemUpdated(Map.Item itemSnapshot, Map.Item prevItemSnapshot) {
                logView.append(itemSnapshot.getKey() + " changed: " + itemSnapshot.getData().toString()+"\n");
            }
        }, new SuccessListener<Map>() {
            @Override
            public void onSuccess(Map thisSyncMap) {
                Timber.d("Opened game state");
                gameStateMap = thisSyncMap;

                // If need be (and only if no game already in play), pick a first player.
                gameStateMap.mutateItem("turn", new Mutator() {
                    @Override
                    public JSONObject onApplied(long flowId, JSONObject currentData) {
                        Timber.d(currentData.toString());
                        if (!currentData.keys().hasNext())
                            return TicTacActivity.UnaryJson("X");
                        else
                            return null;  // i.e. do not change the current turn.
                    }
                }, System.currentTimeMillis(), new SuccessListener<Map.Item>() {
                    @Override
                    public void onSuccess(Map.Item item) {
                        statusView.setText("Up next is Player " + item.getData().optString("value", "<corrupt>"));
                    }

                    @Override
                    public void onError(ErrorInfo e) {
                        // Likely, our mutator returned 'null', aborting this mutation. Double-check.
                        gameStateMap.getItem("turn", 0, new SuccessListener<Map.Item>() {
                            @Override
                            public void onSuccess(Map.Item item) {
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
