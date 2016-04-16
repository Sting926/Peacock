package com.xfdsj.peacock;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.xfdsj.peacock.animation.DefaultAnimationHandler;
import com.xfdsj.peacock.animation.MenuAnimationHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * An alternative Floating Action Button implementation that can be independently placed in
 * one of 8 different places on the screen.
 */
public class PeacockMenu extends FrameLayout {

  public static final float DEFAULT_START_ANGLE = 180.0f;

  public static final float DEFAULT_END_ANGLE = 360.0f;

  public static final int DEFAULT_RADIUS = 250;

  private Drawable menuIco;

  private ImageView menu;

  private int menuMargin =
      getResources().getDimensionPixelSize(R.dimen.action_button_content_margin);
  /** The angle (in degrees, modulus 360) which the circular menu starts from */
  private float startAngle;
  /** The angle (in degrees, modulus 360) which the circular menu ends at */
  private float endAngle;
  /** Distance of menu items from mainActionView */
  private int radius;
  /** List of menu items */
  private List<Item> subMenuItems;
  /** Reference to the preferred {@link MenuAnimationHandler} object */
  private MenuAnimationHandler animationHandler;
  /** Reference to a listener that listens open/close actions */
  private MenuStateChangeListener stateChangeListener;
  /** whether the menu is currently open or not */
  private boolean open;

  public PeacockMenu(Context context) {
    super(context);
  }

