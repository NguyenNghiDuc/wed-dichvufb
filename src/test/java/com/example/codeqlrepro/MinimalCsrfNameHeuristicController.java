package com.example.codeqlrepro;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Test-only fixture for issue #13.
 *
 * This controller is deliberately outside com.example.fbservice, so it is not
 * component-scanned by the application. It is compiled from src/test/java by
 * the existing Maven build and is never included in the production JAR.
 *
 * The GET handlers have no side effects. Their names intentionally contain
 * words used by CodeQL's CSRF name heuristic so we can isolate the
 * zero-length source/sink path candidate without weakening application CSRF.
 */
@Controller
final class MinimalCsrfNameHeuristicController {

    @GetMapping("/__codeql_repro__/login")
    public String reproLogin() {
        return "login";
    }

    @GetMapping("/__codeql_repro__/register")
    public String reproRegister() {
        return "register";
    }

    @GetMapping("/__codeql_repro__/edit")
    public String reproEdit() {
        return "edit";
    }

    /** Negative control: a read-only method whose name begins with "view". */
    @GetMapping("/__codeql_repro__/view-login")
    public String viewLogin() {
        return "login";
    }

    /** Negative control: POST is not a CsrfUnprotectedMethod in this query. */
    @PostMapping("/__codeql_repro__/submit")
    public String reproSubmit() {
        return "submitted";
    }
}
