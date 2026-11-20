package com.example.codeqlrepro;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Deliberately small, read-only GET handlers whose names match the CodeQL
 * CSRF query's state-change name heuristic. The POST route is a control.
 */
@Controller
public class AuthController {
    @GetMapping("/login")
    @ResponseBody
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    @ResponseBody
    public String registerPage() {
        return "register";
    }

    @GetMapping("/edit")
    @ResponseBody
    public String edit() {
        return "edit";
    }

    /**
     * Diagnostic-only differential control for the zero-edge hypothesis.
     * Boolean.FALSE is a constant false, so this self-call is never executed;
     * it exists only as a syntactic call-graph self-edge for CodeQL analysis.
     */
    @GetMapping("/edit-self-edge")
    @ResponseBody
    public String editSelfEdgeProbe() {
        if (Boolean.FALSE.booleanValue()) {
            editSelfEdgeProbe();
        }
        return "edit-self-edge";
    }

    /** Negative control: the query treats the leading "view" as uninteresting. */
    @GetMapping("/view-login")
    @ResponseBody
    public String viewLogin() {
        return "login";
    }

    /** Unsafe-method control: CSRF remains enabled for this state-changing route. */
    @PostMapping("/register")
    @ResponseBody
    public String registerUser() {
        return "registered";
    }
}
