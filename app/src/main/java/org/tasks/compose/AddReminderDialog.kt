package org.tasks.compose

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.composethemeadapter.MdcTheme
import kotlinx.coroutines.android.awaitFrame
import org.tasks.R
import org.tasks.data.Alarm
import org.tasks.reminders.AlarmToString.Companion.getRepeatString
import java.util.concurrent.TimeUnit

@ExperimentalComposeUiApi
object AddReminderDialog {
    @Composable
    fun AddRandomReminderDialog(
        openDialog: Boolean,
        addAlarm: (Alarm) -> Unit,
        closeDialog: () -> Unit,
    ) {
        val time = rememberSaveable { mutableStateOf(15) }
        val units = rememberSaveable { mutableStateOf(0) }
        if (openDialog) {
            AlertDialog(
                onDismissRequest = closeDialog,
                text = { AddRandomReminder(openDialog, time, units) },
                confirmButton = {
                    Constants.TextButton(text = R.string.ok, onClick = {
                        time.value.takeIf { it > 0 }?.let { i ->
                            addAlarm(Alarm(0, i * units.millis, Alarm.TYPE_RANDOM))
                            closeDialog()
                        }
                    })
                },
                dismissButton = {
                    Constants.TextButton(
                        text = R.string.cancel,
                        onClick = closeDialog
                    )
                },
            )
        } else {
            time.value = 15
            units.value = 0
        }
    }

    @Composable
    fun AddCustomReminderDialog(
        openDialog: Boolean,
        addAlarm: (Alarm) -> Unit,
        closeDialog: () -> Unit,
    ) {
        val time = rememberSaveable { mutableStateOf(15) }
        val units = rememberSaveable { mutableStateOf(0) }
        val openRecurringDialog = rememberSaveable { mutableStateOf(false) }
        val interval = rememberSaveable { mutableStateOf(0) }
        val recurringUnits = rememberSaveable { mutableStateOf(0) }
        val repeat = rememberSaveable { mutableStateOf(0) }
        if (openDialog) {
            if (!openRecurringDialog.value) {
                AlertDialog(
                    onDismissRequest = closeDialog,
                    text = {
                        AddCustomReminder(
                            openDialog,
                            time,
                            units,
                            interval,
                            recurringUnits,
                            repeat,
                            showRecurring = {
                                openRecurringDialog.value = true
                            }
                        )
                    },
                    confirmButton = {
                        Constants.TextButton(text = R.string.ok, onClick = {
                            time.value.takeIf { it >= 0 }?.let { i ->
                                addAlarm(
                                    Alarm(
                                        0,
                                        -1 * i * units.millis,
                                        Alarm.TYPE_REL_END,
                                        repeat.value,
                                        interval.value * recurringUnits.millis
                                    )
                                )
                                closeDialog()
                            }
                        })
                    },
                    dismissButton = {
                        Constants.TextButton(
                            text = R.string.cancel,
                            onClick = closeDialog
                        )
                    },
                )
            }
            AddRepeatReminderDialog(
                openDialog = openRecurringDialog,
                initialInterval = interval.value,
                initialUnits = recurringUnits.value,
                initialRepeat = repeat.value,
                selected = { i, u, r ->
                    interval.value = i
                    recurringUnits.value = u
                    repeat.value = r
                }
            )
        } else {
            time.value = 15
            units.value = 0
            interval.value = 0
            recurringUnits.value = 0
            repeat.value = 0
        }
    }

    @Composable
    fun AddRepeatReminderDialog(
        openDialog: MutableState<Boolean>,
        initialInterval: Int,
        initialUnits: Int,
        initialRepeat: Int,
        selected: (Int, Int, Int) -> Unit,
    ) {
        val interval = rememberSaveable { mutableStateOf(initialInterval) }
        val units = rememberSaveable { mutableStateOf(initialUnits) }
        val repeat = rememberSaveable { mutableStateOf(initialRepeat) }
        val closeDialog = {
            openDialog.value = false
        }
        if (openDialog.value) {
            AlertDialog(
                onDismissRequest = closeDialog,
                text = {
                    AddRecurringReminder(
                        openDialog.value,
                        interval,
                        units,
                        repeat,
                    )
                },
                confirmButton = {
                    Constants.TextButton(text = R.string.ok, onClick = {
                        if (interval.value > 0 && repeat.value > 0) {
                            selected(interval.value, units.value, repeat.value)
                            openDialog.value = false
                        }
                    })
                },
                dismissButton = {
                    Constants.TextButton(
                        text = R.string.cancel,
                        onClick = closeDialog
                    )
                },
            )
        } else {
            interval.value = initialInterval.takeIf { it > 0 } ?: 15
            units.value = initialUnits
            repeat.value = initialRepeat.takeIf { it > 0 } ?: 4
        }
    }