  public PeacockMenu(Context context, AttributeSet attrs) {
    super(context, attrs);
    if (attrs != null) {
      TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Peacock, 0, 0);
      startAngle = a.getFloat(R.styleable.Peacock_startAngle, DEFAULT_START_ANGLE);
      endAngle = a.getFloat(R.styleable.Peacock_endAngle, DEFAULT_END_ANGLE);
      radius = a.getDimensionPixelSize(R.styleable.Peacock_radius, DEFAULT_RADIUS);
      menuIco = a.getDrawable(R.styleable.Peacock_menuIco);
      a.recycle();
    }
    if (getBackground() == null) {
      setBackgroundResource(R.drawable.peacock_background);
    }
    if (menuIco != null) {
      setMenuIco(menuIco);
    }
    setClickable(true);
    setOnClickListener(new ActionViewClickListener());
    animationHandler = new DefaultAnimationHandler();
    animationHandler.setMenu(this);
    subMenuItems = new ArrayList<>();
  }

  @Override public void onViewAdded(View child) {
    super.onViewAdded(child);
    if (child instanceof PeacockMenu) {
      PeacockMenu button = (PeacockMenu) child;
      int width = button.getSelfWidth();
      int height = button.getSelfHeight();
      subMenuItems.add(new Item(child, width, height));
      removeViewInLayout(child);
    }
  }

  public int getSelfWidth() {
    if (getBackground() instanceof BitmapDrawable) {
      return getBackground().getIntrinsicWidth();
    } else if (menu != null && menu.getDrawable() instanceof BitmapDrawable) {
      return menu.getDrawable().getIntrinsicWidth() + menuMargin * 2;
    }
    return 0;
  }

  public int getSelfHeight() {
    if (getBackground() instanceof BitmapDrawable) {
      return getBackground().getIntrinsicHeight();
    } else if (menu != null && menu.getDrawable() instanceof BitmapDrawable) {
      return menu.getDrawable().getIntrinsicHeight() + menuMargin * 2;
    }
    return 0;
  }

  private void addViewToCurrentContainer(View view, ViewGroup.LayoutParams layoutParams) {
    try {
      if (layoutParams != null) {
        LayoutParams lp = (LayoutParams) layoutParams;
        ((ViewGroup) getActivityContentView()).addView(view, lp);
      } else {
        ((ViewGroup) getActivityContentView()).addView(view);
      }
    } catch (ClassCastException e) {
      throw new ClassCastException(
          "layoutParams must be an instance of " + "FrameLayout.LayoutParams.");
    }
  }

  public void removeViewFromCurrentContainer(View view) {
    ((ViewGroup) getActivityContentView()).removeView(view);
  }

  /**
   * Calculates the desired positions of all items.
   *
   * @return getActionViewCenter()
   */
  private Point calculateItemPositions() {
    // Create an arc that starts from startAngle and ends at endAngle
    // in an area that is as large as 4*radius^2
    final Point center = getActionViewCenter();
    RectF area =
        new RectF(center.x - radius, center.y - radius, center.x + radius, center.y + radius);

    if (startAngle == endAngle) {
      endAngle += 360.0f;
    }
    Path orbit = new Path();
    orbit.addArc(area, startAngle, endAngle - startAngle);

    PathMeasure measure = new PathMeasure(orbit, false);

    // Prevent overlapping when it is a full circle
    int divisor;
    if (Math.abs(endAngle - startAngle) >= 360 || subMenuItems.size() <= 1) {
      divisor = subMenuItems.size();
    } else {
      divisor = subMenuItems.size() - 1;
    }

    // Measure this path, in order to find points that have the same distance between each other
    for (int i = 0; i < subMenuItems.size(); i++) {
      float[] coords = new float[] { 0f, 0f };
      measure.getPosTan((i) * measure.getLength() / divisor, coords, null);
      // get the x and y values of these points and set them to each of sub action items.
      subMenuItems.get(i).x = (int) coords[0] - subMenuItems.get(i).width / 2;
      subMenuItems.get(i).y = (int) coords[1] - subMenuItems.get(i).height / 2;
    }
    return center;
  }

  public int getStatusBarHeight() {
    int result = 0;
    int resourceId =
        getContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) {
      result = getContext().getResources().getDimensionPixelSize(resourceId);
    }
    return result;
  }

  /**
   * Retrieves the screen size from the Activity context
   *
   * @return the screen size as a Point object
   */
  private Point getScreenSize() {
    Point size = new Point();
    getWindowManager().getDefaultDisplay().getSize(size);
    return size;
  }

  /**
   * Gets the coordinates of the main action view
   * This method should only be called after the main layout of the Activity is drawn,
   * such as when a user clicks the action button.
   *
   * @return a Point containing x and y coordinates of the top left corner of action view
   */
  private Point getActionViewCoordinates() {
    int[] coords = new int[2];
    // This method returns a x and y values that can be larger than the dimensions of the device screen.
    getLocationOnScreen(coords);

    // So, we need to deduce the offsets.
    Rect activityFrame = new Rect();
    getActivityContentView().getWindowVisibleDisplayFrame(activityFrame);
    coords[0] -= (getScreenSize().x - getActivityContentView().getMeasuredWidth());
    coords[1] -=
        (activityFrame.height() + activityFrame.top - getActivityContentView().getMeasuredHeight());

    return new Point(coords[0], coords[1]);
  }

  /**
   * Returns the center point of the main action view
   *
   * @return the action view center point
   */
  public Point getActionViewCenter() {
    Point point = getActionViewCoordinates();
    point.x += getMeasuredWidth() / 2;
    point.y += getMeasuredHeight() / 2;
    return point;
  }

  /**
   * A simple click listener used by the main action view
   */
  public class ActionViewClickListener implements OnClickListener {

    @Override public void onClick(View v) {
      toggle(true);
    }
  }

  /**
   * Toggles the menu
   *
   * @param animated if true, the open/close action is executed by the current {@link
   * MenuAnimationHandler}
   */
  public void toggle(boolean animated) {
    if (open) {
      close(animated);
    } else {
      open(animated);
    }
  }

  /**
   * Simply opens the menu by doing necessary calculations.
   *
   * @param animated if true, this action is executed by the current {@link MenuAnimationHandler}
   */
  public void open(boolean animated) {

    // Get the center of the action view from the following function for efficiency
    // populate destination x,y coordinates of Items
    Point center = calculateItemPositions();

    if (animated && animationHandler != null) {
      // If animations are enabled and we have a MenuAnimationHandler, let it do the heavy work
      if (animationHandler.isAnimating()) {
        // Do not proceed if there is an animation currently going on.
        return;
      }

      for (int i = 0; i < subMenuItems.size(); i++) {
        // It is required that these Item views are not currently added to any parent
        // Because they are supposed to be added to the Activity content view,
        // just before the animation starts
        if (subMenuItems.get(i).view.getParent() != null) {
          throw new RuntimeException(
              "All of the sub action items have to be independent from a parent.");
        }

        // Initially, place all items right at the center of the main action view
        // Because they are supposed to start animating from that point.
        final LayoutParams params =
            new LayoutParams(subMenuItems.get(i).width, subMenuItems.get(i).height,
                Gravity.TOP | Gravity.LEFT);
        params.setMargins(center.x - subMenuItems.get(i).width / 2,
            center.y - subMenuItems.get(i).height / 2, 0, 0);
        addViewToCurrentContainer(subMenuItems.get(i).view, params);
      }
      // Tell the current MenuAnimationHandler to animate from the center
      animationHandler.animateMenuOpening(center);
    } else {
      // If animations are disabled, just place each of the items to their calculated destination positions.
      for (int i = 0; i < subMenuItems.size(); i++) {
        // This is currently done by giving them large margins

        final LayoutParams params =
            new LayoutParams(subMenuItems.get(i).width, subMenuItems.get(i).height,
                Gravity.TOP | Gravity.LEFT);
        params.setMargins(subMenuItems.get(i).x, subMenuItems.get(i).y, 0, 0);
        subMenuItems.get(i).view.setLayoutParams(params);
        // Because they are placed into the main content view of the Activity,
        // which is itself a FrameLayout

        addViewToCurrentContainer(subMenuItems.get(i).view, params);
      }
    }
    // do not forget to specify that the menu is open.
    open = true;

    if (stateChangeListener != null) {
      stateChangeListener.onMenuOpened(this);
    }
  }

  /**
   * Closes the menu.
   *
   * @param animated if true, this action is executed by the current {@link MenuAnimationHandler}
   */
  public void close(boolean animated) {
    // If animations are enabled and we have a MenuAnimationHandler, let it do the heavy work
    if (animated && animationHandler != null) {
      if (animationHandler.isAnimating()) {
        // Do not proceed if there is an animation currently going on.
        return;
      }
      animationHandler.animateMenuClosing(getActionViewCenter());
    } else {
      // If animations are disabled, just detach each of the Item views from the Activity content view.
      for (int i = 0; i < subMenuItems.size(); i++) {
        removeViewFromCurrentContainer(subMenuItems.get(i).view);
      }
    }
    // do not forget to specify that the menu is now closed.
    open = false;

    if (stateChangeListener != null) {
      stateChangeListener.onMenuClosed(this);
    }
  }

  public List<Item> getSubMenuItems() {
    return subMenuItems;
  }

  public Drawable getMenuIco() {
    return menuIco;
  }

  public void setMenuIco(Drawable menuIco) {
    if (menu == null) {
      menu = new ImageView(getContext());
      LayoutParams params =
          new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
      params.setMargins(menuMargin, menuMargin, menuMargin, menuMargin);
      this.addView(menu, params);
    }
    menu.setImageDrawable(menuIco);
    this.menuIco = menuIco;
  }

  /**
   * Finds and returns the main content view from the Activity context.
   *
   * @return the main content view
   */
  public View getActivityContentView() {
    try {
      return ((Activity) getContext()).getWindow()
          .getDecorView()
          .findViewById(android.R.id.content);
    } catch (ClassCastException e) {
      throw new ClassCastException(
          "Please provide an Activity context for this FloatingActionButton.");
    }
  }

  public WindowManager getWindowManager() {
    return (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
  }

  public void setStateChangeListener(MenuStateChangeListener listener) {
    this.stateChangeListener = listener;
  }

  /**
   * A listener to listen open/closed state changes of the Menu
   */
  public static interface MenuStateChangeListener {
    public void onMenuOpened(PeacockMenu menu);

    public void onMenuClosed(PeacockMenu menu);
  }

  public static class Item {
    public int x;
    public int y;
    public int width;
    public int height;

    public float alpha;

    public View view;

    public Item(View view, int width, int height) {
      this.view = view;
      this.width = width;
      this.height = height;
      alpha = view.getAlpha();
      x = 0;
      y = 0;
    }
  }
}
