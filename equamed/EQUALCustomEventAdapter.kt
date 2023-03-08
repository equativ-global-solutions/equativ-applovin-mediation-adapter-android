package equamed

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapter.MaxAdViewAdapter
import com.applovin.mediation.adapter.MaxAdapter
import com.applovin.mediation.adapter.MaxAdapterError
import com.applovin.mediation.adapter.MaxInterstitialAdapter
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters
import com.applovin.mediation.adapters.MediationAdapterBase
import com.applovin.sdk.AppLovinSdkUtils.runOnUiThread
import com.applovin.sdk.AppLovinSdk

// Equativ(Smart) banner and interstitial imports
import com.smartadserver.android.library.exception.SASAdTimeoutException
import com.smartadserver.android.library.exception.SASNoAdToDeliverException
import com.smartadserver.android.library.model.SASAdElement
import com.smartadserver.android.library.ui.SASBannerView
import com.smartadserver.android.library.ui.SASInterstitialManager
import com.smartadserver.android.library.util.SASUtil

// Equativ(Smart) extra Util library
import equamed.EQUALCustomEventUtil.toPx


class EQUALCustomEventAdapter(private val sdk: AppLovinSdk) : MediationAdapterBase(sdk), MaxAdViewAdapter, MaxInterstitialAdapter {

    // Smart banner view that will handle the mediation ad call
    private var sasBannerView: SASBannerView? = null
    // Smart interstitial view that will handle the mediation ad call
    private var sasInterstitialView: SASInterstitialManager? = null


    override fun initialize(parameters: MaxAdapterInitializationParameters , activity: Activity , onCompletionListener: MaxAdapter.OnCompletionListener)
    {
        log("Initialize Equativ Mediation")
        onCompletionListener.onCompletion( MaxAdapter.InitializationStatus.DOES_NOT_APPLY, null );
    }

    override fun getSdkVersion(): String? {
        return EQUALCustomEventUtil.SDKVersionInfo.toString()
    }

    override fun getAdapterVersion(): String?
    {
        return "0.1.1"
    }


