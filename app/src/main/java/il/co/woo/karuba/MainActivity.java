package il.co.woo.karuba;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {


    private static final int NUMBER_OF_TILE_ROWS = 6;
    private static final int NUMBER_OF_TILE_COLUMNS = 6;
    private static final int TILE_GAP = 6;
    public static final int NEW_IMAGEVIEW_ID = 25879;

    private static final String TILE_IMG_NAME_PREFIX = "tile_";
    private static final String DRAWABLE_TYPE = "drawable";
    private static final String TAG = "MainActivity";
    private boolean mFirstTile = true;
    private TilesViewModel mViewModel;
    private int mNumberOfMovedTiles = 0;



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

        mGameBoardImageView.post(this::loadGameBoard);

        mNewTileButton.setOnClickListener(view -> {
            Log.d(TAG, "onClick: User clicked on new tile button");

            //check if there are more tiles to draw
            if (mViewModel.getNumberOfSelectedTiles() == TilesViewModel.NUMBER_OF_TILES) {
                Toast.makeText(this, getApplication().getString(R.string.no_more_tiles), Toast.LENGTH_SHORT).show();
                return;
            }

            //the first tile does not need to be moved
            if (mFirstTile) {
                drawNewTile();
                mFirstTile = false;
                return;
            }

            //duplicate the last tile
            ImageView newView = duplicateView(mLastTileImageView);
            //generate a new tile under the newly duplicated tile
            drawNewTile();

            //the animation needs to be postponed because we need to wait fot the
            //duplicated tile to generated first
            final Handler handler = new Handler();
            handler.postDelayed(() ->
                    slideTileToBaord(newView), 150);

            //if this was the last tile replace the tile with empty tile
            if (mViewModel.getNumberOfSelectedTiles() == TilesViewModel.NUMBER_OF_TILES) {
                Glide
                        .with(this)
                        .load(R.drawable.empty_tile)
                        .into(mNewTileButton);
            }

        });


    }

    private void loadGameBoard() {

        //mGameBoardImageView.setImageResource(R.drawable.tile_board);
        Glide.with(this)
                .load(R.drawable.tile_board)
                .centerCrop()
                .into(mGameBoardImageView);
    }


    private ImageView duplicateView(ImageView imageView) {
        Log.d(TAG, "duplicateView: Enter");
        //inflate a new tile from the layout
        @SuppressLint("InflateParams")
        ImageView newImageView = (ImageView)LayoutInflater.from(this).inflate(R.layout.tile_image_view, null);
        //generate a new unique ID
        newImageView.setId(NEW_IMAGEVIEW_ID + mNumberOfMovedTiles);
        newImageView.setTag(imageView.getTag());

        //put it exactly over the old tile
        newImageView.setX(mLastTileImageView.getX());
        newImageView.setY(mLastTileImageView.getY());

        //get the image of the last tile and put it in the new tile
        newImageView.setImageDrawable(imageView.getDrawable());
        //the width and height should also be exactly the same
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(imageView.getWidth(), imageView.getHeight());
        newImageView.setLayoutParams(layoutParams);
        //add it to the view group
        ViewGroup view = findViewById(android.R.id.content);
        view.addView(newImageView);
        //make sure its the top most - not sure if its needed
        newImageView.bringToFront();
        return newImageView;
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
            scaleResIntoImageView(mLastTileImageView.getWidth(),mLastTileImageView.getHeight(),imageResID,mLastTileImageView);
            //mLastTileImageView.setImageResource(imageResID);
            //save the resID so it will be used later to reload a resampled image
            mLastTileImageView.setTag(imageResID);
        } else {
            Log.d(TAG, "drawNewTile: failed to located tile resource id for tile name: " + tileFileName);
        }
    }

    private void slideTileToBaord(ImageView imageView) {
        Log.d(TAG, "slideTileToBaord: Enter");
        //check that we did not get garbage
        if (imageView == null)
            return;

        //get the dimensions and location of the board
        float gameBoardXCord = mGameBoardImageView.getX();
        float gameBoardYCord = mGameBoardImageView.getY();
        int gameBoardWidth = mGameBoardImageView.getWidth();
        int gameBoardHeight = mGameBoardImageView.getHeight();

        //calc the final size of the tile on the board
        int tileWidth = gameBoardWidth/NUMBER_OF_TILE_COLUMNS;
        int tileHeight = gameBoardHeight/NUMBER_OF_TILE_ROWS;

        //calc the location of the tile (row,col)
        int row = mNumberOfMovedTiles / NUMBER_OF_TILE_ROWS;
        int col = mNumberOfMovedTiles % NUMBER_OF_TILE_COLUMNS;
        //now calc the exact x,y of the tile
        float finalX = col*tileWidth + gameBoardXCord;
        float finalY = row*tileHeight + gameBoardYCord;

        //calc the scale factor for X and Y
        float scaleX = tileWidth/(float)imageView.getWidth();
        float scaleY = tileHeight/(float)imageView.getHeight();

        //create the animation
        imageView.setPivotX(0);
        imageView.setPivotY(0);
        imageView.animate()
                .scaleX(scaleX)
                .scaleY(scaleY)
                .x(finalX)
                .y(finalY)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {}

                    @Override
                    public void onAnimationEnd(Animator animation) {

                        // For changing actual height and width
                        int reqWidth = Math.round(imageView.getWidth() * imageView.getScaleX());
                        int reqHeight = Math.round(imageView.getHeight() * imageView.getScaleY());

                        //resmaple the bitmap to save memory
                        // Load a bitmap from the drawable folder
                        int tag = (int)imageView.getTag();
                        scaleResIntoImageView(reqWidth, reqHeight, tag, imageView);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {}

                    @Override
                    public void onAnimationRepeat(Animator animation) {}
                });
        mNumberOfMovedTiles++;

    }

    private void scaleResIntoImageView(int reqWidth, int reqHeight, int resID, ImageView imageView) {
        Bitmap bMap = BitmapFactory.decodeResource(getResources(), resID);
        Bitmap bMapScaled = Bitmap.createScaledBitmap(bMap, reqWidth, reqHeight, true);
        // Loads the resized Bitmap into an ImageView
        imageView.setImageBitmap(bMapScaled);
    }
}
