import { NavBar } from '../components/NavBar';
import { ContactsPanel } from '../components/ContactsPanel';
import styles from './ContactsPage.module.css';

export default function ContactsPage() {
  return (
    <div className={styles.wrapper}>
      <NavBar />
      <main className={styles.page}>
        <ContactsPanel />
      </main>
    </div>
  );
}
