package il.co.woo.karuba;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
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
    private static final int NEW_BACKGROUND_ID = 25979;

    private static final String TILE_IMG_NAME_PREFIX = "tile_";
    private static final String DRAWABLE_TYPE = "drawable";
    private static final String TAG = "MainActivity";
    private TilesViewModel mViewModel;
    private int mNumberOfMovedTiles;

    private TextToSpeech mTTS;
    private boolean mTTSInit = false;
    private MediaPlayer chimePlayer;
    private MediaPlayer mBGMusic;

    @BindView(R.id.new_tile)
    ImageButton mNewTileButton;
    @BindView(R.id.game_board)
    ImageView mGameBoardImageView;
    @BindView(R.id.main_layout)
    ConstraintLayout mMainLayout;
    @BindView(R.id.settings_button)
    ImageButton mSettingsButton;
    @BindView(R.id.reset_button)
    ImageButton mResetButton;
    @BindView(R.id.tts_status_button)
    ImageButton mTTSStatusButton;

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
        startBGMusic();
        mNewTileButton.setOnClickListener(view -> {
            Log.d(TAG, "onClick: User clicked on new tile button");
            newTileButtonClicked();
        });

        mSettingsButton.setOnClickListener(view -> {
            Log.d(TAG, "onClick: User clicked on Settings button");
            settingsButtonClicked();
        });

        //TTS init is very slow and so
        mTTSStatusButton.setOnClickListener(view -> {
            Log.d(TAG, "onClick: User clicked on TTS status button");
            if (mTTSInit) {
                Toast.makeText(this, getResources().getString(R.string.tile_tts_ready), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getResources().getString(R.string.tile_tts_not_ready), Toast.LENGTH_SHORT).show();
            }
        });

        mResetButton.setOnClickListener(view -> {
            Log.d(TAG, "onClick: User clicked on the reset button");

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogTheme));
            builder.setMessage(getResources().getString(R.string.start_new_game_question))
                    .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                        //clear the data
                        mViewModel.newGame();

                        //clear the board
                        ViewGroup parent = findViewById(android.R.id.content);
                        for (int i = mNumberOfMovedTiles; i >= 0; i--) {
                            ImageView tile = findViewById(NEW_IMAGE_VIEW_ID + i);
                            if (tile != null) {
                                parent.removeView(tile);
                            }
                        }

                        mNewTileButton.setImageResource(R.drawable.tile_back);
                        mNumberOfMovedTiles = 0;
                        mNewTileButton.setEnabled(true);
                    })
                    .setNegativeButton(getResources().getString(R.string.no), null)
                    .show();

        });
        prepareCamDistanceForFlipAffect();
    }

    //start the background music upon preferences check
    private void startBGMusic() {
        Log.d(TAG, "startBGMusic: Enter");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean playBgMusic = preferences.getBoolean(getString(R.string.pref_key_play_bg_music),
                getResources().getBoolean(R.bool.play_bg_music));

        //if this is the first time that we play bg music create the player
        if (playBgMusic) {
            if (mBGMusic == null) {
                mBGMusic = MediaPlayer.create(getApplicationContext(), R.raw.heart_of_the_jungle);
                mBGMusic.setLooping(true);
            }

            mBGMusic.start();
        }
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
                (mNumberOfMovedTiles != TilesViewModel.NUMBER_OF_TILES)) {

            //this is not the first tile so there should be there a last tile in the view model
            int lastTileResID = getTileResIDFromTileIdx(mViewModel.getLastSelectedTile());
            if (lastTileResID > 0) {

                ImageView newView = duplicateView(mNewTileButton, lastTileResID);
                //the animation needs to be postponed because we need to wait for the
                //duplicated tile to be generated first
                final Handler handler = new Handler();
                handler.postDelayed(() ->
                        slideTileToBoard(newView), 150);
            } else {
                Log.e(TAG, "newTileButtonClicked: Could not duplicate last tile becuase the res id is not valid");
            }
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

            //OnInit of TTS is run on the main thread and so is VERY slow
            new Thread(() -> {
                if (status == TextToSpeech.SUCCESS) {
                    //use English - not sure about other languages at the moment.
                    mTTS.setSpeechRate(0.7f);
                    mTTS.setPitch(1.1f);
                    int result = mTTS.setLanguage(Locale.US);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");

                        //notify the user that TTS will not work on this device
                        showToastOnUIThread(getResources().getString(R.string.TTS_missing_lang_error));
                    } else {
                        Log.d(TAG, "onInit: SUCCESS");
                        //Init went fine.
                        //Set a listener when the TTS message finish as we sometime want
                        //to chime if a tile with a gem was produced.
                        mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String utteranceId) {
                            }

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
                        mTTSInit = true;
                        runOnUiThread(() -> mTTSStatusButton.setVisibility(View.GONE));
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                    //notify the user that TTS will not work on this device
                    showToastOnUIThread(getResources().getString(R.string.TTS_missing_lang_error));
                }
            }).start();
        });
    }

    private void showToastOnUIThread(String msg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                msg,
                Toast.LENGTH_LONG).show());
    }


    //a helper function to create images and set a scaled image in them
    private ImageView createImageView(int newID, int x, int y, int width, int height, int resID) {
        //inflate an image view
        @SuppressLint("InflateParams") ImageView iv = (ImageView) LayoutInflater.from(this).inflate(R.layout.tile_image_view, null);
        //generate a new unique ID
        iv.setId(newID);

        iv.setX(x);
        iv.setY(y);
        scaleResIntoImageView(width, height, resID, iv);

        //the width and height should also be exactly the same
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(width, height);
        iv.setLayoutParams(layoutParams);
        return iv;

    }

    //this code is for the flip affect
    //if the camera is too close in some cases the flip will cut because its too close to the
    //screen
    private void prepareCamDistanceForFlipAffect() {
        Log.d(TAG, "prepareCamDistanceForFlipAffect: Enter");
        float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        float distance = mNewTileButton.getCameraDistance() * (scale);
        mNewTileButton.setCameraDistance(distance);
    }

    private void loadInitialImages() {
        Log.d(TAG, "loadInitialImages: Enter");
        Glide.with(this)
                .load(R.drawable.tile_board)
                .centerCrop()
                .into(mGameBoardImageView);

        ImageView background = createImageView(NEW_BACKGROUND_ID,
                0,
                mGameBoardImageView.getHeight(),
                mGameBoardImageView.getWidth(),
                Math.round(mMainLayout.getHeight() - mGameBoardImageView.getHeight()),
                R.drawable.jungle);

        //add it to the view group
        ViewGroup view = findViewById(android.R.id.content);
        view.addView(background, 0);

        //check with the view model to see if this is a new game or is there save data already
        ArrayList<Integer> usedTilesArray = mViewModel.getExistingTiles();
        if (usedTilesArray.size() == 0)
            return;

        //start drawing the existing tiles one by one
        for (int i = 0; i < usedTilesArray.size(); i++) {
            //if this is the last tile we want to draw it on the stack and not on the board
            if (i + 1 == usedTilesArray.size()) {
                int tileResId = getTileResIDFromTileIdx(usedTilesArray.get(i));
                mNewTileButton.setTag(tileResId);
                scaleResIntoImageView(mNewTileButton.getWidth(), mNewTileButton.getHeight(), tileResId, mNewTileButton);
            } else {
                drawTileOnBoard(i, usedTilesArray.get(i));
                mNumberOfMovedTiles++;
            }
        }

    }

    //Draw a tile directly on the board
    //the tile number will determine the tile location on the board as they are drawn one after another
    //the tile Idx is the number on the tile picture to know which tile image to draw
    private void drawTileOnBoard(int tileNumber, int tileIdx) {
        Rect tilePlacement = calculateTilePlacement(tileNumber);
        //get the image of the last tile and put it in the new tile
        int tileResID = getTileResIDFromTileIdx(tileIdx);
        //generate a new unique ID
        int newID = NEW_IMAGE_VIEW_ID + mNumberOfMovedTiles;

        //put it exactly over the old tile
        ImageView newImageView = createImageView(newID,
                tilePlacement.left,
                tilePlacement.top,
                tilePlacement.width(),
                tilePlacement.height(),
                tileResID);

        //add it to the view group
        ViewGroup view = findViewById(android.R.id.content);
        view.addView(newImageView);
        //make sure its the top most - not sure if its needed
        newImageView.bringToFront();
    }


    private ImageView duplicateView(ImageView imageView, int newTileResID) {
        Log.d(TAG, "duplicateView: Enter");
        //inflate a new tile from the layout
        //generate a new unique ID
        int newId = NEW_IMAGE_VIEW_ID + mNumberOfMovedTiles;

        //put it exactly over the old tile
        ImageView newImageView = createImageView(newId,
                Math.round(imageView.getX()),
                Math.round(imageView.getY()),
                imageView.getWidth(),
                imageView.getHeight(),
                newTileResID);

        newImageView.setTag(imageView.getTag());

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
        return getResources().getIdentifier(tileFileName, DRAWABLE_TYPE, getPackageName());
    }

    private Rect calculateTilePlacement(int tileIdx) {
        //get the dimensions and location of the board
        float gameBoardXCord = mGameBoardImageView.getX();
        float gameBoardYCord = mGameBoardImageView.getY();
        int gameBoardWidth = mGameBoardImageView.getWidth();
        int gameBoardHeight = mGameBoardImageView.getHeight();

        //calc the final size of the tile on the board
        int tileWidth = gameBoardWidth / NUMBER_OF_TILE_COLUMNS;
        int tileHeight = gameBoardHeight / NUMBER_OF_TILE_ROWS;

        //calc the location of the tile (row,col)
        int row = tileIdx / NUMBER_OF_TILE_ROWS;
        int col = tileIdx % NUMBER_OF_TILE_COLUMNS;
        //now calc the exact x,y of the tile
        int finalX = Math.round(col * tileWidth + gameBoardXCord);
        int finalY = Math.round(row * tileHeight + gameBoardYCord);

        return new Rect(finalX, finalY, finalX + tileWidth, finalY + tileHeight);
    }

    private void slideTileToBoard(ImageView imageView) {
        Log.d(TAG, "slideTileToBoard: Enter");
        //check that we did not get garbage
        if (imageView == null)
            return;

        Rect tilePlacement = calculateTilePlacement(mNumberOfMovedTiles);

        //calc the scale factor for X and Y
        float scaleX = tilePlacement.width() / (float) imageView.getWidth();
        float scaleY = tilePlacement.height() / (float) imageView.getHeight();

        //create the animation
        imageView.setPivotX(0);
        imageView.setPivotY(0);
        imageView.animate()
                .scaleX(scaleX)
                .scaleY(scaleY)
                .x(tilePlacement.left)
                .y(tilePlacement.top)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {

                        // For changing actual height and width
                        int reqWidth = Math.round(imageView.getWidth() * imageView.getScaleX());
                        int reqHeight = Math.round(imageView.getHeight() * imageView.getScaleY());

                        //resmaple the bitmap to save memory
                        // Load a bitmap from the drawable folder
                        int tag = (int) imageView.getTag();
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
                    scaleResIntoImageView(mNewTileButton.getWidth(), mNewTileButton.getHeight(), newTileResID, mNewTileButton);
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean readOutLoud = preferences.getBoolean(getString(R.string.pref_key_declare_tile_out_loud),
                getResources().getBoolean(R.bool.declare_tile_name_out_loud));

        if (readOutLoud && mTTSInit) {
            String text = getString(R.string.tile) + " " + mViewModel.getLastSelectedTile();

            //to get a call back from TTS we mst supply a KEY_PARAM_UTTERANCE_ID
            HashMap<String, String> ttsHashMap = new HashMap<>();
            ttsHashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, text);
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, ttsHashMap);
        } else {
            playChimeSound();
        }
    }

    private void playChimeSound() {
        Log.d(TAG, "playChimeSound: Enter");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean playChime = preferences.getBoolean(getString(R.string.pref_key_chime_on_gem),
                getResources().getBoolean(R.bool.chime_on_gem));

        if ((mViewModel.getTileHasGem(mViewModel.getLastSelectedTile())) && (playChime)) {
            Log.d(TAG, "playChimeSound: Playing chime");

            //try to reuse the chime player or create one if needed
            if (chimePlayer == null) {
                Log.d(TAG, "playChimeSound: First chimePlayer created");
                chimePlayer = MediaPlayer.create(this, R.raw.chime);
            }
            chimePlayer.start();
        }

        //Trying to play the chime is the last action so the button can be re enabled
        runOnUiThread(() -> mNewTileButton.setEnabled(true));

    }


    @Override
    protected void onPause() {
        super.onPause();

        //if we are playing bg music pause it
        if (mBGMusic != null) {
            if (mBGMusic.isPlaying()) {
                mBGMusic.pause();
            }
        }
    }

    //try to start the bg music if needed
    @Override
    protected void onResume() {
        super.onResume();

        startBGMusic();
    }

    //Text To Speech needs to be released before the app closes
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Enter");

        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        if (chimePlayer != null) {
            chimePlayer.reset();
            chimePlayer.release();
        }

        if (mBGMusic != null) {
            mBGMusic.reset();
            mBGMusic.release();
        }

        super.onDestroy();


    }
}