    @Composable
    fun AddRandomReminder(
        visible: Boolean,
        time: MutableState<Int>,
        units: MutableState<Int>,
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            CenteredH6(text = stringResource(id = R.string.randomly_every, "").trim())
            val focusRequester = remember { FocusRequester() }
            OutlinedIntInput(
                time,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Spacer(modifier = Modifier.height(16.dp))
            options.forEachIndexed { index, option ->
                RadioRow(index, option, time, units)
            }
            ShowKeyboard(visible, focusRequester)
        }
    }

    @Composable
    fun AddCustomReminder(
        visible: Boolean,
        time: MutableState<Int>,
        units: MutableState<Int>,
        interval: MutableState<Int>,
        recurringUnits: MutableState<Int>,
        repeat: MutableState<Int>,
        showRecurring: () -> Unit,
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            CenteredH6(resId = R.string.custom_notification)
            val focusRequester = remember { FocusRequester() }
            OutlinedIntInput(
                time,
                minValue = 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Spacer(modifier = Modifier.height(16.dp))
            options.forEachIndexed { index, option ->
                RadioRow(index, option, time, units, R.string.alarm_before_due)
            }
            Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp)
            Row(modifier = Modifier
                .fillMaxWidth()
                .clickable { showRecurring() })
            {
                IconButton(onClick = showRecurring) {
                    Icon(
                        imageVector = Icons.Outlined.Autorenew,
                        contentDescription = null,
                        modifier = Modifier
                            .align(CenterVertically)
                            .alpha(
                                ResourcesCompat.getFloat(
                                    LocalContext.current.resources,
                                    R.dimen.alpha_secondary
                                )
                            ),
                    )
                }
                val repeating = repeat.value > 0 && interval.value > 0
                val text = if (repeating) {
                    LocalContext.current.resources.getRepeatString(
                        repeat.value,
                        interval.value * recurringUnits.millis
                    )
                } else {
                    stringResource(id = R.string.repeat_option_does_not_repeat)
                }
                BodyText(
                    text = text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .align(CenterVertically)
                )
                if (repeating) {
                    ClearButton {
                        repeat.value = 0
                        interval.value = 0
                        recurringUnits.value = 0
                    }
                }
            }
            ShowKeyboard(visible, focusRequester)
        }
    }

    @Composable
    fun AddRecurringReminder(
        openDialog: Boolean,
        interval: MutableState<Int>,
        units: MutableState<Int>,
        repeat: MutableState<Int>
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            CenteredH6(text = stringResource(id = R.string.repeats_plural, "").trim())
            val focusRequester = remember { FocusRequester() }
            OutlinedIntInput(
                time = interval,
                modifier = Modifier.focusRequester(focusRequester),
            )
            Spacer(modifier = Modifier.height(16.dp))
            options.forEachIndexed { index, option ->
                RadioRow(index, option, interval, units)
            }
            Divider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp)
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedIntInput(
                    time = repeat,
                    modifier = Modifier.weight(0.5f),
                    autoSelect = false,
                )
                BodyText(
                    text = LocalContext.current.resources.getQuantityString(
                        R.plurals.repeat_times,
                        repeat.value
                    ),
                    modifier = Modifier
                        .weight(0.5f)
                        .align(CenterVertically)
                )
            }

            ShowKeyboard(openDialog, focusRequester)
        }
    }

    private val options = listOf(
        R.plurals.reminder_minutes,
        R.plurals.reminder_hours,
        R.plurals.reminder_days,
        R.plurals.reminder_week,
    )

    private val MutableState<Int>.millis: Long
        get() = when (value) {
            1 -> TimeUnit.HOURS.toMillis(1)
            2 -> TimeUnit.DAYS.toMillis(1)
            3 -> TimeUnit.DAYS.toMillis(7)
            else -> TimeUnit.MINUTES.toMillis(1)
        }
}

