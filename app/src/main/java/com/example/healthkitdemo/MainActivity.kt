package com.example.healthkitdemo

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.healthkitdemo.healthkit.HealthKitAuthMethod
import com.example.healthkitdemo.healthkit.HealthKitConfig
import com.example.healthkitdemo.log.AppLog
import com.example.healthkitdemo.ui.HealthKitRoute
import com.example.healthkitdemo.ui.theme.HealthKitDemoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HealthKitViewModel by viewModels()

    private val loginFree = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onLoginFreeResult(it.resultCode, it.data)
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HealthKitDemoTheme {
                HealthKitRoute(
                    viewModel = viewModel,
                    onConnect = ::connect,
                    onOpenHuaweiHealth = ::openHuaweiHealth,
                    onCopyLog = ::copyLog,
                    onShareLog = ::shareLog,
                )
            }
        }

        if (savedInstanceState == null) intent?.data?.let(viewModel::handleRedirect)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let(viewModel::handleRedirect)
    }

    private fun connect() {
        askForNotifications()
        val intent = viewModel.authorizationIntent(this) ?: return
        if (viewModel.uiState.value.authMethod == HealthKitAuthMethod.LOGIN_FREE_SDK) {
            loginFree.launch(intent)
        } else {
            startActivity(intent)
        }
    }

    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return
        notificationPermission.launch(permission)
    }

    private fun openHuaweiHealth() {
        val launch = packageManager.getLaunchIntentForPackage(HealthKitConfig.HUAWEI_HEALTH_PACKAGE)
        if (launch == null) {
            viewModel.showMessage("The Huawei Health app is not installed")
            return
        }
        startActivity(launch)
    }

    private fun copyLog() {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Health Kit log", AppLog.export()))
        Toast.makeText(this, R.string.log_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareLog() {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, AppLog.export())
        startActivity(Intent.createChooser(send, getString(R.string.log_share)))
    }
}
