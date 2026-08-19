package com.andrejKir.connect.accounts.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.Nullable;


import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class AppUser {

    private static final int DISPLAY_NAME_MAX_LENGTH = 100;

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(unique = true, nullable = false, length = 40)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = DISPLAY_NAME_MAX_LENGTH)
    private String displayName;

    @Column(nullable = false,length = 100)
    private String firstName;

    @Column(nullable = false, length = 150)
    private String lastName;

    @Column(nullable = false, length = 256)
    private String description;

    @Column(nullable = false, length = 10)
    private String inviteCode;

    @Column(nullable = false)
    private LocalDate birthDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    private Instant deactivatedAt;

    protected AppUser() {
    }

    public AppUser(String username, String email, String passwordHash, String displayName,
                   String firstName, String lastName,String inviteCode, LocalDate birthDate) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.inviteCode = inviteCode;
        this.birthDate = birthDate;
        this.displayName = resolveDisplayName(displayName, firstName, lastName);
        this.description = "";
    }

    private static String resolveDisplayName(String displayName, String firstName, String lastName) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        String fullName = firstName + " " + lastName;
        return fullName.length() <= DISPLAY_NAME_MAX_LENGTH ? fullName : firstName;
    }

    public UUID getId(){
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {return passwordHash;}

    public String getDisplayName() {
        return displayName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getInviteCode(){return inviteCode;}

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getDescription(){return description;}

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public boolean isDeactivated() {
        return deactivatedAt != null;
    }

    public void deactivate(Instant at) {
        if (deactivatedAt == null) {
            this.deactivatedAt = at;
        }
    }

    public void changePassword(String passwordHash){
        this.passwordHash = passwordHash;
    }
}
