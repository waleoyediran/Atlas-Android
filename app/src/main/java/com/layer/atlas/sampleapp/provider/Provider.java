/**
 * Layer Android SDK
 *
 * Created by Steven Jones on 5/30/14
 * Copyright (c) 2013 Layer. All rights reserved.
 */
package com.layer.atlas.sampleapp.provider;

import java.util.List;

public interface Provider {
    public String getAppId();

    public Contact getLoggedInContact();

    public List<Contact> getAddressBook();

    public Contact getContact(String id);

    public void clear(ClearCallback callback);

    public static interface ClearCallback {
        public void onClearSuccess();

        public void onClearError(String error);
    }

    public void providerResume(LoggedInContact loggedInContact);

    public void providerLogin(String email, String password, String nonce, LoginCallback callback);

    public void providerLogout();

    public static interface LoginCallback {
        public void onLoginSuccess(LoggedInContact loggedInContact);

        public void onLoginError(String error);
    }


    public void providerRegister(String first, String last, String email, String password, RegisterCallback callback);

    public static interface RegisterCallback {
        public void onRegisterSuccess(RegisteredContact registeredContact);

        public void onRegisterError(String error);
    }


    public void refreshAddressBook(String userId, AddressbookCallback callback);

    public static interface AddressbookCallback {
        public void onAddressBookSuccess(List<Contact> addressBook);

        public void onAddressBookError(String error);
    }
}
