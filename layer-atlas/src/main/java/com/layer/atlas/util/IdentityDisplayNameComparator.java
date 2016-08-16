package com.layer.atlas.util;


import com.layer.sdk.messaging.Identity;

import java.util.Comparator;

public class IdentityDisplayNameComparator implements Comparator<Identity> {

    @Override
    public int compare(Identity lhs, Identity rhs) {
        if (lhs.getDisplayName() == null) {
            if (rhs.getDisplayName() == null) {
                return 0;
            } else {
                return -1;
            }
        }
        return lhs.getDisplayName().compareTo(rhs.getDisplayName());
    }
}
