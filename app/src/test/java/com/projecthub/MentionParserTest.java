package com.projecthub;

import com.projecthub.models.User;
import com.projecthub.utils.MentionParser;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MentionParserTest {

    @Test
    public void extractMentionTokens_extractsUniqueLowercaseHandles() {
        List<String> tokens = MentionParser.extractMentionTokens(
            "Hey @RahulSharma and @priya.patel, please check @RahulSharma's PR!"
        );
        assertEquals(2, tokens.size());
        assertEquals("rahulsharma", tokens.get(0));
        assertEquals("priya.patel", tokens.get(1));
    }

    @Test
    public void resolveMentionedUsers_matchesNameAndEmailPrefix() {
        User u1 = new User("u1", "Rahul Sharma", "rahul@nit.ac.in", "CSE", "21CS01");
        User u2 = new User("u2", "Priya Patel", "priya99@mit.edu", "IT", "21IT04");
        User u3 = new User("u3", "Ananya Rao", "ananya@nit.ac.in", "ECE", "21EC02");

        List<User> resolved = MentionParser.resolveMentionedUsers(
            "Assigning review to @Rahul and @priya99",
            Arrays.asList(u1, u2, u3)
        );

        assertEquals(2, resolved.size());
        assertTrue(resolved.stream().anyMatch(u -> "u1".equals(u.getUserId())));
        assertTrue(resolved.stream().anyMatch(u -> "u2".equals(u.getUserId())));
    }
}
