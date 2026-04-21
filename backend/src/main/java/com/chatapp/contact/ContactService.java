package com.chatapp.contact;

import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.common.exception.BadRequestException;
import com.chatapp.common.exception.ForbiddenException;
import com.chatapp.contact.dto.BannedContactDto;
import com.chatapp.contact.dto.FriendDto;
import com.chatapp.contact.dto.FriendRequestDto;
import com.chatapp.contact.entity.FriendRequest;
import com.chatapp.contact.entity.FriendRequestStatus;
import com.chatapp.contact.entity.Friendship;
import com.chatapp.contact.entity.UserBan;
import com.chatapp.contact.repository.FriendRequestRepository;
import com.chatapp.contact.repository.FriendshipRepository;
import com.chatapp.contact.repository.UserBanRepository;
import com.chatapp.common.BrokerTemplate;
import com.chatapp.presence.repository.UserPresenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ContactService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBanRepository userBanRepository;
    private final UserRepository userRepository;
    private final UserPresenceRepository presenceRepository;
    private final BrokerTemplate messagingTemplate;

    public ContactService(FriendRequestRepository friendRequestRepository,
                          FriendshipRepository friendshipRepository,
                          UserBanRepository userBanRepository,
                          UserRepository userRepository,
                          UserPresenceRepository presenceRepository,
                          BrokerTemplate messagingTemplate) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.userBanRepository = userBanRepository;
        this.userRepository = userRepository;
        this.presenceRepository = presenceRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public FriendRequestDto sendRequest(User from, String toUsername, String message) {
        User to = userRepository.findByUsername(toUsername)
                .orElseThrow(() -> new BadRequestException("User not found: " + toUsername));

        if (from.getId().equals(to.getId())) {
            throw new BadRequestException("Cannot send friend request to yourself");
        }
        if (userBanRepository.existsByBannerIdAndBannedId(to.getId(), from.getId())) {
            throw new ForbiddenException("Cannot send friend request");
        }
        if (friendRequestRepository.findPendingBetween(from.getId(), to.getId()).isPresent()) {
            throw new BadRequestException("Friend request already sent");
        }
        if (friendshipRepository.existsBetween(from.getId(), to.getId())) {
            throw new BadRequestException("Already friends");
        }

        // Remove any old non-pending request so the UNIQUE constraint allows a new INSERT
        friendRequestRepository.findByFromUser_IdAndToUser_Id(from.getId(), to.getId())
                .ifPresent(friendRequestRepository::delete);

        FriendRequest request = friendRequestRepository.save(new FriendRequest(from, to, message));

        messagingTemplate.send(
                "/queue/user." + to.getId(),
                Map.of("type", "FRIEND_REQUEST",
                       "requestId", request.getId(),
                       "fromUserId", from.getId(),
                       "fromUsername", from.getUsername()));

        return FriendRequestDto.from(request);
    }

    @Transactional(readOnly = true)
    public List<FriendRequestDto> getIncomingRequests(User user) {
        return friendRequestRepository
                .findByToUser_IdAndStatus(user.getId(), FriendRequestStatus.PENDING)
                .stream()
                .map(FriendRequestDto::from)
                .toList();
    }

    @Transactional
    public void acceptRequest(User user, Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Request not found"));

        if (!request.getToUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Not your request");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BadRequestException("Request already handled");
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequestRepository.save(request);

        long a = Math.min(request.getFromUser().getId(), request.getToUser().getId());
        long b = Math.max(request.getFromUser().getId(), request.getToUser().getId());
        friendshipRepository.save(new Friendship(a, b));

        messagingTemplate.send(
                "/queue/user." + request.getFromUser().getId(),
                Map.of("type", "FRIEND_ACCEPTED",
                       "requestId", request.getId(),
                       "byUserId", user.getId(),
                       "byUsername", user.getUsername()));
    }

    @Transactional
    public void rejectRequest(User user, Long requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Request not found"));

        if (!request.getToUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Not your request");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new BadRequestException("Request already handled");
        }

        request.setStatus(FriendRequestStatus.REJECTED);
        friendRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<FriendDto> getFriends(User user) {
        return friendshipRepository.findAllByUserId(user.getId()).stream()
                .map(f -> {
                    Long friendId = f.getUserIdA().equals(user.getId()) ? f.getUserIdB() : f.getUserIdA();
                    return userRepository.findById(friendId).map(friend -> {
                        String presence = presenceRepository.findById(friend.getId())
                                .map(p -> p.getState().name())
                                .orElse("OFFLINE");
                        return new FriendDto(friend.getId(), friend.getUsername(), presence);
                    }).orElse(null);
                })
                .filter(d -> d != null)
                .toList();
    }

    @Transactional
    public void removeFriend(User user, Long friendId) {
        if (!friendshipRepository.existsBetween(user.getId(), friendId)) {
            throw new BadRequestException("Not friends");
        }
        friendshipRepository.deleteBetween(user.getId(), friendId);
    }

    @Transactional
    public void banUser(User banner, Long bannedId) {
        userRepository.findById(bannedId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (banner.getId().equals(bannedId)) {
            throw new BadRequestException("Cannot ban yourself");
        }

        // terminate friendship if exists
        if (friendshipRepository.existsBetween(banner.getId(), bannedId)) {
            friendshipRepository.deleteBetween(banner.getId(), bannedId);
        }

        userBanRepository.save(new UserBan(banner.getId(), bannedId));
    }

    @Transactional
    public void unbanUser(User banner, Long bannedId) {
        userBanRepository.deleteByBannerIdAndBannedId(banner.getId(), bannedId);
    }

    @Transactional(readOnly = true)
    public List<BannedContactDto> getBannedUsers(User banner) {
        List<Long> bannedIds = userBanRepository.findByBannerId(banner.getId())
                .stream().map(UserBan::getBannedId).toList();
        if (bannedIds.isEmpty()) return List.of();
        return userRepository.findAllById(bannedIds).stream()
                .map(u -> new BannedContactDto(u.getId(), u.getUsername()))
                .toList();
    }
}
