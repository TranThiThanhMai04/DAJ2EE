package DAJ2EE.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class NotificationRealtimeService {

    private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByAdmin = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = registerEmitter(emittersByUser, userId);

        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("status", "ok", "scope", "tenant")));
        } catch (IOException ex) {
            removeEmitter(emittersByUser, userId, emitter);
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    public SseEmitter subscribeAdmin(Long adminUserId) {
        SseEmitter emitter = registerEmitter(emittersByAdmin, adminUserId);

        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("status", "ok", "scope", "admin")));
        } catch (IOException ex) {
            removeEmitter(emittersByAdmin, adminUserId, emitter);
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    public void publishPortalSyncToUser(Long userId, Map<String, Object> payload) {
        sendEvent(emittersByUser, userId, "portal-sync", payload);
    }

    public void publishPortalSyncToAdmins(Map<String, Object> payload) {
        sendEventToAll(emittersByAdmin, "portal-sync", payload);
    }

    public void publishPortalSyncToUserAndAdmins(Long userId, Map<String, Object> payload) {
        publishPortalSyncToUser(userId, payload);
        publishPortalSyncToAdmins(payload);
    }

    public void publishNotification(Long userId, Map<String, Object> payload) {
        sendEvent(emittersByUser, userId, "notification", payload);
    }

    public void publishUnreadCount(Long userId, long unreadCount) {
        sendEvent(emittersByUser, userId, "unread-count", Map.of("unreadCount", unreadCount));
    }

    private SseEmitter registerEmitter(Map<Long, CopyOnWriteArrayList<SseEmitter>> emitterMap, Long key) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitterMap.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(emitterMap, key, emitter));
        emitter.onTimeout(() -> removeEmitter(emitterMap, key, emitter));
        emitter.onError(ex -> removeEmitter(emitterMap, key, emitter));

        return emitter;
    }

    private void sendEvent(Map<Long, CopyOnWriteArrayList<SseEmitter>> emitterMap,
                           Long userId,
                           String eventName,
                           Object payload) {
        List<SseEmitter> emitters = emitterMap.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException ex) {
                log.debug("Drop stale SSE emitter for id={}", userId);
                removeEmitter(emitterMap, userId, emitter);
                emitter.completeWithError(ex);
            }
        }
    }

    private void sendEventToAll(Map<Long, CopyOnWriteArrayList<SseEmitter>> emitterMap,
                                String eventName,
                                Object payload) {
        for (Long id : emitterMap.keySet()) {
            sendEvent(emitterMap, id, eventName, payload);
        }
    }

    private void removeEmitter(Map<Long, CopyOnWriteArrayList<SseEmitter>> emitterMap,
                               Long userId,
                               SseEmitter emitter) {
        List<SseEmitter> emitters = emitterMap.get(userId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emitterMap.remove(userId);
        }
    }
}