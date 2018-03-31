/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.devsupport;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.common.ReactConstants;

import javax.annotation.Nullable;

/**
 * Helper class for controlling overlay view with FPS and JS FPS info
 * that gets added directly to @{link WindowManager} instance.
 */
/* package */ class DebugOverlayController {

  public static void requestPermission(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      // Get permission to show debug overlay in dev builds.
      if (!Settings.canDrawOverlays(context)) {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        FLog.w(ReactConstants.TAG, "Overlay permissions needs to be granted in order for react native apps to run in dev mode");
        if (canHandleIntent(context, intent)) {
          context.startActivity(intent);
        }
      }
    }
  }

  private static boolean permissionCheck(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      // Get permission to show debug overlay in dev builds.
      if (!Settings.canDrawOverlays(context)) {
        // overlay permission not yet granted
        return false;
      } else {
        return true;
      }
    }
    // on pre-M devices permission needs to be specified in manifest
    return hasPermission(context, Manifest.permission.SYSTEM_ALERT_WINDOW);
  }

  private static boolean hasPermission(Context context, String permission) {
    try {
      PackageInfo info = context.getPackageManager().getPackageInfo(
              context.getPackageName(),
              PackageManager.GET_PERMISSIONS);
      if (info.requestedPermissions != null) {
        for (String p : info.requestedPermissions) {
          if (p.equals(permission)) {
            return true;
          }
        }
      }
    } catch (PackageManager.NameNotFoundException e) {
      FLog.e(ReactConstants.TAG, "Error while retrieving package info", e);
    }
    return false;
  }

  private static boolean canHandleIntent(Context context, Intent intent) {
    PackageManager packageManager = context.getPackageManager();
    return intent.resolveActivity(packageManager) != null;
  }

  private final WindowManager mWindowManager;
  private final ReactContext mReactContext;

  private @Nullable FrameLayout mFPSDebugViewContainer;

  public DebugOverlayController(ReactContext reactContext) {
    mReactContext = reactContext;
    mWindowManager = (WindowManager) reactContext.getSystemService(Context.WINDOW_SERVICE);
  }

  private void showFpsDebugView() {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (!permissionCheck(mReactContext)) {
          FLog.d(ReactConstants.TAG, "Wait for overlay permission to be set");
          return;
        }

        if (mFPSDebugViewContainer != null) {
          return;
        }

        mFPSDebugViewContainer = new FpsView(mReactContext);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowOverlayCompat.TYPE_SYSTEM_OVERLAY,
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
              | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
          PixelFormat.TRANSLUCENT);
        mWindowManager.addView(mFPSDebugViewContainer, params);
      }
    });
  }

  private void hideFpsDebugView() {
    UiThreadUtil.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mFPSDebugViewContainer == null) {
          return;
        }
        mFPSDebugViewContainer.removeAllViews();
        mWindowManager.removeView(mFPSDebugViewContainer);
        mFPSDebugViewContainer = null;
      }
    });
  }

  public void setFpsDebugViewVisible(boolean fpsDebugViewVisible) {
    if (fpsDebugViewVisible) {
      showFpsDebugView();
    } else {
      hideFpsDebugView();
    }
  }
}
