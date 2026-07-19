package com.andrejKir.connect.accounts.validation;


import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
public class WeakPasswordBlocklistTest {

    private WeakPasswordBlocklist blocklist = new WeakPasswordBlocklist();

    @Test
    void contains_commonPassword_returnsTrue(){
        assertTrue(blocklist.contains("password"));
    }

    @Test
    void contains_strongPassword_returnsFalse(){
        assertFalse(blocklist.contains("trombone-sunset-91"));
    }

    @Test
    void contains_CapitalizedCommonPassword_returnsTrue(){
        assertTrue(blocklist.contains("PASSWORD"));
    }
}
