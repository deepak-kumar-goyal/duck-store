import { useState } from 'react'
import { COLORS, CURRENCY, SIZES } from '../constants'

const EMPTY_DUCK = { color: COLORS[0], size: SIZES[0], price: '', quantity: '' }

/** Used for both adding and editing; when editing, color and size are read-only. */
export default function DuckForm({ duck, onSubmit, onCancel }) {
  const isEditing = Boolean(duck)
  const [values, setValues] = useState(duck ?? EMPTY_DUCK)
  const [error, setError] = useState('')

  const change = (event) =>
    setValues({ ...values, [event.target.name]: event.target.value })

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    try {
      await onSubmit({
        ...values,
        price: Number(values.price),
        quantity: Number(values.quantity),
      })
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <form className="duck-form" onSubmit={submit}>
      <h2>{isEditing ? `Edit duck ${duck.id}` : 'Add duck'}</h2>
      <label>
        Color
        <select name="color" value={values.color} onChange={change} disabled={isEditing}>
          {COLORS.map((color) => (
            <option key={color}>{color}</option>
          ))}
        </select>
      </label>
      <label>
        Size
        <select name="size" value={values.size} onChange={change} disabled={isEditing}>
          {SIZES.map((size) => (
            <option key={size}>{size}</option>
          ))}
        </select>
      </label>
      <label>
        Price ({CURRENCY})
        <input
          name="price"
          type="number"
          min="0.01"
          step="0.01"
          required
          value={values.price}
          onChange={change}
        />
      </label>
      <label>
        Quantity
        <input
          name="quantity"
          type="number"
          min={isEditing ? 0 : 1}
          step="1"
          required
          value={values.quantity}
          onChange={change}
        />
      </label>
      {error && <p className="error">{error}</p>}
      <div className="actions">
        <button type="submit">{isEditing ? 'Save' : 'Add'}</button>
        <button type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  )
}
