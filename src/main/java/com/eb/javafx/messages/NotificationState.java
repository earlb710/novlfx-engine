package com.eb.javafx.messages;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;

/** Mutable ordered notification collection with read/unread helpers. */
public final class NotificationState {
    private final List<Notification> notifications = new ArrayList<>();

    public void add(Notification notification) {
        notifications.add(Validation.requireNonNull(notification, "Notification is required."));
    }

    public void markRead(String notificationId) {
        String checkedId = Validation.requireNonBlank(notificationId, "Notification id is required.");
        for (int index = 0; index < notifications.size(); index++) {
            Notification notification = notifications.get(index);
            if (notification.id().equals(checkedId)) {
                notifications.set(index, notification.markRead());
                return;
            }
        }
        throw new IllegalArgumentException("Unknown notification: " + checkedId);
    }

    public List<Notification> notifications() {
        return List.copyOf(notifications);
    }

    public List<Notification> unread() {
        return notifications.stream().filter(notification -> !notification.read()).toList();
    }
}
