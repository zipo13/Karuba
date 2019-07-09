package il.co.woo.karuba;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Random;

public class TilesViewModel extends ViewModel {
    static final int NUMBER_OF_TILES = 36;

    private ArrayList<Integer> mTileArray;
    private int mNumberOfSelectedTiles;
    private int mLastSelectedRandom;

    //the games array indicates if a tile has a gem mark on it
    private boolean[] mGems = {false,false,true,true,
            false,false,false,false,
            true,true,true,false,true,
            true,true,true,false,false,
            false,false,false,false,
            false,false,false,false,
            true,false,false,false,true,
            true,true,true,false,false};

    public TilesViewModel() {
        initTiles();
    }

    public void initTiles() {
        //no tiles selected yet
        mNumberOfSelectedTiles = 0;
        mLastSelectedRandom = 0;
        if (mTileArray != null)
            mTileArray.clear();

        //create the tile array to simulate the bag and populate it with tile numbers
        mTileArray = new ArrayList<>();
        for (int i = 1; i <= NUMBER_OF_TILES; i++ ) {
            mTileArray.add(i);
        }
    }


    int getRandomTile() {
        if (mNumberOfSelectedTiles >= NUMBER_OF_TILES)
            return 0;
        //generate a random number between 1-36
        final int random = new Random().nextInt(NUMBER_OF_TILES-mNumberOfSelectedTiles);
        //increase the number of selected tiles;
        mNumberOfSelectedTiles++;
        mLastSelectedRandom = mTileArray.get(random);
        mTileArray.remove(random);
        return mLastSelectedRandom;
    }

    int getNumberOfSelectedTiles() {
        return mNumberOfSelectedTiles;
    }

    int getLastSelectedTile() { return mLastSelectedRandom; }

    boolean getTileHasGem(int tileIdx) {
       //the tile index is 1 based and the array is zero based so adjust
        int i = tileIdx -1;
       if (i < 0 || i >= NUMBER_OF_TILES)
           return false;

       return mGems[i];
    }


}
