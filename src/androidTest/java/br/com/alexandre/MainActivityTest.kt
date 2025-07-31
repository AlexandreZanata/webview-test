package br.com.alexandre

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testActivityLaunch() {
        // Test that the activity launches and WebView is displayed
        onView(withId(R.id.webView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testProgressBarExists() {
        // Test that progress bar exists
        onView(withId(R.id.progressBar))
            .check(matches(isCompletelyDisplayed()))
    }

    @Test
    fun testOfflineLayoutExists() {
        // Test that offline layout exists
        onView(withId(R.id.offlineView))
            .check(matches(isCompletelyDisplayed()))
    }

    @Test
    fun testWebViewSettings() {
        activityRule.scenario.onActivity { activity ->
            val webView = activity.findViewById<android.webkit.WebView>(R.id.webView)
            
            // Verify WebView settings are properly configured
            assert(webView.settings.javaScriptEnabled)
            assert(webView.settings.domStorageEnabled)
            assert(webView.settings.allowFileAccess)
            assert(webView.settings.allowContentAccess)
        }
    }

    @Test
    fun testJavaScriptInterfaceIsSet() {
        activityRule.scenario.onActivity { activity ->
            val webView = activity.findViewById<android.webkit.WebView>(R.id.webView)
            
            // Test that JavaScript interface is accessible
            webView.evaluateJavascript("typeof AndroidInterface !== 'undefined'") { result ->
                assert(result == "true")
            }
        }
    }
}