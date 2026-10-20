import { createHmac, timingSafeEqual } from 'node:crypto'
import { config } from './config.js'

const [type, subType] = config.healthKit.eventType.split('$')

const siteCrossed = (response, body) => {
  if (response.status !== 403) return false
  try {
    return JSON.parse(body).code == 121001
  } catch {
    return false
  }
}

const call = async (host, path, accessToken, init) => {
  const request = (site) => fetch(`https://${site}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json; charset=UTF-8',
      Authorization: `Bearer ${accessToken}`,
      'x-client-id': config.healthKit.clientId,
    },
  })

  let response = await request(host)
  let body = await response.text()
  if (siteCrossed(response, body)) {
    const location = response.headers.get('location')
    if (location) {
      host = new URL(location).host
      response = await request(host)
      body = await response.text()
    }
  }

  return { response, body, site: host }
}

export const subscribe = async (accessToken, storedSite) => {
  const { response, body, site } = await call(storedSite ?? config.healthKit.apiHost, '/healthkit/v2/subscriptions', accessToken, {
    method: 'POST',
    body: JSON.stringify({
      subscriberId: config.healthKit.subscriberId,
      eventTypes: [{ type, subType, eventType: config.healthKit.eventType }],
    }),
  })

  if (!response.ok) throw new Error(`Health Kit answered ${response.status}: ${body}`)

  return { subscriptions: JSON.parse(body), site }
}

export const unsubscribe = async (accessToken, subscriptionId, site) => {
  const { response, body } = await call(
    site ?? config.healthKit.apiHost,
    `/healthkit/v2/subscriptions/${subscriptionId}`,
    accessToken,
    { method: 'DELETE' },
  )

  if (!response.ok) throw new Error(`Health Kit answered ${response.status}: ${body}`)
}

export const tokenOpenId = async (accessToken) => {
  const response = await fetch('https://oauth-api.cloud.huawei.com/rest.php?nsp_fmt=JSON&nsp_svc=huawei.oauth2.user.getTokenInfo', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ open_id: 'OPENID', access_token: accessToken }),
  })
  const body = await response.json()
  if (!response.ok || !body.open_id) {
    throw Object.assign(new Error('The Huawei access token is not valid'), { status: 401 })
  }
  return body.open_id
}

export const signatureMatches = ({ openId, eventType, eventTime }, signature) => {
  if (!config.healthKit.subscriberSecret || !signature) return false

  const expected = createHmac('sha256', Buffer.from(config.healthKit.subscriberSecret, 'base64'))
    .update(`${openId}_${eventType}_${eventTime}`)
    .digest('base64')
  const actual = Buffer.from(signature)
  const wanted = Buffer.from(expected)
  return actual.length === wanted.length && timingSafeEqual(actual, wanted)
}

export const readMeta = (notification, key) =>
  notification.metaData?.find((entry) => entry.metaKey === key)?.metaValue ?? null
