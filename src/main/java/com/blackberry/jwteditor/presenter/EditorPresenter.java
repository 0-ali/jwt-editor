/*
Author : Fraser Winterborn

Copyright 2021 BlackBerry Limited

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.blackberry.jwteditor.presenter;

import com.blackberry.jwteditor.model.jose.JOSEObject;
import com.blackberry.jwteditor.model.jose.JWE;
import com.blackberry.jwteditor.model.jose.JWS;
import com.blackberry.jwteditor.model.jose.MutableJOSEObject;
import com.blackberry.jwteditor.model.keys.Key;
import com.blackberry.jwteditor.model.keys.KeyRing;
import com.blackberry.jwteditor.utils.JSONUtils;
import com.blackberry.jwteditor.utils.Utils;
import com.blackberry.jwteditor.view.EditorView;
import com.blackberry.jwteditor.view.dialog.operations.*;
import com.nimbusds.jose.util.Base64URL;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.blackberry.jwteditor.model.jose.JOSEObjectFinder.containsJOSEObjects;
import static com.blackberry.jwteditor.model.jose.JOSEObjectFinder.extractJOSEObjects;
import static com.blackberry.jwteditor.model.jose.JWEFactory.jweFromParts;
import static com.blackberry.jwteditor.model.jose.JWSFactory.jwsFromParts;
import static javax.swing.JOptionPane.WARNING_MESSAGE;

/**
 * Presenter class for the Editor tab
 */
public class EditorPresenter extends Presenter {

    private final PresenterStore presenters;
    private final EditorView view;

    private final List<MutableJOSEObject> mutableJoseObjects;

    private String message;
    private boolean selectionChanging;

    /**
     * Construct a new editor presenter from a view
     *
     * @param view the view to associate to this presenter
     * @param presenters the shared presenter store
     */
    public EditorPresenter(EditorView view, PresenterStore presenters) {
        this.view = view;
        this.presenters = presenters;
        presenters.register(this);

        mutableJoseObjects = new ArrayList<>();
    }

    /**
     * Determine if the tab should be enabled based on whether a block of text contains JWE/JWSs
     *
     * @param content text that may contain a serialized JWE/JWS
     * @return true if the content contains a JWE/JWS that can be edited
     */
    public boolean isEnabled(String content){
        return containsJOSEObjects(content);
    }

    /**
     * Set the content of the editor tab by extracting and parsing JWE/JWSs from a block of text
     *
     * @param content text that may contain a serialized JWE/JWS
     */
    public void setMessage(String content){

        // Save the input text and clear existing JOSE objects
        message = content;
        mutableJoseObjects.clear();

        // Extract JOSE Objects from the text, build a change set and add them to the dropdown
        List<MutableJOSEObject> joseObjects = extractJOSEObjects(content);
        String[] joseObjectStrings = new String[joseObjects.size()];
        for(int i = 0; i < joseObjects.size(); i++){
            mutableJoseObjects.add(joseObjects.get(i));

            // Truncate the JOSE object for display
            String serializedJWT = joseObjects.get(i).getOriginal();
            if (serializedJWT.length() > EditorView.MAX_JOSE_OBJECT_STRING_LENGTH){
                serializedJWT = String.format("%d - %s ...", i + 1, serializedJWT.substring(0, EditorView.MAX_JOSE_OBJECT_STRING_LENGTH)); //NON-NLS
            }
            joseObjectStrings[i] = serializedJWT;
        }

        // Instruct the view to display the first JOSE object
        view.setJOSEObjects(joseObjectStrings);
        if(joseObjects.size() > 0){
            view.setSelected(0);
        }
    }

