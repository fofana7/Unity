package com.example.unity

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val myWebView: WebView = findViewById(R.id.webview)
        
        // Configuration avancée du WebView pour supporter le JS moderne
        val settings: WebSettings = myWebView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true // Important pour Tailwind et les apps JS modernes
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        
        myWebView.webViewClient = WebViewClient()
        
        // Charger le fichier local
        myWebView.loadUrl("file:///android_asset/index.html")
    }

    // Gérer le bouton retour pour naviguer dans l'historique web
    override fun onBackPressed() {
        val myWebView: WebView = findViewById(R.id.webview)
        if (myWebView.canGoBack()) {
            myWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}