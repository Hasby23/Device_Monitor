package com.example.devicemonitor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ResultScreen(
    viewModel: RecordViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.records.collectAsState()
    var selectedRecord by remember { mutableStateOf<Record?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (selectedRecord != null){
            DetailScreen(
                modifier = modifier,
                onBack = { selectedRecord = null},
                record = selectedRecord!!
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                items(records, key = { it.id }) { record ->
                    Card(
                        onClick = { selectedRecord = record },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Column(modifier = Modifier) {
                                Text(
                                    text = "Start: ${formatTimestamp(record.timestamp.first())}, Duration: ${(record.timestamp.last() - record.timestamp.first()).milliseconds}",
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 4.dp)
                                )
                                Text(
                                    text = "Battery: ${record.batteryPercent.first()}% " +
                                            "to ${record.batteryPercent.last()}%",
                                    modifier = Modifier.padding(horizontal = 4.dp)

                                )
                                Text(
                                    text = "Temperature: ${record.batteryTemperature.min()}°C " +
                                            "to ${record.batteryTemperature.max()}°C",
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Text(
                                    text = "Frame Rate: ${record.fps.min()} FPS " +
                                            "to ${record.fps.max()} FPS",
                                    modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                                )
                                Text(
                                    text = "Size: ${record.timestamp.size}, ${record.batteryPercent.size}, ${record.batteryTemperature.size}, ${record.fps.size}, ",
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, end = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { viewModel.deleteRecord(record) },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.align(Alignment.Bottom)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun DetailScreen(
    record: Record,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
){
    BackHandler(enabled = true) {
        onBack()
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        IconButton(
            onClick =  onBack,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go Back"
            )
        }
        Column(
            modifier = Modifier.padding(8.dp)
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "Start: ${formatTimestamp(record.timestamp.first())}",
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 4.dp)
                )
                Text(
                    text = "End: ${formatTimestamp(record.timestamp.last())}",
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = "Duration: ${(record.timestamp.last() - record.timestamp.first()).milliseconds}",
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = "Battery: ${record.batteryPercent.first()}% " +
                            "to ${record.batteryPercent.last()}%",
                    modifier = Modifier.padding(horizontal = 4.dp)

                )
                Text(
                    text = "Temperature: ${record.batteryTemperature.min()}°C " +
                            "to ${record.batteryTemperature.max()}°C",
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = "Frame Rate: ${record.fps.min()} FPS " +
                            "to ${record.fps.max()} FPS",
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                )
                Text(
                    text = "Size: ${record.timestamp.size}, ${record.batteryPercent.size}, ${record.batteryTemperature.size}, ${record.fps.size}, ",
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, end = 4.dp)
                )
            }
        }
    }
}