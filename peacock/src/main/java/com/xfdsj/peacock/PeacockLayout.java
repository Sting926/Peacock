package com.xfdsj.peacock;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

/**
 * A Layout that arranges its children around its center. The arc can be set by
 * calling {@link #setAngle(float, float) setAngle()}. You can override the method
 * {@link #onMeasure(int, int) onMeasure()}, otherwise it is always
 * WRAP_CONTENT.
 *
 * @author Capricorn
 */
public class PeacockLayout extends ViewGroup {

  private ImageView mMenu;

  private Drawable mMenuIco;

  private int mMenuSize;

  private int mSubMenuSize;

  private int mChildPadding = 5;

  private int mLayoutPadding = 10;

  public static final float DEFAULT_START_ANGLE = 270.0f;

  public static final float DEFAULT_END_ANGLE = 360.0f;

  private float mStartAngle = DEFAULT_START_ANGLE;

  private float mEndAngle = DEFAULT_END_ANGLE;

  /* the distance between the layout's center and any child's center */
  private int mRadius;

  private int mMinRadius;

  private boolean mExpanded = false;

  private OnClickListener itemListener;

  public PeacockLayout(Context context) {
    super(context);
  }

  public PeacockLayout(Context context, AttributeSet attrs) {
    super(context, attrs);

    if (attrs != null) {
      TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Peacock, 0, 0);
      mStartAngle = a.getFloat(R.styleable.Peacock_startAngle, DEFAULT_START_ANGLE);
      mEndAngle = a.getFloat(R.styleable.Peacock_endAngle, DEFAULT_END_ANGLE);
      mMenuIco = a.getDrawable(R.styleable.Peacock_menuIco);
      a.recycle();
    }

    mMenu = new ImageView(context);

    if (mMenuIco != null) {
      mMenu.setImageDrawable(mMenuIco);
    } else {
      mMenu.setImageResource(R.drawable.peacock_bg);
    }

    if (mMenu.getDrawable() instanceof BitmapDrawable|| mMenu.getDrawable() instanceof DrawableContainer) {
      mMenuSize = mMenu.getDrawable().getIntrinsicWidth();
    } else {
      mMenuSize = getResources().getDimensionPixelSize(R.dimen.peacock_menu_size);
    }

    addView(mMenu);

    mSubMenuSize = (int) (mMenuSize * 0.618);

    mMinRadius = mMenuSize / 2 + mSubMenuSize;

