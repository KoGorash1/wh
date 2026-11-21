package com.wh.controller;

import com.wh.service.AdminOrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminOrdersController {

    private final AdminOrderService adminOrderService;

    public AdminOrdersController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    private boolean isNotStaff(HttpSession session) {
        return session == null
                || !Boolean.TRUE.equals(session.getAttribute("isStaff"));
    }

    @GetMapping("/admin/orders")
    public String ordersPage(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) String status,
            Model model,
            HttpSession session) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        String cleanQ = q == null ? "" : q.trim();
        String cleanStatus = status == null ? "" : status.trim();

        model.addAttribute("page", adminOrderService.load(cleanQ, cleanStatus, id));
        model.addAttribute("selectedId", id);
        model.addAttribute("q", cleanQ);
        model.addAttribute("statusFilter", cleanStatus);
        model.addAttribute("isAdmin",
                Boolean.TRUE.equals(session.getAttribute("isAdmin")));
        model.addAttribute("activeSection", "orders");

        return "admin-orders";
    }

    @PostMapping("/admin/orders/{id}/status")
    public String updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "filter", required = false) String filter,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (isNotStaff(session)) {
            return "redirect:/login";
        }

        adminOrderService.updateStatus(id, status);

        if (q != null && !q.isBlank()) {
            redirectAttributes.addAttribute("q", q.trim());
        }

        if (filter != null && !filter.isBlank()) {
            redirectAttributes.addAttribute("status", filter.trim());
        }

        if (id != null) {
            redirectAttributes.addAttribute("id", id);
        }

        return "redirect:/admin/orders";
    }
}