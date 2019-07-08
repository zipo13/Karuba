package il.co.woo.karuba;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Random;

public class TilesViewModel extends ViewModel {
    private static final int NUMBER_OF_TILES = 36;

    private ArrayList<Integer> mTileArray;
    private int mNumberOfSelectedTiles;

    public TilesViewModel() {
        initTiles();
    }

    public void initTiles() {
        //no tiles selected yet
        mNumberOfSelectedTiles = 0;
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
        int randomTileNumber = mTileArray.get(random);
        mTileArray.remove(random);
        return randomTileNumber;
    }

    int getNumberOfSelectedTiles() {
        return mNumberOfSelectedTiles;
    }


}
