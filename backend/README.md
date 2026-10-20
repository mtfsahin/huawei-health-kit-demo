# Health Kit Push

Receives Health Kit subscription notifications from Huawei and pushes them to the phone,
through Firebase on phones with Google services and through Push Kit on Huawei phones.

## Running

```bash
npm install
cp .env.example .env
npm start
```

Node 20.6 or newer, because `--env-file` reads `.env`.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/health` | Liveness, answers `{"ok":true}` |
| GET | `/oauth/callback` | The Web OAuth redirect URI. Sends the code and state on to the app's deep link |
| POST | `/oauth/token` | Exchanges an authorization code for Huawei tokens |
| POST | `/oauth/refresh` | Refreshes Huawei tokens |
| POST | `/subscriptions` | Registers a push token and creates a Health Kit subscription for a new user |
| DELETE | `/subscriptions` | Deletes a Health Kit subscription and the saved device |
| POST | `/healthkit/notifications` | Receives a signed Huawei callback |

Send JSON `{ "code": "...", "redirectUri": "..." }` to `POST /oauth/token`.
`redirectUri` may be empty. Send `{ "refreshToken": "..." }` to `POST /oauth/refresh`.
Both endpoints return Huawei's JSON and HTTP status. Missing required values get 400.

Register `https://<your domain>/oauth/callback` as the redirect URI of the app in AppGallery
Connect. Huawei sends the user there after the sign-in. The page opens `APP_DEEP_LINK` with the
same `code` and `state`, and shows a "Return to the app" link in case the browser does not open
the app on its own.

Keep `HEALTH_CLIENT_SECRET` on the server. Anything in an APK can be extracted, so the
Android app sends its authorization code to this backend instead of holding the secret.

Send JSON `{ "accessToken": "...", "pushToken": "...", "provider": "fcm" }` to
`POST /subscriptions`. Use `"hms"` for a Push Kit token. The server gets `openId` and
`subscriptionId` from Huawei's first subscription result and answers `200 { "openId": "..." }`.
For a saved subscription, it updates the push token and provider without subscribing again.
Missing tokens or an invalid provider get 400.

Call `DELETE /subscriptions` with the header `Authorization: Bearer <access token>`. The server gets the
`openId` from Huawei's token info API, deletes the subscription at its saved Health Kit
site, and removes the device. It answers 204 even when no device was saved.

This sample has no authentication on its own API beyond the Huawei access token. A
production server must add its own authentication.

## Notification handling

Huawei signs every notification with `x-notification-signature`. It is a Base64 encoded
HMAC-SHA256 of `openId_eventType_eventTime`, keyed with the Base64 decoded
`HEALTH_SUBSCRIBER_SECRET`. The callback accepts any content type and parses its raw body
as JSON. It answers 404 for malformed JSON, a missing secret, or an invalid signature.

For a valid `CONNECTION_VERIFY_EVENT$VERIFY` callback, it answers 204 without sending a
push. For other valid events, it answers 204 before sending the push.

## Configuration

| Variable | Where it comes from |
| --- | --- |
| `HEALTH_SUBSCRIBER_ID` | Health Kit service card on HUAWEI Developers, after the callback URL is registered |
| `HEALTH_SUBSCRIBER_SECRET` | The same card, Base64, used for the signature |
| `HEALTH_CLIENT_ID` | The Health Kit App ID, sent as `x-client-id` |
| `HEALTH_CLIENT_SECRET` | The Health Kit App secret, used by the server for OAuth token requests |
| `HEALTH_EVENT_TYPE` | `ACTIVITY_RECORD_EVENT$UPDATE` fires when a workout is added or changed |
| `HEALTH_API_HOST` | Initial Health Kit host, defaults to `health-api.cloud.huawei.com` |
| `APP_DEEP_LINK` | The app's deep link, the same value as `healthkit.deepLink` in the app. Defaults to `healthkitdemo://oauth/callback` |
| `FCM_SERVICE_ACCOUNT_FILE` | Firebase console, Project settings, Service accounts, generate a private key |
| `HMS_APP_ID`, `HMS_APP_SECRET` | AppGallery Connect, the app that owns Push Kit |

The subscription capability needs an enterprise developer account. The device registry
is a JSON file at `STORE_FILE`, one entry per `openId`. The server saves the Health Kit
site for each user. If Huawei returns 403 with code 121001, it retries once at the host
in the `Location` header.
