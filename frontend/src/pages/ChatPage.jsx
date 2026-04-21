import { useState, useCallback, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useMessages } from '../hooks/useMessages';
import { useUnread } from '../hooks/useUnread';
import { usePresence } from '../hooks/usePresence';
import { MessageList } from '../components/MessageList';
import { MessageComposer } from '../components/MessageComposer';
import { MembersPanel } from '../components/MembersPanel';
import { ContactsPanel } from '../components/ContactsPanel';
import { ManageRoomModal } from '../components/ManageRoomModal';
import { UnreadBadge } from '../components/UnreadBadge';
import { PresenceIndicator } from '../components/PresenceIndicator';
import { NavBar } from '../components/NavBar';
import { getFriends } from '../api/contacts';
import { getMyRooms, listMembers, leaveRoom } from '../api/rooms';
import { sendRoomMessage, sendDmMessage, connect, disconnect, onConnectionChange, isConnected } from '../api/socket';
import { uploadAttachment } from '../api/attachments';
import styles from './ChatPage.module.css';

export default function ChatPage() {
  const { user } = useAuth();
  const [rooms, setRooms] = useState([]);
  const [friends, setFriends] = useState([]);
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedFriend, setSelectedFriend] = useState(null);
  const [showContacts, setShowContacts] = useState(false);
  const [showManage, setShowManage] = useState(false);
  const [replyTo, setReplyTo] = useState(null);
  const [myRole, setMyRole] = useState(null);
  const [socketConnected, setSocketConnected] = useState(isConnected);

  const roomId = selectedRoom?.id ?? null;
  const partnerId = selectedFriend?.userId ?? null;

  const { counts, clearRoom, clearDm, getCountForRoom, getCountForDm } = useUnread({
    rooms,
    userId: user?.id,
    activeRoomId: roomId,
    activeDmPartnerId: partnerId,
  });
  const presenceMap = usePresence();

  const { messages, hasMore, loading, loadMore, upsertMessage } = useMessages({
    roomId,
    partnerId,
    currentUserId: user?.id,
  });

  useEffect(() => {
    connect();
    const unsub = onConnectionChange(setSocketConnected);
    return () => { disconnect(); unsub(); };
  }, []);

  function loadRooms() {
    getMyRooms().then(setRooms).catch(() => {});
  }

  useEffect(() => { loadRooms(); }, []);

  useEffect(() => {
    getFriends().then(setFriends).catch(() => {});
  }, []);

  useEffect(() => {
    if (!selectedRoom) { setMyRole(null); return; }
    listMembers(selectedRoom.id)
      .then((members) => {
        const me = members.find((m) => m.userId === user?.id);
        setMyRole(me?.role ?? null);
      })
      .catch(() => setMyRole(null));
  }, [selectedRoom, user?.id]);

  useEffect(() => {
    if (roomId) clearRoom(roomId);
  }, [roomId, clearRoom]);

  useEffect(() => {
    if (partnerId) clearDm(partnerId);
  }, [partnerId, clearDm]);

  const handleSend = useCallback((content, replyToId) => {
    if (roomId) sendRoomMessage(roomId, content, replyToId);
    else if (partnerId) sendDmMessage(partnerId, content, replyToId);
  }, [roomId, partnerId]);

  const handleSendAttachment = useCallback(async (file, content, replyToId) => {
    try {
      await uploadAttachment({ file, roomId, recipientId: partnerId, content, replyToId });
    } catch (err) {
      console.error('Attachment upload failed', err);
    }
  }, [roomId, partnerId]);

  async function handleLeave(room) {
    if (!window.confirm(`Leave room "${room.name}"?`)) return;
    try {
      await leaveRoom(room.id);
      if (selectedRoom?.id === room.id) setSelectedRoom(null);
      loadRooms();
    } catch (err) {
      console.error('Leave failed', err);
    }
  }

  function handleRoomDeleted() {
    setShowManage(false);
    setSelectedRoom(null);
    loadRooms();
  }

  const isAdminOrOwner = myRole === 'ADMIN' || myRole === 'OWNER';
  const isOwner = myRole === 'OWNER';
  const chatTitle = selectedRoom?.name ?? selectedFriend?.username ?? 'Select a conversation';

  const publicRooms = rooms.filter(r => r.isPublic);
  const privateRooms = rooms.filter(r => !r.isPublic);

  function renderRoom(r) {
    const isSelected = selectedRoom?.id === r.id;
    const isRoomOwner = r.ownerId === user?.id;
    return (
      <div key={r.id} className={`${styles.sideItem} ${isSelected ? styles.active : ''}`}>
        <button
          className={styles.sideItemBtn}
          onClick={() => { setSelectedRoom(r); setSelectedFriend(null); setShowContacts(false); }}
        >
          <span className={styles.sideItemName}>#{r.name}</span>
          <UnreadBadge count={getCountForRoom(r.id)} />
        </button>
        <div className={styles.sideItemActions}>
          {isSelected && isAdminOrOwner && (
            <button
              className={styles.sideIconBtn}
              title="Manage room"
              onClick={() => setShowManage(true)}
            >⚙</button>
          )}
          {!isRoomOwner && (
            <button
              className={styles.sideIconBtn}
              title="Leave room"
              onClick={() => handleLeave(r)}
            >✕</button>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className={styles.wrapper}>
    <NavBar />
    <div className={styles.layout}>
      <aside className={styles.sidebar}>
        <section className={styles.sideSection}>
          <div className={styles.sideTitleRow}>
            <h4 className={styles.sideTitle}>Rooms</h4>
          </div>
          {publicRooms.map(renderRoom)}
          {privateRooms.length > 0 && (
            <>
              <p className={styles.sideSubtitle}>Private</p>
              {privateRooms.map(renderRoom)}
            </>
          )}
          {rooms.length === 0 && (
            <p className={styles.emptyHint}>Join rooms from the <a href="/rooms">catalog</a></p>
          )}
        </section>

        <section className={styles.sideSection}>
          <div className={styles.sideTitleRow}>
            <h4 className={styles.sideTitle}>Direct Messages</h4>
            <button
              className={`${styles.sideManageBtn} ${showContacts ? styles.sideManageBtnActive : ''}`}
              onClick={() => { setShowContacts(v => !v); setSelectedRoom(null); setSelectedFriend(null); }}
              title="Manage contacts"
            >+</button>
          </div>
          {friends.map((f) => (
            <button
              key={f.userId}
              className={`${styles.sideItem} ${styles.sideItemFlat} ${selectedFriend?.userId === f.userId ? styles.active : ''}`}
              onClick={() => { setSelectedFriend(f); setSelectedRoom(null); setShowContacts(false); }}
            >
              <PresenceIndicator state={presenceMap[f.userId] ?? f.presence ?? 'OFFLINE'} />
              <span className={styles.sideItemName}>{f.username}</span>
              <UnreadBadge count={getCountForDm(f.userId)} />
            </button>
          ))}
          {friends.length === 0 && <p className={styles.emptyHint}>No friends yet — click + to add</p>}
        </section>
      </aside>

      <main className={styles.main}>
        <header className={styles.header}>
          <h2 className={styles.title}>{showContacts ? 'Contacts' : chatTitle}</h2>
        </header>

        {showContacts ? (
          <ContactsPanel onFriendsChanged={setFriends} />
        ) : (selectedRoom || selectedFriend) ? (
          <>
            <MessageList
              key={roomId ?? `dm-${partnerId}`}
              messages={messages}
              hasMore={hasMore}
              loading={loading}
              onLoadMore={loadMore}
              currentUserId={user?.id}
              isRoomAdmin={isAdminOrOwner}
              onReply={setReplyTo}
              onMessageUpdated={upsertMessage}
            />
            <MessageComposer
              onSend={handleSend}
              onSendAttachment={handleSendAttachment}
              replyTo={replyTo}
              onCancelReply={() => setReplyTo(null)}
              disabled={!socketConnected}
            />
          </>
        ) : (
          <div className={styles.empty}>Select a room or friend to start chatting</div>
        )}
      </main>

      {selectedRoom && (
        <MembersPanel
          roomId={selectedRoom.id}
          currentUserId={user?.id}
          isAdminOrOwner={isAdminOrOwner}
          presenceMap={presenceMap}
        />
      )}
    </div>

    {showManage && selectedRoom && (
      <ManageRoomModal
        room={selectedRoom}
        currentUserId={user?.id}
        isOwner={isOwner}
        onClose={() => setShowManage(false)}
        onDeleted={handleRoomDeleted}
      />
    )}
    </div>
  );
}
