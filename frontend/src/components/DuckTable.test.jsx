import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import DuckTable from './DuckTable'

const ducks = [
  { id: 1, color: 'Red', size: 'Large', price: 5, quantity: 2 },
  { id: 2, color: 'Black', size: 'XSmall', price: 12.5, quantity: 40 },
]

describe('DuckTable', () => {
  it('shows one row per duck with the price to two decimals and the currency', () => {
    render(<DuckTable ducks={ducks} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText('5.00 USD')).toBeInTheDocument()
    expect(screen.getByText('12.50 USD')).toBeInTheDocument()
    expect(screen.getAllByRole('row')).toHaveLength(3) // header + 2 ducks
  })

  it('shows an empty message when there are no ducks', () => {
    render(<DuckTable ducks={[]} onEdit={vi.fn()} onDelete={vi.fn()} />)

    expect(screen.getByText('No ducks in the warehouse yet.')).toBeInTheDocument()
  })

  it('passes the clicked duck to onEdit and onDelete', async () => {
    const onEdit = vi.fn()
    const onDelete = vi.fn()
    render(<DuckTable ducks={ducks} onEdit={onEdit} onDelete={onDelete} />)

    await userEvent.click(screen.getAllByText('edit')[1])
    await userEvent.click(screen.getAllByText('delete')[0])

    expect(onEdit).toHaveBeenCalledWith(ducks[1])
    expect(onDelete).toHaveBeenCalledWith(ducks[0])
  })
})
