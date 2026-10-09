import { API_ENDPOINTS } from '../constants'

const BASE_URL = API_ENDPOINTS.DUCKS

async function request(url, options) {
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }
  return response.status === 204 ? null : response.json()
}

// The backend always answers errors with { message, fieldErrors }.
async function errorMessage(response) {
  try {
    const { message, fieldErrors } = await response.json()
    const details = Object.values(fieldErrors ?? {}).join(', ')
    return details ? `${message}: ${details}` : message
  } catch {
    return `Request failed (${response.status})`
  }
}

export const listDucks = () => request(BASE_URL)

export const addDuck = (duck) =>
  request(BASE_URL, { method: 'POST', body: JSON.stringify(duck) })

export const updateDuck = (id, { price, quantity }) =>
  request(`${BASE_URL}/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ price, quantity }),
  })

export const deleteDuck = (id) => request(`${BASE_URL}/${id}`, { method: 'DELETE' })
