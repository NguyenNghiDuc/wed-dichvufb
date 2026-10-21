package com.example.fbservice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import static org.assertj.core.api.Assertions.*;

/**
 * Deployment regression checks that run with the EXISTING Java CI.
 * Does not need bank credentials, Render access, or production funds.
 */
class RenderStagingDeploymentTest {
    @SuppressWarnings("unchecked")
    @Test void renderBlueprintMustDefaultToNoCustomerPayments() throws Exception {
        Path blueprint = Path.of("render.yaml");
        assertThat(blueprint).exists();
        Map<String,Object> config = new Yaml().load(Files.readString(blueprint));
        List<Map<String,Object>> services = (List<Map<String,Object>>) config.get("services");
        List<Map<String,Object>> databases = (List<Map<String,Object>>) config.get("databases");
        assertThat(services).hasSize(1);
        assertThat(databases).hasSize(1);

        Map<String,Object> web=services.get(0), db=databases.get(0);
        assertThat(web.get("runtime")).isEqualTo("docker");
        assertThat(web.get("type")).isEqualTo("web");
        assertThat(web.get("region")).isEqualTo("singapore");
        assertThat(web.get("healthCheckPath")).isEqualTo("/actuator/health");
        assertThat(web.get("autoDeployTrigger")).isEqualTo("off");
        assertThat(web.get("branch")).isEqualTo("feature/techcombank-wallet-settlement-20261021");
        assertThat(db.get("ipAllowList")).isEqualTo(List.of());
        assertThat(db.get("region")).isEqualTo("singapore");

        List<Map<String,Object>> env=(List<Map<String,Object>>) web.get("envVars");
        assertThat(env).extracting(e->e.get("key"))
            .contains("RENDER_DATABASE_URL","DB_USERNAME","DB_PASSWORD");
        for(String key: List.of("GROWTH_LIVE_PAYMENTS_READY",
                "GROWTH_CASSO_ENABLED","GROWTH_INTERNAL_VERIFIED_ENABLED")) {
            assertThat(valueFor(env,key).get("value")).as(key+" must be disabled").isEqualTo("false");
        }
        Map<String,Object> connectionRef=(Map<String,Object>) valueFor(env,"RENDER_DATABASE_URL").get("fromDatabase");
        assertThat(connectionRef.get("name")).isEqualTo(db.get("name"));
        assertThat(connectionRef.get("property")).isEqualTo("connectionString");
        assertThat(valueFor(env,"DB_USERNAME").get("fromDatabase")).isNotNull();
        assertThat(valueFor(env,"DB_PASSWORD").get("fromDatabase")).isNotNull();
        assertThat(web.toString()).doesNotContain("GROWTH_CASSO_WEBHOOK_SECRET","GROWTH_BANK_ACCOUNT_NUMBER");
    }

    private Map<String,Object> valueFor(List<Map<String,Object>> vars,String key) {
        return vars.stream().filter(v->key.equals(v.get("key"))).findFirst()
            .orElseThrow(()->new AssertionError("Missing variable "+key));
    }

    @Test void deploymentShellHandlesRenderAndDockerComposeWithoutJavaProcess() throws Exception {
        Assumptions.assumeTrue(!System.getProperty("os.name").toLowerCase().contains("windows"));
        Process process = new ProcessBuilder("sh","scripts/test_deployment_entrypoint.sh")
            .redirectErrorStream(true).start();
        try {
            boolean finished=process.waitFor(12,TimeUnit.SECONDS);
            if(!finished) {
                process.destroyForcibly();
                fail("Deployment entrypoint smoke test timed out");
            }
            String output=new String(process.getInputStream().readAllBytes());
            assertThat(process.exitValue()).as(output).isZero();
            assertThat(output).contains("deployment-entrypoint checks OK");
        } finally {
            if(process.isAlive())process.destroyForcibly();
        }
    }
}
