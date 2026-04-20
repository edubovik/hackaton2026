import { useState, useCallback } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useMessages } from '../hooks/useMessages';
import { useUnread } from '../hooks/useUnread';
import { MessageList } from '../components/MessageList';
import { MessageComposer } from '../components/MessageComposer';
import { MembersPanel } from '../components/MembersPanel';
import { RoomPanel } from '../components/RoomPanel';
import { UnreadBadge } from '../components/UnreadBadge';
import { getFriends } from '../api/contacts';
import { listMembers } from '../api/rooms';
import { sendRoomMessage, sendDmMessage } from '../api/socket';
import { useEffect } from 'react';
import styles from './ChatPage.module.css';

export default function ChatPage() {
  const { user } = useAuth();
  const [rooms, setRooms] = useState([]);
  const [friends, setFriends] = useState([]);
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedFriend, setSelectedFriend] = useState(null);
  const [replyTo, setReplyTo] = useState(null);
  const [myRole, setMyRole] = useState(null);

  const { counts, clearRoom, clearDm, getCountForRoom, getCountForDm } = useUnread();

  const roomId = selectedRoom?.id ?? null;
  const partnerId = selectedFriend?.userId ?? null;

  const { messages, hasMore, loading, loadMore } = useMessages({
    roomId,
    partnerId,
    currentUserId: user?.id,
  });

  useEffect(() => {
    getFriends().then(setFriends).catch(() => {});
  }, []);

  // Fetch rooms user is a member of (joined rooms via room catalog context)
  useEffect(() => {
    // rooms are stored in RoomPanel via its own state — we use the selected room
  }, []);

  // Resolve current user's role in selected room
  useEffect(() => {
    if (!selectedRoom) { setMyRole(null); return; }
    listMembers(selectedRoom.id)
      .then((members) => {
        const me = members.find((m) => m.userId === user?.id);
        setMyRole(me?.role ?? null);
      })
      .catch(() => setMyRole(null));
  }, [selectedRoom, user?.id]);

  // Clear unread when switching to a room/DM
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

  const handleMessageUpdated = useCallback((updated) => {
    // useMessages will receive the update via the STOMP subscription
    // No local state mutation needed
  }, []);

  const isAdminOrOwner = myRole === 'ADMIN' || myRole === 'OWNER';
  const chatTitle = selectedRoom?.name ?? selectedFriend?.username ?? 'Select a conversation';

  return (
    <div className={styles.layout}>
      {/* Left sidebar */}
      <aside className={styles.sidebar}>
        <section className={styles.sideSection}>
          <h4 className={styles.sideTitle}>Rooms</h4>
          {rooms.map((r) => (
            <button
              key={r.id}
              className={`${styles.sideItem} ${selectedRoom?.id === r.id ? styles.active : ''}`}
              onClick={() => { setSelectedRoom(r); setSelectedFriend(null); }}
            >
              <span className={styles.sideItemName}>#{r.name}</span>
              <UnreadBadge count={getCountForRoom(r.id)} />
            </button>
          ))}
          {rooms.length === 0 && (
            <p className={styles.emptyHint}>Join rooms from the <a href="/rooms">catalog</a></p>
          )}
        </section>

        <section className={styles.sideSection}>
          <h4 className={styles.sideTitle}>Direct Messages</h4>
          {friends.map((f) => (
            <button
              key={f.userId}
              className={`${styles.sideItem} ${selectedFriend?.userId === f.userId ? styles.active : ''}`}
              onClick={() => { setSelectedFriend(f); setSelectedRoom(null); }}
            >
              <span className={styles.sideItemName}>{f.username}</span>
              <UnreadBadge count={getCountForDm(f.userId)} />
            </button>
          ))}
          {friends.length === 0 && <p className={styles.emptyHint}>Add friends to start DMs</p>}
        </section>
      </aside>

      {/* Main chat area */}
      <main className={styles.main}>
        <header className={styles.header}>
          <h2 className={styles.title}>{chatTitle}</h2>
        </header>

        {(selectedRoom || selectedFriend) ? (
          <>
            <MessageList
              messages={messages}
              hasMore={hasMore}
              loading={loading}
              onLoadMore={loadMore}
              currentUserId={user?.id}
              isRoomAdmin={isAdminOrOwner}
              onReply={setReplyTo}
              onMessageUpdated={handleMessageUpdated}
            />
            <MessageComposer
              onSend={handleSend}
              replyTo={replyTo}
              onCancelReply={() => setReplyTo(null)}
            />
          </>
        ) : (
          <div className={styles.empty}>Select a room or friend to start chatting</div>
        )}
      </main>

      {/* Right panel: members (rooms only) */}
      {selectedRoom && (
        <MembersPanel
          roomId={selectedRoom.id}
          currentUserId={user?.id}
          isAdminOrOwner={isAdminOrOwner}
        />
      )}
    </div>
  );
}
