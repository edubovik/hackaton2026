import { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../api/auth';
import styles from './ForgotPasswordPage.module.css';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!email) {
      setError('Email is required');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await forgotPassword(email);
      setSubmitted(true);
    } catch {
      setError('Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1>Forgot Password</h1>
        {submitted ? (
          <p className={styles.success}>
            If an account with that email exists, a reset link has been sent. Check your inbox.
          </p>
        ) : (
          <>
            {error && <div className={styles.error}>{error}</div>}
            <p className={styles.hint}>
              Enter your email and we&apos;ll send you a reset link.
            </p>
            <form onSubmit={handleSubmit} noValidate>
              <div className={styles.field}>
                <label htmlFor="email">Email</label>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  autoComplete="email"
                />
              </div>
              <button className={styles.button} type="submit" disabled={loading}>
                {loading ? 'Sending…' : 'Send reset link'}
              </button>
            </form>
          </>
        )}
        <Link className={styles.link} to="/signin">Back to Sign In</Link>
      </div>
    </div>
  );
}
