package com.github.ma1co.openmemories.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sony.scalar.sysutil.ScalarInput;
import com.sony.scalar.sysutil.ScalarProperties;

public class MainActivity extends Activity {
    public static class AppInfo {
        private final ActivityInfo info;
        private final String label;

        public AppInfo(ActivityInfo info, String label) {
            this.info = info;
            this.label = label;
        }

        public ComponentName getComponentName() {
            return new ComponentName(info.packageName, info.name);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private ListView listView;
    private ArrayAdapter<AppInfo> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isCamera() && !hasDisplay()) {
            // action cam
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
            layoutParams.width = 100;
            layoutParams.height = 80;
            getWindow().setAttributes(layoutParams);

            setTheme(R.style.ActionCamTheme);
        }

        listView = new ListView(this) {
            @Override
            public int getMaxScrollAmount() {
                return Integer.MAX_VALUE;
            }
        };
        setContentView(listView);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((adapterView, view, pos, id) -> {
            AppInfo app = listAdapter.getItem(pos);
            if (app != null)
                startActivity(new Intent().setComponent(app.getComponentName()).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        listAdapter.clear();
        Intent[] intents = {
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
        };
        for (Intent intent : intents) {
            for (ResolveInfo info : getPackageManager().queryIntentActivities(intent, 0)) {
                if (!getComponentName().getClassName().equals(info.activityInfo.name)) {
                    String label = info.activityInfo.loadLabel(getPackageManager()).toString();
                    label = label.replaceAll("\\s+", " ");
                    listAdapter.add(new AppInfo(info.activityInfo, label));
                }
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = convertKeyCode(event.getScanCode());
        if (code == KeyEvent.KEYCODE_UNKNOWN)
            code = event.getKeyCode();
        return super.dispatchKeyEvent(new KeyEvent(event.getAction(), code));
    }

    public boolean isCamera() {
        return "sony".equals(Build.BRAND) && "ScalarA".equals(Build.MODEL) && "dslr-diadem".equals(Build.DEVICE);
    }

    public boolean hasDisplay() {
        return ScalarProperties.getInt(ScalarProperties.PROP_DEVICE_PANEL_ASPECT, -1) != 0;
    }

    public int convertKeyCode(int scanCode) {
        switch (scanCode) {
            case ScalarInput.ISV_KEY_UP:
            case ScalarInput.ISV_KEY_IR_UP:
            case ScalarInput.ISV_KEY_LEFT:// action cam
                return KeyEvent.KEYCODE_DPAD_UP;
            case ScalarInput.ISV_KEY_DOWN:
            case ScalarInput.ISV_KEY_IR_DOWN:
            case ScalarInput.ISV_KEY_RIGHT:// action cam
                return KeyEvent.KEYCODE_DPAD_DOWN;
            case ScalarInput.ISV_KEY_ENTER:
            case ScalarInput.ISV_KEY_IR_ENTER:
            case ScalarInput.ISV_KEY_STASTOP:
            case ScalarInput.ISV_KEY_IR_STASTOP:
            case ScalarInput.ISV_KEY_IR_RIGHT:// camcorder
                return KeyEvent.KEYCODE_DPAD_CENTER;
            case ScalarInput.ISV_KEY_MENU:
            case ScalarInput.ISV_KEY_IR_MENU:
            case ScalarInput.ISV_KEY_IR_LEFT:// camcorder
                return KeyEvent.KEYCODE_BACK;
            default:
                return KeyEvent.KEYCODE_UNKNOWN;
        }
    }
}
