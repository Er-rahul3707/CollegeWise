package com.collegewise.security;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JwtServiceTest {
    @Test void issuesAndParsesRoleAndSubject(){
        JwtService service=new JwtService(randomSecret(),60000);
        String token=service.issue("student@example.com","STUDENT");
        assertEquals("student@example.com",service.parse(token).getSubject());
        assertEquals("STUDENT",service.parse(token).get("role"));
    }
    @Test void rejectsShortSecret(){assertThrows(IllegalArgumentException.class,()->new JwtService("short",1000));}
    private static String randomSecret(){byte[] value=new byte[48];new SecureRandom().nextBytes(value);return Base64.getEncoder().encodeToString(value);}
}