    /**
     * Display a JWS in the editor
     * @param jws the JWS to display
     */
    private void setJWS(JWS jws){

        // Check if the header survives pretty printing and compaction without changes (i.e it was compact when deserialized)
        String header = jws.getHeader();
        try {
            String prettyPrintedJSON = JSONUtils.prettyPrintJSON(header);
            if(JSONUtils.compactJSON(prettyPrintedJSON).equals(header)) {
                // If it does, display the pretty printed version
                view.setJWSHeaderCompact(true);
                view.setJWSHeader(prettyPrintedJSON);
            }
            else {
                // Otherwise, it contained whitespace, so don't try to pretty print, as the re-compacted version won't match the original
                view.setJWSHeaderCompact(false);
                view.setJWSHeader(header);
            }
        }
        catch(JSONException e){
            view.setJWSHeader(header);
        }

        // Check if the payload survives pretty printing and compaction without changes (i.e it was compact when deserialized)
        String payload = jws.getPayload();
        try {
            String prettyPrintedJSON = JSONUtils.prettyPrintJSON(payload);
            if(JSONUtils.compactJSON(prettyPrintedJSON).equals(payload)) {
                view.setJWSPayloadCompact(true);
                view.setPayload(prettyPrintedJSON);
            }
            else {
                view.setJWSPayloadCompact(false);
                view.setPayload(payload);
            }
        }
        catch(JSONException e){
            view.setPayload(payload);
        }

        // Set the signature hex view
        view.setSignature(jws.getSignature());
    }

    /**
     * Convert the text/hex entry fields to a JWS
     * @return the JWS built from the editor entry fields
     */
    private JWS getJWS() {
        Base64URL header;
        Base64URL payload;

        // Get the header text entry as base64. Compact the JSON if the compact checkbox is ticked
        // Return the entry encoded as-is if this fails, or the compact checkbox is unticked
        try {
            if (view.getJWSHeaderCompact()) {
                header = Base64URL.encode(JSONUtils.compactJSON(view.getJWSHeader()));
            } else {
                header = Base64URL.encode(view.getJWSHeader());
            }
        }
        catch (JSONException e) {
            header = Base64URL.encode(view.getJWSHeader());
        }

        // Get the payload text entry as base64. Compact the JSON if the checkbox is ticked
        // Return the entry encoded as-is if this fails, or the compact checkbox is unticked
        try {
            if (view.getJWSPayloadCompact()) {
                payload = Base64URL.encode(JSONUtils.compactJSON(view.getPayload()));
            } else {
                payload = Base64URL.encode(view.getPayload());
            }
        }
        catch (JSONException e) {
            payload = Base64URL.encode(view.getPayload());
        }

        return jwsFromParts(header, payload, Base64URL.encode(view.getSignature()));
    }

    /**
     * Display a JWE in the editor
     * @param jwe the JWE to display
     */
    private void setJWE(JWE jwe){

        // Check if the header survives pretty printing and compaction without changes (i.e it was compact when deserialized)
        String header = jwe.getHeader();
        try {
            String prettyPrintedJSON = JSONUtils.prettyPrintJSON(header);
            if(JSONUtils.compactJSON(prettyPrintedJSON).equals(header)) {
                // If it does, display the pretty printed version
                view.setJWEHeaderCompact(true);
                view.setJWEHeader(prettyPrintedJSON);
            }
            else {
                // Otherwise, it contained whitespace, so don't try to pretty print, as the re-compacted version won't match the original
                view.setJWEHeaderCompact(false);
                view.setJWEHeader(header);
            }
        }
        catch(JSONException e){
            view.setJWEHeader(header);
        }

        // Set the other JWE fields - these are all byte arrays
        view.setEncryptedKey(jwe.getEncryptedKey());
        view.setCiphertext(jwe.getCiphertext());
        view.setIV(jwe.getIV());
        view.setTag(jwe.getTag());
    }

