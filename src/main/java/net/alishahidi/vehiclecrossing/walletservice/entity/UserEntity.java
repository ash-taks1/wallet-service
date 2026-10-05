package net.alishahidi.vehiclecrossing.walletservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import net.alishahidi.vehiclecrossing.walletservice.entity.base.BaseEntity;

@Entity
@Table(name = "users")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserEntity extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true, length = 320)
    String email;

    @Column(name = "full_name", nullable = false, length = 200)
    String fullName;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;
}
