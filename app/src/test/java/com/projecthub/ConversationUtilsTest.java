package com.projecthub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.projecthub.models.Conversation;
import com.projecthub.utils.ConversationUtils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConversationUtilsTest {

    @Test
    public void buildDirectConversationId_isSymmetricAndRejectsInvalidInputs() {
        String id1 = ConversationUtils.buildDirectConversationId("userB", "userA");
        String id2 = ConversationUtils.buildDirectConversationId("userA", "userB");

        assertEquals("dm_userA_userB", id1);
        assertEquals(id1, id2);
        assertEquals(Arrays.asList("userA", "userB"), ConversationUtils.sortedDirectParticipants("userB", "userA"));

        assertEquals("", ConversationUtils.buildDirectConversationId("userA", "userA"));
        assertEquals("", ConversationUtils.buildDirectConversationId("", "userB"));
        assertEquals("", ConversationUtils.buildDirectConversationId(null, "userB"));
    }

    @Test
    public void resolveDisplayTitle_handlesDirectAndGroupConversations() {
        Conversation dm = new Conversation();
        dm.setType("DIRECT");
        dm.setParticipantIds(Arrays.asList("u1", "u2"));
        Map<String, String> names = new HashMap<>();
        names.put("u1", "Aarav");
        names.put("u2", "Diya");
        dm.setParticipantNames(names);

        assertEquals("Diya", ConversationUtils.resolveDisplayTitle(dm, "u1"));
        assertEquals("Aarav", ConversationUtils.resolveDisplayTitle(dm, "u2"));

        Conversation group = new Conversation();
        group.setType("GROUP");
        group.setTitle("Backend Sprint Squad");
        group.setParticipantIds(Arrays.asList("u1", "u2", "u3"));
        assertEquals("Backend Sprint Squad", ConversationUtils.resolveDisplayTitle(group, "u1"));
    }

    @Test
    public void isUnread_tracksSenderAndReadTimestamps() {
        Conversation conv = new Conversation();
        conv.setLastMessageTimestamp(5000L);
        conv.setLastMessageSenderId("u2");

        // Self-sent message is never unread
        assertFalse(ConversationUtils.isUnread(conv, "u2"));

        // Never read by u1 -> unread
        assertTrue(ConversationUtils.isUnread(conv, "u1"));

        // Read before latest message -> unread
        conv.setReadBy(new HashMap<>(Collections.singletonMap("u1", 4000L)));
        assertTrue(ConversationUtils.isUnread(conv, "u1"));

        // Caught up -> read
        conv.setReadBy(new HashMap<>(Collections.singletonMap("u1", 5000L)));
        assertFalse(ConversationUtils.isUnread(conv, "u1"));
    }
}
