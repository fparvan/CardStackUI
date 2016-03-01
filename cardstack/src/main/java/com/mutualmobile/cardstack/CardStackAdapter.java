package com.mutualmobile.cardstack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * This class acts as an adapter for the {@link CardStackLayout} view. This adapter is intentionally
 * made an abstract class with following abstract methods -
 * <p/>
 * <p/>
 * {@link #getCount()} - Decides the number of views present in the view
 * <p/>
 * {@link #createView(int, ViewGroup)} - Creates the view for all positions in range [0, {@link #getCount()})
 * <p/>
 * Contains the logic for touch events in {@link #onTouch(View, MotionEvent)}
 */
public abstract class CardStackAdapter implements View.OnTouchListener, View.OnClickListener {


    public static final int ANIM_DURATION = 800;
    public static final int DECELERATION_FACTOR = 2;

    // Settings for the adapter from layout
    private float mCardGapBottom;
    private float mCardGapTop;
    private float mTouchHeight;
    private boolean mShowInitAnimation;

    private final int mScreenHeight;
    private int fullCardHeight;

    private View[] mCardViews;

    private float dp8;
    private final int dp30;

    private CardStackLayout mParent;

    private boolean mScreenTouchable = true;
    private int mSelectedCardPosition = 0;
    private int mParentPaddingTop = 0;
    /**
     * Defines and initializes the view to be shown in the {@link CardStackLayout}
     * Provides two parameters to the sub-class namely -
     *
     *
     * @return View corresponding to the position and parent container
     */
    public abstract View createView(int position, ViewGroup container);

    /**
     * Defines the number of cards that are present in the {@link CardStackLayout}
     *
     * @return cardCount - Number of views in the related {@link CardStackLayout}
     */
    public abstract int getCount();

    private void setScreenTouchable(boolean screenTouchable) {
        this.mScreenTouchable = screenTouchable;
    }

    private boolean isScreenTouchable()
    {
        return mScreenTouchable;
    }

    public CardStackAdapter(Context context) {
        Resources resources = context.getResources();

        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        mScreenHeight = dm.heightPixels;
        dp30 = (int) resources.getDimension(R.dimen.dp30);
        dp8 = (int) resources.getDimension(R.dimen.dp8);
        mTouchHeight = resources.getDimension(R.dimen.card_touch_height);

        mCardViews = new View[getCount()];
    }

    void addView(final int position) {
        View root = createView(position, mParent);
        root.setOnTouchListener(this);
        root.setTag(R.id.cardstack_internal_position_tag, position);
        root.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, fullCardHeight);
        root.setLayoutParams(lp);
        if (mShowInitAnimation) {
            root.setY(getLastCardY());
//            setScreenTouchable(false);
        } else {
            root.setY(getCardOriginalY() - mParentPaddingTop);
            setScreenTouchable(true);
        }

        mCardViews[position] = root;

        mParent.addView(root);
    }


    private float getSecondLastCardY()
    {
        return mScreenHeight - (mCardGapBottom * 3);
    }

    private float getLastCardY() {
        return mScreenHeight - (mCardGapBottom * 2);
    }

    private float getCardOriginalY() {
        return mParentPaddingTop;
    }

    private float getPositionForLastCardSwipped()
    {
        return mParentPaddingTop + mCardGapTop * 2;
    }

    private float getSecondCardY()
    {
        return mParentPaddingTop + mCardGapTop;
    }

    /**
     * Resets all cards in {@link CardStackLayout} to their initial positions
     *
     * @param r Execute r.run() once the reset animation is done
     */
    public void resetCards(Runnable r) {
        List<Animator> animations = new ArrayList<>(getCount());
        for (int i = 0; i < getCount(); i++) {
            final View child = mCardViews[i];
            if (i == 0) {
                animations.add(ObjectAnimator.ofFloat(child, View.Y, (int) child.getY(), getCardOriginalY()));
            }
            else
            {
                if (i == getCount() - 1) {
                    animations.add(ObjectAnimator.ofFloat(child, View.Y, (int) child.getY(), getLastCardY()));
                }
                else
                {
                    animations.add(ObjectAnimator.ofFloat(child, View.Y, (int) child.getY(), getSecondLastCardY()));
                }
            }
        }
        startAnimations(animations, r, 0);
        mSelectedCardPosition = 0;
        rearangeViews();
    }

    /**
     * Plays together all animations passed in as parameter. Once animation is completed, r.run() is
     * executed.
     * @param animations animations
     * @param r runnable
     * @param duration the duration of the animation
     */
    private void startAnimations(List<Animator> animations, final Runnable r, int duration) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animations);
        animatorSet.setDuration(duration);
        animatorSet.setInterpolator(new DecelerateInterpolator(DECELERATION_FACTOR));
        animatorSet.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                if (r != null) r.run();
                setScreenTouchable(true);

            }
        });
        animatorSet.start();
    }

    private void startAnimations(List<Animator> animations, final Runnable r)
    {
        startAnimations(animations, r, ANIM_DURATION);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!isScreenTouchable()) {
            return false;
        }

        float y = event.getRawY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;

            case MotionEvent.ACTION_MOVE:
                return false;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:

                if (Math.abs(v.getY() - y) < mTouchHeight)
                    onClick(v);

                return false;
        }
        return true;
    }

    @Override
    public void onClick(final View v) {
        if (!isScreenTouchable()) {
            return;
        }

        setScreenTouchable(false);

        int nextPosition = (int) v.getTag(R.id.cardstack_internal_position_tag);
        if (Math.abs(nextPosition - mSelectedCardPosition) > 1)
        {
            return;
        }
        else if (mSelectedCardPosition > 0 && mSelectedCardPosition == nextPosition)
        {
            mSelectedCardPosition = nextPosition - 1;
        }
        else {
            mSelectedCardPosition = nextPosition;
        }

            List<Animator> animations = new ArrayList<>(getCount());
            for (int i = 0; i < getCount(); i++) {
                View child = mCardViews[i];
                if (i != mSelectedCardPosition) {
                    float position;

                    if (i == 0)
                    {
                        position = getCardOriginalY();
                    }
                    else if (i < mSelectedCardPosition)
                    {
                        if (i == mSelectedCardPosition - 1)
                        {
                            position = getCardOriginalY();
                        }
                        else
                        {
                            position = getSecondCardY();
                        }
                    }
                    else if (i == getCount() - 1)
                    {
                        if (mSelectedCardPosition == getCount() - 2)
                        {
                            position = getSecondLastCardY();
                        }
                        else
                        {
                            position = getLastCardY();
                        }
                    }
                    else
                    {
                        position = getSecondLastCardY();
                    }

                    animations.add(ObjectAnimator.ofFloat(child, View.Y, (int) child.getY(), position));
                }
                else
                {
                    float position;

                    if (i == 0)
                    {
                        position = getCardOriginalY();
                    }
                    else if (i == getCount() - 1)
                    {
                        position = getPositionForLastCardSwipped();
                    }
                    else
                    {
                        position = getSecondCardY();
                    }

                    animations.add(ObjectAnimator.ofFloat(child, View.Y, (int) child.getY(), position));
                }
            }
            startAnimations(animations, new Runnable() {
                @Override
                public void run() {
                    setScreenTouchable(true);
                    if (mParent.getOnCardSelectedListener() != null) {
                        mParent.getOnCardSelectedListener().onCardSelected(v, mSelectedCardPosition);
                    }
                }
            });

        rearangeViews();
    }

    private void rearangeViews()
    {
        for (int i = getCount() - 1; i > mSelectedCardPosition; i--)
        {
            mCardViews[i].bringToFront();
        }
        mCardViews[0].getParent().requestLayout();
    }

    /**
     * Provides an API to {@link CardStackLayout} to set the parameters provided to it in its XML
     *
     * @param cardStackLayout Parent of all cards
     */
    void setAdapterParams(CardStackLayout cardStackLayout) {
        mParent = cardStackLayout;
        mCardGapBottom = cardStackLayout.getCardGapBottom();
        mCardGapTop = cardStackLayout.getCardGapTop();
        mShowInitAnimation = cardStackLayout.isShowInitAnimation();
        mParentPaddingTop = cardStackLayout.getPaddingTop();
        fullCardHeight = (int) (mScreenHeight - dp30 - dp8 - mCardGapBottom);
    }

    /**
     * Resets all cards in {@link CardStackLayout} to their initial positions
     */
    public void resetCards() {
        resetCards(null);
    }

    /**
     * Returns the position of selected card.
     */
    public int getSelectedCardPosition() {
        return mSelectedCardPosition;
    }

    /**
     * Since there is no view recycling in {@link CardStackLayout}, we maintain an instance of every
     * view that is set for every position. This method returns a view at the requested position.
     *
     * @param position Position of card in {@link CardStackLayout}
     * @return View at requested position
     */
    public View getCardView(int position) {
        if (mCardViews == null) return null;

        return mCardViews[position];
    }
}