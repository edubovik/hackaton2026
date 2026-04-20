# REQUIREMENTS.md — Online Chat Server

This is the source of truth for what to build. Claude Code must read this file at the start of every session and implement only what is described here. Do not add features not listed.

---

## 1. Introduction

A classic web-based online chat application supporting:

- User registration and authentication
- Public and private chat rooms
- One-to-one personal messaging
- Contacts / friends
- File and image sharing
- Basic moderation and administration
- Persistent message history

The application should resemble a classic web chat (not a modern social network or collaboration suite). It must support up to **300 simultaneously connected users**.

---

## 2. Functional Requirements

### 2.1 User Accounts and Authentication

#### 2.1.1 Registration

Users can self-register using:
- Email
- Password
- Unique username

**Rules:**
- Email must be unique
- Username must be unique
- Username is immutable after registration
- Email verification is not required

#### 2.1.2 Authentication

- Sign in with email and password
- Sign out — logs out the current browser session only; other active sessions are not affected
- Persistent login across browser close/reopen ("Keep me signed in")

#### 2.1.3 Password Management

- Password reset (via email link)
- Password change for logged-in users
- No forced periodic password change required
- Passwords must be stored securely in hashed form

#### 2.1.4 Account Removal

Users can delete their own account. On deletion:
- Account is removed
- Only chat rooms **owned** by that user are deleted
- All messages, files, and images in those deleted rooms are permanently deleted
- Membership in other rooms is removed

---

### 2.2 User Presence and Sessions

#### 2.2.1 Presence States

- `online`
- `AFK`
- `offline`

#### 2.2.2 AFK Rule

A user is AFK if they have not interacted with any open browser tab for more than **1 minute**. If active in at least one tab → online.

#### 2.2.3 Multi-Tab Support

- If active in at least one tab → appears online to others
- AFK only when **all** tabs have been inactive for more than 1 minute
- Offline only when **all** browser tabs are closed or offloaded

#### 2.2.4 Active Sessions

Users can:
- View a list of their active sessions (including browser/IP details)
- Log out individual sessions from that screen
- Logging out from the current browser invalidates only that browser's session

---

### 2.3 Contacts / Friends

#### 2.3.1 Friend List

Each user has a personal contact/friend list.

#### 2.3.2 Sending Friend Requests

A user can send a friend request:
- By username
- From the user list inside a chat room

A friend request may include optional text.

#### 2.3.3 Friendship Confirmation

Adding a friend requires confirmation by the recipient.

#### 2.3.4 Removing Friends

A user may remove another user from their friend list.

#### 2.3.5 User-to-User Ban

A user may ban another user. Ban effects:
- Banned user cannot contact the banning user in any way
- New personal messaging between them is blocked
- Existing personal message history remains visible but becomes read-only/frozen
- Friend relationship is effectively terminated

#### 2.3.6 Personal Messaging Rule

Users may exchange personal messages **only if** they are friends and neither side has banned the other.

---

### 2.4 Chat Rooms

#### 2.4.1 Creation

Any registered user may create a chat room.

#### 2.4.2 Room Properties

- Name (must be unique)
- Description
- Visibility: public or private
- Owner
- Admins
- Members
- Banned users list

#### 2.4.3 Public Rooms

- Listed in a public catalog showing: name, description, current member count
- Catalog supports simple search
- Any authenticated user can join unless banned from that room

#### 2.4.4 Private Rooms

- Not visible in the public catalog
- Users may join only by invitation

#### 2.4.5 Joining and Leaving Rooms

- Users may freely join public rooms unless banned
- Users may leave rooms freely
- The owner cannot leave their own room — they can only delete it

#### 2.4.6 Room Deletion

On room deletion:
- All messages are permanently deleted
- All files and images are permanently deleted

#### 2.4.7 Owner and Admin Roles

Each room has one owner. The owner is always an admin and cannot lose admin privileges.

**Admins may:**
- Delete messages in the room
- Remove members from the room
- Ban members from the room
- View the list of banned users and who banned each user
- Remove users from the ban list
- Remove admin status from other admins (except the owner)

**Owner may additionally:**
- Remove any admin
- Remove any member
- Delete the room

#### 2.4.8 Room Ban Rules

