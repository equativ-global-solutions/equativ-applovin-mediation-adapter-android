Equativ - AppLovin SDK - Custom Adapter
==============================================
> **DISCLAIMER:** This solution has not been validated by the Equativ Product team. The Equativ's Product team cannot be held responsible for any potential issues arising from this integration.<br>
> This solution should be considered as a *customized workaround* provided by Equativ's Global Solutions team for the integration of the Equativ(Smart) SDK.

Introduction
------------
The _Smart Display SDK_ can be utilized for banner and interstitial ads through the adapter provided in this repository when using the  _AppLovin SDK_. These adapters are compatible with _Smart Display SDK_ v7.0.
> **Note:** Throughout this document, Equativ refers to Smart/Smart Ad Server as its former name.


Setup
-----

1) Install the _AppLovin SDK_ according to the official [documentation](https://dash.applovin.com/documentation/mediation/android/getting-started/integration).

2) Install the _Smart Display SDK_ by adding the ```smart-display-sdk``` dependency to your _gradle_ file (more info in [the documentation](https://documentation.smartadserver.com/displaySDK/android/gettingstarted.html)).

3) Copy the _Custom Event Classes_ into your Android project, including the ```equamed``` folder as a package to enable proper referencing during the integration process.

    * ```EQUALCustomEventAdapter```
    * ```EQUALCustomEventUtil```

4) In your AppLovin interface, Equativ should be included as part of the mediation waterfall. It is needed to set up a _Custom Network_ as part of the Mediation Networks.

    In order to deliver Equativ(Smart) ads on an AppLovin Ad Unit, it is needed to follow two main steps:

    * Indicate the _Android / Fire OS Adapter Class Name_: set `equamed.EQUALCustomEventAdapter` in order to associate the network with Equativ.
    * Integrate the _Cutom Network_ as part of the _Ad Unit_'s waterfall, following AppLovin [documentation](https://dash.applovin.com/documentation/mediation/ui-max/ad-units/create-ad-unit).

    During this second step, AppLovin provides several options to configure your _Custom Network_. Please refer to the details below to configure them with the necessary Equativ network and placement details.

    | **Option**|**Format**| **Value(from Equativ)** | **Description**  |
    | :--   | :-- | --------------- | --------------- |
    | Placement ID      | `Number` |```Network ID``` | Equativ network's main ID    |
    | Custom Parameters | `JSON` |```{"siteid":"1234","pageid":"5678","formatid":"9101","targeting":""}```| Ad-placement ids from Equativ.      |
    | CPM Price         | `Number` |```TBD by client```| Not supported for yet   |

    > Note: AppLovin's Ad Unit is designed to work with either "Banner" or "Interstitial" Ad Types, which will automatically link to the appropriate mediation methods in Equativ's Class reference.

5) If you plan to use keyword targeting in your Equativ insertions, it is necessary to set up these keywords in the AppLovin's _Ad Unit_ by using the targeting field in the _Custom Parameters_'s JSON.

    E.g. ```{"siteid":"1234","pageid":"5678","formatid":"9101","targeting":"age=32"}```

    This way, _AppLovin SDK_ will pass targeting infotmation to the `EQUALCustomEventAdapter` instance, which in turn will extract the keyword targeting to be passed to the _Smart SDK_.

6) AppLovin is in charge of initiating mediation process. Equativ will be called as part of their Max Mediation workflow.

More infos
----------
You can find more informations about the [_Smart Display SDK_](https://documentation.smartadserver.com/displaySDK/) and the [_AppLovin SDK_](https://dash.applovin.com/documentation/mediation) in the official documentation. 
