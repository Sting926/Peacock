package com.xfdsj.peacock;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
public class FloatingActionButton extends FrameLayout {

  public static final int THEME_LIGHT = 0;
  public static final int THEME_DARK = 1;

  public static final int POSITION_TOP_CENTER = 1;
  public static final int POSITION_TOP_RIGHT = 2;
  public static final int POSITION_RIGHT_CENTER = 3;
  public static final int POSITION_BOTTOM_RIGHT = 4;
  public static final int POSITION_BOTTOM_CENTER = 5;
  public static final int POSITION_BOTTOM_LEFT = 6;
  public static final int POSITION_LEFT_CENTER = 7;
  public static final int POSITION_TOP_LEFT = 8;

  public static final float DEFAULT_START_ANGLE = 180.0f;

  public static final float DEFAULT_END_ANGLE = 360.0f;

  public static final int DEFAULT_RADIUS = 250;

  private View contentView;

  private Drawable menuIco;

  private ImageView menu;
  /** The angle (in degrees, modulus 360) which the circular menu starts from */
  private float startAngle;
  /** The angle (in degrees, modulus 360) which the circular menu ends at */
  private float endAngle;
  /** Distance of menu items from mainActionView */
  private int radius;
  /** List of menu items */
  private List<Item> subActionItems;
  /** Reference to the preferred {@link MenuAnimationHandler} object */
  private MenuAnimationHandler animationHandler;
  /** whether the menu is currently open or not */
  private boolean open;

  public FloatingActionButton(Context context) {
    super(context);
  }

  public FloatingActionButton(Context context, AttributeSet attrs) {
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
    subActionItems = new ArrayList<>();
  }

  /**
   * Constructor that takes parameters collected using {@link FloatingActionMenu.Builder}
   *
   * @param context a reference to the current context
   */
  public FloatingActionButton(Context context, ViewGroup.LayoutParams layoutParams, int theme,
      Drawable backgroundDrawable, int position, View contentView, LayoutParams contentParams,
      boolean systemOverlay) {
    super(context);

    if (!systemOverlay && !(context instanceof Activity)) {
      throw new RuntimeException("Given context must be an instance of Activity, "
          + "since this FAB is not a systemOverlay.");
    }

    setPosition(position, layoutParams);

    // If no custom backgroundDrawable is specified, use the background drawable of the theme.
    if (backgroundDrawable == null) {
      if (theme == THEME_LIGHT) {
        backgroundDrawable = context.getResources().getDrawable(R.drawable.button_action_selector);
      } else {
        backgroundDrawable =
            context.getResources().getDrawable(R.drawable.button_action_dark_selector);
      }
    }
    setBackgroundResource(backgroundDrawable);
    if (contentView != null) {
      setContentView(contentView, contentParams);
    }
    setClickable(true);

    attach(layoutParams);
  }

  @Override public void onViewAdded(View child) {
    super.onViewAdded(child);
    if (child instanceof FloatingActionButton) {
      FloatingActionButton button = (FloatingActionButton) child;
      int width = button.getSelfWidth();
      int height = button.getSelfHeight();
      subActionItems.add(new Item(child, width, height));
      removeViewInLayout(child);
    }
  }

  public int getSelfWidth() {
    if (getBackground() instanceof BitmapDrawable) {
      return getBackground().getIntrinsicWidth();
    } else if (menu != null && menu.getDrawable() instanceof BitmapDrawable) {
      int margin = getResources().getDimensionPixelSize(R.dimen.action_button_content_margin);
      return menu.getDrawable().getIntrinsicWidth() + margin * 2;
    }
    return 0;
  }

