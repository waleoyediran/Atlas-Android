package com.layer.atlas;

import java.util.Comparator;

public class Contact {
    public String userId;

    public String firstName;
    public String lastName;
    public String email;

    public String getInitials() {
        StringBuilder sb = new StringBuilder();
        sb.append(firstName != null && firstName.trim().length() > 0 ? firstName.trim().charAt(0) : "");
        sb.append(lastName != null && lastName.trim().length() > 0 ? lastName.trim().charAt(0) : "");
        return sb.toString();
    }

    public String getFirstAndL() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && firstName.trim().length() > 0) {
            sb.append(firstName.trim()).append(" ");
        }
        if (lastName != null && lastName.trim().length() > 0) {
            sb.append(lastName.trim().charAt(0));
            sb.append(".");
        }
        return sb.toString();
    }

    public String getFirstAndLast() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && firstName.trim().length() > 0) {
            sb.append(firstName.trim()).append(" ");
        }
        if (lastName != null && lastName.trim().length() > 0) {
            sb.append(lastName.trim());
        }
        return sb.toString();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Contact [userId: ").append(userId).append(", firstName: ").append(firstName).append(", lastName: ").append(lastName).append(", email: ").append(email).append("]");
        return builder.toString();
    }

    public static final Comparator<Contact> FIRST_LAST_EMAIL_ASCENDING = new Comparator<Contact>() {
        public int compare(Contact lhs, Contact rhs) {
            int result = compareSting(lhs.firstName, rhs.firstName);
            if (result != 0) return result;
            result = compareSting(lhs.lastName, rhs.lastName);
            if (result != 0) return result;
            result = compareSting(lhs.email, rhs.email);
            return result;
        }
    };

    public static int compareSting(String lhs, String rhs) {
        if (lhs == null) {
            if (rhs == null) return 0;
            return -1;
        }
        if (rhs == null) return 1;
        return String.CASE_INSENSITIVE_ORDER.compare(lhs, rhs);
    }

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
            int left = lhs != null ? lhs.toLowerCase().indexOf(filter) : -1;
            int right = rhs != null ? rhs.toLowerCase().indexOf(filter) : -1;

            if (left == -1 && right == -1) return 0;
            if (left != -1 && right == -1) return -1;
            if (left == -1 && right != -1) return 1;
            if (left - right != 0) return left - right;
            return String.CASE_INSENSITIVE_ORDER.compare(lhs, rhs);
        }
    }
}
