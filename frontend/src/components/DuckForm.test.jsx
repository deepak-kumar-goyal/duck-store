import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import DuckForm from './DuckForm'

describe('DuckForm', () => {
  it('submits a new duck with numeric price and quantity', async () => {
    const onSubmit = vi.fn().mockResolvedValue()
    render(<DuckForm duck={null} onSubmit={onSubmit} onCancel={vi.fn()} />)

    await userEvent.selectOptions(screen.getByLabelText('Color'), 'Green')
    await userEvent.selectOptions(screen.getByLabelText('Size'), 'Small')
    await userEvent.type(screen.getByLabelText(/Price/), '12.5')
    await userEvent.type(screen.getByLabelText('Quantity'), '3')
    await userEvent.click(screen.getByRole('button', { name: 'Add' }))

    expect(onSubmit).toHaveBeenCalledWith({
      color: 'Green',
      size: 'Small',
      price: 12.5,
      quantity: 3,
    })
  })

  it('makes color and size read-only when editing', () => {
    const duck = { id: 4, color: 'Red', size: 'Large', price: 5, quantity: 2 }
    render(<DuckForm duck={duck} onSubmit={vi.fn()} onCancel={vi.fn()} />)

    expect(screen.getByLabelText('Color')).toBeDisabled()
    expect(screen.getByLabelText('Size')).toBeDisabled()
    expect(screen.getByLabelText(/Price/)).toBeEnabled()
    expect(screen.getByRole('heading', { name: 'Edit duck 4' })).toBeInTheDocument()
  })

  it('allows quantity 0 when editing but requires at least 1 when adding', () => {
    const duck = { id: 4, color: 'Red', size: 'Large', price: 5, quantity: 2 }
    const { unmount } = render(<DuckForm duck={duck} onSubmit={vi.fn()} onCancel={vi.fn()} />)
    expect(screen.getByLabelText('Quantity')).toHaveAttribute('min', '0')
    unmount()

    render(<DuckForm duck={null} onSubmit={vi.fn()} onCancel={vi.fn()} />)
    expect(screen.getByLabelText('Quantity')).toHaveAttribute('min', '1')
  })

  it('shows the error returned by the backend and stays open', async () => {
    const onSubmit = vi.fn().mockRejectedValue(new Error('Duck already exists at that price'))
    render(<DuckForm duck={null} onSubmit={onSubmit} onCancel={vi.fn()} />)

    await userEvent.type(screen.getByLabelText(/Price/), '1')
    await userEvent.type(screen.getByLabelText('Quantity'), '1')
    await userEvent.click(screen.getByRole('button', { name: 'Add' }))

    expect(await screen.findByText('Duck already exists at that price')).toBeInTheDocument()
  })

  it('calls onCancel', async () => {
    const onCancel = vi.fn()
    render(<DuckForm duck={null} onSubmit={vi.fn()} onCancel={onCancel} />)

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(onCancel).toHaveBeenCalled()
  })
})