  public int getSelfHeight() {
    if (getBackground() instanceof BitmapDrawable) {
      return getBackground().getIntrinsicHeight();
    } else if (menu != null && menu.getDrawable() instanceof BitmapDrawable) {
      int margin = getResources().getDimensionPixelSize(R.dimen.action_button_content_margin);
      return menu.getDrawable().getIntrinsicHeight() + margin * 2;
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

    Path orbit = new Path();
    orbit.addArc(area, startAngle, endAngle - startAngle);

    PathMeasure measure = new PathMeasure(orbit, false);

    // Prevent overlapping when it is a full circle
    int divisor;
    if (Math.abs(endAngle - startAngle) >= 360 || subActionItems.size() <= 1) {
      divisor = subActionItems.size();
    } else {
      divisor = subActionItems.size() - 1;
    }

    // Measure this path, in order to find points that have the same distance between each other
    for (int i = 0; i < subActionItems.size(); i++) {
      float[] coords = new float[] { 0f, 0f };
      measure.getPosTan((i) * measure.getLength() / divisor, coords, null);
      // get the x and y values of these points and set them to each of sub action items.
      subActionItems.get(i).x = (int) coords[0] - subActionItems.get(i).width / 2;
      subActionItems.get(i).y = (int) coords[1] - subActionItems.get(i).height / 2;
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

      for (int i = 0; i < subActionItems.size(); i++) {
        // It is required that these Item views are not currently added to any parent
        // Because they are supposed to be added to the Activity content view,
        // just before the animation starts
        if (subActionItems.get(i).view.getParent() != null) {
          throw new RuntimeException(
              "All of the sub action items have to be independent from a parent.");
        }

        // Initially, place all items right at the center of the main action view
        // Because they are supposed to start animating from that point.
        final LayoutParams params =
            new LayoutParams(subActionItems.get(i).width, subActionItems.get(i).height,
                Gravity.TOP | Gravity.LEFT);
        params.setMargins(center.x - subActionItems.get(i).width / 2,
            center.y - subActionItems.get(i).height / 2, 0, 0);
        addViewToCurrentContainer(subActionItems.get(i).view, params);
      }
      // Tell the current MenuAnimationHandler to animate from the center
      animationHandler.animateMenuOpening(center);
    } else {
      // If animations are disabled, just place each of the items to their calculated destination positions.
      for (int i = 0; i < subActionItems.size(); i++) {
        // This is currently done by giving them large margins

        final LayoutParams params =
            new LayoutParams(subActionItems.get(i).width, subActionItems.get(i).height,
                Gravity.TOP | Gravity.LEFT);
        params.setMargins(subActionItems.get(i).x, subActionItems.get(i).y, 0, 0);
        subActionItems.get(i).view.setLayoutParams(params);
        // Because they are placed into the main content view of the Activity,
        // which is itself a FrameLayout

        addViewToCurrentContainer(subActionItems.get(i).view, params);
      }
    }
    // do not forget to specify that the menu is open.
    open = true;

/*    if(stateChangeListener != null) {
      stateChangeListener.onMenuOpened(this);
    }*/

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
      for (int i = 0; i < subActionItems.size(); i++) {
        removeViewFromCurrentContainer(subActionItems.get(i).view);
      }
    }
    // do not forget to specify that the menu is now closed.
    open = false;

/*    if(stateChangeListener != null) {
      stateChangeListener.onMenuClosed(this);
    }*/
  }

  public List<Item> getSubActionItems() {
    return subActionItems;
  }

  public Drawable getMenuIco() {
    return menuIco;
  }

  public void setMenuIco(Drawable menuIco) {
    if (menu == null) {
      menu = new ImageView(getContext());
      LayoutParams params =
          new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
      int margin = getResources().getDimensionPixelSize(R.dimen.action_button_content_margin);
      params.setMargins(margin, margin, margin, margin);
      this.addView(menu, params);
    }
    menu.setImageDrawable(menuIco);
    this.menuIco = menuIco;
  }

  /**
   * Sets the position of the button by calculating its Gravity from the position parameter
   *
   * @param position one of 8 specified positions.
   * @param layoutParams should be either FrameLayout.LayoutParams or WindowManager.LayoutParams
   */
  public void setPosition(int position, ViewGroup.LayoutParams layoutParams) {

    boolean setDefaultMargin = false;

    int gravity;
    switch (position) {
      case POSITION_TOP_CENTER:
        gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        break;
      case POSITION_TOP_RIGHT:
        gravity = Gravity.TOP | Gravity.RIGHT;
        break;
      case POSITION_RIGHT_CENTER:
        gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        break;
      case POSITION_BOTTOM_CENTER:
        gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        break;
      case POSITION_BOTTOM_LEFT:
        gravity = Gravity.BOTTOM | Gravity.LEFT;
        break;
      case POSITION_LEFT_CENTER:
        gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        break;
      case POSITION_TOP_LEFT:
        gravity = Gravity.TOP | Gravity.LEFT;
        break;
      case POSITION_BOTTOM_RIGHT:
      default:
        setDefaultMargin = true;
        gravity = Gravity.BOTTOM | Gravity.RIGHT;
        break;
    }
    try {
      LayoutParams lp = (LayoutParams) layoutParams;
      lp.gravity = gravity;
      setLayoutParams(lp);
    } catch (ClassCastException e) {
      throw new ClassCastException("layoutParams must be an instance of "
          + "FrameLayout.LayoutParams, since this FAB is not a systemOverlay");
    }
  }

