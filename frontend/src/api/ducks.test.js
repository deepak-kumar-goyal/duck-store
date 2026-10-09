import { afterEach, describe, expect, it, vi } from 'vitest'
import { addDuck, deleteDuck, listDucks, updateDuck } from './ducks'

function respondWith(status, body) {
  const response = {
    ok: status >= 200 && status < 300,
    status,
    json: () => (body === undefined ? Promise.reject(new Error('no body')) : Promise.resolve(body)),
  }
  const fetchMock = vi.fn().mockResolvedValue(response)
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

afterEach(() => vi.unstubAllGlobals())

describe('ducks api', () => {
  it('lists ducks from /api/ducks', async () => {
    const fetchMock = respondWith(200, [{ id: 1 }])

    expect(await listDucks()).toEqual([{ id: 1 }])
    expect(fetchMock.mock.calls[0][0]).toBe('/api/ducks')
  })

  it('posts a new duck as json', async () => {
    const duck = { color: 'Red', size: 'Large', price: 9.5, quantity: 3 }
    const fetchMock = respondWith(201, { id: 7, ...duck })

    await addDuck(duck)

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/ducks')
    expect(options.method).toBe('POST')
    expect(JSON.parse(options.body)).toEqual(duck)
  })

  it('updates only price and quantity', async () => {
    const fetchMock = respondWith(200, {})

    await updateDuck(5, { color: 'Red', size: 'Large', price: 2, quantity: 4 })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/ducks/5')
    expect(options.method).toBe('PUT')
    expect(JSON.parse(options.body)).toEqual({ price: 2, quantity: 4 })
  })

  it('deletes by id and returns null for 204', async () => {
    const fetchMock = respondWith(204)

    expect(await deleteDuck(5)).toBeNull()
    expect(fetchMock.mock.calls[0][0]).toBe('/api/ducks/5')
    expect(fetchMock.mock.calls[0][1].method).toBe('DELETE')
  })

  it('shows the backend message with its field errors', async () => {
    respondWith(400, { message: 'Validation failed', fieldErrors: { price: 'must be positive' } })

    await expect(listDucks()).rejects.toThrow('Validation failed: must be positive')
  })

  it('shows only the message when there are no field errors', async () => {
    respondWith(409, { message: 'Insufficient stock', fieldErrors: {} })

    await expect(listDucks()).rejects.toThrow('Insufficient stock')
  })

  it('falls back to the status when the error body is not json', async () => {
    respondWith(500)

    await expect(listDucks()).rejects.toThrow('Request failed (500)')
  })
})
