import { useEffect, useRef, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import ChangePasswordModal from './ChangePasswordModal';
import DeleteAccountModal from './DeleteAccountModal';
import styles from './NavBar.module.css';

export function NavBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const [showChangePwd, setShowChangePwd] = useState(false);
  const [showDeleteAcc, setShowDeleteAcc] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    if (!menuOpen) return;
    function handleOutsideClick(e) {
      if (!menuRef.current?.contains(e.target)) setMenuOpen(false);
    }
    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, [menuOpen]);

  async function handleSignOut() {
    setMenuOpen(false);
    await logout();
    navigate('/signin');
  }

  function linkClass({ isActive }) {
    return isActive ? `${styles.link} ${styles.activeLink}` : styles.link;
  }

  return (
    <>
      <nav className={styles.nav} role="navigation" aria-label="Main navigation">
        <NavLink to="/" className={styles.logo}>ChatApp</NavLink>

        <div className={styles.links}>
          <NavLink to="/rooms" className={linkClass}>Rooms</NavLink>
          <NavLink to="/contacts" className={linkClass}>Contacts</NavLink>
          <NavLink to="/sessions" className={linkClass}>Sessions</NavLink>
        </div>

        <div className={styles.profileWrapper} ref={menuRef}>
          <button
            className={styles.profileBtn}
            onClick={() => setMenuOpen((v) => !v)}
            aria-haspopup="true"
            aria-expanded={menuOpen}
            aria-label="Profile menu"
          >
            {user?.username} ▾
          </button>

          {menuOpen && (
            <div className={styles.dropdown} role="menu">
              <button
                role="menuitem"
                onClick={() => { setShowChangePwd(true); setMenuOpen(false); }}
              >
                Change Password
              </button>
              <button
                role="menuitem"
                className={styles.danger}
                onClick={() => { setShowDeleteAcc(true); setMenuOpen(false); }}
              >
                Delete Account
              </button>
              <div className={styles.divider} />
              <button
                role="menuitem"
                onClick={handleSignOut}
              >
                Sign Out
              </button>
            </div>
          )}
        </div>
      </nav>

      {showChangePwd && (
        <ChangePasswordModal onClose={() => setShowChangePwd(false)} />
      )}
      {showDeleteAcc && (
        <DeleteAccountModal
          onClose={() => setShowDeleteAcc(false)}
          onDeleted={() => navigate('/signin')}
        />
      )}
    </>
  );
}
