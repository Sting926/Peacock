package com.xfdsj.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;
import com.capricorn.ArcMenu;
import com.capricorn.RayMenu;
import com.xfdsj.peacock.PeacockLayout;
import com.xfdsj.peacock.PeacockMenu;

/**
 * @author Capricorn
 */
public class MainActivity extends Activity {
  private static final int[] ITEM_DRAWABLES = {
      R.drawable.composer_camera, R.drawable.composer_music, R.drawable.composer_place,
      R.drawable.composer_sleep, R.drawable.composer_thought, R.drawable.composer_with
  };

  /** Called when the activity is first created. */
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    ArcMenu arcMenu = (ArcMenu) findViewById(R.id.arc_menu);
    ArcMenu arcMenu2 = (ArcMenu) findViewById(R.id.arc_menu_2);

    initArcMenu(arcMenu, ITEM_DRAWABLES);
    initArcMenu(arcMenu2, ITEM_DRAWABLES);

    RayMenu rayMenu = (RayMenu) findViewById(R.id.ray_menu);
    final int itemCount = ITEM_DRAWABLES.length;
    for (int i = 0; i < itemCount; i++) {
      ImageView item = new ImageView(this);
      item.setBackgroundResource(R.color.colorAccent);
      item.setImageResource(ITEM_DRAWABLES[i]);

      final int position = i;
      rayMenu.addItem(item, new OnClickListener() {

        @Override public void onClick(View v) {
          Toast.makeText(MainActivity.this, "position:" + position, Toast.LENGTH_SHORT).show();
        }
      });// Add a menu item
    }

    final PeacockLayout peacockLayout = (PeacockLayout) findViewById(R.id.peacock);
    peacockLayout.setItemListener(new OnClickListener() {
      @Override public void onClick(View v) {
        if (v.getId() == R.id.iv_1) {
          Toast.makeText(MainActivity.this, v.getId() + "", Toast.LENGTH_SHORT).show();
        }
      }
    });

    PeacockMenu peacockMenu = (PeacockMenu) findViewById(R.id.floatingBtn);
    peacockMenu.setStateChangeListener(new PeacockMenu.MenuStateChangeListener() {
      @Override public void onMenuOpened(PeacockMenu menu) {
        //menu.getPeacockParent().closeOther(menu);
        menu.setStatus(PeacockMenu.Status.OPEN);
      }

      @Override public void onMenuClosed(PeacockMenu menu) {
        menu.closeAll(menu);
      }
    });
  }

  private void initArcMenu(ArcMenu menu, int[] itemDrawables) {
    final int itemCount = itemDrawables.length;
    for (int i = 0; i < itemCount; i++) {
      ImageView item = new ImageView(this);
      item.setBackgroundResource(R.color.colorAccent);
      item.setImageResource(itemDrawables[i]);

      final int position = i;
      menu.addItem(item, new OnClickListener() {

        @Override public void onClick(View v) {
          Toast.makeText(MainActivity.this, "position:" + position, Toast.LENGTH_SHORT).show();
        }
      });
    }
  }
}
