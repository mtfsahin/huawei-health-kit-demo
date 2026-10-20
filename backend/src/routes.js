import { Router } from 'express'
import { subscribe, tokenOpenId, unsubscribe } from './health-kit.js'
import { config } from './config.js'
import { exchangeToken } from './oauth.js'
import { findDevice, removeDevice, saveDevice } from './store.js'

const route = (handler) => (request, response, next) => handler(request, response).catch(next)

export const router = Router()

router.get('/health', (_request, response) => response.json({ ok: true }))

router.get('/oauth/callback', (request, response) => {
  const link = `${config.appDeepLink}?${new URLSearchParams(request.query)}`
  response.type('html').send(
    `<!doctype html><meta name="viewport" content="width=device-width">` +
      `<p><a href="${link}">Return to the app</a></p><script>location.href = ${JSON.stringify(link)}</script>`,
  )
})

router.post('/oauth/token', route(async (request, response) => {
  const { code, redirectUri } = request.body ?? {}
  if (!code) return response.status(400).json({ error: 'code is required' })

  const token = await exchangeToken({
    grant_type: 'authorization_code',
    code,
    ...(redirectUri ? { redirect_uri: redirectUri } : {}),
  })
  return response.status(token.status).type('json').send(token.body)
}))

router.post('/oauth/refresh', route(async (request, response) => {
  const { refreshToken } = request.body ?? {}
  if (!refreshToken) return response.status(400).json({ error: 'refreshToken is required' })

  const token = await exchangeToken({ grant_type: 'refresh_token', refresh_token: refreshToken })
  return response.status(token.status).type('json').send(token.body)
}))

router.post('/subscriptions', route(async (request, response) => {
  const { accessToken, pushToken, provider } = request.body ?? {}
  if (!accessToken || !pushToken || !['fcm', 'hms'].includes(provider)) {
    return response.status(400).json({ error: 'accessToken, pushToken, and a valid provider are required' })
  }

  const openId = await tokenOpenId(accessToken)
  const existing = await findDevice(openId)
  if (existing?.subscriptionId) {
    await saveDevice({ ...existing, pushToken, provider })
    return response.json({ openId })
  }

  const { subscriptions, site } = await subscribe(accessToken, existing?.site)
  const { openId: subscribedOpenId, subscriptionId } = subscriptions[0] ?? {}
  if (!subscribedOpenId || !subscriptionId) throw new Error('Health Kit returned no subscription')

  await saveDevice({ openId: subscribedOpenId, pushToken, provider, subscriptionId, site })
  return response.json({ openId: subscribedOpenId })
}))

router.delete('/subscriptions', route(async (request, response) => {
  const accessToken = request.get('authorization')?.replace(/^Bearer /, '')
  if (!accessToken) return response.status(400).json({ error: 'Authorization: Bearer <access token> is required' })

  const openId = await tokenOpenId(accessToken)
  const device = await findDevice(openId)
  if (!device) return response.sendStatus(204)

  await unsubscribe(accessToken, device.subscriptionId, device.site)
  await removeDevice(openId)
  return response.sendStatus(204)
}))
