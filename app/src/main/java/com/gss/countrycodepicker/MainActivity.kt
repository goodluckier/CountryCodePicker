package com.gss.countrycodepicker

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var ll_country_code = findViewById<View>(R.id.ll_country_code)
        var tv_country_code = findViewById<View>(R.id.tv_country_code)

        ll_country_code.setOnClickListener{

        }
    }
}