@ExperimentalComposeUiApi
@Composable
fun ShowKeyboard(visible: Boolean, focusRequester: FocusRequester) {
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(visible) {
        focusRequester.freeFocus()
        awaitFrame()
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
fun OutlinedIntInput(
    time: MutableState<Int>,
    modifier: Modifier = Modifier,
    minValue: Int = 1,
    autoSelect: Boolean = true,
) {
    val value = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val text = time.value.toString()
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(0, if (autoSelect) text.length else 0)
            )
        )
    }
    OutlinedTextField(
        value = value.value,
        onValueChange = {
            value.value = it.copy(text = it.text.filter { t -> t.isDigit() })
            time.value = value.value.text.toIntOrNull() ?: 0
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.padding(horizontal = 16.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = MaterialTheme.colors.onSurface,
            focusedBorderColor = MaterialTheme.colors.onSurface
        ),
        isError = value.value.text.toIntOrNull()?.let { it < minValue } ?: true,
    )
}

@Composable
fun CenteredH6(@StringRes resId: Int) {
    CenteredH6(text = stringResource(id = resId))
}

@Composable
fun CenteredH6(text: String) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.h6
    )
}

@Composable
fun RadioRow(
    index: Int,
    option: Int,
    time: MutableState<Int>,
    units: MutableState<Int>,
    formatString: Int? = null,
) {
    val optionString = LocalContext.current.resources.getQuantityString(option, time.value)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { units.value = index }
    ) {
        RadioButton(
            selected = index == units.value,
            onClick = { units.value = index },
            modifier = Modifier.align(CenterVertically)
        )
        BodyText(
            text = if (index == units.value) {
                formatString
                    ?.let { stringResource(id = formatString, optionString) }
                    ?: optionString

            } else {
                optionString
            },
            modifier = Modifier.align(CenterVertically),
        )
    }
}

@Composable
fun BodyText(modifier: Modifier = Modifier, text: String) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.body1,
    )
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddCustomReminderOne() =
    MdcTheme {
        AddReminderDialog.AddCustomReminder(
            visible = true,
            time = remember { mutableStateOf(1) },
            units = remember { mutableStateOf(0) },
            interval = remember { mutableStateOf(0) },
            recurringUnits = remember { mutableStateOf(0) },
            repeat = remember { mutableStateOf(0) },
            showRecurring = {},
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddCustomReminder() =
    MdcTheme {
        AddReminderDialog.AddCustomReminder(
            visible = true,
            time = remember { mutableStateOf(15) },
            units = remember { mutableStateOf(1) },
            interval = remember { mutableStateOf(0) },
            recurringUnits = remember { mutableStateOf(0) },
            repeat = remember { mutableStateOf(0) },
            showRecurring = {},
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRepeatingReminderOne() =
    MdcTheme {
        AddReminderDialog.AddRecurringReminder(
            openDialog = true,
            interval = remember { mutableStateOf(1) },
            units = remember { mutableStateOf(0) },
            repeat = remember { mutableStateOf(1) },
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRepeatingReminder() =
    MdcTheme {
        AddReminderDialog.AddRecurringReminder(
            openDialog = true,
            interval = remember { mutableStateOf(15) },
            units = remember { mutableStateOf(1) },
            repeat = remember { mutableStateOf(4) },
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRandomReminderOne() =
    MdcTheme {
        AddReminderDialog.AddRandomReminder(
            visible = true,
            time = remember { mutableStateOf(1) },
            units = remember { mutableStateOf(0) }
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRandomReminder() =
    MdcTheme {
        AddReminderDialog.AddRandomReminder(
            visible = true,
            time = remember { mutableStateOf(15) },
            units = remember { mutableStateOf(1) }
        )
    }