  /**
   * Sets a content view that will be displayed inside this FloatingActionButton.
   */
  public void setContentView(View contentView, LayoutParams contentParams) {
    this.contentView = contentView;
    LayoutParams params;
    if (contentParams == null) {
      params =
          new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
      final int margin = getResources().getDimensionPixelSize(R.dimen.action_button_content_margin);
      params.setMargins(margin, margin, margin, margin);
    } else {
      params = contentParams;
    }
    params.gravity = Gravity.CENTER;

    contentView.setClickable(false);
    this.addView(contentView, params);
  }

  /**
   * Attaches it to the content view with specified LayoutParams.
   */
  public void attach(ViewGroup.LayoutParams layoutParams) {
    ((ViewGroup) getActivityContentView()).addView(this, layoutParams);
  }

  /**
   * Detaches it from the container view.
   */
  public void detach() {
    ((ViewGroup) getActivityContentView()).removeView(this);
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

  private void setBackgroundResource(Drawable drawable) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      setBackground(drawable);
    } else {
      setBackgroundDrawable(drawable);
    }
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

  /**
   * A builder for {@link FloatingActionButton} in conventional Java Builder format
   */
  public static class Builder {

    private Context context;
    private ViewGroup.LayoutParams layoutParams;
    private int theme;
    private Drawable backgroundDrawable;
    private int position;
    private View contentView;
    private LayoutParams contentParams;
    private boolean systemOverlay;

    public Builder(Context context) {
      this.context = context;

      // Default FloatingActionButton settings
      int size = context.getResources().getDimensionPixelSize(R.dimen.action_button_size);
      int margin = context.getResources().getDimensionPixelSize(R.dimen.action_button_margin);
      LayoutParams layoutParams = new LayoutParams(size, size, Gravity.BOTTOM | Gravity.RIGHT);
      layoutParams.setMargins(margin, margin, margin, margin);
      setLayoutParams(layoutParams);
      setTheme(FloatingActionButton.THEME_LIGHT);
      setPosition(FloatingActionButton.POSITION_BOTTOM_RIGHT);
      setSystemOverlay(false);
    }

    public Builder setLayoutParams(ViewGroup.LayoutParams params) {
      this.layoutParams = params;
      return this;
    }

    public Builder setTheme(int theme) {
      this.theme = theme;
      return this;
    }

    public Builder setBackgroundDrawable(Drawable backgroundDrawable) {
      this.backgroundDrawable = backgroundDrawable;
      return this;
    }

    public Builder setBackgroundDrawable(int drawableId) {
      return setBackgroundDrawable(context.getResources().getDrawable(drawableId));
    }

    public Builder setPosition(int position) {
      this.position = position;
      return this;
    }

    public Builder setContentView(View contentView) {
      return setContentView(contentView, null);
    }

    public Builder setContentView(View contentView, LayoutParams contentParams) {
      this.contentView = contentView;
      this.contentParams = contentParams;
      return this;
    }

    public Builder setSystemOverlay(boolean systemOverlay) {
      this.systemOverlay = systemOverlay;
      return this;
    }

    public FloatingActionButton build() {
      return new FloatingActionButton(context, layoutParams, theme, backgroundDrawable, position,
          contentView, contentParams, systemOverlay);
    }

    public static WindowManager.LayoutParams getDefaultSystemWindowParams(Context context) {
      int size = context.getResources().getDimensionPixelSize(R.dimen.action_button_size);
      WindowManager.LayoutParams params =
          new WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
              // z-ordering
              WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                  | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
      params.format = PixelFormat.RGBA_8888;
      params.gravity = Gravity.TOP | Gravity.LEFT;
      return params;
    }
  }
}
