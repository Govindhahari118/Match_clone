package com.match.app.core.review

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Play In-App Review prompts.
 *
 * Guidelines for when to trigger:
 * - After a mutual match
 * - After 7+ days of active use
 * - After completing profile (high engagement signal)
 * - Maximum once per 30 days
 */
@Singleton
class InAppReviewManager @Inject constructor() {

    companion object {
        private const val TAG = "InAppReview"
    }

    fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    Log.d(TAG, "Review flow completed")
                }
            } else {
                Log.w(TAG, "Review request failed", task.exception)
            }
        }
    }
}
