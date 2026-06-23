package com.project.ieum.service.admin;

import com.project.ieum.dto.admin.AdminDashboardStats;
import com.project.ieum.entity.ApplicationStatus;
import com.project.ieum.entity.UserRole;
import com.project.ieum.entity.UserStatus;
import com.project.ieum.entity.inquiry.InquiryStatus;
import com.project.ieum.entity.market.MarketPostStatus;
import com.project.ieum.entity.request.HelpRequestStatus;
import com.project.ieum.repository.*;
import com.project.ieum.repository.market.MarketPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final HelpRequestRepository helpRequestRepository;
    private final HelpRequestApplicationRepository applicationRepository;
    private final InquiryRepository inquiryRepository;
    private final MarketPostRepository marketPostRepository;

    public AdminDashboardStats getStats() {
        Map<String, Long> requestByStatus = new LinkedHashMap<>();
        requestByStatus.put("OPEN",        helpRequestRepository.countByStatus(HelpRequestStatus.OPEN));
        requestByStatus.put("MATCHED",     helpRequestRepository.countByStatus(HelpRequestStatus.MATCHED));
        requestByStatus.put("IN_PROGRESS", helpRequestRepository.countByStatus(HelpRequestStatus.IN_PROGRESS));
        requestByStatus.put("COMPLETED",   helpRequestRepository.countByStatus(HelpRequestStatus.COMPLETED));
        requestByStatus.put("CLOSED",      helpRequestRepository.countByStatus(HelpRequestStatus.CLOSED));

        Map<String, Long> userByStatus = new LinkedHashMap<>();
        userByStatus.put("ACTIVE",  userRepository.countByStatus(UserStatus.ACTIVE));
        userByStatus.put("PAUSED",  userRepository.countByStatus(UserStatus.PAUSED));
        userByStatus.put("BANNED",  userRepository.countByStatus(UserStatus.BANNED));
        userByStatus.put("DELETED", userRepository.countByStatus(UserStatus.DELETED));

        Map<String, Long> marketByStatus = new LinkedHashMap<>();
        long activeMarket = marketPostRepository.countByStatus(MarketPostStatus.ACTIVE);
        long reservedMarket = marketPostRepository.countByStatus(MarketPostStatus.RESERVED);
        long soldMarket = marketPostRepository.countByStatus(MarketPostStatus.SOLD);
        long removedMarket = marketPostRepository.countByStatus(MarketPostStatus.REMOVED);
        marketByStatus.put("ACTIVE",   activeMarket);
        marketByStatus.put("RESERVED", reservedMarket);
        marketByStatus.put("SOLD",     soldMarket);
        marketByStatus.put("REMOVED",  removedMarket);

        long requestTotal = requestByStatus.values().stream().mapToLong(Long::longValue).sum();
        long uTotal = userByStatus.values().stream().mapToLong(Long::longValue).sum();
        long marketTotal = activeMarket + reservedMarket + soldMarket + removedMarket;

        return AdminDashboardStats.builder()
                .totalUsers(userRepository.countByRole(UserRole.USER))
                .totalCaregivers(userRepository.countByRole(UserRole.CAREGIVER))
                .newMembersThisWeek(userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(7)))
                .activeMatchings(helpRequestRepository.countByStatus(HelpRequestStatus.IN_PROGRESS))
                .pendingApplications(applicationRepository.countByStatus(ApplicationStatus.PENDING))
                .unansweredInquiries(inquiryRepository.countByStatus(InquiryStatus.PENDING))
                .activeMarketPosts(activeMarket)
                .soldMarketPosts(soldMarket)
                .helpRequestByStatus(requestByStatus)
                .helpRequestTotal(requestTotal == 0 ? 1 : requestTotal)
                .userByStatus(userByStatus)
                .userTotal(uTotal == 0 ? 1 : uTotal)
                .marketPostByStatus(marketByStatus)
                .marketPostTotal(marketTotal == 0 ? 1 : marketTotal)
                .recentInquiries(inquiryRepository.findTop5ByOrderByCreatedAtDesc())
                .recentUsers(userRepository.findTop5ByOrderByCreatedAtDesc())
                .recentCompletedMatchings(helpRequestRepository.findTop5ByStatusOrderByUpdatedAtDesc(HelpRequestStatus.COMPLETED))
                .recentMarketPosts(marketPostRepository.findTop5ByOrderByCreatedAtDesc())
                .build();
    }
}
