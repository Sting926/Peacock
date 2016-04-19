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

  protected final PeacockMenu menu;

  private AnimationEndListener animationEndListener;

  public MenuAnimationHandler(PeacockMenu menu) {
    this.menu = menu;
  }

  /**
   * Starts the opening animation
   * Should be overriden by children
   */
  public void menuOpening(Point center) {
    if (menu == null) {
      throw new NullPointerException(
          "MenuAnimationHandler cannot animate without a valid FloatingMenu.");
    }
  }

  /**
   * Ends the opening animation
   * Should be overriden by children
   */
  public void menuClosing(Point center) {
    if (menu == null) {
      throw new NullPointerException(
          "MenuAnimationHandler cannot animate without a valid FloatingMenu.");
    }
  }

  /**
   * Ends the opening animation
   * Should be overriden by children
   */
  public void otherMenuClosing(Point center, PeacockMenu menu) {
    if (menu == null) {
      throw new NullPointerException(
          "MenuAnimationHandler cannot animate without a valid FloatingMenu.");
    }
  }

  /**
   * Restores the specified sub action view to its final state, according to the current actionType
   * Should be called after an animation finishes.
   */
  protected void restoreSubActionViewAfterAnimation(PeacockMenu subActionItem,
      ActionType actionType) {
    ViewGroup.LayoutParams params = subActionItem.getLayoutParams();
    subActionItem.setTranslationX(0);
    subActionItem.setTranslationY(0);
    subActionItem.setRotation(0);
    subActionItem.setScaleX(1);
    subActionItem.setScaleY(1);
    subActionItem.setAlpha(1);
    if (actionType == ActionType.OPENING) {
      FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) params;
      lp.setMargins(subActionItem.coordX, subActionItem.coordY, 0, 0);
      subActionItem.setLayoutParams(lp);
    } else if (actionType == ActionType.CLOSING) {
      Point center = menu.getActionViewCenter();
      FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) params;
      lp.setMargins(center.x - subActionItem.width / 2, center.y - subActionItem.height / 2, 0, 0);
      subActionItem.setLayoutParams(lp);
      menu.removeViewFromCurrentContainer(subActionItem);
    }
  }

  /**
   * A special animation listener that is intended to listen the last of the sequential animations.
   * Changes the animating property of children.
   */
  public class AnimationsListener implements Animator.AnimatorListener {
    private ActionType actionType;

    public AnimationsListener(ActionType actionType) {
      this.actionType = actionType;
    }

    @Override public void onAnimationStart(Animator animation) {
      menu.setStatus(PeacockMenu.Status.PLAYING);
    }

    @Override public void onAnimationEnd(Animator animation) {
      if (actionType == ActionType.OPENING) {
        menu.setStatus(PeacockMenu.Status.OPEN);
      } else {
        menu.setStatus(PeacockMenu.Status.CLOSE);
      }
      if (animationEndListener != null) {
        animationEndListener.onAnimationEnd();
        animationEndListener = null;
      }
    }

    @Override public void onAnimationCancel(Animator animation) {
      if (actionType == ActionType.OPENING) {
        menu.setStatus(PeacockMenu.Status.CLOSE);
      } else {
        menu.setStatus(PeacockMenu.Status.OPEN);
      }
    }

    @Override public void onAnimationRepeat(Animator animation) {
      menu.setStatus(PeacockMenu.Status.PLAYING);
    }
  }

  public void setAnimationEndListener(AnimationEndListener animationEndListener) {
    this.animationEndListener = animationEndListener;
  }

  public interface AnimationEndListener {
    void onAnimationEnd();
  }
}
