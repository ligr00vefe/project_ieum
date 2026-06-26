package com.project.ieum.controller;

import com.project.ieum.dto.CalendarEventDto;
import com.project.ieum.entity.ApplicationStatus;
import com.project.ieum.entity.User;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.request.HelpRequest;
import com.project.ieum.entity.request.HelpRequestApplication;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.repository.UserRepository;
import com.project.ieum.service.HelpRequestService;
import com.project.ieum.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final UserRepository userRepository;
    private final HelpRequestService helpRequestService;
    private final MatchingService matchingService;

    @GetMapping("/calendar")
    public String calendarPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        model.addAttribute("userRole", user.getRole().name());
        model.addAttribute("title", "일정 달력");
        model.addAttribute("content", "calendar/page");
        return "layout/layout";
    }

    @GetMapping("/api/calendar/events")
    @ResponseBody
    public List<CalendarEventDto> getEvents(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();

        if (user.getRole() == UserRole.USER) {
            return helpRequestService.getMyRequests().stream()
                    .map(r -> {
                        String sd = r.getDesiredStartDatetime().toLocalDate().toString();
                        String ed = r.getDesiredEndDatetime() != null
                                ? r.getDesiredEndDatetime().toLocalDate().toString() : sd;
                        return CalendarEventDto.builder()
                                .requestId(r.getId())
                                .title(r.getTitle())
                                .startDate(sd).endDate(ed).date(sd)
                                .type(isMatchedRequest(r) ? "matched" : "registered")
                                .build();
                    })
                    .collect(Collectors.toList());
        } else if (user.getRole() == UserRole.CAREGIVER) {
            return matchingService.getMyApplications().stream()
                    .map(a -> {
                        String sd = a.getHelpRequest().getDesiredStartDatetime().toLocalDate().toString();
                        String ed = a.getHelpRequest().getDesiredEndDatetime() != null
                                ? a.getHelpRequest().getDesiredEndDatetime().toLocalDate().toString() : sd;
                        return CalendarEventDto.builder()
                                .requestId(a.getHelpRequest().getId())
                                .title(a.getHelpRequest().getTitle())
                                .startDate(sd).endDate(ed).date(sd)
                                .type(isMatchedApplication(a) ? "matched" : "applied")
                                .build();
                    })
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private boolean isMatchedRequest(HelpRequest r) {
        return r.getStatus() == HelpRequestStatus.MATCHED
                || r.getStatus() == HelpRequestStatus.IN_PROGRESS
                || r.getStatus() == HelpRequestStatus.COMPLETED;
    }

    private boolean isMatchedApplication(HelpRequestApplication a) {
        return a.getStatus() == ApplicationStatus.ACCEPTED
                || a.getStatus() == ApplicationStatus.COMPLETED;
    }
}
