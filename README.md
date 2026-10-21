> **Note:** To access all shared projects, get information about environment setup, and view other guides, please visit [Explore-In-HMOS-Wearable Index](https://github.com/Explore-In-HMOS-Wearable/hmos-index).

# Huawei Health Kit demo

An Android app, with a small Node.js backend, that reads a user's health data from
**Huawei Health Kit Cloud**: steps, calories, distance, heart rate, resting heart rate, stress,
sleep, daily activity goals and workouts. The user wears a Huawei watch, Huawei Health syncs the
data to the Huawei cloud, and this app reads it after the user allows it. The backend
holds the App secret, exchanges the authorization code for tokens, and sends a push
notification when Huawei reports a new workout.

This project used to share a repository with the Wear Engine sample. Wear Engine is now in
its own project, `wear-engine-and-huawei-health-kit-demo`.

## Contents

1. [What you will build](#what-you-will-build)
2. [What you need](#what-you-need)
3. [How it works](#how-it-works)
4. [Step 1. Register your app](#step-1-register-your-app)
5. [Step 2. Apply for Health Service Kit](#step-2-apply-for-health-service-kit)
6. [Step 3. Run the backend](#step-3-run-the-backend)
7. [Step 4. Set up the callbacks](#step-4-set-up-the-callbacks)
8. [Step 5. Configure the app](#step-5-configure-the-app)
9. [Step 6. Run the app](#step-6-run-the-app)
10. [Step 7. Read the code](#step-7-read-the-code)
11. [Step 8. Run the tests](#step-8-run-the-tests)
12. [Before production](#before-production)
13. [Troubleshooting](#troubleshooting)
14. [Official documentation](#official-documentation)

## What you will build

A Material 3 app with three tabs that covers the whole Health Kit flow:

| Where | What happens |
| --- | --- |
| Connect screen | Choose **Web OAuth** or the **Login-free SDK** and tap **Connect Huawei Health**. The backend turns the code into an access token and a refresh token. |
| **Today** tab | Steps against the user's own goal, active calories, distance, activity goals, a 7-day step chart, heart rate, stress and last night's sleep with its stages |
| **Today** tab, Data access | One row per Health Kit read with its scope, and the error code when the read fails |
| **Workouts** tab | The workouts of the last 7 days, 30 days, 90 days or 1 year. Tap one for its details. |
| **Logs** tab | Every HTTP call with method, path, status, time, trace ID and the privacy header, every error with its cause and fix. Filter by level, copy or share. |
| Menu, Disconnect | The app stops the workout notifications and forgets the tokens |
| Menu, Delete this app's data | Huawei cancels the authorization and deletes the data the app created |

After the authorization, the app also turns on workout notifications through the backend.

## What you need

| | Version |
| --- | --- |
| Android Studio | A version that supports Android Gradle Plugin 9.4 |
| JDK | 17 or later (Android Studio ships one) |
| Android phone | Android 7.0 (API 24) or later |
| Huawei Health app | On the phone, signed in with the user's HUAWEI ID, with workouts synced to the cloud |
| HUAWEI developer account | Identity verified. The subscription for notifications needs an enterprise account |
| Node.js | 20.6 or later, for the backend |
| A domain with HTTPS | For the backend, so Huawei can reach it (Step 3) |

Web OAuth works on any Android phone. The login-free SDK works best with the Huawei Health app
or HMS Core on the phone, and falls back to a Huawei web page.

## How it works

```
                       sign-in (Custom Tab or login-free SDK)
user ----------------------------------------------------------> Huawei OAuth
                                                                     |
   app <-- deep link with code and state -- backend /oauth/callback <+
   app -- code ---------------------------> backend /oauth/token  --> Huawei token endpoint
   app <-- access token, refresh token ----  (App secret stays here)
   app -- access token --------------------------------------------> Health Kit Cloud
                                                                     (workouts)
   app -- access token, push token ------> backend /subscriptions --> Huawei subscription
   Huawei -- signed notification --------> backend /healthkit/notifications --> push --> phone
```

| # | What happens | Code |
| --- | --- | --- |
| 1 | The app opens Huawei's authorization and gets a code back | `healthkit/auth/` |
| 2 | The backend exchanges the code for tokens with the App secret | `token/HealthKitTokenClient.kt`, `backend/src/oauth.js` |
| 3 | The app reads the health data and the workouts with the access token | `healthkit/HealthKitApi.kt` |
| 4 | The backend refreshes the access token when it expires, after about 1 hour | `healthkit/HealthKitClient.kt` |
| 5 | The backend subscribes the user and sends a push for every new workout | `push/`, `backend/` |

The two authorization methods differ only in step 1:

| | Web OAuth | Login-free SDK |
| --- | --- | --- |
| SDK in the app | None, a Custom Tab | `com.huawei.hms:healthlite:6.16.0.300` |
| Sign-in page | Always shown | Not shown when the user is already signed in |
| Where the user allows the app | A Huawei web page | The HUAWEI ID screen, the Huawei Health app or a Huawei web page |
| Redirect URI sent to Huawei | `https://<your backend>/oauth/callback` | Huawei's fixed callback URL |
| How the code reaches the app | The backend page opens the app's deep link | The SDK opens the app's deep link (the Scheme URL) |

## Step 1. Register your app

1. Sign in to [AppGallery Connect](https://developer.huawei.com/consumer/en/service/josp/agc/index.html).
2. Create a project, then add an Android app to it.
   - **Package name:** the `applicationId` of your app. In this sample it is
     `com.example.healthkitdemo`. Change it in `app/build.gradle.kts` to your own value.
   - Note the **App ID** and the **App secret**. The App ID goes into the app, the App secret
     only into the backend.
3. Create a keystore and add its SHA-256 fingerprint to the app under
   **Project settings > General information**:

   ```bash
   keytool -genkeypair -v -keystore release.jks -alias release -keyalg RSA -keysize 2048 -validity 10000
   keytool -list -v -keystore release.jks -alias release
   ```

   The login-free SDK checks the signing certificate. A build signed with another key fails
   with code 6003.

## Step 2. Apply for Health Service Kit

1. Open **HUAWEI Developers > Console > App services > Health Service Kit**.
2. Apply for the scopes this sample uses. All of them start with
   `https://www.huawei.com/healthkit/`, except `openid`:

   | Scope | Used for |
   | --- | --- |
   | `openid` | Who the user is (`openId`). Required. |
   | `activityrecord.read` | Workout records |
   | `activity.read` | The details of a workout |
   | `step.read` | Steps per day |
   | `calories.read` | Active calories per day |
   | `distance.read` | Distance per day |
   | `heartrate.read` | Heart rate and resting heart rate per day |
   | `stress.read` | Stress per day |
   | `sleep.read` | Sleep records |
   | `dailyactivitysummary.read` | Activity goals: steps, active calories, exercise minutes, active hours |

   The list lives in `HealthFeature` in `healthkit/HealthKitConfig.kt`. Remove a row there if
   you do not apply for its scope.

3. Wait for the approval. Huawei asks you to wait 24 hours after the scopes are granted
   before you test.

Health Kit Cloud only serves data created after the user authorized your app. To read older
data, also apply for a history scope (`historydata.open.week`, `.month` or `.year`) and set
`healthkit.historyWindow` in Step 5. The app never asks for older data than the scope allows.
Asking for a scope that is not approved makes the whole authorization fail, so keep
`historyWindow=NONE` until the scope is approved.

In test mode, at most 100 users can use the scopes. Apply for verification on the same card
to remove the limit.

## Step 3. Run the backend

The backend is a small Express server in `backend/`. It holds the App secret, so the secret is
never inside the APK.

```bash
cd backend
npm install
cp .env.example .env
npm start
```

Fill in `.env`:

| Variable | Value |
| --- | --- |
| `HEALTH_CLIENT_ID` | The App ID from Step 1 |
| `HEALTH_CLIENT_SECRET` | The App secret from Step 1 |
| `APP_DEEP_LINK` | The app's deep link, the same value as `healthkit.deepLink` in Step 5 |
| `HEALTH_SUBSCRIBER_ID`, `HEALTH_SUBSCRIBER_SECRET` | From the subscription setup in Step 4 |
| `HEALTH_EVENT_TYPE` | `ACTIVITY_RECORD_EVENT$UPDATE`, fires when a workout is added or changed |
| `FCM_SERVICE_ACCOUNT_FILE` | Firebase console, Project settings, Service accounts, Generate new private key |
| `HMS_APP_ID`, `HMS_APP_SECRET` | AppGallery Connect, the app with Push Kit |

Huawei must reach the backend over HTTPS with a public certificate. To put it on a server,
follow [backend/deploy/README.md](backend/deploy/README.md). The endpoints are described in
[backend/README.md](backend/README.md).

For a quick test on your desk, run the backend on your computer and connect the phone with
USB. `adb reverse tcp:3000 tcp:3000` makes `http://127.0.0.1:3000` on the phone reach the
backend. Only the login-free SDK works this way, because Huawei only accepts the redirect URI
you registered.

## Step 4. Set up the callbacks

### For Web OAuth

Add `https://<your backend>/oauth/callback` to the app's callback URLs in AppGallery Connect.
The app sends exactly this value to Huawei, so it must match character by character.

### For the login-free SDK

1. Add Huawei's fixed callback URL to the app's callback URLs, exactly like this:

   ```
   https://h5hosting.dbankcdn.com/cch5/healthkit/oauth-h5/oauth-callback.html
   ```

2. Enter your app's deep link as the **Scheme URL**, for example
   `healthkitdemo://oauth/callback`. The console then gives you a **Scheme Secret**.
3. Apply for the login-free authorization permission for your regions.

### For workout notifications

1. On the Health Service Kit card, register the subscription capability in **HTTP** mode.
   This needs an enterprise developer account.
2. Enter `https://<your backend>/healthkit/notifications` as the callback URL. Huawei sends a
   signed test notification and expects 204.
3. Put the **subscriber ID** and the **subscriber secret** into the backend `.env`.
4. For Huawei phones, enable **Push Kit** for the app in AppGallery Connect.
5. For phones with Google services, add the app in the Firebase console and download
   `google-services.json` into `app/`. The project applies the Google Services plugin only
   when this file exists, so it builds without it.

## Step 5. Configure the app

1. Clone the project and open it in Android Studio.
2. Copy `local.properties.example` to `local.properties` and fill it in:

   | Key | Value |
   | --- | --- |
   | `sdk.dir` | Path to your Android SDK. Android Studio writes this line for you. |
   | `healthkit.clientId` | The App ID from Step 1 |
   | `backend.baseUrl` | Your backend, for example `https://your-domain.example.com` |
   | `healthkit.deepLink` | The app's deep link, also the login-free Scheme URL. Default `healthkitdemo://oauth/callback`. The manifest builds its intent filter from it. |
   | `healthkit.schemeSecret` | Login-free Scheme Secret from Step 4 |
   | `healthkit.authMethod` | The method selected at start: `WEB_OAUTH` or `LOGIN_FREE_SDK` |
   | `healthkit.historyWindow` | `NONE`, `WEEK`, `MONTH` or `YEAR`. Use anything but `NONE` only after the history scope is approved. |
   | `healthkit.apiHost` | Leave empty. The app finds the user's regional site. |

   The App secret is not here. It lives only in the backend `.env`.

3. Copy `keystore.properties.example` to `keystore.properties` and point it at the keystore
   from Step 1:

   ```properties
   storeFile=release.jks
   storePassword=your-store-password
   keyAlias=release
   keyPassword=your-key-password
   ```

`local.properties`, `keystore.properties`, `*.jks`, `app/agconnect-services.json`,
`app/google-services.json` and `backend/.env` are all in `.gitignore`.

## Step 6. Run the app

1. Sign in to the Huawei Health app on the phone with the HUAWEI ID that has workouts.
2. In Huawei Health, open **Me > Privacy management** and turn on **HUAWEI Health Kit**.
   Without it the workout list stays empty.
3. Click **Run** in Android Studio, or run:

   ```bash
   ./gradlew installDebug
   ```

4. Choose **Web OAuth** or **Login-free SDK** and tap **Connect Huawei Health**. On Android 13
   and later, the app also asks for the notification permission.
5. Allow the app. The app comes back, shows `Huawei Health account connected` in the **Logs**
   tab and reads the data.
6. Check **Data access** at the bottom of **Today**. Every row should be green.
7. Open **Workouts**, choose a range and tap a workout to see its details.

If you add a scope later, users who connected before do not have it. Their reads answer 403
`Insufficient Permission`. Disconnect and connect again to grant the new scope.

## Step 7. Read the code

| File | What it holds |
| --- | --- |
| `MainActivity.kt` | Starts the sign-in intents, asks for the notification permission, passes deep links to the view model |
| `HealthKitViewModel.kt` | The screen state and every user action |
| `healthkit/HealthKitClient.kt` | The single entry point for Health Kit: authorize, read, refresh, cancel |
| `healthkit/HealthKitApi.kt` | The REST calls: daily statistics, activity summary, sleep, workouts, cancel |
| `healthkit/HealthData.kt`, `HealthSummary.kt` | Reading Huawei's sample points into one `DaySummary` per day |
| `healthkit/errors/ErrorCatalog.kt` | Turns any failure into a title, cause, fix and documentation link |
| `assets/health_kit_errors.json` | 140 official error codes: REST, OAuth, login-free, subscription, privacy |
| `log/AppLog.kt` | The logger behind the Logs tab and Logcat |
| `healthkit/SessionStore.kt` | Tokens, the pending OAuth state, the user's site and the chosen method |
| `healthkit/HealthKitConfig.kt` | Values from `local.properties`, URLs and scopes |
| `healthkit/auth/` | Web OAuth, the login-free SDK and the redirect check |
| `healthkit/token/HealthKitTokenClient.kt` | Calls the backend to exchange and refresh tokens |
| `push/` | Push token, backend subscription and the notification |
| `ui/` | `HealthKitRoute` connects the view model, `HealthKitScreen` and the tab screens only draw the state |
| `ui/theme/` | Full light and dark color schemes generated from one seed color, and one accent per metric |
| `backend/` | The Node.js server for tokens, the subscription and the push |

### 7.1 Start the authorization

Before each sign-in the app stores a random `state`. Web OAuth opens Huawei's authorize page in
a Custom Tab (`WebOAuthAuthorizer.kt`):

```kotlin
val url = HealthKitConfig.AUTHORIZE_URL.toUri().buildUpon()
    .appendQueryParameter("response_type", "code")
    .appendQueryParameter("client_id", HealthKitConfig.clientId)
    .appendQueryParameter("redirect_uri", tokenRedirectUri)
    .appendQueryParameter("scope", HealthKitScopes.DEFAULT.joinToString(" "))
    .appendQueryParameter("state", state)
    .appendQueryParameter("access_type", "offline")
    .appendQueryParameter("display", "touch")
    .build()

return CustomTabsIntent.Builder().build().intent.setData(url)
```

`access_type=offline` asks for a refresh token.

The login-free SDK builds the intent for Huawei's own screen (`LoginFreeAuthorizer.kt`). This is
solution 1 of the login-free guide, which picks the HUAWEI ID screen, the Huawei Health app or
a web page by itself:

```kotlin
HuaweiHiHealthLite.getLoginFreeAuthController(context).requestLoginFreeAuthorizationIntent(
    HealthKitScopes.DEFAULT.toTypedArray(),
    state,
    HealthKitConfig.clientId,
    HealthKitConfig.deepLink,
    HealthKitConfig.schemeSecret,
)
```

`MainActivity` opens the Custom Tab with `startActivity` and the login-free intent with an
Activity Result launcher, so it can report a failed or cancelled sign-in:

```kotlin
private val loginFree = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    viewModel.onLoginFreeResult(it.resultCode, it.data)
}
```

### 7.2 Check the redirect

Both methods come back to the app's deep link with `code` and `state`. `AuthorizationRedirect`
accepts the code only when a sign-in was started and the state matches the stored one:

```kotlin
if (expectedState == null) return failure("No sign-in was started. Tap Connect again.")
if (uri.getQueryParameter("state") != expectedState) return failure("The state value did not match the request.")
```

The state is stored in `SharedPreferences`, so the check also works when Android stopped the
app while the user was signing in.

### 7.3 Exchange the code on the backend

`HealthKitTokenClient` posts the code to the backend, and the backend adds the App secret:

```
POST https://<your backend>/oauth/token   {"code": "...", "redirectUri": "..."}
POST https://<your backend>/oauth/refresh {"refreshToken": "..."}
```

The backend answers with Huawei's JSON: `access_token`, `refresh_token` and `expires_in`.
When the refresh token has expired too (result 1203), the app signs the user out.

### 7.4 Read the workouts

```
GET https://health-api.cloud.huawei.com/healthkit/v2/activityRecords?startTime=<ms>&endTime=<ms>
Authorization: Bearer <access token>
x-client-id: <App ID>
```

`HealthKitApi.kt` handles four things for you:

- **31-day windows.** One query may cover at most 31 days, so a year is read as twelve
  windows.
- **Pages.** When the answer has `hasMoreData`, it follows the `cursor`.
- **Regional sites.** A token works only on the site its HUAWEI ID belongs to. Another site
  answers 403 with code 121001 and a `Location` header. The app repeats the call on that host
  once and remembers it.
- **The privacy switch.** The header `x-health-app-privacy: 2` means the user has not turned on
  HUAWEI Health Kit in Huawei Health. The screen then shows a button that opens Huawei Health.

Without `sourceType`, Huawei returns every workout except the ones the user entered by hand.

### 7.5 Read the daily health data

Each item on **Today** is one call, and the calls run in parallel. One failing scope does not
hide the others: the failure shows up in **Data access** and in the log.

| Data | Call | Request type | Answer type and fields |
| --- | --- | --- | --- |
| Steps | `POST sampleSet:dailyPolymerize` | `com.huawei.continuous.steps.delta` | `...steps.total`: `steps` |
| Active calories | same | `com.huawei.continuous.calories.burnt` | `...calories.burnt.total`: `calories_total` (kcal) |
| Distance | same | `com.huawei.continuous.distance.delta` | `...distance.total`: `distance` (m) |
| Heart rate | same | `com.huawei.instantaneous.heart_rate` | `...heart_rate.statistics`: `avg`, `min`, `max`, `last` |
| Resting heart rate | same | `com.huawei.instantaneous.resting_heart_rate` | `...resting_heart_rate.statistics`: `avg`, `last` |
| Stress | same | `com.huawei.instantaneous.stress` | `...stress.statistics`: `avg`, `min`, `max` |
| Activity goals | `POST sampleSet:dailyActivitySummary` | none | `steps`, `activeCalories`, `exerciseTime`, `activeHours` and their `...Goal` |
| Sleep | `GET healthRecords` | `com.huawei.health.record.sleep` | `fall_asleep_time`, `wakeup_time`, `all_sleep_time`, `deep_sleep_time`, `light_sleep_time`, `dream_time`, `awake_time`, `sleep_score` |

The two daily calls take days, not timestamps, and the phone's time zone:

```
POST https://health-api.cloud.huawei.com/healthkit/v2/sampleSet:dailyPolymerize
{ "dataTypes": ["com.huawei.continuous.steps.delta"],
  "startDay": "20261015", "endDay": "20261021", "timeZone": "+0300" }
```

Watch the time units, they differ per call:

| Where | Unit |
| --- | --- |
| `dailyPolymerize`, `dailyActivitySummary` request | `yyyyMMdd` and `timeZone` |
| `healthRecords` request | nanoseconds |
| `activityRecords` request | milliseconds |
| `group.startTime` in an answer | milliseconds |
| `samplePoint.startTime` in an answer | nanoseconds |

Values come as `integerValue`, `floatValue` or `longValue`. Read them by `fieldName`, never by
position:

```kotlin
fun JSONObject.fieldValues(): Map<String, Double> =
    optJSONArray("value").objects().mapNotNull { entry ->
        val key = NUMBER_KEYS.firstOrNull(entry::has) ?: return@mapNotNull null
        entry.optString("fieldName") to entry.getDouble(key)
    }.toMap()
```

Naps (`sleep_type` 3) are left out of the sleep card. Regular sleep without TruSleep has no
stages, so the card then shows only the total.

### 7.6 Logging

`AppLog` keeps the last 500 entries in a `StateFlow` for the Logs tab and writes each one to
Logcat with the tag `HealthKitDemo/<category>`:

```bash
adb logcat -s "HealthKitDemo/API:*" "HealthKitDemo/AUTH:*" "HealthKitDemo/APP:*" "HealthKitDemo/PUSH:*"
```

Every HTTP call logs one line with its timing, and the detail holds what you need for a ticket
to Huawei:

```
POST /healthkit/v2/sampleSet:dailyPolymerize -> 403 (744 ms)
host health-api.cloud.huawei.eu
trace 9b86194f-dcce-4b80-9a18-01d0c6d627ab
x-health-app-privacy 1
body {"error":{"code":403,"message":"Insufficient Permission: ..."}}
```

The `trace` value is also sent as the `x-caller-trace-id` header. Access tokens, codes and
secrets are never logged.

### 7.7 Error catalog

`ErrorCatalog` reads `assets/health_kit_errors.json` once and explains any failure:

| Failure | How it is matched |
| --- | --- |
| HTTP error from Health Kit | `error.code` (for example 121001), then the HTTP status and the message |
| HTTP error from the token endpoint | `error` and `sub_error` (for example 1203 and 11205) |
| Login-free SDK result | The `DEEPLINK_RESULT` code (for example 51011) |
| `x-health-app-privacy` header | 1, 2 or 3 |
| No network, timeout | Fixed answers |

Each answer has a title, the cause, the fix and a link to the Huawei page it came from. The
screen shows it in an error card, and the log keeps the full text.

### 7.8 Cancel the authorization

Huawei asks every app to let users cancel the authorization. One call does it and deletes the
data the app created at once, instead of after 3 days:

```
DELETE https://health-api.cloud.huawei.com/healthkit/v2/consents/<App ID>?deleteData=true
```

Before that, the app removes the workout subscription on the backend, so no more
notifications arrive.

### 7.9 Workout notifications

Right after the authorization, and on every app start while the user is connected,
`WorkoutSubscriptions` gets a push token (Firebase when `google-services.json` is in the build,
otherwise Push Kit) and sends it with the access token to `POST /subscriptions`. The backend
asks Huawei for the user's `openId` and creates the subscription only once per user. When the
push service hands out a new token, `onNewToken` sends it the same way.

## Step 8. Run the tests

| Test | What it checks |
| --- | --- |
| `AuthorizationRedirectTest` | A matching state returns the code, another state or no started sign-in is rejected, Huawei's error is reported |
| `WorkoutRecordTest` | A workout record and its summary values are read from Huawei's JSON |
| `ErrorCatalogTest` | Scope, site, refresh token, login-free, privacy and network errors get the right catalog entry |
| `HealthDataTest` | Values are read by field name, and a day is built from every data type |
| `HealthKitScreenTest` | Connect, the method switch, deleting asks first, Data access shows the error code, the Logs tab shows the entries |

They run on a phone and do not need a Huawei account:

```bash
./gradlew connectedDebugAndroidTest
```

> On Xiaomi phones, turn on **Developer options > USB debugging (Security settings)**.
> Without it, MIUI blocks the test from opening its activity and the run waits forever.

## Before production

- **The backend's own API.** `/oauth/*` and `/subscriptions` trust the Huawei access token and
  nothing else. Add your own user authentication in a real server.
- **Token storage.** Tokens are in `SharedPreferences` here and are excluded from backups. Keep
  them on your backend, or encrypt them on the phone.
- **Scheme Secret.** The login-free SDK needs it on the phone. Treat it as public.
- **Login-free SDK.** `healthlite:6.16.0.300` needs `androidx.fragment` at runtime, so
  `app/build.gradle.kts` adds it.
- **Versions.** Huawei's Maven repository has no version index. Write versions in full,
  `6.16.0.300` and not `6.16.+`.
- **Verification.** Apply for Health Kit verification to lift the 100-user test limit, and
  remove the scopes you do not use before you apply.
- **Callback.** The backend answers 404 to every notification whose signature does not match,
  including all of them while `HEALTH_SUBSCRIBER_SECRET` is empty.

## Troubleshooting

| What you see | What to do |
| --- | --- |
| `Add healthkit.clientId, ... to local.properties` | Fill in the keys the selected method needs (Step 5) and build again. |
| `invalid redirect_uri` (1101, 20022) | The redirect URI is not in a valid format. Use `https://<your backend>/oauth/callback`. |
| `redirect_uri not registered` (1101, 20023) | Add the redirect URI to the app's callback URLs. For the login-free SDK, add Huawei's fixed callback URL too (Step 4). |
| `invalid code` (1101 or 1103) | The code was used already, expired, or was issued for another App ID or redirect URI. |
| Login-free code 6003 | The APK is not signed with the registered keystore. |
| 403 `Insufficient Permission ... scopes of type ...` | The token lacks that scope. Check that the scope is approved (Step 2), then disconnect and connect again. |
| 403 `The query time is out of range` | The query starts before the history scope allows. Apply for a history scope or read a shorter range. |
| 403 with code 121001 | Wrong regional site. Leave `healthkit.apiHost` empty so the app follows Huawei's `Location` header. |
| Empty workout list, `x-health-app-privacy 2` | Turn on HUAWEI Health Kit in Huawei Health (Step 6). Check that the user signed in with the same HUAWEI ID as Huawei Health. |
| 1203 on token refresh | The refresh token expired or was revoked. Connect again. |
| `No push token on this phone` | Add `google-services.json`, or use a phone with HMS Core and Push Kit enabled. |
| No push arrives | Check the backend log, the subscription, the push credentials and the notification permission. |

## Official documentation

- [Authentication](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/auth-example-0000001054581058)
- [Login-free authorization, overview](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/cloud-free-auth-login-overview-0000002054076576)
- [Querying sampling data statistics of multiple days](https://developer.huawei.com/consumer/en/doc/HMSCore-References/sampleset_daily_polymerize-0000001078113560)
- [Querying daily activity statistics](https://developer.huawei.com/consumer/en/doc/HMSCore-References/querying-daily-activity-statistics-0000001572908385)
- [Querying health records](https://developer.huawei.com/consumer/en/doc/HMSCore-References/get-health-record-by-datatype-0000001142843917)
- [Result codes](https://developer.huawei.com/consumer/en/doc/HMSCore-References/error-code-0000001054236973)
- [Querying created exercise records](https://developer.huawei.com/consumer/en/doc/HMSCore-References/activityrecords_list-0000001050114862)
- [Subscribing to data](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/subscription-0000001078496860)
- [Canceling authorization](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/canceling-authorization-0000001209549384)
- [Applying for verification](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/verification-0000001211786611)
- [Health Service Kit FAQs](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/faq-0000001476980529)
- [Push Kit, obtaining a push token](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/android-client-dev-0000001050042041)
- [Push Kit, sending downlink messages](https://developer.huawei.com/consumer/en/doc/HMSCore-References/https-send-api-0000001050986197)

## License

See [LICENSE](LICENSE).
