package il.co.woo.karuba;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {


    private static final int NUMBER_OF_TILE_ROWS = 6;
    private static final int NUMBER_OF_TILE_COLUMNS = 6;

    private static final String TILE_IMG_NAME_PREFIX = "tile_";
    private static final String DRAWABLE_TYPE = "drawable";
    private static final String TAG = "MainActivity";
    private TilesViewModel mViewModel;



    @BindView(R.id.new_tile) ImageButton mNewTileButton;
    @BindView(R.id.last_tile) ImageView mLastTileImageView;
    @BindView(R.id.game_board) ImageView mGameBoardImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Enter");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mViewModel = ViewModelProviders.of(this).get(TilesViewModel.class);



        mNewTileButton.setOnClickListener(view -> {
            Log.d(TAG, "onClick: User clicked on new tile button");
            drawNewTile();
        });
    }

    private void drawNewTile() {
        Log.d(TAG, "drawNewTile: Enter");
        int random = mViewModel.getRandomTile();
        if (random == 0)
            return;

        //replace the image in the last tile image view with the tile number generated

        //Generate the resource name
        String tileFileName = TILE_IMG_NAME_PREFIX + random;

        //locate the id
        int imageResID = getResources().getIdentifier(tileFileName, DRAWABLE_TYPE,getPackageName());
        if (imageResID != 0) {
            mLastTileImageView.setImageResource(imageResID);
        } else {
            Log.d(TAG, "drawNewTile: failed to located tile resource id for tile name: " + tileFileName);
        }
    }

}