    mMenu.setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        mMenu.startAnimation(createHintSwitchAnimation(isExpanded()));
        switchState(true);
      }
    });
  }

  private static int computeRadius(final float arcDegrees, final int childCount,
      final int childSize, final int childPadding, final int minRadius) {
    if (childCount < 2) {
      return minRadius;
    }

    final float perDegrees = arcDegrees / (childCount - 1);
    final float perHalfDegrees = perDegrees / 2;
    final int perSize = childSize + childPadding;

    final int radius = (int) ((perSize / 2) / Math.sin(Math.toRadians(perHalfDegrees)));

    return Math.max(radius, minRadius);
  }

  private static Rect computeChildFrame(final int centerX, final int centerY, final int radius,
      final float degrees, final int size) {

    final double childCenterX = centerX + radius * Math.cos(Math.toRadians(degrees));
    final double childCenterY = centerY + radius * Math.sin(Math.toRadians(degrees));

    return new Rect((int) (childCenterX - size / 2), (int) (childCenterY - size / 2),
        (int) (childCenterX + size / 2), (int) (childCenterY + size / 2));
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int radius = mRadius =
        computeRadius(Math.abs(mEndAngle - mStartAngle), getChildCount() - 1, mSubMenuSize,
            mChildPadding, mMinRadius);
    final int size = radius * 2 + mSubMenuSize + mChildPadding + mLayoutPadding * 2;

    setMeasuredDimension(size, size);

/*    final int count = getChildCount();
    for (int i = 1; i < count; i++) {
      getChildAt(i).measure(MeasureSpec.makeMeasureSpec(mSubMenuSize, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(mSubMenuSize, MeasureSpec.EXACTLY));
    }
    getChildAt(0).measure(MeasureSpec.makeMeasureSpec(mSubMenuSize * 2, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(mSubMenuSize * 2, MeasureSpec.EXACTLY));*/
  }

  @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int centerX = getWidth() / 2;
    final int centerY = getHeight() / 2;
    final int radius = mExpanded ? mRadius : 0;

    final int childCount = getChildCount() - 1;
    final float perDegrees = (mEndAngle - mStartAngle) / (childCount - 1);

    float degrees = mStartAngle;
    for (int i = 0; i < childCount; i++) {
      Rect frame = computeChildFrame(centerX, centerY, radius, degrees, mSubMenuSize);
      degrees += perDegrees;
      getChildAt(i).layout(frame.left, frame.top, frame.right, frame.bottom);
    }
    Rect frame = computeChildFrame(centerX, centerY, 0, 0, mMenuSize);
    mMenu.layout(frame.left, frame.top, frame.right, frame.bottom);
    mMenu.bringToFront();
  }

  private static long computeStartOffset(final int childCount, final boolean expanded,
      final int index, final float delayPercent, final long duration, Interpolator interpolator) {
    final float delay = delayPercent * duration;
    final long viewDelay = (long) (getTransformedIndex(expanded, childCount, index) * delay);
    final float totalDelay = delay * childCount;

    float normalizedDelay = viewDelay / totalDelay;
    normalizedDelay = interpolator.getInterpolation(normalizedDelay);

    return (long) (normalizedDelay * totalDelay);
  }

  private static int getTransformedIndex(final boolean expanded, final int count, final int index) {
    if (expanded) {
      return count - 1 - index;
    }

    return index;
  }

  private static Animation createExpandAnimation(float fromXDelta, float toXDelta, float fromYDelta,
      float toYDelta, long startOffset, long duration, Interpolator interpolator) {
    Animation animation = new RotateAndTranslateAnimation(0, toXDelta, 0, toYDelta, 0, 720);
    animation.setStartOffset(startOffset);
    animation.setDuration(duration);
    animation.setInterpolator(interpolator);
    animation.setFillAfter(true);

    return animation;
  }

  private static Animation createShrinkAnimation(float fromXDelta, float toXDelta, float fromYDelta,
      float toYDelta, long startOffset, long duration, Interpolator interpolator) {
    AnimationSet animationSet = new AnimationSet(false);
    animationSet.setFillAfter(true);

    final long preDuration = duration / 2;
    Animation rotateAnimation =
        new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
            0.5f);
    rotateAnimation.setStartOffset(startOffset);
    rotateAnimation.setDuration(preDuration);
    rotateAnimation.setInterpolator(new LinearInterpolator());
    rotateAnimation.setFillAfter(true);

    animationSet.addAnimation(rotateAnimation);

    Animation translateAnimation =
        new RotateAndTranslateAnimation(0, toXDelta, 0, toYDelta, 360, 720);
    translateAnimation.setStartOffset(startOffset + preDuration);
    translateAnimation.setDuration(duration - preDuration);
    translateAnimation.setInterpolator(interpolator);
    translateAnimation.setFillAfter(true);

    animationSet.addAnimation(translateAnimation);

    return animationSet;
  }

  private void bindChildAnimation(final View child, final int index, final long duration) {
    final boolean expanded = mExpanded;
    final int centerX = getWidth() / 2;
    final int centerY = getHeight() / 2;
    final int radius = expanded ? 0 : mRadius;

    final int childCount = getChildCount() - 1;
    final float perDegrees = (mEndAngle - mStartAngle) / (childCount - 1);
    Rect frame =
        computeChildFrame(centerX, centerY, radius, mStartAngle + index * perDegrees, mSubMenuSize);

    final int toXDelta = frame.left - child.getLeft();
    final int toYDelta = frame.top - child.getTop();

    Interpolator interpolator =
        mExpanded ? new AccelerateInterpolator() : new OvershootInterpolator(1.5f);
    final long startOffset =
        computeStartOffset(childCount, mExpanded, index, 0.1f, duration, interpolator);

    Animation animation =
        mExpanded ? createShrinkAnimation(0, toXDelta, 0, toYDelta, startOffset, duration,
            interpolator)
            : createExpandAnimation(0, toXDelta, 0, toYDelta, startOffset, duration, interpolator);

    final boolean isLast = getTransformedIndex(expanded, childCount, index) == childCount - 1;
    animation.setAnimationListener(new AnimationListener() {

      @Override public void onAnimationStart(Animation animation) {

      }

      @Override public void onAnimationRepeat(Animation animation) {

      }

      @Override public void onAnimationEnd(Animation animation) {
        if (isLast) {
          postDelayed(new Runnable() {

            @Override public void run() {
              onAllAnimationsEnd();
            }
          }, 0);
        }
      }
    });

    child.setAnimation(animation);
  }

  public boolean isExpanded() {
    return mExpanded;
  }

  public void setAngle(float startAngle, float endAngle) {
    if (mStartAngle == startAngle && mEndAngle == endAngle) {
      return;
    }

    mStartAngle = startAngle;
    mEndAngle = endAngle;

    requestLayout();
  }

  /**
   * switch between expansion and shrinkage
   */
  public void switchState(final boolean showAnimation) {
    if (showAnimation) {
      final int childCount = getChildCount() - 1;
      for (int i = 0; i < childCount; i++) {
        View item = getChildAt(i);
        item.setOnClickListener(listener);
        bindChildAnimation(item, i, 300);
      }
    }

    mExpanded = !mExpanded;

    if (!showAnimation) {
      requestLayout();
    }

    invalidate();
  }

  private void onAllAnimationsEnd() {
    final int childCount = getChildCount() - 1;
    for (int i = 0; i < childCount; i++) {
      getChildAt(i).clearAnimation();
    }
    requestLayout();
  }

  private OnClickListener listener = new OnClickListener() {

    @Override public void onClick(final View viewClicked) {
      Animation animation = bindItemAnimation(viewClicked, true, 400);
      animation.setAnimationListener(new AnimationListener() {

        @Override public void onAnimationStart(Animation animation) {

        }

        @Override public void onAnimationRepeat(Animation animation) {

        }

        @Override public void onAnimationEnd(Animation animation) {
          postDelayed(new Runnable() {

            @Override public void run() {
              itemDidDisappear();
            }
          }, 0);
        }
      });

      final int itemCount = getChildCount() - 1;
      for (int i = 0; i < itemCount; i++) {
        View item = getChildAt(i);
        if (viewClicked != item) {
          bindItemAnimation(item, false, 300);
        }
      }

      invalidate();
      mMenu.startAnimation(createHintSwitchAnimation(true));

      if (itemListener != null) {
        itemListener.onClick(viewClicked);
      }
    }
  };

  private Animation bindItemAnimation(final View child, final boolean isClicked,
      final long duration) {
    Animation animation = createItemDisappearAnimation(duration, isClicked);
    child.setAnimation(animation);

    return animation;
  }

  private void itemDidDisappear() {
    final int itemCount = getChildCount() - 1;
    for (int i = 0; i < itemCount; i++) {
      View item = getChildAt(i);
      item.clearAnimation();
    }

    switchState(false);
  }

  private static Animation createItemDisappearAnimation(final long duration,
      final boolean isClicked) {
    AnimationSet animationSet = new AnimationSet(true);
    animationSet.addAnimation(
        new ScaleAnimation(1.0f, isClicked ? 2.0f : 0.0f, 1.0f, isClicked ? 2.0f : 0.0f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
    animationSet.addAnimation(new AlphaAnimation(1.0f, 0.0f));

    animationSet.setDuration(duration);
    animationSet.setInterpolator(new DecelerateInterpolator());
    animationSet.setFillAfter(true);

    return animationSet;
  }

  private static Animation createHintSwitchAnimation(final boolean expanded) {
    Animation animation =
        new RotateAnimation(expanded ? 45 : 0, expanded ? 0 : 45, Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f);
    animation.setStartOffset(0);
    animation.setDuration(100);
    animation.setInterpolator(new DecelerateInterpolator());
    animation.setFillAfter(true);

    return animation;
  }

  public void setItemListener(OnClickListener itemListener) {
    this.itemListener = itemListener;
  }
}
