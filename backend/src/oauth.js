import { config } from './config.js'

export const exchangeToken = async (params) => {
  const response = await fetch('https://oauth-login.cloud.huawei.com/oauth2/v3/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      ...params,
      client_id: config.healthKit.clientId,
      client_secret: config.healthKit.clientSecret,
    }),
  })

  return { status: response.status, body: await response.text() }
}
