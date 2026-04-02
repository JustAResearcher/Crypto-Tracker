package com.cryptotracker.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cryptotracker.CryptoApp
import com.cryptotracker.MainActivity
import com.cryptotracker.R
import com.cryptotracker.repository.CryptoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@HiltWorker
class PriceAlertWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CryptoRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val activeAlerts = repository.getActiveAlertsList()
            if (activeAlerts.isEmpty()) return Result.success()

            // Group alerts by coinId to minimize API calls
            val coinIds = activeAlerts.map { it.coinId }.distinct()
            val coins = repository.getCoinsByIds(coinIds)
            val priceMap = coins.associate { it.id to (it.currentPrice ?: 0.0) }

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
                currency = Currency.getInstance("USD")
                maximumFractionDigits = 2
            }

            for (alert in activeAlerts) {
                val currentPrice = priceMap[alert.coinId] ?: continue

                val triggered = if (alert.isAbove) {
                    currentPrice >= alert.targetPrice
                } else {
                    currentPrice <= alert.targetPrice
                }

                if (triggered) {
                    repository.markAlertTriggered(alert.id)
                    sendNotification(
                        alertId = alert.id,
                        title = "Price Alert: ${alert.coinName}",
                        message = "${alert.coinName} is now ${currencyFormat.format(currentPrice)} " +
                                "(target: ${currencyFormat.format(alert.targetPrice)})"
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(alertId: Int, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alertId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CryptoApp.ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(alertId, notification)
    }
}
