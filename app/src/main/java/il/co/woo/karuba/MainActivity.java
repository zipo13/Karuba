package il.co.woo.karuba;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
    public static final int NEW_IMAGE_VIEW_ID = 25879;

    private static final String TILE_IMG_NAME_PREFIX = "tile_";
    private static final String DRAWABLE_TYPE = "drawable";
    private static final String TAG = "MainActivity";
    private boolean mFirstTile = true;
    private boolean mLastTile = false;
    private TilesViewModel mViewModel;
    private int mNumberOfMovedTiles = 0;



    @BindView(R.id.new_tile) ImageButton mNewTileButton;
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
                if (!mLastTile)
                    mLastTile = true;
                else
                    return;
            }

            //duplicate the last tile
            if (!mFirstTile) {
                ImageView newView = duplicateView(mNewTileButton);
                //the animation needs to be postponed because we need to wait fot the
                //duplicated tile to generated first
                final Handler handler = new Handler();
                handler.postDelayed(() ->
                        slideTileToBaord(newView), 150);
            }
            mFirstTile = false;

            //if this was the last tile replace the tile with empty tile
            if (mLastTile) {
                mNewTileButton.setTag(null);
                Glide
                        .with(this)
                        .load(R.drawable.empty_tile)
                        .into(mNewTileButton);
            } else {
                mNewTileButton.setImageResource(R.drawable.tile_back);
                startFlipAnimation(getNewRandomTileID());
            }
        });

        prepareCamDistanceForFlipAffect();
    }

    //this code is for the flip affect
    //if the camera is too close in some cases the filp will cut because its too close to the
    //screen
    private void prepareCamDistanceForFlipAffect() {
        Log.d(TAG, "prepareCamDistanceForFlipAffect: Enter");
        float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        float distance = mNewTileButton.getCameraDistance() * (scale );
        mNewTileButton.setCameraDistance(distance);
    }

    private void loadGameBoard() {
        Log.d(TAG, "loadGameBoard: Enter");
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
        newImageView.setId(NEW_IMAGE_VIEW_ID + mNumberOfMovedTiles);
        newImageView.setTag(imageView.getTag());

        //put it exactly over the old tile
        newImageView.setX(imageView.getX());
        newImageView.setY(imageView.getY());

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

    private int getNewRandomTileID() {
        Log.d(TAG, "getNewRandomTileID: Enter");
        int random = mViewModel.getRandomTile();
        if (random == 0)
            return 0;
        //Generate the resource name
        String tileFileName = TILE_IMG_NAME_PREFIX + random;

        //locate the id
        return getResources().getIdentifier(tileFileName, DRAWABLE_TYPE,getPackageName());
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
                .setListener(new AnimatorListenerAdapter()  {
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

                });
        mNumberOfMovedTiles++;

    }

    private void scaleResIntoImageView(int reqWidth, int reqHeight, int resID, ImageView imageView) {
        Log.d(TAG, "scaleResIntoImageView: Enter");
        Bitmap bMap = BitmapFactory.decodeResource(getResources(), resID);
        Bitmap bMapScaled = Bitmap.createScaledBitmap(bMap, reqWidth, reqHeight, true);
        // Loads the resized Bitmap into an ImageView
        imageView.setImageBitmap(bMapScaled);
    }


    public void startFlipAnimation(int newTileResID) {
        Log.d(TAG, "startFlipAnimation: Enter");
        //flip animation is made of 2 parts
        //1. flip the imageview half way (90 deg)
        //2. replace the image
        //3. flip the imageview back (-90 deg)
        mNewTileButton.animate()
                //flip it half way
                .setStartDelay(300)
                .withLayer()
                .rotationY(90)
                .setDuration(350)
                .withEndAction(() -> {
                    //replace the image
                    scaleResIntoImageView(mNewTileButton.getWidth(),mNewTileButton.getHeight(),newTileResID,mNewTileButton);
                    //flip it back
                    mNewTileButton.setTag(newTileResID);
                    mNewTileButton.setRotationY(-90);
                    mNewTileButton.animate()
                            .withLayer()
                            .rotationY(0)
                            .setDuration(350)
                            .start();
                });
    }
}
