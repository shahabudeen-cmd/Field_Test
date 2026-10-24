package com.commissioning.cctv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Root(vm)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.flush()
    }
}

@Composable
private fun Root(vm: MainViewModel) {
    val sl = vm.selectedSl
    if (sl != null) {
        BackHandler { vm.close() }
        DetailScreen(vm, sl)
    } else {
        HomeScreen(vm)
    }
}
