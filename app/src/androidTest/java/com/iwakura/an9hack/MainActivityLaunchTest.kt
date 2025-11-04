package com.iwakura.an9hack

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLaunchTest {
    @Test
    fun launches_main_activity() {
        ActivityScenario.launch(MainActivity::class.java).use { }
    }
}


