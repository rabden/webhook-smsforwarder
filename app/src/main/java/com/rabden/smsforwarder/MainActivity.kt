package com.rabden.smsforwarder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rabden.smsforwarder.ui.navigation.AppNavigation
import com.rabden.smsforwarder.ui.theme.SmsForwarderTheme
import com.rabden.smsforwarder.util.PermissionHelper
import com.rabden.smsforwarder.repository.SettingsRepository
import com.rabden.smsforwarder.service.SmsForwardingService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val settingsRepository = SettingsRepository(this)

        setContent {
            SmsForwarderTheme {
                var hasPermissions by remember { mutableStateOf(hasAllPermissions()) }
                
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    hasPermissions = result.values.all { it }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasPermissions) {
                        AppNavigation()
                        LaunchedEffect(Unit) {
                            if (!PermissionHelper.isBatteryOptimizationIgnored(this@MainActivity)) {
                                PermissionHelper.requestIgnoreBatteryOptimizations(this@MainActivity)
                            }
                            
                            // Start service now that we are in foreground and have permissions
                            // Only if both forwarding is enabled AND High Reliability Mode is ON
                            val isHighRel = settingsRepository.isHighReliabilityMode.first() ?: com.rabden.smsforwarder.util.BrandHelper.isAggressiveBrand()
                            if (settingsRepository.isForwardingEnabled.first() && isHighRel) {
                                SmsForwardingService.start(this@MainActivity)
                            }
                        }
                    } else {
                        com.rabden.smsforwarder.ui.permission.PermissionRequestScreen(
                            onGrantClick = {
                                launcher.launch(PermissionHelper.getAllRequiredPermissions())
                            }
                        )
                    }
                }
            }
        }
    }

    private fun hasAllPermissions(): Boolean {
        return PermissionHelper.hasSmsPermissions(this) &&
                PermissionHelper.hasContactsPermission(this) &&
                PermissionHelper.hasNotificationPermission(this)
    }
}