    /**
     * Convert the text/hex entry fields to a JWE
     * @return the JWE built from the editor entry fields
     */
    private JWE getJWE() {

        Base64URL header;
        Base64URL encryptedKey = Base64URL.encode(view.getEncryptedKey());
        Base64URL iv = Base64URL.encode(view.getIV());
        Base64URL ciphertext = Base64URL.encode(view.getCiphertext());
        Base64URL tag = Base64URL.encode(view.getTag());

        // Get the header text entry as base64. Compact the JSON if the compact checkbox is ticked
        // Return the entry encoded as-is if this fails, or the compact checkbox is unticked
        try {
            if (view.getJWEHeaderCompact()) {
                header = Base64URL.encode(JSONUtils.compactJSON(view.getJWEHeader()).getBytes(StandardCharsets.UTF_8));
            } else {
                header = Base64URL.encode(view.getJWEHeader().getBytes(StandardCharsets.UTF_8));
            }
        }
        catch (JSONException e){
            header = Base64URL.encode(view.getJWEHeader().getBytes(StandardCharsets.UTF_8));
        }

        return jweFromParts(header, encryptedKey, iv, ciphertext, tag);
    }

    /**
     * Handle clicks events from the Embedded JWK Attack button
     */
    public void onAttackEmbedJWKClicked() {
        signingDialog(SignDialog.Mode.EMBED_JWK);
    }

    /**
     * Handle click events from the HMAC Key Confusion button
     */
    public void onAttackKeyConfusionClicked() {
        KeysPresenter keysPresenter = (KeysPresenter) presenters.get(KeysPresenter.class);

        List<Key> attackKeys = new ArrayList<>();

        // Get a list of verification capable public keys
        List<Key> verificationKeys = keysPresenter.getVerificationKeys();
        for(Key signingKey: verificationKeys) {
            if(signingKey.isPublic() && signingKey.hasPEM()){
                attackKeys.add(signingKey);
            }
        }

        if(attackKeys.size() == 0) {
            JOptionPane.showMessageDialog(view.uiComponent(), Utils.getResourceString("error_no_signing_keys"), Utils.getResourceString("error_title_no_signing_keys"), WARNING_MESSAGE);
            return;
        }

        // Create the key confusion attack dialog with the JWS currently in the editor fields
        KeyConfusionAttackDialog keyConfusionAttackDialog = new KeyConfusionAttackDialog(view.window(), verificationKeys, getJWS());
        keyConfusionAttackDialog.pack();
        keyConfusionAttackDialog.setLocationRelativeTo(view.window());
        keyConfusionAttackDialog.setVisible(true);
        // Blocks here until dialog finishes

        // Set the result as the JWS in the editor if the attack succeeds
        JWS signedJWS = keyConfusionAttackDialog.getJWS();
        if(signedJWS != null) {
            setJWS(signedJWS);
        }
    }

    /**
     * Handle clicks events from the none Signing algorithm button
     */
    public void onAttackSignNoneClicked() {
        // Get the JWS from the editor, strip the signature and set the editor to the new JWS
        NoneDialog noneDialog = new NoneDialog(view.window(), getJWS());
        noneDialog.pack();
        noneDialog.setLocationRelativeTo(view.window());
        noneDialog.setVisible(true);

        JWS unsignedJWS = noneDialog.getJWS();

        if (unsignedJWS != null) {
            setJWS(unsignedJWS);
        }
    }

    public void onAttackSignEmptyKeyClicked() {
        EmptyKeySigningDialog signingDialog = new EmptyKeySigningDialog(view.window(), getJWS());
        signingDialog.pack();
        signingDialog.setLocationRelativeTo(view.window());
        signingDialog.setVisible(true);

        JWS signedJWS = signingDialog.getJWS();

        if (signedJWS != null) {
            setJWS(signedJWS);
        }
    }

    /**
     * Handle click events from the Sign button
     */
    public void onSignClicked(){
        signingDialog(SignDialog.Mode.NORMAL);
    }

    /**
     * Create a signing dialog based on the provided mode
     *
     * @param mode mode of the signing dialog to display
     */
    private void signingDialog(SignDialog.Mode mode){
        KeysPresenter keysPresenter = (KeysPresenter) presenters.get(KeysPresenter.class);

        // Check there are signing keys in the keystore
        if(keysPresenter.getSigningKeys().size() == 0) {
            JOptionPane.showMessageDialog(view.uiComponent(), Utils.getResourceString("error_no_signing_keys"), Utils.getResourceString("error_title_no_signing_keys"), WARNING_MESSAGE);
            return;
        }

        // Create a new signing dialog
        SignDialog signDialog = new SignDialog(view.window(), keysPresenter.getSigningKeys(), getJWS(), mode);
        signDialog.pack();
        signDialog.setLocationRelativeTo(view.window());
        signDialog.setVisible(true);
        // Block here until dialog completes

        // If a JWS was created by the dialog, replace the contents of the editor
        JWS signedJWS = signDialog.getJWS();
        if(signedJWS != null) {
            setJWS(signedJWS);
        }
    }

