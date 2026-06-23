package com.project.ieum.dto.admin;

import com.project.ieum.entity.inquiry.Inquiry;
import com.project.ieum.entity.User;
import com.project.ieum.entity.market.MarketPost;
import com.project.ieum.entity.request.HelpRequest;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AdminDashboardStats {
    private long totalUsers;
    private long totalCaregivers;
    private long newMembersThisWeek;

    private long activeMatchings;
    private long pendingApplications;
    private long unansweredInquiries;

    private long activeMarketPosts;
    private long soldMarketPosts;

    private Map<String, Long> helpRequestByStatus;
    private long helpRequestTotal;
    private Map<String, Long> userByStatus;
    private long userTotal;
    private Map<String, Long> marketPostByStatus;
    private long marketPostTotal;

    private List<Inquiry> recentInquiries;
    private List<User> recentUsers;
    private List<HelpRequest> recentCompletedMatchings;
    private List<MarketPost> recentMarketPosts;
}
