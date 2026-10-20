import express, { Router } from 'express'
import { signatureMatches } from './health-kit.js'
import { notifyDevice } from './notifications.js'

const anyBody = express.raw({ type: () => true, limit: '1mb' })

export const callbackRouter = Router()

callbackRouter.post('/healthkit/notifications', anyBody, (request, response) => {
  let notification
  try {
    notification = JSON.parse(request.body?.toString() ?? '')
  } catch {
    return response.sendStatus(404)
  }

  if (!notification || typeof notification !== 'object' ||
      !signatureMatches(notification, request.get('x-notification-signature'))) {
    return response.sendStatus(404)
  }

  response.sendStatus(204)
  if (notification.eventType === 'CONNECTION_VERIFY_EVENT$VERIFY') return

  notifyDevice(notification).catch((error) => console.error('Push failed', error))
})
