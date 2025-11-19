package com.example.myapplication.activity

import android.content.Context
import android.content.Intent
import android.graphics.pdf.content.PdfPageGotoLinkContent
import android.icu.util.Currency
import androidx.appcompat.app.AppCompatActivity

fun  switchToActivity(
    context: Context,
    destinationActivity: Class<out AppCompatActivity>,
    finishCurrent: Boolean = false
){
    val intent = Intent(context, destinationActivity)

    context.startActivity(intent)

    if(finishCurrent && context is AppCompatActivity){
        context.finish()
    }
}