package com.github.ma1co.openmemories.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sony.scalar.hardware.indicator.SubLCD;
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

    public static abstract class Scroller implements Runnable {
        private final int shortDelay = 250;
        private final int longDelay = 1000;
        private final Handler handler = new Handler();
        private final int width;
        private String text;
        private int off;

        public Scroller(int width) {
            this.width = width;
        }

        public void setText(String text) {
            handler.removeCallbacks(this);
            this.text = text;
            off = 0;
            run();
        }

        @Override
        public void run() {
            if (text.length() <= width) {
                display(text);
            } else {
                display(text.substring(off, off + width));
                if (off + width < text.length()) {
                    handler.postDelayed(this, off == 0 ? longDelay : shortDelay);
                    off++;
                } else {
                    handler.postDelayed(this, longDelay);
                    off = 0;
                }
            }
        }

        public abstract void display(String text);
    }

    private ListView listView;
    private ArrayAdapter<AppInfo> listAdapter;
    private Scroller scroller = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isCamera() && getPanelAspect() == 0) {
            // new action cam
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
            startActivity(new Intent().setComponent(listAdapter.getItem(pos).getComponentName()).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        });

        if (isCamera() && getSubLcdType() == 1) {
            // old action cam
            scroller = new Scroller(SubLCD.getStringLength(SubLCD.Sub.LID_INFOMATION)) {
                @Override
                public void display(String text) {
                    SubLCD.setString(SubLCD.Sub.LID_INFOMATION, true, SubLCD.PTN_ON, text);
                }
            };

            listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                    scroller.setText(listAdapter.getItem(pos).toString());
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    scroller.setText("");
                }
            });
        }
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

        if (scroller != null) {
            int pos = listView.getSelectedItemPosition();
            scroller.setText(pos >= 0 ? listAdapter.getItem(pos).toString() : "");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (scroller != null)
            scroller.setText("");
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

    public int getPanelAspect() {
        return ScalarProperties.getInt(ScalarProperties.PROP_DEVICE_PANEL_ASPECT, -1);
    }

    public int getSubLcdType() {
        return ScalarProperties.getInt(ScalarProperties.PROP_DVICE_SUBLCD_TYPE, -1);
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
