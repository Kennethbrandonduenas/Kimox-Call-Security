package com.kimoxcallsecurity

import android.os.Bundle
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

class MainActivity : ReactActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    KimoxDiagnostics.record(this, "INFO", "MAIN_ACTIVITY_ON_CREATE_BEFORE_SUPER")
    try {
      super.onCreate(savedInstanceState)
      KimoxDiagnostics.record(this, "INFO", "MAIN_ACTIVITY_ON_CREATE_AFTER_SUPER")
    } catch (error: Throwable) {
      KimoxDiagnostics.recordCrash(this, Thread.currentThread(), error)
      throw error
    }
  }

  override fun onStart() {
    super.onStart()
    KimoxDiagnostics.record(this, "INFO", "MAIN_ACTIVITY_ON_START")
  }

  override fun onResume() {
    super.onResume()
    KimoxDiagnostics.record(this, "INFO", "MAIN_ACTIVITY_ON_RESUME")
  }

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  override fun getMainComponentName(): String = "KimoxCallSecurity"

  /**
   * Returns the instance of the [ReactActivityDelegate]. We use [DefaultReactActivityDelegate]
   * which allows you to enable New Architecture with a single boolean flags [fabricEnabled]
   */
  override fun createReactActivityDelegate(): ReactActivityDelegate =
      DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)
}
