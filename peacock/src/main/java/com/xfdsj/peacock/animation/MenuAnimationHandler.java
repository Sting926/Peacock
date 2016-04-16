/*
 *   Copyright 2014 Oguz Bilgener
 */
package com.xfdsj.peacock.animation;

import android.animation.Animator;
import android.graphics.Point;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.xfdsj.peacock.PeacockMenu;

/**
 * An abstract class that is a prototype for the actual animation handlers
 */
public abstract class MenuAnimationHandler {

  // There are only two distinct animations at the moment.
  protected enum ActionType {
    OPENING, CLOSING
  }

  protected PeacockMenu menu;

  public MenuAnimationHandler() {
  }

  public void setMenu(PeacockMenu menu) {
    this.menu = menu;
  }

  /**
   * Starts the opening animation
   * Should be overriden by children
   */
  public void animateMenuOpening(Point center) {
    if (menu == null) {
      throw new NullPointerException(
          "MenuAnimationHandler cannot animate without a valid FloatingActionMenu.");
    }
  }

  /**
   * Ends the opening animation
   * Should be overriden by children
   */
  public void animateMenuClosing(Point center) {
    if (menu == null) {
      throw new NullPointerException(
          "MenuAnimationHandler cannot animate without a valid FloatingActionMenu.");
    }
  }

  /**
   * Restores the specified sub action view to its final state, according to the current actionType
   * Should be called after an animation finishes.
   */
  protected void restoreSubActionViewAfterAnimation(PeacockMenu.Item subActionItem,
      ActionType actionType) {
    ViewGroup.LayoutParams params = subActionItem.view.getLayoutParams();
    subActionItem.view.setTranslationX(0);
    subActionItem.view.setTranslationY(0);
    subActionItem.view.setRotation(0);
    subActionItem.view.setScaleX(1);
    subActionItem.view.setScaleY(1);
    subActionItem.view.setAlpha(1);
    if (actionType == ActionType.OPENING) {
      FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) params;
      lp.setMargins(subActionItem.x, subActionItem.y, 0, 0);
      subActionItem.view.setLayoutParams(lp);
    } else if (actionType == ActionType.CLOSING) {
      Point center = menu.getActionViewCenter();
      FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) params;
      lp.setMargins(center.x - subActionItem.width / 2, center.y - subActionItem.height / 2, 0, 0);
      subActionItem.view.setLayoutParams(lp);
      menu.removeViewFromCurrentContainer(subActionItem.view);
    }
  }

  /**
   * A special animation listener that is intended to listen the last of the sequential animations.
   * Changes the animating property of children.
   */
  public class LastAnimationListener implements Animator.AnimatorListener {

    @Override public void onAnimationStart(Animator animation) {
      setAnimating(true);
    }

    @Override public void onAnimationEnd(Animator animation) {
      setAnimating(false);
    }

    @Override public void onAnimationCancel(Animator animation) {
      setAnimating(false);
    }

    @Override public void onAnimationRepeat(Animator animation) {
      setAnimating(true);
    }
  }

  public abstract boolean isAnimating();

  protected abstract void setAnimating(boolean animating);
}
