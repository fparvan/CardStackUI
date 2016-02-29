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


    public static final int ANIM_DURATION = 600;
    public static final int DECELERATION_FACTOR = 2;

    public static final int INVALID_CARD_POSITION = 0;

    // Settings for the adapter from layout
    private float mCardGapBottom;
    private float mCardGapTop;
    private int mParallaxScale;
    private boolean mParallaxEnabled;
    private boolean mShowInitAnimation;

    private final int mScreenHeight;
    private int fullCardHeight;

    private View[] mCardViews;

    private float dp8;
    private final int dp30;

    private CardStackLayout mParent;

    private boolean mScreenTouchable = false;
    private float mTouchFirstY = -1;
    private float mTouchPrevY = -1;
    private float mTouchDistance = 0;
    private int mSelectedCardPosition = INVALID_CARD_POSITION;
    private float scaleFactorForElasticEffect;
    private int mParentPaddingTop = 0;
    private int mCardPaddingInternal = 0;

    /**
     * Defines and initializes the view to be shown in the {@link CardStackLayout}
     * Provides two parameters to the sub-class namely -
     *
     * @param position
     * @param container
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

    /**
     * Returns true if no animation is in progress currently. Can be used to disable any events
     * if they are not allowed during an animation. Returns false if an animation is in progress.
     *
     * @return - true if animation in progress, false otherwise
     */
    public boolean isScreenTouchable() {
        return mScreenTouchable;
    }

    public CardStackAdapter(Context context) {
        Resources resources = context.getResources();

        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        mScreenHeight = dm.heightPixels;
        dp30 = (int) resources.getDimension(R.dimen.dp30);
        scaleFactorForElasticEffect = (int) resources.getDimension(R.dimen.dp8);
        dp8 = (int) resources.getDimension(R.dimen.dp8);

        mCardViews = new View[getCount()];
    }

    void addView(final int position) {
        View root = createView(position, mParent);
        root.setOnTouchListener(this);
        root.setTag(R.id.cardstack_internal_position_tag, position);
        root.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        mCardPaddingInternal = root.getPaddingTop();

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, fullCardHeight);
        root.setLayoutParams(lp);
        if (mShowInitAnimation) {
            root.setY(getLastCardY());
            setScreenTouchable(false);
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
        startAnimations(animations, r, true);
        mSelectedCardPosition = 0;
        rearangeViews();
    }

    /**
     * Plays together all animations passed in as parameter. Once animation is completed, r.run() is
     * executed. If parameter isReset is set to true, {@link #mSelectedCardPosition} is set to {@link #INVALID_CARD_POSITION}
     * @param animations
     * @param r
     * @param isReset
     */
    private void startAnimations(List<Animator> animations, final Runnable r, final boolean isReset) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animations);
        animatorSet.setDuration(ANIM_DURATION);
        animatorSet.setInterpolator(new DecelerateInterpolator(DECELERATION_FACTOR));
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (r != null) r.run();
                setScreenTouchable(true);
                if (isReset)
                    mSelectedCardPosition = INVALID_CARD_POSITION;
            }
        });
        animatorSet.start();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
//        if (!isScreenTouchable()) {
//            return false;
//        }

        float y = event.getRawY();
        int positionOfCardToMove = (int) v.getTag(R.id.cardstack_internal_position_tag);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mTouchFirstY != -1) {
                    return false;
                }
                mTouchPrevY = mTouchFirstY = y;
                mTouchDistance = 0;
                break;
            case MotionEvent.ACTION_MOVE:
//                if (mSelectedCardPosition == INVALID_CARD_POSITION)
//                    moveCards(positionOfCardToMove, y - mTouchFirstY);
//                mTouchDistance += Math.abs(y - mTouchPrevY);
                return false;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
//                if (mTouchDistance < dp8 && Math.abs(y - mTouchFirstY) < dp8 && mSelectedCardPosition == INVALID_CARD_POSITION) {
                if (Math.abs(v.getY() - y) < mCardGapTop * 3)
                    onClick(v);
//                } else {
//                    resetCards();
//                }
                mTouchPrevY = mTouchFirstY = -1;
                mTouchDistance = 0;
                return false;
        }
        return true;
    }

    @Override
    public void onClick(final View v) {

//        if (!isScreenTouchable()) {
//            return;
//        }
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
                        position = getSecondCardY();
                    }
                    else if (i == getCount() - 1)
                    {
                        position = getLastCardY();
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
            }, false);

        rearangeViews();
    }

    private void rearangeViews()
    {
        for (int i = getCount() - 1; i > mSelectedCardPosition; i--)
        {
            mCardViews[i].bringToFront();
        }
    }

    private void moveCards(int positionOfCardToMove, float diff) {
        if (diff < 0 || positionOfCardToMove < 0 || positionOfCardToMove >= getCount()) return;
        for (int i = positionOfCardToMove; i < getCount(); i++) {
            final View child = mCardViews[i];
            float diffCard = diff / scaleFactorForElasticEffect;
            if (mParallaxEnabled) {
                if (mParallaxScale > 0) {
                    diffCard = diffCard * (mParallaxScale / 3) * (getCount() + 1 - i);
                } else {
                    int scale = mParallaxScale * -1;
                    diffCard = diffCard * (i * (scale / 3) + 1);
                }
            } else diffCard = diffCard * (getCount() * 2 + 1);
            child.setY(getCardOriginalY() + diffCard);
        }
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
        mParallaxScale = cardStackLayout.getParallaxScale();
        mParallaxEnabled = cardStackLayout.isParallaxEnabled();
        if (mParallaxEnabled && mParallaxScale == 0)
            mParallaxEnabled = false;
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
     * Returns false if all the cards are in their initial position i.e. no card is selected
     *
     * Returns true if the {@link CardStackLayout} has a card selected and all other cards are
     * at the bottom of the screen.
     *
     * @return true if any card is selected, false otherwise
     */
    public boolean isCardSelected() {
        return mSelectedCardPosition != INVALID_CARD_POSITION;
    }

    /**
     * Returns the position of selected card. If no card
     * is selected, returns {@link #INVALID_CARD_POSITION}
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