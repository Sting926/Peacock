/*
 *   Copyright 2014 Oguz Bilgener
 */
package com.xfdsj.peacock.animation;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Point;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import com.xfdsj.peacock.PeacockMenu;

/**
 * An example animation handler
 * Animates translation, rotation, scale and alpha at the same time using Property Animation APIs.
 */
public class DefaultAnimationHandler extends MenuAnimationHandler {

  /** duration of animations, in milliseconds */
  protected static final int DURATION = 500;
  /** duration to wait between each of */
  protected static final int LAG_BETWEEN_ITEMS = 20;
  /** holds the current state of animation */
  private boolean animating;

  public DefaultAnimationHandler() {
    setAnimating(false);
  }

  @Override public void animateMenuOpening(Point center) {
    super.animateMenuOpening(center);

    setAnimating(true);

    Animator lastAnimation = null;
    for (int i = 0; i < menu.getSubMenus().size(); i++) {
      if (menu.getSubMenus().get(i).active) {
        continue;
      }

      menu.getSubMenus().get(i).setScaleX(0);
      menu.getSubMenus().get(i).setScaleY(0);
      menu.getSubMenus().get(i).setAlpha(0);

      PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X,
          menu.getSubMenus().get(i).coordX - center.x + menu.getSubMenus().get(i).width / 2);
      PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y,
          menu.getSubMenus().get(i).coordY - center.y + menu.getSubMenus().get(i).height / 2);
      PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 720);
      PropertyValuesHolder pvhsX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1);
      PropertyValuesHolder pvhsY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1);
      PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 1);

      final ObjectAnimator animation =
          ObjectAnimator.ofPropertyValuesHolder(menu.getSubMenus().get(i), pvhX, pvhY, pvhR, pvhsX,
              pvhsY, pvhA);
      animation.setDuration(DURATION);
      animation.setInterpolator(new OvershootInterpolator(0.9f));
      animation.addListener(
          new SubActionItemAnimationListener(menu.getSubMenus().get(i), ActionType.OPENING));

      if (i == 0) {
        lastAnimation = animation;
      }

      // Put a slight lag between each of the menu items to make it asymmetric
      animation.setStartDelay((menu.getSubMenus().size() - i) * LAG_BETWEEN_ITEMS);
      animation.start();
    }
    if (lastAnimation != null) {
      lastAnimation.addListener(new LastAnimationListener());
    } else {
      setAnimating(false);
    }
  }

  @Override public void animateMenuClosing(Point center) {
    super.animateMenuOpening(center);

    setAnimating(true);

    Animator lastAnimation = null;
    for (int i = 0; i < menu.getSubMenus().size(); i++) {
      if (menu.getSubMenus().get(i).active) {
        continue;
      }
      PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X,
          -(menu.getSubMenus().get(i).coordX - center.x + menu.getSubMenus().get(i).width / 2));
      PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y,
          -(menu.getSubMenus().get(i).coordY - center.y + menu.getSubMenus().get(i).height / 2));
      PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, -720);
      PropertyValuesHolder pvhsX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0);
      PropertyValuesHolder pvhsY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0);
      PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 0);

      final ObjectAnimator animation =
          ObjectAnimator.ofPropertyValuesHolder(menu.getSubMenus().get(i), pvhX, pvhY, pvhR, pvhsX,
              pvhsY, pvhA);
      animation.setDuration(DURATION);
      animation.setInterpolator(new AccelerateDecelerateInterpolator());
      animation.addListener(
          new SubActionItemAnimationListener(menu.getSubMenus().get(i), ActionType.CLOSING));

      if (i == 0) {
        lastAnimation = animation;
      }

      animation.setStartDelay((menu.getSubMenus().size() - i) * LAG_BETWEEN_ITEMS);
      animation.start();
    }
    if (lastAnimation != null) {
      lastAnimation.addListener(new LastAnimationListener());
    } else {
      setAnimating(false);
    }
  }

  @Override public boolean isAnimating() {
    return animating;
  }

  @Override protected void setAnimating(boolean animating) {
    this.animating = animating;
  }

  protected class SubActionItemAnimationListener implements Animator.AnimatorListener {

    private PeacockMenu subActionItem;
    private ActionType actionType;

    public SubActionItemAnimationListener(PeacockMenu subActionItem, ActionType actionType) {
      this.subActionItem = subActionItem;
      this.actionType = actionType;
    }

    @Override public void onAnimationStart(Animator animation) {

    }

    @Override public void onAnimationEnd(Animator animation) {
      restoreSubActionViewAfterAnimation(subActionItem, actionType);
    }

    @Override public void onAnimationCancel(Animator animation) {
      restoreSubActionViewAfterAnimation(subActionItem, actionType);
    }

    @Override public void onAnimationRepeat(Animator animation) {
    }
  }
}
