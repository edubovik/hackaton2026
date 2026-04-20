import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { PresenceIndicator } from './PresenceIndicator';

describe('PresenceIndicator', () => {
  it('renders ● for ONLINE', () => {
    render(<PresenceIndicator state="ONLINE" />);
    const el = screen.getByLabelText('Online');
    expect(el).toBeInTheDocument();
    expect(el).toHaveTextContent('●');
  });

  it('renders ◐ for AFK', () => {
    render(<PresenceIndicator state="AFK" />);
    const el = screen.getByLabelText('Away');
    expect(el).toBeInTheDocument();
    expect(el).toHaveTextContent('◐');
  });

  it('renders ○ for OFFLINE', () => {
    render(<PresenceIndicator state="OFFLINE" />);
    const el = screen.getByLabelText('Offline');
    expect(el).toBeInTheDocument();
    expect(el).toHaveTextContent('○');
  });

  it('defaults to OFFLINE when state is not provided', () => {
    render(<PresenceIndicator />);
    expect(screen.getByLabelText('Offline')).toBeInTheDocument();
  });

  it('defaults to OFFLINE for unknown state', () => {
    render(<PresenceIndicator state="UNKNOWN" />);
    expect(screen.getByLabelText('Offline')).toBeInTheDocument();
  });
});
