package org.tedka.photoseeker.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.tedka.photoseeker.R;
import org.tedka.photoseeker.channel.base.ChannelController;
import org.tedka.photoseeker.channel.base.ChannelModel;

/**
 * The Adapter used to handle images on the results grid
 */
public class ImageAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private RelativeLayout.LayoutParams mImageViewLayoutParams;
    private ChannelController channelController;
    private int mItemHeight = 0;
    private int mNumColumns = 0;

    /**
     * Initialize the ImageAdapter instance with the corresponding channel controller and
     * imagefetcher
     *
     * @param mainActivity
     * @param _c
     */
    public ImageAdapter(MainActivity mainActivity, ChannelController _c) {
        this.channelController = _c;
        mInflater = (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageViewLayoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    /**
     * Fetch the count of images in the results
     *
     * @return
     */
    public int getCount() {
        return channelController.getImageFeed().size();
    }

    /**
     * Set the number of columns in the grid
     *
     * @param numColumns
     */
    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
    }

    /**
     * Get the number of columns in the grid
     *
     * @return
     */
    public int getNumColumns() {
        return mNumColumns;
    }

    /**
     * Set the height of image item
     *
     * @param height
     */
    public void setItemHeight(int height) {
        if (height == mItemHeight) {
            return;
        }
        mItemHeight = height;
        mImageViewLayoutParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, mItemHeight);
        notifyDataSetChanged();
    }

    /**
     * Return the ChannelModel instance corresponding to the item at
     * given position
     *
     * @param position
     * @return
     */
    public ChannelModel getItem(int position) {
        return channelController.getImageFeed().get(position);
    }

    /**
     * Implement the method in interface android.widget.Adapter
     * @param position
     * @return
     */
    public long getItemId(int position) {
        return position;
    }

    /**
     * Create the thumbnail view that is to be shown in a given position of the grid
     *
     * @param position
     * @param view
     * @param parent
     * @return
     */
    public View getView(final int position, View view, ViewGroup parent) {
        ViewHolder holder;

        if (view == null) {
            holder = new ViewHolder();
            view = mInflater.inflate(R.layout.thumbnail, null);
            holder.thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            holder.title = (TextView) view.findViewById(R.id.title);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        holder.thumbnail.setLayoutParams(mImageViewLayoutParams);

        // Check the height matches our calculated column width
        if (holder.thumbnail.getLayoutParams().height != mItemHeight) {
            holder.thumbnail.setLayoutParams(mImageViewLayoutParams);
        }

        ChannelModel photo = getItem(position);
        Picasso.with(parent.getContext())
                .load(channelController.getPhotoUrl(position))
                .placeholder(R.drawable.thumbnail_placeholder)
                .error(R.drawable.thumbnail_placeholder)
                .into(holder.thumbnail);
        holder.thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.title.setText(photo.getTitle());

        return view;
    }

    /**
     * A holder for the view. Currently has only the thumbnail enabled as per requirements.
     * The title can be used to handle the image title.
     */
    class ViewHolder {
        ImageView thumbnail;
        TextView title;
    }
}
