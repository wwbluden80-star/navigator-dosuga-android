package ru.navigatordosuga.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.navigatordosuga.app.ui.main.NavigatorRoot

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container=AppContainer.get(this)
        setContent { NavigatorRoot(container) }
    }
}
