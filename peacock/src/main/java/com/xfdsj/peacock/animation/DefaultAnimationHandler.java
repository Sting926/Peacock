package com.xfdsj.peacock.animation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Point;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import com.xfdsj.peacock.PeacockMenu;
import java.util.ArrayList;
import java.util.List;

/**
 * An example animation handler
 * Animates translation, rotation, scale and alpha at the same time using Property Animation APIs.
 */
public class DefaultAnimationHandler extends MenuAnimationHandler {

  /** duration of animations, in milliseconds */
  protected static final int DURATION = 300;
  /** duration to wait between each of */
  protected static final int LAG_BETWEEN_ITEMS = 20;

  /** holds the current state of animation */
  //private boolean animating;
  public DefaultAnimationHandler(PeacockMenu menu) {
    super(menu);
  }

  @Override public void animateMenuOpening(Point center) {
    super.animateMenuOpening(center);

    List<Animator> animators = new ArrayList<>();
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
      // Put a slight lag between each of the menu items to make it asymmetric
      animation.setStartDelay((menu.getSubMenus().size() - i) * LAG_BETWEEN_ITEMS);
      animators.add(animation);
    }
    if (animators.size() > 0) {
      AnimatorSet animatorSet = new AnimatorSet();
      animatorSet.playTogether(animators);
      animatorSet.addListener(new LastAnimationListener(ActionType.OPENING));
      animatorSet.start();
    }
  }

  @Override public void animateMenuClosing(Point center) {
    super.animateMenuClosing(center);

    List<Animator> animators = new ArrayList<>();
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
      animation.setStartDelay((menu.getSubMenus().size() - i) * LAG_BETWEEN_ITEMS);
      animators.add(animation);
    }
    if (animators.size() > 0) {
      AnimatorSet animatorSet = new AnimatorSet();
      animatorSet.playTogether(animators);
      animatorSet.addListener(new LastAnimationListener(ActionType.CLOSING));
      animatorSet.start();
    }
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
