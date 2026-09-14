package com.match.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import com.match.app.data.repo.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single process-wide Play Billing connection for the Play-distributed app.
 *
 * Billing is intentionally only a purchase transport. Entitlements are never granted here;
 * every PURCHASED token is verified by the authenticated Firebase backend against the Google
 * Play Developer API before local premium state is refreshed.
 */
@Singleton
class PlayBillingManager @Inject constructor(
    @ApplicationContext context: Context,
    private val subscriptionRepository: SubscriptionRepository
) : PurchasesUpdatedListener, BillingClientStateListener {

    data class Offer(
        val planId: String,
        val productId: String,
        val formattedPrice: String,
        val priceCurrencyCode: String,
        val priceAmountMicros: Long
    )

    sealed interface Event {
        data class Activated(val planId: String, val premiumUntil: Long) : Event
        data class Pending(val productId: String) : Event
        data object Cancelled : Event
        data class Error(val message: String) : Event
    }

    private data class Purchasable(
        val offer: Offer,
        val productDetails: ProductDetails,
        val offerToken: String
    )

    companion object {
        const val SILVER_PRODUCT_ID = "match_silver_3m"
        const val GOLD_PRODUCT_ID = "match_gold_6m"
        const val PLATINUM_PRODUCT_ID = "match_platinum_12m"

        private val PRODUCT_TO_PLAN = mapOf(
            SILVER_PRODUCT_ID to "SILVER_3M",
            GOLD_PRODUCT_ID to "GOLD_6M",
            PLATINUM_PRODUCT_ID to "PLATINUM_12M"
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val auth = FirebaseAuth.getInstance()
    private val inFlightTokens = ConcurrentHashMap.newKeySet<String>()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _offers = MutableStateFlow<Map<String, Offer>>(emptyMap())
    val offers: StateFlow<Map<String, Offer>> = _offers.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 16)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    @Volatile
    private var connecting = false

    private val purchasables = ConcurrentHashMap<String, Purchasable>()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    @Synchronized
    fun connect() {
        if (billingClient.isReady) {
            _ready.value = true
            refresh()
            return
        }
        if (connecting) return
        connecting = true
        billingClient.startConnection(this)
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        connecting = false
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            _ready.value = true
            refresh()
        } else {
            _ready.value = false
            _events.tryEmit(Event.Error(billingMessage(result, "Google Play billing is unavailable")))
        }
    }

    override fun onBillingServiceDisconnected() {
        connecting = false
        _ready.value = false
        // enableAutoServiceReconnection() handles later API calls. connect() is also invoked from
        // Activity.onResume so a foreground session proactively restores catalogue/recovery state.
    }

    fun refresh() {
        if (!billingClient.isReady) {
            connect()
            return
        }
        queryProducts()
        recoverPurchases()
    }

    private fun queryProducts() {
        val products = PRODUCT_TO_PLAN.keys.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _events.tryEmit(Event.Error(billingMessage(result, "Unable to load membership prices")))
                return@queryProductDetailsAsync
            }

            val next = linkedMapOf<String, Purchasable>()
            detailsResult.productDetailsList.forEach { details ->
                val planId = PRODUCT_TO_PLAN[details.productId] ?: return@forEach
                val offerDetails = details.oneTimePurchaseOfferDetailsList.firstOrNull() ?: return@forEach
                val offer = Offer(
                    planId = planId,
                    productId = details.productId,
                    formattedPrice = offerDetails.formattedPrice,
                    priceCurrencyCode = offerDetails.priceCurrencyCode,
                    priceAmountMicros = offerDetails.priceAmountMicros
                )
                next[planId] = Purchasable(offer, details, offerDetails.offerToken)
            }
            purchasables.clear()
            purchasables.putAll(next)
            _offers.value = next.mapValues { it.value.offer }
        }
    }

    fun launchPurchase(activity: Activity, planId: String): BillingResult {
        if (!billingClient.isReady) {
            connect()
            return errorResult(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, "Google Play billing is reconnecting")
        }
        val uid = auth.currentUser?.uid
            ?: return errorResult(BillingClient.BillingResponseCode.ERROR, "Sign in before purchasing")
        val selected = purchasables[planId]
            ?: return errorResult(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, "Membership is not available from Google Play")

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(selected.productDetails)
            .setOfferToken(selected.offerToken)
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .setObfuscatedAccountId(accountHash(uid))
            .build()

        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            recoverPurchases()
        } else if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _events.tryEmit(Event.Error(billingMessage(result, "Unable to start purchase")))
        }
        return result
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach(::processPurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> _events.tryEmit(Event.Cancelled)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> recoverPurchases()
            else -> _events.tryEmit(Event.Error(billingMessage(result, "Purchase was not completed")))
        }
    }

    private fun recoverPurchases() {
        if (!billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach(::processPurchase)
            }
        }
    }

    private fun processPurchase(purchase: Purchase) {
        val productId = purchase.products.singleOrNull()
        val planId = productId?.let(PRODUCT_TO_PLAN::get)
        if (productId == null || planId == null) {
            _events.tryEmit(Event.Error("Google Play returned an unknown membership product."))
            return
        }

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> _events.tryEmit(Event.Pending(productId))
            Purchase.PurchaseState.PURCHASED -> verifyPurchasedToken(productId, planId, purchase.purchaseToken)
            else -> Unit
        }
    }

    private fun verifyPurchasedToken(productId: String, planId: String, purchaseToken: String) {
        if (!inFlightTokens.add(purchaseToken)) return
        scope.launch {
            try {
                val result = subscriptionRepository.verifyGooglePlayPurchase(productId, purchaseToken).getOrThrow()
                _events.emit(Event.Activated(result.planId.ifBlank { planId }, result.premiumUntil))
                // The server consumes the one-time product after the entitlement transaction.
                // Refreshing lets Play remove the consumed item and catches a server-side consume
                // retry if the first consume call was temporarily unavailable.
                refresh()
            } catch (_: Exception) {
                _events.emit(Event.Error("Purchase verification is pending. Do not pay again; reopen the app to retry securely."))
            } finally {
                inFlightTokens.remove(purchaseToken)
            }
        }
    }

    private fun billingMessage(result: BillingResult, fallback: String): String {
        return result.debugMessage.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun errorResult(code: Int, message: String): BillingResult = BillingResult.newBuilder()
        .setResponseCode(code)
        .setDebugMessage(message)
        .build()

    private fun accountHash(uid: String): String = MessageDigest.getInstance("SHA-256")
        .digest(uid.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
