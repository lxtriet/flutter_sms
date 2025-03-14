package com.example.flutter_sms
import android.annotation.TargetApi
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import android.app.Activity
import android.net.Uri
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
class FlutterSmsPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
  private lateinit var mChannel: MethodChannel
  private var activity: Activity? = null
  private var sendSMSResult: Result? = null
  private val REQUEST_CODE_SEND_SMS = 205
  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    binding.addActivityResultListener(this)
    activity = binding.activity
  }
  override fun onDetachedFromActivity() {
    activity = null
  }
  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }
  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
  }
  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    setupCallbackChannels(flutterPluginBinding.binaryMessenger)
  }
  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    teardown()
  }
  private fun setupCallbackChannels(messenger: BinaryMessenger) {
    mChannel = MethodChannel(messenger, "flutter_sms")
    mChannel.setMethodCallHandler(this)
  }
  private fun teardown() {
    mChannel.setMethodCallHandler(null)
  }
  // V1 embedding entry point. This is deprecated and will be removed in a future Flutter
  // release but we leave it here in case someone's app does not utilize the V2 embedding yet.
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val inst = FlutterSmsPlugin()
      inst.activity = registrar.activity()
      inst.setupCallbackChannels(registrar.messenger())
    }
  }
  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
        "sendSMS" -> {
          if (!canSendSMS()) {
            result.error(
                    "device_not_capable",
                    "The current device is not capable of sending text messages.",
                    "A device may be unable to send messages if it does not support messaging or if it is not currently configured to send messages. This only applies to the ability to send text messages via iMessage, SMS, and MMS.")
            return
          }
          val message = call.argument<String?>("message")
          val recipients = call.argument<String?>("recipients")
          sendSMSResult = result
          sendSMS(recipients, message!!)
        }
        "canSendSMS" -> result.success(canSendSMS())
        else -> result.notImplemented()
    }
  }
  @TargetApi(Build.VERSION_CODES.ECLAIR)
  private fun canSendSMS(): Boolean {
    if (!activity!!.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
      return false
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("smsto:")
    val activityInfo = intent.resolveActivityInfo(activity!!.packageManager, intent.flags.toInt())
    return !(activityInfo == null || !activityInfo.exported)
  }
  private fun sendSMS(phones: String?, message: String?) {
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("smsto:$phones")
    intent.putExtra("sms_body", message)
    intent.putExtra(Intent.EXTRA_TEXT, message)
    activity?.startActivityForResult(intent, REQUEST_CODE_SEND_SMS)
  }
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    when (requestCode) {
      REQUEST_CODE_SEND_SMS -> sendSMSResult?.success("SMS Sent!")
    }
    return false
  }
}