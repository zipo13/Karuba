package il.co.woo.karuba;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

public class TilesViewModel extends AndroidViewModel {
    static final int NUMBER_OF_TILES = 36;
    private static final String KEY_USED_TILES_ARRAY = "key_used_tiles_arr";
    private static final String KEY_AVAIL_TILES_ARRAY = "key_avail_tiles_arr";
    private static final String TAG = "TilesViewModel";

    private ArrayList<Integer> availTileArray;
    private ArrayList<Integer> usedTileArray;
    private int mLastSelectedRandom;

    //the games array indicates if a tile has a gem mark on it
    private final boolean[] mGems = {false, false, true, true,
            false, false, false, false,
            true, true, true, false, true,
            true, true, true, false, false,
            false, false, false, false,
            false, false, false, false,
            true, false, false, false, true,
            true, true, true, false, false};

    public TilesViewModel(Application app) {
        super(app);
        initTiles();
        //if we failed to load saved data for some reason consider this is a new game
        if (!loadDataFromStorage()) {
            newGame();
        }
    }

    public boolean isNewGame() {
        return availTileArray.size() == NUMBER_OF_TILES;
    }

    private boolean loadDataFromStorage() {
        Log.d(TAG, "loadDataFromStorage: Enter");
        //check if we have saved data
        String json = FilePersistHelper.read(getApplication().getApplicationContext());
        if (json == null)
            return false;

        try {
            Log.d(TAG, "loadDataFromStorage: Trying to load data from JSON");
            //try and load the data one by one
            JSONObject jsonObj = new JSONObject(json);

            if (jsonObj.has(KEY_AVAIL_TILES_ARRAY)) {
                JSONArray jsonArr = jsonObj.getJSONArray(KEY_AVAIL_TILES_ARRAY);
                availTileArray.clear();
                if (jsonArr != null) {
                    for (int i = 0; i < jsonArr.length(); i++) {
                        availTileArray.add(jsonArr.getInt(i));
                    }
                }
            }

            if (jsonObj.has(KEY_USED_TILES_ARRAY)) {
                JSONArray jsonArr = jsonObj.getJSONArray(KEY_USED_TILES_ARRAY);
                usedTileArray.clear();
                if (jsonArr != null) {
                    for (int i = 0; i < jsonArr.length(); i++) {
                        usedTileArray.add(jsonArr.getInt(i));
                    }
                }
            }
            if (usedTileArray.size() > 0)
                mLastSelectedRandom = usedTileArray.get(usedTileArray.size() - 1);
            else
                mLastSelectedRandom = 0;

            Log.d(TAG, "loadDataFromStorage: Loading data was successful");

        } catch (JSONException ex) {
            Log.e(TAG, "loadDataFromStorage: Failed to create JSON object from JSON string");
            return false;
        }
        return true;
    }

    private void saveStateParams() {
        Log.d(TAG, "saveStateParams: Enter");
        //save the current game state
        //serialize the data into JSON and save it as text
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put(KEY_AVAIL_TILES_ARRAY, new JSONArray(availTileArray));
            jsonObj.put(KEY_USED_TILES_ARRAY, new JSONArray(usedTileArray));
            FilePersistHelper.create(getApplication().getApplicationContext(), jsonObj.toString());
        } catch (JSONException ex) {
            Log.e(TAG, "saveStateParams: Failed to create JSON object before saving it");
        }
    }

    void newGame() {
        Log.d(TAG, "newGame: Enter");
        initTiles();
        saveStateParams();
    }

    private void initTiles() {
        Log.d(TAG, "initTiles: Enter");
        //no tiles selected yet
        mLastSelectedRandom = 0;
        if (availTileArray != null)
            availTileArray.clear();

        if (usedTileArray != null)
            usedTileArray.clear();

        //create the tile array to simulate the bag and populate it with tile numbers
        availTileArray = new ArrayList<>();
        for (int i = 1; i <= NUMBER_OF_TILES; i++) {
            availTileArray.add(i);
        }

        usedTileArray = new ArrayList<>();
    }

    int getRandomTile() {
        Log.d(TAG, "getRandomTile: Enter");
        if (availTileArray.size() <= 0)
            return 0;
        //generate a random number between 0-35
        final int random = new Random().nextInt(availTileArray.size());
        mLastSelectedRandom = availTileArray.get(random);
        availTileArray.remove(random);
        usedTileArray.add(mLastSelectedRandom);
        saveStateParams();
        return mLastSelectedRandom;
    }


    int getLastSelectedTile() {
        Log.d(TAG, "getLastSelectedTile: Last selected tile is: " + mLastSelectedRandom);
        return mLastSelectedRandom;
    }

    boolean getTileHasGem(int tileIdx) {
        //the tile index is 1 based and the array is zero based so adjust
        int i = tileIdx - 1;
        if (i < 0 || i >= NUMBER_OF_TILES)
            return false;

        return mGems[i];
    }

    int getNumberOfTilesLeft() {
        return availTileArray.size();
    }

    ArrayList<Integer> getExistingTiles() {
        return usedTileArray;
    }

}
