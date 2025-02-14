package org.tasks.compose.edit

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.android.material.composethemeadapter.MdcTheme
import com.todoroo.astrid.ui.ReminderControlSetViewModel
import org.tasks.R
import org.tasks.compose.AddReminderDialog
import org.tasks.compose.DisabledText
import org.tasks.compose.TaskEditRow
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.data.Alarm
import org.tasks.reminders.AlarmToString
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AlarmRow(
    vm: ReminderControlSetViewModel = viewModel(),
    permissionStatus: PermissionStatus,
    launchPermissionRequest: () -> Unit,
    alarms: List<Alarm>,
    ringMode: Int,
    locale: Locale,
    newAlarm: () -> Unit,
    addAlarm: (Alarm) -> Unit,
    deleteAlarm: (Alarm) -> Unit,
    openRingType: () -> Unit,
) {
    TaskEditRow(
        iconRes = R.drawable.ic_outline_notifications_24px,
        content = {
            val viewState = vm.viewState.collectAsStateLifecycleAware().value
            when (permissionStatus) {
                PermissionStatus.Granted -> {
                    Alarms(
                        alarms = alarms,
                        ringMode = ringMode,
                        locale = locale,
                        addAlarm = newAlarm,
                        deleteAlarm = deleteAlarm,
                        openRingType = openRingType,
                    )
                }
                is PermissionStatus.Denied -> {
                    Column(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable {
                            launchPermissionRequest()
                        }
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(id = R.string.enable_reminders),
                            color = colorResource(id = R.color.red_500),
                        )
                        Text(
                            text = stringResource(id = R.string.enable_reminders_description),
                            style = MaterialTheme.typography.caption,
                            color = colorResource(id = R.color.red_500),
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            AddReminderDialog.AddCustomReminderDialog(
                openDialog = viewState.showCustomDialog,
                addAlarm = addAlarm,
                closeDialog = { vm.showCustomDialog(visible = false) }
            )

            AddReminderDialog.AddRandomReminderDialog(
                openDialog = viewState.showRandomDialog,
                addAlarm = addAlarm,
                closeDialog = { vm.showRandomDialog(visible = false) }
            )
        },
    )
}

@Composable
fun Alarms(
    alarms: List<Alarm>,
    ringMode: Int,
    locale: Locale,
    addAlarm: () -> Unit,
    deleteAlarm: (Alarm) -> Unit,
    openRingType: () -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        alarms.forEach { alarm ->
            org.tasks.compose.AlarmRow(AlarmToString(LocalContext.current, locale).toString(alarm)) {
                deleteAlarm(alarm)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            DisabledText(
                text = stringResource(id = R.string.add_reminder),
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false),
                        onClick = { addAlarm() }
                    )
            )
            Spacer(modifier = Modifier.weight(1f))
            if (alarms.isNotEmpty()) {
                Text(
                    text = stringResource(
                        id = when (ringMode) {
                            2 -> R.string.ring_nonstop
                            1 -> R.string.ring_five_times
                            else -> R.string.ring_once
                        }
                    ),
                    style = MaterialTheme.typography.body1.copy(
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false),
                            onClick = openRingType
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun NoAlarms() {
    MdcTheme {
        AlarmRow(
            alarms = emptyList(),
            ringMode = 0,
            locale = Locale.getDefault(),
            newAlarm = {},
            addAlarm = {},
            deleteAlarm = {},
            openRingType = {},
            permissionStatus = PermissionStatus.Granted,
            launchPermissionRequest = {}
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun PermissionDenied() {
    MdcTheme {
        AlarmRow(
            alarms = emptyList(),
            ringMode = 0,
            locale = Locale.getDefault(),
            newAlarm = {},
            addAlarm = {},
            deleteAlarm = {},
            openRingType = {},
            permissionStatus = PermissionStatus.Denied(true),
            launchPermissionRequest = {}
        )
    }
}