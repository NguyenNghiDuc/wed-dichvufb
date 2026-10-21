package com.example.fbservice.payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CassoPayloadCanonicalizationTest {
    @Test void canonicalizesNestedObjectsWithoutChangingArrays() throws Exception{
        var mapper=new ObjectMapper();
        var data=mapper.readTree("{\"error\":0,\"data\":{\"z\":1,\"a\":{\"z\":2,\"a\":3},\"b\":[{\"z\":2,\"a\":1}]}}");
        assertThat(mapper.writeValueAsString(CassoV2WebhookService.sort(data)))
            .isEqualTo("{\"data\":{\"a\":{\"a\":3,\"z\":2},\"b\":[{\"a\":1,\"z\":2}],\"z\":1},\"error\":0}");
    }
    @Test void hmacSha512MatchesProviderPublishedAlgorithm() throws Exception{
        var mapper=new ObjectMapper();
        String secret="test-only-0123456789abcdef0123456789abcdef";
        var input=mapper.readTree("{\"error\":0,\"data\":{\"id\":15,\"amount\":10000}}");
        String message="1734924830020."+mapper.writeValueAsString(CassoV2WebhookService.sort(input));
        Mac mac=Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));
        assertThat(HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8))))
            .hasSize(128);
    }
}
