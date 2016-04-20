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

  /** holds the current state of animation */
  //private boolean animating;
  public DefaultAnimationHandler(PeacockMenu menu) {
    super(menu);
  }

  @Override public void menuOpening(Point center) {
    super.menuOpening(center);

    List<Animator> animators = new ArrayList<>();
    for (PeacockMenu m : menu.getSubMenus()) {
      m.setScaleX(0);
      m.setScaleY(0);
      m.setAlpha(0);

      PropertyValuesHolder pvhX =
          PropertyValuesHolder.ofFloat(View.TRANSLATION_X, m.coordX - center.x + m.width / 2);
      PropertyValuesHolder pvhY =
          PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, m.coordY - center.y + m.height / 2);
      PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 720);
      PropertyValuesHolder pvhsX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1);
      PropertyValuesHolder pvhsY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1);
      PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 1);

      final ObjectAnimator animation =
          ObjectAnimator.ofPropertyValuesHolder(m, pvhX, pvhY, pvhR, pvhsX, pvhsY, pvhA);
      animation.setDuration(DURATION);
      animation.setInterpolator(new OvershootInterpolator(0.9f));
      animation.addListener(new SubMenuItemAnimationListener(m, ActionType.OPENING));
      animators.add(animation);
    }
    if (animators.size() > 0) {
      AnimatorSet animatorSet = new AnimatorSet();
      animatorSet.playTogether(animators);
      animatorSet.addListener(new AnimationsListener(ActionType.OPENING));
      animatorSet.start();
    }
  }

  @Override public void menuClosing(Point center) {
    super.menuClosing(center);

    List<Animator> animators = new ArrayList<>();
    for (PeacockMenu m : menu.getSubMenus()) {
      PropertyValuesHolder pvhX =
          PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -(m.coordX - center.x + m.width / 2));
      PropertyValuesHolder pvhY =
          PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -(m.coordY - center.y + m.height / 2));
      PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, -720);
      PropertyValuesHolder pvhsX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0);
      PropertyValuesHolder pvhsY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0);
      PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 0);

      final ObjectAnimator animation =
          ObjectAnimator.ofPropertyValuesHolder(m, pvhX, pvhY, pvhR, pvhsX, pvhsY, pvhA);
      animation.setDuration(DURATION);
      animation.setInterpolator(new AccelerateDecelerateInterpolator());
      animation.addListener(new SubMenuItemAnimationListener(m, ActionType.CLOSING));
      animators.add(animation);
    }
    if (animators.size() > 0) {
      AnimatorSet animatorSet = new AnimatorSet();
      animatorSet.playTogether(animators);
      animatorSet.addListener(new AnimationsListener(ActionType.CLOSING));
      animatorSet.start();
    }
  }

  @Override public void otherMenuClosing(Point center, PeacockMenu currentMenu) {
    super.otherMenuClosing(center, currentMenu);

    List<Animator> animators = new ArrayList<>();
    for (PeacockMenu m : menu.getSubMenus()) {
      if (m == currentMenu) {
        continue;
      }
      PropertyValuesHolder pvhX =
          PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -(m.coordX - center.x + m.width / 2));
      PropertyValuesHolder pvhY =
          PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, -(m.coordY - center.y + m.height / 2));
      PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, -720);
      PropertyValuesHolder pvhsX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0);
      PropertyValuesHolder pvhsY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0);
      PropertyValuesHolder pvhA = PropertyValuesHolder.ofFloat(View.ALPHA, 0);

      final ObjectAnimator animation =
          ObjectAnimator.ofPropertyValuesHolder(m, pvhX, pvhY, pvhR, pvhsX, pvhsY, pvhA);
      animation.setDuration(DURATION);
      animation.setInterpolator(new AccelerateDecelerateInterpolator());
      animation.addListener(new SubMenuItemAnimationListener(m, ActionType.CLOSING));
      animators.add(animation);
    }
    if (animators.size() > 0) {
      AnimatorSet animatorSet = new AnimatorSet();
      animatorSet.playTogether(animators);
      animatorSet.start();
    }
  }

  protected class SubMenuItemAnimationListener implements Animator.AnimatorListener {

    private PeacockMenu subActionItem;
    private ActionType actionType;

    public SubMenuItemAnimationListener(PeacockMenu subActionItem, ActionType actionType) {
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
