package il.co.woo.karuba;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import il.co.woo.karuba.databinding.ActivityPlayerSelectionBinding;

public class PlayerSelectionActivity extends AppCompatActivity {

    private static final String TAG = "PlayerSelectionActivity";

    private List<Integer> meepleLocations, templeLocations;
    private final int MAX_LOCATIONS = 11;
    private final Random random = new Random();
    private HashMap<View, Point> initialImagesLocations;
    private MediaPlayer bgMusicMediaPlayer;
    private ActivityPlayerSelectionBinding binder;

    private final float TOP_OFFSET = 0.14f;
    private final float VERTICAL_GAP = 0.165f;
    private final float HORIZONTAL_GAP = 0.123f;

    private final float MEEPLE_LEFT_OFFSET = 0.16f;
    private final float MEEPLE_LEFT_BOTTOM_OFFSET = 0.25f;
    private final float MEEPLE_MAX_Y_OFFSET = 0.93f;

    private final float TEMPLE_RIGHT_TOP_OFFSET = 0.18f;
    private final float TEMPLE_LEFT_TOP_OFFSET = 0.28f;
    private final float TEMPLE_MAX_X_OFFSET = 0.98f;
    private final float TEMPLE_TOP_OFFSET = 0.05f;

    private boolean firstMeepleSet = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binder = ActivityPlayerSelectionBinding.inflate(getLayoutInflater());
        setContentView(binder.getRoot());

        initialImagesLocations = new HashMap<>();

        binder.setMeeple.setOnClickListener(view -> {
            setMeepleButtonPressed();
        });

        binder.beginAdventure.setOnClickListener(view -> {
            moveToTileSelectionActivity();
        });

        binder.settings.setOnClickListener(view -> {
            Log.d(TAG, "onClick: User clicked on Settings button");
            settingsButtonClicked();
        });

        binder.meepleReset.setOnClickListener(view -> {
            resetToInitialState();
        });

        //we defer this action because during OnCreate the final size of the image is no known yet
        //and so we don't know how to scale the image yet
        binder.fullBoard.post(this::loadInitialImages);