Removing a user from a room by an admin = a ban:
- User is removed and cannot rejoin unless unbanned
- User loses access to room messages, files, and images in the UI
- Existing files remain stored unless the room itself is deleted

#### 2.4.9 Room Invitations

Users may invite other users to private rooms.

---

### 2.5 Messaging

#### 2.5.1 Room and Personal Chat Model

- Personal dialogs behave the same as room chats from a UI/feature perspective
- A personal dialog is a chat with exactly two fixed participants
- Personal chats support the same message and attachment features as room chats
- Personal chats do not have admins (only room chats have owner/admin moderation)

#### 2.5.2 Message Content

Messages may contain:
- Plain text
- Multiline text
- Emoji
- Attachments
- Reply/reference to another message

**Constraints:**
- Maximum text size per message: 3 KB
- Message text must support UTF-8

#### 2.5.3 Message Replies

- A user may reply to another message
- The replied-to message is visually quoted/outlined in the UI

#### 2.5.4 Message Editing

- Users may edit their own messages
- Edited messages show a gray "edited" indicator

#### 2.5.5 Message Deletion

Messages may be deleted by:
- The message author
- Room admins (in room chats only)

Deleted messages are not required to be recoverable.

#### 2.5.6 Message Ordering and History

- Messages are stored persistently in chronological order
- Infinite scroll for older history
- Messages sent to offline users are persisted and delivered when the recipient next opens the app

---

### 2.6 Attachments

#### 2.6.1 Supported Types

- Images
- Arbitrary file types

#### 2.6.2 Upload Methods

- Explicit upload button
- Copy and paste

#### 2.6.3 Metadata

- Original file name is preserved
- User may add an optional comment to an attachment

#### 2.6.4 Access Control

- Files and images may be downloaded only by current members of the room or authorized participants of the personal chat
- If a user loses room access, they immediately lose access to all room files and images

#### 2.6.5 Persistence

- If an uploader later loses room access, the file remains stored
- The uploader can no longer see, download, or manage it

---

### 2.7 Notifications

#### 2.7.1 Unread Indicators

- UI shows a notification indicator next to room or contact name when there are unread messages
- Indicator is cleared when the user opens the corresponding chat

#### 2.7.2 Presence Update Speed

Online/AFK/offline presence updates must propagate with low latency (target: below 2 seconds).

---

## 3. Non-Functional Requirements

### 3.1 Capacity and Scale

- Up to 300 simultaneous users
- A single chat room may contain up to 1000 participants
- A user may belong to an unlimited number of rooms
- Typical user: ~20 rooms, ~50 contacts

### 3.2 Performance

- Message delivery to recipients within 3 seconds of sending
- Presence update propagation below 2 seconds
- Application must remain usable in rooms with at least 10,000 messages

### 3.3 Persistence

- Messages must be stored persistently and remain available for years
- Chat history loading must support infinite scrolling

### 3.4 File Storage

- Files stored on the local file system
- Maximum file size: 20 MB
- Maximum image size: 3 MB

### 3.5 Session Behavior

- No automatic logout due to inactivity
- Login state persists across browser close/reopen
- Application works correctly across multiple tabs for the same user

### 3.6 Reliability

The system must preserve consistency of:
- Membership
- Room bans
- File access rights
- Message history
- Admin/owner permissions

---

## 4. UI Requirements

### 4.1 General Layout

Standard web chat layout:
- Top navigation menu
- Message area in the center
- Message input at the bottom
- Rooms and contacts list on the side (right)

#### 4.1.1 Side Layout

- Rooms and contacts displayed on the right sidebar
- After entering a room, the room list compacts in accordion style
- Room members shown on the right side with online statuses

### 4.2 Chat Window Behavior

- Auto-scroll to new messages when user is already at the bottom
- No forced auto-scroll if user has scrolled up to read history
- Infinite scroll for older messages

### 4.3 Message Composition Input

- Multiline text entry
- Emoji support
- File/image attachment
- Reply to message

### 4.4 Unread Indicators

Unread message indicators shown near:
- Room names
- Contact names

### 4.5 Admin UI

Administrative actions available from menus, implemented through modal dialogs:
- Ban / unban user
- Remove member
- Manage admins
- View banned users
- Delete messages
- Delete room

