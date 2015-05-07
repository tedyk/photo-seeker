package org.tedka.photoseeker.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.imagehandler.ImageCache.ImageCacheParams;
import com.android.imagehandler.ImageFetcher;

import org.tedka.photoseeker.R;
import org.tedka.photoseeker.channel.base.ChannelController;
import org.tedka.photoseeker.channel.flickr.FlickrController;
import org.tedka.photoseeker.channel.flickr.FlickrModel;
import org.tedka.photoseeker.util.Constants;
import org.tedka.photoseeker.util.Network;

import java.util.ArrayList;

/**
 * MainActivity of the Application
 */
public class MainActivity extends ActionBarActivity {

    private int imageThumbSize;
    private int imageThumbSpacing;
    private ImageFetcher imageFetcher;

    public static int currentPage = 1;
    private int lastItem = 0;
    private boolean endOfResultsDisplay = false;

    private TextView txtNoAlbums;
    private ProgressBar progressLoadMore;

    private GridView albumGrid;
    private ImageAdapter imageAdapter;
    private ChannelController channelController;
    private ProgressDialog progressDialog;
    private EditText txtSearch;

    private Handler myHandler = new Handler();
    private Runnable updateRunnable;

    private Animator animator;
    private int animationDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the UI elements, setup progressbars etc.
        initUIElements();

        // We are going to use Flickr here, so grab a reference to its controller
        channelController = FlickrController.getInstance();

