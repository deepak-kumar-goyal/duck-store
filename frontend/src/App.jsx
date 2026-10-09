import { useCallback, useEffect, useState } from 'react'
import { addDuck, deleteDuck, listDucks, updateDuck } from './api/ducks'
import DuckForm from './components/DuckForm'
import DuckTable from './components/DuckTable'
import './App.css'

function App() {
  const [ducks, setDucks] = useState([])
  // null = form closed, 'new' = adding, a duck object = editing that duck
  const [formTarget, setFormTarget] = useState(null)
  const [error, setError] = useState('')

  const refresh = useCallback(async () => {
    try {
      setDucks(await listDucks())
      setError('')
    } catch (e) {
      setError(e.message)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial data load
    refresh()
  }, [refresh])

  const save = async (values) => {
    if (formTarget === 'new') {
      await addDuck(values)
    } else {
      await updateDuck(formTarget.id, values)
    }
    setFormTarget(null)
    await refresh()
  }

  const remove = async (duck) => {
    if (!window.confirm(`Delete ${duck.color} ${duck.size} duck ${duck.id}?`)) {
      return
    }
    try {
      await deleteDuck(duck.id)
      await refresh()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <main>
      <h1>Duck Warehouse</h1>
      {error && <p className="error">{error}</p>}
      {formTarget ? (
        <DuckForm
          key={formTarget === 'new' ? 'new' : formTarget.id}
          duck={formTarget === 'new' ? null : formTarget}
          onSubmit={save}
          onCancel={() => setFormTarget(null)}
        />
      ) : (
        <button type="button" onClick={() => setFormTarget('new')}>
          Add duck
        </button>
      )}
      <DuckTable ducks={ducks} onEdit={setFormTarget} onDelete={remove} />
    </main>
  )
}

export default App