---

## 5. Authentication Implementation

- Use **JWT tokens** for authentication
- Tokens must be stored securely (httpOnly cookies preferred)
- Persistent login implemented via refresh token mechanism
- Each browser session has its own token — sign out invalidates only that token

---

## 6. Advanced Requirements (implement only after core is complete)

> Implement these only if the core requirements are fully working and tested.

### 6.1 Jabber / XMPP Protocol Support

- Users can connect to the server using a Jabber client
- Use an available Jabber library for Java
- Servers must support **federation** (messages between two separate server instances)
- Docker Compose must be updated to support the two-server federation setup

**Admin UI additions:**
- Connection dashboard for admin
- Federation traffic info / statistics

**Load test for federation:**
- 50+ clients connected to server A, 50+ to server B
- Messaging from A to B and back

---

## 7. Submission

- Public GitHub repository
- Must be buildable and runnable via `docker compose up` in the root folder

---

## Appendix A: Wireframes

### Auth screens

```
+--------------------------------------+    +----------------------------------------+
|            SIGN IN                   |    |               REGISTER                 |
|--------------------------------------|    |----------------------------------------|
| Email                                |    | Email                                  |
| [______________________________]     |    | [______________________________]       |
| Password                             |    | Username                               |
| [______________________________]     |    | [______________________________]       |
| [ ] Keep me signed in                |    | Password                               |
|                                      |    | [______________________________]       |
| ( Sign in )                          |    | Confirm password                       |
|                                      |    | [______________________________]       |
| Forgot password?                     |    |                                        |
+--------------------------------------+    | ( Create account )                     |
                                            +----------------------------------------+
```

### Main chat layout

```
+------------------------------------------------------------------------------------------------------+
| ChatLogo | Public Rooms | Private Rooms | Contacts | Sessions | Profile ▼ | Sign out                |
+------------------------------------------------------------------------------------------------------+
+----------------------------+------------------------------------------------+------------------------+
| RIGHT SIDEBAR              |              MAIN CHAT                         | MEMBERS / CONTEXT      |
|----------------------------|------------------------------------------------|------------------------|
| Search [______________]    | # engineering-room                             | Room info              |
|                            |------------------------------------------------| Owner: alice           |
| ROOMS                      | [10:21] Bob: Hello team                        | Admins: alice, dave    |
| > Public Rooms             | [10:22] Alice: Uploading spec                  |                        |
|   • general        (3)     | [10:23] You: Here's the file                   | Members (38)           |
|   • engineering           |   [ spec-v3.pdf — latest requirements ]         | ● Alice                |
| > Private Rooms            | [10:25] Carol replied to Bob:                  | ● Bob                  |
|   • core-team      (1)     |   > Hello team                                 | ◐ Carol (AFK)          |
|                            |   Can we make this private?                    | ○ Mike (offline)       |
| CONTACTS                   |                                                |                        |
|   ● Alice                  |                                                | [Invite user]          |
|   ◐ Bob                    |                                                | [Manage room]          |
|   ○ Carol          (2)     |                                                |                        |
| [Create room]              |                                                |                        |
+----------------------------+------------------------------------------------+------------------------+
| [😊] [Attach] [Replying to: Bob ×]                    [ message input / Send ]                      |
+------------------------------------------------------------------------------------------------------+
```

### Manage Room modal

```
| Manage Room: #engineering-room                                                                       |
| Tabs: [Members] [Admins] [Banned users] [Invitations] [Settings]                                    |

[Members tab]
| Username   Status    Role     Actions                                                                |
| alice      online    Owner    --                                                                     |
| dave       AFK       Admin    [Remove admin] [Ban]                                                   |
| bob        online    Member   [Make admin] [Ban] [Remove from room]                                  |

[Banned users tab]
| Username   Banned by   Date/time              Actions                                                |
| mike       alice       2026-04-18 13:25       [Unban]                                                |

[Settings tab]
| Room name   [ engineering-room ]                                                                     |
| Description [ backend + frontend discussions ]                                                       |
| Visibility  (•) Public  ( ) Private                                                                  |
| [ Save changes ]                                              [ Delete room ]                        |
```
