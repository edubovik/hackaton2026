import { render, screen } from '@testing-library/react'
import App from './App'

test('renders app heading', () => {
  render(<App />)
  expect(screen.getByRole('heading', { name: /chatapp/i })).toBeInTheDocument()
})

test('renders loading text', () => {
  render(<App />)
  expect(screen.getByText(/loading/i)).toBeInTheDocument()
})
