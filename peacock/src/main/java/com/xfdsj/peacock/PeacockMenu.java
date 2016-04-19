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
import android.graphics.drawable.DrawableContainer;
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

  public int coordX;
  public int coordY;
  public int width;
  public int height;
  public boolean active;

  public static final float DEFAULT_START_ANGLE = 180.0f;

  public static final float DEFAULT_END_ANGLE = 360.0f;

  public static final int DEFAULT_RADIUS = 250;

  private Drawable menuIco;

  private ImageView menu;

  private int menuMargin =
      getResources().getDimensionPixelSize(R.dimen.peacock_menu_content_margin);
  /** The angle (in degrees, modulus 360) which the circular menu starts from */
  private float startAngle;
  /** The angle (in degrees, modulus 360) which the circular menu ends at */
  private float endAngle;
  /** Distance of menu items from mainActionView */
  private int radius;
  /** List of menu items */
  private List<PeacockMenu> subMenus;

  public MenuAnimationHandler getAnimationHandler() {
    return animationHandler;
  }

  public void setAnimationHandler(MenuAnimationHandler animationHandler) {
    this.animationHandler = animationHandler;
  }

  /** Reference to the preferred {@link MenuAnimationHandler} object */
  private MenuAnimationHandler animationHandler;
  /** Reference to a listener that listens openMenu/closeMenu actions */
  private MenuStateChangeListener stateChangeListener;

  private Status status = Status.CLOSE;

  private PeacockMenu peacockParent;

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
      setBackgroundResource(R.drawable.peacock_bg);
    }
    if (menuIco != null) {
      setMenuIco(menuIco);
    }
    setClickable(true);
    setOnClickListener(new ActionViewClickListener());
    animationHandler = new DefaultAnimationHandler(this);
    subMenus = new ArrayList<>();
  }

  @Override public void onViewAdded(View child) {
    super.onViewAdded(child);
    if (child instanceof PeacockMenu) {
      PeacockMenu button = (PeacockMenu) child;
      button.setPeacockParent(this);
      button.width = button.getSelfWidth();
      button.height = button.getSelfHeight();
      subMenus.add(button);
      removeViewInLayout(child);
    }
  }

  public int getSelfWidth() {
    int width = 0;
    if (menuIco != null) {
      if (menuIco instanceof BitmapDrawable || menuIco instanceof DrawableContainer) {
        width = menuIco.getIntrinsicWidth() + menuMargin * 2;
      }
    }
    //return Math.max(width, getBackground().getIntrinsicWidth());
    return width;
  }

  public int getSelfHeight() {
    int height = 0;
    if (menuIco != null) {
      if (menuIco instanceof BitmapDrawable || menuIco instanceof DrawableContainer) {
        height = menuIco.getIntrinsicHeight() + menuMargin * 2;
      }
    }
    //return Math.max(height, getBackground().getIntrinsicHeight());
    return height;
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
    if (Math.abs(endAngle - startAngle) >= 360 || subMenus.size() <= 1) {
      divisor = subMenus.size();
    } else {
      divisor = subMenus.size() - 1;
    }

    // Measure this path, in order to find points that have the same distance between each other
    for (int i = 0; i < subMenus.size(); i++) {
      float[] coords = new float[] { 0f, 0f };
      measure.getPosTan((i) * measure.getLength() / divisor, coords, null);
      // get the coordX and coordY values of these points and set them to each of sub action items.
      subMenus.get(i).coordX = (int) coords[0] - subMenus.get(i).width / 2;
      subMenus.get(i).coordY = (int) coords[1] - subMenus.get(i).height / 2;
    }
    return center;
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
   * @return a Point containing coordX and coordY coordinates of the top left corner of action view
   */
  private Point getActionViewCoordinates() {
    int[] coords = new int[2];
    // This method returns a coordX and coordY values that can be larger than the dimensions of the device screen.
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

  public PeacockMenu getPeacockParent() {
    return peacockParent;
  }

  public void setPeacockParent(PeacockMenu peacockParent) {
    this.peacockParent = peacockParent;
  }

  /**
   * A simple click listener used by the main action view
   */
  public class ActionViewClickListener implements OnClickListener {

    @Override public void onClick(View v) {
      active = true;
      toggle(true);
    }
  }

  /**
   * Toggles the menu
   *
   * @param animated if true, the openMenu/closeMenu action is executed by the current {@link
   * MenuAnimationHandler}
   */
  public void toggle(boolean animated) {
    switch (status) {
      case OPEN:
        closeAll(this);
        break;
      case CLOSE:
        openMenu(animated);
        break;
      case PLAYING:
        return;
    }
    if (getPeacockParent() != null) {
      getPeacockParent().closeMenu(true);
    }
  }

  public void openAll(final PeacockMenu menu) {
    menu.openMenu(true);
    menu.getAnimationHandler()
        .setAnimationEndListener(new MenuAnimationHandler.AnimationEndListener() {
          @Override public void onAnimationEnd() {
            for (final PeacockMenu subMenu : menu.getSubMenus()) {
              if (subMenu.getSubMenus().size() > 0) {
                post(new Runnable() {
                  @Override public void run() {
                    openAll(subMenu);
                  }
                });
              }
              subMenu.active = false;
            }
          }
        });
  }

  public void closeAll(PeacockMenu menu) {
    for (PeacockMenu subMenu : menu.getSubMenus()) {
      if (subMenu.getSubMenus().size() > 0) {
        closeAll(subMenu);
      }
      subMenu.active = false;
    }
    menu.closeMenu(true);
  }

  @Deprecated public void closeAllWithAnimate(final PeacockMenu menu) {
    menu.closeMenu(true);
    menu.getAnimationHandler()
        .setAnimationEndListener(new MenuAnimationHandler.AnimationEndListener() {
          @Override public void onAnimationEnd() {
            for (final PeacockMenu subMenu : menu.getSubMenus()) {
              if (subMenu.getSubMenus().size() > 0) {
                post(new Runnable() {
                  @Override public void run() {
                    closeAllWithAnimate(subMenu);
                  }
                });
              }
              subMenu.active = false;
            }
          }
        });
  }

  /**
   * Simply opens the menu by doing necessary calculations.
   *
   * @param animated if true, this action is executed by the current {@link MenuAnimationHandler}
   */
  public void openMenu(boolean animated) {
    if (subMenus.size() > 0) {
      if (animated && animationHandler != null) {
        // If animations are enabled and we have a MenuAnimationHandler, let it do the heavy work
        if (status == Status.PLAYING) {
          // Do not proceed if there is an animation currently going on.
          return;
        }
        // Get the center of the action view from the following function for efficiency
        // populate destination coordX,coordY coordinates of Items
        Point center = calculateItemPositions();

        for (int i = 0; i < subMenus.size(); i++) {
          // It is required that these Item views are not currently added to any parent
          // Because they are supposed to be added to the Activity content view,
          // just before the animation starts
          if (subMenus.get(i).getParent() != null) {
            continue;
          }
          // Initially, place all items right at the center of the main action view
          // Because they are supposed to start animating from that point.
          final LayoutParams params =
              new LayoutParams(subMenus.get(i).width, subMenus.get(i).height,
                  Gravity.TOP | Gravity.LEFT);
          params.setMargins(center.x - subMenus.get(i).width / 2,
              center.y - subMenus.get(i).height / 2, 0, 0);
          addViewToCurrentContainer(subMenus.get(i), params);
        }
        // Tell the current MenuAnimationHandler to animate from the center
        animationHandler.animateMenuOpening(center);
      } else {
        // If animations are disabled, just place each of the items to their calculated destination positions.
        for (int i = 0; i < subMenus.size(); i++) {
          if (subMenus.get(i).getParent() != null) {
            continue;
          }
          // This is currently done by giving them large margins
          final LayoutParams params =
              new LayoutParams(subMenus.get(i).width, subMenus.get(i).height,
                  Gravity.TOP | Gravity.LEFT);
          params.setMargins(subMenus.get(i).coordX, subMenus.get(i).coordY, 0, 0);
          subMenus.get(i).setLayoutParams(params);
          // Because they are placed into the main content view of the Activity,
          // which is itself a FrameLayout
          addViewToCurrentContainer(subMenus.get(i), params);
          // do not forget to specify that the menu is open.
          setStatus(Status.OPEN);
        }
      }
    }

    if (stateChangeListener != null) {
      stateChangeListener.onMenuOpened(this);
    }
  }

  /**
   * Closes the menu.
   *
   * @param animated if true, this action is executed by the current {@link MenuAnimationHandler}
   */
  public void closeMenu(boolean animated) {
    if (subMenus.size() > 0) {
      // If animations are enabled and we have a MenuAnimationHandler, let it do the heavy work
      if (animated && animationHandler != null) {
        if (status == Status.PLAYING) {
          animationHandler.setAnimationEndListener(new MenuAnimationHandler.AnimationEndListener() {
            @Override public void onAnimationEnd() {
              closeAll(PeacockMenu.this);
            }
          });
          return;
        }
        animationHandler.animateMenuClosing(getActionViewCenter());
      } else {
        // If animations are disabled, just detach each of the Item views from the Activity content view.
        for (int i = 0; i < subMenus.size(); i++) {
          if (subMenus.get(i).active) {
            continue;
          }
          removeViewFromCurrentContainer(subMenus.get(i));
        }
        // do not forget to specify that the menu is now closed.
        setStatus(Status.CLOSE);
      }
    }

    if (stateChangeListener != null) {
      stateChangeListener.onMenuClosed(this);
    }
  }

  public List<PeacockMenu> getSubMenus() {
    return subMenus;
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

  public void setStatus(Status status) {
    this.status = status;
    if (status == Status.CLOSE) {
      for (PeacockMenu subMenu : subMenus) {
        if (subMenu.active) {
          this.status = Status.OPEN;
          break;
        }
      }
    }
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

  public enum Status {
    OPEN,
    CLOSE,
    PLAYING;
  }
}
