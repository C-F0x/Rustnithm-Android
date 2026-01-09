package org.cf0x.rustnithm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Page.Bon
import org.cf0x.rustnithm.Page.Jour
import org.cf0x.rustnithm.Theme.RustnithmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val dataManager: DataManager = viewModel(
                factory = DataManager.Factory(context)
            )
            val themeMode by dataManager.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                0 -> false
                1 -> true
                else -> isSystemInDarkTheme()
            }

            RustnithmTheme(darkTheme = darkTheme) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedPage by remember { mutableIntStateOf(0) }
    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(paddingValues)) {
                Crossfade(targetState = selectedPage, label = "pageTransition") { page ->
                    when (page) {
                        0 -> Bon()
                        1 -> Jour()
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "Rustnithm",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            modifier = Modifier.height(32.dp),
                            selected = selectedPage == 0,
                            onClick = { selectedPage = 0 },
                            label = { Text("Bon") },
                            border = null
                        )
                        FilterChip(
                            modifier = Modifier.height(32.dp),
                            selected = selectedPage == 1,
                            onClick = { selectedPage = 1 },
                            label = { Text("Jour") },
                            border = null
                        )
                    }
                }
            }
        }
    }
}