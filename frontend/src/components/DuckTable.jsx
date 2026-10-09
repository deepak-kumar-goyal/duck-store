import { CURRENCY } from '../constants'

export default function DuckTable({ ducks, onEdit, onDelete }) {
  return (
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>Color</th>
          <th>Size</th>
          <th>Price</th>
          <th>Quantity</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {ducks.map((duck) => (
          <tr key={duck.id}>
            <td>{duck.id}</td>
            <td>{duck.color}</td>
            <td>{duck.size}</td>
            <td>{duck.price.toFixed(2)} {CURRENCY}</td>
            <td>{duck.quantity}</td>
            <td>
              <button type="button" className="link" onClick={() => onEdit(duck)}>
                edit
              </button>
              {' / '}
              <button type="button" className="link" onClick={() => onDelete(duck)}>
                delete
              </button>
            </td>
          </tr>
        ))}
        {ducks.length === 0 && (
          <tr>
            <td colSpan="6">No ducks in the warehouse yet.</td>
          </tr>
        )}
      </tbody>
    </table>
  )
}
