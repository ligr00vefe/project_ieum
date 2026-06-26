package com.project.ieum.controller;

import com.project.ieum.entity.conversation.Conversation;
import com.project.ieum.exception.NotFoundException;
import com.project.ieum.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final ConversationRepository conversationRepository;

    private static final String[] DAY_KO = {"일", "월", "화", "수", "목", "금", "토"};
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @GetMapping({"", "/"})
    public String index(Authentication auth) {
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAREGIVER"))) {
            return "redirect:/caregiver/board";
        }
        return "redirect:/disabled/board";
    }

    @GetMapping("/confirmed")
    public String confirmed(@RequestParam(required = false) Long conversationId,
                            Authentication auth, Model model) {
        model.addAttribute("title", "매칭 확정");

        if (conversationId == null || auth == null) {
            model.addAttribute("content", "matching/confirmed");
            return "layout/layout";
        }

        Conversation conv = conversationRepository.findWithParticipantsById(conversationId)
                .orElseThrow(() -> new NotFoundException("대화방을 찾을 수 없습니다."));

        boolean isUser = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        boolean isCaregiver = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CAREGIVER"));

        var req = conv.getApplication().getHelpRequest();

        // 파트너 정보
        String partnerName;
        String partnerInitial;
        String partnerSubtitle;
        if (isUser) {
            var cg = conv.getCaregiver();
            partnerName = cg.getFullName() + " 활동지원사";
            partnerInitial = cg.getFullName().substring(0, 1);
            String cert = cg.getCertificationType() != null ? cg.getCertificationType() : "";
            String exp  = cg.getExperience() != null ? "경력 " + cg.getExperience() : "";
            partnerSubtitle = (cert + (cert.isEmpty() || exp.isEmpty() ? "" : " · ") + exp).trim();
        } else {
            var rq = conv.getRequester();
            partnerName = rq.getFullName() + " 이용자";
            partnerInitial = rq.getFullName().substring(0, 1);
            partnerSubtitle = "이용자";
        }

        // 날짜·시간 계산
        LocalDateTime start = req.getDesiredStartDatetime();
        LocalDateTime end   = req.getDesiredEndDatetime();
        boolean sameDay = end == null || start.toLocalDate().isEqual(end.toLocalDate());

        String dateDisplay;
        String timeDisplay;

        if (sameDay) {
            String day = DAY_KO[start.getDayOfWeek().getValue() % 7];
            dateDisplay = start.format(DATE_FMT) + " (" + day + ")";
            if (end != null) {
                long mins = java.time.Duration.between(start, end).toMinutes();
                String duration = mins > 0
                        ? " (" + (mins % 60 == 0 ? mins / 60 + "시간" : mins / 60 + "시간 " + mins % 60 + "분") + ")"
                        : "";
                timeDisplay = start.format(TIME_FMT) + " ~ " + end.format(TIME_FMT) + duration;
            } else {
                timeDisplay = start.format(TIME_FMT) + " ~";
            }
        } else {
            String startDay = DAY_KO[start.getDayOfWeek().getValue() % 7];
            String endDay   = DAY_KO[end.getDayOfWeek().getValue() % 7];
            dateDisplay = start.format(DATE_FMT) + " (" + startDay + ") ~ " + end.format(DATE_FMT) + " (" + endDay + ")";
            timeDisplay = "-";
        }

        String location = req.getRoadAddress() != null ? req.getRoadAddress() : "";
        if (req.getAddressDetail() != null && !req.getAddressDetail().isBlank()) {
            location += " " + req.getAddressDetail();
        }

        String userName      = conv.getRequester().getFullName() + " 이용자님";
        String caregiverName = conv.getCaregiver().getFullName() + " 활동지원사님";

        model.addAttribute("isUser", isUser);
        model.addAttribute("isCaregiver", isCaregiver);
        model.addAttribute("partnerName", partnerName);
        model.addAttribute("partnerInitial", partnerInitial.isEmpty() ? "?" : partnerInitial);
        model.addAttribute("partnerSubtitle", partnerSubtitle.isEmpty() ? null : partnerSubtitle);
        model.addAttribute("dateDisplay", dateDisplay);
        model.addAttribute("timeDisplay", timeDisplay);
        model.addAttribute("location", location.isBlank() ? null : location.trim());
        model.addAttribute("requestTitle", req.getTitle());
        model.addAttribute("conversationId", conversationId);
        model.addAttribute("userName", userName);
        model.addAttribute("caregiverName", caregiverName);
        model.addAttribute("content", "matching/confirmed");
        return "layout/layout";
    }
}
