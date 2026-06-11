package com.rabden.smsforwarder.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object BrandHelper {

    enum class Brand {
        XIAOMI, SAMSUNG, OPPO, VIVO, HUAWEI, HONOR, INFINIX, WALTON, 
        ASUS, MEIZU, SONY, ZTE, LENOVO, NOKIA, DURASPEED, PIXEL, OTHER
    }

    data class BrandAction(
        val label: String,
        val intent: Intent
    )

    fun getDeviceBrand(): Brand {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") || manufacturer.contains("blackshark") -> Brand.XIAOMI
            manufacturer.contains("samsung") -> Brand.SAMSUNG
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> Brand.OPPO
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> Brand.VIVO
            manufacturer.contains("huawei") -> Brand.HUAWEI
            manufacturer.contains("honor") -> Brand.HONOR
            manufacturer.contains("infinix") || manufacturer.contains("tecno") || manufacturer.contains("itel") -> Brand.INFINIX
            manufacturer.contains("walton") -> Brand.WALTON
            manufacturer.contains("asus") -> Brand.ASUS
            manufacturer.contains("meizu") -> Brand.MEIZU
            manufacturer.contains("sony") -> Brand.SONY
            manufacturer.contains("zte") || manufacturer.contains("nubia") || manufacturer.contains("redmagic") -> Brand.ZTE
            manufacturer.contains("lenovo") || manufacturer.contains("motorola") || manufacturer.contains("moto") -> Brand.LENOVO
            manufacturer.contains("nokia") || manufacturer.contains("hmd") -> Brand.NOKIA
            // MediaTek generic (Blackview, Ulefone, Doogee, Oukitel) often use DuraSpeed
            manufacturer.contains("blackview") || manufacturer.contains("ulefone") || manufacturer.contains("doogee") || 
            manufacturer.contains("oukitel") || manufacturer.contains("umidigi") -> Brand.DURASPEED
            manufacturer.contains("google") -> Brand.PIXEL
            else -> Brand.OTHER
        }
    }

    fun isAggressiveBrand(): Boolean {
        return when (getDeviceBrand()) {
            Brand.XIAOMI,
            Brand.OPPO,
            Brand.VIVO,
            Brand.HUAWEI,
            Brand.HONOR,
            Brand.INFINIX,
            Brand.WALTON,
            Brand.ASUS,
            Brand.MEIZU,
            Brand.ZTE,
            Brand.NOKIA,
            Brand.DURASPEED -> true
            else -> false
        }
    }

    fun getBrandActions(context: Context, brand: Brand = getDeviceBrand()): List<BrandAction> {
        val actions = mutableListOf<BrandAction>()
        
        // 1. App Info: Universal & most important for Android 14 (Battery -> Unrestricted)
        val appInfoIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        actions.add(BrandAction("Open App Info", appInfoIntent))

        // 2. Brand Specific Settings (Deep links)
        try {
            when (brand) {
                Brand.XIAOMI -> {
                    actions.add(BrandAction("Autostart Settings", Intent().apply {
                        component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }))
                }
                Brand.SAMSUNG -> {
                    actions.add(BrandAction("Device Care", Intent().apply {
                        component = ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }))
                }
                Brand.OPPO -> {
                    actions.add(BrandAction("Startup Manager", Intent().apply {
                        component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }))
                }
                Brand.VIVO -> {
                    actions.add(BrandAction("Background Start Manager", Intent().apply {
                        component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }))
                }
                Brand.HUAWEI -> {
                    actions.add(BrandAction("App Launch Settings", Intent().apply {
                        component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }))
                }
                Brand.HONOR -> {
                    actions.add(BrandAction("App Launch Settings", Intent().apply {
                        component = ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.optimize.process.ProtectActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }))
                }
                Brand.INFINIX -> {
                    actions.add(BrandAction("Phone Master (Autostart)", Intent().apply {
                        component = ComponentName("com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }))
                }
                Brand.ASUS -> {
                    actions.add(BrandAction("Mobile Manager", Intent().apply {
                        component = ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.MainActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }))
                }
                Brand.MEIZU -> {
                    actions.add(BrandAction("App Security", Intent().apply {
                        component = ComponentName("com.meizu.safe", "com.meizu.safe.security.AppSecActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }))
                }
                Brand.ZTE -> {
                    actions.add(BrandAction("App Manager", Intent().apply {
                        component = ComponentName("com.zte.heartyservice", "com.zte.heartyservice.setting.ClearConfigActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }))
                }
                Brand.LENOVO -> {
                    actions.add(BrandAction("Power Setting", Intent().apply {
                        component = ComponentName("com.lenovo.powersetting", "com.lenovo.powersetting.ui.Settings\$BackgroundAppManagementActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }))
                }
                Brand.DURASPEED -> {
                    actions.add(BrandAction("DuraSpeed Settings", Intent().apply {
                        component = ComponentName("com.mediatek.duraspeed", "com.mediatek.duraspeed.DuraSpeedMainActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }))
                }
                else -> {}
            }
        } catch (e: Exception) {
            Log.e("BrandHelper", "Failed to add brand-specific action: ${e.message}")
        }

        // 3. Standard Battery Optimization List (Fallback)
        val batteryIntent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        actions.add(BrandAction("All Battery Optimizations", batteryIntent))
        
        return actions
    }

    fun getBrandInstructions(brand: Brand = getDeviceBrand()): String {
        return when (brand) {
            Brand.XIAOMI -> "1. Enable 'Autostart'\n2. Battery Saver -> 'No Restrictions'\n3. Recents Screen -> Swipe down on app to 'Lock' it\n4. If blocked, App Info -> (⋮) -> 'Allow restricted settings'"
            Brand.SAMSUNG -> "1. App Info -> Battery -> 'Unrestricted'\n2. Device Care -> Battery -> Background usage limits -> 'Never auto-sleeping apps' -> Add this app\n3. Ensure 'Auto-optimization' is OFF"
            Brand.OPPO -> "1. Enable 'Auto-launch'\n2. App Battery Management -> Enable 'Allow background activity'\n3. Lock the app in the Task Switcher"
            Brand.VIVO -> "1. Battery -> Background Power Consumption Management -> Select 'High background power consumption'\n2. Enable 'Autostart' in Permissions"
            Brand.HUAWEI -> "1. Battery -> App Launch -> Toggle to 'Manage Manually'\n2. Ensure 'Auto-launch', 'Secondary launch', and 'Run in background' are all ON"
            Brand.HONOR -> "1. Battery -> App Launch -> Toggle to 'Manage Manually'\n2. Ensure 'Auto-launch' and 'Run in background' are ON\n3. App Info -> Battery -> Ensure 'Power-intensive prompt' is OFF"
            Brand.INFINIX -> "1. Phone Master -> Toolbox -> Auto-start -> Enable app\n2. Open Recent Apps -> Swipe down on this app to 'Lock' it\n3. Ensure app is NOT in 'Freezer' folder"
            Brand.WALTON -> "1. App Info -> Battery -> 'Unrestricted'\n2. Ensure 'Smart Power Saving' is OFF\n3. Lock the app in the Recent Apps list"
            Brand.ASUS -> "1. Mobile Manager -> Hanlde Autostart -> Enable app\n2. Battery -> Battery optimization -> Set to 'Don't optimize'"
            Brand.MEIZU -> "1. Security App -> Permissions -> Autostart -> Enable\n2. Security App -> Permissions -> Background Management -> 'Allow running in background'"
            Brand.SONY -> "1. Battery -> STAMINA mode -> Apps exempted from STAMINA -> Add this app\n2. Disable 'Adaptive Battery'"
            Brand.ZTE -> "1. Settings -> Battery -> App Power Management -> This App -> Disable 'Auto-optimize'\n2. Enable 'Allow background execution'"
            Brand.LENOVO -> "1. Settings -> Power Setting -> Background App Management -> Enable app\n2. App Info -> Battery -> 'Unrestricted'"
            Brand.NOKIA -> "1. Disable 'Adaptive Battery' in Battery settings\n2. Battery -> Battery optimization -> Set to 'Don't optimize'"
            Brand.DURASPEED -> "1. Settings -> Smart Assist -> DuraSpeed -> Ensure app is toggled ON\n2. System Manager -> App Management -> Auto Clean -> Ensure app is Protected"
            else -> "1. Set Battery Optimization to 'Unrestricted' in App Info\n2. Lock the app in the Recent Apps (Recents) screen."
        }
    }
}
