package com.TheBudgeteers.dragonomics

import android.content.Context
import android.content.Intent
import android.os.Bundle

fun openIntent(context: Context, order: String, activityToOpen: Class<*>) {
    //Declare Intent with context and class to pass the value to
    val intent = Intent(context, activityToOpen)
    //pass through the string value with key "order"
    intent.putExtra("order", order)
    //if the context is not an activity, add FLAG_ACTIVITY_NEW_TASK
    if (context !is android.app.Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    //start the activity
    context.startActivity(intent)
}

fun shareIntent(context: Context, order: String) {
    val sendIntent = Intent()
    //set the action to indicate what to do
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.putExtra(Intent.EXTRA_TEXT, order)
    // we are sending plain text
    sendIntent.type = "text/plain"
    //show the share sheet
    val shareIntent = Intent.createChooser(sendIntent, null)
    //if the context is not an activity, add FLAG_ACTIVITY_NEW_TASK
    if (context !is android.app.Activity) {
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    //start the activity
    context.startActivity(shareIntent)

}