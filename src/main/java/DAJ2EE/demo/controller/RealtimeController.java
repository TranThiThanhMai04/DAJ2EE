package DAJ2EE.demo.controller;

import DAJ2EE.demo.entity.User;
import DAJ2EE.demo.service.CurrentUserService;
import DAJ2EE.demo.service.NotificationRealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@RequiredArgsConstructor
public class RealtimeController {

    private final CurrentUserService currentUserService;
    private final NotificationRealtimeService notificationRealtimeService;

    @GetMapping(value = "/tenant/realtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('TENANT')")
    public SseEmitter tenantStream(Authentication authentication) {
        User user = currentUserService.getRequiredUser(authentication);
        return notificationRealtimeService.subscribe(user.getId());
    }

    @GetMapping(value = "/admin/realtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public SseEmitter adminStream(Authentication authentication) {
        User user = currentUserService.getRequiredUser(authentication);
        return notificationRealtimeService.subscribeAdmin(user.getId());
    }
}
