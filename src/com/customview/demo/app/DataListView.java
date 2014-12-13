
package com.customview.demo.app;

import java.util.List;
import java.util.Stack;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;

/**
 * Custom ViewGroup that behaves similar to a ListView, except it has parallax effect on it's contents.
 */
public final class DataListView extends ScrollView {

    private static final float OVERLAY_MAX_TRANSPARENCY = 150f;

    private float mMinViewScaleFactor;

    // Determine which part of visible area a fully expanded view should take.
    private static final float FULL_VIEW_SCREEN_PART = 0.6f;

    private static final float COLLAPSED_VIEW_SCREEN_PART = 0.3f;

    private List<Data> mData;

    private int mViewMaxHeight;
    private int mViewMinHeight;
    private int mLastItemHeight;

    // Height of visible screen area
    private int mScrollViewHeight;

    // Current scroll position
    private int mScrollTopPosition;

    private ViewContainer mViewGroupContainer;

    private LayoutInflater mLayoutInflater;

    private int mTotalElements = 1;

    private Bundle mSavedState;

    private static final String STATE_SUPER = "savedState";

    private static final String STATE_SCROLL_POSITION = "scrollPos";

    public DataListView(Context context) {
        this(context, null);
    }

    public DataListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DataListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context c) {
        mLayoutInflater = LayoutInflater.from(c);
        mViewGroupContainer = new ViewContainer(c);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        addView(mViewGroupContainer, lp);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        mScrollTopPosition = t;
        mViewGroupContainer.onScrollChanged();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putInt(STATE_SCROLL_POSITION, mScrollTopPosition);
        bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState());

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            mSavedState = (Bundle) state;
            super.onRestoreInstanceState(mSavedState.getParcelable(STATE_SUPER));
            mScrollTopPosition = mSavedState.getInt(STATE_SCROLL_POSITION);
        }
    }

    /**
     * Sets displayable data.
     * 
     * @param dataset
     * @param reset true to reset scroll position
     */
    public void setData(List<Data> dataset, boolean reset) {
        // remove all children
        mViewGroupContainer.removeAllViews();

        mViewGroupContainer.mFirstVisibleItem = 0;
        mData = dataset;

        // at least we have last element
        mTotalElements = 1;

        if (mData != null) {
            mTotalElements += mData.size();
        }

        if (!reset) {
            // if we have restored scroll position display list from that position
            if (mSavedState != null) {
                final int scrollPosition = mSavedState.getInt(STATE_SCROLL_POSITION);

                post(new Runnable() {
                    @Override
                    public void run() {
                        scrollTo(0, scrollPosition);
                    }
                });
            } else {
                // if not, simply re-layout everything with current scroll position
                mViewGroupContainer.onScrollChanged();
            }
        } else {
            if (mScrollTopPosition == 0) {
                // just make sure it's redrawn
                mViewGroupContainer.onScrollChanged();
            } else {
                mScrollTopPosition = 0;
                // scroll to top
                scrollTo(0, 0);
            }
        }
        mSavedState = null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = getMeasuredHeight();

        // When we have a non-null height of ScrollView it means we can calculate dataset element sizes.
        if (height > 0 && mScrollViewHeight < height) {
            mScrollViewHeight = getMeasuredHeight();
            mViewMaxHeight = (int) (mScrollViewHeight * FULL_VIEW_SCREEN_PART);

            // Scaled-down view size.
            mViewMinHeight = (int) (mScrollViewHeight * COLLAPSED_VIEW_SCREEN_PART);

            // Relative size of a scaled-down view to a full-size view
            mMinViewScaleFactor = (float) mViewMinHeight / mViewMaxHeight;

            // Calculate height for last element.
            mLastItemHeight = mScrollViewHeight - mViewMaxHeight;

            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            mViewGroupContainer.onScrollChanged();
        }
    }

    private View mLastItem;

    /**
     * Returns the "last item" view. it's reused in case when scrolled away and back.
     * 
     * @return
     */
    private View getLastItem() {
        if (mLastItem == null) {
            mLastItem = mLayoutInflater.inflate(R.layout.last_element, this, false);
            mLastItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    DataListView.this.smoothScrollTo(0, 0);
                }
            });
        }

        return mLastItem;
    }

    /**
     * Custom ViewGroup responsible for Adapter items measurement and layout. Also contains logic for items reuse.
     */
    private final class ViewContainer extends ViewGroup {

        private final Stack<View> mViewStack = new Stack<View>();

        private int mFirstVisibleItem = -1;

        public ViewContainer(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int height = 0;
            if (mData != null) {
                height = (mData.size() * mViewMaxHeight);
            }

            // Take the last list element into account when determining ViewContainer height.
            if (height > mScrollViewHeight) {
                height += mLastItemHeight;
            }

            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            layoutChildren();
        }

        private void onScrollChanged() {
            if (mScrollViewHeight > 0) {
                // Remove views outside of visible area, add views that came into visible area and layout them.
                removeViews();
                addViews();
                layoutChildren();
            }
        }

        /**
         * Lays out all children. Calculates their sizes taking scroll position into account.
         */
        private void layoutChildren() {
            int top = -1;
            for (int i = 0; i < getChildCount(); i++) {
                int position = mFirstVisibleItem + i;

                int width = getMeasuredWidth();
                View child = getChildAt(i);

                if (top < 0) {
                    top = position * mViewMaxHeight;
                }

                if (position < mTotalElements - 1) {
                    float scale = calculateScale(top - mScrollTopPosition, mViewMaxHeight, mMinViewScaleFactor);
                    // Calculate view's position.
                    int height = Math.round(mViewMaxHeight * scale);
                    int bottom = top + height;
                    ViewHolder vh = (ViewHolder) child.getTag();

                    vh.<View>get(R.id.fading_view).setBackgroundColor(getOverlayColor(scale));

                    child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(mViewMaxHeight, MeasureSpec.EXACTLY));



                    child.layout(0, top, width, bottom);

                    top = bottom;
                } else {
                    // Layout the special last element on the list.
                    // Without that element, the last content element could never be fully expanded.
                    float scale = calculateScale(top - mScrollTopPosition - mViewMaxHeight, mLastItemHeight,
                            mMinViewScaleFactor);

                    View fadingView = child.findViewById(R.id.fading_view);
                    fadingView.setBackgroundColor(getOverlayColor(scale));

                    child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(mLastItemHeight, MeasureSpec.EXACTLY));

                    child.layout(0, top, width, top + mLastItemHeight);
                }
            }
        }

        private int getOverlayColor(float scale) {
            float alphaDiff = OVERLAY_MAX_TRANSPARENCY / mMinViewScaleFactor * (scale - mMinViewScaleFactor);
            return Color.argb(Math.max(0, Math.round(OVERLAY_MAX_TRANSPARENCY - alphaDiff)), 0, 0, 0);
        }

        private float calculateScale(int disposition, int normalHeight, float minScaleFactor) {
            float scale = 1f;

            if (disposition > normalHeight) {
                scale = minScaleFactor;
            } else if (disposition > 0) {
                // itemPosition from top in percents
                // 1 when item is 0px from top, 0 when item is mItemMaxHeight from top
                float deltaPercent = (float) (normalHeight - disposition) / normalHeight;

                // calculation result will be in range from MIN_SCALE_FACTOR to 1
                scale = minScaleFactor + deltaPercent * (1.0f - minScaleFactor);
            }

            return scale;
        }

        private void removeViews() {
            // remove children at top
            boolean removed = true;
            while (removed && getChildCount() > 0) {
                removed = false;
                View child = getChildAt(0);
                if (child.getBottom() < mScrollTopPosition) {
                    mFirstVisibleItem++;
                    storeViewInStack(child);
                    removed = true;
                }
            }

            // remove children from bottom
            if (getChildCount() > 0) {
                View firstVisible = getChildAt(0);
                int scrollBottomPosition = mScrollTopPosition + mScrollViewHeight;
                int visibleChildCount = ((scrollBottomPosition - firstVisible.getBottom()) / mViewMinHeight) + 2;

                removed = true;

                while (removed && getChildCount() > 0 && getChildCount() > visibleChildCount) {
                    removed = false;
                    View child = getChildAt(getChildCount() - 1);
                    if (child.getTop() > scrollBottomPosition) {
                        storeViewInStack(child);
                        removed = true;
                    }
                }
            }

            if (getChildCount() == 0) {
                mFirstVisibleItem = -1;
            }
        }

        private void addViews() {
            // bottom of visible area
            int b = mScrollTopPosition + mScrollViewHeight;

            int bottom = 0;
            int top = 0;

            if (getChildCount() == 0) {
                mFirstVisibleItem = mScrollTopPosition / mViewMaxHeight;
                addView(getView(mFirstVisibleItem));
            }

            top = mFirstVisibleItem * mViewMaxHeight;
            bottom = top + mViewMaxHeight + (mViewMinHeight * (getChildCount() - 1));

            // add children at top of list in visible available area
            while (top > mScrollTopPosition && mFirstVisibleItem > 0) {
                mFirstVisibleItem--;

                addView(getView(mFirstVisibleItem), 0);
                top -= mViewMinHeight;
            }

            // add children at bottom of list in visible available area
            int lastVisiblePosition = mFirstVisibleItem + getChildCount() - 1;

            while (bottom < b && lastVisiblePosition < mTotalElements - 1) {
                lastVisiblePosition++;
                addView(getView(lastVisiblePosition));
                bottom += mViewMinHeight;
            }
        }

        private void storeViewInStack(View view) {
            if (view.getId() != R.id.data_list_enditem) {
                mViewStack.push(view);
            }
            removeView(view);
        }

        private View getView(final int position) {
            if (position == mTotalElements - 1) {
                // return last item view
                return getLastItem();
            } else {
                final Data data = mData.get(position);
                ViewHolder vh;
                View convertView = null;
                if (!mViewStack.isEmpty()) {
                    convertView = mViewStack.pop();
                }
                if (convertView == null) {
                    vh = new ViewHolder();
                    convertView = mLayoutInflater.inflate(R.layout.data_element, this, false);
                    vh.putViewFromParent(convertView, R.id.data_image);
                    vh.putViewFromParent(convertView, R.id.fading_view);

                    convertView.setTag(vh);
                } else {
                    vh = (ViewHolder) convertView.getTag();
                }
                vh.<ImageView>get(R.id.data_image).setImageResource(data.getImageResource());

                return convertView;
            }
        }
    }
}
