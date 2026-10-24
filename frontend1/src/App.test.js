import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';

test('renders login button', async () => {
  localStorage.clear();
  render(<App />);
  expect(await screen.findByLabelText('Email')).toBeInTheDocument();
});

test('stays on the form when the server is down', async () => {
  localStorage.clear();
  global.fetch = jest.fn(() => Promise.reject(new TypeError('Failed to fetch')));
  render(<App />);
  await userEvent.type(await screen.findByLabelText('Email'), 'a@b.com');
  await userEvent.type(screen.getByLabelText('Password'), 'secret');
  const loginButtons = screen.getAllByRole('button', { name: 'Login' });
  await userEvent.click(loginButtons[loginButtons.length - 1]);
  expect(await screen.findByText('Cannot reach the server')).toBeInTheDocument();
  expect(screen.getByLabelText('Email')).toBeInTheDocument();
});
