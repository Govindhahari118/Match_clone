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
 * Single process-wide Play Billing connection for all Play-distributed digital entitlements.
 * Billing is only a purchase transport: every PURCHASED token is verified by the authenticated
 * Firebase backend before membership or boost state is granted.
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
        data class BoostActivated(val boostId: String, val boostUntil: Long) : Event
        data class Pending(val productId: String) : Event
        data object Cancelled : Event
        data class Error(val message: String) : Event
    }

    private data class CatalogEntry(
        val entitlementId: String,
        val boost: Boolean
    )

    private data class Purchasable(
        val offer: Offer,
        val productDetails: ProductDetails,
        val offerToken: String?
    )

    companion object {
        const val SILVER_PRODUCT_ID = "match_silver_3m"
        const val GOLD_PRODUCT_ID = "match_gold_6m"
        const val PLATINUM_PRODUCT_ID = "match_platinum_12m"
        const val BOOST_3H_PRODUCT_ID = "match_boost_3h"
        const val BOOST_24H_PRODUCT_ID = "match_boost_24h"
        const val BOOST_7D_PRODUCT_ID = "match_boost_7d"

        const val BOOST_3H_ID = "BOOST_3H"
        const val BOOST_24H_ID = "BOOST_24H"
        const val BOOST_7D_ID = "BOOST_7D"

        private val PRODUCT_CATALOG = mapOf(
            SILVER_PRODUCT_ID to CatalogEntry("SILVER_3M", false),
            GOLD_PRODUCT_ID to CatalogEntry("GOLD_6M", false),
            PLATINUM_PRODUCT_ID to CatalogEntry("PLATINUM_12M", false),
            BOOST_3H_PRODUCT_ID to CatalogEntry(BOOST_3H_ID, true),
            BOOST_24H_PRODUCT_ID to CatalogEntry(BOOST_24H_ID, true),
            BOOST_7D_PRODUCT_ID to CatalogEntry(BOOST_7D_ID, true)
        )

        private val BOOST_PRODUCT_IDS = setOf(
            BOOST_3H_PRODUCT_ID,
            BOOST_24H_PRODUCT_ID,
            BOOST_7D_PRODUCT_ID
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
    }

    fun refresh() {
        if (!billingClient.isReady) {
            connect()
            return
        }
        queryProducts()
        recoverPurchases()
    }

    fun isBoostProduct(productId: String): Boolean = productId in BOOST_PRODUCT_IDS

    private fun queryProducts() {
        val products = PRODUCT_CATALOG.keys.map { productId ->
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
                _events.tryEmit(Event.Error(billingMessage(result, "Unable to load Google Play prices")))
                return@queryProductDetailsAsync
            }

            val next = linkedMapOf<String, Purchasable>()
            detailsResult.productDetailsList.forEach { details ->
                val catalog = PRODUCT_CATALOG[details.productId] ?: return@forEach
                val offerDetails = details.oneTimePurchaseOfferDetailsList?.firstOrNull() ?: return@forEach
                val offer = Offer(
                    planId = catalog.entitlementId,
                    productId = details.productId,
                    formattedPrice = offerDetails.formattedPrice,
                    priceCurrencyCode = offerDetails.priceCurrencyCode,
                    priceAmountMicros = offerDetails.priceAmountMicros
                )
                next[catalog.entitlementId] = Purchasable(offer, details, offerDetails.offerToken)
            }
            purchasables.clear()
            purchasables.putAll(next)
            _offers.value = next.mapValues { it.value.offer }
        }
    }

    fun launchPurchase(activity: Activity, planId: String): BillingResult {
        if (!billingClient.isReady) {
            connect()
            return errorResult(
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                "Google Play billing is reconnecting"
            )
        }
        val uid = auth.currentUser?.uid
            ?: return errorResult(BillingClient.BillingResponseCode.ERROR, "Sign in before purchasing")
        val selected = purchasables[planId]
            ?: return errorResult(
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                "This item is not available from Google Play"
            )

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(selected.productDetails)
        selected.offerToken?.takeIf { it.isNotBlank() }?.let(productParamsBuilder::setOfferToken)
        val productParams = productParamsBuilder.build()
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
        val catalog = productId?.let(PRODUCT_CATALOG::get)
        if (productId == null || catalog == null) {
            _events.tryEmit(Event.Error("Google Play returned an unknown app product."))
            return
        }

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> _events.tryEmit(Event.Pending(productId))
            Purchase.PurchaseState.PURCHASED -> verifyPurchasedToken(
                productId,
                catalog.entitlementId,
                purchase.purchaseToken
            )
            else -> Unit
        }
    }

    private fun verifyPurchasedToken(productId: String, entitlementId: String, purchaseToken: String) {
        if (!inFlightTokens.add(purchaseToken)) return
        scope.launch {
            try {
                val result = subscriptionRepository
                    .verifyGooglePlayPurchase(productId, purchaseToken)
                    .getOrThrow()
                if (result.entitlementType == "BOOST") {
                    _events.emit(
                        Event.BoostActivated(
                            result.entitlementId.ifBlank { entitlementId },
                            result.expiresAt
                        )
                    )
                } else {
                    _events.emit(
                        Event.Activated(
                            result.entitlementId.ifBlank { entitlementId },
                            result.expiresAt
                        )
                    )
                }
                refresh()
            } catch (_: Exception) {
                _events.emit(
                    Event.Error(
                        "Purchase verification is pending. Do not pay again; reopen the app to retry securely."
                    )
                )
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
