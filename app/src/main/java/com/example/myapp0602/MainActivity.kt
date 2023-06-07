package com.example.myapp0602

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapp0602.ui.theme.MyApp0602Theme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val KEY_MEDICATION_NAME = "medication_name"
    private val KEY_DOSAGE = "dosage"
    private val KEY_FREQUENCY = "frequency"
    private val KEY_TIME = "time"
    private val SHARED_PREFS_NAME = "my_app_shared_prefs"
    private val channelId = "medication_reminder"
    private val notificationId = 1
    private val historyDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val medications = mutableStateListOf<Medication>()
    private val medicationNameState = mutableStateOf("")
    private val dosageState = mutableStateOf("")
    private val frequencyState = mutableStateOf(Frequency.DAILY)
    private val timeState = mutableStateOf(Date())
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        createNotificationChannel()
        medications.addAll(getSavedMedications())
        setContent {
            MyApp0602Theme {
                MedicationReminderScreen()
            }
        }
    }
    private fun getSavedMedications(): List<Medication> {
        val savedMedications = mutableListOf<Medication>()

        val medicationName = sharedPreferences.getString(KEY_MEDICATION_NAME, "")
        val dosage = sharedPreferences.getString(KEY_DOSAGE, "")
        val frequency = sharedPreferences.getString(KEY_FREQUENCY, Frequency.DAILY.name)
        val timeMillis = sharedPreferences.getLong(KEY_TIME, 0L)

        if (medicationName != null && dosage != null && frequency != null && timeMillis > 0L) {
            val time = Date(timeMillis)
            val savedMedication = Medication(medicationName, dosage, Frequency.valueOf(frequency), time)
            savedMedications.add(savedMedication)
        }

        return savedMedications
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "藥物提醒"
            val descriptionText = "藥物提醒通知"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(message: String) {
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle("藥物提醒")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, notificationBuilder.build())
        }
    }

    private fun recordMedicationTaken(medication: Medication) {
        val timestamp = historyDateFormat.format(Date())
        val historyEntry = "$timestamp - ${medication.name}"
        println(historyEntry)
        // 顯示通知
        val reminderMessage = "已服用藥物: ${medication.name}"
        showNotification(reminderMessage)
    }

    @Composable
    fun MedicationReminderScreen() {
        Surface(color = MaterialTheme.colors.background) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                MedicationInputFields()

                Spacer(modifier = Modifier.height(16.dp))

                MedicationList()
            }
        }
    }

    @Composable
    fun MedicationInputFields() {
        Column {
            OutlinedTextField(
                value = medicationNameState.value,
                onValueChange = { medicationNameState.value = it },
                label = { Text("藥物名稱") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = dosageState.value,
                onValueChange = { dosageState.value = it },
                label = { Text("劑量") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("頻率: ")
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.wrapContentSize()) {
                    Text(
                        text = frequencyState.value.name,
                        modifier = Modifier
                            .clickable { expanded = true }
                            .padding(horizontal = 8.dp)
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DropdownMenuItem(onClick = {
                            frequencyState.value = Frequency.DAILY
                            expanded = false
                        }) {
                            Text("每日")
                        }
                        DropdownMenuItem(onClick = {
                            frequencyState.value = Frequency.WEEKLY
                            expanded = false
                        }) {
                            Text("每週")
                        }
                        DropdownMenuItem(onClick = {
                            frequencyState.value = Frequency.MONTHLY
                            expanded = false
                        }) {
                            Text("每月")
                        }
                    }
                }
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TODO: 實作時間選擇器
            Text("時間: ${timeState.value}")

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { addMedication() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("新增藥物")
            }
        }
    }


    @Composable
    fun MedicationList() {
        LazyColumn {
            items(medications) { medication ->
                MedicationItem(medication)
            }
        }
    }
    private fun saveMedication(medication: Medication) {
        with(sharedPreferences.edit()) {
            putString(KEY_MEDICATION_NAME, medication.name)
            putString(KEY_DOSAGE, medication.dosage)
            putString(KEY_FREQUENCY, medication.frequency.name)
            putLong(KEY_TIME, medication.time.time)
            apply()
        }
    }
    @Composable
    fun MedicationItem(medication: Medication) {
        val updatedMedication = remember { mutableStateOf(medication) }
        val saveMedicationEffect = rememberUpdatedState(updatedMedication.value)

        LaunchedEffect(saveMedicationEffect.value) {
            saveMedication(saveMedicationEffect.value)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp,
            backgroundColor = MaterialTheme.colors.surface,
            contentColor = MaterialTheme.colors.onSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("藥物名稱: ${updatedMedication.value.name}")
                Text("劑量: ${updatedMedication.value.dosage}")
                Text("頻率: ${updatedMedication.value.frequency}")
                Text("時間: ${historyDateFormat.format(updatedMedication.value.time)}")

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showNotification(getReminderMessage(updatedMedication.value)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("設置提醒")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        recordMedicationTaken(updatedMedication.value)
                        updatedMedication.value = updatedMedication.value.copy(dosage = (updatedMedication.value.dosage.toInt() - 1).toString())
                        saveMedication(updatedMedication.value) // 儲存更新後的資料
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("確認已服用藥物")
                }
            }

        }
    }



    private fun addMedication() {
        val medication = Medication(
            medicationNameState.value,
            dosageState.value,
            frequencyState.value,
            timeState.value
        )

        medications.add(medication)
        saveMedication(medication)
        // 清空輸入欄位
        medicationNameState.value = ""
        dosageState.value = ""
        frequencyState.value = Frequency.DAILY
        timeState.value = Date()
    }

//    private fun getReminderMessage(medication: Medication): String {
//        return "提醒: 在 ${historyDateFormat.format(medication.time)} ${medication.frequency} 時服用 ${medication.name} - ${medication.dosage}"
//    }
private fun getReminderMessage(medication: Medication): String {
    val calendar = Calendar.getInstance()
    calendar.time = medication.time
    calendar.add(Calendar.DAY_OF_MONTH, 1) // 將日期加一天

    val nextDay = calendar.time
    val nextDayFormatted = historyDateFormat.format(nextDay)

    return "請在${nextDayFormatted} 時服用藥物 ${medication.name} 剩餘藥量:${medication.dosage}"
}



    data class Medication(
        val name: String,
        val dosage: String,
        val frequency: Frequency,
        val time: Date
    )

    enum class Frequency {
        DAILY,
        WEEKLY,
        MONTHLY
    }
}
