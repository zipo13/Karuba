package il.co.woo.karuba;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final int NUMBER_OF_TILE_ROWS = 6;
    private static final int NUMBER_OF_TILE_COLUMNS = 6;
    private static final int NEW_IMAGE_VIEW_ID = 25879;

    private static final String TILE_IMG_NAME_PREFIX = "tile_";
    private static final String DRAWABLE_TYPE = "drawable";
    private static final String TAG = "MainActivity";
    private TilesViewModel mViewModel;
    private int mNumberOfMovedTiles;

    private TextToSpeech mTTS;
    private boolean mTTSInit;

    @BindView(R.id.new_tile) ImageButton mNewTileButton;
    @BindView(R.id.game_board) ImageView mGameBoardImageView;
    @BindView(R.id.main_layout) ConstraintLayout mMainLayout;
    @BindView(R.id.settings_button) ImageButton mSettingsButton;
    @BindView(R.id.reset_button) ImageButton mResetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Enter");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mNumberOfMovedTiles = 0;
        mViewModel = ViewModelProviders.of(this).get(TilesViewModel.class);
        //we defer this action because during OnCreate the final size of the image is no known yet
        //and so we don't know how to scale the image yet
        mGameBoardImageView.post(this::loadInitialImages);

        initTTS();
        mNewTileButton.setOnClickListener(view -> {
            Log.d(TAG, "onClick: User clicked on new tile button");
            newTileButtonClicked();
        });

        mSettingsButton.setOnClickListener( view -> {
            Log.d(TAG, "onCreate: User Clicked on Settings button");
            settingsButtonClicked();
        });
        prepareCamDistanceForFlipAffect();
    }

    private void settingsButtonClicked() {
        Log.d(TAG, "settingsButtonClicked: Enter");
        Intent karubaSettingsActivity = new Intent(this, KarubaPrefActivity.class);
        startActivity(karubaSettingsActivity);
    }

    private void newTileButtonClicked() {
        Log.d(TAG, "newTileButtonClicked: Enter");
        //check if there are more tiles to draw
        boolean thisIsTheLastTile = mViewModel.getNumberOfTilesLeft() <= 0;
        //to prevent fast clicks before the animation is over disable the button
        mNewTileButton.setEnabled(false);

        //duplicate the last tile if its not the first tile
        //and if not all the tiles were already moved
        if ((mViewModel.getNumberOfTilesLeft() != TilesViewModel.NUMBER_OF_TILES) &&
                (mNumberOfMovedTiles != TilesViewModel.NUMBER_OF_TILES)){
            ImageView newView = duplicateView(mNewTileButton);
            //the animation needs to be postponed because we need to wait fot the
            //duplicated tile to generated first
            final Handler handler = new Handler();
            handler.postDelayed(() ->
                    slideTileToBoard(newView), 150);
        }

        //if this was the last tile replace the tile with empty tile
        if (thisIsTheLastTile) {
            Toast.makeText(this, getApplication().getString(R.string.no_more_tiles), Toast.LENGTH_SHORT).show();
            mNewTileButton.setTag(null);
            Glide
                    .with(this)
                    .load(R.drawable.empty_tile)
                    .into(mNewTileButton);
        } else {
            mNewTileButton.setImageResource(R.drawable.tile_back);
            startFlipAnimation(getNewRandomTileResID());
        }
    }

    private void initTTS() {
        Log.d(TAG, "initTTS: Enter");
        //assume the worst
        mTTSInit = false;

        //create a TTS and do not use it until you get a confirmation that the init process went well
        mTTS = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                //use English - not sure about other languages at the moment.
                int result = mTTS.setLanguage(Locale.ENGLISH);

                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                } else {
                    Log.d(TAG, "onInit: SUCCESS");
                    //Init went fine.
                    //Set a listener when the TTS message finish as we sometime want
                    //to chime if a tile with a gem was produced.
                    mTTSInit = true;
                    mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {}

                        @Override
                        public void onDone(String utteranceId) {
                            Log.d(TAG, "onDone: TTS: " + utteranceId);
                            playChimeSound();
                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.d(TAG, "onError: TTS error while trying to say: " + utteranceId);
                        }
                    });
                }
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });
    }

    //this code is for the flip affect
    //if the camera is too close in some cases the flip will cut because its too close to the
    //screen
    private void prepareCamDistanceForFlipAffect() {
        Log.d(TAG, "prepareCamDistanceForFlipAffect: Enter");
        float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        float distance = mNewTileButton.getCameraDistance() * (scale );
        mNewTileButton.setCameraDistance(distance);
    }

    private void loadInitialImages() {
        Log.d(TAG, "loadInitialImages: Enter");
        Glide.with(this)
                .load(R.drawable.tile_board)
                .centerCrop()
                .into(mGameBoardImageView);

        Bitmap bMap = BitmapFactory.decodeResource(getResources(), R.drawable.jungle);
        Bitmap bMapScaled = Bitmap.createScaledBitmap(bMap, mMainLayout.getWidth(), mMainLayout.getHeight(), true);
        mMainLayout.setBackground(new BitmapDrawable(getResources(),bMapScaled));

        //check with the view model to see if this is a new game or is there save data already
        ArrayList<Integer> usedTilesArray = mViewModel.getExistingTiles();
        if (usedTilesArray.size() == 0)
            return;

        //start drawing the existing tiles one by one
        for (int i = 0; i < usedTilesArray.size(); i++) {
            //if this is the last tile we want to draw it on the stack and not on the board
            if (i+1 == usedTilesArray.size()) {
                int tileResId = getTileResIDFromTileIdx(usedTilesArray.get(i));
                mNewTileButton.setTag(tileResId);
                scaleResIntoImageView(mNewTileButton.getWidth(),mNewTileButton.getHeight(),tileResId,mNewTileButton);
            }
            else {
                drawTileOnBoard(i, usedTilesArray.get(i));
            }
        }

    }

    //Draw a tile directly on the board
    //the tile number will determine the tile location on the board as they are drawn one after another
    //the tile Idx is the number on the tile picture to know which tile image to draw
    private void drawTileOnBoard(int tileNumber,int tileIdx) {
        Rect tilePlacement = calculateTilePlacement(tileNumber);

        ImageView newImageView = (ImageView)LayoutInflater.from(this).inflate(R.layout.tile_image_view, null);
        //generate a new unique ID
        newImageView.setId(NEW_IMAGE_VIEW_ID + mNumberOfMovedTiles);
        mNumberOfMovedTiles++;

        //put it exactly over the old tile
        newImageView.setX(tilePlacement.left);
        newImageView.setY(tilePlacement.top);

        //get the image of the last tile and put it in the new tile
        int tileResID = getTileResIDFromTileIdx(tileIdx);
        scaleResIntoImageView(tilePlacement.width(),tilePlacement.height(),tileResID,newImageView);
        //the width and height should also be exactly the same
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(tilePlacement.width(), tilePlacement.height());
        newImageView.setLayoutParams(layoutParams);
        //add it to the view group
        ViewGroup view = findViewById(android.R.id.content);
        view.addView(newImageView);
        //make sure its the top most - not sure if its needed
        newImageView.bringToFront();
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

    private int getNewRandomTileResID() {
        Log.d(TAG, "getNewRandomTileResID: Enter");
        int random = mViewModel.getRandomTile();
        if (random == 0)
            return 0;
        return getTileResIDFromTileIdx(random);
    }

    private int getTileResIDFromTileIdx(int tileIdx) {
        //Generate the resource name
        String tileFileName = TILE_IMG_NAME_PREFIX + tileIdx;

        //locate the id
        return getResources().getIdentifier(tileFileName, DRAWABLE_TYPE,getPackageName());
    }

    private Rect calculateTilePlacement(int tileIdx) {
        //get the dimensions and location of the board
        float gameBoardXCord = mGameBoardImageView.getX();
        float gameBoardYCord = mGameBoardImageView.getY();
        int gameBoardWidth = mGameBoardImageView.getWidth();
        int gameBoardHeight = mGameBoardImageView.getHeight();

        //calc the final size of the tile on the board
        int tileWidth = gameBoardWidth/NUMBER_OF_TILE_COLUMNS;
        int tileHeight = gameBoardHeight/NUMBER_OF_TILE_ROWS;

        //calc the location of the tile (row,col)
        int row = tileIdx / NUMBER_OF_TILE_ROWS;
        int col = tileIdx % NUMBER_OF_TILE_COLUMNS;
        //now calc the exact x,y of the tile
        int finalX = Math.round(col*tileWidth + gameBoardXCord);
        int finalY = Math.round(row*tileHeight + gameBoardYCord);

        return new Rect(finalX,finalY,finalX+tileWidth,finalY+tileHeight);
    }

    private void slideTileToBoard(ImageView imageView) {
        Log.d(TAG, "slideTileToBoard: Enter");
        //check that we did not get garbage
        if (imageView == null)
            return;

        Rect tilePlacement = calculateTilePlacement(mNumberOfMovedTiles);

        //calc the scale factor for X and Y
        float scaleX = tilePlacement.width()/(float)imageView.getWidth();
        float scaleY = tilePlacement.height()/(float)imageView.getHeight();

        //create the animation
        imageView.setPivotX(0);
        imageView.setPivotY(0);
        imageView.animate()
                .scaleX(scaleX)
                .scaleY(scaleY)
                .x(tilePlacement.left)
                .y(tilePlacement.top)
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

    //this method is used to take a resource image and scale it to the needed size on the device
    private void scaleResIntoImageView(int reqWidth, int reqHeight, int resID, ImageView imageView) {
        Log.d(TAG, "scaleResIntoImageView: Enter");
        Bitmap bMap = BitmapFactory.decodeResource(getResources(), resID);
        Bitmap bMapScaled = Bitmap.createScaledBitmap(bMap, reqWidth, reqHeight, true);
        // Loads the resized Bitmap into an ImageView
        imageView.setImageBitmap(bMapScaled);
    }


    //flip card animation when the user clicks on a tile
    private void startFlipAnimation(int newTileResID) {
        Log.d(TAG, "startFlipAnimation: Enter");
        //flip animation is made of 2 parts
        //1. flip the ImageView half way (90 deg)
        //2. replace the image
        //3. flip the ImageView back (-90 deg)
        mNewTileButton.animate()
                //flip it half way
                .setStartDelay(300)
                .withLayer()
                .rotationY(90)
                .setDuration(300)
                .withEndAction(() -> {
                    //replace the image
                    scaleResIntoImageView(mNewTileButton.getWidth(),mNewTileButton.getHeight(),newTileResID,mNewTileButton);
                    //flip it back
                    mNewTileButton.setTag(newTileResID);
                    mNewTileButton.setRotationY(-90);
                    mNewTileButton.animate()
                            .withLayer()
                            .rotationY(0)
                            .setDuration(300)
                            .withEndAction(this::allAnimationEnd)
                            .start();
                });
    }

    //when all the animation ends we have some action that need to be done
    //play sounds
    private void allAnimationEnd() {
        Log.d(TAG, "allAnimationEnd: Enter");

        //re enable the button
        mNewTileButton.setEnabled(true);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean readOutLoud = preferences.getBoolean(getString(R.string.pref_key_declare_tile_out_loud),
                getResources().getBoolean(R.bool.declare_tile_name_out_loud));

        if (readOutLoud) {
            if (mTTSInit) {
                String text = getString(R.string.tile) + " " + mViewModel.getLastSelectedTile();
                mTTS.setSpeechRate(0.7f);

                //to get a call back from TTS we mst supply a KEY_PARAM_UTTERANCE_ID
                HashMap<String, String> ttsHashMap = new HashMap<>();
                ttsHashMap.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_NOTIFICATION));
                ttsHashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, text);
                mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, ttsHashMap);
            }
        } else {
            playChimeSound();
        }
    }

    private void playChimeSound() {
        Log.d(TAG, "playChimeSound: Enter");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean playChime = preferences.getBoolean(getString(R.string.pref_key_chime_on_gem),
                getResources().getBoolean(R.bool.chime_on_gem));

        if ((mViewModel.getTileHasGem(mViewModel.getLastSelectedTile())) && (playChime))  {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.chime);
            mediaPlayer.start();
        }
    }

    //Text To Speech needs to be released before the app closes
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Enter");

        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
        super.onDestroy();
    }
}
