package burp.scanner;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.Range;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.audit.insertionpoint.AuditInsertionPoint;
import com.blackberry.jwteditor.model.jose.JWS;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.JSONObjectUtils;

import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static burp.api.montoya.core.Range.range;
import static burp.api.montoya.http.message.requests.HttpRequest.httpRequest;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableMap;

class JWSHeaderInsertionPoint implements AuditInsertionPoint {
    private final HttpRequest baseRequest;
    private final JWS jws;
    private final String headerParameterName;
    private final String encodedJWS;
    private final int startOffset;
    private final byte[] baseRequestPrefix;
    private final byte[] baseRequestPostfix;
    private final Map<String, Object> headerJsonMap;

    JWSHeaderInsertionPoint(HttpRequest baseRequest, JWS jws, String headerParameterName, String encodedJWS) {
        this.baseRequest = baseRequest;
        this.jws = jws;
        this.headerParameterName = headerParameterName;
        this.encodedJWS = encodedJWS;

        startOffset = baseRequest.toString().indexOf(encodedJWS);
        int endOffset = startOffset + encodedJWS.length();

        ByteArray baseRequestBytes = baseRequest.toByteArray();
        baseRequestPrefix = startOffset == 0 ? new byte[0] : baseRequestBytes.subArray(0, startOffset).getBytes();
        baseRequestPostfix = endOffset == baseRequestBytes.length() ? new byte[0] : baseRequestBytes.subArray(endOffset, baseRequestBytes.length()).getBytes();

        try {
            headerJsonMap = unmodifiableMap(JSONObjectUtils.parse(jws.getHeader()));
        } catch (ParseException e) {
            throw new IllegalStateException("Could not parse JWS header!", e);
        }
    }

    @Override
    public String name() {
        return "JWS header '%s'".formatted(headerParameterName);
    }

    @Override
    public String baseValue() {
        return encodedJWS;
    }

    @Override
    public HttpRequest buildHttpRequestWithPayload(ByteArray payload) {
        try {
            byte[] updatedJWSBytes = buildWeaponizedJWS(payload);

            int l = baseRequestPrefix.length + updatedJWSBytes.length + baseRequestPostfix.length;
            byte[] modifiedRequest = new byte[l];
            System.arraycopy(baseRequestPrefix, 0, modifiedRequest, 0, baseRequestPrefix.length);
            System.arraycopy(updatedJWSBytes, 0, modifiedRequest, baseRequestPrefix.length, updatedJWSBytes.length);
            System.arraycopy(baseRequestPostfix, 0, modifiedRequest, baseRequestPrefix.length + updatedJWSBytes.length, baseRequestPostfix.length);

            return httpRequest(baseRequest.httpService(), ByteArray.byteArray(modifiedRequest));
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Range> issueHighlights(ByteArray payload) {
        try {
            return singletonList(range(startOffset, startOffset + buildWeaponizedJWS(payload).length));
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] buildWeaponizedJWS(ByteArray payload) throws ParseException {
        Map<String, Object> updatedHeaderMap = headerJsonMap == null ? new HashMap<>() : new LinkedHashMap<>(headerJsonMap);
        updatedHeaderMap.put(headerParameterName, payload.toString());
        Base64URL headerBase64 = JWSHeader.parse(updatedHeaderMap).toBase64URL();

        return "%s.%s.%s".formatted(headerBase64, jws.getEncodedPayload(), jws.getEncodedSignature()).getBytes(UTF_8);
    }
}