        // Initialize all the image handling stuff - cache, grid etc.
        initImageHandling();
    }

    /**
     * Initialize all the UI elements
     */
    private void initUIElements() {
        txtSearch = (EditText) findViewById(R.id.searchText);
        albumGrid = (GridView) findViewById(R.id.photoGrid);
        txtNoAlbums = (TextView) findViewById(R.id.tvNoAlbums);
        progressLoadMore = (ProgressBar) findViewById(R.id.progress);
        progressLoadMore.setVisibility(View.GONE);
        imageThumbSize = getResources().getDimensionPixelSize(R.dimen.photo_thumbnail_size);
        imageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.photo_thumbnail_spacing);

        animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching images, please wait...");
        progressDialog.setCancelable(false);
        txtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    channelController.getImageFeed().clear();
                    currentPage = 1;
                    hideSoftKeyboard();
                    fetchAndShowImages(txtSearch.getText().toString().trim());
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Initialize all the Image Handling items - cache, grid, adapter etc.
     */
    private void initImageHandling() {
        // ImageFetcher loads images into ImageView children asynchronously
        ImageCacheParams cacheParams = new ImageCacheParams(this, Constants.IMAGE_CACHE_DIR);
        imageFetcher = new ImageFetcher(this, imageThumbSize);
        imageFetcher.setLoadingImage(R.drawable.thumbnail_placeholder);
        imageFetcher.addImageCache(this.getSupportFragmentManager(), cacheParams);

        imageAdapter = new ImageAdapter(this, channelController, imageFetcher);
        albumGrid.setAdapter(imageAdapter);
        albumGrid.setFastScrollEnabled(true);

        // Determine final width of the GridView and calculate number of columns and its width.
        albumGrid.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (imageAdapter.getNumColumns() == 0) {
                    final int numColumns = (int) Math.floor(albumGrid.getWidth() / (imageThumbSize + imageThumbSpacing));
                    if (numColumns > 0) {
                        final int columnWidth = (albumGrid.getWidth() / numColumns) - imageThumbSpacing;
                        imageAdapter.setNumColumns(numColumns);
                        imageAdapter.setItemHeight(columnWidth);

                    }
                }
            }
        });

        albumGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View v, int pos, long arg3) {
                // Zoom the image for a full screen view
                performImageZoom(v, pos);
            }
        });

        albumGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                // Pause fetcher to ensure smoother scrolling when flinging
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    imageFetcher.setPauseWork(true);
                } else {
                    imageFetcher.setPauseWork(false);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                final int _lastItem = firstVisibleItem + visibleItemCount;
                if (_lastItem > 0 && totalItemCount > 0) {
                    if (_lastItem == channelController.getImageFeed().size() &&
                            !endOfResultsDisplay && lastItem != _lastItem) {
                        lastItem = _lastItem;
                        // Last item is fully visible.
                        fetchAndShowImages(txtSearch.getText().toString().trim());
                    }
                }
            }
        });
    }

    /**
     * This method is invoked when the 'SEARCH' button is tapped. This kicks off a
     * image search with the keyword provided, and loads results into the imageGrid
     *
     * @param view
     */
    public void fireSearch(View view) {
        channelController.getImageFeed().clear();
        currentPage = 1;
        hideSoftKeyboard();
        fetchAndShowImages(txtSearch.getText().toString().trim().replaceAll(" ", ""));
    }

    /**
     * This method does the actual heavy-lifting of displaying the search results on the
     * screen.
     *
     * @param tag
     */
    private void fetchAndShowImages(final String tag) {

        if (currentPage == 1) {
            channelController.getImageFeed().clear();
            endOfResultsDisplay = false;
            lastItem = 0;
            // get new photos
            progressDialog.show();
        } else {
            progressLoadMore.setVisibility(View.VISIBLE);
        }

        if (Network.isNetworkAvailable(MainActivity.this)) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<FlickrModel> photoResults;
                    // get the photo search results
                    photoResults = FlickrController.getPhotos(tag, currentPage);
                    if (photoResults.size() > 0)
                        channelController.getImageFeed().addAll(photoResults);
                    else
                        endOfResultsDisplay = true;

                    currentPage++;

                    myHandler.post(updateRunnable);
                }
            }).start();
            updateRunnable = new Runnable() {
                @Override
                public void run() {

                    if (channelController.getImageFeed().size() > 0) {
                        imageAdapter.notifyDataSetChanged();
                        // Obtain current position to maintain scroll position
                        int currentPosition = albumGrid.getFirstVisiblePosition();

                        // Set new scroll position
                        albumGrid.smoothScrollToPosition(currentPosition + 1, 0);
                    } else
                        txtNoAlbums.setVisibility(View.VISIBLE);

                    progressDialog.dismiss();
                    progressLoadMore.setVisibility(View.GONE);
                }
            };
        } else {
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            progressLoadMore.setVisibility(View.GONE);
        }

    }


    /**
     * This method handles displaying the big image, along with the animations involved, when
     * tapped on a thumbnail
     *
     * @param thumbView
     * @param pos
     */
    private void performImageZoom(final View thumbView, int pos) {
        // If there's an animation in progress, cancel it process this.
        if (animator != null) {
            animator.cancel();
        }

        // Load the high-resolution image.
        final View expandedImageContainer = findViewById(R.id.expanded_image_container);
        final ImageView expandedImageView = (ImageView) findViewById(R.id.expanded_image);
        final TextView expandedImageTitle = (TextView) findViewById(R.id.expanded_image_title);
        final TextView expandedImagePrompt = (TextView) findViewById(R.id.info);

        imageFetcher.loadImage(channelController.getPhotoUrl(pos), expandedImageView);
        expandedImageTitle.setText(channelController.getImageFeed().get(pos).getTitle());

        // Calculate the starting and ending bounds for the zoomed-in image.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        /**************************************************************************
         * The start bounds are the global visible rectangle of the thumbnail,
         * and final bounds are the global visible rectangle of the container view.
         * Set the container view's offset as the origin for the bounds, since
         * that's the origin for the positioning animation properties (X, Y).
         **************************************************************************/
        thumbView.getGlobalVisibleRect(startBounds);
        findViewById(R.id.container).getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x+32, -globalOffset.y+32);

        /**************************************************************************
         * Adjust the start bounds to be the same aspect ratio as the final bounds
         * using the "center crop" technique. This prevents undesirable stretching
         * during the animation. Also calculate the start scaling factor (the end
         * scaling factor is always 1.0).
         **************************************************************************/
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height() > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view.
        thumbView.setAlpha(0f);
        expandedImageContainer.setVisibility(View.VISIBLE);

        // Construct and run the animation
        expandedImageContainer.setPivotX(0f);
        expandedImageContainer.setPivotY(0f);
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(expandedImageContainer, View.X, startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageContainer, View.Y, startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageContainer, View.SCALE_X, startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageContainer, View.SCALE_Y, startScale, 1f));
        set.setDuration(animationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                expandedImageTitle.setVisibility(View.VISIBLE);
                expandedImagePrompt.setVisibility(View.VISIBLE);
                animator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animator = null;
            }
        });
        set.start();
        animator = set;

        // Upon clicking the zoomed-in image, it zooms back down to the
        // original bounds and show the thumbnail instead of the expanded image.
        final float startScaleFinal = startScale;
        expandedImageContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (animator != null) {
                    animator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator.ofFloat(expandedImageContainer, View.X, startBounds.left))
                        .with(ObjectAnimator.ofFloat(expandedImageContainer, View.Y, startBounds.top))
                        .with(ObjectAnimator.ofFloat(expandedImageContainer, View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator.ofFloat(expandedImageContainer, View.SCALE_Y, startScaleFinal));
                set.setDuration(animationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageContainer.setVisibility(View.GONE);
                        animator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageContainer.setVisibility(View.GONE);
                        animator = null;
                    }
                });
                expandedImageTitle.setVisibility(View.GONE);
                expandedImagePrompt.setVisibility(View.GONE);
                set.start();
                animator = set;
            }
        });
    }

    /**
     * Hide the soft keyboard after the search is fired.
     */
    private void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

}
