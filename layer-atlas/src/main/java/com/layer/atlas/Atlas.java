package com.layer.atlas;

import java.util.Comparator;
import java.util.HashMap;

import org.json.JSONObject;

import com.layer.sdk.LayerClient;
import com.layer.sdk.messaging.Conversation;

/**
 * @author olegorlov
 * @since 12 May 2015
 */
public class Atlas {

    public static final String MIME_TYPE_ATLAS_LOCATION = "location/coordinate";
    public static final String MIME_TYPE_TEXT = "text/plain";
    public static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_IMAGE_PNG = "image/png";

    private Conversation conv;
    private LayerClient layerClient;
    public HashMap<String, Contact> contactsMap = new HashMap<String, Contact>();
    
    public Atlas(LayerClient layerClient) {
        this.layerClient = layerClient; 
    }

    public LayerClient getLayerClient() {
        return layerClient;
    }

    public static class Contact {
        
        public String userId;
        public String firstName;
        public String lastName;
        public String email;
        
        public static Contact fromRecord(JSONObject jObject) {
            Contact contact = new Contact();
            contact.userId = jObject.optString("id");
            contact.firstName = jObject.optString("first_name");
            contact.lastName = jObject.optString("last_name");
            contact.email = jObject.optString("email");
            return contact;
        }
    
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Contact [userId: ").append(userId).append(", firstName: ").append(firstName).append(", lastName: ").append(lastName).append(", email: ").append(email).append("]");
            return builder.toString();
        }
        
        public static final Comparator<Contact> FIRST_LAST_EMAIL_ASCENDING = new Comparator<Atlas.Contact>() {
            public int compare(Contact lhs, Contact rhs) {
                int result = String.CASE_INSENSITIVE_ORDER.compare(lhs.firstName, rhs.firstName);
                if (result != 0) return result;
                result = String.CASE_INSENSITIVE_ORDER.compare(lhs.lastName, rhs.lastName);
                if (result != 0) return result;
                result = String.CASE_INSENSITIVE_ORDER.compare(lhs.email, rhs.email);
                return 0;
            }
        };
    
        public static final class FilteringComparator implements Comparator<Contact> {
            
            private final String filter;
        
            /** 
             * @param filter - the less indexOf(filter) the less order of contact
             */
            public FilteringComparator(String filter) {
                this.filter = filter;
            }
        
            @Override
            public int compare(Contact lhs, Contact rhs) {
                int result = subCompareCaseInsensitive(lhs.firstName, rhs.firstName);
                if (result != 0) return result;
                result = subCompareCaseInsensitive(lhs.lastName, rhs.lastName);
                if (result != 0) return result;
                return subCompareCaseInsensitive(lhs.email, rhs.email);
            }
        
            private int subCompareCaseInsensitive(String lhs, String rhs) {
                int left  = lhs != null ? lhs.toLowerCase().indexOf(filter) : -1;
                int right = rhs != null ? rhs.toLowerCase().indexOf(filter) : -1;
                
                if (left == -1 && right == -1) return 0;
                if (left != -1 && right == -1) return -1;
                if (left == -1 && right != -1) return 1;
                if (left - right != 0) return left - right;
                return String.CASE_INSENSITIVE_ORDER.compare(lhs, rhs);
            }
        }
        
    }

    public static class AtlasContactProvider {
        public HashMap<String, Contact> contactsMap = new HashMap<String, Contact>();

        // -------    Tools   -----
        //
        //
        public static String getContactInitials(Atlas.Contact contact) {
            if (contact == null) return null;
            StringBuilder sb = new StringBuilder();
            sb.append(contact.firstName != null && contact.firstName.trim().length() > 0 ? contact.firstName.trim().charAt(0) : "");
            sb.append(contact.lastName != null  && contact.lastName.trim().length() > 0 ?  contact.lastName.trim().charAt(0) : "");
            return sb.toString();
        }

        public static String getContactFirstAndL(Atlas.Contact contact) {
            if (contact == null) return null;
            StringBuilder sb = new StringBuilder();
            if (contact.firstName != null && contact.firstName.trim().length() > 0) {
                sb.append(contact.firstName.trim()).append(" ");
            }
            if (contact.lastName != null && contact.lastName.trim().length() > 0) {
                sb.append(contact.lastName.trim().charAt(0));
                sb.append(".");
            }
            return sb.toString();
        }

        public static String getContactFirstAndLast(Atlas.Contact contact) {
            if (contact == null) return null;
            StringBuilder sb = new StringBuilder();
            if (contact.firstName != null && contact.firstName.trim().length() > 0) {
                sb.append(contact.firstName.trim()).append(" ");
            }
            if (contact.lastName != null && contact.lastName.trim().length() > 0) {
                sb.append(contact.lastName.trim());
            }
            return sb.toString();
        }
    }
    
    
}
