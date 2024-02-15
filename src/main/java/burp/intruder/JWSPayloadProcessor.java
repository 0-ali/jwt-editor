package burp.intruder;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.intruder.PayloadData;
import burp.api.montoya.intruder.PayloadProcessingResult;
import burp.api.montoya.intruder.PayloadProcessor;
import burp.api.montoya.logging.Logging;
import com.blackberry.jwteditor.exceptions.SigningException;
import com.blackberry.jwteditor.model.jose.JOSEObject;
import com.blackberry.jwteditor.model.jose.JWS;
import com.blackberry.jwteditor.model.jose.JWSFactory;
import com.blackberry.jwteditor.model.keys.Key;
import com.blackberry.jwteditor.model.keys.KeysModel;
import com.nimbusds.jose.util.Base64URL;
import org.json.JSONObject;

import java.util.Optional;

import static burp.intruder.FuzzLocation.PAYLOAD;
import static com.blackberry.jwteditor.model.jose.JOSEObjectFinder.parseJOSEObject;

public class JWSPayloadProcessor implements PayloadProcessor {
    private final Logging logging;
    private final IntruderConfig intruderConfig;
    private final KeysModel keysModel;

    public JWSPayloadProcessor(IntruderConfig intruderConfig, Logging logging, KeysModel keysModel) {
        this.logging = logging;
        this.intruderConfig = intruderConfig;
        this.keysModel = keysModel;
    }

    @Override
    public PayloadProcessingResult processPayload(PayloadData payloadData) {
        ByteArray baseValue = payloadData.insertionPoint().baseValue();
        Optional<JOSEObject> joseObject = parseJOSEObject(baseValue.toString());

        if (joseObject.isPresent() && (joseObject.get() instanceof JWS jws)) {
            boolean fuzzPayload = intruderConfig.fuzzLocation() == PAYLOAD;
            String targetData = fuzzPayload ? jws.getPayload() : jws.getHeader();
            JSONObject targetJson = new JSONObject(targetData);

            if (targetJson.has(intruderConfig.fuzzParameter())) {
                targetJson.put(intruderConfig.fuzzParameter(), payloadData.currentPayload().toString());

                Base64URL updatedHeader = fuzzPayload
                        ? jws.getEncodedHeader()
                        : Base64URL.encode(targetJson.toString());

                Base64URL updatedPayload = fuzzPayload
                        ? Base64URL.encode(targetJson.toString())
                        : jws.getEncodedPayload();

                JWS updatedJws = createJWS(updatedHeader, updatedPayload, jws.getEncodedSignature());
                baseValue = ByteArray.byteArray(updatedJws.serialize());
            }
        }

        return PayloadProcessingResult.usePayload(baseValue);
    }

    private Optional<Key> loadKey() {
        if (!intruderConfig.resign()) {
            return Optional.empty();
        }

        Key key = keysModel.getKey(intruderConfig.signingKeyId());

        if (key == null) {
            logging.logToError("Key with ID " + intruderConfig.signingKeyId() + " not found.");
        }

        return Optional.ofNullable(key);
    }

    @Override
    public String displayName() {
        return "JWS payload processor";
    }

    // Creates a JWS object from the given attributes. Signs the JWS if possible (i.e., available key selected in Intruder settings)
    private JWS createJWS(Base64URL header, Base64URL payload, Base64URL originalSignature) {
        return this.loadKey().flatMap(key -> {
            Optional<JWS> result = Optional.empty();

            try {
                // TODO - update alg within header
                result = Optional.of(JWSFactory.sign(key, intruderConfig.signingAlgorithm(), header, payload));
            } catch (SigningException ex) {
                logging.logToError("Failed to sign JWS: " + ex);
            }

            return result;
        }).orElseGet(() -> JWSFactory.jwsFromParts(header, payload, originalSignature));
    }
}
