package burp.intruder;

import burp.api.montoya.MontoyaExtension;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.FakeByteArray;
import burp.api.montoya.internal.MontoyaObjectFactory;
import burp.api.montoya.internal.ObjectFactoryLocator;
import burp.api.montoya.intruder.FakePayloadProcessingResult;
import burp.api.montoya.intruder.PayloadData;
import burp.api.montoya.intruder.PayloadProcessingResult;
import burp.api.montoya.logging.Logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;

import com.blackberry.jwteditor.model.keys.KeysModel;

import static burp.api.montoya.intruder.FakePayloadData.payloadData;
import static burp.api.montoya.intruder.PayloadProcessingAction.USE_PAYLOAD;
import static burp.intruder.FuzzLocation.HEADER;
import static burp.intruder.FuzzLocation.PAYLOAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

@ExtendWith(MontoyaExtension.class)
class JWTPayloadProcessorTest {
    @BeforeEach
    void configureMocks() {
        MontoyaObjectFactory factory = ObjectFactoryLocator.FACTORY;

        when(factory.usePayload(any(ByteArray.class))).thenAnswer((Answer<PayloadProcessingResult>) i ->
                new FakePayloadProcessingResult(i.getArgument(0, ByteArray.class)));

        when(factory.byteArray(anyString())).thenAnswer((Answer<ByteArray>) i ->
                new FakeByteArray(i.getArgument(0, String.class)));
    }

    @Test
    void givenBaseValueNotJWS_whenPayloadProcessed_thenPayloadLeftUnchanged() {
        String baseValue = "isogeny";
        PayloadData payloadData = payloadData().withBaseValue(baseValue).build();
        Optional<Logging> emptyLogging = Optional.empty();
        Optional<KeysModel> emptyKeysModel = Optional.empty();
        JWSPayloadProcessor processor = new JWSPayloadProcessor(intruderConfig("role", PAYLOAD), emptyLogging, emptyKeysModel);

        PayloadProcessingResult result = processor.processPayload(payloadData);

        assertThat(result.action()).isEqualTo(USE_PAYLOAD);
        assertThat(result.processedPayload().toString()).isEqualTo(baseValue);
    }

    @Test
    void givenBaseValueJWSAndFuzzParameterNotPresent_whenPayloadProcessed_thenPayloadLeftUnchanged() {
        String baseValue = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        PayloadData payloadData = payloadData().withBaseValue(baseValue).build();
        Optional<Logging> emptyLogging = Optional.empty();
        Optional<KeysModel> emptyKeysModel = Optional.empty();
        JWSPayloadProcessor processor = new JWSPayloadProcessor(intruderConfig("role", PAYLOAD), emptyLogging, emptyKeysModel);

        PayloadProcessingResult result = processor.processPayload(payloadData);

        assertThat(result.action()).isEqualTo(USE_PAYLOAD);
        assertThat(result.processedPayload().toString()).isEqualTo(baseValue);
    }

    @Test
    void givenBaseValueJWSAndFuzzParameterPresentInWrongContext_whenPayloadProcessed_thenPayloadLeftUnchanged() {
        String baseValue = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        PayloadData payloadData = payloadData().withBaseValue(baseValue).build();
        Optional<Logging> emptyLogging = Optional.empty();
        Optional<KeysModel> emptyKeysModel = Optional.empty();
        JWSPayloadProcessor processor = new JWSPayloadProcessor(intruderConfig("alg", PAYLOAD), emptyLogging, emptyKeysModel);

        PayloadProcessingResult result = processor.processPayload(payloadData);

        assertThat(result.action()).isEqualTo(USE_PAYLOAD);
        assertThat(result.processedPayload().toString()).isEqualTo(baseValue);
    }

    @Test
    void givenBaseValueJWSAndFuzzParameterPresentInHeader_whenPayloadProcessed_thenPayloadModified() {
        String baseValue = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        PayloadData payloadData = payloadData().withBaseValue(baseValue).withCurrentPayload("RS256").build();
        Optional<Logging> emptyLogging = Optional.empty();
        Optional<KeysModel> emptyKeysModel = Optional.empty();
        JWSPayloadProcessor processor = new JWSPayloadProcessor(intruderConfig("alg", HEADER), emptyLogging, emptyKeysModel);

        PayloadProcessingResult result = processor.processPayload(payloadData);

        assertThat(result.action()).isEqualTo(USE_PAYLOAD);
        assertThat(result.processedPayload().toString()).isEqualTo("eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
    }

    @Test
    void givenBaseValueJWSAndFuzzParameterPresentInPayload_whenPayloadProcessed_thenPayloadModified() {
        String baseValue = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        PayloadData payloadData = payloadData().withBaseValue(baseValue).withCurrentPayload("emanon").build();
        Optional<Logging> emptyLogging = Optional.empty();
        Optional<KeysModel> emptyKeysModel = Optional.empty();
        JWSPayloadProcessor processor = new JWSPayloadProcessor(intruderConfig("name", PAYLOAD), emptyLogging, emptyKeysModel);

        PayloadProcessingResult result = processor.processPayload(payloadData);

        assertThat(result.action()).isEqualTo(USE_PAYLOAD);
        assertThat(result.processedPayload().toString()).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6ImVtYW5vbiIsImlhdCI6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
    }

    private static IntruderConfig intruderConfig(String parameterName, FuzzLocation parameterLocation) {
        IntruderConfig intruderConfig = new IntruderConfig();
        intruderConfig.setFuzzParameter(parameterName);
        intruderConfig.setFuzzLocation(parameterLocation);

        return intruderConfig;
    }
}