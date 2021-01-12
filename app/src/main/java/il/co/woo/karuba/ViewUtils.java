package il.co.woo.karuba;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class ViewUtils {
    private static final String TAG = "ViewUtils";

    //this method is used to take a resource image and scale it to the needed size on the device
    public static void scaleResIntoImageView(int reqWidth, int reqHeight, int resID, AppCompatActivity activity, ImageView imageView) {
        Log.d(TAG, "scaleResIntoImageView: Enter");
        Bitmap bMap = BitmapFactory.decodeResource(activity.getResources(), resID);
        Bitmap bMapScaled = Bitmap.createScaledBitmap(bMap, reqWidth, reqHeight, true);
        // Loads the resized Bitmap into an ImageView
        imageView.setImageBitmap(bMapScaled);
    }

    public static Rect getResourceImageDimensions(AppCompatActivity activity, int resourceId) {
        BitmapFactory.Options dimensions = new BitmapFactory.Options();
        dimensions.inJustDecodeBounds = true;
        Bitmap mBitmap = BitmapFactory.decodeResource(activity.getResources(), resourceId, dimensions);
        return new Rect(0, 0, dimensions.outWidth, dimensions.outHeight);
    }

    //a helper function to create images and set a scaled image in them
    public static ImageView createImageView(AppCompatActivity activity, int newID, int x, int y, int width, int height, int resID) {
        //inflate an image view
        @SuppressLint("InflateParams") ImageView iv = (ImageView) LayoutInflater.from(activity).inflate(R.layout.tile_image_view, null);
        //generate a new unique ID
        iv.setId(newID);

        iv.setX(x);
        iv.setY(y);
        ViewUtils.scaleResIntoImageView(width, height, resID, activity, iv);

        //the width and height should also be exactly the same
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(width, height);
        iv.setLayoutParams(layoutParams);
        return iv;

    }
}