    /**
     * Handle click events from the Verify button
     */
    public void onVerifyClicked() {
        List<Key> keys = ((KeysPresenter) presenters.get(KeysPresenter.class)).getVerificationKeys();
        String titleKey;
        String message;

        // Check there are verification keys in the keystore
        if (keys.isEmpty()) {
            titleKey = "error_title_no_verification_keys";
            message = Utils.getResourceString("error_no_verification_keys");
        } else {
            titleKey = "editor_view_message_title_verification";
            KeyRing keyRing = new KeyRing(keys);

            message = keyRing.findVerifyingKey(getJWS())
                    .map(key -> Utils.getResourceString("editor_view_message_verified").formatted(key.getID()))
                    .orElseGet(() -> Utils.getResourceString("editor_view_message_not_verified"));
        }

        JOptionPane.showMessageDialog(
                view.uiComponent(),
                message,
                Utils.getResourceString(titleKey),
                WARNING_MESSAGE
        );
    }

    /**
     * Handle click events from the Encrypt button
     */
    public void onEncryptClicked(){
        KeysPresenter keysPresenter = (KeysPresenter) presenters.get(KeysPresenter.class);

        // Check there are encryption keys in the keystore
        if(keysPresenter.getEncryptionKeys().size() == 0) {
            JOptionPane.showMessageDialog(view.uiComponent(), Utils.getResourceString("error_no_encryption_keys"), Utils.getResourceString("error_title_no_encryption_keys"), WARNING_MESSAGE);
            return;
        }

        // Show the encryption dialog
        EncryptDialog encryptDialog = new EncryptDialog(view.window(), getJWS(), keysPresenter.getEncryptionKeys());
        encryptDialog.pack();
        encryptDialog.setLocationRelativeTo(view.window());
        encryptDialog.setVisible(true);
        // Block here until dialog completes

        // If a JWE was created by the dialog, replace the contents of the editor and change to JWE mode
        JWE jwe = encryptDialog.getJWE();
        if(jwe != null){
            view.setJWEMode();
            setJWE(jwe);
        }
    }

    /**
     * Handle click events from the Decrypt button
     */
    public void onDecryptClicked(){
        KeysPresenter keysPresenter = (KeysPresenter) presenters.get(KeysPresenter.class);

        // Check there are decryption keys in the keystore
        if (keysPresenter.getDecryptionKeys().isEmpty()) {
            JOptionPane.showMessageDialog(view.uiComponent(), Utils.getResourceString("error_no_decryption_keys"), Utils.getResourceString("error_title_no_decryption_keys"), WARNING_MESSAGE);
            return;
        }

        // Attempt to decrypt the contents of the editor with all available keys
        try {
            List<Key> keys = keysPresenter.getDecryptionKeys();
            Optional<JWS> jws = new KeyRing(keys).attemptDecryption(getJWE());

            // If decryption was successful, set the contents of the editor to the decrypted JWS and set the editor mode to JWS
            if (jws.isPresent()) {
                view.setJWSMode();
                setJWS(jws.get());
            } else {
                JOptionPane.showMessageDialog(view.uiComponent(), Utils.getResourceString("error_decryption_all_keys_failed"), Utils.getResourceString("error_title_unable_to_decrypt"), WARNING_MESSAGE);
            }
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(view.uiComponent(), Utils.getResourceString("error_decryption_invalid_header"), Utils.getResourceString("error_title_unable_to_decrypt"), WARNING_MESSAGE);
        }
    }

    /**
     * Handle click events from the Copy button
     */
    public void onCopyClicked() {
        Utils.copyToClipboard(view.getSerialized());
    }

