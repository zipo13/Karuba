package il.co.woo.karuba;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import il.co.woo.karuba.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public static final int CLEAR_RESULT = 123;

    private static final int NUMBER_OF_TILE_ROWS = 6;
    private static final int NUMBER_OF_TILE_COLUMNS = 6;
    private static final int NEW_IMAGE_VIEW_ID = 25879;
    private ActivityMainBinding binder;

    private static final String TILE_IMG_NAME_PREFIX = "tile_";
    private static final String DRAWABLE_TYPE = "drawable";
    private static final String TAG = "MainActivity";
    private TilesViewModel viewModel;
    private int numberOfMovedTiles;

    private TextToSpeech textToSpeech;
    private boolean ttsWasInit = false;
    private MediaPlayer chimePlayerMediaPlayer;
    private MediaPlayer bgMusicMediaPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Enter");
        super.onCreate(savedInstanceState);
        binder = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binder.getRoot());

        numberOfMovedTiles = 0;
        viewModel = new ViewModelProvider(this).get(TilesViewModel.class);

        //we defer this action because during OnCreate the final size of the image is no known yet
        //and so we don't know how to scale the image yet
        binder.gameBoard.post(this::loadInitialImages);

        initTTS();
        startBGMusic();
        binder.newTile.setOnClickListener(view -> {
            Log.d(TAG, "onClick: User clicked on new tile button");
            newTileButtonClicked();
        });

        binder.settingsButton.setOnClickListener(view -> {
            Log.d(TAG, "onClick: User clicked on Settings button");
            settingsButtonClicked();
        });

        //TTS init is very slow and so
        binder.ttsStatusButton.setOnClickListener(view -> {
            Log.d(TAG, "onClick: User clicked on TTS status button");
            if (ttsWasInit) {
                Toast.makeText(this, getResources().getString(R.string.tile_tts_ready), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getResources().getString(R.string.tile_tts_not_ready), Toast.LENGTH_SHORT).show();
            }
        });

        binder.resetButton.setOnClickListener(view -> {
            Log.d(TAG, "onClick: User clicked on the reset button");

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogTheme));
            builder.setMessage(getResources().getString(R.string.start_new_game_question))
                    .setPositiveButton(getResources().getString(R.string.yes), (dialog, which) -> {
                        //clear the data
                        viewModel.newGame();

                        //clear the board
                        ViewGroup parent = findViewById(android.R.id.content);
                        for (int i = numberOfMovedTiles; i >= 0; i--) {
                            ImageView tile = findViewById(NEW_IMAGE_VIEW_ID + i);
                            if (tile != null) {
                                parent.removeView(tile);
                            }
                        }

                        binder.newTile.setImageResource(R.drawable.tile_back);
                        numberOfMovedTiles = 0;
                        binder.newTile.setEnabled(true);

                        Intent previousScreen = new Intent(getApplicationContext(), PlayerSelectionActivity.class);
                        setResult(CLEAR_RESULT, previousScreen);
                        finish();
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
            if (bgMusicMediaPlayer == null) {
                bgMusicMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.heart_of_the_jungle);
                bgMusicMediaPlayer.setLooping(true);
            }

            bgMusicMediaPlayer.start();
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
        boolean thisIsTheLastTile = viewModel.getNumberOfTilesLeft() <= 0;
        //to prevent fast clicks before the animation is over disable the button
        binder.newTile.setEnabled(false);

        //duplicate the last tile if its not the first tile
        //and if not all the tiles were already moved
        if ((viewModel.getNumberOfTilesLeft() != TilesViewModel.NUMBER_OF_TILES) &&
                (numberOfMovedTiles != TilesViewModel.NUMBER_OF_TILES)) {

            //this is not the first tile so there should be there a last tile in the view model
            int lastTileResID = getTileResIDFromTileIdx(viewModel.getLastSelectedTile());
            if (lastTileResID > 0) {

                ImageView newView = duplicateView(binder.newTile, lastTileResID);
                //the animation needs to be postponed because we need to wait for the
                //duplicated tile to be generated first
                final Handler handler = new Handler();
                handler.postDelayed(() ->
                        slideTileToBoard(newView), 150);
            } else {
                Log.e(TAG, "newTileButtonClicked: Could not duplicate last tile because the res id is not valid");
            }
        }

        //if this was the last tile replace the tile with empty tile
        if (thisIsTheLastTile) {
            Toast.makeText(this, getApplication().getString(R.string.no_more_tiles), Toast.LENGTH_SHORT).show();
            binder.newTile.setTag(null);
            Glide
                    .with(this)
                    .load(R.drawable.empty_tile)
                    .into(binder.newTile);
        } else {
            binder.newTile.setImageResource(R.drawable.tile_back);
            startFlipAnimation(getNewRandomTileResID());
        }
    }

    private void initTTS() {
        Log.d(TAG, "initTTS: Enter");
        //assume the worst
        ttsWasInit = false;

        //create a TTS and do not use it until you get a confirmation that the init process went well
        textToSpeech = new TextToSpeech(this, status -> {

            //OnInit of TTS is run on the main thread and so is VERY slow
            new Thread(() -> {
                if (status == TextToSpeech.SUCCESS) {
                    //use English - not sure about other languages at the moment.
                    textToSpeech.setSpeechRate(0.7f);
                    textToSpeech.setPitch(1.1f);
                    int result = textToSpeech.setLanguage(Locale.US);

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
                        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
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
                        ttsWasInit = true;
                        runOnUiThread(() -> binder.ttsStatusButton.setVisibility(View.GONE));
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

    //this code is for the flip affect
    //if the camera is too close in some cases the flip will cut because its too close to the
    //screen
    private void prepareCamDistanceForFlipAffect() {
        Log.d(TAG, "prepareCamDistanceForFlipAffect: Enter");
        float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        float distance = binder.newTile.getCameraDistance() * (scale);
        binder.newTile.setCameraDistance(distance);
    }

    private void loadInitialImages() {
        Log.d(TAG, "loadInitialImages: Enter");
        Glide.with(this)
                .load(R.drawable.tile_board)
                .centerCrop()
                .into(binder.gameBoard);

        Glide.with(this)
                .load(R.drawable.jungle).fitCenter()
                .into(binder.tileJungleBg);

        //check with the view model to see if this is a new game or is there save data already
        ArrayList<Integer> usedTilesArray = viewModel.getExistingTiles();
        if (usedTilesArray.size() == 0)
            return;

        //start drawing the existing tiles one by one
        for (int i = 0; i < usedTilesArray.size(); i++) {
            //if this is the last tile we want to draw it on the stack and not on the board
            if (i + 1 == usedTilesArray.size()) {
                int tileResId = getTileResIDFromTileIdx(usedTilesArray.get(i));
                binder.newTile.setTag(tileResId);
                ViewUtils.scaleResIntoImageView(binder.newTile.getWidth(), binder.newTile.getHeight(), tileResId, this, binder.newTile);
            } else {
                drawTileOnBoard(i, usedTilesArray.get(i));
                numberOfMovedTiles++;
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
        int newID = NEW_IMAGE_VIEW_ID + numberOfMovedTiles;

        //put it exactly over the old tile
        ImageView newImageView = ViewUtils.createImageView(this, newID,
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
        int newId = NEW_IMAGE_VIEW_ID + numberOfMovedTiles;

        //put it exactly over the old tile
        ImageView newImageView = ViewUtils.createImageView(this, newId,
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
        int random = viewModel.getRandomTile();
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
        float gameBoardXCord = binder.gameBoard.getX();
        float gameBoardYCord = binder.gameBoard.getY();
        int gameBoardWidth = binder.gameBoard.getWidth();
        int gameBoardHeight = binder.gameBoard.getHeight();

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

        Rect tilePlacement = calculateTilePlacement(numberOfMovedTiles);

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

                        //resample the bitmap to save memory
                        // Load a bitmap from the drawable folder
                        int tag = (int) imageView.getTag();
                        ViewUtils.scaleResIntoImageView(reqWidth, reqHeight, tag, MainActivity.this, imageView);
                    }

                });
        numberOfMovedTiles++;

    }


    //flip card animation when the user clicks on a tile
    private void startFlipAnimation(int newTileResID) {
        Log.d(TAG, "startFlipAnimation: Enter");
        //flip animation is made of 2 parts
        //1. flip the ImageView half way (90 deg)
        //2. replace the image
        //3. flip the ImageView back (-90 deg)
        binder.newTile.animate()
                //flip it half way
                .setStartDelay(300)
                .withLayer()
                .rotationY(90)
                .setDuration(300)
                .withEndAction(() -> {
                    //replace the image
                    ViewUtils.scaleResIntoImageView(binder.newTile.getWidth(), binder.newTile.getHeight(), newTileResID, this, binder.newTile);
                    //flip it back
                    binder.newTile.setTag(newTileResID);
                    binder.newTile.setRotationY(-90);
                    binder.newTile.animate()
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

        if (readOutLoud && ttsWasInit) {
            String text = getString(R.string.tile) + " " + viewModel.getLastSelectedTile();

            //to get a call back from TTS we mst supply a KEY_PARAM_UTTERANCE_ID
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, text);
            }
            else {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
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

        if ((viewModel.getTileHasGem(viewModel.getLastSelectedTile())) && (playChime)) {
            Log.d(TAG, "playChimeSound: Playing chime");

            //try to reuse the chime player or create one if needed
            if (chimePlayerMediaPlayer == null) {
                Log.d(TAG, "playChimeSound: First chimePlayer created");
                chimePlayerMediaPlayer = MediaPlayer.create(this, R.raw.chime);
            }
            chimePlayerMediaPlayer.start();
        }

        //Trying to play the chime is the last action so the button can be re enabled
        runOnUiThread(() -> binder.newTile.setEnabled(true));

    }


    @Override
    protected void onPause() {
        super.onPause();

        //if we are playing bg music pause it
        if (bgMusicMediaPlayer != null) {
            if (bgMusicMediaPlayer.isPlaying()) {
                bgMusicMediaPlayer.pause();
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

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (chimePlayerMediaPlayer != null) {
            chimePlayerMediaPlayer.reset();
            chimePlayerMediaPlayer.release();
        }

        if (bgMusicMediaPlayer != null) {
            bgMusicMediaPlayer.reset();
            bgMusicMediaPlayer.release();
        }

        super.onDestroy();
    }
}
