package com.project.ieum.controller.chat;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/chat")
public class ChatPageController {

    @GetMapping("/conversations")
    public String conversations() {
        return "chat/conversations";
    }

    @GetMapping("/conversations/{conversationId}")
    public String room(@PathVariable Long conversationId, Model model) {
        model.addAttribute("conversationId", conversationId);
        return "chat/room";
    }
}
