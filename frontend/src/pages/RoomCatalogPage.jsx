import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listRooms, joinRoom } from '../api/rooms';
import { RoomListItem } from '../components/RoomListItem';
import { CreateRoomModal } from '../components/CreateRoomModal';
import { NavBar } from '../components/NavBar';
import styles from './RoomCatalogPage.module.css';

export default function RoomCatalogPage() {
  const navigate = useNavigate();
  const [rooms, setRooms] = useState([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [showCreate, setShowCreate] = useState(false);
  const [error, setError] = useState('');

  async function load(q, p) {
    const data = await listRooms(q, p);
    setRooms(data.content);
    setTotalPages(data.totalPages);
  }

  useEffect(() => { load(search, page); }, [search, page]);

  function handleSearch(e) {
    setSearch(e.target.value);
    setPage(0);
  }

  async function handleJoin(roomId) {
    setError('');
    try {
      await joinRoom(roomId);
      navigate('/');
    } catch (err) {
      setError(err.message);
    }
  }

  function handleCreated(room) {
    setShowCreate(false);
    load(search, page);
  }

  return (
    <div className={styles.wrapper}>
    <NavBar />
    <main className={styles.page}>
      <div className={styles.toolbar}>
        <h2>Room Catalog</h2>
        <input
          placeholder="Search rooms…"
          value={search}
          onChange={handleSearch}
          className={styles.search}
        />
        <button onClick={() => setShowCreate(true)}>+ Create Room</button>
      </div>

      {error && <p className={styles.error}>{error}</p>}

      <ul className={styles.list}>
        {rooms.map(r => (
          <RoomListItem
            key={r.id}
            room={r}
            onJoin={handleJoin}
            onSelect={() => {}}
          />
        ))}
        {rooms.length === 0 && <li className={styles.empty}>No rooms found</li>}
      </ul>

      {totalPages > 1 && (
        <div className={styles.pagination}>
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>Prev</button>
          <span>{page + 1} / {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next</button>
        </div>
      )}

      {showCreate && (
        <CreateRoomModal
          onCreated={handleCreated}
          onClose={() => setShowCreate(false)}
        />
      )}
    </main>
    </div>
  );
}
