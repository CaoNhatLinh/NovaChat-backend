package com.chatapp.chat_service.conversation.controller;

import com.chatapp.chat_service.conversation.dto.AddMemberRequest;
import com.chatapp.chat_service.conversation.dto.ConversationMemberDto;
import com.chatapp.chat_service.conversation.dto.CreateInvitationLinkRequest;
import com.chatapp.chat_service.conversation.dto.GrantAdminRequest;
import com.chatapp.chat_service.conversation.dto.InvitationLinkDto;
import com.chatapp.chat_service.conversation.dto.TransferOwnershipRequest;
import com.chatapp.chat_service.conversation.service.ConversationMemberService;
import com.chatapp.chat_service.conversation.service.InvitationLinkService;
import com.chatapp.chat_service.security.core.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller để quản lý conversation (group/channel)
 * Bao gồm: quản lý members, quyền admin, invitation links, cập nhật thông tin conversation
 */
@RestController
@RequestMapping("/api/conversations/{conversationId}/management")
@RequiredArgsConstructor
public class ConversationManagementController {
    
    private final ConversationMemberService memberService;
    private final InvitationLinkService invitationLinkService;
    
    // ============= QUẢN LÝ MEMBERS =============
    
    /**
     * Lấy danh sách members của conversation
     * GET /api/conversations/{conversationId}/management/members
     */
    @GetMapping("/members")
    public ResponseEntity<List<ConversationMemberDto>> getMembers(
            @PathVariable UUID conversationId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        // Kiểm tra user có phải member không
        if (!memberService.isMemberOfConversation(conversationId, userId)) {
            return ResponseEntity.status(403).build();
        }
        
        List<ConversationMemberDto> members = memberService.getConversationMembers(conversationId);
        return ResponseEntity.ok(members);
    }
    
    /**
     * Thêm members vào conversation (chỉ owner/admin)
     * POST /api/conversations/{conversationId}/management/members/add
     */
    @PostMapping("/members/add")
    public ResponseEntity<String> addMembers(
            @PathVariable UUID conversationId,
            @RequestBody AddMemberRequest request,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        memberService.addMembers(conversationId, request.getMemberIds(), userId);
        return ResponseEntity.ok("Đã thêm thành viên thành công");
    }
    
    /**
     * Kick member khỏi conversation (chỉ owner/admin)
     * DELETE /api/conversations/{conversationId}/management/members/{memberId}
     */
    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<String> removeMember(
            @PathVariable UUID conversationId,
            @PathVariable UUID memberId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        memberService.removeMember(conversationId, memberId, userId);
        return ResponseEntity.ok("Đã kick thành viên thành công");
    }
    
    /**
     * Rời khỏi conversation (owner không được rời)
     * POST /api/conversations/{conversationId}/management/leave
     */
    @PostMapping("/leave")
    public ResponseEntity<String> leaveConversation(
            @PathVariable UUID conversationId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        memberService.leaveConversation(conversationId, userId);
        return ResponseEntity.ok("Đã rời khỏi phòng thành công");
    }
    
    // ============= QUẢN LÝ QUYỀN =============
    
    /**
     * Chuyển quyền owner (chỉ owner hiện tại)
     * POST /api/conversations/{conversationId}/management/transfer-ownership
     */
    @PostMapping("/transfer-ownership")
    public ResponseEntity<String> transferOwnership(
            @PathVariable UUID conversationId,
            @RequestBody TransferOwnershipRequest request,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        memberService.transferOwnership(conversationId, request.getNewOwnerId(), userId);
        return ResponseEntity.ok("Đã chuyển quyền chủ phòng thành công");
    }
    
    /**
     * Trao quyền admin (chỉ owner)
     * POST /api/conversations/{conversationId}/management/grant-admin
     */
    @PostMapping("/grant-admin")
    public ResponseEntity<String> grantAdmin(
            @PathVariable UUID conversationId,
            @RequestBody GrantAdminRequest request,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        memberService.grantAdmin(conversationId, request.getUserId(), userId);
        return ResponseEntity.ok("Đã trao quyền admin thành công");
    }
    
    /**
     * Thu hồi quyền admin (chỉ owner)
     * POST /api/conversations/{conversationId}/management/revoke-admin
     */
    @PostMapping("/revoke-admin")
    public ResponseEntity<String> revokeAdmin(
            @PathVariable UUID conversationId,
            @RequestBody GrantAdminRequest request,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        memberService.revokeAdmin(conversationId, request.getUserId(), userId);
        return ResponseEntity.ok("Đã thu hồi quyền admin thành công");
    }
    
    // ============= QUẢN LÝ INVITATION LINKS =============
    
    /**
     * Tạo invitation link (chỉ owner/admin)
     * POST /api/conversations/{conversationId}/management/invitations
     */
    @PostMapping("/invitations")
    public ResponseEntity<InvitationLinkDto> createInvitationLink(
            @PathVariable UUID conversationId,
            @RequestBody CreateInvitationLinkRequest request,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        InvitationLinkDto link = invitationLinkService.createInvitationLink(
                conversationId, 
                userId, 
                request.getExpiresInHours(), 
                request.getMaxUses()
        );
        return ResponseEntity.ok(link);
    }
    
    /**
     * Lấy danh sách invitation links của conversation
     * GET /api/conversations/{conversationId}/management/invitations
     */
    @GetMapping("/invitations")
    public ResponseEntity<List<InvitationLinkDto>> getInvitationLinks(
            @PathVariable UUID conversationId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        List<InvitationLinkDto> links = invitationLinkService.getConversationLinks(conversationId, userId);
        return ResponseEntity.ok(links);
    }
    
    /**
     * Xóa invitation link (owner/admin/người tạo)
     * DELETE /api/conversations/{conversationId}/management/invitations/{linkId}
     */
    @DeleteMapping("/invitations/{linkId}")
    public ResponseEntity<String> deleteInvitationLink(
            @PathVariable UUID conversationId,
            @PathVariable UUID linkId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        invitationLinkService.deleteInvitationLink(linkId, userId);
        return ResponseEntity.ok("Đã xóa link mời thành công");
    }
    
    /**
     * Vô hiệu hóa invitation link (owner/admin/người tạo)
     * PUT /api/conversations/{conversationId}/management/invitations/{linkId}/deactivate
     */
    @PutMapping("/invitations/{linkId}/deactivate")
    public ResponseEntity<String> deactivateInvitationLink(
            @PathVariable UUID conversationId,
            @PathVariable UUID linkId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        invitationLinkService.deactivateLink(linkId, userId);
        return ResponseEntity.ok("Đã vô hiệu hóa link mời thành công");
    }
    
    /**
     * Join conversation qua invitation link (public endpoint - không cần auth ở đây)
     * POST /api/invitations/join/{linkToken}
     * Note: API này nên được đặt ở root level hoặc public controller
     */
    @PostMapping("/invitations/join/{linkToken}")
    public ResponseEntity<String> joinViaInvitation(
            @PathVariable String linkToken,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getUserId();
        
        invitationLinkService.joinViaInvitationLink(linkToken, userId);
        return ResponseEntity.ok("Đã tham gia phòng thành công");
    }
}
