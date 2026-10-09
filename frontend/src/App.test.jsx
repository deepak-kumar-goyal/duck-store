import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import * as api from './api/ducks'

vi.mock('./api/ducks')

const duck = { id: 1, color: 'Red', size: 'Large', price: 5, quantity: 2 }

beforeEach(() => {
  api.listDucks.mockResolvedValue([duck])
})

afterEach(() => vi.restoreAllMocks())

describe('App', () => {
  it('loads and shows the ducks', async () => {
    render(<App />)

    expect(await screen.findByText('5.00 USD')).toBeInTheDocument()
  })

  it('shows the error when loading fails', async () => {
    api.listDucks.mockRejectedValue(new Error('Request failed (500)'))
    render(<App />)

    expect(await screen.findByText('Request failed (500)')).toBeInTheDocument()
  })

  it('adds a duck, closes the form and reloads the list', async () => {
    api.addDuck.mockResolvedValue({})
    render(<App />)
    await screen.findByText('5.00 USD')

    await userEvent.click(screen.getByRole('button', { name: 'Add duck' }))
    await userEvent.type(screen.getByLabelText(/Price/), '3')
    await userEvent.type(screen.getByLabelText('Quantity'), '4')
    await userEvent.click(screen.getByRole('button', { name: 'Add' }))

    await waitFor(() => expect(api.listDucks).toHaveBeenCalledTimes(2))
    expect(api.addDuck).toHaveBeenCalledWith({ color: 'Red', size: 'XLarge', price: 3, quantity: 4 })
    expect(screen.queryByRole('heading', { name: 'Add duck' })).not.toBeInTheDocument()
  })

  it('edits a duck through updateDuck', async () => {
    api.updateDuck.mockResolvedValue({})
    render(<App />)
    await screen.findByText('5.00 USD')

    await userEvent.click(screen.getByText('edit'))
    const quantity = screen.getByLabelText('Quantity')
    await userEvent.clear(quantity)
    await userEvent.type(quantity, '9')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() =>
      expect(api.updateDuck).toHaveBeenCalledWith(1, { ...duck, quantity: 9 }),
    )
  })

  it('deletes only after the user confirms', async () => {
    api.deleteDuck.mockResolvedValue(null)
    const confirm = vi.spyOn(window, 'confirm')
    render(<App />)
    await screen.findByText('5.00 USD')

    confirm.mockReturnValueOnce(false)
    await userEvent.click(screen.getByText('delete'))
    expect(api.deleteDuck).not.toHaveBeenCalled()

    confirm.mockReturnValueOnce(true)
    await userEvent.click(screen.getByText('delete'))
    await waitFor(() => expect(api.deleteDuck).toHaveBeenCalledWith(1))
  })

  it('shows the error when delete fails', async () => {
    api.deleteDuck.mockRejectedValue(new Error('Duck changed by someone else'))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<App />)
    await screen.findByText('5.00 USD')

    await userEvent.click(screen.getByText('delete'))

    expect(await screen.findByText('Duck changed by someone else')).toBeInTheDocument()
  })
})