    //region MaxAdViewAdapter Methods
    override fun loadAdViewAd( parameters: MaxAdapterResponseParameters?, adFormat: MaxAdFormat?, activity: Activity?, listener: MaxAdViewAdapterListener? )
    {
        runOnUiThread {
            fun requestBannerAd( context: Context, customEventBannerListener: MaxAdViewAdapterListener, placement: String?, adFormat: MaxAdFormat?, mediationParameters: MaxAdapterParameters, bundle: Bundle?, targeting: String? ) {

                // get the smart placement object
                val placementString = placement?: ""
                // get the targeting string
                val targeting = targeting?: ""

                // Configure the Smart Display SDK and retrieve the ad placement.
                val adPlacement = EQUALCustomEventUtil.configureSDKAndGetAdPlacement( context, placementString,targeting )

                // test if the ad placement is valid
                if ( adPlacement == null ) {
                    // incorrect smart placement : exit in error
                    customEventBannerListener.onAdViewAdLoadFailed(MaxAdapterError.INVALID_LOAD_STATE)
                    return
                }
                // check if sasBannerView is valid
                if (sasBannerView != null) {
                    // Quit if there is already a banner being handled.
                    return
                }

                // Instantiate the SASBannerView
                sasBannerView = object : SASBannerView(context) {
                    /**
                     * Overriden to force banner size to received AppLovin size
                     * @param params
                     */
                    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
                        if (!this.isExpanded) {

                            params.height = adFormat?.size?.height!!.toPx()
                            params.width = adFormat?.size?.width!!.toPx()
                        }
                        super.setLayoutParams(params)
                    }
                }
                // Set a listener on the SASBannerView
                sasBannerView?.bannerListener = object : SASBannerView.BannerListener {
                    // get a Handler on the main thread to execute code on this thread
                    var handler = SASUtil.getMainLooperHandler()

                    override fun onBannerAdLoaded(bannerView: SASBannerView, sasAdElement: SASAdElement) {
                        // Smart banner ad was successfully loaded
                        handler.post { customEventBannerListener.onAdViewAdLoaded(bannerView) }
                    }

                    override fun onBannerAdFailedToLoad(sasBannerView: SASBannerView, e: Exception) {
                        // Smart banner ad failed to load
                        handler.post {
                            var errorCode = MaxAdapterError.ERROR_CODE_INTERNAL_ERROR
                            var errorMessage = e.message ?: ""
                            if (e is SASNoAdToDeliverException) {
                                // no ad to deliver
                                errorCode = MaxAdapterError.ERROR_CODE_NO_FILL
                                errorMessage = "No ad to deliver"
                            } else if (e is SASAdTimeoutException) {
                                // ad request timeout translates to admob network error
                                errorCode = MaxAdapterError.ERROR_CODE_NO_CONNECTION
                                errorMessage = "Timeout while waiting ad call response"
                            }
                            customEventBannerListener.onAdViewAdLoadFailed(MaxAdapterError.AD_DISPLAY_FAILED)
                        }
                    }

                    override fun onBannerAdClicked(sasBannerView: SASBannerView) {
                        // Smart banner ad was clicked
                        handler.post { customEventBannerListener.onAdViewAdClicked() }
                    }

                    override fun onBannerAdExpanded(sasBannerView: SASBannerView) {
                        // Smart banner ad was displayed full screen
                        handler.post { customEventBannerListener.onAdViewAdExpanded() }
                    }

                    override fun onBannerAdCollapsed(sasBannerView: SASBannerView) {
                        // Smart banner ad was restored to its default state
                        handler.post { customEventBannerListener.onAdViewAdCollapsed() }
                    }

                    override fun onBannerAdResized(sasBannerView: SASBannerView) {
                        // nothing to do here
                    }

                    override fun onBannerAdClosed(sasBannerView: SASBannerView) {
                        // nothing to do here
                    }

                    override fun onBannerAdVideoEvent(sasBannerView: SASBannerView, i: Int) {
                        // nothing to do here
                    }
                }
                // Now request ad for this SASBannerView
                sasBannerView?.loadAd(adPlacement)
            }

            /* Data needed for the Ad Request to Equativ */

            // application context
            var context = applicationContext
            // set placement from customParameters
            var placementString: String? = parameters?.customParameters?.getString("siteid").toString() + "/" + parameters?.customParameters?.getString("pageid").toString() + "/" + parameters?.customParameters?.getString("formatid").toString()
            // set targeting from customParameters
            var targeting: String? = parameters?.customParameters?.getString("targeting").toString()

            // call requestBannerAd for sending ad-call to Equativ
            if (listener != null && parameters != null) {
                requestBannerAd(context, listener, placementString, adFormat, parameters, parameters.customParameters, targeting)
            }
        }
    }


    /*
    *   INTERSTITIAL AD MEDITATION METHODS
    *   ==================================
    */

    override fun loadInterstitialAd(parameters: MaxAdapterResponseParameters, activity: Activity, listener: MaxInterstitialAdapterListener) {
        runOnUiThread {
            fun requestInterstitialAd(context: Context, customEventInterstitialListener: MaxInterstitialAdapterListener, placement: String?, mediationParameters: MaxAdapterParameters, bundle: Bundle?, targeting: String? ) {
                // get the smart placement object
                val placementString = placement?: ""
                // get the targeting string
                val targeting = targeting?: ""

                // Configure the Smart Display SDK and retrieve the ad placement.
                val adPlacement = EQUALCustomEventUtil.configureSDKAndGetAdPlacement( context, placementString,targeting )

                // test if the ad placement is valid
                if ( adPlacement == null ) {
                    // incorrect smart placement : exit in error
                    customEventInterstitialListener.onInterstitialAdLoadFailed(MaxAdapterError.INVALID_LOAD_STATE)
                    return
                }
                // check if sasBannerView is valid
                if ( sasInterstitialView != null ) {
                    // Quit if there is already a banner being handled.
                    return
                }

                // instantiate a Smart interstitial manager
                sasInterstitialView = SASInterstitialManager(context, adPlacement)

                sasInterstitialView?.interstitialListener = object : SASInterstitialManager.InterstitialListener {

                    // get a Handler on the main thread to execute code on this thread
                    var handler = SASUtil.getMainLooperHandler()

                    override fun onInterstitialAdLoaded(
                        p0: SASInterstitialManager,
                        p1: SASAdElement
                    ) {
                        log("Listener -> Loaded")

                        handler.post { customEventInterstitialListener.onInterstitialAdLoaded() }
                    }

                    override fun onInterstitialAdFailedToLoad(
                        p0: SASInterstitialManager,
                        e: java.lang.Exception
                    ) {
                        // Smart banner ad failed to load
                        handler.post {
                            var errorCode = MaxAdapterError.ERROR_CODE_INTERNAL_ERROR
                            var errorMessage = e.message ?: ""
                            if (e is SASNoAdToDeliverException) {
                                // no ad to deliver
                                errorCode = MaxAdapterError.ERROR_CODE_NO_FILL
                                errorMessage = "No ad to deliver"
                            } else if (e is SASAdTimeoutException) {
                                // ad request timeout translates to admob network error
                                errorCode = MaxAdapterError.ERROR_CODE_NO_CONNECTION
                                errorMessage = "Timeout while waiting ad call response"
                            }
                            log("Listener -> Failed")
                            customEventInterstitialListener.onInterstitialAdLoadFailed(MaxAdapterError.AD_DISPLAY_FAILED)
                        }
                    }

                    override fun onInterstitialAdShown(p0: SASInterstitialManager) {
                        // Smart interstitial ad was displayed (full screen)
                        log("Listener -> Shown")
                        handler.post { customEventInterstitialListener.onInterstitialAdDisplayed() }
                    }

                    override fun onInterstitialAdFailedToShow(
                        p0: SASInterstitialManager,
                        p1: java.lang.Exception
                    ) {
                        // Smart interstitial ad failed to show
                        log("Listener -> Show failed")
                        handler.post { customEventInterstitialListener.onInterstitialAdDisplayFailed(MaxAdapterError.AD_DISPLAY_FAILED) }
                    }

                    override fun onInterstitialAdClicked(p0: SASInterstitialManager) {
                        // Smart interstitial ad was clicked
                        handler.post { customEventInterstitialListener.onInterstitialAdClicked() }
                    }

                    override fun onInterstitialAdDismissed(p0: SASInterstitialManager) {
                        // Smart interstitial ad was closed
                        handler.post { customEventInterstitialListener.onInterstitialAdHidden() }
                    }

                    override fun onInterstitialAdVideoEvent(p0: SASInterstitialManager, p1: Int) {
                        // nothing to do
                    }

                }
                sasInterstitialView?.loadAd()

            }
            /* Data needed for the Ad Request to Equativ */
            // application context
            var context = applicationContext
            // set placement from customParameters
            var placementString: String? = parameters?.customParameters?.getString("siteid").toString() + "/" + parameters?.customParameters?.getString("pageid").toString() + "/" + parameters?.customParameters?.getString("formatid").toString()
            log("Placement -> $placementString")
            // set targeting from customParameters
            var targeting: String? = parameters?.customParameters?.getString("targeting").toString()

            // call requestInterstitialAd for sending ad-call to Equativ
            if (listener != null && parameters != null) {
                log("Requesting Interstitial to Equativ")
                requestInterstitialAd(context, listener, placementString, parameters, parameters.customParameters, targeting)
            }
        }

    }

    override fun showInterstitialAd(parameters: MaxAdapterResponseParameters, activity: Activity, listener: MaxInterstitialAdapterListener ) {
        runOnUiThread {
            sasInterstitialView?.show()
        }
    }

    @Synchronized
    override fun onDestroy() {
        if ( sasBannerView == null ) {
            sasBannerView?.onDestroy()
            sasBannerView = null
        }

        if ( sasInterstitialView == null ) {
            sasInterstitialView?.onDestroy()
            sasInterstitialView = null
        }

    }

}