        meepleLocations = new ArrayList<>();
        templeLocations = new ArrayList<>();
        resetAvailableLocations();
        checkIsNewGame();
    }

    private void resetToInitialState() {
        Log.d(TAG, "resetToInitialState: User clicked on reset button button");
        binder.beginAdventure.setVisibility(View.INVISIBLE);
        binder.beginAdventure.setEnabled(false);
        firstMeepleSet = true;

        for (View viewObj : initialImagesLocations.keySet()) {
            Point location = initialImagesLocations.get(viewObj);
            viewObj.animate()
                    .scaleX(1)
                    .scaleY(1)
                    .rotation(0)
                    .x(location.x)
                    .y(location.y);

        }
    }

    //try to start the bg music if needed
    @Override
    protected void onResume() {
        super.onResume();

        startBGMusic();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: returned from MainActivity");
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == MainActivity.CLEAR_RESULT) {
            resetToInitialState();
        }
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

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Enter");

        if (bgMusicMediaPlayer != null) {
            bgMusicMediaPlayer.reset();
            bgMusicMediaPlayer.release();
        }

        super.onDestroy();
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

    private void checkIsNewGame() {
        Log.d(TAG, "checkIsNewGame: Enter");
        TilesViewModel viewModel = new ViewModelProvider(this).get(TilesViewModel.class);
        if (!viewModel.isNewGame()) {
            moveToTileSelectionActivity();
        }
    }

    private void settingsButtonClicked() {
        Log.d(TAG, "settingsButtonClicked: Enter");
        Intent karubaSettingsActivity = new Intent(this, KarubaPrefActivity.class);
        startActivity(karubaSettingsActivity);
    }

    private void resetAvailableLocations() {
        Log.d(TAG, "resetAvailableLocations: Enter");
        meepleLocations.clear();
        templeLocations.clear();
        for (int i = 0; i < MAX_LOCATIONS; i++) {
            meepleLocations.add(i);
            templeLocations.add(i);
        }
    }

    private void moveToTileSelectionActivity() {
        Log.d(TAG, "moveToTileSelectionActivity: Enter");
        Intent intent = new Intent(this, MainActivity.class);
        startActivityForResult(intent, 100);
    }

    private void setMeepleButtonPressed() {
        Log.d(TAG, "setMeepleButtonPressed: Enter");
        if (firstMeepleSet) {
            firstMeepleSet = false;
            moveSetButtonUp();
        }
        resetAvailableLocations();
        randomMoveTempleAndMeeple(binder.temple1, binder.meeple1);
        randomMoveTempleAndMeeple(binder.temple2, binder.meeple2);
        randomMoveTempleAndMeeple(binder.temple3, binder.meeple3);
        randomMoveTempleAndMeeple(binder.temple4, binder.meeple4);
    }

    private void moveSetButtonUp() {
        Log.d(TAG, "moveSetButtonUp: Enter");
        final float prevYLocation = binder.setMeeple.getY();
        binder.setMeeple.animate()
                .y(binder.temple1.getY())
                .withEndAction(() -> {
                    binder.beginAdventure.setY(prevYLocation);
                    binder.beginAdventure.setEnabled(true);
                    binder.beginAdventure.setVisibility(View.VISIBLE);
                    binder.beginAdventure.animate().alpha(100).setDuration(10000);
                });
    }

    private void randomMoveTempleAndMeeple(ImageView temple, ImageView meeple) {
        Log.d(TAG, "randomMoveTempleAndMeeple: Enter");
        int selectedMeepleLocation = random.nextInt(meepleLocations.size());
        createMoveMeepleAnimation(meeple, meepleLocations.get(selectedMeepleLocation));

        int selectedTempleLocation = -1;

        boolean validCombination = false;
        while (!validCombination) {
            selectedTempleLocation = random.nextInt(templeLocations.size());
            int meepleLocation = meepleLocations.get(selectedMeepleLocation);
            int templeLocation = templeLocations.get(selectedTempleLocation);

            switch (meepleLocation) {
                case 2:
                    if (templeLocation != 0) {
                        validCombination = true;
                    }
                    break;
                case 1:
                    if (templeLocation > 1) {
                        validCombination = true;
                    }
                    break;
                case 0:
                    if (templeLocation > 2) {
                        validCombination = true;
                    }
                    break;
                case 8:
                    if (templeLocation != 10) {
                        validCombination = true;
                    }
                    break;
                case 9:
                    if (templeLocation < 9) {
                        validCombination = true;
                    }
                    break;
                case 10:
                    if (templeLocation < 8) {
                        validCombination = true;
                    }
                    break;
                default:
                    validCombination = true;
                    break;
            }
        }

        createMoveTempleAnimation(temple, templeLocations.get(selectedTempleLocation));

        meepleLocations.remove(selectedMeepleLocation);
        templeLocations.remove(selectedTempleLocation);
    }

    private void loadInitialImages() {
        Log.d(TAG, "loadInitialImages: Enter");
        Glide.with(this)
                .load(R.drawable.karuba_board)
                .centerCrop()
                .into(binder.fullBoard);

        Glide.with(this)
                .load(R.drawable.jungle)
                .fitCenter()
                .into(binder.jungleBg);

        saveInitialItemsLocations();
    }

    private void saveInitialItemsLocations() {
        Log.d(TAG, "saveInitialItemsLocations: Enter");
        initialImagesLocations.put(binder.meeple1, new Point((int) binder.meeple1.getX(), (int) binder.meeple1.getY()));
        initialImagesLocations.put(binder.meeple2, new Point((int) binder.meeple2.getX(), (int) binder.meeple2.getY()));
        initialImagesLocations.put(binder.meeple3, new Point((int) binder.meeple3.getX(), (int) binder.meeple3.getY()));
        initialImagesLocations.put(binder.meeple4, new Point((int) binder.meeple4.getX(), (int) binder.meeple4.getY()));

        initialImagesLocations.put(binder.temple1, new Point((int) binder.temple1.getX(), (int) binder.temple1.getY()));
        initialImagesLocations.put(binder.temple2, new Point((int) binder.temple2.getX(), (int) binder.temple2.getY()));
        initialImagesLocations.put(binder.temple3, new Point((int) binder.temple3.getX(), (int) binder.temple3.getY()));
        initialImagesLocations.put(binder.temple4, new Point((int) binder.temple4.getX(), (int) binder.temple4.getY()));

        initialImagesLocations.put(binder.setMeeple, new Point((int) binder.setMeeple.getX(), (int) binder.setMeeple.getY()));
    }

    private void createMoveMeepleAnimation(ImageView meeple, int locationIndex) {
        Log.d(TAG, "createMoveMeepleAnimation: Enter");
        Point destination = calculateMeepleLocation(locationIndex);
        float scale = 0.4f;
        int scaleXCorrection = (int) (meeple.getWidth() * scale);
        int scaleYCorrection = (int) (meeple.getHeight() * scale);
        meeple.animate()
                .scaleX(scale)
                .scaleY(scale)
                .x(destination.x - scaleXCorrection)
                .y(destination.y - scaleYCorrection);
    }

    private void createMoveTempleAnimation(ImageView temple, int locationIndex) {
        Log.d(TAG, "createMoveTempleAnimation: Enter");
        Point destination = calculateTempleLocation(locationIndex);
        int rotation = locationIndex >= 6 ? 90 : 0;
        float scale = 0.55f;
        int scaleXCorrection = (int) (temple.getWidth() * scale);
        int scaleYCorrection = (int) (temple.getHeight() * scale);
        temple.animate()
                .scaleX(scale)
                .scaleY(scale)
                .rotation(rotation)
                .x(destination.x - scaleXCorrection)
                .y(destination.y - scaleYCorrection);
    }

    private Point calculateMeepleLocation(int meepleIndex) {
        Log.d(TAG, "calculateMeepleLocation: Enter");
        int width = binder.fullBoard.getMeasuredWidth();
        int height = binder.fullBoard.getMeasuredHeight();

        int xCappedIndex = Math.max(0, meepleIndex - 5);
        float leftOffset = meepleIndex <= 4 ? MEEPLE_LEFT_OFFSET : MEEPLE_LEFT_BOTTOM_OFFSET;
        int x = (int) (width * leftOffset) + (int) (xCappedIndex * width * HORIZONTAL_GAP);

        int yCappedIndex = Math.min(meepleIndex, 5);
        int y = (int) (height * TOP_OFFSET + height * VERTICAL_GAP * yCappedIndex);
        if (y > MEEPLE_MAX_Y_OFFSET * height) {
            y = (int) (MEEPLE_MAX_Y_OFFSET * height);
        }
        return new Point(x, y);
    }

    private Point calculateTempleLocation(int templeIndex) {
        Log.d(TAG, "calculateTempleLocation: Enter");
        int width = binder.fullBoard.getMeasuredWidth();
        int height = binder.fullBoard.getMeasuredHeight();

        int xCappedIndex = Math.min(templeIndex, 6);
        int x = (int) (width * TEMPLE_LEFT_TOP_OFFSET) + (int) (xCappedIndex * width * HORIZONTAL_GAP);
        if (x > TEMPLE_MAX_X_OFFSET * width) {
            x = (int) (TEMPLE_MAX_X_OFFSET * width);
        }

        int yCappedIndex = Math.max(0, templeIndex - 6);
        float topOffset = templeIndex <= 5 ? TEMPLE_TOP_OFFSET : TEMPLE_RIGHT_TOP_OFFSET;
        int y = (int) (height * topOffset + height * VERTICAL_GAP * yCappedIndex);

        return new Point(x, y);
    }
}