    /**
     * Get the message set by setMessage with the changes made by the editor
     *
     * @return the altered message
     */
    public String getMessage() {
        // Create two lists, one containing the original, the other containing the modified version at the same index
        List<String> searchList = new ArrayList<>();
        List<String> replacementList = new ArrayList<>();

        //Add a replacement pair to the lists if the JOSEObjectPair has changed
        for(MutableJOSEObject mutableJoseObject : mutableJoseObjects){
            if(mutableJoseObject.changed()) {
                searchList.add(mutableJoseObject.getOriginal());
                replacementList.add(mutableJoseObject.getModified().serialize());
            }
        }

        // Conver the lists to arrays
        String[] search = new String[searchList.size()];
        searchList.toArray(search);
        String[] replacement = new String[replacementList.size()];
        replacementList.toArray(replacement);

        // Use the Apache Commons StringUtils library to do in-place replacement
        return StringUtils.replaceEach(message, search, replacement);
    }

    /**
     * Has the editor modified any of the JOSE Objects
     *
     * @return true if changes have been made in the editor
     */
    public boolean isModified() {
        return mutableJoseObjects.stream().anyMatch(MutableJOSEObject::changed);
    }

    /**
     * Callback called by the view whenever the dropdown selection is changed
     */
    public void onSelectionChanged() {
        // Set a selectionChanging to true, so componentChanged doesn't treat the change as a user event
        selectionChanging = true;

        // Get the JOSEObject pair corresponding to the selected dropdown entry index
        MutableJOSEObject mutableJoseObject = mutableJoseObjects.get(view.getSelected());
        JOSEObject joseObject = mutableJoseObject.getModified();

        // Change to JWE/JWS mode based on the newly selected JOSEObject
        if(joseObject instanceof JWS){
            view.setJWSMode();
            setJWS((JWS) joseObject);
        }
        else {
            view.setJWEMode();
            setJWE((JWE) joseObject);
        }

        // Allow user events in componentChanged again
        selectionChanging = false;
    }

    /**
     * Callback called by the view whenever the contents of a text or hex editor changes, or the compact checkbox is modified
     */
    public void componentChanged() {
        // Get the currently selected object
        MutableJOSEObject mutableJoseObject = mutableJoseObjects.get(view.getSelected());

        //Serialize the text/hex entries to a JWS/JWE in compact form, depending on the editor mode
        JOSEObject joseObject = view.getMode() == EditorView.TAB_JWS ? getJWS() : getJWE();
        //Update the JOSEObjectPair with the change
        mutableJoseObject.setModified(joseObject);
        //Highlight the serialized text as changed if it differs from the original, and the change wasn't triggered by onSelectionChanging
        view.setSerialized(joseObject.serialize(), mutableJoseObject.changed() && !selectionChanging);
    }

    /**
     * Handle click events from the JWE Header Format button
     */
    public void formatJWEHeader() {
        try {
            view.setJWEHeader(JSONUtils.prettyPrintJSON(view.getJWEHeader()));
        }
        catch (JSONException e){
            JOptionPane.showMessageDialog(view.uiComponent(), Utils.getResourceString("error_format_json"), Utils.getResourceString("error_title_unable_to_format_json"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handle click events from the JWS Header Format button
     */
    public void formatJWSHeader() {
        try {
            view.setJWSHeader(JSONUtils.prettyPrintJSON(view.getJWSHeader()));
        }
        catch (JSONException e){
            JOptionPane.showMessageDialog(view.uiComponent(), Utils.getResourceString("error_format_json"), Utils.getResourceString("error_title_unable_to_format_json"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handle click events from the JWS Payload Format button
     */
    public void formatJWSPayload() {
        try {
            view.setPayload(JSONUtils.prettyPrintJSON(view.getPayload()));
        }
        catch (JSONException e){
            JOptionPane.showMessageDialog(view.uiComponent(), Utils.getResourceString("error_format_json"), Utils.getResourceString("error_title_unable_to_format_json"), JOptionPane.ERROR_MESSAGE);
        }
    }
}
