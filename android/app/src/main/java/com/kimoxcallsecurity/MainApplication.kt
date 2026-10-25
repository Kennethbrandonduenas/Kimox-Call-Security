package com.kimoxcallsecurity

import android.app.Application
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost

class MainApplication : Application(), ReactApplication {

  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList =
        PackageList(this).packages.apply {
          // Packages that cannot be autolinked yet can be added manually here, for example:
          // add(MyReactNativePackage())
          add(KimoxSecurityPackage())
        },
    )
  }

  override fun onCreate() {
    super.onCreate()
    KimoxCrashHandler.install(this)
    KimoxDiagnostics.record(this, "INFO", "PROCESS_START", "process=${currentProcessName()}")
    KimoxDiagnostics.recordEnvironment(this)
    if (currentProcessName().endsWith(RECOVERY_PROCESS_SUFFIX)) {
      KimoxDiagnostics.record(this, "INFO", "RECOVERY_PROCESS_READY")
      return
    }
    KimoxBackgroundManager.initialize(this)
    try {
      KimoxDiagnostics.record(this, "INFO", "REACT_NATIVE_INITIALIZING")
      loadReactNative(this)
      KimoxDiagnostics.record(this, "INFO", "REACT_NATIVE_INITIALIZED")
    } catch (error: Throwable) {
      KimoxDiagnostics.recordCrash(this, Thread.currentThread(), error)
      throw error
    }
  }

  private fun currentProcessName(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return getProcessName()
    val pid = android.os.Process.myPid()
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return manager.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName.orEmpty()
  }

  companion object {
    private const val RECOVERY_PROCESS_SUFFIX = ":recovery"
  }
}
