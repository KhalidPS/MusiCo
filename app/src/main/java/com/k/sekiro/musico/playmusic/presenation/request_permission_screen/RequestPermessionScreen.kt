package com.k.sekiro.taskmanagementapp.task_management_feature.presentation.request_permission_screen

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.*
import android.os.*
import android.provider.*
import android.util.*
import androidx.activity.compose.*
import androidx.activity.result.contract.*
import androidx.annotation.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import com.k.sekiro.musico.MainActivity
import com.k.sekiro.musico.R


@Composable
fun RequestPermissionScreen(
    //navController:NavHostController,
    showDialog:Boolean = false,
    goToSettings:Boolean = false,
    updateShowDialog:(Boolean) -> Unit,
    updateGoToSettings:(Boolean) -> Unit,
    context: Activity = LocalContext.current as MainActivity,
    onGrantedShowScreen: @Composable () -> Unit
) {




    val externalStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    var permissionType by remember { mutableStateOf(externalStoragePermission) }


    val permissions = arrayOf(
        externalStoragePermission,
        Manifest.permission.POST_NOTIFICATIONS
    )


    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            permissions.forEach { permission ->
                if (result[permission] == false) {
                    if (!context.shouldShowRequestPermissionRationale(permission)) {
                        updateGoToSettings(true)
                    }
                    updateShowDialog(true)
                }else updateShowDialog(false)
            }

        }
    )


    if (showDialog) {
        PermissionRationalDialog(
            titleResId = when(permissionType){
                externalStoragePermission -> R.string.storage_permission_title
                else -> R.string.notify_permission_msg_title
            },
            textResId = when(permissionType){
                externalStoragePermission -> R.string.storage_permission_msg
                else -> R.string.notify_permission_msg_content
            },
            onDismissRequest = { updateShowDialog(false) },
            onConfirmRequest = {
                updateShowDialog(false)
                if (goToSettings) {
                    updateGoToSettings(false)

                    Intent().also { intent ->
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        intent.putExtra("android.provider.extra.APP_PACKAGE",context.packageName)
                        intent.action = when(permissionType){
                            externalStoragePermission -> Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                            else -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        }

                        context.startActivity(intent)
                    }
                } else {

                    requestPermissionLauncher.launch(permissions)
                }

            }
        )
    }


    permissions.forEachIndexed { index,  permission ->

        permissionType = permission

        val isGranted =
            if (permission == Manifest.permission.POST_NOTIFICATIONS){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }else{
                    true
                }
            }else{
                    context.checkSelfPermission(externalStoragePermission) == PackageManager.PERMISSION_GRANTED

            }



        if (isGranted) {
            updateShowDialog(false)
            if (index == permissions.lastIndex){
                onGrantedShowScreen()
            }
        } else {
            if (context.shouldShowRequestPermissionRationale(permission)) {
                updateShowDialog(true)
                Log.e("ks","in should show  rational")
            } else {
                SideEffect {
                    requestPermissionLauncher.launch(permissions)

                }

            }
        }

    }

}




@Composable
fun PermissionRationalDialog(
    titleResId:Int,
    textResId:Int,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(onClick = onConfirmRequest) {
                Text(text = stringResource(id = R.string.cancel_btn))
            }
        },
        title = {
            Text(text = stringResource(id = titleResId))
        },
        text = {
            Text(text = stringResource(id = textResId))
        }
    )

}